/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * PropertiesFileAutoEditStratergyTest
 *
 * @since 3.7
 */
public class PropertiesFileAutoEditStrategyTest {
	private static final String UTF_8= "UTF-8";
	private static final String ISO_8859_1= "ISO-8859-1";

	private final DocumentCommand fDocumentCommand;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private IFile fPropertiesFileISO_8859_1;
	private IFile fPropertiesFileUTF8;

	private Accessor fAccessorPropertiesFileAutoEditStrategyISO_8859_1;

	private Accessor fAccessorPropertiesFileUTF8;

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	public PropertiesFileAutoEditStrategyTest() {
		fDocumentCommand= new DocumentCommand() {
		};
	}


	@Before
	public void setUp() throws Exception {
		try {
			fJProject1= pts.getProject();
			fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
			IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
			fPropertiesFileISO_8859_1= write((IFolder)pack.getCorrespondingResource(), "", "Messages_ISO_8859_1.properties");
			fPropertiesFileISO_8859_1.setCharset(ISO_8859_1, null);
			fPropertiesFileUTF8= write((IFolder)pack.getCorrespondingResource(), "", "Messages_UTF8.properties");
			fPropertiesFileUTF8.setCharset(UTF_8, null);

			fAccessorPropertiesFileAutoEditStrategyISO_8859_1= new Accessor("org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileAutoEditStrategy", getClass().getClassLoader(),
					new Class[] { IFile.class, ISourceViewer.class }, new Object[] { fPropertiesFileISO_8859_1, null });

			fAccessorPropertiesFileUTF8= new Accessor("org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileAutoEditStrategy", getClass().getClassLoader(), new Class[] { IFile.class,
					ISourceViewer.class }, new Object[] { fPropertiesFileUTF8, null });

		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	//utility methods
	private Object performTestISO_8859_1() {
		return fAccessorPropertiesFileAutoEditStrategyISO_8859_1.invoke("escape", new Class[] { DocumentCommand.class }, new Object[] { fDocumentCommand });
	}

	private Object performTestUTF8() {
		return fAccessorPropertiesFileUTF8.invoke("escape", new Class[] { DocumentCommand.class }, new Object[] { fDocumentCommand });
	}

	private IFile write(IFolder folder, final String content, final String fileName) throws CoreException {
		ByteArrayInputStream stream= new ByteArrayInputStream(content.getBytes());
		IFile file= fJProject1.getProject().getFile(folder.getProjectRelativePath().append(fileName));
		file.create(stream, true, null);
		return file;
	}

	private String getProposalText(Object proposal) {
		String proposalInfo= ((ICompletionProposal)proposal).getAdditionalProposalInfo();
		proposalInfo= proposalInfo.substring(5, proposalInfo.length() - 6); //remove <pre> and </pre> tags
		return proposalInfo;
	}

	//typing ISO-8859-1
	@Test
	public void testTypeISO_8859_1_01() throws Exception {
		fDocumentCommand.text= "\\";
		Object proposal= performTestISO_8859_1();
		assertEquals("\\", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testTypeISO_8859_1_02() throws Exception {
		fDocumentCommand.text= "\t";
		Object proposal= performTestISO_8859_1();
		assertEquals("\t", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testTypeISO_8859_1_03() throws Exception {
		fDocumentCommand.text= "\f";
		Object proposal= performTestISO_8859_1();
		assertEquals("\f", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testTypeISO_8859_1_04() throws Exception {
		fDocumentCommand.text= "\r";
		Object proposal= performTestISO_8859_1();
		assertEquals("\r", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testTypeISO_8859_1_05() throws Exception {
		fDocumentCommand.text= "\n";
		Object proposal= performTestISO_8859_1();
		assertEquals("\n", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testTypeISO_8859_1_06() throws Exception {
		fDocumentCommand.text= "\u2603";
		Object proposal= performTestISO_8859_1();
		assertEquals("\\u2603", fDocumentCommand.text);
		assertNull(proposal);
	}

	//typing UTF-8
	@Test
	public void testTypeUTF8_01() throws Exception {
		fDocumentCommand.text= "\u2603";
		Object proposal= performTestUTF8();
		assertEquals("\u2603", fDocumentCommand.text);
		assertNull(proposal);
	}

	//paste ISO-8859-1
	@Test
	public void testPasteISO_8859_1_01() throws Exception {
		fDocumentCommand.text= "C:\\Program Files\\Java";
		Object proposal= performTestISO_8859_1();
		assertEquals("C:\\Program Files\\Java", fDocumentCommand.text);
		assertEquals("C:\\\\Program Files\\\\Java", getProposalText(proposal));
	}

	@Test
	public void testPasteISO_8859_1_02() throws Exception {
		fDocumentCommand.text= "C:\\new folder\\A.java";
		Object proposal= performTestISO_8859_1();
		assertEquals("C:\\new folder\\A.java", fDocumentCommand.text);
		assertEquals("C:\\\\new folder\\\\A.java", getProposalText(proposal));
	}

	@Test
	public void testPasteISO_8859_1_03() throws Exception {
		fDocumentCommand.text= "\u0926 \u0905";
		Object proposal= performTestISO_8859_1();
		assertEquals("\\u0926 \\u0905", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteISO_8859_1_04() throws Exception {
		fDocumentCommand.text= "\u0926 \\u0905";
		Object proposal= performTestISO_8859_1();
		assertEquals("\\u0926 \\u0905", fDocumentCommand.text);
		assertEquals("\\u0926 \\\\u0905", getProposalText(proposal));
	}

	@Test
	public void testPasteISO_8859_1_05() throws Exception {
		fDocumentCommand.text= "ä \u0926";
		Object proposal= performTestISO_8859_1();
		assertEquals("ä \\u0926", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteISO_8859_1_06() throws Exception {
		fDocumentCommand.text= "some text\\";
		Object proposal= performTestISO_8859_1();
		assertEquals("some text\\", fDocumentCommand.text);
		assertEquals("some text\\\\", getProposalText(proposal));
	}

	//paste UTF-8
	@Test
	public void testPasteUTF8_01() throws Exception {
		fDocumentCommand.text= "C:\\Program Files\\Java";
		Object proposal= performTestUTF8();
		assertEquals("C:\\Program Files\\Java", fDocumentCommand.text);
		assertEquals("C:\\\\Program Files\\\\Java", getProposalText(proposal));
	}

	@Test
	public void testPasteUTF8_02() throws Exception {
		fDocumentCommand.text= "C:\\new folder\\A.java";
		Object proposal= performTestUTF8();
		assertEquals("C:\\new folder\\A.java", fDocumentCommand.text);
		assertEquals("C:\\\\new folder\\\\A.java", getProposalText(proposal));
	}

	@Test
	public void testPasteUTF8_03() throws Exception {
		fDocumentCommand.text= "\u0926 \u0905";
		Object proposal= performTestUTF8();
		assertEquals("\u0926 \u0905", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteUTF8_04() throws Exception {
		fDocumentCommand.text= "\u0926 \\u0905";
		Object proposal= performTestUTF8();
		assertEquals("\u0926 \\u0905", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteUTF8_05() throws Exception {
		fDocumentCommand.text= "\u0926 \\some text";
		Object proposal= performTestUTF8();
		assertEquals("\u0926 \\some text", fDocumentCommand.text);
		assertEquals("\u0926 \\\\some text", getProposalText(proposal));
	}

	//paste from properties file ISO-8859-1
	@Test
	public void testPasteFromPropertiesFileISO_8859_1_01() throws Exception {
		fDocumentCommand.text= "\t \n \f \r";
		Object proposal= performTestISO_8859_1();
		assertEquals("\t \n \f \r", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteFromPropertiesFileISO_8859_1_02() throws Exception {
		fDocumentCommand.text= "\\u00e4 \\t \\u0926 \\n";
		Object proposal= performTestISO_8859_1();
		assertEquals("\\u00e4 \\t \\u0926 \\n", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteFromPropertiesFileISO_8859_1_03() throws Exception {
		fDocumentCommand.text= "C:\\\\Program Files\\\\Java";
		Object proposal= performTestISO_8859_1();
		assertEquals("C:\\\\Program Files\\\\Java", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteFromPropertiesFileISO_8859_1_04() throws Exception {
		fDocumentCommand.text= "C:\\\\new folder\\\\A.java";
		Object proposal= performTestISO_8859_1();
		assertEquals("C:\\\\new folder\\\\A.java", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteFromPropertiesFileISO_8859_1_05() throws Exception {
		fDocumentCommand.text= "\\u2603 \\\\u2603";
		Object proposal= performTestISO_8859_1();
		assertEquals("\\u2603 \\\\u2603", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteFromPropertiesFileISO_8859_1_06() throws Exception {
		fDocumentCommand.text= "key=value\\\nsecond line";
		Object proposal= performTestISO_8859_1();
		assertEquals("key=value\\\nsecond line", fDocumentCommand.text);
		assertNull(proposal);
	}

	@Test
	public void testPasteFromPropertiesFileISO_8859_1_07() throws Exception {
		fDocumentCommand.text= "\\:\\=";
		Object proposal= performTestISO_8859_1();
		assertEquals("\\:\\=", fDocumentCommand.text);
		assertNull(proposal);
	}

	//change encoding of file
	@Test
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

	@Test
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
