package com.tibco.devtools.workspace.installer.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.tibco.devtools.workspace.model.VersionInfo;

/**
 * Captures a bunch of utility functions for creating and launching an Eclipse configuration
 * from outside of Eclipse.
 *
 */
public class EclipseUtils {

    private static final String WI_MOCK_PLATFORM_TXT = "wi-mock-platform.txt";

    /**
     * Create a default settings file in the given configuration folder that captures the indicated workspace
     * as a recent one.
     *
     * @param workspaceLocation The location of the workspace to be used as a "recent" setting.
     * @param eclipseConfigLoc  The eclipse configuration directory.
     * @throws IOException
     */
    public static void createDefaultSettings(File workspaceLocation, File eclipseConfigLoc) throws IOException {

        File settingsDir = new File( eclipseConfigLoc, ".settings");
        FileUtils.createDirectories(settingsDir);

        File ideSettings = new File( settingsDir, "org.eclipse.ui.ide.prefs");

        StringBuffer bufSettings = new StringBuffer();
        bufSettings.append("RECENT_WORKSPACES=");
        // this is a Java properties file, so Windows backslashes have to be converted to forward slashes
        String workspaceLoc = workspaceLocation.toString();
        workspaceLoc = workspaceLoc.replace('\\', '/');
        bufSettings.append( workspaceLoc );
        bufSettings.append("\n");
        // with Eclipse 3.3.2 and later, the following seems to be necessary to play nice - they changed the "protocol" to 3,
        // which means it now must be specified.
        bufSettings.append("RECENT_WORKSPACES_PROTOCOL=2\neclipse.preferences.version=1\n");

        FileUtils.createFileWithContents(ideSettings, bufSettings.toString(), Charset.defaultCharset() );
    }

    /**
     * Creates a "platform.xml" file that captures the custom extension location.
     *
     * @param destConfig    The configuration folder that gets the platform.xml file.
     * @param destEclipseLoc    The extension location that is used.
     * @param isP2Enabled Is this platform.xml for a p2 enabled Eclipse?
     *
     * @throws IOException    If any problems occur creating the file.
     */
    public static void createPlatformXml(File destConfig, File destEclipseLoc, File toolsLoc, boolean isP2Enabled) throws IOException {

        File destOrgEclipseUpdate = new File(destConfig, "org.eclipse.update");
        FileUtils.createDirectories(destOrgEclipseUpdate);

        File platformXml = new File(destOrgEclipseUpdate, "platform.xml");

        FileOutputStream fos = new FileOutputStream(platformXml);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter bw = new BufferedWriter(osw);

        // write the start of the platform configuration.
        bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        bw.write("<config date=\"0\" transient=\"false\" version=\"3.0\">\n");

        // write the default site.
        bw.write("<site enabled=\"true\" policy=\"USER-EXCLUDE\" updateable=\"true\" url=\"platform:/base/\">\n");
        bw.write("</site>\n");

        if (!isP2Enabled) {
            if (destEclipseLoc != null)
            {
                // write our new custom extension location, with the string form of the File URI.
                String extensionLocStr = getEclipseUrlFromFile(destEclipseLoc);
                bw.write("<site enabled=\"true\" policy=\"USER-EXCLUDE\" updateable=\"true\" url=\"" + extensionLocStr + "\">\n");
                bw.write("</site>\n");
            }

            if (toolsLoc != null)
            {
                String toolsLocStr = getEclipseUrlFromFile(toolsLoc);
                bw.write("<site enabled=\"true\" policy=\"USER-EXCLUDE\" updateable=\"true\" url=\"" + toolsLocStr + "\">\n");
                bw.write("</site>\n");
            }
        }
        else if (destEclipseLoc != null || toolsLoc != null) {
            // write out a cookie crumb to be used later when we launch Eclipse....
            File wiMockPlatform = new File(destOrgEclipseUpdate, WI_MOCK_PLATFORM_TXT);
            Writer wiMockPlatformWriter = new FileWriter(wiMockPlatform);

            if (destEclipseLoc != null)
                wiMockPlatformWriter.write(destEclipseLoc.toString() + "\n");

            if (toolsLoc != null)
                wiMockPlatformWriter.write(toolsLoc.toString() + "\n");

            wiMockPlatformWriter.close();
        }

        bw.write("</config>\n");

        bw.close();
    }

	/**
	 * Eclipse has a very specific notion of how it wants its URLs when then actually
	 * point at files, and it happens that that approach uses a deprecated method,
	 * herein captured in a method so that we can suppress warnings.
	 * 
	 * @param destEclipseLoc	File for which we want a URL.
	 * @return	The String representation of a URL.
	 * @throws MalformedURLException
	 */
	@SuppressWarnings("deprecation")
	private static String getEclipseUrlFromFile(File destEclipseLoc)
			throws MalformedURLException {
		return destEclipseLoc.toURL().toString();
	}

    /**
     * Copy plugin contents to plugin folder.
     *
     * <p>Note that when debugging within Eclipse we don't get a JAR file - just
     * a folder....</p>
     *
     * @param pluginFolder The destination plugin folder.
     * @throws IOException If something should go awry while reading or writing.
     */
    public static void copySelfToPluginFolder(File pluginFolder) throws IOException {

        URL resourceUrl = EclipseUtils.class.getResource("/MarkerFile.txt");
        String protocol = resourceUrl.getProtocol();
        if (protocol.equalsIgnoreCase("jar") ) {
            JarURLConnection jarUrlConn = (JarURLConnection) resourceUrl.openConnection();
            URL jarUrl = jarUrlConn.getJarFileURL();
            File jarFile = new File( jarUrl.getFile() );
            String jarFileName = jarFile.getName();

            File myDestPlugin = new File(pluginFolder, jarFileName);
            InputStream jarStream = jarUrl.openStream();
            FileUtils.copyStreamToFile(jarStream, myDestPlugin);

        }
        else if (protocol.equalsIgnoreCase("file")) {
            // this is a file location, so perhaps a run from Eclipse IDE...
            URI resUri;
            try {
                resUri = resourceUrl.toURI();
            } catch (URISyntaxException e) {
                System.out.println("Unable to convert URL to URI:" + resourceUrl.toString());
                throw new IllegalStateException("Error in URL syntax.");
            }
            File manifestFile = new File(resUri);
            File classesFolder = manifestFile.getParentFile();

            File destPluginFolder = new File(pluginFolder, "com.tibco.devtools.workspace.installer_1.0.0");
            if (destPluginFolder.exists()) {
                FileUtils.deleteFolder(destPluginFolder);
            }
            FileUtils.copyFolder(classesFolder, destPluginFolder);
        }
    }

    /**
     * Given a destination extension location - existing or not, create an "eclipse" folder
     * within it, and return that location.
     *
     * @param destExtensionLoc    The "extension location".
     * @param copySelf            Copy the workspace installer into the target folder.
     * @return The File object representing the "eclipse" folder inside of the extension location.
     *
     * @throws IOException
     */
    public static void createEclipseFolder(File destEclipseFolder, boolean copySelf) throws IOException {

        // The features and plugins folders.
        File destFeaturesFolder = new File(destEclipseFolder, "features");
        File destPluginsFolder = new File(destEclipseFolder, "plugins");
        FileUtils.createDirectories(destFeaturesFolder);
        FileUtils.createDirectories(destPluginsFolder);

        // Create the .eclipseextension file with dummy text.
        File markerFile = new File(destEclipseFolder, ".eclipseextension");
        if (!markerFile.isFile()) {
            String dummyContents = "This is a marker file that Eclipse requires in an extension folder.";
            FileWriter fw = new FileWriter(markerFile);
            try {
				fw.write(dummyContents);
			}
            finally {
				fw.close();
			}
        }

        // Now copy my plugin JAR into the plugins folder...
        if (copySelf) {
            copySelfToPluginFolder(destPluginsFolder);
        }
    }

    public static final String CONFIG_FOLDER_NAME = "configuration";

    public static void createConfigFolder( File baseEclipseLocation, File destConfig,
            File destEclipseExtLoc, File workspaceLocation, File toolsLocation) throws IOException {

        File baseConfig = new File(baseEclipseLocation, CONFIG_FOLDER_NAME);

        // make all the intervening directories.
        FileUtils.createDirectories(destConfig);

        // copy over config.ini
        File configIniSrc = new File(baseConfig, "config.ini");
        File configIniDest = new File(destConfig, "config.ini");

        FileUtils.copyFile(configIniSrc, configIniDest);

        boolean isP2Enabled = isP2EnabledEclipseFolder(baseEclipseLocation);

        if (isP2Enabled) {
        	rewriteConfigIniOsgiFramework(baseEclipseLocation, configIniDest);
        }
        boolean copyWorkspacePlugin = (toolsLocation == null);
        if (destEclipseExtLoc != null) {
            createEclipseFolder( destEclipseExtLoc, copyWorkspacePlugin);
        }
        createPlatformXml(destConfig, destEclipseExtLoc, toolsLocation, isP2Enabled);

        createDefaultSettings( workspaceLocation, destConfig );

    }

    /**
     * These crazy machinations are to fix a VERY subtle bug on the Mac - when trying
     * to figure out where the location of the framework is, the Mac guesses poorly.
     * 
     * <p>To get around that issue, we simply hard-code the location of the framework
     * JAR.</p>
     * 
     * @param baseEclipseLocation Where is the framework JAR coming from?
     * @param configIniDest	What config file are we editing?
     * @throws IOException
     */
    private static void rewriteConfigIniOsgiFramework(File baseEclipseLocation, File configIniDest) throws IOException {
    	
    	// first, read the file.
    	FileInputStream fis = new FileInputStream(configIniDest);
    	Properties props;
    	try {
			props = new Properties();
			props.load(fis);
		}
    	finally {
    		fis.close();
		}
    	
    	// get the property in question, and get the file name portion of it.
    	String frameworkStr = props.getProperty("osgi.framework");
    	URL frameworkUrl = new URL(frameworkStr);
    	String asFile = frameworkUrl.getFile();
    	File loc = new File(asFile);
    	String pluginName = loc.getName();
    	
    	// compute a new location.
    	File pluginsFolder = new File(baseEclipseLocation, "plugins");
    	File targetJar = new File(pluginsFolder, pluginName);
    	
    	// convert back to a URL and put it in the file.
    	targetJar = targetJar.getAbsoluteFile();
    	frameworkStr = getEclipseUrlFromFile(targetJar);
    	props.setProperty("osgi.framework", frameworkStr);
    	
    	// now write the file back.
    	FileOutputStream fos = new FileOutputStream(configIniDest);
    	try {
			props.store(fos, "Regenerated by workspace installer.");
		}
    	finally {
        	fos.close();
		}
	}

	public static void createOsShortcut(String projectName, File eclipseLocation,
            File configurationLocation, File shortcutFolder) throws IOException {

        File iconPath = getEclipseIcon(eclipseLocation, "eclipse48.png");
        StringBuffer fileNameBuf = new StringBuffer("Eclipse for ");
        fileNameBuf.append(projectName);

        // what Operating system is this?
        String systemId = System.getProperty("os.name").toLowerCase();
        if (systemId.indexOf("linux") >= 0) {
            // Linux, create a .desktop file.
            // see: http://standards.freedesktop.org/desktop-entry-spec/latest/
            StringBuffer result = new StringBuffer();
            result.append("[Desktop Entry]\n");
            result.append("Comment=Generated Shortcut for Eclipse installation.\n");
            result.append("Exec='");
            File eclipsePath = new File(eclipseLocation, "eclipse");
            result.append(eclipsePath.toString() );
            result.append("' -configuration ");
            result.append(configurationLocation.toString() );
            result.append("\n");
            result.append("GenericName=Java IDE\n");
            result.append("Icon=");
            result.append( iconPath.toString() );
            result.append("\n");
            result.append("Name=Eclipse Configuration for ");
            result.append( projectName );
            result.append("\n");
            result.append("Path=");
            result.append(shortcutFolder);
            result.append("\n");
            result.append("StartupNotify=true\n");
            result.append("Terminal=false\n");
            result.append("Type=Application\n");

            String contents = result.toString();

            fileNameBuf.append(".desktop");

            String fileName = fileNameBuf.toString();
            File shortcutFile = new File(shortcutFolder, fileName);
            FileUtils.createFileWithContents(shortcutFile, contents, Charset.defaultCharset() );
        }
        else if (systemId.indexOf("windows") >= 0) {

            fileNameBuf.append(".lnk");
            String fileName = fileNameBuf.toString();
            File shortcutFile = new File(shortcutFolder, fileName);

            // go ahead and create a VB script, and then run it.
            StringBuffer jsScript = new StringBuffer();
            jsScript.append("var oWS = WScript.CreateObject(\"WScript.Shell\");\r\n");
            jsScript.append("var sLinkFile = \"");
            appendPathWithDoubleBackslash(jsScript, shortcutFile.toString() );
            jsScript.append("\";\r\n");
            jsScript.append("var oLink = oWS.CreateShortcut(sLinkFile);\r\n");
            jsScript.append("oLink.TargetPath = \"");
            File eclipsePath = new File( eclipseLocation, "eclipse.exe");
            appendPathWithDoubleBackslash(jsScript, eclipsePath.toString());
            jsScript.append("\";\r\n");
            jsScript.append("oLink.Arguments = \"-configuration \\\"");
            appendPathWithDoubleBackslash(jsScript, configurationLocation.toString());
            jsScript.append("\\\"\";\r\n");

            jsScript.append("oLink.Description = \"Eclipse configuration for ");
            jsScript.append( projectName );
            jsScript.append("\";\r\n");

            //   '    oLink.HotKey = "ALT+CTRL+F"
            //   '    oLink.IconLocation = "C:\Program Files\MyApp\MyProgram.EXE, 2"
            //   '    oLink.WindowStyle = "1"

            // set the working directory - required for Eclipse 3.4.
            jsScript.append("oLink.WorkingDirectory = \"");
            appendPathWithDoubleBackslash(jsScript, shortcutFolder.toString());
            jsScript.append("\";\r\n");

            jsScript.append("oLink.Save();\r\n");

            String jsContents = jsScript.toString();
            File jsScriptFile = new File(shortcutFolder, "shelllinkscript.js");
            FileUtils.createFileWithContents(jsScriptFile, jsContents, Charset.defaultCharset() );

            // OK, now we've created the JScript, need to execute it.
            List<String> params = new ArrayList<String>();
            params.add("cscript.exe");
            params.add("\"" + jsScriptFile.toString() + "\"");

            ProcessBuilder shortcutProcessBuilder = new ProcessBuilder(params);
            Process shortcutProcess = shortcutProcessBuilder.start();
            try {
                shortcutProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else if (systemId.indexOf("mac") >=0) {
            String tmpFileName = fileNameBuf.toString() + ".tmp";
            String appFileName = fileNameBuf.toString() + ".app";
            File scriptFile = new File(shortcutFolder, tmpFileName);
            File appFile = new File(shortcutFolder, appFileName);

            StringBuffer osaScript = new StringBuffer();
            osaScript.append("on run\n");

            // the shell script command and its arguments *must be on one line*
            File eclipsePath = new File(eclipseLocation, "eclipse");
            osaScript.append("   do shell script \"cd ").append(shortcutFolder.toString());
            osaScript.append(" && ").append(eclipsePath.toString());
            osaScript.append(" -configuration ");
            osaScript.append(configurationLocation.toString());
            osaScript.append("\"\n");

            osaScript.append("end run\n");
            String osaContents = osaScript.toString();

            FileUtils.createFileWithContents(scriptFile, osaContents, Charset.defaultCharset());

            // osacompile -o appFile scriptFile
            List<String> params = new ArrayList<String>();
            params.add("osacompile");
            params.add("-o");
            params.add(appFile.toString());
            params.add(scriptFile.toString());

            ProcessBuilder shortcutProcessBuilder = new ProcessBuilder(params);
            Process shortcutProcess = shortcutProcessBuilder.start();
            try {
                shortcutProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // now delete the temporary file
            FileUtils.blindlyDelete(scriptFile);
        }
    }

    /**
     * Get the icon for the Eclipse application.
     *
     * <p>Starting with Eclipse 3.4, there is no longer an icon at top level - instead we
     * must dig and find the platform or SDK feature.</p>
     *
     * @param eclipseLocation    The location of a base Eclipse install.
     *
     * @return
     */
    private static File getEclipseIcon(File eclipseLocation, String fileName) {

        File result = null;
        File plugins = new File(eclipseLocation, "plugins");

        String folderNames[] = plugins.list();

        for (String oneName : folderNames) {
            File oneFolder = new File(plugins, oneName);
            if (oneFolder.isDirectory()) {
                File iconFile = new File(oneFolder, fileName);
                if (iconFile.isFile() ) {
                    result = iconFile;
                    break;
                }
            }
        }

        return result;
    }


    /**
     * Guesses whether the target folder is a P2 enabled Eclipse folder based on
     * what is found in that target folder.
     *
     * @param eclipseAppFolder    The folder to check
     * @return    <code>true</code> if we find a dropins folder.
     */
    public static boolean isP2EnabledEclipseFolder(File eclipseAppFolder) {

        File dropinsFolder = new File(eclipseAppFolder, "dropins");

        return dropinsFolder.isDirectory();
    }

    /**
     * Do the nitpick work of launching eclipse to run the workspace installer.
     *
     * @param eclipseLoc        The location for Eclipse.
     * @param isDebug           Is this a "debug" launch - enables remote debugging.
     * @param workspaceLocation What workspace should be used?
     * @param configurationLocation What configuration to use?
     * @param applicationName     What eclipse application do I wish to launch?
     * @param args              Arguments to the target application.
     * @param workingDir        What is the working directory for launching Eclipse?
     *     With Eclipse 3.4, we will see a "p2" folder created here, in addition to an "artifacts.xml" file.
     */
    public static int launchEclipseWithArgs(File eclipseLoc, boolean isDebug,
            File workspaceLocation, File configurationLocation, String applicationName,
            List<String> args, File workingDir) throws IOException, InterruptedException {

        // if this is a p2 enabled Eclipse, then we want to copy all of the plugins from the extension
        // locations that we build into the "dropins" folder.
        boolean isP2Enabled = isP2EnabledEclipseFolder(eclipseLoc);
        if (isP2Enabled) {
            copyExtensionsToDropins(configurationLocation, workingDir);
        }

        System.out.println("Using Eclipse installed at " + eclipseLoc);
        List<String> params = new ArrayList<String>();
        
        String javaHomeStr = System.getProperty("java.home");
        System.out.println("Java home is " + javaHomeStr);
        File javaHome = new File(javaHomeStr);
        File javaExe = new File(javaHome, "bin" + File.separator + "java");

        File startupJar = findStartupJar(eclipseLoc);

        params.add(javaExe.toString());
        if (isDebug) {
            params.add("-Xdebug -Xnoagent -Djava.compiler=none");
            params.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000");
        }
        params.add("-Xmx256m");
        params.add("-jar");
        params.add(startupJar.toString());
        
        if (isDebug) {
            params.add("-debug");
            params.add("-consoleLog");
        }
        params.add("-data");
        params.add(workspaceLocation.toString() );
        params.add("-configuration");
        params.add(configurationLocation.toString() );
        params.add("-application");
        params.add(applicationName);

        // passing what the application should do is very simple - just pass the config file.
        params.addAll( args );

        ProcessBuilder eclipseProcessBuilder = new ProcessBuilder(params);
        eclipseProcessBuilder.directory(workingDir);

        int result = executeProcessAndCaptureOutput(eclipseProcessBuilder);

        return result;

    }

    /**
     * Copy the extensions folders from the configuration into the "dropins" folder.
     * @param configurationLocation
     * @param workingDir
     * @throws IOException
     */
    private static void copyExtensionsToDropins(File configurationLocation,
            File workingDir) throws IOException {

        // I'm totally cheating here - if I knew when I was writing the configuration that this
        // was targeting Eclipse 3.4, then I read the "mock" platform.xml.
        File destOrgEclipseUpdate = new File(configurationLocation, "org.eclipse.update/" + WI_MOCK_PLATFORM_TXT);

        BufferedReader bufReader = new BufferedReader(new FileReader(destOrgEclipseUpdate) );

        try {
            File dropinsFolder = new File(workingDir, "dropins");

            // first empty out the folder.
            FileUtils.deleteFolder(dropinsFolder);
            FileUtils.createDirectories(dropinsFolder);

            // get the places that extensions have been written to.
            String line;
            while ( (line = bufReader.readLine()) != null) {
                if (line.length() > 0) {
                    File oneLoc = new File(line);
                    File pluginsFolder = new File(oneLoc, "plugins");
                    FileUtils.copyFolderContentsToFolder(pluginsFolder, dropinsFolder);
                }
            }
            
        }
        finally {
        	bufReader.close();
        }
    }

    /**
     * Execute a process and capture what output it may have.
     *
     * @param processBuilder    The builder for the process to be run.
     * @return    The exit code for the process
     * @throws IOException    Thrown if there is some difficulty capturing output.
     * @throws InterruptedException    Thrown if the process is interrupted.
     */
    public static int executeProcessAndCaptureOutput(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        // redirect error stream so that our output processing captures both.
        processBuilder.redirectErrorStream(true);
        Process eclipseProcess = processBuilder.start();

        // read the "input" aka output of the launched process.
        InputStream stream = eclipseProcess.getInputStream();
        BufferedReader br = new BufferedReader( new InputStreamReader(stream) );
        String line;
        while ( (line = br.readLine() ) != null) {
            System.out.println( line );
        }
        br.close();
        int result = eclipseProcess.waitFor();
        System.out.println("The process returned status " + result);
        return result;
    }

    /**
     * Find the correct jar to invoke in order to start eclipse.
     * Returns startup.jar if it exists, and otherwise finds the launcher in the
     * plugins directory.
     *
     * This method assumes that the caller has: verified the existence of the
     * baseLocation parameter, verified that it is a directory, and verified that
     * it contains (at least) a subdirectory named "plugins".  It does no checking
     * of these preconditions.
     *
     * @param baseLocation the Eclipse base location (containing plugins and features
     * directories, and in 3.2 and earlier, startup.jar), which <em>should</em> be
     * a canonical path.
     * @return the path to the requisite jar, selecting the highest version if multiple
     * launchers are available, or null if no launcher can be found; the returned File
     * will be relative if the baseLocation is relative.
     * @throw IOException if any preconditions are not satisfied, or other file-related
     * nastiness occurs.
     */
    public static File findStartupJar(File baseLocation) throws IOException {
        File candidate = new File(baseLocation, "startup.jar");
        if (candidate.exists() && candidate.isFile()) {
            return candidate;
        } else {
            candidate = null;
            File [] candidates = new File(baseLocation, "plugins").listFiles(new java.io.FileFilter() {
                public boolean accept(File file) {
                    String filename = file.toString();
                    if (filename.indexOf("org.eclipse.equinox.launcher_") >= 0) {
                        if (!filename.endsWith(".jar")) {
                            return false;
                        }
                        return true;
                    }
                    return false;
                }
            });
            if (candidates.length == 1) {
                return candidates[0];
            }
            VersionInfo bestVersion = null;
            for (File file : candidates) {
                String filename = file.toString();
                VersionInfo thisVersion = VersionInfo.parseVersion(filename.substring(filename.lastIndexOf('_') + 1, filename.lastIndexOf('.')));
                if (bestVersion == null) {
                    candidate = file;
                    bestVersion = thisVersion;
                } else {
                    if (thisVersion.compareTo(bestVersion) > 0) { // if multiple, take the (random) first?
                        candidate = file;
                        bestVersion = thisVersion;
                    }
                }
            }
        }
        return candidate;
    }

    private static void appendPathWithDoubleBackslash(StringBuffer strResult, String toAppend) {

        // OK, so this looks weird, but the first is creating the RE \\
        // and the second is replacing that RE matching a single "\" with
        // a literal value of two slashes.
        toAppend = toAppend.replace("\\", "\\\\");
        strResult.append(toAppend);
    }

}
