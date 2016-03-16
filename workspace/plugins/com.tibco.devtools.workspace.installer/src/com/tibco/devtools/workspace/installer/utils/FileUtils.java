package com.tibco.devtools.workspace.installer.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class FileUtils {

	/**
	 * Create all the directories up to and including the passed file.
	 *
	 * @param dirLoc	The directory location to create.
	 * @throws IOException If unable to create the target directory.
	 */
	public static void createDirectories(File dirLoc) throws IOException {
		if (!dirLoc.isDirectory() ) {
			boolean success = dirLoc.mkdirs();
			if (!success) {
				throw new IOException("Unable to create directory" + dirLoc.getAbsolutePath());
			}
		}
	}

	/**
	 * Delete a file either via Subversion or direct file system calls,
	 * depending on what is appropriate.
	 * 
	 * <p>TODO - this assumes, incorrectly, that just because a folder has a .svn
	 * folder, the particular file in queston is tracked by Subversion.  This is
	 * clearly not required, and could be misleading.</p>
	 * 
	 * @param toDelete	The file to be deleted.
	 * @return true on success
	 */
	public static boolean deleteViaFileSystemOrSubverion(File toDelete) {
		if (!toDelete.isFile()) {
			throw new IllegalStateException("Path " + toDelete.toString() + " is not a file and cannot be deleted.");
		}
		File parentDir = toDelete.getParentFile();
		File svnIndicator = new File(parentDir, ".svn");
		if (svnIndicator.exists()) {
			return getSubversionInvoker().deleteFile(toDelete);
		}
		else {
			return toDelete.delete();
		}
	}
	
    public static void blindlyDelete(File toDelete) {
        boolean deleted = toDelete.delete();
        if (!deleted) {
            System.out.println("(Ignoring) Unable to delete file " + toDelete.toString() );
        }
    }
	/**
	 * Pretty rote routine to copy a stream to a file destination.
	 *
	 * @param stream The stream whose contents should be copied.
	 * @param dest	The destination file.
	 *
	 * @throws IOException If something goes wrong creating, reading, or writing.
	 */
	public static void copyStreamToFile(InputStream stream, File dest) throws IOException {
		OutputStream fos = new FileOutputStream(dest);

		byte[] buffer = new byte[8192];

		transferStreamContents(buffer, stream, fos);

		stream.close();
		fos.close();
	}

	/**
	 * Transfer the entire contents of an InputStream to an OutputStream.
	 *
	 * @param buffer	Buffer to use for reading the contents.
	 * @param source	Source for data.
	 * @param dest		Destination for data from source.
	 *
	 * @throws IOException If something goes wrong reading or writing.
	 */
	public static void transferStreamContents(byte[] buffer, InputStream source, OutputStream dest) throws IOException {
		int bytesRead;
		while ( (bytesRead = source.read(buffer)) >= 0) {
			dest.write(buffer, 0, bytesRead);
		}
	}

	/**
	 * Transfer the contents of a stream to a byte array.
	 * 
	 * @param source	The source stream we're reading from.
	 * @param defaultCapacity	The default expected size - must be non-negative.
	 * @return	The bytes of the stream.
	 * 
	 * @throws IOException	Thrown if any errors occur reading the stream.
	 */
	public static byte[] transferStreamToBytes(InputStream source, int defaultCapacity) throws IOException {
		
		// create my destination buffer with the default size.
		ByteArrayOutputStream baos = new ByteArrayOutputStream( defaultCapacity );
		byte[] buffer = new byte[8192];
		FileUtils.transferStreamContents(buffer, source, baos);
		baos.close();

		// OK - finally we can read that darn file. 
		return baos.toByteArray();
	}

	/**
	 * Returns true if the passed bytes in question being with a traditional XML prefix.
	 *  
	 * @param content	The bytes to check as XML.
	 * @return
	 */
	public static boolean hasXmlStartingBytes(byte[] content) {
		
		boolean matched = false;
		int idx = 0;
		// loop through all the possible prefixes that we recognize...
		while (!matched && idx < sm_xmlprefixes.length) {
			byte[] toMatch = sm_xmlprefixes[idx++];
			
			// does the length of the content at least match the possible prefix.
			if (content.length > toMatch.length) {
				int matchIdx = 0;
				while (matchIdx < toMatch.length && content[matchIdx] == toMatch[matchIdx]) {
					matchIdx++;
				}
				matched = matchIdx >= toMatch.length;
			}
		}
		
		return matched;
	}
	
	private static byte[][] sm_xmlprefixes = new byte[][] {
		{ 0x3c, 0x3f, 0x78, 0x6d},	// standard 7 or 8 byte encoding
		{ 0x4c, 0x6f, (byte) 0xa7, (byte) 0x94},
		{ 0x3c, 0x00, 0x3f, 0x00, 0x78, 0x00, 0x6d, 0x00 }, // one of the UTF-16 encodings
		{ 0x00, 0x3c, 0x00, 0x3c, 0x00, 0x78, 0x00, 0x6d }, // the other UTF-16 encoding
		{ (byte) 0xff, (byte) 0xfe, 0x3c, 0x00, 0x3f, 0x00, 0x78, 0x00, 0x6d, 0x00 }, // one of the UTF-16 encodings, with BOM
		{ (byte) 0xfe, (byte) 0xff, 0x00, 0x3c, 0x00, 0x3c, 0x00, 0x78, 0x00, 0x6d }, // the other UTF-16 encoding, with BOM
	};
	
	/**
	 * Copy a file from one location to another.
	 *
	 * @param src The source file to copy
	 * @param dest	Destination file to copy.
	 * @throws IOException If something goes awry.
	 */
	public static void copyFile(File src, File dest) throws IOException {

		FileInputStream fis = new FileInputStream(src);
		copyStreamToFile(fis, dest);
	}

	public static void copyFolder(File src, File dest) throws IOException {

		if (dest.exists()) {
			throw new IOException("Destination of copyFolder exists.");
		}

		copyFolderContentsToFolder(src, dest);
	}

	/**
	 * Copy the contents of a folder to a new existing folder.
	 *
	 * @param src	The source folder to copy
	 * @param dest	The existing folder to copy into.
	 * @throws IOException
	 */
	public static void copyFolderContentsToFolder(File src, File dest) throws IOException {

		if (!dest.isDirectory() && !dest.mkdir()) {
		    throw new IOException("Failed to create directory " + dest.toString() );
        }

		File children[] = src.listFiles();
		for (File child: children) {
			String name = child.getName();
			File destChild = new File(dest, name);
			if (child.isDirectory() ) {
				copyFolderContentsToFolder(child, destChild);
			}
			else {
				copyFile(child, destChild);
			}
		}
	}

	/**
	 * Recursively delete the contents of a folder.
	 *
	 * @param folder	The folder to delete.
	 */
	public static boolean deleteFolder(File folder) {

        boolean deletedFolder = true;
        
		if (folder.exists()) {
			File children[] = folder.listFiles();
            File couldntDeleteChild = null; 
			for (File child : children) {
                boolean deleted = child.isDirectory() ?
                        deleteFolder(child) : child.delete();
                if (!deleted) {
                    couldntDeleteChild = child;
                }
			}

            if (couldntDeleteChild == null) {
                deletedFolder = folder.delete();
            }
		}
        
        return deletedFolder;
	}

	/**
	 * Zip the contents of a folder to a file...
	 *
	 * @param folderToZip The folder to zip up.
	 * @param target	The target file.
	 * @throws IOException If something goes wrong writing files
	 * @throws ZipException If something goes wrong with zipping the file
	 */
	public static void zipFolderToFile(File folderToZip, File target) throws ZipException, IOException {

		FileOutputStream fos = new FileOutputStream( target );
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		ZipOutputStream zos = new ZipOutputStream(bos);

		byte buffer[] = new byte[8192];

		zipContents(zos, folderToZip, "", buffer);

		zos.close();
	}

	/**
	 * Recursively generates the contents of a ZIP file...
	 *
	 * @param zos
	 * @param folderToZip
	 * @param pathSoFar
	 * @throws IOException
	 */
	public static void zipContents(ZipOutputStream zos, File folderToZip, String pathSoFar, byte[] buffer) throws IOException {

		File items[] = folderToZip.listFiles();
		for (File item : items) {

			String fullPath = pathSoFar + item.getName();

			// is it a file or a folder?
			if (item.isFile() ) {
				// a file - zip it up...
				ZipEntry ze = new ZipEntry( fullPath );
				ze.setTime( item.lastModified() );
				zos.putNextEntry( ze );

				FileInputStream fis = new FileInputStream(item);
				transferStreamContents(buffer, fis, zos);
				fis.close();

				zos.closeEntry();
			}
			else {
				zipContents(zos, item, fullPath + "/", buffer );
			}
		}
	}

    public static void createFileWithContents(File shortcutFile, String contents, Charset charset) throws IOException {
        FileOutputStream fos = new FileOutputStream(shortcutFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, charset );
        osw.write(contents);
        osw.close();
    }

	/**
	 * Convert a string into an absolute file location
	 * @param strLoc The location in question, which could start with "../", for example.
	 *
	 * @return The desired file location.
	 * @throws IOException If the file cannot be canonicalized.
	 */
	public static File resolvePath(File baseLoc, String strLoc) {
	    File absLoc = null;
	    if (strLoc != null && strLoc.length() > 0) {
	    	strLoc = stripQuotes(strLoc);
	    	boolean isAbsolute = baseLoc == null || isAbsolutePath(strLoc);
	        if (strLoc.length() > 0) {
	        	try {
		            if (isAbsolute)
		                absLoc = new File(strLoc).getCanonicalFile();
		            else
		                absLoc = new File(baseLoc, strLoc).getCanonicalFile();
	        	}
	        	catch (IOException ioe) {
	        		throw new IllegalArgumentException("Unable to canonicalize file for path " + strLoc);
	        	}
	        }
	    }
	
	    return absLoc;
	}

	/**
	 * Strip the quotation marks from a string, if need be.
	 * @param strLoc
	 * @return
	 */
	private static String stripQuotes(String strLoc) {
		if (strLoc.length() >= 2 && strLoc.startsWith("\"") &&
				strLoc.endsWith("\"") ) {
			strLoc = strLoc.substring(1, strLoc.length() - 1);
		}
		
		return strLoc;
	}

	public static boolean isAbsolutePath(String fileName)
	{
	    if (fileName == null)
	        return false;
	    if ( ((fileName.length() > 1) && (fileName.charAt(1) == ':')) // windows drive specifier
	        || (fileName.startsWith("/")) // unix root or unc path in samba format
	        || (fileName.startsWith("\\\\")) ) // unc path
	        return true;
	    return false;
	}

	/**
	 * Take a string that is split by either semicolons or colons, and turn it
	 * into a list of {@link File}.
	 *
	 * @param buildFeatures The string broken by path separators.
	 *
	 * @return A {@link List<File} of the files at the given destination.
	 * @throws IOException If the file path cannot be canonicalized properly.
	 */
	public static List<File> fileListFromString(File baseLocation, String buildFeatures) throws IOException {
	    List<File> features = new ArrayList<File>();
	    if (buildFeatures != null && buildFeatures.length() > 0) {
	        StringTokenizer tok = new StringTokenizer(buildFeatures, ";:");
	        while (tok.hasMoreTokens()) {
	            String fileLoc = tok.nextToken().trim();
	            File canonicalLoc = resolvePath(baseLocation, fileLoc);
	            if ( !canonicalLoc.exists() ) {
	                System.out.println("Expecting to find a file or folder at " + canonicalLoc.toString());
	                throw new IllegalStateException("Expecting to find a file or folder");
	            }
	
	            features.add(canonicalLoc);
	        }
	    }
	    return features;
	}

	/**
	 * Extract a ZIP file to a target location.
	 * @param zipFile	The file to extract
	 * @param targetDir	The target directory.
	 * @throws IOException
	 */
	public static void extractZipFile(File zipFile, File targetDir) throws IOException {
		FileInputStream fis = new FileInputStream(zipFile);
		ZipInputStream zis = new ZipInputStream(fis);
		extractZipStream(zis, targetDir);
	}
	
	/**
	 * Extract a ZIP stream to a particular folder
	 * @param zis	The zip stream to expand.
	 * @param targetDir	The target folder for the expansion.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void extractZipStream(ZipInputStream zis, File targetDir) throws IOException, FileNotFoundException {
		createDirectories(targetDir);
		
		byte [] buffer = new byte[8192];
		ZipEntry entry;
		while ( (entry = zis.getNextEntry() ) != null ) {
			File destItem = new File(targetDir, entry.getName());
			
			// is it a directory or file?
			if (entry.isDirectory()) {
				
				// make the dir.
				createDirectories(destItem);
			}
			else {
				// make sure the ancestor folder(s) exist. 
				File parent = destItem.getParentFile();
				createDirectories(parent);
				
                boolean success = false;
                try {
                    // write the contents of the file...
                    FileOutputStream fos = new FileOutputStream(destItem);
                    transferStreamContents(buffer, zis, fos);
                    fos.close();
                    success = true;
                }
                finally {
                    // if the file was not successfully written, delete it,
                    // and report an error
                    if (!success) {
                        System.out.println("Exception occurred while writing to " + destItem.toString() + ".  Deleting file.");
                        blindlyDelete(destItem);
                    }
                }
			}
			
			// close the entry and advance to the next.
			zis.closeEntry();
		}
		
		zis.close();
	}

	/**
	 * Copies the contents of a stream into a OS-level exclusive locked file.
	 * 
	 * @param bis	The InputStream whose contents should transfer.
	 * @param target	The target file operation.
	 * @throws IOException
	 */
	public static void copyStreamToLockedFile(InputStream bis, File target)
			throws IOException {
		RandomAccessFile targetRaf = new RandomAccessFile(target, "rw");
		FileChannel targetChannel = targetRaf.getChannel();
		
		// this exclusive lock here is meant to block other attempts to read or
		// write the file.
		targetChannel.lock();
		boolean success = false;
		try {
			
		    targetChannel.truncate(0);
		    OutputStream os = Channels.newOutputStream(targetChannel);
		    
			byte[] buffer = new byte[8192];
	
		    transferStreamContents(buffer, bis, os);
		    bis.close();
		    
		    success = true;
		} finally {
			
			// this should free the lock on the file.
			targetChannel.close();
			targetRaf.close();
			
			if (!success) {
				// make sure we clean up the file we've got a lock on.
				if (target.exists()) {
					blindlyDelete(target);
				}
			}
		}
	}
	
	private static synchronized SubversionInvoker getSubversionInvoker() {
		if (sm_svnInvoker == null) {
			sm_svnInvoker = new SubversionInvoker();
		}
		
		return sm_svnInvoker;
	}
	
	/**
	 * singleton useful for invoking Subversion....
	 */
	private static SubversionInvoker sm_svnInvoker;
}
