/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring.MultiFixTarget;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.refactoring.IRefactoringSaveModes;
import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.IMultiFix;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IStatusLineProposal;
import org.eclipse.jdt.internal.ui.viewsupport.ImageImageDescriptor;

/**
 * A correction proposal which uses an {@link ICleanUpFix} to fix a problem. A fix correction
 * proposal may have an {@link ICleanUp} attached which can be executed instead of the provided
 * IFix.
 */
public class FixCorrectionProposal extends LinkedCorrectionProposal implements ICompletionProposalExtension2, IStatusLineProposal {

	private ICleanUp fCleanUp;

	public FixCorrectionProposal(IProposableFix fix, ICleanUp cleanUp, int relevance, Image image, IInvocationContext context) {
		super(fix.getDisplayString(), context.getCompilationUnit(), null, relevance, image, new FixCorrectionProposalCore(fix, cleanUp, relevance, context));
		this.fCleanUp= cleanUp;
	}

	public FixCorrectionProposal(IProposableFix fix, ICleanUp cleanUp, int relevance, Image image, IInvocationContext context, FixCorrectionProposalCore delegate) {
		super(fix.getDisplayString(), context.getCompilationUnit(), null, relevance, image, delegate);
		this.fCleanUp= cleanUp;
	}

	public void resolve(MultiFixTarget[] targets, final IProgressMonitor monitor) throws CoreException {
		if (targets.length == 0)
			return;

		if (fCleanUp == null)
			return;

		String changeName;
		String[] descriptions= fCleanUp.getStepDescriptions();
		if (descriptions.length == 1) {
			changeName= descriptions[0];
		} else {
			changeName= CorrectionMessages.FixCorrectionProposal_MultiFixChange_label;
		}

		final CleanUpRefactoring refactoring= new CleanUpRefactoring(changeName);
		for (MultiFixTarget target : targets) {
			refactoring.addCleanUpTarget(target);
		}

		refactoring.addCleanUp(fCleanUp);

		IRunnableContext context= (fork, cancelable, runnable) -> runnable.run(monitor == null ? new NullProgressMonitor() : monitor);

		Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		RefactoringExecutionHelper helper= new RefactoringExecutionHelper(refactoring, IStatus.INFO, IRefactoringSaveModes.SAVE_REFACTORING, shell, context);
		try {
			helper.perform(true, true);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			Throwable cause= e.getCause();
			if (cause instanceof CoreException) {
				throw (CoreException) cause;
			} else {
				throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, cause.getLocalizedMessage(), cause));
			}
		}
	}

	public ICleanUp getCleanUp() {
		return fCleanUp;
	}

	public IStatus getFixStatus() {
		return ((FixCorrectionProposalCore) getDelegate()).getFixStatus();
	}

	@Override
	public Image getImage() {
		FixCorrectionProposalCore d= ((FixCorrectionProposalCore) getDelegate());
		IStatus status= d == null ? Status.OK_STATUS : d.getFixStatus();
		if (status != null && !status.isOK()) {
			ImageImageDescriptor image= new ImageImageDescriptor(super.getImage());

			int flag= JavaElementImageDescriptor.WARNING;
			if (status.getSeverity() == IStatus.ERROR) {
				flag= JavaElementImageDescriptor.ERROR;
			} else if (status.getSeverity() == IStatus.INFO) {
				flag= JavaElementImageDescriptor.INFO;
			}

			ImageDescriptor composite= new JavaElementImageDescriptor(image, flag, new Point(image.getImageData().width, image.getImageData().height));
			return composite.createImage();
		} else {
			return super.getImage();
		}
	}

	@Override
	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	@Override
	public void unselected(ITextViewer viewer) {
	}

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return false;
	}

	@Override
	public int getRelevance() {
		return ((FixCorrectionProposalCore) getDelegate()).getRelevance();
	}

	@Override
	public String getStatusMessage() {
		if (fCleanUp == null) {
			return null;
		}
		int count= computeNumberOfFixesForCleanUp(fCleanUp);

		if (count == -1) {
			return CorrectionMessages.FixCorrectionProposal_HitCtrlEnter_description;
		} else if (count < 2) {
			return null;
		} else {
			return Messages.format(CorrectionMessages.FixCorrectionProposal_hitCtrlEnter_variable_description, Integer.valueOf(count));
		}
	}

	/**
	 * Compute the number of problems that can be fixed by the clean up in a compilation unit.
	 *
	 * @param cleanUp the clean up
	 * @return the maximum number of fixes or -1 if unknown
	 * @since 3.6
	 */
	public int computeNumberOfFixesForCleanUp(ICleanUp cleanUp) {
		CompilationUnit cu= ((FixCorrectionProposalCore) getDelegate()).getAstCompilationUnit();
		return cleanUp instanceof IMultiFix ? ((IMultiFix) cleanUp).computeNumberOfFixes(cu) : -1;
	}

	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		if ((stateMask & SWT.MODIFIER_MASK) == SWT.CONTROL && fCleanUp != null) {
			CleanUpRefactoring refactoring= new CleanUpRefactoring();
			refactoring.addCompilationUnit(getCompilationUnit());
			refactoring.addCleanUp(fCleanUp);
			refactoring.setLeaveFilesDirty(true);

			int stopSeverity= RefactoringCore.getConditionCheckingFailedSeverity();
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			IRunnableContext context= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			RefactoringExecutionHelper executer= new RefactoringExecutionHelper(refactoring, stopSeverity, IRefactoringSaveModes.SAVE_NOTHING, shell, context);
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
}
