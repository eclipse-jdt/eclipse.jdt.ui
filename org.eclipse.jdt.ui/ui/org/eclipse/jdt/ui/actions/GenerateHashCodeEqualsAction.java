/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.GenerateHashCodeEqualsOperation;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.GenerateHashCodeEqualsDialog;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Adds method implementations for
 * <code>{@link java.lang.Object#equals(java.lang.Object)}</code> and
 * <code>{@link java.lang.Object#hashCode()}</code>. The action opens a
 * dialog from which the user can choose the fields to be considered.
 * <p>
 * Will open the parent compilation unit in a Java editor. The result is
 * unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements of type
 * {@link org.eclipse.jdt.core.IType}.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.2
 */
public final class GenerateHashCodeEqualsAction extends SelectionDispatchAction {

	private static final String METHODNAME_HASH_CODE= "hashCode"; //$NON-NLS-1$

	private static final String METHODNAME_EQUALS= "equals"; //$NON-NLS-1$

	private CompilationUnitEditor fEditor;

	private CompilationUnit fUnit;

	private ITypeBinding fTypeBinding;

	private IVariableBinding[] fCandidateFields;

	private class HashCodeEqualsInfo {
		
		public boolean foundHashCode= false;

		public boolean foundEquals= false;

		public boolean foundFinalHashCode= false;

		public boolean foundFinalEquals= false;
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 * 
	 * @param editor the compilation unit editor
	 */
	public GenerateHashCodeEqualsAction(final CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled( (fEditor != null && SelectionConverter.canOperateOn(fEditor)));
	}

	/**
	 * Creates a new generate hashCode equals action.
	 * <p>
	 * The action requires that the selection provided by the site's selection
	 * provider is of type
	 * {@link org.eclipse.jface.viewers.IStructuredSelection}.
	 * 
	 * @param site the workbench site providing context information for this
	 *            action
	 */
	public GenerateHashCodeEqualsAction(final IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.GenerateHashCodeEqualsAction_label);
		setDescription(ActionMessages.GenerateHashCodeEqualsAction_description);
		setToolTipText(ActionMessages.GenerateHashCodeEqualsAction_tooltip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.GENERATE_HASHCODE_EQUALS_ACTION);
	}

	/**
	 * Can this action be enabled on the specified selection?
	 * 
	 * @param selection the selection to test
	 * @return <code>true</code> if it can be enabled, <code>false</code>
	 *         otherwise
	 * @throws JavaModelException if the kind of the selection cannot be
	 *             determined
	 */
	private boolean canEnable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			final Object element= selection.getFirstElement();
			if (element instanceof IType) {
				final IType type= (IType) element;
				return type.getCompilationUnit() != null && type.isClass();
			}
			if (element instanceof ICompilationUnit)
				return true;
		}
		return false;
	}

	/**
	 * Returns the single selected type from the specified selection.
	 * 
	 * @param selection the selection
	 * @return a single selected type, or <code>null</code>
	 * @throws JavaModelException if the kind of the selection cannot be
	 *             determined
	 */
	private IType getSelectedType(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1 && selection.getFirstElement() instanceof IType) {
			final IType type= (IType) selection.getFirstElement();
			if (type.getCompilationUnit() != null && type.isClass())
				return type;
		} else if (selection.getFirstElement() instanceof ICompilationUnit) {
			final ICompilationUnit unit= (ICompilationUnit) selection.getFirstElement();
			final IType type= unit.findPrimaryType();
			if (type != null && type.isClass())
				return type;
		}
		return null;
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			checkAndRun(getSelectedType(selection));
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption,
					ActionMessages.GenerateHashCodeEqualsAction_error_cannot_create);
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		try {
			checkAndRun(SelectionConverter.getTypeAtOffset(fEditor));
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption,
					ActionMessages.GenerateHashCodeEqualsAction_error_cannot_create);
		}
	}

	private void checkAndRun(IType type) throws CoreException {
		if (type == null) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption,
					ActionMessages.GenerateHashCodeEqualsAction_error_not_applicable);
			notifyResult(false);
		}
		if (!ElementValidator.check(type, getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption, false)
				|| ! ActionUtil.isEditable(fEditor, getShell(), type)) {
			notifyResult(false);
			return;
		}
		if (type == null) {
			MessageDialog.openError(getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption,
					ActionMessages.GenerateHashCodeEqualsAction_error_removed_type);
			notifyResult(false);
			return;
		}
		if (type.isAnnotation()) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption,
					ActionMessages.GenerateHashCodeEqualsAction_annotation_not_applicable);
			notifyResult(false);
			return;
		}
		if (type.isInterface()) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption,
					ActionMessages.GenerateHashCodeEqualsAction_interface_not_applicable);
			notifyResult(false);
			return;
		}
		if (type.isEnum()) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption,
					ActionMessages.GenerateHashCodeEqualsAction_enum_not_applicable);
			notifyResult(false);
			return;
		}
		if (type.isAnonymous()) {
			MessageDialog.openError(getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption,
					ActionMessages.GenerateHashCodeEqualsAction_anonymous_type_not_applicable);
			notifyResult(false);
			return;
		}
		run(getShell(), type);
	}

	/**
	 * Runs the action.
	 * 
	 * @param shell the shell to use
	 * @param type the type to generate stubs for
	 * @throws CoreException if an error occurs
	 */
	private void run(Shell shell, IType type) throws CoreException {

		initialize(type);

		boolean regenerate= false;
		if (hasHashCodeOrEquals(fTypeBinding)) {
			regenerate= MessageDialog.openQuestion(getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption, Messages.format(ActionMessages.GenerateHashCodeEqualsAction_already_has_hashCode_equals_error, fTypeBinding.getQualifiedName()));
			if (!regenerate) {
				notifyResult(false);
				return;
			}
		}

		List allFields= new ArrayList();
		List selectedFields= new ArrayList();
		for (int i= 0; i < fCandidateFields.length; i++) {
			if (!Modifier.isStatic(fCandidateFields[i].getModifiers())) {
				allFields.add(fCandidateFields[i]);
				if (!Modifier.isTransient(fCandidateFields[i].getModifiers()))
					selectedFields.add(fCandidateFields[i]);
			}
		}

		if (allFields.isEmpty()) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption,
					ActionMessages.GenerateHashCodeEqualsAction_no_nonstatic_fields_error);
			notifyResult(false);
			return;
		}

		IVariableBinding[] allFieldBindings= (IVariableBinding[]) allFields.toArray(new IVariableBinding[0]);
		IVariableBinding[] selectedFieldBindings= (IVariableBinding[]) selectedFields.toArray(new IVariableBinding[0]);

		final GenerateHashCodeEqualsDialog dialog= new GenerateHashCodeEqualsDialog(shell, fEditor, type, allFieldBindings, selectedFieldBindings);
		final int dialogResult= dialog.open();
		if (dialogResult == Window.OK) {

			final Object[] selected= dialog.getResult();
			if (selected == null) {
				notifyResult(false);
				return;
			}

			final IVariableBinding[] selectedBindings= (IVariableBinding[]) Arrays.asList(selected).toArray(new IVariableBinding[0]);

			ITypeBinding superclass= fTypeBinding.getSuperclass();
			RefactoringStatus status= new RefactoringStatus();
			ArrayList alreadyChecked= new ArrayList();

			if (!"java.lang.Object".equals(superclass.getQualifiedName())) { //$NON-NLS-1$
				status.merge(checkHashCodeEqualsExists(superclass, true));
			}

			for (int i= 0; i < selectedBindings.length; i++) {
				ITypeBinding fieldsType= selectedBindings[i].getType();
				if (fieldsType.isArray())
					fieldsType= fieldsType.getElementType();
				if (!fieldsType.isPrimitive() && !fieldsType.isEnum() && !alreadyChecked.contains(fieldsType) && !fieldsType.equals(fTypeBinding)) {
					status.merge(checkHashCodeEqualsExists(fieldsType, false));
					alreadyChecked.add(fieldsType);
				}
				if (Modifier.isTransient(selectedBindings[i].getModifiers()))
					status.addWarning(Messages.format(ActionMessages.GenerateHashCodeEqualsAction_transient_field_included_error, selectedBindings[i]
							.getName()), createRefactoringStatusContext(selectedBindings[i].getJavaElement()));
			}

			if (status.hasEntries()) {
				Dialog d= RefactoringUI.createLightWeightStatusDialog(status, getShell(), ActionMessages.GenerateHashCodeEqualsAction_error_caption);
				if (d.open() != Window.OK) {
					notifyResult(false);
					return;
				}
			}

			final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject());
			settings.createComments= dialog.getGenerateComment();
			final IEditorPart editor= JavaUI.openInEditor(type.getCompilationUnit());
			final IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;

			if (target != null)
				target.beginCompoundChange();
			try {
				final GenerateHashCodeEqualsOperation operation= new GenerateHashCodeEqualsOperation(fTypeBinding, selectedBindings, fUnit, dialog
						.getElementPosition(), settings, dialog.isUseInstanceOf(), regenerate, true, false);
				IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
				if (context == null)
					context= new BusyIndicatorRunnableContext();
				PlatformUI.getWorkbench().getProgressService().runInUI(context,
						new WorkbenchRunnableAdapter(operation, operation.getSchedulingRule()), operation.getSchedulingRule());
			} catch (InvocationTargetException exception) {
				ExceptionHandler.handle(exception, shell, ActionMessages.GenerateHashCodeEqualsAction_error_caption, null);
			} catch (InterruptedException exception) {
				// Do nothing. Operation has been canceled by user.
			} finally {
				if (target != null)
					target.endCompoundChange();
			}
		}
		notifyResult(dialogResult == Window.OK);
	}
	
	private static RefactoringStatusContext createRefactoringStatusContext(IJavaElement element) {
		if (element instanceof IMember) {
			return JavaStatusContext.create((IMember) element);
		}
		if (element instanceof ISourceReference) {
			IOpenable openable= element.getOpenable();
			try {
				if (openable instanceof ICompilationUnit) {
					return JavaStatusContext.create((ICompilationUnit) openable, ((ISourceReference) element).getSourceRange());
				} else if (openable instanceof IClassFile) {
					return JavaStatusContext.create((IClassFile) openable, ((ISourceReference) element).getSourceRange());
				}
			} catch (JavaModelException e) {
				// ignore
			}
		}
		return null;
	}

	private boolean hasHashCodeOrEquals(ITypeBinding someType) {
		HashCodeEqualsInfo info= getTypeInfo(someType);
		return (info.foundEquals || info.foundHashCode);
	}

	private RefactoringStatus checkHashCodeEqualsExists(ITypeBinding someType, boolean superClass) {

		RefactoringStatus status= new RefactoringStatus();
		HashCodeEqualsInfo info= getTypeInfo(someType);

		String concreteTypeWarning= superClass
				? ActionMessages.GenerateHashCodeEqualsAction_super_class
				: ActionMessages.GenerateHashCodeEqualsAction_field_type;
		String concreteMethWarning= (someType.isInterface() || Modifier.isAbstract(someType.getModifiers()))
				? ActionMessages.GenerateHashCodeEqualsAction_interface_does_not_declare_hashCode_equals_error
				: ActionMessages.GenerateHashCodeEqualsAction_type_does_not_implement_hashCode_equals_error;
		String concreteHCEWarning= null;

		if (!info.foundEquals && (!info.foundHashCode))
			concreteHCEWarning= ActionMessages.GenerateHashCodeEqualsAction_equals_and_hashCode;
		else if (!info.foundEquals)
			concreteHCEWarning= ActionMessages.GenerateHashCodeEqualsAction_equals;
		else if (!info.foundHashCode)
			concreteHCEWarning= ActionMessages.GenerateHashCodeEqualsAction_hashCode;

		if (!info.foundEquals && !info.foundHashCode)
			status.addWarning(Messages.format(concreteMethWarning, new String[] { Messages.format(concreteTypeWarning, someType.getQualifiedName()),
					concreteHCEWarning }), createRefactoringStatusContext(someType.getJavaElement()));

		if (superClass && (info.foundFinalEquals || info.foundFinalHashCode)) {
			status.addError(Messages.format(ActionMessages.GenerateHashCodeEqualsAction_final_hashCode_equals_in_superclass_error, Messages.format(
					concreteTypeWarning, someType.getQualifiedName())), createRefactoringStatusContext(someType.getJavaElement()));
		}

		return status;
	}

	private HashCodeEqualsInfo getTypeInfo(ITypeBinding someType) {
		HashCodeEqualsInfo info= new HashCodeEqualsInfo();
		if (someType.isTypeVariable()) {
			someType= someType.getErasure();
		}
		
		IMethodBinding[] declaredMethods= someType.getDeclaredMethods();

		for (int i= 0; i < declaredMethods.length; i++) {
			if (declaredMethods[i].getName().equals(METHODNAME_EQUALS)) {
				ITypeBinding[] b= declaredMethods[i].getParameterTypes();
				if ( (b.length == 1) && (b[0].getQualifiedName().equals("java.lang.Object"))) { //$NON-NLS-1$
					info.foundEquals= true;
					if (Modifier.isFinal(declaredMethods[i].getModifiers()))
						info.foundFinalEquals= true;
				}
			}
			if (declaredMethods[i].getName().equals(METHODNAME_HASH_CODE) && declaredMethods[i].getParameterTypes().length == 0) {
				info.foundHashCode= true;
				if (Modifier.isFinal(declaredMethods[i].getModifiers()))

					info.foundFinalHashCode= true;
			}
			if (info.foundEquals && info.foundHashCode)
				break;
		}
		return info;
	}

	private void initialize(IType type) throws JavaModelException {
		RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
		fUnit= parser.parse(type.getCompilationUnit(), true);
		fTypeBinding= null;
		// type cannot be anonymous
		final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(NodeFinder.perform(fUnit, type.getNameRange()),
				AbstractTypeDeclaration.class);
		if (declaration != null)
			fTypeBinding= declaration.resolveBinding();

		fCandidateFields= fTypeBinding.getDeclaredFields();
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException exception) {
			if (JavaModelUtil.isExceptionToBeLogged(exception))
				JavaPlugin.log(exception);
			setEnabled(false);
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
		// Do nothing
	}
}
