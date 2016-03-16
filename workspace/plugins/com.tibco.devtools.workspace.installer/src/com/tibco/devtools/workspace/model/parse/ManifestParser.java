package com.tibco.devtools.workspace.model.parse;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tibco.devtools.workspace.model.BundleDescriptor;
import com.tibco.devtools.workspace.model.ExportPackageSpec;
import com.tibco.devtools.workspace.model.ImportPackageSpec;
import com.tibco.devtools.workspace.model.Problem;
import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.RequireBundleSpec;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;
import com.tibco.devtools.workspace.util.NoRemoveIterator;

public class ManifestParser {

	/**
	 * Iterate over all of the valid feature files in a directory.
	 * 
	 * @param dir	The directory to scan.
	 * 
	 * @return The iterator over the entries in the directory.
	 */
	public static Iterable<BundleDescriptor> bundlesInDir(File dir) {
		return new BundleDescriptorIterator(dir);
	}
	
	public static File[] potentialBundleJars(File dir) {
		return dir.listFiles(sm_filterForBundleJars);
	}
	
	/**
	 * Parse a range string of the form [versLow,versHigh], or [..), or (..), or (..],
	 * where versLow and versHigh are OSGi format version strings.
	 * 
	 * @param str	What to parse.
	 * 
	 * @return	The range to return.
	 */
	public static Range<VersionInfo> parseRange(String str) {
		
		// is this an "at least" range?
		if (str.charAt(0) != '(' && str.charAt(0) != '[') {
			VersionInfo start = VersionInfo.parseVersion(str);
			return new Range<VersionInfo>( start, true, VersionInfo.UNBOUNDED, true);
		}
		else {
			Matcher matcher = sm_rangeExpr.matcher(str);
			if (matcher.matches()) {
				return new Range<VersionInfo>( VersionInfo.parseVersion(matcher.group(2)),
						matcher.group(1).equals("["), VersionInfo.parseVersion(matcher.group(3)),
						matcher.group(4).equals("]"));
			}
			throw new IllegalArgumentException("Unrecognized version range: " + str);
		}
	}
	
	protected ManifestParser() {
	}
	
	protected void parse(String source, Manifest mf) {
		
		m_currentBundle = source;
		Attributes attrs = mf.getMainAttributes();
		String symbolicName = attrs.getValue("bundle-symbolicname");
		if (symbolicName == null) {
			throw new BadBundleException("Missing a Bundle-SymbolicName entry.");
		}
		parseSymbolicName(symbolicName);
		
		VersionInfo version = ZERO_VERSION;
		String verStr = attrs.getValue("bundle-version");
		if (verStr != null) {
			version = VersionInfo.parseVersion(verStr);
		}
		else {
			addProblem("NO_BUNDLE_VERSION",
					"No Bundle-Version specified for {0}", m_currentBundle);
		}

		m_bd.setTarget(new Target<VersionInfo>(m_symbolicName, version) );

		for (Map.Entry<Object, Object> entries : attrs.entrySet()) {
			
			String key = entries.getKey().toString().toLowerCase();
			m_currentHeader = key;
			
			String value = entries.getValue().toString();
			
			if (key.equals("bundle-symbolicname") || key.equals("bundle-version")) {
				// do nothing, we grabbed it before the loop.
			}
			else if (key.equals("require-bundle")) {
				parseRequireBundle(value);
			}
			else if (key.equals("bundle-classpath")) {
				parseBundleClassPath(value);
			}
			else if (key.equals("manifest-version")) {
				if (!"1.0".equals(value) && !"1".equals(value)) {
					addProblem("ODD_MANIFEST_VERSION",
							"Unrecognized Manifest-Version of \"{0}\" in bundle {1}", value, m_currentBundle);
				}
			}
			else if (key.equals("bundle-manifestversion")) {
				if (!"2".equals(value)) {
					throw new BadBundleException("Unrecognized Bundle-ManifestVersion of " + value);
				}
			}
			else if (key.equals("bundle-requiredexecutionenvironment")) {
				parseRequiredExecutionEnvironment(value);
			}
			else if (key.equals("export-package")) {
				parseExportPackage(value);
			}
			else if (key.equals("import-package")) {
				parseImportPackage(value);
			}
			else if (key.equals("bundle-activationpolicy")) {
				parseActivationPolicy(value);
			}
			else if (key.equals("bundle-activator")) {
				// TODO - should check that this is a valid Java identifer.
				m_bd.setActivator(value);
			}
			else if (sm_ignoredHeaders.contains(key)) {
				// TODO - which of the above do we actually want to capture?
			}
			else {
				addProblem("UNRECOGNIZED_HEADER",
						"Unrecognized bundle header {0} from bundle {1}", key,
						m_currentBundle);
			}
		}
	}

	private void parseActivationPolicy(String value) {
		List<CharSequence> options = splitOnChar(value, ';');

		if (!options.get(0).toString().equals("lazy") ) {
			addProblem("UNRECOGNIZED_ACTIVATION_POLICY",
					"Unrecognized Bundle-ActivationPolicy value \"{0}\" from bundle {1}",
					value, m_currentBundle);
		}
		options.remove(0);
		m_bd.setIsLazyActivate(true);
		
		Map<String, String> directives = DataUtils.newMap();
		Map<String, String> attributes = DataUtils.newMap();
		paramsIntoMap(options, directives, attributes, "Bundle-ActivationPolicy");
		
		if (attributes.size() > 0) {
			addProblem("ATTRIBUTES_NOT_ALLOWED",
					"Attributes not allowed on Bundle-ActivationPolicy in bundle {0}",
					m_currentBundle);
		}
		for ( Map.Entry<String, String> entry : directives.entrySet()) {
			String name = entry.getKey();
			
			if (name.equals("exclude")) {
				// TODO - do we care?
			}
		}
	}

	private void paramsIntoMap(List<CharSequence> items,
			Map<String, String> directives,
			Map<String, String> attributes, String header) {
		
		for (CharSequence item : items) {
			splitParameters(item, directives, attributes, header);
		}
		
	}
	private List<CharSequence> splitOnChar(CharSequence value, char ch) {
		
		int idx = 0;
		int lastStart = 0;
		List<CharSequence> results = DataUtils.newList();
		while (idx < value.length()) {
			if (value.charAt(idx) == ch) {
				addResultsAfterTrimmingSpaces(results, value, lastStart, idx);
				idx++;
				idx = skipSpaces(value, idx);
				lastStart = idx;
			}
			else {
				if (value.charAt(idx) == '"') {
					idx++;
					while (idx < value.length() && value.charAt(idx) != '"') {
						idx++;
					}
					if (idx < value.length()) {
						idx++;
					}
				}
				else {
					idx++;
				}
			}
		}
		
		addResultsAfterTrimmingSpaces(results, value, lastStart, idx);
		return results;
	}

	private void addResultsAfterTrimmingSpaces(List<CharSequence> results,
			CharSequence value, int startIdx, int endIdx) {
		
		startIdx = skipSpaces(value, startIdx);

		// trim off the spaces on the end.
		while (startIdx < endIdx && value.charAt(endIdx - 1) == ' ') {
			endIdx--;
		}
		results.add(value.subSequence(startIdx, endIdx));
	}
	
	private boolean containsEquals(CharSequence seq) {
		int idx = 0;
		while (idx < seq.length()) {
			if (seq.charAt(idx++) == '=') {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isValidPackageName(CharSequence pkgName) {
		
		int idx = 0;
		while (idx < pkgName.length()) {
			if (Character.isJavaIdentifierStart(pkgName.charAt(idx++))) {
				
				while (idx < pkgName.length() && Character.isJavaIdentifierPart(pkgName.charAt(idx))) {
					idx++;
				}
				if (idx < pkgName.length()) {
					if (pkgName.charAt(idx++) != '.') {
						return false;
					}
				}
			}
			else {
				return false;
			}
		}
		
		return true;
	}
	private void parseImportPackage(String value) {
		List<PackageDeclaration> pkgDecls = extractPackageDirectives(value, "Import-Package");

		for (PackageDeclaration onePkg : pkgDecls) {
			boolean mandatory = true;
			Range<VersionInfo> range = null;
			String pkgName = onePkg.packageName;
			
			String resolution = onePkg.directives.remove("resolution");
			if (resolution != null) {
				if (!resolution.equals("optional")) {
					addProblem("IMPORT_PACKAGE_RESOLUTION",
							"Import-Package statement has unrecognized resolution value of \"{0}\" in bundle {1}",
							resolution, m_currentBundle);
					
				}
				mandatory = false;
			}
			
			warnUnusedDirectives(onePkg.directives, "Import-Package");
			
			String bundleVersion = onePkg.attributes.remove("bundle-symbolic-name");
			if (bundleVersion != null) {
				addProblem("UNSUPPORTED_ATTRIBUTE",
						"Unsupported attribute 'bundle-symbolic-name' on Import-Package in bundle {0}",
						m_currentBundle);
			}
			
			String version = onePkg.attributes.remove("version");
			if (version != null) {
				range = parseRange(version);
			}
			
			if (range != null) {
				addProblem( isTibcoBundle() ? "MISSING_IMPORT_RANGE" : "NON_TIBCO_MISSING_IMPORT_RANGE",
						"No range specified for import of package {0} from bundle {1}",
						pkgName, m_currentBundle);
				
			}
			ImportPackageSpec ips = new ImportPackageSpec();
			ips.setPackageName(pkgName);
			ips.setMandatory(mandatory);
			ips.setImportRange(range);
			
			ips.getAttributes().putAll(onePkg.attributes);
			
			m_bd.getImportPackages().add(ips);
		}
	}
	
	private void addProblem(String identifier, String format, Object ... params) {
		
		m_bd.getProblems().add(
				new GenericProblem(identifier,
						format,
						params));
		
	}
	private static class PackageDeclaration {
		
		public PackageDeclaration(String packageName, Map<String, String> directives, Map<String, String> attributes) {
			this.packageName = packageName;
			this.directives = directives;
			this.attributes = attributes;
		}
		
		public String packageName;
		public Map<String, String> directives;
		public Map<String, String> attributes;
	}
	
	private void parseExportPackage(String value) {
		List<PackageDeclaration> pkgDecls = extractPackageDirectives(value, "Export-Package");
		
		List<ExportPackageSpec> exports = m_bd.getExportPackages();
		
		for (PackageDeclaration packageInfo : pkgDecls) {
			String pkgName = packageInfo.packageName;
			
			VersionInfo vers = null;
			List<String> uses = null;
			
			// first look at the directives associated with this package.
			String usesVal = packageInfo.directives.remove("uses");
			if (usesVal != null) {
				uses = parseUses(usesVal);
			}
			
			// TODO - do I want to do anything with these?
			/* String xinternal = */packageInfo.directives.remove("x-internal");
			/* String xfriends = */packageInfo.directives.remove("x-friends");
			
			warnUnusedDirectives(packageInfo.directives, "Export-Package");
			
			// now go through the attributes.
			String versionStr = packageInfo.attributes.remove("version");
			String specVerStr = packageInfo.attributes.remove("specification-version");
			
			if (specVerStr != null) {
				addProblem("DEPRECATED_SPECIFICATION_VERSION",
						"Deprecated: Use of 'specification-version' in Export-Package entry in bundle {0}",
						m_currentBundle);
			}
			
			if (versionStr != null) {
				vers = VersionInfo.parseVersion(versionStr);
			}
			
			if (specVerStr != null) {
				VersionInfo specVers = VersionInfo.parseVersion(specVerStr);
				if (versionStr != null && !specVers.equals(vers)) {
					addProblem("MISMATCHED_SPECIFICATION_VERSION",
							"In Export-Package header, 'specification-version' of {0} doesn't match 'version' of {1} in bundle {2}",
							specVerStr, versionStr, m_currentBundle);
				}
				vers = specVers;
			}
			
			if (vers == null) {
				addProblem(isTibcoBundle() ? "MISSING_EXPORT_VERSION" : "NON_TIBCO_MISSING_EXPORT_VERSION",
						"No version specified for exported pacakge {0} from bundle {1}",
						pkgName, m_currentBundle);
			}
			
			// look for attributes that are *not* allowed.
			if (packageInfo.attributes.remove("bundle-symbolic-name") != null) {
				addProblem("ATTRIBUTE_NOT_ALLOWED",
						"In Export-Package, attribute 'bundle-symbolic-name' not allowed in bundle {0}",
						m_currentBundle);
			}
			
			if (packageInfo.attributes.remove("bundle-version") != null) {
				addProblem("ATTRIBUTE_NOT_ALLOWED",
						"In Export-Package, attribute 'bundle-version' not allowed in bundle {0}",
						m_currentBundle);
			}
			
			// save it all.
			ExportPackageSpec eps = new ExportPackageSpec();
			eps.setPackageName(pkgName);
			eps.setVersion(vers);
			if (uses != null) {
				eps.getUses().addAll(uses);
			}
			
			eps.getAttributes().putAll(packageInfo.attributes);
			exports.add(eps);
		}
		
		// now warn on duplicate exports by sorting...
		Collections.sort(exports, new Comparator<ExportPackageSpec>() {

			public int compare(ExportPackageSpec o1, ExportPackageSpec o2) {
				return o1.getPackageName().compareTo(o2.getPackageName());
			}
		});
		
		String lastPackageName = "";
		for ( ExportPackageSpec oneEps : exports) {
			if (oneEps.getPackageName().equals(lastPackageName)) {
				lastPackageName = oneEps.getPackageName();
				addProblem("MULTIPLE_EXPORTS", "Multiple exports of package {0} in bundle {1}",
						lastPackageName, m_currentBundle);
			}
		}
	}

	/**
	 * Warn about unused directives.
	 * 
	 * @param directives	Which directives do we have?
	 * @param header		Which header are we parsing?
	 */
	private void warnUnusedDirectives(Map<String, String> directives,
			String header) {
		
		for (String directive : directives.keySet()) {
			addProblem("UNUSED_DIRECTIVE",
					"Unused directive {0} found in {1} for bundle {2}.",
					directive, header, m_currentBundle);
		}
	}

	private List<String> parseUses(String paramValue) {
		List<CharSequence> uses = splitOnChar(paramValue, ',');
		List<String> result = DataUtils.newList();
		for (CharSequence cs : uses) {
			result.add(cs.toString() );
		}
		return result;
	}

	private List<PackageDeclaration> extractPackageDirectives(
			String value, String header) {
		
		List<PackageDeclaration> results = DataUtils.newList();
		
		List<CharSequence> declaration = splitOnChar(value, ',');
		
		for (CharSequence cs : declaration) {
			List<CharSequence> packagesAndParams = splitOnChar(cs, ';');
			Map<String, String> directives = DataUtils.newMap();
			Map<String, String> attributes = DataUtils.newMap();
			
			for (CharSequence csPkg : packagesAndParams) {
				if (!containsEquals(csPkg)) {
					String pkgName = csPkg.toString();
					if (!isValidPackageName(csPkg)) {
						addProblem("BAD_PACKAGE_NAME", "Bad package name {0} found on {1} in bundle {2}",
								pkgName, header, m_currentBundle);
					}
					// note that this is forward looking, in that parameters haven't been
					// parsed yet, but the result of where they will be put is being shoved in the map.
					results.add(new PackageDeclaration(pkgName, directives, attributes));
				}
				else {
					splitParameters(csPkg, directives, attributes, header);
				}
			}
		}
		return results;
	}

	private int skipToken(CharSequence csPkg, int idx) {
		while (idx < csPkg.length() && isTokenChar(csPkg.charAt(idx))) {
			idx++;
		}
		
		return idx;
	}
	
	private int skipSymbolicName(CharSequence csPkg, int idx) {
		while (idx < csPkg.length() && ( isTokenChar(csPkg.charAt(idx)) || csPkg.charAt(idx) == '.') ) {
			idx++;
		}
		
		return idx;
	}
	

	/**
	 * Due to a bug in the PDE manifest editor, some Eclipse manifests
	 * have been created with invalid parameter names, and this function
	 * cheats that problem by simply skipping ahead to the equals sign, colon,
	 * or next space.
	 * 
	 * @param csPkg	The character sequence to scan
	 * @param idx	The index at which to start.
	 * @return	The index of the end of the string.
	 */
	private int skipParameterName(CharSequence csPkg, int idx) {
		char ch;
		boolean isValid = true;
		int start = idx;
		while (idx < csPkg.length() && (ch = csPkg.charAt(idx)) != ' ' && ch != ':' && ch != '=') {
			if (isValid && !isTokenChar(ch)) {
				isValid = false;
			}
			idx++;
		}
		
		if (!isValid) {
			// TODO - this should be captured via a formal warning mechanism.
			System.out.println("Header " + m_currentHeader + " has invalid parameter "
					+ csPkg.subSequence(start, idx) + " in bundle "
					+ m_currentBundle);
			
		}
		return idx;
	}
	
	private boolean isAlphaNum(char ch) {
		return (ch >= 'a' && ch <= 'z')
		|| (ch >= 'A' && ch <= 'Z')
		|| (ch >= '0' && ch <= '9');
	}
	
	private boolean isTokenChar(char ch) {
		return isAlphaNum(ch)
			|| (ch == '_')
			|| (ch == '-')
			|| (ch == '.');
	}

	private int skipSpaces(CharSequence csPkg, int idx) {
		while (idx < csPkg.length() && csPkg.charAt(idx) == ' ') {
			idx++;
		}
		
		return idx;
	}
	private void splitParameters(CharSequence csPkg,
			Map<String, String> directives, Map<String, String> attributes, String currentHeader) {
		
		int tokenStart = skipSpaces(csPkg, 0);
		int idx = skipParameterName(csPkg, tokenStart);
		String name = csPkg.subSequence(tokenStart, idx).toString();
		
		// this is cheating, but some places are apparently allowing spaces....
		idx = skipSpaces(csPkg, idx);
		boolean isDirective = false;
		if (csPkg.charAt(idx) == ':') {
			idx++;
			isDirective = true;
		}
		if (csPkg.charAt(idx) != '=') {
			throw new IllegalArgumentException("Equals sign not in expected location for parameter." + csPkg);
		}
		idx++;
		
		idx = skipSpaces(csPkg, idx);
		
		// now look for the parameter's value - either quoted or not.
		int valueStartIdx = idx;
		int valueEndIdx;
		if (csPkg.charAt(idx) == '"') {
			idx++;
			valueStartIdx = idx;
			while ( idx < csPkg.length() && csPkg.charAt(idx) != '"') {
				idx++;
			}
			valueEndIdx = idx;
			if (idx < csPkg.length()) {
				idx++;
			}
		}
		else {
			valueEndIdx = skipToken(csPkg, idx);
			int trailing = skipSpaces(csPkg, valueEndIdx);
			checkForCruft(csPkg, trailing);
		}
		String value = csPkg.subSequence(valueStartIdx, valueEndIdx).toString();
		
		Map<String, String> whichMap = isDirective ? directives : attributes;
		
		String currentEntry = whichMap.put(name, value);
		if (currentEntry != null) {
			addProblem("DUPLICATE_PARAMETER", "Duplicate parameter {0} for header {1} in bundle {2}",
					name, currentHeader, m_currentBundle);
		}
	}

	private void checkForCruft(CharSequence csPkg, int valueEndIdx) {
		if (valueEndIdx < csPkg.length()) {
			throw new IllegalArgumentException("Unexpected characters after parameter value: "
					+ csPkg.subSequence(valueEndIdx, csPkg.length()) );
		}
	}

	private void parseRequiredExecutionEnvironment(String value) {
		
		m_bd.getRequiredExecutionEnvironments().addAll(commaSeparatedStrIntoList(value));
	}

	private void parseBundleClassPath(String value) {
		m_bd.getBundleClassPath().addAll( commaSeparatedStrIntoList(value) );
	}

	private List<String> commaSeparatedStrIntoList(String value) {
		String[] entries = value.split(",");
		return Arrays.asList(entries);
	}

	public static BundleDescriptor parseBundleFromFile(File bundle) throws IOException, BadBundleException {
		JarFile jf = new JarFile(bundle);
		Manifest mf = jf.getManifest();
		jf.close();
		
		if (mf != null) {
			return bundleDescriptorFromManifest(bundle.toString(), mf);
		}
		else {
			return null;
		}
	}
	
	public static BundleDescriptor parseManifestFromStream(String source, InputStream is) throws IOException {
		
		Manifest mf = new Manifest(is);
		is.close();
		
		return bundleDescriptorFromManifest(source, mf);
	}

	private static BundleDescriptor bundleDescriptorFromManifest(String source, Manifest mf) {
		ManifestParser mp = new ManifestParser();
		mp.parse(source, mf);
		return mp.m_bd;
	}
	
	/**
	 * Parses a "Require-Bundle" manifest header.
	 * @param value
	 */
	private void parseRequireBundle(String value) {
		
		List<CharSequence> items = splitOnChar(value, ',');
		
		for (CharSequence oneItem : items) {
			List<CharSequence> reqEntries = splitOnChar(oneItem, ';');
			
			String bundleName = reqEntries.get(0).toString();
			int end = skipSymbolicName(bundleName, 0);
			if (end != bundleName.length() ) {
				throw new IllegalArgumentException("symbolic name " + bundleName + " has invalid characters.");
			}
			
			reqEntries.remove(0);
			Map<String, String> directives = DataUtils.newMap();
			Map<String, String> attributes = DataUtils.newMap();
			paramsIntoMap(reqEntries, directives, attributes, "Require-Bundle");

			Range<VersionInfo> range = null;
			boolean isMandatory = true;
			boolean isReexport = false;
			
			String visibility = directives.remove("visibility");
			if (visibility != null) {
				if (visibility.equals("reexport")) {
					isReexport = true;
				}
				else if (visibility.equals("private")) {
					isReexport = false;
				}
				else {
					addProblem("UNRECOGNIZED_REQUIRE_BUNDLE_VISIBILITY",
							"Unrecognized visibility value of \"{0}\" on Require-Bundle statement, in bundle {1}",
							visibility, m_currentBundle);
				}
			}
			
			String resolution = directives.remove("resolution");
			if (resolution != null) {
				if (resolution.equals("optional")) {
					isMandatory = false;
				}
				else if (!resolution.equals("mandatory")) {
					addProblem("UNRECOGNIZED_RESOLUTION_VALUE",
							"Require-Bundle resolution value of {0} is unrecognized in bundle {1}",
							resolution, m_currentBundle);
				}
			}
			
			// now warn on any remaining directives.
			warnUnusedDirectives(directives, "Require-Bundle");
			
			String badVersion = attributes.remove("version");
			if (badVersion != null) {
				addProblem("UNINTENDED_VERSION_ATTRIBUTE_ON_REQUIRE_BUNDLE",
						"Unintended \"version\" attribute found on Require-Bundle for {0}",
						m_currentBundle);
			}
			
			String bundleVersion = attributes.remove("bundle-version");
			if (bundleVersion != null) {
				range = parseRange(bundleVersion);
			}
			else {
				if (isTibcoBundle()) {
					addProblem("MISSING_BUNDLE_RANGE",
							"No range specified in Require-Bundle reference to {0} as used in bundle {1}",
							bundleName, m_currentBundle);
				}
				else {
					addProblem("NON_TIBCO_MISSING_BUNDLE_RANGE",
							"No range specified in Require-Bundle reference to {0} as used in a non-TIBCO bundle {1}",
							bundleName, m_currentBundle);
				}
			}
			
			RequireBundleSpec rbs = new RequireBundleSpec(bundleName, range, isMandatory, isReexport);

			rbs.getAttributes().putAll(attributes);
			m_bd.getRequireBundles().add(rbs);
		}
		
		// TODO - should go back through RequireBundleSpec entries and produce warnings for duplicate
		// require statements.
	}

	/**
	 * Used to identify bundles provided by TIBCO...
	 */
	private boolean isTibcoBundle() {
		return m_bd.getTarget().getTargetId().contains("com.tibco");
	}
	
	private void parseSymbolicName(String symbolicNameEntry) {
		
		List<CharSequence> items = splitOnChar(symbolicNameEntry, ';');
		
		m_symbolicName = items.get(0).toString();
		int end = skipSymbolicName(m_symbolicName, 0);
		if (end != m_symbolicName.length()) {
			throw new IllegalArgumentException("Unrecognized symbolic-name of " + m_symbolicName);
		}
		
		items.remove(0);
		Map<String, String> directives = DataUtils.newMap();
		Map<String, String> attributes = DataUtils.newMap();
		paramsIntoMap(items, directives, attributes, "Require-Bundle");
		
		String singleton = directives.remove("singleton");
		if (singleton != null) {
			if (singleton.equals("true")) {
				m_bd.setIsSingleton(true);
			}
			else if (singleton.equals("false")) {
				m_bd.setIsSingleton(false);
			}
			else {
				addProblem("UNRECOGNIZED_SINGLETON_VALUE",
						"Unrecognized singleton value of \"{0}\" in bundle {1}",
						singleton, m_currentBundle);
			}
		}
		
		warnUnusedDirectives(directives, "Bundle-SymbolicName");
		
		//TODO - warn on attributes.
	}

	private String m_currentBundle;
	
	private String m_currentHeader;
	
	private String m_symbolicName;
	
	private BundleDescriptor m_bd = new BundleDescriptor();

	/**
	 * Quick filter to eliminate all of the non-feature files in a folder -
	 * at least based on file name.
	 */
	public static final FileFilter sm_filterForBundleJars = new FileFilter() {
	
		public boolean accept(File pathname) {
			
			String name = pathname.getName();
		    if (name.startsWith(".") || name.startsWith("_")) {
		        return false;
		    }
		    
		    if (pathname.isDirectory()) {
		    	return false;
		    }
		    else {
		    	// must be a JAR file.
		    	return name.endsWith(".jar");
		    }
		}
		
	};
	
	/**
	 * Quick filter to eliminate all of the non-feature files in a folder -
	 * at least based on file name.
	 */
	public static final FileFilter sm_filterPluginDirsAndJars = new FileFilter() {
	
		public boolean accept(File pathname) {
			
			String name = pathname.getName();
		    if (name.startsWith(".") || name.startsWith("_")) {
		        return false;
		    }
		    
		    if (pathname.isDirectory()) {
		    	// must have a META-INF/MANIFEST.MF file
		    	File manifestFile = new File(pathname, "META-INF/MANIFEST.MF");
		    	return manifestFile.isFile();
		    }
		    else {
		    	// must be a JAR file.
		    	return name.endsWith(".jar");
		    }
		}
		
	};
	
	private static class BundleDescriptorIterator extends NoRemoveIterator<BundleDescriptor> {

		public BundleDescriptorIterator(File pluginsDir) {
			m_possibles = pluginsDir.listFiles(sm_filterPluginDirsAndJars);
			m_index = 0;
		}

		public boolean hasNext() {
			// TODO Auto-generated method stub
			return m_index < m_possibles.length;
		}

		public BundleDescriptor next() {
			BundleDescriptor result = null;
			File toParse = m_possibles[m_index++];
			try {
				if (toParse.isDirectory()) {
					File manifestFile = new File(toParse, "META-INF/MANIFEST.MF");
					if (manifestFile.isFile()) {
						FileInputStream fis = new FileInputStream(manifestFile);
						
						result = parseManifestFromStream(manifestFile.toString(), fis);
					}
				}
				else {
					// strictly speaking, bundles don't have to end with .jar, but it is probably a safe check.
					result = parseBundleFromFile(toParse);
				}
			} catch (Exception e) {
				throw new IllegalArgumentException("Failure on bundle: " + toParse.toString(), e);
			}
			return result;
		}
		
		private File[] m_possibles;
		private int m_index;
	};
	
	private static Pattern sm_rangeExpr = Pattern.compile("^(\\[|\\()([^,]*)\\,\\s*([^\\]\\)]*)(\\)|\\])$");

	private static Set<String> sm_ignoredHeaders = new HashSet<String>( Arrays.asList( new String[] {
		// OSGi defined headers not currently processed.
		"bundle-contactaddress", "bundle-version", "bundle-copyright", "bundle-description", "bundle-docurl", "bundle-license",
		"bundle-localization", "bundle-name", "bundle-nativecode", "bundle-vendor", "dynamic-importpackage",
		"export-service", "fragment-host", "import-service", 
		
		// TODO - Likely erroneous entries
		"dynamicimport-package",
		
		// Eclipse defined manifest entries.
		"eclipse-autostart", "eclipse-buddypolicy", "eclipse-bundleshape", "eclipse-extensibleapi",
		"eclipse-jrebundle",
		"eclipse-lazystart", "eclipse-patchfragment", "eclipse-platformfilter", "eclipse-registerbuddy",
		"eclipse-sourcebundle", "eclipse-systembundle",

		// Java defined entries.
		"class-path", "implementation-title", "implementation-vendor", "implementation-vendor-id",
		"implementation-version",
		"main-class", "specification-title", "specification-vendor", "specification-version",
		
		// miscellaneous items.
		"ant-version", "archiver-version", "art", "bnd-lastmodified", "build-jdk", "built-by", "comment-dynamicimport",
		"comment-header",
		"created-by", "extension-name", "hk2-bundle-name", "ignore-package", "lazy-manifestfilter",
		"mode", "name", "package", "premain-class", "service-component", "tool", "url",
		"x-compile-source-jdk", "x-compile-target-jdk", 
		
		// unclassified.
		"jpa-initializer", "import-package-comment", "private-package",
		"spring-version", "provide-package", "plugin-class", "spring-dm-version",
		"embed-directory", "embed-dependency", "embed-stripgroup",
		"embed-transitive", "jetty-version", "provided-services",
		"can-redefine-classes", "content-id", "prvinfo-providers",
		"prv-storage", "disabled-eclipse-platformfilter", "application", "plugin-version",
		"plugin-displayname", "packagenamespace", "plugin-name", "plugin-description",
		"plugin-contextroot", "com-tibco-neo-model-bundle-symbolicname-aliases",
		"vendor", "rmanifest-version", "tibco-homedaemonlet", "shared-resources",
		"admin-config-plugin-id"
	}));
	
	/**
	 * Generic problem statement...
	 */
	private static class GenericProblem extends Problem {
		
		public GenericProblem(String identifier, String format, Object... parameters) {
			super(identifier);
			
			m_format = format;
			m_parameters = parameters;
		}
		
		@Override
		public String toString() {
			if (m_message == null) {
				m_message = MessageFormat.format(m_format, m_parameters); 
			}
			return m_message; 
		}

		private String m_message;
		
		private String m_format; 
		
		private Object[] m_parameters;
	}
	
	private static final VersionInfo ZERO_VERSION = new VersionInfo(0, 0, 0);
}
