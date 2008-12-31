/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Arrays;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.examples.AddTestMarkersAction;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionAssistant;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;

public class MarkerResolutionTest extends QuickFixTest {


	private static final class TextViewerContext implements IQuickAssistInvocationContext {

		private ISourceViewer fSourceViewer;
		private int fOffset;

		TextViewerContext(ISourceViewer sourceViewer, int offset) {
			fSourceViewer= sourceViewer;
			fOffset= offset;
		}

		/*
		 * @see org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext#getOffset()
		 */
		public int getOffset() {
			return fOffset;
		}

		/*
		 * @see org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext#getLength()
		 */
		public int getLength() {
			return -1;
		}

		/*
		 * @see org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext#getSourceViewer()
		 */
		public ISourceViewer getSourceViewer() {
			return fSourceViewer;
		}
	}


	private static final Class THIS= MarkerResolutionTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	private boolean BUG_46227= true;

	public MarkerResolutionTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new MarkerResolutionTest("testQuickFixAfterModification"));
			return suite;
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}


	private IMarker createMarker(ICompilationUnit cu, int line, int offset, int len) throws CoreException {
		IFile file= (IFile) cu.getResource();
		IMarker marker= file.createMarker(AddTestMarkersAction.MARKER_TYPE);
		marker.setAttribute(IMarker.LOCATION, cu.getElementName());
		marker.setAttribute(IMarker.MESSAGE, "Test marker");
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		marker.setAttribute(IMarker.LINE_NUMBER, line);
		marker.setAttribute(IMarker.CHAR_START, offset);
		marker.setAttribute(IMarker.CHAR_END, offset + len);
		return marker;
	}


	public void testQuickFix() throws Exception {
		if (BUG_46227)
			return;

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        goo(true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		createMarker(cu, 0, 0, 7);

		IEditorPart part= JavaUI.openInEditor(cu);

		JavaEditor javaEditor= (JavaEditor) part;
		ISourceViewer viewer= javaEditor.getViewer();

		try {
			JavaCorrectionAssistant assistant= new JavaCorrectionAssistant(javaEditor);
			JavaCorrectionProcessor processor= new JavaCorrectionProcessor(assistant);
			ICompletionProposal[] proposals= processor.computeQuickAssistProposals(new TextViewerContext(viewer, 0));

			assertNumberOf("proposals", proposals.length, 1);
			assertCorrectLabels(Arrays.asList(proposals));

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());

			proposals[0].apply(doc);

			buf= new StringBuffer();
			buf.append("PACKAGE test1;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E {\n");
			buf.append("    void foo(Vector vec) {\n");
			buf.append("        goo(true);\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	public void testQuickFixAfterModification() throws Exception {
		if (BUG_46227)
			return;

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        goo(true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int markerPos= 8;
		createMarker(cu, 0, markerPos, 5);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			doc.replace(0, 0, "\n"); // insert new line

			JavaCorrectionAssistant assistant= new JavaCorrectionAssistant((ITextEditor) part);
			JavaCorrectionProcessor processor= new JavaCorrectionProcessor(assistant);
			ICompletionProposal[] proposals= processor.computeQuickAssistProposals(new TextViewerContext(null, markerPos + 1));

			assertNumberOf("proposals", proposals.length, 1);
			assertCorrectLabels(Arrays.asList(proposals));

			proposals[0].apply(doc);

			buf= new StringBuffer();
			buf.append("\n");
			buf.append("package TEST1;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E {\n");
			buf.append("    void foo(Vector vec) {\n");
			buf.append("        goo(true);\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

}
