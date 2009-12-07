/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.chkpii;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

import junit.extensions.ActiveTestSuite;
import junit.framework.Test;
import junit.framework.TestCase;

import org.eclipse.osgi.service.environment.Constants;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

public class ChkpiiTests extends TestCase {

	private String fLogDirectoryName;

	private class FileCategory {
		private final String fName;
		protected FileCategory(String name) {
			fName= name;
		}
		public String getOutputFile() {
			return fLogDirectoryName + fName.toLowerCase() + ".txt";
		}
		public String getFilesToTest() {
			return getPluginDirectory() + getExtension();
		}
		protected String getExtension() {
			return "*." + fName.toLowerCase();
		}

		public String toString() {
			return fName.toUpperCase();
		}
	}
	private static class StreamConsumer extends Thread {
		StringBuffer fStrBuffer;
		BufferedReader fReader;


		public String getContents() {
			return fStrBuffer.toString();
		}

		public StreamConsumer(InputStream inputStream) {
			super();
			setDaemon(true);
			try {
				fReader = new BufferedReader(new InputStreamReader(inputStream, "LATIN1"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				fReader = new BufferedReader(new InputStreamReader(inputStream));
			}
			fStrBuffer= new StringBuffer();
		}

		public void run() {
			try {
				char[] buf= new char[1024];
				int count;
				while (0 < (count = fReader.read(buf))) {
					fStrBuffer.append(buf, 0, count);
				}
				fReader.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		public void terminate() {
			interrupt();
			try {
				fReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private final FileCategory HTML= new FileCategory("HTML") {
		protected String getExtension() {
			return "*.htm*";
		}
	};
	private final FileCategory PROPERTIES= new FileCategory("PROPERTIES");
	private final FileCategory XML= new FileCategory("XML");

	public static Test suite() {
		return new ActiveTestSuite(ChkpiiTests.class);
	}
	
	public void testHTMLFiles() {
		assertChkpii(HTML);
	}

	public void testXMLFiles() {
		assertChkpii(XML);
	}

	public void testPropertiesFiles() {
		assertChkpii(PROPERTIES);
	}

	private void assertChkpii(FileCategory type) {

		boolean isExecuted= executeChkpiiProcess(type);
		assertTrue("Could not run chkpii test on " + type + " files. See console for details.", isExecuted); //$NON-NLS-1$
		StringBuffer buf= new StringBuffer();
		boolean isValid= checkLogFile(type, buf);
		assertTrue(buf + "See " + type.getOutputFile() + " for details.", isValid); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean executeChkpiiProcess(FileCategory type) {
		Runtime aRuntime= Runtime.getRuntime();
		String chkpiiString= getChkpiiString(type);

		StreamConsumer err= null;
		StreamConsumer out= null;
		Process process= null;
		try {
			process= aRuntime.exec(chkpiiString);
			err= new StreamConsumer(process.getErrorStream());
			out= new StreamConsumer(process.getInputStream());
			err.start();
			out.start();
			process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			return false;
		} finally {
			out.terminate();
			err.terminate();
		}

 		if (err.getContents().length() > 0 || !new File(type.getOutputFile()).exists()) {
			System.out.println(out.getContents());
			System.out.println(err.getContents());
			System.out.flush();
			return false;
		}
 		int res= process.exitValue();
 		System.out.println("ChkpiiTests#" + getName() + "() exit value: " + res);
		return true;
	}

	private String getChkpiiString(FileCategory type) {
		return getExec() + " " + type.getFilesToTest() + " -E -O " + type.getOutputFile() + " -XM @" + getExcludeErrors() + " -X " + getExcludeFile () + " -S -JSQ"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	private String getPluginDirectory() {

		// Get some path inside a plug-in
		String filePath= toLocation(getClass().getResource("ignoreFiles.txt"));

		StringTokenizer tokenizer= new StringTokenizer(filePath, File.separator);

		String path= "";
		if (filePath.charAt(0) != File.separatorChar)
			path= tokenizer.nextToken();

		while (tokenizer.hasMoreTokens()) {
			String token= tokenizer.nextToken();
			if (token.equals("org.eclipse.jdt.ui.tests"))
				break;

			path= path + File.separator + token;
		}
		return path + File.separator;
	}

	private String toLocation(URL platformURL) {
		File localFile;
		try {
			localFile= new File(FileLocator.toFileURL(platformURL).getFile());
		} catch (IOException e) {
			e.printStackTrace();
			return platformURL.getFile();
		}
		try {
			return localFile.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
			return localFile.getPath();
		}
	}

	/**
	 * Method getExcludeFiles.
	 *
	 * @return String
	 */
	private String getExcludeFile() {
		return toLocation(getClass().getResource("ignoreFiles.txt"));
	}

	/**
	 * Method getExec.
	 *
	 * @return String
	 */
	private String getExec() {
		return new File("chkpii.exe").getPath(); //$NON-NLS-1$
	}

	private String getExcludeErrors() {

		String fileName;

		if (Platform.getOS().equals(Constants.OS_WIN32))
			fileName= "ignoreErrorsWindows.txt"; //$NON-NLS-1$
		else
			fileName= "ignoreErrorsUnix.txt"; //$NON-NLS-1$

		return toLocation(getClass().getResource(fileName));
	}

	/**
	 * Checks if the given log file is valid and states no errors.
	 *
	 * @param type the file category
	 * @param message a string buffer to append error messages to
	 * @return <code>true</code> if there are errors in the log file
	 */
	private boolean checkLogFile(FileCategory type, StringBuffer message) {
		String logFilePath= type.getOutputFile();
		BufferedReader aReader= null;
		int errors= -1, warnings= -1, notProcessed= -1, endOfSummary= -1;
		boolean hasFailed= false;

		try {
			aReader= new BufferedReader(new InputStreamReader(new FileInputStream(logFilePath), Charset.forName("ISO-8859-1")));
			String aLine= aReader.readLine();
			while (aLine != null) {
				if (errors == -1)
					errors= parseSummary(aLine, "Files Contain Error");
				if (XML != type  && warnings == -1)
					warnings= parseSummary(aLine, "Files Contain Warnings Only");
				else if (notProcessed == -1)
					notProcessed= parseNotProcessedSummary(aLine);
				else if (endOfSummary == -1)
					endOfSummary= parseEndOfSummary(aLine);
				else
					break;

				aLine= aReader.readLine();
			}

			if (errors > 0) {
				message.append("" + errors + " files containing errors\n");
				hasFailed= true;
			}
			if (XML != type && warnings > 0) {
				message.append("" + errors + " files containing errors\n");
				hasFailed= true;
			}
			if (notProcessed > 0) {
				message.append("" + notProcessed + " files not found\n");
				hasFailed= true;
			}
			if (endOfSummary != 0) {
				message.append("Incomplete logfile\n");
				hasFailed= true;
			}

		} catch (FileNotFoundException e) {
			message.append("Could not open log file: " + logFilePath + "\n" + e.getLocalizedMessage() + "\n"); //$NON-NLS-1$
			hasFailed= true;
		} catch (IOException e) {
			message.append("Error reading log file: " + logFilePath + "\n" + e.getLocalizedMessage() + "\n"); //$NON-NLS-1$
			hasFailed= true;
		} finally {
			if (aReader != null) {
				try {
					aReader.close();
				} catch (IOException e) {
					message.append("Error closing log file: " + logFilePath + "\n" + e.getLocalizedMessage() + "\n"); //$NON-NLS-1$
					hasFailed= true;
				}
			}
		}

		return !hasFailed;
	}

	private int parseSummary(String aLine, String parseString) {
		int index= aLine.indexOf(parseString);
		if (index == -1)
			return -1;

		String aString= aLine.substring(0, index).trim();
		try {
			return Integer.parseInt(aString);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private int parseNotProcessedSummary(String aLine) {
		int index= aLine.indexOf("Files Could Not Be Processed"); //$NON-NLS-1$
		if (index == -1)
			return -1;

		String aString= aLine.substring(0, index).trim();
		try {
			return Integer.parseInt(aString);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private int parseEndOfSummary(String aLine) {
		int index= aLine.indexOf("End of Listing"); //$NON-NLS-1$
		if (index == -1)
			return -1;
		return 0;
	}

	/**
	 * Constructor for EmptyDirectoriesTest.
	 * 
	 * @param name the test name
	 */
	public ChkpiiTests(String name) {
		super(name);
		fLogDirectoryName= getPluginDirectory() + "chkpiiResults" + File.separator; //$NON-NLS-1$
		new File(PROPERTIES.getOutputFile()).delete();
		new File(HTML.getOutputFile()).delete();
		new File(XML.getOutputFile()).delete();
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		new File(fLogDirectoryName).mkdirs();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		fLogDirectoryName= null;
	}
}
