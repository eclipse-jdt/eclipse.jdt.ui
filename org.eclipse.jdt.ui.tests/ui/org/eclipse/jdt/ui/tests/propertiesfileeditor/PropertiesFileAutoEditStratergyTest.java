/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.propertiesfileeditor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.DocumentCommand;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileAutoEditStrategy;

/**
 * PropertiesFileAutoEditStratergyTest
 * 
 * @since 3.7
 */
public class PropertiesFileAutoEditStratergyTest extends TestCase {

	private static final String UTF_8= "UTF-8";
	private static final String ISO_8859_1= "ISO-8859-1";

	private DocumentCommand fDocumentCommand;

	private IPreferenceStore fPreferenceStore;

	private PropertiesFileAutoEditStrategy fPropertiesFileAutoEditStrategyISO_8859_1;
	private PropertiesFileAutoEditStrategy fPropertiesFileAutoEditStrategyUTF8;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private IFile fPropertiesFileISO_8859_1;
	private IFile fPropertiesFileUTF8;

	public PropertiesFileAutoEditStratergyTest(String name) {
		super(name);
		fPreferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		fDocumentCommand= new DocumentCommand() {
		};
	}

	public static Test suite() {
		return new ProjectTestSetup(new TestSuite(PropertiesFileAutoEditStratergyTest.class));
	}

	protected void setUp() throws Exception {
		super.setUp();
		setEscapeBackslashIfRequired(true); // make sure that the preference is set to true
		try {
			fJProject1= ProjectTestSetup.getProject();
			fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
			IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
			fPropertiesFileISO_8859_1= write((IFolder)pack.getCorrespondingResource(), "", "Messages_ISO_8859_1.properties");
			fPropertiesFileISO_8859_1.setCharset(ISO_8859_1, null);
			fPropertiesFileUTF8= write((IFolder)pack.getCorrespondingResource(), "", "Messages_UTF8.properties");
			fPropertiesFileUTF8.setCharset(UTF_8, null);
			fPropertiesFileAutoEditStrategyISO_8859_1= new PropertiesFileAutoEditStrategy(fPreferenceStore, fPropertiesFileISO_8859_1);
			fPropertiesFileAutoEditStrategyUTF8= new PropertiesFileAutoEditStrategy(fPreferenceStore, fPropertiesFileUTF8);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
		super.tearDown();
	}

	//utility methods
	private void performTestISO_8859_1() {
		fPropertiesFileAutoEditStrategyISO_8859_1.customizeDocumentCommand(null, fDocumentCommand);
	}

	private void performTestAutoEscapeSlashDisabledISO_8859_1() {
		setEscapeBackslashIfRequired(false);
		fPropertiesFileAutoEditStrategyISO_8859_1.customizeDocumentCommand(null, fDocumentCommand);
		setEscapeBackslashIfRequired(true);
	}

	private void performTestUTF8() {
		fPropertiesFileAutoEditStrategyUTF8.customizeDocumentCommand(null, fDocumentCommand);
	}

	private void performTestAutoEscapeSlashDisabledUTF8() {
		setEscapeBackslashIfRequired(false);
		fPropertiesFileAutoEditStrategyUTF8.customizeDocumentCommand(null, fDocumentCommand);
		setEscapeBackslashIfRequired(true);
	}

	private void setEscapeBackslashIfRequired(boolean value) {
		fPreferenceStore.setValue(PreferenceConstants.PROPERTIES_FILE_WHEN_PASTING_ESCAPE_BACKSLASH_IF_REQUIRED, value);
	}

	private IFile write(IFolder folder, final String content, final String fileName) throws CoreException {
		InputStream stream= new InputStream() {
			private final Reader fReader= new StringReader(content);

			public int read() throws IOException {
				return fReader.read();
			}
		};
		IFile file= fJProject1.getProject().getFile(folder.getProjectRelativePath().append(fileName));
		file.create(stream, true, null);
		return file;
	}

	//typing ISO-8859-1
	public void testTypeISO_8859_1_01() throws Exception {
		fDocumentCommand.text= "\\";
		performTestISO_8859_1();
		assertEquals("\\", fDocumentCommand.text);
	}

	public void testTypeISO_8859_1_02() throws Exception {
		fDocumentCommand.text= "\t";
		performTestISO_8859_1();
		assertEquals("\t", fDocumentCommand.text);
	}

	public void testTypeISO_8859_1_03() throws Exception {
		fDocumentCommand.text= "\f";
		performTestISO_8859_1();
		assertEquals("\f", fDocumentCommand.text);
	}

	public void testTypeISO_8859_1_04() throws Exception {
		fDocumentCommand.text= "\r";
		performTestISO_8859_1();
		assertEquals("\r", fDocumentCommand.text);
	}

	public void testTypeISO_8859_1_05() throws Exception {
		fDocumentCommand.text= "\n";
		performTestISO_8859_1();
		assertEquals("\n", fDocumentCommand.text);
	}

	public void testTypeISO_8859_1_06() throws Exception {
		fDocumentCommand.text= "\u2603";
		performTestISO_8859_1();
		assertEquals("\\u2603", fDocumentCommand.text);
	}

	//typing UTF-8
	public void testTypeUTF8_01() throws Exception {
		fDocumentCommand.text= "\u2603";
		performTestUTF8();
		assertEquals("\u2603", fDocumentCommand.text);
	}

	//paste ISO-8859-1
	public void testPasteISO_8859_1_01() throws Exception {
		fDocumentCommand.text= "C:\\Program Files\\Java";
		performTestISO_8859_1();
		assertEquals("C:\\\\Program Files\\\\Java", fDocumentCommand.text);
	}

	public void testPasteISO_8859_1_02() throws Exception {
		fDocumentCommand.text= "C:\\new folder\\A.java";
		performTestISO_8859_1();
		assertEquals("C:\\\\new folder\\\\A.java", fDocumentCommand.text);
	}

	public void testPasteISO_8859_1_03() throws Exception {
		fDocumentCommand.text= "\u0926 \u0905";
		performTestISO_8859_1();
		assertEquals("\\u0926 \\u0905", fDocumentCommand.text);
	}

	public void testPasteISO_8859_1_04() throws Exception {
		fDocumentCommand.text= "\u0926 \\u0905";
		performTestISO_8859_1();
		assertEquals("\\u0926 \\\\u0905", fDocumentCommand.text);
	}

	public void testPasteISO_8859_1_05() throws Exception {
		fDocumentCommand.text= "ä \u0926";
		performTestISO_8859_1();
		assertEquals("ä \\u0926", fDocumentCommand.text);
	}

	public void testPasteISO_8859_1_06() throws Exception {
		fDocumentCommand.text= "some text\\";
		performTestISO_8859_1();
		assertEquals("some text\\\\", fDocumentCommand.text);
	}

	//paste UTF-8
	public void testPasteUTF8_01() throws Exception {
		fDocumentCommand.text= "C:\\Program Files\\Java";
		performTestUTF8();
		assertEquals("C:\\\\Program Files\\\\Java", fDocumentCommand.text);
	}

	public void testPasteUTF8_02() throws Exception {
		fDocumentCommand.text= "C:\\new folder\\A.java";
		performTestUTF8();
		assertEquals("C:\\\\new folder\\\\A.java", fDocumentCommand.text);
	}

	public void testPasteUTF8_03() throws Exception {
		fDocumentCommand.text= "\u0926 \u0905";
		performTestUTF8();
		assertEquals("\u0926 \u0905", fDocumentCommand.text);
	}

	public void testPasteUTF8_04() throws Exception {
		fDocumentCommand.text= "\u0926 \\u0905";
		performTestUTF8();
		assertEquals("\u0926 \\u0905", fDocumentCommand.text);
	}

	public void testPasteUTF8_05() throws Exception {
		fDocumentCommand.text= "\u0926 \\some text";
		performTestUTF8();
		assertEquals("\u0926 \\\\some text", fDocumentCommand.text);
	}

	//paste from properties file ISO-8859-1
	public void testPasteFromPropertiesFileISO_8859_1_01() throws Exception {
		fDocumentCommand.text= "\t \n \f \r";
		performTestISO_8859_1();
		assertEquals("\t \n \f \r", fDocumentCommand.text);
	}

	public void testPasteFromPropertiesFileISO_8859_1_02() throws Exception {
		fDocumentCommand.text= "\\u00e4 \\t \\u0926 \\n";
		performTestISO_8859_1();
		assertEquals("\\u00e4 \\t \\u0926 \\n", fDocumentCommand.text);
	}

	public void testPasteFromPropertiesFileISO_8859_1_03() throws Exception {
		fDocumentCommand.text= "C:\\\\Program Files\\\\Java";
		performTestISO_8859_1();
		assertEquals("C:\\\\Program Files\\\\Java", fDocumentCommand.text);
	}

	public void testPasteFromPropertiesFileISO_8859_1_04() throws Exception {
		fDocumentCommand.text= "C:\\\\new folder\\\\A.java";
		performTestISO_8859_1();
		assertEquals("C:\\\\new folder\\\\A.java", fDocumentCommand.text);
	}

	public void testPasteFromPropertiesFileISO_8859_1_05() throws Exception {
		fDocumentCommand.text= "\\u2603 \\\\u2603";
		performTestISO_8859_1();
		assertEquals("\\u2603 \\\\u2603", fDocumentCommand.text);
	}

	// paste ISO-8859-1 and PreferenceConstants.PROPERTIES_FILE_ESCAPE_BACKSLASH_ON_PASTE_IF_REQUIRED disabled
	public void testAutoEscapeSlashDisabledISO_8859_1_01() throws Exception {
		fDocumentCommand.text= "C:\\Program Files\\Java";
		performTestAutoEscapeSlashDisabledISO_8859_1();
		assertEquals("C:\\Program Files\\Java", fDocumentCommand.text);
	}

	public void testAutoEscapeSlashDisabledISO_8859_1_02() throws Exception {
		fDocumentCommand.text= "C:\\new folder\\A.java";
		performTestAutoEscapeSlashDisabledISO_8859_1();
		assertEquals("C:\\new folder\\A.java", fDocumentCommand.text);
	}

	public void testAutoEscapeSlashDisabledISO_8859_1_03() throws Exception {
		fDocumentCommand.text= "\u0926 \\u0905";
		performTestAutoEscapeSlashDisabledISO_8859_1();
		assertEquals("\\u0926 \\u0905", fDocumentCommand.text);
	}

	//paste UTF-8 and PreferenceConstants.PROPERTIES_FILE_ESCAPE_BACKSLASH_ON_PASTE_IF_REQUIRED disabled
	public void testAutoEscapeSlashDisabledUTF8_01() throws Exception {
		fDocumentCommand.text= "\u0926 \\some text";
		performTestAutoEscapeSlashDisabledUTF8();
		assertEquals("\u0926 \\some text", fDocumentCommand.text);
	}
	
	//change encoding of file
	public void testChangeEncodingOfFile_01() throws Exception {
		fDocumentCommand.text= "\u2603";
		performTestUTF8();
		assertEquals("\u2603", fDocumentCommand.text);
		
		fPropertiesFileUTF8.setCharset(ISO_8859_1, null);

		fDocumentCommand.text= "\u2603";
		performTestUTF8();
		assertEquals("\\u2603", fDocumentCommand.text);

		fPropertiesFileUTF8.setCharset(UTF_8, null);
	}

	public void testChangeEncodingOfFile_02() throws Exception {
		fDocumentCommand.text= "\u2603";
		performTestISO_8859_1();
		assertEquals("\\u2603", fDocumentCommand.text);
		
		fPropertiesFileISO_8859_1.setCharset(UTF_8, null);
		
		fDocumentCommand.text= "\u2603";
		performTestISO_8859_1();
		assertEquals("\u2603", fDocumentCommand.text);

		fPropertiesFileISO_8859_1.setCharset(ISO_8859_1, null);
	}
}
