/*******************************************************************************
 * Copyright (c) 2017, 2026 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *     Red Hat Ltd - refactor parts to CreatePackageInfoWithNullnessProposalCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring.MultiFixTarget;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoringCore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

public final class CreatePackageInfoWithDefaultNullnessProposal extends ChangeCorrectionProposal {

	private CreatePackageInfoWithDefaultNullnessProposalCore fCore;
	public CreatePackageInfoWithDefaultNullnessProposal(CreatePackageInfoWithDefaultNullnessProposalCore core) {
		super(core.getName(), null, core.getRelevance());
		fCore = core;
	}

	@Override
	protected Change createChange() throws CoreException {
		return fCore.getChange();
	}
	public CreatePackageInfoWithDefaultNullnessProposal(int problemId, String name, Change change, int relevance, ICompilationUnit unit) {
		super(name, change, relevance);
		fCore= new CreatePackageInfoWithDefaultNullnessProposalCore(problemId, name, change, relevance, unit);
	}

	@Override
	public void apply(org.eclipse.jface.text.IDocument document) {
		super.apply(document);
		IEditorPart part= null;
		if (fCore.fUnit.getResource().exists()) {
			part= EditorUtility.isOpenInEditor(fCore.fUnit);
			if (part == null) {
				try {
					part= JavaUI.openInEditor(fCore.fUnit);
				} catch (PartInitException | JavaModelException e) {
					return;
				}
			}
			IWorkbenchPage page= JavaPlugin.getActivePage();
			if (page != null && part != null) {
				page.bringToTop(part);
			}
			if (part != null) {
				part.setFocus();
			}
		}
	}

	public int getProblemId() {
		return fCore.fProblemId;
	}

	public void resolve(MultiFixTarget[] problems, IProgressMonitor monitor) throws CoreException {
		List<CleanUpRefactoringCore.MultiFixTarget> coreProblems= new ArrayList<>();
		for (MultiFixTarget problem : problems) {
			coreProblems.add(new CleanUpRefactoringCore.MultiFixTarget(problem.getCompilationUnit(), problem.getProblems()));
		}
		fCore.resolve(coreProblems.toArray(new CleanUpRefactoringCore.MultiFixTarget[0]), monitor);
	}
}
