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
import java.util.List;

import org.eclipse.core.runtime.CoreException;


import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.codemanipulation.AddCustomConstructorOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Creates constructors for a type based on existing fields.
 * <p>
 * Will open the parent compilation unit in a Java editor. Opens a dialog
 * with a list fields from which a constructor will be generated.
 * User is able to check or uncheck items before constructors are generated.
 * The result is unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>IType</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class GenerateNewConstructorUsingFieldsAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private static final String DIALOG_TITLE= ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.title"); //$NON-NLS-1$
	static final int UP_INDEX= 0;
	static final int DOWN_INDEX= 1;

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

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public GenerateNewConstructorUsingFieldsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}

	//---- Structured Viewer -----------------------------------------------------------

	private static String getDialogTitle() {
		return DIALOG_TITLE;
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
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

	private boolean canEnable(IStructuredSelection selection) throws JavaModelException {
		if (getSelectedFields(selection) != null)
			return true;

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			IType type= (IType) selection.getFirstElement();
			return type.getCompilationUnit() != null && type.isClass() && !type.isLocal();
			// look if class: not cheap but done by all source generation actions
		}

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return true;

		return false;
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
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
			}
			else if (firstElement instanceof ICompilationUnit) {
				IType type= ((ICompilationUnit) firstElement).findPrimaryType();
				if (type.isInterface()) {
					MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.interface_not_applicable")); //$NON-NLS-1$					
					return;
				} else
					run(((ICompilationUnit) firstElement).findPrimaryType(), new IField[0], false);
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.actionfailed")); //$NON-NLS-1$
		}
	}

	private IType getSelectedType(IStructuredSelection selection) throws JavaModelException {
		Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			IType type= (IType) elements[0];
			if (type.getCompilationUnit() != null && type.isClass()) {
				return type;
			}
		} else if (elements[0] instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit) elements[0];
			IType type= cu.findPrimaryType();
			if (type != null && !type.isInterface())
				return type;
		} else if (elements[0] instanceof IField) {
			return ((IField) elements[0]).getCompilationUnit().findPrimaryType();
		}
		return null;
	}

	private boolean canRunOn(IField[] fields) throws JavaModelException {
		if (fields != null && fields.length > 0) {
			for (int i= 0; i < fields.length; i++) {
				if (JdtFlags.isEnum(fields[i])) {
					MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.enum_not_applicable")); //$NON-NLS-1$			
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/*
	 * Returns fields in the selection or <code>null</code> if the selection is 
	 * empty or not valid.
	 */
	private IField[] getSelectedFields(IStructuredSelection selection) {
		List elements= selection.toList();
		int nElements= elements.size();
		if (nElements > 0) {
			IField[] res= new IField[nElements];
			ICompilationUnit cu= null;
			for (int i= 0; i < nElements; i++) {
				Object curr= elements.get(i);
				if (curr instanceof IField) {
					IField fld= (IField) curr;

					if (i == 0) {
						// remember the CU of the first element
						cu= fld.getCompilationUnit();
						if (cu == null) {
							return null;
						}
					} else if (!cu.equals(fld.getCompilationUnit())) {
						// all fields must be in the same CU
						return null;
					}
					try {
						if (fld.getDeclaringType().isInterface()) {
							// no constructors for interfaces
							return null;
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
						return null;
					}

					res[i]= fld;
				} else {
					return null;
				}
			}
			return res;
		}
		return null;
	}

	//---- Java Editor --------------------------------------------------------------

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try {
			IJavaElement[] elements= SelectionConverter.codeResolve(fEditor);
			if (elements.length == 1 && (elements[0] instanceof IField)) {
				IField field= (IField) elements[0];
				run(field.getDeclaringType(), new IField[] { field },false);
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
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.actionfailed")); //$NON-NLS-1$
		}
	}

	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}

	//---- Helpers -------------------------------------------------------------------

	void run(IType type, IField[] preselected, boolean activatedFromEditor) throws CoreException {
		if (type.isEnum()) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.enum_not_applicable")); //$NON-NLS-1$
			return;
		}
		if (!ElementValidator.check(type, getShell(), getDialogTitle(), activatedFromEditor)) {
			return;
		}
		if (!ActionUtil.isProcessable(getShell(), type)) {
			return;
		}

		IField[] constructorFields= type.getFields();

		ArrayList constructorFieldsList= new ArrayList();
		for (int i= 0; i < constructorFields.length; i++) {
			boolean isStatic= Flags.isStatic(constructorFields[i].getFlags());
			boolean isFinal= Flags.isFinal(constructorFields[i].getFlags());
			if (!isStatic) {
				if (isFinal) {
					try {
						// Do not add final fields which have been set in the <clinit>
						IScanner scanner= ToolFactory.createScanner(true, false, false, false);
						scanner.setSource(constructorFields[i].getSource().toCharArray());
						TokenScanner tokenScanner= new TokenScanner(scanner);
						tokenScanner.getTokenStartOffset(ITerminalSymbols.TokenNameEQUAL, 0);
					} catch (JavaModelException e) {
					} catch (CoreException e) {
						constructorFieldsList.add(constructorFields[i]);
					}			
				} else
					constructorFieldsList.add(constructorFields[i]);
			}
		}
		if (constructorFieldsList.isEmpty()) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.typeContainsNoFields.message")); //$NON-NLS-1$
			return;
		}
		IMethod[] superConstructors= getSuperConstructors(type);
		if (superConstructors.length == 0) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.nothing_found")); //$NON-NLS-1$
			return;
		}		

		JavaElementLabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		GenerateConstructorUsingFieldsContentProvider contentProvider= new GenerateConstructorUsingFieldsContentProvider(constructorFieldsList);
		GenerateConstructorUsingFieldsSelectionDialog dialog= new GenerateConstructorUsingFieldsSelectionDialog(getShell(), labelProvider, contentProvider, fEditor, type, superConstructors);
		dialog.setCommentString(ActionMessages.getString("SourceActionDialog.createConstructorComment")); //$NON-NLS-1$
		dialog.setTitle(ActionMessages.getString("GenerateConstructorUsingFieldsAction.dialog.title")); //$NON-NLS-1$
		dialog.setInitialSelections(preselected);
		dialog.setContainerMode(true);
		dialog.setSize(60, 18);
		dialog.setInput(new Object());
		dialog.setMessage(ActionMessages.getString("GenerateConstructorUsingFieldsAction.dialog.label")); //$NON-NLS-1$
		dialog.setValidator(createValidator(constructorFieldsList.size(), dialog, type));

		int dialogResult= dialog.open();
		if (dialogResult == Window.OK) {
			Object[] checkedElements= dialog.getResult();
			if (checkedElements == null)
				return;
			ArrayList result= new ArrayList(checkedElements.length);
			for (int i= 0; i < checkedElements.length; i++) {
				Object curr= checkedElements[i];
				if (curr instanceof IField) {
					result.add(curr);
				}
			}
			IEditorPart editor= EditorUtility.openInEditor(type.getCompilationUnit());
					
			IField[] workingCopyFields= (IField[]) result.toArray(new IField[result.size()]);

			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject());
			settings.createComments= dialog.getGenerateComment();

			IJavaElement elementPosition= dialog.getElementPosition();
			
			IMethod selectedConstructor= dialog.getSuperConstructorChoice();

			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null) {
				target.beginCompoundChange();
			}
			try {						
				AddCustomConstructorOperation op= new AddCustomConstructorOperation(type, settings, workingCopyFields, false, elementPosition, selectedConstructor);
				op.setVisbility(dialog.getVisibilityModifier());
				// Ignore the omit super() checkbox if the default constructor is not chosen
				if (selectedConstructor.getParameterNames().length == 0)
					op.setOmitSuper(dialog.isOmitSuper());
				

				IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
				if (context == null) {
					context= new BusyIndicatorRunnableContext();
				}
				PlatformUI.getWorkbench().getProgressService().runInUI(context,
					new WorkbenchRunnableAdapter(op, op.getScheduleRule()),
					op.getScheduleRule());
				IMethod res= op.getCreatedConstructor();
				JavaModelUtil.reconcile(res.getCompilationUnit());
				EditorUtility.revealInEditor(editor, res);

			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.actionfailed")); //$NON-NLS-1$
			} catch (InterruptedException e) {
				// Do nothing. Operation has been cancelled by user.
			} finally {
				if (target != null) {
					target.endCompoundChange();
				}
			}
		}
	}
	
	private static IMethod[] getSuperConstructors(IType type) throws CoreException {
		List constructorMethods= new ArrayList();
		
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(null);				
		IType supertype= hierarchy.getSuperclass(type);

		if (supertype != null) {
			IMethod[] superMethods= supertype.getMethods();
			boolean constuctorFound= false;
			for (int i= 0; i < superMethods.length; i++) {
					IMethod curr= superMethods[i];
					if (curr.isConstructor())  {
						constuctorFound= true;
						if (JavaModelUtil.isVisibleInHierarchy(curr, type.getPackageFragment())) {
							constructorMethods.add(curr);
						}
					}
			}
			
			if (!constuctorFound)  {
				IType objectType= type.getJavaProject().findType("java.lang.Object"); //$NON-NLS-1$
				IMethod curr= objectType.getMethod("Object", new String[0]);  //$NON-NLS-1$
				constructorMethods.add(curr);
			}
		}
		return (IMethod[]) constructorMethods.toArray(new IMethod[constructorMethods.size()]);
	}
	

	static ISelectionStatusValidator createValidator(int entries, GenerateConstructorUsingFieldsSelectionDialog dialog, IType type) {
		GenerateConstructorUsingFieldsValidator validator= new GenerateConstructorUsingFieldsValidator(entries, dialog, type);
		return validator;
	}

}
