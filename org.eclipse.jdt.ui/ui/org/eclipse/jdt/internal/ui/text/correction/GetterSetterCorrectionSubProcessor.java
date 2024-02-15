/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldCompositeRefactoring;

import org.eclipse.jdt.ui.refactoring.IRefactoringSaveModes;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.sef.SelfEncapsulateFieldWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


public class GetterSetterCorrectionSubProcessor extends GetterSetterCorrectionBaseSubProcessor<ICommandAccess> {

	public static final String SELF_ENCAPSULATE_FIELD_ID= GetterSetterCorrectionBaseSubProcessor.SELF_ENCAPSULATE_FIELD_COMMAND_ID;

	public static class SelfEncapsulateFieldProposal extends ChangeCorrectionProposal { // public for tests

		private IField fField;
		private boolean fNoDialog;

		public SelfEncapsulateFieldProposal(int relevance, IField field) {
			super(SelfEncapsulateFieldProposalCore.getDescription(field), null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			fField= field;
			fNoDialog= false;
			setCommandId(SELF_ENCAPSULATE_FIELD_ID);
		}

		public IField getField() {
			return fField;
		}

		public void setNoDialog(boolean noDialog) {
			fNoDialog= noDialog;
		}

		// I can't find any caller for this but it is public and cannot be removed?
		public TextFileChange getChange(IFile file) throws CoreException {
			return SelfEncapsulateFieldProposalCore.getChange(fField, file);
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension5#getAdditionalProposalInfo(org.eclipse.core.runtime.IProgressMonitor)
		 * @since 3.5
		 */
		@Override
		public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
			return CorrectionMessages.GetterSetterCorrectionSubProcessor_additional_info;
		}

		@Override
		public void apply(IDocument document) {
			try {
				final SelfEncapsulateFieldCompositeRefactoring compositeRefactoring = new SelfEncapsulateFieldCompositeRefactoring(Collections.singletonList(fField));
				compositeRefactoring.getRefactorings().forEach(refactoring -> {
					refactoring.setVisibility(Flags.AccPublic);
					refactoring.setConsiderVisibility(false);//private field references are just searched in local file
				});
				if (fNoDialog) {
					IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					final RefactoringExecutionHelper helper= new RefactoringExecutionHelper(compositeRefactoring, RefactoringStatus.ERROR, IRefactoringSaveModes.SAVE_REFACTORING, JavaPlugin.getActiveWorkbenchShell(), window);
					if (Display.getCurrent() != null) {
						try {
							helper.perform(false, false);
						} catch (InterruptedException | InvocationTargetException e) {
							JavaPlugin.log(e);
						}
					} else {
						Display.getDefault().syncExec(() -> {
							try {
								helper.perform(false, false);
							} catch (InterruptedException | InvocationTargetException e) {
								JavaPlugin.log(e);
							}
						});
					}
				} else {
					new RefactoringStarter().activate(new SelfEncapsulateFieldWizard(compositeRefactoring, Collections.singletonList(fField)), JavaPlugin.getActiveWorkbenchShell(), "", IRefactoringSaveModes.SAVE_REFACTORING); //$NON-NLS-1$
				}
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, CorrectionMessages.GetterSetterCorrectionSubProcessor_encapsulate_field_error_title, CorrectionMessages.GetterSetterCorrectionSubProcessor_encapsulate_field_error_message);
			}
		}
	}

	/**
	 * Used by quick assist
	 * @param context the invocation context
	 * @param coveringNode the covering node
	 * @param locations the problems at the corrent location
	 * @param resultingCollections the resulting proposals
	 * @return <code>true</code> if the quick assist is applicable at this offset
	 */
	public static boolean addGetterSetterProposal(IInvocationContext context, ASTNode coveringNode, IProblemLocation[] locations, ArrayList<ICommandAccess> resultingCollections) {
		return new GetterSetterCorrectionSubProcessor().addGetterSetterProposals(context, coveringNode, locations, resultingCollections);
	}

	public static void addGetterSetterProposal(IInvocationContext context, IProblemLocation location, Collection<ICommandAccess> proposals, int relevance) {
		new GetterSetterCorrectionSubProcessor().addGetterSetterProposals(context, location, proposals, relevance);
	}

	private GetterSetterCorrectionSubProcessor() {
		super();
	}

	@Override
	protected ICommandAccess createNonNullMethodGetterProposal(String label, ICompilationUnit compilationUnit, ASTRewrite astRewrite, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, compilationUnit, astRewrite, relevance, image);
		return proposal;
	}

	@Override
	protected ICommandAccess createFieldGetterProposal(int relevance, IField field) {
		return new SelfEncapsulateFieldProposal(relevance, field);
	}

	@Override
	protected ICommandAccess createMethodSetterProposal(String label, ICompilationUnit compilationUnit, ASTRewrite astRewrite, int relevance) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, compilationUnit, astRewrite, relevance, image);
		return proposal;
	}

	@Override
	protected ICommandAccess createFieldSetterProposal(int relevance, IField field) {
		return new SelfEncapsulateFieldProposal(relevance, field);
	}

}
