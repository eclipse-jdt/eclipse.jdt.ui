/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.ui.IEditorPart;import org.eclipse.ui.PartInitException;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IField;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.IUIConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.codemanipulation.AddGetterSetterOperation;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * Create Getter and Setter for a selected field
 * Always forces the he field to be in an open editor. The result is unsaved,
 * so the user can decide if he wnats to accept the changes
 */
public class AddGetterSetterAction extends JavaUIAction {

	private static final String PREFIX= "AddGetterSetterAction.";
	
	private static final String NOTINWORKINGCOPY_PREFIX= PREFIX + "NotInWorkingCopyDialog.";
	private static final String GETSETALREADYEXISTS_PREFIX= PREFIX + "GetterSetterAlreadyExistsDialog.";
	private static final String SETALREADYEXISTS_PREFIX= PREFIX + "SetterAlreadyExistsDialog.";
	private static final String GETALREADYEXISTS_PREFIX= PREFIX + "GetterAlreadyExistsDialog.";
		
	private ISelectionProvider fSelectionProvider;

	public AddGetterSetterAction(ISelectionProvider selProvider) {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		fSelectionProvider= selProvider;
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.GETTERSETTER_ACTION });
	}

	public void run() {
		IField field= getSelectedField();
		if (field == null) {
			return;
		}

		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		
		try {
			ICompilationUnit cu= field.getCompilationUnit();
			IType parentType= field.getDeclaringType();
			// open an editor and work on a working copy
			IEditorPart editor= EditorUtility.openInEditor(parentType);
			IType workingCopyType= EditorUtility.getWorkingCopy(parentType);
			if (workingCopyType != null) {
				field= workingCopyType.getField(field.getElementName());
				if (!field.exists()) {
					showSimpleDialog(shell, NOTINWORKINGCOPY_PREFIX);
				}
			} else {
				showSimpleDialog(shell, NOTINWORKINGCOPY_PREFIX);
				return;				
			}
			
			AddGetterSetterOperation op= new AddGetterSetterOperation(field);

			ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
			dialog.run(false, true, op);
			IMethod getter= op.getCreatedGetter();
			IMethod setter= op.getCreatedSetter();
			if (getter == null && setter == null) {
				showSimpleDialog(shell, GETSETALREADYEXISTS_PREFIX);
			} else if (getter == null) {
				showSimpleDialog(shell, GETALREADYEXISTS_PREFIX);
			} else if (setter == null) {
				showSimpleDialog(shell, SETALREADYEXISTS_PREFIX);
			}
			
			if (editor != null) {
				if (getter != null) {
					EditorUtility.revealInEditor(editor, getter);
				} else if (setter != null) {
					EditorUtility.revealInEditor(editor, setter);
				}
			}
		} catch (InvocationTargetException e) {
			MessageDialog.openError(shell, "AddGetterSetterAction failed", e.getTargetException().getMessage());
		} catch (JavaModelException e) {
			ErrorDialog.openError(shell, "AddGetterSetterAction failed", null, e.getStatus());
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by the user.
		} catch (PartInitException e) {
			MessageDialog.openError(shell, "AddGetterSetterAction failed", e.getMessage());
		}
		
	}
	
	private void showSimpleDialog(Shell shell, String resourcePrefix) {
		JavaPlugin plugin= JavaPlugin.getDefault();
		String okLabel= plugin.getResourceString(IUIConstants.KEY_OK);
		String message= plugin.getResourceString(resourcePrefix + "message");
		String title= plugin.getResourceString(resourcePrefix + "title");
	
		MessageDialog dialog= new MessageDialog(shell, title, null, message, SWT.ICON_INFORMATION,
	 		new String[] { okLabel }, 0);
	 	dialog.open();
	}
	
	private IField getSelectedField() {
		ISelection sel= fSelectionProvider.getSelection();
		if (sel instanceof IStructuredSelection) {
			Object[] elements= ((IStructuredSelection)sel).toArray();
			if (elements.length == 1 && elements[0] instanceof IField) {
				IField field= (IField)elements[0];
				if (field.getCompilationUnit() != null) {
					return field;
				}
			}
		}
		return null;
	}		
	
	
	public boolean canActionBeAdded() {
		return getSelectedField() != null;
	}

}