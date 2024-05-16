/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Arrays;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionAssistant;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;

public class MarkerResolutionTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private static final class TextViewerContext implements IQuickAssistInvocationContext {

		private final ISourceViewer fSourceViewer;
		private final int fOffset;

		TextViewerContext(ISourceViewer sourceViewer, int offset) {
			fSourceViewer= sourceViewer;
			fOffset= offset;
		}

		/*
		 * @see org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext#getOffset()
		 */
		@Override
		public int getOffset() {
			return fOffset;
		}

		/*
		 * @see org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext#getLength()
		 */
		@Override
		public int getLength() {
			return -1;
		}

		/*
		 * @see org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext#getSourceViewer()
		 */
		@Override
		public ISourceViewer getSourceViewer() {
			return fSourceViewer;
		}
	}


	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	private final boolean BUG_46227= true;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
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


	@Test
	public void testQuickFix() throws Exception {
		if (BUG_46227)
			return;

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        goo(true);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

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

			String str1= """
				PACKAGE test1;
				import java.util.Vector;
				public class E {
				    void foo(Vector vec) {
				        goo(true);
				    }
				}
				""";
			assertEqualString(doc.get(), str1);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	@Test
	public void testQuickFixAfterModification() throws Exception {
		if (BUG_46227)
			return;

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        goo(true);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

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

			String str1= """
				
				package TEST1;
				import java.util.Vector;
				public class E {
				    void foo(Vector vec) {
				        goo(true);
				    }
				}
				""";
			assertEqualString(doc.get(), str1);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

}
