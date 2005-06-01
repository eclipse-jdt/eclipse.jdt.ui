/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.code.ConvertAnonymousToNestedRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameResourceProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.UseSuperTypeRefactoring;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.refactoring.RenameSupport;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ChangeSignatureWizard;
import org.eclipse.jdt.internal.ui.refactoring.ChangeTypeWizard;
import org.eclipse.jdt.internal.ui.refactoring.ConvertAnonymousToNestedWizard;
import org.eclipse.jdt.internal.ui.refactoring.ExtractInterfaceWizard;
import org.eclipse.jdt.internal.ui.refactoring.InferTypeArgumentsWizard;
import org.eclipse.jdt.internal.ui.refactoring.InlineConstantWizard;
import org.eclipse.jdt.internal.ui.refactoring.InlineTempWizard;
import org.eclipse.jdt.internal.ui.refactoring.IntroduceFactoryWizard;
import org.eclipse.jdt.internal.ui.refactoring.MoveInnerToTopWizard;
import org.eclipse.jdt.internal.ui.refactoring.MoveInstanceMethodWizard;
import org.eclipse.jdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.jdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.jdt.internal.ui.refactoring.PushDownWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.UseSupertypeWizard;
import org.eclipse.jdt.internal.ui.refactoring.UserInterfaceStarter;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.code.InlineMethodWizard;
import org.eclipse.jdt.internal.ui.refactoring.reorg.CreateTargetQueries;
import org.eclipse.jdt.internal.ui.refactoring.reorg.DeleteUserInterfaceManager;
import org.eclipse.jdt.internal.ui.refactoring.reorg.RenameUserInterfaceManager;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgMoveWizard;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgQueries;
import org.eclipse.jdt.internal.ui.refactoring.sef.SelfEncapsulateFieldWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Helper class to run refactorings from action code.
 * <p>
 * This class has been introduced to decouple actions from the refactoring code, in order not to eagerly load refactoring classes during action initialization.
 * </p>
 * 
 * @since 3.1
 */
public final class RefactoringExecutionStarter {

	private static RenameSupport createRenameSupport(IJavaElement element, String newName, int flags) throws CoreException {
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				return RenameSupport.create((IJavaProject) element, newName, flags);
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return RenameSupport.create((IPackageFragmentRoot) element, newName);
			case IJavaElement.PACKAGE_FRAGMENT:
				return RenameSupport.create((IPackageFragment) element, newName, flags);
			case IJavaElement.COMPILATION_UNIT:
				return RenameSupport.create((ICompilationUnit) element, newName, flags);
			case IJavaElement.TYPE:
				return RenameSupport.create((IType) element, newName, flags);
			case IJavaElement.METHOD:
				final IMethod method= (IMethod) element;
				if (method.isConstructor())
					return createRenameSupport(method.getDeclaringType(), newName, flags);
				else
					return RenameSupport.create((IMethod) element, newName, flags);
			case IJavaElement.FIELD:
				return RenameSupport.create((IField) element, newName, flags);
			case IJavaElement.TYPE_PARAMETER:
				return RenameSupport.create((ITypeParameter) element, newName, flags);
			case IJavaElement.LOCAL_VARIABLE:
				return RenameSupport.create((ILocalVariable) element, newName, flags);
		}
		return null;
	}

	public static void startChangeSignatureRefactoring(final IMethod method, final SelectionDispatchAction action, final Shell shell) throws JavaModelException {
		final ChangeSignatureRefactoring change= ChangeSignatureRefactoring.create(method);
		if (!ActionUtil.isProcessable(shell, change.getMethod()))
			return;
		final UserInterfaceStarter starter= new UserInterfaceStarter() {

			public final void activate(final Refactoring refactoring, final Shell parent, final boolean save) throws CoreException {
				final RefactoringStatus status= refactoring.checkInitialConditions(new NullProgressMonitor());
				if (status.hasFatalError()) {
					final RefactoringStatusEntry entry= status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
					if (entry.getCode() == RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD || entry.getCode() == RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE) {

						String message= entry.getMessage();
						final Object element= entry.getData();
						message= message + RefactoringMessages.RefactoringErrorDialogUtil_okToPerformQuestion;
						if (element != null && MessageDialog.openQuestion(shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, message)) {

							final IStructuredSelection selection= new StructuredSelection(element);
							action.selectionChanged(selection);
							if (action.isEnabled()) {
								action.run(selection);
							} else {
								MessageDialog.openInformation(shell, ActionMessages.ModifyParameterAction_problem_title, ActionMessages.ModifyParameterAction_problem_message);
							}
						}
						return;
					}
				}
				super.activate(refactoring, parent, save);
			}
		};
		starter.initialize(new ChangeSignatureWizard(change));
		try {
			starter.activate(change, shell, true);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.RefactoringStarter_unexpected_exception);
		}
	}

	public static void startChangeTypeRefactoring(final ICompilationUnit unit, final Shell shell, final int offset, final int length) throws JavaModelException {
		final ChangeTypeRefactoring refactoring= ChangeTypeRefactoring.create(unit, offset, length);
		if (refactoring != null)
			new RefactoringStarter().activate(refactoring, new ChangeTypeWizard(refactoring), shell, RefactoringMessages.ChangeTypeAction_dialog_title, false);
	}

	public static void startConvertAnonymousRefactoring(final ICompilationUnit unit, final int offset, final int length, final Shell shell) throws JavaModelException {
		final ConvertAnonymousToNestedRefactoring refactoring= ConvertAnonymousToNestedRefactoring.create(unit, offset, length);
		if (refactoring == null)
			return;
		new RefactoringStarter().activate(refactoring, new ConvertAnonymousToNestedWizard(refactoring), shell, RefactoringMessages.ConvertAnonymousToNestedAction_dialog_title, false);
	}

	public static void startCutRefactoring(final Object[] elements, final Shell shell) throws CoreException, InterruptedException, InvocationTargetException {
		final JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		processor.setSuggestGetterSetterDeletion(false);
		processor.setQueries(new ReorgQueries(shell));
		new RefactoringExecutionHelper(new DeleteRefactoring(processor), RefactoringCore.getConditionCheckingFailedSeverity(), false, shell, new ProgressMonitorDialog(shell)).perform();
	}

	public static void startDeleteRefactoring(final Object[] elements, final Shell shell) throws CoreException {
		final DeleteRefactoring refactoring= new DeleteRefactoring(new JavaDeleteProcessor(elements));
		DeleteUserInterfaceManager.getDefault().getStarter(refactoring).activate(refactoring, shell, false);
	}

	public static void startExtractInterfaceRefactoring(final IType type, final Shell shell) throws JavaModelException {
		final ExtractInterfaceRefactoring refactoring= ExtractInterfaceRefactoring.create(type, JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject()));
		if (!ActionUtil.isProcessable(shell, refactoring.getExtractInterfaceProcessor().getType()))
			return;
		new RefactoringStarter().activate(refactoring, new ExtractInterfaceWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, true); 
	}

	public static void startInferTypeArgumentsRefactoring(final IJavaElement[] elements, final Shell shell) {
		if (! ActionUtil.areProcessable(shell, elements))
			return;
		try {
			final InferTypeArgumentsRefactoring refactoring= InferTypeArgumentsRefactoring.create(elements);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, new InferTypeArgumentsWizard(refactoring), shell, RefactoringMessages.InferTypeArgumentsAction_dialog_title, true);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.InferTypeArgumentsAction_dialog_title, RefactoringMessages.OpenRefactoringWizardAction_exception);
		}
	}

	public static boolean startInlineConstantRefactoring(final ICompilationUnit unit, final CompilationUnit node, final int offset, final int length, final Shell shell, final boolean activate) throws JavaModelException {
		final InlineConstantRefactoring refactoring= InlineConstantRefactoring.create(unit, node, offset, length);
		if (refactoring == null) {
			if (activate)
				MessageDialog.openInformation(shell, RefactoringMessages.InlineConstantAction_dialog_title, RefactoringMessages.InlineConstantAction_no_constant_reference_or_declaration);
			return false;
		}
		if (activate)
			new RefactoringStarter().activate(refactoring, new InlineConstantWizard(refactoring), shell, RefactoringMessages.InlineConstantAction_dialog_title, true);
		return true;
	}

	public static boolean startInlineMethodRefactoring(final ICompilationUnit unit, final CompilationUnit node, final int offset, final int length, final Shell shell, final boolean activate) throws JavaModelException {
		final InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(unit, node, offset, length);
		if (refactoring == null) {
			if (activate)
				MessageDialog.openInformation(shell, RefactoringMessages.InlineMethodAction_dialog_title, RefactoringMessages.InlineMethodAction_no_method_invocation_or_declaration_selected);
			return false;
		}
		if (activate)
			new RefactoringStarter().activate(refactoring, new InlineMethodWizard(refactoring), shell, RefactoringMessages.InlineMethodAction_dialog_title, true);
		return true;
	}

	public static boolean startInlineTempRefactoring(final ICompilationUnit unit, final CompilationUnit node, final ITextSelection selection, final Shell shell, final boolean activate) throws JavaModelException {
		final Refactoring refactoring= InlineTempRefactoring.create(unit, node, selection.getOffset(), selection.getLength());
		if (refactoring != null) {
			if (activate)
				new RefactoringStarter().activate(refactoring, new InlineTempWizard((InlineTempRefactoring) refactoring), shell, RefactoringMessages.InlineTempAction_inline_temp, false);
			return true;
		}
		return false;
	}

	public static void startIntroduceFactoryRefactoring(final ICompilationUnit unit, final ITextSelection selection, final Shell shell) throws JavaModelException {
		final IntroduceFactoryRefactoring refactoring= IntroduceFactoryRefactoring.create(unit, selection.getOffset(), selection.getLength());
		if (refactoring != null)
			new RefactoringStarter().activate(refactoring, new IntroduceFactoryWizard(refactoring, RefactoringMessages.IntroduceFactoryAction_use_factory), shell, RefactoringMessages.IntroduceFactoryAction_dialog_title, false);
	}

	public static void startMoveInnerRefactoring(final IType type, final Shell shell) throws JavaModelException {
		final MoveInnerToTopRefactoring refactoring= MoveInnerToTopRefactoring.create(type, JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject()));
		if (!ActionUtil.isProcessable(shell, refactoring.getInputType()))
			return;
		new RefactoringStarter().activate(refactoring, new MoveInnerToTopWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, true);
	}

	public static void startMoveMethodRefactoring(final IMethod method, final Shell shell) throws JavaModelException {
		final MoveInstanceMethodRefactoring refactoring= MoveInstanceMethodRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings(method.getJavaProject()));
		if (refactoring == null)
			MessageDialog.openInformation(shell, RefactoringMessages.MoveInstanceMethodAction_dialog_title, RefactoringMessages.MoveInstanceMethodAction_No_reference_or_declaration);
		else
			new RefactoringStarter().activate(refactoring, new MoveInstanceMethodWizard(refactoring), shell, RefactoringMessages.MoveInstanceMethodAction_dialog_title, true);
	}

	public static void startMoveStaticMembersRefactoring(final IMember[] members, final Shell shell) throws JavaModelException {
		final Set set= new HashSet();
		set.addAll(Arrays.asList(members));
		final IMember[] elements= (IMember[]) set.toArray(new IMember[set.size()]);
		IJavaProject project= null;
		if (elements.length > 0)
			project= elements[0].getJavaProject();
		final MoveRefactoring refactoring= new MoveRefactoring(MoveStaticMembersProcessor.create(elements, JavaPreferencesSettings.getCodeGenerationSettings(project)));
		if (ActionUtil.isProcessable(shell, ((MoveStaticMembersProcessor) refactoring.getAdapter(MoveStaticMembersProcessor.class)).getMembersToMove()[0].getCompilationUnit()))
			new RefactoringStarter().activate(refactoring, new MoveMembersWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, true);
	}

	public static void startPullUpRefactoring(final IMember[] members, final Shell shell) throws JavaModelException {
		IJavaProject project= null;
		if (members != null && members.length > 0)
			project= members[0].getJavaProject();
		final PullUpRefactoring refactoring= PullUpRefactoring.create(members, JavaPreferencesSettings.getCodeGenerationSettings(project));
		if (!ActionUtil.isProcessable(shell, refactoring.getDeclaringType()))
			return;
		new RefactoringStarter().activate(refactoring, new PullUpWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, true);
	}

	public static void startPushDownRefactoring(final IMember[] members, final Shell shell) throws JavaModelException {
		final PushDownRefactoring refactoring= PushDownRefactoring.create(members);
		if (!ActionUtil.isProcessable(shell, refactoring.getDeclaringType()))
			return;
		new RefactoringStarter().activate(refactoring, new PushDownWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, true);
	}

	public static void startRefactoring(final IResource[] resources, final IJavaElement[] elements, final Shell shell) throws JavaModelException {
		final JavaMoveProcessor processor= JavaMoveProcessor.create(resources, elements);
		final MoveRefactoring refactoring= new MoveRefactoring(processor);
		final RefactoringWizard wizard= new ReorgMoveWizard(refactoring);
		processor.setCreateTargetQueries(new CreateTargetQueries(wizard));
		processor.setReorgQueries(new ReorgQueries(wizard));
		new RefactoringStarter().activate(refactoring, wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, true);
	}

	public static void startRenameRefactoring(final IJavaElement element, final Shell shell) throws CoreException {
		final RenameSupport support= createRenameSupport(element, null, RenameSupport.UPDATE_REFERENCES);
		if (support != null && support.preCheck().isOK())
			support.openDialog(shell);
	}

	public static void startRenameResourceRefactoring(final IResource resource, final Shell shell) throws CoreException {
		final RenameRefactoring refactoring= new RenameRefactoring(new RenameResourceProcessor(resource));
		RenameUserInterfaceManager.getDefault().getStarter(refactoring).activate(refactoring, shell, true);
	}

	public static void startSelfEncapsulateRefactoring(final IField field, final Shell shell) {
		if (!ActionUtil.isProcessable(shell, field))
			return;
		try {
			final SelfEncapsulateFieldRefactoring refactoring= SelfEncapsulateFieldRefactoring.create(field);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, new SelfEncapsulateFieldWizard(refactoring), shell, ActionMessages.SelfEncapsulateFieldAction_dialog_title, true);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, ActionMessages.SelfEncapsulateFieldAction_dialog_title, ActionMessages.SelfEncapsulateFieldAction_dialog_cannot_perform);
		}
	}

	public static void startUseSupertypeRefactoring(final IType type, final Shell shell) throws JavaModelException {
		final UseSuperTypeRefactoring refactoring= UseSuperTypeRefactoring.create(type);
		if (!ActionUtil.isProcessable(shell, refactoring.getUseSuperTypeProcessor().getSubType()))
			return;
		new RefactoringStarter().activate(refactoring, new UseSupertypeWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, true);
	}

	private RefactoringExecutionStarter() {
		// Not for instantiation
	}
}
