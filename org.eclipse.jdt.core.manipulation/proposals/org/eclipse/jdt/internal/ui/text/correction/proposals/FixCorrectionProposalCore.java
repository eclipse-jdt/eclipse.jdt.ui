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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.fix.ILinkedFix;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;

import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class FixCorrectionProposalCore extends LinkedCorrectionProposalCore {

	private final IProposableFix fFix;
	private final ICleanUp fCleanUp;
	private CompilationUnit fCompilationUnit;

	public FixCorrectionProposalCore(IProposableFix fix, ICleanUp cleanUp, int relevance, IInvocationContext context) {
		super(fix.getDisplayString(), context.getCompilationUnit(), null, relevance);
		fFix= fix;
		fCleanUp= cleanUp;
		fCompilationUnit= context.getASTRoot();
	}
	public ICleanUp getCleanUp() {
		return fCleanUp;
	}
	public CompilationUnit getAstCompilationUnit() {
		return this.fCompilationUnit;
	}

	public IStatus getFixStatus() {
		return fFix.getStatus();
	}

	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		StringBuilder result= new StringBuilder();

		IStatus status= getFixStatus();
		if (status != null && !status.isOK()) {
			result.append("<b>"); //$NON-NLS-1$
			if (status.getSeverity() == IStatus.WARNING) {
				result.append(CorrectionMessages.FixCorrectionProposal_WarningAdditionalProposalInfo);
			} else if (status.getSeverity() == IStatus.ERROR) {
				result.append(CorrectionMessages.FixCorrectionProposal_ErrorAdditionalProposalInfo);
			}
			result.append("</b>"); //$NON-NLS-1$
			result.append(status.getMessage());
			result.append("<br><br>"); //$NON-NLS-1$
		}

		String info= fFix.getAdditionalProposalInfo();
		if (info != null) {
			result.append(info);
		} else {
			result.append(super.getAdditionalProposalInfo(monitor));
		}

		return result.toString();
	}

	@Override
	public int getRelevance() {
		IStatus status= getFixStatus();
		if (status != null && !status.isOK()) {
			return super.getRelevance() - 100;
		} else {
			return super.getRelevance();
		}
	}

	@Override
	public TextChange createTextChange() throws CoreException {
		CompilationUnitChange createChange= fFix.createChange(null);
		createChange.setSaveMode(TextFileChange.LEAVE_DIRTY);

		if (fFix instanceof ILinkedFix) {
			setLinkedProposalModel(((ILinkedFix) fFix).getLinkedPositions());
		}

		return createChange;
	}
}