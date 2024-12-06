/*******************************************************************************
 * Copyright (c) 2017, 2020 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;

public class ContinuousTypingCompletionTest extends AbstractCompletionTest {
	private final static class CompletionSelectionTracker implements ICompletionListener {
		private AbstractJavaCompletionProposal fSelectedProposal;

		@Override
		public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
			if (proposal instanceof AbstractJavaCompletionProposal p) {
				this.setSelectedProposal(p);
			}
		}

		@Override
		public void assistSessionStarted(ContentAssistEvent event) {
			// not used
		}

		@Override
		public void assistSessionEnded(ContentAssistEvent event) {
			// not used
		}

		public synchronized AbstractJavaCompletionProposal getSelectedProposal() {
			return this.fSelectedProposal;
		}

		public synchronized void setSelectedProposal(AbstractJavaCompletionProposal selectedProposal) {
			fSelectedProposal = selectedProposal;
		}
	}

	private JavaEditor fEditor;



	/*
	 * This tests https://bugs.eclipse.org/bugs/show_bug.cgi?id=511542
	 */
	@Test
	public void testContinousTypingSelectsTopProposal() throws Exception {
		String contents= "public class " + getName() + " {\n" +
				"	int ab, ba;\n" +
				"	void m() {\n" +
				"		/*COMPLETE_HERE*/\n" +
				"	}\n" +
				"}\n";
		ICompilationUnit compilationUnit= cts.getAnonymousTestPackage().createCompilationUnit(getName() + ".java", contents, true, new NullProgressMonitor());
		fEditor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		int completionOffset= contents.indexOf("/*COMPLETE_HERE*/");
		fEditor.getViewer().setSelectedRange(completionOffset, 0);
		Display display= fEditor.getSite().getShell().getDisplay();
		CompletionSelectionTracker selectionTracker= new CompletionSelectionTracker();
		((JavaSourceViewer) fEditor.getViewer()).getContentAssistantFacade().addCompletionListener(selectionTracker);
		fEditor.getAction(ITextEditorActionConstants.CONTENT_ASSIST).run();
		new DisplayHelper() {
			@Override
			public boolean condition() {
				return selectionTracker.getSelectedProposal()!=null;
			}
		}.waitForCondition(display, 10000);
		assertEquals("ab", selectionTracker.getSelectedProposal().getJavaElement().getElementName());
		IDocument document= fEditor.getViewer().getDocument();
		document.replace(completionOffset, 0, "b");
		selectionTracker.setSelectedProposal(null);
		fEditor.getViewer().setSelectedRange(completionOffset + 1, 0);
		new DisplayHelper() {
			@Override
			public boolean condition() {
				return selectionTracker.getSelectedProposal()!=null;
			}
		}.waitForCondition(display, 10000);
		assertEquals("ba", selectionTracker.getSelectedProposal().getJavaElement().getElementName());
	}

	@Override
	public void tearDown() throws Exception {
		if (fEditor != null) {
			fEditor.close(false);
			fEditor= null;
		}
		super.tearDown();
	}
}
