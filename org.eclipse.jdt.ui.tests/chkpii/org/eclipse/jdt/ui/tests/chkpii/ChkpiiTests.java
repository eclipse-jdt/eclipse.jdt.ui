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
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Platform;

import org.eclipse.osgi.service.environment.Constants;

public class ChkpiiTests extends TestCase {
	
	private String fLogDirectoryName;
	
	private static final int HTML= 0;
	private static final int PROPERTIES= 1;
	private static final int XML= 2;
	
	/**
	 * Checks if the given log file contains errors.
	 * 
	 * @param logFilePath the path of the chkpii log file
	 * @return <code>true</code> if there are errors in the log file
	 */
	private boolean hasErrors(String logFilePath) {
		BufferedReader aReader= null;
		
		try {
			aReader= new BufferedReader(new InputStreamReader(new FileInputStream(logFilePath)));
			String aLine= aReader.readLine();
			while (aLine != null) {
				int aNumber= parseLine(aLine);
				if (aNumber > 0)
					return true;

				aLine= aReader.readLine();
			}
		} catch (FileNotFoundException e) {
			System.out.println("Could not open log file: " + logFilePath); //$NON-NLS-1$
			return true;
		} catch (IOException e) {
			System.out.println("Error reading log file: " + logFilePath); //$NON-NLS-1$
			return true;
		} finally {
			if (aReader != null) {
				try {
					aReader.close();
				} catch (IOException e) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public void testChkpii() {
			
		boolean testExecuted= testChkpii(HTML);
		assertTrue("Could not run chkpii test. See console for details.", testExecuted); //$NON-NLS-1$
		boolean result1= !hasErrors(getOutputFile(HTML));

		testChkpii(XML);
		assertTrue("Could not run chkpii test. See console for details.", testExecuted); //$NON-NLS-1$
		boolean result2= !hasErrors(getOutputFile(XML));
		
		testChkpii(PROPERTIES);
		assertTrue("Could not run chkpii test. See console for details.", testExecuted); //$NON-NLS-1$
		boolean result3= !hasErrors(getOutputFile(PROPERTIES));

		assertTrue("CHKPII warnings or errors in files. See " + fLogDirectoryName + " for details.", (result1 && result2 && result3)); //$NON-NLS-1$ //$NON-NLS-2$
	}
		
	private boolean testChkpii(int type) {
		Runtime aRuntime= Runtime.getRuntime();
		String chkpiiString= getChkpiiString(type);
		BufferedReader aBufferedReader= null;
		StringBuffer consoleLog= new StringBuffer();
		try {
			Process aProcess= aRuntime.exec(chkpiiString);
			aBufferedReader= new BufferedReader(new InputStreamReader(aProcess.getInputStream()));
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
	
	/**
	 * Method getChkpiiString.
	 * 
	 * @param HTML
	 * @return String
	 */
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

	/**
	 * Method getFilesToTest.
	 * 
	 * @param HTML
	 * @return String
	 */
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
				return fLogDirectoryName + File.separator + "html.txt"; //$NON-NLS-1$

			case PROPERTIES :
				return fLogDirectoryName + File.separator + "properties.txt"; //$NON-NLS-1$
		
			case XML : 
				return fLogDirectoryName + File.separator + "xml.txt"; //$NON-NLS-1$

			default :
				return fLogDirectoryName + File.separator + "other.txt"; //$NON-NLS-1$
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
	
	/**
	 * Method getExcludeErrors.
	 */
	private String getExcludeErrors() {
		
		String fileName;
		
		if (Platform.getOS().equals(Constants.OS_WIN32))
			fileName= "ignoreErrorsWindows.txt"; //$NON-NLS-1$
		else
			fileName= "ignoreErrorsUnix.txt"; //$NON-NLS-1$
		
		return toLocation(getClass().getResource(fileName));
	}
		
	/**
	 * Method parseLine.
	 * 
	 * @param aLine
	 * @return -1 if not an error or warning line or the number of errors or
	 * warnings.
	 */
	private int parseLine(String aLine) {
		int index= aLine.indexOf("Files Contain Error"); //$NON-NLS-1$
		
//		if (index == -1) {
//			index= aLine.indexOf("Files Contain Warning"); //$NON-NLS-1$
//		}
		
		if (index == -1) {
			index= aLine.indexOf("Files Could Not Be Processed"); //$NON-NLS-1$
		}
		
		if (index == -1) {
			return index;
		}
		
		String aString= aLine.substring(0, index).trim();
		return Integer.parseInt(aString);
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
