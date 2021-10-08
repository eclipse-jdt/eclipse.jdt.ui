/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial code based on SurroundWithTryCatchRefactoring
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.surround;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;

/**
 * Surround a set of statements with a try-with-resources block.
 *
 * Special case:
 *
 * URL url= file.toURL();
 *
 * In this case the variable declaration statement gets convert into a
 * declaration without initializer. So the body of the try/catch block
 * only consists of new assignments. In this case we can't move the
 * selected nodes (e.g. the declaration) into the try block.
 */
public class SurroundWithTryWithResourcesRefactoring extends SurroundWithTryWithResourcesRefactoringCore {

	private SurroundWithTryWithResourcesRefactoring(ICompilationUnit cu, Selection selection) {
		super(cu, selection);
	}

	public static SurroundWithTryWithResourcesRefactoring create(ICompilationUnit cu, int offset, int length) {
		return new SurroundWithTryWithResourcesRefactoring(cu, Selection.createFromStartLength(offset, length));
	}

	public static SurroundWithTryWithResourcesRefactoring create(ICompilationUnit cu, ITextSelection selection) {
		return new SurroundWithTryWithResourcesRefactoring(cu, Selection.createFromStartLength(selection.getOffset(), selection.getLength()));
	}

	@Override
	protected LinkedProposalModelCore createLinkedProposalModel() {
		return new LinkedProposalModel();
	}

	public LinkedProposalModel getLinkedProposalModel() {
		return (LinkedProposalModel)getLinkedProposalModelCore();
	}

}
