/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.RefactoringCorrectionProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.InsertEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;

public class RefactoringCorrectionProposalCore extends LinkedCorrectionProposalCore {

	private final Refactoring fRefactoring;
	private RefactoringStatus fRefactoringStatus;

	public RefactoringCorrectionProposalCore(String name, ICompilationUnit cu, Refactoring refactoring, int relevance) {
		super(name, cu, null, relevance);
		fRefactoring= refactoring;
	}

	public Refactoring getRefactoring() {
		return fRefactoring;
	}

	/**
	 * Can be overridden by clients to perform expensive initializations of the refactoring
	 *
	 * @param refactoring the refactoring
	 * @throws CoreException if something goes wrong during init
	 */
	protected void init(Refactoring refactoring) throws CoreException {
		// empty default implementation
	}

	@Override
	public TextChange createTextChange() throws CoreException {
		init(fRefactoring);
		fRefactoringStatus= fRefactoring.checkFinalConditions(new NullProgressMonitor());
		if (fRefactoringStatus.hasFatalError()) {
			TextFileChange dummyChange= new TextFileChange("fatal error", (IFile) getCompilationUnit().getResource()); //$NON-NLS-1$
			dummyChange.setEdit(new InsertEdit(0, "")); //$NON-NLS-1$
			return dummyChange;
		}
		Change o = fRefactoring.createChange(new NullProgressMonitor());
		if(o instanceof TextChange)
			return (TextChange) o;
		if( o instanceof CompositeChange) {
			CompositeChange cc = (CompositeChange)o;
			Change[] children = cc.getChildren();
			if( children != null && children.length == 1 && children[0] instanceof TextChange) {
				return ((TextChange)children[0]);
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.proposals.CUCorrectionProposal#getAdditionalProposalInfo(org.eclipse.core.runtime.IProgressMonitor)
	 * @since 3.6
	 */
	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		if (fRefactoringStatus != null && fRefactoringStatus.hasFatalError()) {
			return fRefactoringStatus.getEntryWithHighestSeverity().getMessage();
		}
		return super.getAdditionalProposalInfo(monitor);
	}
}