/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.eclipse.osgi.service.environment.Constants;

import org.eclipse.core.runtime.Platform;

public class ChkpiiTests extends TestCase {
	
	private String fLogDirectoryName;
	
	private static final int HTML= 0;
	private static final int PROPERTIES= 1;
	private static final int XML= 2;
	
	public void testChkpii() {
			
		StringBuffer message= new StringBuffer();
		StringBuffer buf= new StringBuffer();
		boolean testExecuted= testChkpii(HTML);
		assertTrue("Could not run chkpii test on HTML files. See console for details.", testExecuted); //$NON-NLS-1$
		boolean result1= checkValidLogFile(getOutputFile(HTML), buf);
		if (!result1)
			message.append("\n- HTML: \n" + buf);

		buf= new StringBuffer();
		testExecuted= testChkpii(XML);
		assertTrue("Could not run chkpii test on XML files. See console for details.", testExecuted); //$NON-NLS-1$
		boolean result2= checkValidLogFile(getOutputFile(XML), buf);
		if (!result2)
			message.append("\n- XML: \n" + buf);
		
		buf= new StringBuffer();
		testExecuted= testChkpii(PROPERTIES);
		assertTrue("Could not run chkpii test on PROPERTIES files. See console for details.", testExecuted); //$NON-NLS-1$
		boolean result3= checkValidLogFile(getOutputFile(PROPERTIES), buf);
		if (!result3)
			message.append("\n- PROPERTIES: \n" + buf);

		assertTrue(message + "\nSee " + fLogDirectoryName + " for details.", (result1 && result2 && result3)); //$NON-NLS-1$ //$NON-NLS-2$
	}
		
	private boolean testChkpii(int type) {
		Runtime aRuntime= Runtime.getRuntime();
		String chkpiiString= getChkpiiString(type);
		BufferedReader aBufferedReader= null;
		StringBuffer consoleLog= new StringBuffer();
		try {
			Process aProcess= aRuntime.exec(chkpiiString);
			aBufferedReader= new BufferedReader(new InputStreamReader(aProcess.getErrorStream()));
			String line= aBufferedReader.readLine();
			while (line != null) {
				consoleLog.append(line);
				consoleLog.append('\n');
				line= aBufferedReader.readLine();
			}
			aProcess.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			return false;
		} finally {
			if (aBufferedReader != null)
				try {
					aBufferedReader.close();
				} catch (IOException ex) {
				}
		}
		if (!new File(getOutputFile(type)).exists()) {
			System.out.println(consoleLog.toString());
			System.out.flush();
			return false;
		}
		return true;
	}
	
	private String getChkpiiString(int type) {
		return getExec() + " " + getFilesToTest(type) + " -E -O " + getOutputFile(type) + " -XM @" + getExcludeErrors() + " -X " + getExcludeFile () + " -S"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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

	private String getFilesToTest(int type) {
			
		String aString= getPluginDirectory();
			
		switch (type) {
			case HTML :
				return aString + "*.htm*"; //$NON-NLS-1$
			case PROPERTIES :
				return aString + "*.properties"; //$NON-NLS-1$
							
			case XML : 
				return aString + "*.xml"; //$NON-NLS-1$
			
			default :
				return aString + "*.*"; //$NON-NLS-1$
		}
	}
	
	private String toLocation(URL platformURL) {
		File localFile;
		try {
			localFile= new File(Platform.asLocalURL(platformURL).getFile());
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
	 * Returns the output file path.
	 * 
	 * @param type the file type
	 * @return the string with the output file path
	 */
	private String getOutputFile(int type) {

		switch (type) {

			case HTML :
				return fLogDirectoryName + "html.txt"; //$NON-NLS-1$

			case PROPERTIES :
				return fLogDirectoryName + "properties.txt"; //$NON-NLS-1$
		
			case XML : 
				return fLogDirectoryName + "xml.txt"; //$NON-NLS-1$

			default :
				return fLogDirectoryName + "other.txt"; //$NON-NLS-1$
		}
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
	 * @param logFilePath the path of the chkpii log file
	 * @param message a stringbuffer to append error messages to
	 * @return <code>true</code> if there are errors in the log file
	 */
	private boolean checkValidLogFile(String logFilePath, StringBuffer message) {
		BufferedReader aReader= null;
		int errors= -1, notProcessed= -1, endOfSummary= -1;
		boolean hasErrors= false;
		
		try {
			aReader= new BufferedReader(new InputStreamReader(new FileInputStream(logFilePath), Charset.forName("ISO-8859-1")));
			String aLine= aReader.readLine();
			while (aLine != null) {
				if (errors == -1)
					errors= parseErrorSummary(aLine);
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
				hasErrors= true;
			}
			if (notProcessed > 0) { 
				message.append("" + notProcessed + " files not found\n");
				hasErrors= true;
			}
			if (endOfSummary != 0) {
				message.append("Incomplete logfile\n");
				hasErrors= true;
			}
			
		} catch (FileNotFoundException e) {
			message.append("Could not open log file: " + logFilePath + "\n" + e.getLocalizedMessage() + "\n"); //$NON-NLS-1$
			hasErrors= true;
		} catch (IOException e) {
			message.append("Error reading log file: " + logFilePath + "\n" + e.getLocalizedMessage() + "\n"); //$NON-NLS-1$
			hasErrors= true;
		} finally {
			if (aReader != null) {
				try {
					aReader.close();
				} catch (IOException e) {
					message.append("Error closing log file: " + logFilePath + "\n" + e.getLocalizedMessage() + "\n"); //$NON-NLS-1$
					hasErrors= true;
				}
			}
		}
		
		return !hasErrors;
	}
	
	private int parseErrorSummary(String aLine) {
		int index= aLine.indexOf("Files Contain Error"); //$NON-NLS-1$
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
	 * @param arg0
	 */
	public ChkpiiTests(String arg0) {
		super(arg0);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();

		fLogDirectoryName= getPluginDirectory() + "chkpiiResults" + File.separator; //$NON-NLS-1$ //$NON-NLS-2$

		new File(fLogDirectoryName).mkdirs();

		new File(getOutputFile(PROPERTIES)).delete();
		new File(getOutputFile(HTML)).delete();
		new File(getOutputFile(XML)).delete();
		
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		fLogDirectoryName= null;		
	}
}
