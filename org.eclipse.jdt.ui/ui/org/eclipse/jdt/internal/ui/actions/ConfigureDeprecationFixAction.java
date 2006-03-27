/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Configures the deprecation fix of a deprecated method. The action opens a
 * dialog from which the user can choose the kind of deprecation fix to be
 * stored in the project.
 * <p>
 * The action is applicable to structured selections containing elements of type
 * {@link org.eclipse.jdt.core.IMethod}.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.2
 */
public class ConfigureDeprecationFixAction extends SelectionDispatchAction {

	/**
	 * Source menu: name of standard configure deprecation fix global action
	 * (value <code>"org.eclipse.jdt.ui.actions.ConfigureDeprecationFix"</code>).
	 * 
	 * TODO: make API in {@link JdtActionConstants}
	 * 
	 * @since 3.2
	 */
	public static final String CONFIGURE_DEPRECATION_FIX= "org.eclipse.jdt.ui.actions.ConfigureDeprecationFix"; //$NON-NLS-1$

	/**
	 * Action definition ID of the source ->configure deprecation fix action
	 * (value
	 * <code>"org.eclipse.jdt.ui.edit.text.java.configure.deprecation.fix"</code>).
	 * 
	 * TODO: make API in {@link IJavaEditorActionDefinitionIds}
	 * 
	 * @since 3.2
	 */
	public static final String CONFIGURE_DEPRECATION_FIXES= "org.eclipse.jdt.ui.edit.text.java.configure.deprecation.fix"; //$NON-NLS-1$

	/** The compilation unit editor */
	private CompilationUnitEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 * 
	 * @param editor
	 *            the compilation unit editor
	 */
	public ConfigureDeprecationFixAction(final CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled((fEditor != null && SelectionConverter.canOperateOn(fEditor)));
	}

	/**
	 * Creates a new configure deprecation fix action.
	 * <p>
	 * The action requires that the selection provided by the site's selection
	 * provider is of type
	 * {@link org.eclipse.jface.viewers.IStructuredSelection}.
	 * 
	 * @param site
	 *            the workbench site providing context information for this
	 *            action
	 */
	public ConfigureDeprecationFixAction(final IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.ConfigureDeprecationFixAction_text);
		setDescription(ActionMessages.ConfigureDeprecationFixAction_description);
		setToolTipText(ActionMessages.ConfigureDeprecationFixAction_tool_tip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CONFIGURE_DEPRECATION_FIX_ACTION);
	}

	private boolean canEnable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			final Object element= selection.getFirstElement();
			if (element instanceof IMethod) {
				final IMethod method= (IMethod) element;
				return !method.isReadOnly() && !method.isMainMethod() && Flags.isDeprecated(method.getFlags());
			} else if (element instanceof IField) {
				final IField field= (IField) element;
				return !field.isReadOnly() && Flags.isDeprecated(field.getFlags());
			}
		}
		return false;
	}

	private IMember getSelectedMember(final IStructuredSelection selection) throws JavaModelException {
		final Object[] elements= selection.toArray();
		if (elements.length == 1) {
			if (elements[0] instanceof IMethod) {
				final IMethod method= (IMethod) elements[0];
				if (!method.isReadOnly() && !method.isMainMethod() && Flags.isDeprecated(method.getFlags()))
					return method;
			} else if (elements[0] instanceof IField) {
				final IField field= (IField) elements[0];
				if (!field.isReadOnly() && Flags.isDeprecated(field.getFlags()))
					return field;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(final IStructuredSelection selection) {
		try {
			final IMember member= getSelectedMember(selection);
			if (member == null) {
				MessageDialog.openInformation(getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, ActionMessages.ConfigureDeprecationFixAction_not_applicable);
				notifyResult(false);
				return;
			}
			if (!ElementValidator.check(member, getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, false) || !ActionUtil.isProcessable(getShell(), member)) {
				notifyResult(false);
				return;
			}
			run(getShell(), member);
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, ActionMessages.ConfigureDeprecationFixAction_general_error);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(final ITextSelection selection) {
		try {
			IMember member= null;
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element instanceof IMethod)
				member= (IMember) element;
			else if (element instanceof IField)
				member= (IMember) element;
			if (member != null) {
				if (!ElementValidator.check(member, getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, false) || !ActionUtil.isProcessable(getShell(), member)) {
					notifyResult(false);
					return;
				}
				if (member.isReadOnly()) {
					MessageDialog.openInformation(getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, ActionMessages.ConfigureDeprecationFixAction_error_read_only);
					notifyResult(false);
					return;
				}
				if (!Flags.isDeprecated(member.getFlags())) {
					MessageDialog.openInformation(getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, ActionMessages.ConfigureDeprecationFixAction_error_not_deprecated);
					notifyResult(false);
					return;
				}
				if (member instanceof IMethod && ((IMethod) member).isMainMethod()) {
					MessageDialog.openInformation(getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, ActionMessages.ConfigureDeprecationFixAction_error_main_method);
					notifyResult(false);
					return;
				}
				run(getShell(), member);
			} else {
				MessageDialog.openInformation(getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, ActionMessages.ConfigureDeprecationFixAction_not_applicable);
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, null);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), ActionMessages.ConfigureDeprecationFixAction_dialog_title, ActionMessages.ConfigureDeprecationFixAction_general_error);
		}
	}

	private void run(final Shell shell, final IMember member) throws CoreException {
		CompilationUnit unit= RefactoringASTParser.parseWithASTProvider(member.getCompilationUnit(), true, null);
		if (unit != null) {
			IBinding binding= null;
			BodyDeclaration declaration= ASTNodeSearchUtil.getBodyDeclarationNode(member, unit);
			if (declaration instanceof MethodDeclaration) {
				final MethodDeclaration extended= (MethodDeclaration) declaration;
				binding= extended.resolveBinding();
			} else if (declaration instanceof FieldDeclaration) {
				final FieldDeclaration extended= (FieldDeclaration) declaration;
				for (final Iterator iterator= extended.fragments().iterator(); iterator.hasNext();) {
					final VariableDeclarationFragment fragment= (VariableDeclarationFragment) iterator.next();
					if (fragment.getName().getIdentifier().equals(member.getElementName())) {
						binding= fragment.resolveBinding();
						break;
					}
				}
			}
			if (binding != null) {
				ConfigureDeprecationFixDialog dialog= new ConfigureDeprecationFixDialog(shell, member.getJavaProject(), binding);
				notifyResult(dialog.open() == Window.OK);
			} else
				notifyResult(false);
		}
		notifyResult(true);
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(final IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException exception) {
			if (JavaModelUtil.isExceptionToBeLogged(exception))
				JavaPlugin.log(exception);
			setEnabled(false);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(final ITextSelection selection) {
		// Do nothing
	}
}
