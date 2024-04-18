/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

public class AddModuleRequiresCorrectionProposal extends ChangeCorrectionProposal {
	private AddModuleRequiresCorrectionProposalCore fCore;
	public AddModuleRequiresCorrectionProposal(AddModuleRequiresCorrectionProposalCore core) {
		super(core.getName(), null, core.getRelevance());
		fCore = core;
	}
	public AddModuleRequiresCorrectionProposal(String moduleName, String changeName, String changeDescription, ICompilationUnit moduleCu, int relevance) {
		super(changeName, null, relevance);
		fCore = new AddModuleRequiresCorrectionProposalCore(moduleName, changeName, changeDescription, moduleCu, relevance);
	}

	@Override
	protected Change createChange() throws CoreException {
		return fCore.getChange();
	}

	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		return fCore.getChangeDescription();
	}

	/**
	 * Returns the list of package fragments for the matching types based on a given string pattern. The
	 * remaining parameters are used to narrow down the type of expected results.
	 *
	 * @param stringPattern the given pattern
	 * @param typeRule determines the nature of the searched elements
	 * @param javaElement limits the search scope to this element
	 * @return list of package fragments for the matching types
	 */
	public static List<IPackageFragment> getPackageFragmentsOfMatchingTypes(String stringPattern, int typeRule, IJavaElement javaElement) {
		return AddModuleRequiresCorrectionProposalCore.getPackageFragmentsOfMatchingTypesImpl(stringPattern, typeRule, javaElement);
	}
}
