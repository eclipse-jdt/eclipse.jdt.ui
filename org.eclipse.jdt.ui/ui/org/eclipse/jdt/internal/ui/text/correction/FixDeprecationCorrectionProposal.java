/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.corext.refactoring.deprecation.DeprecationRefactorings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.deprecation.FixDeprecationRefactoringWizard;
import org.eclipse.jdt.internal.ui.text.HTMLPrinter;

/**
 * Correction proposal to fix a deprecation.
 * 
 * @since 3.2
 */
public final class FixDeprecationCorrectionProposal extends ChangeCorrectionProposal {

	/** The height of the wizard */
	private static final int SIZING_WIZARD_HEIGHT= 470;

	/** The width of the wizard */
	private static final int SIZING_WIZARD_WIDTH= 490;

	/** The deprecated binding */
	private final IBinding fBinding;

	/** The invocation context */
	private final IInvocationContext fContext;

	/** The refactoring history to execute */
	private final RefactoringHistory fHistory;

	/** The triggering ast node */
	private final ASTNode fNode;

	/**
	 * Creates a new fix deprecation correction proposal.
	 * 
	 * @param context
	 *            the invocation context
	 * @param history
	 *            the refactoring history to execute
	 * @param binding
	 *            the binding to the deprecated member
	 * @param node
	 *            the node which triggered this proposal
	 */
	public FixDeprecationCorrectionProposal(final IInvocationContext context, final RefactoringHistory history, final IBinding binding, final ASTNode node) {
		super(CorrectionMessages.QuickAssistProcessor_fix_deprecation_name, null, 100, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		fContext= context;
		fHistory= history;
		fBinding= binding;
		fNode= node;
	}

	/**
	 * {@inheritDoc}
	 */
	public void apply(final IDocument document) {
		if (fHistory.isEmpty())
			return;
		final FixDeprecationRefactoringWizard wizard= new FixDeprecationRefactoringWizard(fHistory.getDescriptors().length > 1, fContext.getCompilationUnit(), fNode.getStartPosition(), fNode.getLength());
		final WizardDialog dialog= new WizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard);
		wizard.setRefactoringHistory(fHistory);
		final IPackageFragmentRoot root= DeprecationRefactorings.getPackageFragmentRoot(fBinding);
		if (root != null)
			wizard.setPackageFragmentRoot(root);
		dialog.create();
		dialog.getShell().setSize(Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x), SIZING_WIZARD_HEIGHT);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IJavaHelpContextIds.FIX_DEPRECATION_WIZARD_PAGE);
		dialog.getShell().getDisplay().asyncExec(new Runnable() {

			public final void run() {
				dialog.showPage(wizard.getNextPage(wizard.getStartingPage()));
			}
		});
		dialog.open();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAdditionalProposalInfo() {
		if (fHistory.isEmpty())
			return ""; //$NON-NLS-1$
		final StringBuffer buffer= new StringBuffer(512);
		HTMLPrinter.startBulletList(buffer);
		final RefactoringDescriptorProxy[] proxies= fHistory.getDescriptors();
		for (int index= 0; index < proxies.length; index++) {
			HTMLPrinter.addBullet(buffer, proxies[index].getDescription());
		}
		HTMLPrinter.endBulletList(buffer);
		return Messages.format(CorrectionMessages.QuickAssistProcessor_fix_deprecation_info, buffer.toString());
	}
}