/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.FileLocator;

import org.eclipse.jdt.internal.ui.util.StringMatcher;

/**
 * Runs the 'tidy' command (e.g. from Cygwin) on HTML files in the workspace
 * and collects HTMLTidy's errors and warnings into /chkpiiResults/tidy.txt
 *
 * Note: Currently disabled.
 */
public class HTMLTidyTest extends TestCase {

	private static final int TIDY_TIMEOUT= 10000;
	private static final Pattern EXCLUSION_PATTERN_TABLE_SUMMARY= Pattern.compile("line \\d+ column \\d+ - Warning: <table> lacks \"summary\" attribute");

	private static final Pattern EXCLUSION_PATTERN_EMPTY_TAG= Pattern.compile("line \\d+ column \\d+ - Warning: trimming empty <\\w+>");
	private static final Pattern EXCLUSION_PATTERN_MALFORMED_URI= Pattern.compile("line \\d+ column \\d+ - Warning: <a> escaping malformed URI reference");
	private static final Pattern eXCLUSION_PATTERN_SCRIPT_TYPE= Pattern.compile("line \\d+ column \\d+ - Warning: <script> inserting \"type\" attribute");
	private static final Pattern eXCLUSION_PATTERN_IMG_LACKS_ALT= Pattern.compile("line \\d+ column \\d+ - Warning: <img> lacks \"alt\" attribute");

	private int fworkspacePathLength;
	private StringMatcher[] fIgnores;
	private Writer fTidyResults;

	/**
	 * Main method for manual testing of a file or folder selected in the workspace. To run, create
	 * a Java launch configuration with program argument<br>
	 * <code>"${resource_loc}"</code><br>
	 * (including double quotes!).
	 * 
	 * @param args 1 argument: Absolute path to a file or folder
	 * @throws Exception if checking fails
	 */
	public static void main(String[] args) throws Exception {
		HTMLTidyTest test= new HTMLTidyTest();
		test.fIgnores= new StringMatcher[0];
		test.fTidyResults= new OutputStreamWriter(System.out);
		File file= new File(args[0]);
		if (file.isDirectory())
			test.checkFolder(file);
		else
			test.checkFile(file);
	}

	public void testHTML() throws Exception {
		URL testBundleRoot= JavaTestPlugin.getDefault().getBundle().getEntry("/");
		URL testBundleFile= FileLocator.toFileURL(testBundleRoot);
		File hostWorkspace= new File(testBundleFile.getFile()).getParentFile();
		String workspacePath= hostWorkspace.getAbsolutePath();
		fworkspacePathLength= workspacePath.length();
		File chkpiiResults= new File(hostWorkspace, "chkpiiResults");
		File tidy= new File(chkpiiResults, "tidy.txt");
		fTidyResults= new OutputStreamWriter(new FileOutputStream(tidy), "UTF-8");
		String startTime= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		fTidyResults.write(getClass().getName() + " started at " + startTime + "\n");
		fIgnores= getIgnores();

		try {
			boolean ok= true;
//			ok&= checkFolder(new File(hostWorkspace, "org.eclipse.jdt.doc.user"), tidyResults);
//			ok&= checkFolder(new File(hostWorkspace, "org.eclipse.jdt.doc.isv"), tidyResults);
//			ok&= checkFolder(new File(hostWorkspace, "org.eclipse.platform.doc.user"), tidyResults);
//			ok&= checkFolder(new File(hostWorkspace, "org.eclipse.platform.doc.isv"), tidyResults);
//			ok&= checkFolder(new File(hostWorkspace + "/org.eclipse.jdt.doc.isv/guide"));

			ok&= checkFolder(hostWorkspace);

			assertTrue("See " + tidy.getAbsolutePath(), ok);
		} finally {
			fTidyResults.flush();
			fTidyResults.close();
			fTidyResults= null;
		}
	}

	private StringMatcher[] getIgnores() throws IOException {
		ArrayList matchers= new ArrayList();
		InputStream is= getClass().getResourceAsStream("ignoreFiles.txt");
		BufferedReader reader= new BufferedReader(new InputStreamReader(is));
		while (reader.ready()) {
			String line= reader.readLine();
			if (line.length() > 0) {
				char first= line.charAt(0);
				if (line.endsWith("/*"))
					line= line.substring(0, line.length() - 2); // stops at directory during tree traversal
				if (first != '/' && first != '*') { // relative matches
					// emulate CHKPII specification:
					// matchers.add(new StringMatcher(fWorkspacePath + "/" + line, true, false));

					// emulate actual CHKPII implementation:
					matchers.add(new StringMatcher("*/" + line, true, false));
				}
				matchers.add(new StringMatcher(line, true, false));
			}
		}
		return (StringMatcher[]) matchers.toArray(new StringMatcher[matchers.size()]);
	}

	private boolean isIgnored(File file) {
		String relativePath= file.getAbsolutePath().substring(fworkspacePathLength);
		relativePath= relativePath.replace('\\', '/');
		for (int i= 0; i < fIgnores.length; i++) {
			StringMatcher matcher= fIgnores[i];
			if (matcher.match(relativePath))
				return true;
		}
		return false;
	}

	private boolean checkFolder(File folder) throws Exception {

		File[] files= folder.listFiles();
		boolean success= true;
		for (int i= 0; i < files.length; i++) {
			File file= files[i];
			if (isIgnored(file)) {
//				System.out.println("Ignored: " + file.getAbsolutePath() + (file.isDirectory() ? "/*" : ""));
				continue;
			}
			if (isHTMLFile(file)) {
				success&= checkFile(file);
			} else if (file.isDirectory()) {
				success&= checkFolder(file);
			}
		}
		return success;
	}

	private boolean checkFile(File file) throws Exception {
		String filePath= file.getAbsolutePath();
		String command= "tidy -eq \"" + filePath + "\"";
		final Process process= Runtime.getRuntime().exec(command);
		long timeout= System.currentTimeMillis() + TIDY_TIMEOUT;

		BufferedReader tidyReader= new BufferedReader(new InputStreamReader(process.getErrorStream()));
		boolean available= tidyReader.ready();
		boolean first= true;
		while (available || System.currentTimeMillis() < timeout) {
			if (! available) {
				if (! isAlive(process))
					break;
				Thread.sleep(100);
			} else {
				while (available) {
					String line= tidyReader.readLine();
					if (isRelevant(line)) {
						if (first) {
							first= false;
							fTidyResults.write("\n--- " + filePath +'\n');
						}
						fTidyResults.write(line);
						fTidyResults.write('\n');
					}
					available= tidyReader.ready();
				}
			}
			available= tidyReader.ready();
			if (! available && ! isAlive(process))
				break;
		}
		fTidyResults.flush();
		try {
			return process.exitValue() == 0;
		} catch (IllegalThreadStateException e) {
			process.destroy();
			fail("'" + command + "' killed after " + TIDY_TIMEOUT +" ms");
			return false;
		}
	}

	private static boolean isRelevant(String line) {
		return ! EXCLUSION_PATTERN_TABLE_SUMMARY.matcher(line).matches()
				&& ! EXCLUSION_PATTERN_EMPTY_TAG.matcher(line).matches()
				&& ! EXCLUSION_PATTERN_MALFORMED_URI.matcher(line).matches()
				&& ! eXCLUSION_PATTERN_SCRIPT_TYPE.matcher(line).matches()
				&& ! eXCLUSION_PATTERN_IMG_LACKS_ALT.matcher(line).matches()
				;
	}

	private static boolean isAlive(Process process) {
		try {
			process.exitValue();
		} catch (IllegalThreadStateException e) {
			return true;
		}
		return false;
	}

	private static boolean isHTMLFile(File file) {
		String name= file.getName();
		return (name.endsWith(".html") || name.endsWith(".htm")) && file.isFile();
	}
}
