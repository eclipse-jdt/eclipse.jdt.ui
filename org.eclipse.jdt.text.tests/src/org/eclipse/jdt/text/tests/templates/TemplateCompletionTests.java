/*******************************************************************************
 * Copyright (c) 2019, 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.jdt.text.tests.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.SWT;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.java.TemplateCompletionProposalComputer;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal;

public class TemplateCompletionTests {

	private IJavaProject fJProject;

	private IPackageFragmentRoot javaSrc;

	private IPackageFragment pkg;

	@Before
	public void setUp() throws Exception {
		fJProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar18(fJProject);
		javaSrc= JavaProjectHelper.addSourceContainer(fJProject, "src");
		pkg= javaSrc.createPackageFragment("test", false, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);
	}

	@Test
	public void testEmptyFile() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("   $");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "EmptyFile.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("new_class - create new class"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "new_class", completionIndex);
		String lineSeparator= System.lineSeparator();
		StringBuffer expected= new StringBuffer();
		expected.append("   package test;" + lineSeparator +
				lineSeparator +
				"public class EmptyFile  {" + lineSeparator +
				lineSeparator +
				"}");

		assertEquals(expected.toString(), viewer.getDocument().get());
	}

	@Test
	public void testExepectNoProposals() throws Exception {
		String propDisplay= "new_class - create new class";
		StringBuffer buf= new StringBuffer();
		buf.append("package test;$");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "ExpectNoProposals.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		boolean fail= proposals.stream().anyMatch(p -> "new_class - create new class".equals(p.getDisplayString()));
		if (fail) {
			fail("Proposal '" + propDisplay + "' should not exist");
		}
	}

	@Test
	public void testRegressionBug574267() throws Exception {
		String propDisplay= "sysout - print to standard out";
		StringBuffer buf= new StringBuffer();
		buf.append("""
			class Sample {
				void sample(String foo) {
					if (foo != null) {
						sys$ // content assist here
						System.out.println();
					}
				}
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Sample.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList(propDisplay), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "sysout", completionIndex);

		String str= """
			class Sample {
				void sample(String foo) {
					if (foo != null) {
						System.out.println(); // content assist here
						System.out.println();
					}
				}
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	private ITextViewer initializeViewer(ICompilationUnit cu) throws Exception {
		IEditorPart editor= EditorUtility.openInEditor(cu);
		ITextViewer viewer= new TextViewer(editor.getSite().getShell(), SWT.NONE);
		viewer.setDocument(new Document(cu.getSource()));
		return viewer;
	}

	private ICompilationUnit getCompilationUnit(IPackageFragment pack, StringBuffer buf, String name) throws JavaModelException {
		return pack.createCompilationUnit(name, buf.toString().replace("$", ""), false, null);
	}

	private int getCompletionIndex(StringBuffer buf) {
		return buf.toString().indexOf('$');
	}

	private List<ICompletionProposal> computeCompletionProposals(ICompilationUnit cu, int completionIndex) throws Exception {
		TemplateCompletionProposalComputer comp= new TemplateCompletionProposalComputer();

		IEditorPart editor= EditorUtility.openInEditor(cu);
		ITextViewer viewer= new TextViewer(editor.getSite().getShell(), SWT.NONE);
		viewer.setDocument(new Document(cu.getSource()));
		JavaContentAssistInvocationContext ctx= new JavaContentAssistInvocationContext(viewer, completionIndex, editor);

		return comp.computeCompletionProposals(ctx, null);
	}

	private void assertProposalsExist(List<String> expected, List<ICompletionProposal> proposals) {
		for (String propDisplay : expected) {
			assertTrue(proposals.stream().anyMatch(p -> propDisplay.equals(p.getDisplayString())));
		}
	}

	private void applyProposal (ITextViewer viewer, List<ICompletionProposal> proposals, String name, int offset) throws Exception {
		TemplateProposal proposal= (TemplateProposal) proposals.stream().filter(p -> ((TemplateProposal)p).getTemplate().getName().equals(name)).findFirst().get();
		proposal.apply(viewer, '0', -1, offset);
	}

}
