/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Creates unimplemented constructors for a type.
 * <p>
 * Will open the parent compilation unit in a Java editor. Opens a dialog
 * with a list of constructors from the super class which can be generated.
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
 * @since 2.0
 */
public class AddUnimplementedConstructorsAction extends SelectionDispatchAction {
	
	private CompilationUnitEditor fEditor;
	private static final String dialogTitle= ActionMessages.getString("AddUnimplementedConstructorsAction.error.title"); //$NON-NLS-1$

	/**
	 * Creates a new <code>AddUnimplementedConstructorsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddUnimplementedConstructorsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("AddUnimplementedConstructorsAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("AddUnimplementedConstructorsAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddUnimplementedConstructorsAction.tooltip")); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_UNIMPLEMENTED_CONSTRUCTORS_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public AddUnimplementedConstructorsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}
	
	//---- Structured Viewer -----------------------------------------------------------
	
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
		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			IType type= (IType) selection.getFirstElement();
			return type.getCompilationUnit() != null && type.isClass(); // look if class: not cheap but done by all source generation actions
		}

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return true;

		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		Shell shell= getShell();
		try {
			IType type= getSelectedType(selection);
			if (type == null) {
				MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.not_applicable")); //$NON-NLS-1$
				return;
			}		
			// open an editor and work on a working copy
			IEditorPart editor= EditorUtility.openInEditor(type);
			type= (IType)EditorUtility.getWorkingCopy(type);
			
			if (type == null) {
				MessageDialog.openError(shell, getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.error.type_removed_in_editor")); //$NON-NLS-1$
				return;
			}
			
			run(shell, type, editor, false);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, getDialogTitle(), null);
		}			
	}

	//---- Java Editior --------------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(ITextSelection selection) {
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		Shell shell= getShell();
		try {
			IType type= SelectionConverter.getTypeAtOffset(fEditor);
			if (type != null)
				run(shell, type, fEditor, true);
			else
				MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.not_applicable")); //$NON-NLS-1$
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		}
	}
	
	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}	
	
	//---- Helpers -------------------------------------------------------------------
	
	private void run(Shell shell, IType type, IEditorPart editor, boolean activatedFromEditor) throws CoreException {
		if (!ElementValidator.check(type, getShell(), getDialogTitle(), activatedFromEditor)) {
			return;
		}
		if (!ActionUtil.isProcessable(getShell(), type)) {
			return;		
		}		

		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(type);
		
		if (constructorMethods.length == 0) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.error.nothing_found")); //$NON-NLS-1$
			return;
		}
		
		JavaElementLabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		AddUnimplementedConstructorsContentProvider contentProvider = new AddUnimplementedConstructorsContentProvider(constructorMethods);			

		SourceActionDialog dialog= new SourceActionDialog(shell, labelProvider, contentProvider, fEditor, type);
		dialog.setCommentString(ActionMessages.getString("SourceActionDialog.createConstructorComment")); //$NON-NLS-1$
		dialog.setTitle(getDialogTitle());
		dialog.setInitialSelections(constructorMethods);
		dialog.setContainerMode(true);
		dialog.setSorter(new JavaElementSorter());
		dialog.setSize(60, 18);			
		dialog.setInput(new Object());
		dialog.setMessage(ActionMessages.getString("AddUnimplementedConstructorsAction.dialog.label")); //$NON-NLS-1$
		dialog.setValidator(createValidator(constructorMethods.length));
		
		IMethod[] selected= null;
		int dialogResult = dialog.open();
		if (dialogResult == Window.OK) {			
			Object[] checkedElements = dialog.getResult();
			if (checkedElements == null)
				return;
			ArrayList result= new ArrayList(checkedElements.length);
			for (int i= 0; i < checkedElements.length; i++) {
				Object curr= checkedElements[i];
				if (curr instanceof IMethod) {
					result.add(curr);
				}
			}
						
			selected= (IMethod[]) result.toArray(new IMethod[result.size()]);
			
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			settings.createComments= dialog.getGenerateComment();
	
			IJavaElement elementPosition= dialog.getElementPosition();
			AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(type, settings, selected, false, elementPosition);
			
			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null) {
				target.beginCompoundChange();		
			}
			try {
				IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
				if (context == null) {
					context= new BusyIndicatorRunnableContext();
				}				
				context.run(false, true, new WorkbenchRunnableAdapter(op));
				IMethod[] res= op.getCreatedMethods();
				if (res == null || res.length == 0) {
					MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.error.nothing_found")); //$NON-NLS-1$
				} else if (editor != null) {
					EditorUtility.revealInEditor(editor, res[0]);
				}
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, shell, getDialogTitle(), null); 
			} catch (InterruptedException e) {
				// Do nothing. Operation has been canceled by user.
			} finally {
				if (target != null) {
					target.endCompoundChange();		
				}
			}		
		}
	}
	
	private static ISelectionStatusValidator createValidator(int entries) {
		AddUnimplementedConstructorsValidator validator= new AddUnimplementedConstructorsValidator(entries);
		return validator;
	}
		
	private IType getSelectedType(IStructuredSelection selection) throws JavaModelException {
		Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			IType type= (IType) elements[0];
			if (type.getCompilationUnit() != null && type.isClass()) {
				return type;
			}
		}
		else if (elements[0] instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit) elements[0];
			IType type= cu.findPrimaryType();
			if (type != null && !type.isInterface())
				return type;
		}
		return null;
	}	
	
	private String getDialogTitle() {
		return dialogTitle;
	}	

	private static class AddUnimplementedConstructorsContentProvider implements ITreeContentProvider {
		
		private IMethod[] fMethodsList;
		private static final Object[] EMPTY= new Object[0];
		
		public AddUnimplementedConstructorsContentProvider(IMethod[] methodList) {
			fMethodsList= methodList;
		}
		
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			return EMPTY;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			return null;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}
		
				
		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fMethodsList;
		}
		
		
		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}									
	}
	
	private static class AddUnimplementedConstructorsValidator implements ISelectionStatusValidator {
		private static int fEntries;
			
		AddUnimplementedConstructorsValidator(int entries) {
			super();
			fEntries= entries;
		}

		public IStatus validate(Object[] selection) {
			int count= countSelectedMethods(selection);
			if (count == 0)
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			String message= ActionMessages.getFormattedString("AddUnimplementedConstructorsAction.methods_selected", //$NON-NLS-1$
																new Object[] { String.valueOf(count), String.valueOf(fEntries)} ); 																	
			return new StatusInfo(IStatus.INFO, message);
		}

		private int countSelectedMethods(Object[] selection){
			int count= 0;
			for (int i = 0; i < selection.length; i++) {
				if (selection[i] instanceof IMethod)
					count++;
			}
			return count;
		}		
	}	
}
