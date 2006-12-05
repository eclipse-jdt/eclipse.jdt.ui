/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.codemanipulation.AddCustomConstructorOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.GenerateConstructorUsingFieldsContentProvider;
import org.eclipse.jdt.internal.ui.actions.GenerateConstructorUsingFieldsSelectionDialog;
import org.eclipse.jdt.internal.ui.actions.GenerateConstructorUsingFieldsValidator;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

/**
 * Creates constructors for a type based on existing fields.
 * <p>
 * Will open the parent compilation unit in a Java editor. Opens a dialog with a list
 * fields from which a constructor will be generated. User is able to check or uncheck
 * items before constructors are generated. The result is unsaved, so the user can decide
 * if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements of type
 * <code>IType</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class GenerateNewConstructorUsingFieldsAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this
	 * constructor.
	 * 
	 * @param editor the compilation unit editor
	 */
	public GenerateNewConstructorUsingFieldsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}

	/**
	 * Creates a new <code>GenerateConstructorUsingFieldsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public GenerateNewConstructorUsingFieldsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.GenerateConstructorUsingFieldsAction_label); 
		setDescription(ActionMessages.GenerateConstructorUsingFieldsAction_description); 
		setToolTipText(ActionMessages.GenerateConstructorUsingFieldsAction_tooltip); 

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CREATE_NEW_CONSTRUCTOR_ACTION);
	}

	private boolean canEnable(IStructuredSelection selection) throws JavaModelException {
		if (getSelectedFields(selection) != null)
			return true;

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			IType type= (IType) selection.getFirstElement();
			return type.getCompilationUnit() != null && !type.isInterface() && !type.isAnnotation();
		}

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return true;

		return false;
	}

	private boolean canRunOn(IField[] fields) throws JavaModelException {
		if (fields != null && fields.length > 0) {
			for (int index= 0; index < fields.length; index++) {
				if (JdtFlags.isEnum(fields[index])) {
					MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_enum_not_applicable); 
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}

	/*
	 * Returns fields in the selection or <code>null</code> if the selection is empty or
	 * not valid.
	 */
	private IField[] getSelectedFields(IStructuredSelection selection) {
		List elements= selection.toList();
		if (elements.size() > 0) {
			IField[] fields= new IField[elements.size()];
			ICompilationUnit unit= null;
			for (int index= 0; index < elements.size(); index++) {
				if (elements.get(index) instanceof IField) {
					IField field= (IField) elements.get(index);
					if (index == 0) {
						// remember the CU of the first element
						unit= field.getCompilationUnit();
						if (unit == null) {
							return null;
						}
					} else if (!unit.equals(field.getCompilationUnit())) {
						// all fields must be in the same CU
						return null;
					}
					try {
						final IType declaringType= field.getDeclaringType();
						if (declaringType.isInterface() || declaringType.isAnnotation())
							return null;
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
						return null;
					}
					fields[index]= field;
				} else {
					return null;
				}
			}
			return fields;
		}
		return null;
	}

	private IType getSelectedType(IStructuredSelection selection) throws JavaModelException {
		Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			IType type= (IType) elements[0];
			if (type.getCompilationUnit() != null && !type.isInterface() && !type.isAnnotation()) {
				return type;
			}
		} else if (elements[0] instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit) elements[0];
			IType type= unit.findPrimaryType();
			if (type != null && !type.isInterface() && !type.isAnnotation())
				return type;
		} else if (elements[0] instanceof IField) {
			return ((IField) elements[0]).getCompilationUnit().findPrimaryType();
		}
		return null;
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		try {
			IType selectionType= getSelectedType(selection);
			if (selectionType == null) {
				MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_not_applicable);
				notifyResult(false);
				return;
			}

			IField[] selectedFields= getSelectedFields(selection);

			if (canRunOn(selectedFields)) {
				run(selectedFields[0].getDeclaringType(), selectedFields, false);
				return;
			}
			Object firstElement= selection.getFirstElement();

			if (firstElement instanceof IType) {
				run((IType) firstElement, new IField[0], false);
			} else if (firstElement instanceof ICompilationUnit) {
				IType type= ((ICompilationUnit) firstElement).findPrimaryType();
				if (type.isAnnotation()) {
					MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_annotation_not_applicable);
					notifyResult(false);
					return;
				} else if (type.isInterface()) {
					MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_interface_not_applicable);
					notifyResult(false);
					return;
				} else
					run(((ICompilationUnit) firstElement).findPrimaryType(), new IField[0], false);
			}
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_actionfailed); 
		}
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(fEditor)) {
			notifyResult(false);
			return;
		}
		try {
			IJavaElement[] elements= SelectionConverter.codeResolveForked(fEditor, true);
			if (elements.length == 1 && (elements[0] instanceof IField)) {
				IField field= (IField) elements[0];
				run(field.getDeclaringType(), new IField[] { field}, false);
				return;
			}
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element != null) {
				IType type= (IType) element.getAncestor(IJavaElement.TYPE);
				if (type != null) {
					if (type.getFields().length > 0) {
						run(type, new IField[0], true);
					} else {
						MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_typeContainsNoFields_message);
					}
					return;
				}
			}
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_not_applicable); 
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_actionfailed); 
		} catch (InvocationTargetException exception) {
			ExceptionHandler.handle(exception, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_actionfailed); 
		} catch (InterruptedException e) {
			// cancelled
		}
	}

	// ---- Helpers -------------------------------------------------------------------

	void run(IType type, IField[] selected, boolean activated) throws CoreException {
		if (!ElementValidator.check(type, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, activated)) {
			notifyResult(false);
			return;
		}
		if (!ActionUtil.isEditable(fEditor, getShell(), type)) {
			notifyResult(false);
			return;
		}
		if (type.getCompilationUnit() == null) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateNewConstructorUsingFieldsAction_error_not_a_source_file);
			notifyResult(false);
			return;
			
		}
		
		IField[] candidates= type.getFields();
		ArrayList fields= new ArrayList();
		for (int index= 0; index < candidates.length; index++) {
			boolean isStatic= Flags.isStatic(candidates[index].getFlags());
			boolean isFinal= Flags.isFinal(candidates[index].getFlags());
			if (!isStatic) {
				if (isFinal) {
					try {
						// Do not add final fields which have been set in the <clinit>
						IScanner scanner= ToolFactory.createScanner(true, false, false, false);
						scanner.setSource(candidates[index].getSource().toCharArray());
						TokenScanner tokenScanner= new TokenScanner(scanner);
						tokenScanner.getTokenStartOffset(ITerminalSymbols.TokenNameEQUAL, 0);
					} catch (JavaModelException e) {
					} catch (CoreException e) {
						fields.add(candidates[index]);
					}
				} else
					fields.add(candidates[index]);
			}
		}
		if (fields.isEmpty()) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_typeContainsNoFields_message);
			notifyResult(false);
			return;
		}
		final GenerateConstructorUsingFieldsContentProvider provider= new GenerateConstructorUsingFieldsContentProvider(type, fields, Arrays.asList(selected));
		IMethodBinding[] bindings= null;
		final ITypeBinding provided= provider.getType();
		if (provided.isAnonymous()) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_anonymous_class);
			notifyResult(false);
			return;
		}
		if (provided.isEnum()) {
			bindings= new IMethodBinding[] {getObjectConstructor(provider.getCompilationUnit())};
		} else {
			bindings= StubUtility2.getVisibleConstructors(provided, false, true);
			if (bindings.length == 0) {
				MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_nothing_found);
				notifyResult(false);
				return;
			}
		}

		GenerateConstructorUsingFieldsSelectionDialog dialog= new GenerateConstructorUsingFieldsSelectionDialog(getShell(), new BindingLabelProvider(), provider, fEditor, type, bindings);
		dialog.setCommentString(ActionMessages.SourceActionDialog_createConstructorComment); 
		dialog.setTitle(ActionMessages.GenerateConstructorUsingFieldsAction_dialog_title); 
		dialog.setInitialSelections(provider.getInitiallySelectedElements());
		dialog.setContainerMode(true);
		dialog.setSize(60, 18);
		dialog.setInput(new Object());
		dialog.setMessage(ActionMessages.GenerateConstructorUsingFieldsAction_dialog_label); 
		dialog.setValidator(new GenerateConstructorUsingFieldsValidator(dialog, provided, fields.size()));

		final int dialogResult= dialog.open();
		if (dialogResult == Window.OK) {
			Object[] elements= dialog.getResult();
			if (elements == null) {
				notifyResult(false);
				return;
			}
			ArrayList result= new ArrayList(elements.length);
			for (int index= 0; index < elements.length; index++) {
				if (elements[index] instanceof IVariableBinding)
					result.add(elements[index]);
			}
			IVariableBinding[] variables= new IVariableBinding[result.size()];
			result.toArray(variables);
			IEditorPart editor= JavaUI.openInEditor(type.getCompilationUnit());
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject());
			settings.createComments= dialog.getGenerateComment();
			IMethodBinding constructor= dialog.getSuperConstructorChoice();
			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null)
				target.beginCompoundChange();
			try {
				AddCustomConstructorOperation operation= new AddCustomConstructorOperation(type, dialog.getElementPosition(), provider.getCompilationUnit(), variables, constructor, settings, true, false);
				operation.setVisibility(dialog.getVisibilityModifier());
				if (constructor.getParameterTypes().length == 0)
					operation.setOmitSuper(dialog.isOmitSuper());
				IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
				if (context == null)
					context= new BusyIndicatorRunnableContext();
				PlatformUI.getWorkbench().getProgressService().runInUI(context, new WorkbenchRunnableAdapter(operation, operation.getSchedulingRule()), operation.getSchedulingRule());
			} catch (InvocationTargetException exception) {
				ExceptionHandler.handle(exception, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_actionfailed); 
			} catch (InterruptedException exception) {
				// Do nothing. Operation has been canceled by user.
			} finally {
				if (target != null) {
					target.endCompoundChange();
				}
			}
		}
		notifyResult(dialogResult == Window.OK);
	}

	private IMethodBinding getObjectConstructor(CompilationUnit compilationUnit) {
		final ITypeBinding binding= compilationUnit.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
		return Bindings.findMethodInType(binding, "Object", new ITypeBinding[0]); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}

	// ---- Java Editor --------------------------------------------------------------

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
	}
}
