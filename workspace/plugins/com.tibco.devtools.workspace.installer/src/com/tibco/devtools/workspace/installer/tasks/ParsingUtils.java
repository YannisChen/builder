package com.tibco.devtools.workspace.installer.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Captures utility methods for parsing various different formats...
 */
public class ParsingUtils {

	/**
	 * Reads text from a reader, treating each line as a constraint or comment,
	 * and turns it into a target constraint with the passed source.
	 * 
	 * @param <S>	The type of the source object.
	 * @param source	The "source" of the target constraints being generated.
	 * @param rdr		The source of the text to parse.
	 * 
	 * @return List of constraints, never null, but possibly empty.
	 * @throws IOException Thrown if something goes wrong parsing the original data.
	 */
	public static <S> List<TargetConstraint<VersionInfo, S>>
	readerIntoConstraintsList(S source, Reader rdr) throws IOException {
		
		List<TargetConstraint<VersionInfo, S>> result = DataUtils.newList();
		BufferedReader br = new BufferedReader(rdr);
		
		String line;
		while ( (line = br.readLine()) != null ) {
			Matcher blankMatcher = sm_lineToSkip.matcher(line);
			if (!blankMatcher.matches()) {
				result.add( constraintFromString(source, line) );
			}
		}
		
		return result;
	}
	
	/**
	 * Turns a line with a feature name, some amount of space separation, and a version range into
	 * a TargetConstraint.
	 * 
	 * @param <S>	The type of the source of the constraint.
	 * @param source	What "owns" this constraint?
	 * @param line		What is the line we need to parse?
	 * 
	 * @return	A generated constraint.
	 */
	public static <S> TargetConstraint<VersionInfo, S> constraintFromString(
			S source, String line) {
		Matcher matcher = sm_versionTarget.matcher(line);
		if (!matcher.find() )
			throw new IllegalStateException("Unrecognized versioned target string " + line);
		
		String feature = matcher.group(1);
		boolean lowInclusive = matcher.group(2).equals("[");
		VersionInfo lowVers = VersionInfo.parseVersion( matcher.group(3) );
		VersionInfo highVers = VersionInfo.parseVersion( matcher.group(4) );
		boolean highInclusive = matcher.group(5).equals("]");
	
		Range<VersionInfo> range = new Range<VersionInfo>(lowVers, lowInclusive, highVers, highInclusive);
		TargetConstraint<VersionInfo, S> newConstraint = new TargetConstraint<VersionInfo, S>(source, true, feature, range);
		return newConstraint;
	}

	private static Pattern sm_versionTarget = Pattern.compile("([^\\s]*)\\s+(\\[|\\()([^,]*),([^\\]\\)]*)(\\)|\\])");

	// we ignore any line that has zero or more spaces, followed, optionally by a hash
	// mark ("#"), then any number of characters, and then the end of the line.
	private static Pattern sm_lineToSkip = Pattern.compile("\\s*(#.*)?");
}
