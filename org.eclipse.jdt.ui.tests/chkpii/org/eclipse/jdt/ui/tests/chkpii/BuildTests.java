/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import junit.framework.TestCase;

import org.eclipse.core.boot.BootLoader;

public class BuildTests extends TestCase {
	
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
			
			boolean result1= testChkpii(HTML);
			boolean result2= testChkpii(XML);
			boolean result3= testChkpii(PROPERTIES);

			assertTrue("CHKPII errors in files. See the <workspace>/chkpiiResults/ for details.", (result1 && result2 && result3)); //$NON-NLS-1$
		}
		
		private boolean testChkpii(int type) {
			Runtime aRuntime= Runtime.getRuntime();
			String chkpiiString= getChkpiiString(type);
			try {
				Process aProcess= aRuntime.exec(chkpiiString);
				BufferedReader aBufferedReader= new BufferedReader(new InputStreamReader(aProcess.getInputStream()));
				while (aBufferedReader.readLine() != null) {
				}
				aProcess.waitFor();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (InterruptedException e) {
				return false;
			}
			return !hasErrors(getOutputFile(type));
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


		private String getUserDirectory() {
			return System.getProperty("user.dir") + File.separator; //$NON-NLS-1$
		}

		/**
		 * Method getFilesToTest.
		 * 
		 * @param HTML
		 * @return String
		 */
		private String getFilesToTest(int type) {
				
			String sniffFolder=	getUserDirectory() + ".." + File.separator; //$NON-NLS-1$
	
			String aString= sniffFolder;
				
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
		
		/**
		 * Method getExcludeFiles.
		 * 
		 * @return String
		 */
		private String getExcludeFile() {
			String aString= getUserDirectory() + "chkpii" + File.separator + "ignoreFiles.txt";  //$NON-NLS-1$//$NON-NLS-2$
			return new File(aString).getPath();
		}
		
		/**
		 * Returns the output file path.
		 * 
		 * @param type the file type
		 * @return the string with the output file path
		 */
		private String getOutputFile(int type) {

			new File(fLogDirectoryName).mkdirs();

			String aString;
			try {
				aString= new File(fLogDirectoryName).getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
				aString= new File(fLogDirectoryName).getPath();
			}

			switch (type) {

				case HTML :
					return aString + File.separator + "html.txt"; //$NON-NLS-1$

				case PROPERTIES :
					return aString + File.separator + "properties.txt"; //$NON-NLS-1$
			
				case XML : 
					return aString + File.separator + "xml.txt"; //$NON-NLS-1$

				default :
					return aString + File.separator + "other.txt"; //$NON-NLS-1$
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
			
			if (BootLoader.getOS().equals(BootLoader.OS_WIN32))
				fileName= "ignoreErrorsWindows.txt"; //$NON-NLS-1$
			else
				fileName= "ignoreErrorsUnix.txt"; //$NON-NLS-1$
			
			String aString= getUserDirectory() + "chkpii" + File.separator + fileName; //$NON-NLS-1$
			return new File(aString).getPath();
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
		
		if (index == -1) {
			index= aLine.indexOf("Files Contain Warning"); //$NON-NLS-1$
		}
		
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
	public BuildTests(String arg0) {
		super(arg0);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		fLogDirectoryName= getUserDirectory() + ".." + File.separator + "chkpiiResults" + File.separator; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		fLogDirectoryName= null;		
	}

}
