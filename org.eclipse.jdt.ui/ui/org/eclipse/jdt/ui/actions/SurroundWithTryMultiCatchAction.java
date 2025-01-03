/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * Action to surround a set of statements with a try/multi-catch block.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 3.7.1
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class SurroundWithTryMultiCatchAction extends SurroundWithTryCatchAction {

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the compilation unit editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public SurroundWithTryMultiCatchAction(CompilationUnitEditor editor) {
		super(editor);
		setText(RefactoringMessages.SurroundWithTryMultiCatchAction_label);
	}

	@Override
	SurroundWithTryCatchRefactoring createRefactoring(ITextSelection selection, ICompilationUnit cu) {
		return SurroundWithTryCatchRefactoring.create(cu, selection.getOffset(), selection.getLength(), true);
	}

	@Override
	String getDialogTitle() {
		return RefactoringMessages.SurroundWithTryMultiCatchAction_dialog_title;
	}
}
