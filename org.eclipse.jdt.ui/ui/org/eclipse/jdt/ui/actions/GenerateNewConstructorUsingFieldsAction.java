/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.codemanipulation.AddCustomConstructorOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.SourceActionLabelProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

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

	private static final String DIALOG_TITLE= ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.title"); //$NON-NLS-1$

	static final int DOWN_INDEX= 1;

	static final int UP_INDEX= 0;

	// ---- Structured Viewer -----------------------------------------------------------

	private static String getDialogTitle() {
		return DIALOG_TITLE;
	}

	private CompilationUnitEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this
	 * constructor.
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
		setText(ActionMessages.getString("GenerateConstructorUsingFieldsAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("GenerateConstructorUsingFieldsAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("GenerateConstructorUsingFieldsAction.tooltip")); //$NON-NLS-1$

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CREATE_NEW_CONSTRUCTOR_ACTION);
	}

	private boolean canEnable(IStructuredSelection selection) throws JavaModelException {
		if (getSelectedFields(selection) != null)
			return true;

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			IType type= (IType) selection.getFirstElement();
			return type.getCompilationUnit() != null && !type.isAnnotation() && !type.isInterface() && !type.isEnum();
		}

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return true;

		return false;
	}

	private boolean canRunOn(IField[] fields) throws JavaModelException {
		if (fields != null && fields.length > 0) {
			for (int index= 0; index < fields.length; index++) {
				if (JdtFlags.isEnum(fields[index])) {
					MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.enum_not_applicable")); //$NON-NLS-1$			
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
						if (declaringType.isInterface() || declaringType.isAnnotation() || declaringType.isEnum()) {
							// no constructors for interfaces
							return null;
						}
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
			if (type.getCompilationUnit() != null && !type.isInterface() && !type.isAnnotation() && !type.isEnum()) {
				return type;
			}
		} else if (elements[0] instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit) elements[0];
			IType type= unit.findPrimaryType();
			if (type != null && !type.isInterface() && !type.isAnnotation() && !type.isEnum())
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
				MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.not_applicable")); //$NON-NLS-1$
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
				if (type.isInterface()) {
					MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.interface_not_applicable")); //$NON-NLS-1$					
					return;
				} else if (type.isAnnotation()) {
					MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.annotation_not_applicable")); //$NON-NLS-1$					
					return;
				} else if (type.isEnum()) {
					MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.enum_not_applicable")); //$NON-NLS-1$					
					return;
				} else
					run(((ICompilationUnit) firstElement).findPrimaryType(), new IField[0], false);
			}
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.actionfailed")); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try {
			IJavaElement[] elements= SelectionConverter.codeResolve(fEditor);
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
						return;
					}
				}
			}
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.not_applicable")); //$NON-NLS-1$
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.actionfailed")); //$NON-NLS-1$
		}
	}

	// ---- Helpers -------------------------------------------------------------------

	void run(IType type, IField[] selected, boolean activated) throws CoreException {
		if (!ElementValidator.check(type, getShell(), getDialogTitle(), activated))
			return;
		if (!ActionUtil.isProcessable(getShell(), type))
			return;

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
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.typeContainsNoFields.message")); //$NON-NLS-1$
			return;
		}
		final GenerateConstructorUsingFieldsContentProvider provider= new GenerateConstructorUsingFieldsContentProvider(type, fields, Arrays.asList(selected));
		IMethodBinding[] bindings= StubUtility2.getOverridableConstructors(provider.getType());
		if (bindings.length == 0) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.nothing_found")); //$NON-NLS-1$
			return;
		}

		GenerateConstructorUsingFieldsSelectionDialog dialog= new GenerateConstructorUsingFieldsSelectionDialog(getShell(), new SourceActionLabelProvider(), provider, fEditor, type, bindings);
		dialog.setCommentString(ActionMessages.getString("SourceActionDialog.createConstructorComment")); //$NON-NLS-1$
		dialog.setTitle(ActionMessages.getString("GenerateConstructorUsingFieldsAction.dialog.title")); //$NON-NLS-1$
		dialog.setInitialSelections(provider.getInitiallySelectedElements());
		dialog.setContainerMode(true);
		dialog.setSize(60, 18);
		dialog.setInput(new Object());
		dialog.setMessage(ActionMessages.getString("GenerateConstructorUsingFieldsAction.dialog.label")); //$NON-NLS-1$
		dialog.setValidator(new GenerateConstructorUsingFieldsValidator(dialog, provider.getType(), fields.size()));

		if (dialog.open() == Window.OK) {
			Object[] elements= dialog.getResult();
			if (elements == null)
				return;
			ArrayList result= new ArrayList(elements.length);
			for (int index= 0; index < elements.length; index++) {
				if (elements[index] instanceof IVariableBinding)
					result.add(elements[index]);
			}
			IVariableBinding[] variables= new IVariableBinding[result.size()];
			result.toArray(variables);
			IEditorPart editor= EditorUtility.openInEditor(type.getCompilationUnit());
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject());
			settings.createComments= dialog.getGenerateComment();
			IMethodBinding constructor= dialog.getSuperConstructorChoice();
			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null)
				target.beginCompoundChange();
			try {
				AddCustomConstructorOperation operation= new AddCustomConstructorOperation(type, dialog.getElementPosition(), variables, constructor, settings, false);
				operation.setVisibility(dialog.getVisibilityModifier());
				if (constructor.getParameterTypes().length == 0)
					operation.setOmitSuper(dialog.isOmitSuper());
				IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
				if (context == null)
					context= new BusyIndicatorRunnableContext();
				PlatformUI.getWorkbench().getProgressService().runInUI(context, new WorkbenchRunnableAdapter(operation, operation.getSchedulingRule()), operation.getSchedulingRule());
			} catch (InvocationTargetException exception) {
				ExceptionHandler.handle(exception, getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.actionfailed")); //$NON-NLS-1$
			} catch (InterruptedException exception) {
				// Do nothing. Operation has been cancelled by user.
			} finally {
				if (target != null) {
					target.endCompoundChange();
				}
			}
		}
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
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