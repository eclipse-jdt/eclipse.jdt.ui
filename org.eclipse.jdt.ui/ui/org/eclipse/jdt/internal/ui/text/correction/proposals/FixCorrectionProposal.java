/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.ILinkedFix;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.fix.IMultiFix;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IStatusLineProposal;
import org.eclipse.jdt.internal.ui.viewsupport.ImageImageDescriptor;

/**
 * A correction proposal which uses an {@link IFix} to
 * fix a problem. A fix correction proposal may have an {@link ICleanUp}
 * attached which can be executed instead of the provided IFix.
 */
public class FixCorrectionProposal extends LinkedCorrectionProposal implements ICompletionProposalExtension2, IStatusLineProposal {

	private final IProposableFix fFix;
	private final ICleanUp fCleanUp;
	private CompilationUnit fCompilationUnit;

	public FixCorrectionProposal(IProposableFix fix, ICleanUp cleanUp, int relevance, Image image, IInvocationContext context) {
		super(fix.getDisplayString(), context.getCompilationUnit(), null, relevance, image);
		fFix= fix;
		fCleanUp= cleanUp;
		fCompilationUnit= context.getASTRoot();
	}

	public ICleanUp getCleanUp() {
		return fCleanUp;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal#getImage()
	 */
	public Image getImage() {
		IStatus status= fFix.getStatus();
		if (status != null && !status.isOK()) {
			ImageImageDescriptor image= new ImageImageDescriptor(super.getImage());

			int flag= JavaElementImageDescriptor.WARNING;
			if (status.getSeverity() == IStatus.ERROR) {
				flag= JavaElementImageDescriptor.ERROR;
			}

			ImageDescriptor composite= new JavaElementImageDescriptor(image, flag, new Point(image.getImageData().width, image.getImageData().height));
			return composite.createImage();
		} else {
			return super.getImage();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer result= new StringBuffer();

		IStatus status= fFix.getStatus();
		if (status != null && !status.isOK()) {
			result.append("<b>"); //$NON-NLS-1$
			if (status.getSeverity() == IStatus.WARNING) {
				result.append(CorrectionMessages.FixCorrectionProposal_WarningAdditionalProposalInfo);
			} else if (status.getSeverity() == IStatus.ERROR) {
				result.append(CorrectionMessages.FixCorrectionProposal_0);
			}
			result.append("</b>"); //$NON-NLS-1$
			result.append(status.getMessage());
			result.append("<br><br>"); //$NON-NLS-1$
		}

		String info= fFix.getAdditionalProposalInfo();
		if (info != null) {
			result.append(info);
		} else {
			result.append(super.getAdditionalProposalInfo());
		}

		return result.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal#getRelevance()
	 */
	public int getRelevance() {
		IStatus status= fFix.getStatus();
		if (status != null && !status.isOK()) {
			return super.getRelevance() - 100;
		} else {
			return super.getRelevance();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#createTextChange()
	 */
	protected TextChange createTextChange() throws CoreException {
		CompilationUnitChange createChange= fFix.createChange();
		createChange.setSaveMode(TextFileChange.LEAVE_DIRTY);

		if (fFix instanceof ILinkedFix) {
			setLinkedProposalModel(((ILinkedFix) fFix).getLinkedPositions());
		}

		return createChange;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		if (stateMask == SWT.CONTROL && fCleanUp != null){
			CleanUpRefactoring refactoring= new CleanUpRefactoring();
			refactoring.addCompilationUnit(getCompilationUnit());
			refactoring.addCleanUp(fCleanUp);
			refactoring.setLeaveFilesDirty(true);

			int stopSeverity= RefactoringCore.getConditionCheckingFailedSeverity();
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			ProgressMonitorDialog context= new ProgressMonitorDialog(shell);
			RefactoringExecutionHelper executer= new RefactoringExecutionHelper(refactoring, stopSeverity, RefactoringSaveHelper.SAVE_NOTHING, shell, context);
			try {
				executer.perform(true, true);
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				JavaPlugin.log(e);
			}
			return;
		}
		apply(viewer.getDocument());
	}

	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	public void unselected(ITextViewer viewer) {
	}

	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getStatusMessage() {
		if (fCleanUp == null)
			return null;

		int count;
		if (fCleanUp instanceof IMultiFix) {
			count= ((IMultiFix)fCleanUp).computeNumberOfFixes(fCompilationUnit);
		} else {
			count= -1;
		}

		if (count == -1) {
			return CorrectionMessages.FixCorrectionProposal_HitCtrlEnter_description;
		} else if (count < 2) {
			return ""; //$NON-NLS-1$
		} else {
			return Messages.format(CorrectionMessages.FixCorrectionProposal_hitCtrlEnter_variable_description, new Integer(count));
		}
	}
}
