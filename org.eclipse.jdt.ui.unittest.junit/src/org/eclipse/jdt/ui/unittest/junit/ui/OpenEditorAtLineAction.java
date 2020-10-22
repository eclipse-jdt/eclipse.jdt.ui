/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.unittest.junit.ui;

import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Open a test in the Java editor and reveal a given line
 */
public class OpenEditorAtLineAction extends OpenEditorAction {

	private int fLineNumber;

	public OpenEditorAtLineAction(Shell shell, String className, int line, ITestRunSession session) {
		super(shell, className, session);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.OPENEDITORATLINE_ACTION);
		fLineNumber = line;
	}

	@Override
	protected void reveal(ITextEditor textEditor) {
		if (fLineNumber >= 0) {
			try {
				IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
				textEditor.selectAndReveal(document.getLineOffset(fLineNumber - 1),
						document.getLineLength(fLineNumber - 1));
			} catch (BadLocationException x) {
				// marker refers to invalid text position -> do nothing
			}
		}
	}

	@Override
	protected IJavaElement findElement(IJavaProject project, String className) throws CoreException {
		return findType(project, className);
	}

}
