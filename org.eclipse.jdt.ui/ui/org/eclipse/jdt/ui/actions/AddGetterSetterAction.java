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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.codemanipulation.IRequestQuery;
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
import org.eclipse.jdt.internal.ui.refactoring.IVisibilityChangeListener;
import org.eclipse.jdt.internal.ui.refactoring.VisibilityControlUtil;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;


/**
 * Creates getter and setter methods for a type's fields. Opens a dialog
 * with a list of fields for which a setter or getter can be generated.
 * User is able to check or uncheck items before setters or getters
 * are generated.
 * <p>
 * Will open the parent compilation unit in a Java editor. The result is 
 * unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>IField</code> or <code>IType</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 */
public class AddGetterSetterAction extends SelectionDispatchAction {
	private boolean fSort;
	private boolean fSynchronized;
	private boolean fFinal;	
	private boolean fNative;
	private boolean fGenerateComment;
	private int fNumEntries;
	private CompilationUnitEditor fEditor;
	private static final String dialogTitle= ActionMessages.getString("AddGetterSetterAction.error.title"); //$NON-NLS-1$

	/**
	 * Creates a new <code>AddGetterSetterAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddGetterSetterAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("AddGetterSetterAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("AddGetterSetterAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddGetterSetterAction.tooltip")); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.GETTERSETTER_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public AddGetterSetterAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(editor) != null);
		fEditor.getEditorSite();
	}
		
	//---- Structured Viewer -----------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}
		
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(IStructuredSelection selection) {
		try {
			IField[] selectedFields= getSelectedFields(selection);
			if (canEnableOn(selectedFields)){
				run(selectedFields[0].getDeclaringType(), selectedFields, false);
				return;
			}	
			Object firstElement= selection.getFirstElement();

			if (firstElement instanceof IType)
				run((IType)firstElement, new IField[0], false);
			else if (firstElement instanceof ICompilationUnit)	
				run(((ICompilationUnit) firstElement).findPrimaryType(), new IField[0], false);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), dialogTitle, ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
		}
				
	}
	
	private boolean canEnable(IStructuredSelection selection) throws JavaModelException{
		if (canEnableOn(getSelectedFields(selection)))
			return true;
			
		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType))
			return canEnableOn((IType)selection.getFirstElement());

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return canEnableOn(((ICompilationUnit)selection.getFirstElement()).findPrimaryType());

		return false;	
	}

	private static boolean canEnableOn(IType type) throws JavaModelException {
		if (type == null)
			return false;
		if (type.getFields().length == 0)
			return false;
		if (type.getCompilationUnit() == null)
			return false;
		return true;
	}
	
	private static boolean canEnableOn(IField[] fields) throws JavaModelException {
		return fields != null && fields.length > 0;
	}
	
	private void resetNumEntries() {
		fNumEntries= 0;
	}
	
	private void incNumEntries() {
		fNumEntries++;
	}

	private void run(IType type, IField[] preselected, boolean editor) throws CoreException {		
		if (!ElementValidator.check(type, getShell(), dialogTitle, editor))
			return;
		if (!ActionUtil.isProcessable(getShell(), type))
			return;			
		
		ILabelProvider lp= new AddGetterSetterLabelProvider();
		resetNumEntries();
		Map entries= createGetterSetterMapping(type);
		if (entries.isEmpty()){
			MessageDialog.openInformation(getShell(), dialogTitle, ActionMessages.getString("AddGettSetterAction.typeContainsNoFields.message")); //$NON-NLS-1$
			return;
		}	
		ITreeContentProvider cp= new AddGetterSetterContentProvider(entries);
		GetterSetterTreeSelectionDialog dialog= new GetterSetterTreeSelectionDialog(getShell(), lp, cp, fEditor, type);
		dialog.setSorter(new JavaElementSorter());
		dialog.setTitle(dialogTitle);
		String message= ActionMessages.getString("AddGetterSetterAction.dialog.title");//$NON-NLS-1$
		dialog.setMessage(message);
		dialog.setValidator(createValidator(fNumEntries));
		dialog.setContainerMode(true);
		dialog.setSize(60, 18);
		dialog.setInput(type);
		dialog.setExpandedElements(type.getFields());
		dialog.setInitialSelections(preselected);
		int dialogResult= dialog.open();
		if (dialogResult == Window.OK) {
			Object[] result= dialog.getResult();
			if (result == null)
				return;
			fSort= dialog.getSort();
			fSynchronized= dialog.getSynchronized();
			fNative= dialog.getNative();
			fFinal= dialog.getFinal();
			fGenerateComment= dialog.getGenerateComment();
			IField[] getterFields, setterFields, getterSetterFields;
			if (fSort) {
				getterFields= getGetterFields(result);
				setterFields= getSetterFields(result);
				getterSetterFields= new IField[0];
			}
			else {
				getterFields= getGetterOnlyFields(result);
				setterFields= getSetterOnlyFields(result);
				getterSetterFields= getGetterSetterFields(result);			
			}
			IJavaElement elementPosition= dialog.getElementPosition();
			
			generate(getterFields, setterFields, getterSetterFields, elementPosition);
		}
	}
	
	private static class AddGetterSetterSelectionStatusValidator implements ISelectionStatusValidator {
		private static int fEntries;
			
		AddGetterSetterSelectionStatusValidator(int entries) {
			fEntries= entries;
		}
		
		public IStatus validate(Object[] selection) {
			int count= countSelectedMethods(selection);
			if (count == 0)
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			String message= ActionMessages.getFormattedString("AddGetterSetterAction.methods_selected", //$NON-NLS-1$
																new Object[] { String.valueOf(count), String.valueOf(fEntries)} ); 																	
			return new StatusInfo(IStatus.INFO, message);
		}

		private int countSelectedMethods(Object[] selection){
			int count= 0;
			for (int i = 0; i < selection.length; i++) {
				if (selection[i] instanceof GetterSetterEntry)
					count++;
			}
			return count;
		}			
	}
	
	private static ISelectionStatusValidator createValidator(int entries) {
		AddGetterSetterSelectionStatusValidator validator= new AddGetterSetterSelectionStatusValidator(entries);
		return validator;
	}

	//	returns a list of fields with setter entries checked
	private static IField[] getSetterFields(Object[] result){
		Collection list= new ArrayList(0);
		Object each= null;
		GetterSetterEntry entry= null;
		for (int i = 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)){ 
				entry= (GetterSetterEntry)each;
				if (! entry.fGetterEntry) {
					list.add(entry.fField);
				}
			}
		}
		return (IField[]) list.toArray(new IField[list.size()]);			
	}
	
	//	returns a list of fields with getter entries checked
	private static IField[] getGetterFields(Object[] result){
		Collection list= new ArrayList(0);
		Object each= null;
		GetterSetterEntry entry= null;
		for (int i = 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)){ 
				entry= (GetterSetterEntry)each;
				if (entry.fGetterEntry) {
					list.add(entry.fField);
				}
			}
		}
		return (IField[]) list.toArray(new IField[list.size()]);			
	}
	
	//	returns a list of fields with only getter entires checked
	private static IField[] getGetterOnlyFields(Object[] result){
		Collection list= new ArrayList(0);
		Object each= null;
		GetterSetterEntry entry= null;
		boolean getterSet= false;
		for (int i = 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)){ 
				entry= (GetterSetterEntry)each;
				if (entry.fGetterEntry) {
					list.add(entry.fField);
					getterSet= true;
				}
				if ((! entry.fGetterEntry) && (getterSet == true)) {
					list.remove(entry.fField);
					getterSet= false;
				}
			}
			else
				getterSet= false;	
		}
		return (IField[]) list.toArray(new IField[list.size()]);			
	}
	
	//	returns a list of fields with only setter entries checked
	private static IField[] getSetterOnlyFields(Object[] result){
		Collection list= new ArrayList(0);
		Object each= null;	
		GetterSetterEntry entry= null;
		boolean getterSet= false;
		for (int i = 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)){ 
				entry= (GetterSetterEntry)each;
				if (entry.fGetterEntry) {
					getterSet= true;
				}
				if ((! entry.fGetterEntry) && (getterSet != true)) {
					list.add(entry.fField);
					getterSet= false;
				}
			}
			else
				getterSet= false;	
		}
		return (IField[]) list.toArray(new IField[list.size()]);					
	}
	
	// returns a list of fields with both entries checked
	private static IField[] getGetterSetterFields(Object[] result){
		Collection list= new ArrayList(0);
		Object each= null;		
		GetterSetterEntry entry= null;
		boolean getterSet= false;
		for (int i = 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)){ 
				entry= (GetterSetterEntry)each;
				if (entry.fGetterEntry) {
					getterSet= true;
				}
				if ((! entry.fGetterEntry) && (getterSet == true)) {
					list.add(entry.fField);
					getterSet= false;
				}
			}
			else
				getterSet= false;	
		}
		return (IField[]) list.toArray(new IField[list.size()]);			
	}
	
	private void generate(IField[] getterFields, IField[] setterFields, IField[] getterSetterFields, IJavaElement elementPosition) throws CoreException{
		if (getterFields.length == 0 && setterFields.length == 0 && getterSetterFields.length == 0)
			return;
		
		ICompilationUnit cu= null;
		if (getterFields.length != 0)
			cu= getterFields[0].getCompilationUnit();
		else if (setterFields.length != 0)
			cu= setterFields[0].getCompilationUnit();
		else
			cu= getterSetterFields[0].getCompilationUnit();
		//open the editor, forces the creation of a working copy
		IEditorPart editor= EditorUtility.openInEditor(cu);
		
		IField[] workingCopyGetterFields= getWorkingCopyFields(getterFields);
		IField[] workingCopySetterFields= getWorkingCopyFields(setterFields);
		IField[] workingCopyGetterSetterFields= getWorkingCopyFields(getterSetterFields);
		IJavaElement workingCopyElementPosition= getWorkingCopy(elementPosition);

		if (workingCopyGetterFields != null && workingCopySetterFields != null && workingCopyGetterSetterFields != null)
			run(workingCopyGetterFields, workingCopySetterFields, workingCopyGetterSetterFields, editor, workingCopyElementPosition);			
	}
	
	private static IJavaElement getWorkingCopy(IJavaElement elementPosition) {
		if(elementPosition instanceof IMember)
			return JavaModelUtil.toWorkingCopy((IMember)elementPosition);
		return elementPosition;
	}

	private IField[] getWorkingCopyFields(IField[] fields) throws CoreException{
		if (fields.length == 0)
			return new IField[0];
		ICompilationUnit cu= fields[0].getCompilationUnit();
		
		ICompilationUnit workingCopyCU;
		IField[] workingCopyFields;
		if (cu.isWorkingCopy()) {
			workingCopyCU= cu;
			workingCopyFields= fields;
		} else {
			workingCopyCU= EditorUtility.getWorkingCopy(cu);
			if (workingCopyCU == null) {
				showError(ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
				return null;
			}
			workingCopyFields= new IField[fields.length];
			for (int i= 0; i < fields.length; i++) {
				IField field= fields[i];
				IField workingCopyField= (IField) JavaModelUtil.findMemberInCompilationUnit(workingCopyCU, field);
				if (workingCopyField == null) {
					showError(ActionMessages.getFormattedString("AddGetterSetterAction.error.fieldNotExisting", field.getElementName())); //$NON-NLS-1$
					return null;
				}
				workingCopyFields[i]= workingCopyField;
			}
		}
		return workingCopyFields;	
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
	protected void run(ITextSelection selection) {
		try {
			IJavaElement input= SelectionConverter.getInput(fEditor);
			if (!ActionUtil.isProcessable(getShell(), input))
				return;					
			
			IJavaElement[] elements= SelectionConverter.codeResolve(fEditor);
			if (elements.length == 1 && (elements[0] instanceof IField)) {
				IField field= (IField)elements[0];
				run(field.getDeclaringType(), new IField[] {field}, true);
				return;
			}
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			
			if (element != null){
				IType type= (IType)element.getAncestor(IJavaElement.TYPE);
				if (type != null){
					if (type.getFields().length > 0){
						run(type, new IField[0], true);	
						return;
					} 
				}
			} 
			MessageDialog.openInformation(getShell(), dialogTitle, 
				ActionMessages.getString("AddGetterSetterAction.not_applicable")); //$NON-NLS-1$
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), dialogTitle, ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
		}
	}
		
	//---- Helpers -------------------------------------------------------------------
	
	private void run(IField[] getterFields, IField[] setterFields, IField[] getterSetterFields, IEditorPart editor, IJavaElement elementPosition) {
		IRewriteTarget target= (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);
		if (target != null) {
			target.beginCompoundChange();
		}
		try {
			AddGetterSetterOperation op= createAddGetterSetterOperation(getterFields, setterFields, getterSetterFields, elementPosition);
			setOperationStatusFields(op);
			
			IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
			if (context == null) {
				context= new BusyIndicatorRunnableContext();
			}
			context.run(false, true, new WorkbenchRunnableAdapter(op));
		
			IMethod[] createdMethods= op.getCreatedAccessors();
			if (createdMethods.length > 0) {
				EditorUtility.revealInEditor(editor, createdMethods[0]);
			}		
		} catch (InvocationTargetException e) {
			String message= ActionMessages.getString("AddGetterSetterAction.error.actionfailed"); //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), dialogTitle, message);
		} catch (InterruptedException e) {
			// operation cancelled
		} finally {
			if (target != null) {
				target.endCompoundChange();
			}
		}
	}
	
	private void setOperationStatusFields(AddGetterSetterOperation op) {
		// Set the status fields corresponding to the visibility and modifiers set
		op.setSort(fSort);
		op.setVisibility(Modifier.PUBLIC);
		op.setSynchronized(fSynchronized);
		op.setFinal(fFinal);
		op.setNative(fNative);
	}

	private AddGetterSetterOperation createAddGetterSetterOperation(IField[] getterFields, IField[] setterFields, IField[] getterSetterFields, IJavaElement elementPosition) {
		IRequestQuery skipSetterForFinalQuery= skipSetterForFinalQuery();
		IRequestQuery skipReplaceQuery= skipReplaceQuery();
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		settings.createComments= fGenerateComment;
		return new AddGetterSetterOperation(getterFields, setterFields, getterSetterFields, settings, skipSetterForFinalQuery, skipReplaceQuery, elementPosition);
	}
	
	private IRequestQuery skipSetterForFinalQuery() {
		return new IRequestQuery() {
			public int doQuery(IMember field) {
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19367
				int[] returnCodes= {IRequestQuery.YES, IRequestQuery.YES_ALL, IRequestQuery.NO, IRequestQuery.CANCEL};
				String[] options= {IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
				String fieldName= JavaElementLabels.getElementLabel(field, 0);
				String formattedMessage= ActionMessages.getFormattedString("AddGetterSetterAction.SkipSetterForFinalDialog.message", fieldName); //$NON-NLS-1$
				return showQueryDialog(formattedMessage, options, returnCodes);	
			}
		};
	}
	
	private IRequestQuery skipReplaceQuery() {
		return new IRequestQuery() {
			public int doQuery(IMember method) {
				int[] returnCodes= {IRequestQuery.YES, IRequestQuery.NO, IRequestQuery.YES_ALL, IRequestQuery.CANCEL};
				String skipLabel= ActionMessages.getString("AddGetterSetterAction.SkipExistingDialog.skip.label"); //$NON-NLS-1$
				String replaceLabel= ActionMessages.getString("AddGetterSetterAction.SkipExistingDialog.replace.label"); //$NON-NLS-1$
				String skipAllLabel= ActionMessages.getString("AddGetterSetterAction.SkipExistingDialog.skipAll.label"); //$NON-NLS-1$
				String[] options= { skipLabel, replaceLabel, skipAllLabel, IDialogConstants.CANCEL_LABEL};
				String methodName= JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES);
				String formattedMessage= ActionMessages.getFormattedString("AddGetterSetterAction.SkipExistingDialog.message", methodName); //$NON-NLS-1$
				return showQueryDialog(formattedMessage, options, returnCodes);		
			}
		};
	}
	
	
	private int showQueryDialog(final String message, final String[] buttonLabels, int[] returnCodes) {
		final Shell shell= getShell();
		if (shell == null) {
			JavaPlugin.logErrorMessage("AddGetterSetterAction.showQueryDialog: No active shell found"); //$NON-NLS-1$
			return IRequestQuery.CANCEL;
		}		
		final int[] result= { MessageDialog.CANCEL };
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= ActionMessages.getString("AddGetterSetterAction.QueryDialog.title"); //$NON-NLS-1$
				MessageDialog dialog= new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, buttonLabels, 0);
				result[0]= dialog.open();				
			}
		});
		int returnVal= result[0];
		return returnVal < 0 ? IRequestQuery.CANCEL : returnCodes[returnVal];
	}	

	private void showError(String message) {
		MessageDialog.openError(getShell(), dialogTitle, message);
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
					IField fld= (IField)curr;
					
					if (i == 0) {
						// remember the cu of the first element
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
							// no setters/getters for interfaces
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
	
	private static class AddGetterSetterLabelProvider extends JavaElementLabelProvider {
		AddGetterSetterLabelProvider() {
		}
		
		/*
		 * @see ILabelProvider#getText(Object)
		 */
		public String getText(Object element) {
			if (element instanceof GetterSetterEntry) {
				GetterSetterEntry entry= (GetterSetterEntry) element;
				try {
					if (entry.fGetterEntry) {
						return GetterSetterUtil.getGetterName(entry.fField, null) + "()"; //$NON-NLS-1$
					} else {
						return GetterSetterUtil.getSetterName(entry.fField, null) + '(' + Signature.getSimpleName(Signature.toString(entry.fField.getTypeSignature())) + ')';
					}
				} catch (JavaModelException e) {
					return ""; //$NON-NLS-1$
				}
			}
			return super.getText(element);
		}

		/*
		 * @see ILabelProvider#getImage(Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof GetterSetterEntry) {
				int flags= 0;
				try {
					flags= ((GetterSetterEntry) element).fField.getFlags();
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
				ImageDescriptor desc= JavaElementImageProvider.getFieldImageDescriptor(false, Flags.AccPublic);
				int adornmentFlags= Flags.isStatic(flags) ? JavaElementImageDescriptor.STATIC : 0;
				desc= new JavaElementImageDescriptor(desc, adornmentFlags, JavaElementImageProvider.BIG_SIZE);
				return JavaPlugin.getImageDescriptorRegistry().get(desc);
			}
			return super.getImage(element);
		}
	}
	
	/**
	 * @return map IField -> GetterSetterEntry[]
	 */
	private Map createGetterSetterMapping(IType type) throws JavaModelException{
		IField[] fields= type.getFields();
		Map result= new HashMap();
		for (int i= 0; i < fields.length; i++) {
			List l= new ArrayList(2);
			if (GetterSetterUtil.getGetter(fields[i]) == null) {
				l.add(new GetterSetterEntry(fields[i], true));
				incNumEntries();
			}
				
			if (GetterSetterUtil.getSetter(fields[i]) == null) {		
				l.add(new GetterSetterEntry(fields[i], false));	
				incNumEntries();
			}

			if (! l.isEmpty())
				result.put(fields[i], (GetterSetterEntry[]) l.toArray(new GetterSetterEntry[l.size()]));
		}
		return result;
	}
	
	private static class AddGetterSetterContentProvider implements ITreeContentProvider{
		private static final Object[] EMPTY= new Object[0];
		private Map fGetterSetterEntries;	//IField -> Object[] (with 0 to 2 elements of type GetterSetterEntry)
		public AddGetterSetterContentProvider(Map entries) throws JavaModelException {
			fGetterSetterEntries= entries;
		}
		
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IField)
				return (Object[])fGetterSetterEntries.get(parentElement);	
			return EMPTY;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMember) 
				return ((IMember)element).getDeclaringType();
			if (element instanceof GetterSetterEntry)
				return ((GetterSetterEntry)element).fField;
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
			return fGetterSetterEntries.keySet().toArray();
		}
		
		
		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
			fGetterSetterEntries.clear();
			fGetterSetterEntries= null;
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	private static class GetterSetterTreeSelectionDialog extends SourceActionDialog {

		private boolean fSort;
		private int fVisibility;
		private boolean fNative;
		private boolean fFinal;
		private boolean fSynchronized;
		private ITreeContentProvider fContentProvider;
		private static final int SELECT_GETTERS_ID= IDialogConstants.CLIENT_ID + 1;
		private static final int SELECT_SETTERS_ID= IDialogConstants.CLIENT_ID + 2;
				
		public GetterSetterTreeSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, CompilationUnitEditor editor, IType type) {
			super(parent, labelProvider, contentProvider, editor, type);
			fContentProvider= contentProvider;
		}
	
		public int getVisibility() {
			return fVisibility;
		}

		public boolean getSort() {
			return fSort;
		}
		
		public boolean getFinal() {
			return fFinal;
		}		

		public boolean getNative() {
			return fNative;
		}	
		
		public boolean getSynchronized() {
			return fSynchronized;
		}					
				
		protected void createGetterSetterButtons(Composite buttonComposite) {
			createButton(buttonComposite, SELECT_GETTERS_ID, ActionMessages.getString("GetterSetterTreeSelectionDialog.select_getters"), false); //$NON-NLS-1$	
			createButton(buttonComposite, SELECT_SETTERS_ID, ActionMessages.getString("GetterSetterTreeSelectionDialog.select_setters"), false); //$NON-NLS-1$				
		}
						
		protected void buttonPressed(int buttonId) {
			super.buttonPressed(buttonId);
			switch(buttonId) {
				case SELECT_GETTERS_ID: {
					getTreeViewer().setCheckedElements(getGetterSetterElements(true));
					updateOKStatus();						
					break;
				}
				case SELECT_SETTERS_ID: {
					getTreeViewer().setCheckedElements(getGetterSetterElements(false));
					updateOKStatus();									
					break;
				}
			}
		}			
		
		protected Composite createEntryPtCombo(Composite composite) {
			Composite entryComposite= super.createEntryPtCombo(composite);
			entryComposite= addSortOrder(entryComposite);
			entryComposite= addVisibilityAndModifiersChoices(entryComposite);
			return entryComposite;						
		}
		
		private Composite addSortOrder(Composite composite) {
			Label label= new Label(composite, SWT.NONE);
			label.setText(ActionMessages.getString("GetterSetterTreeSelectionDialog.sort_label")); //$NON-NLS-1$
			GridData gd= new GridData(GridData.FILL_BOTH);
			label.setLayoutData(gd);

			final Combo combo= new Combo(composite, SWT.READ_ONLY);
			combo.setItems(new String[]{ActionMessages.getString("GetterSetterTreeSelectionDialog.alpha_pair_sort"), //$NON-NLS-1$
										ActionMessages.getString("GetterSetterTreeSelectionDialog.alpha_method_sort")});  //$NON-NLS-1$  
			final int methodIndex= 1;	// Hard-coded. Change this if the list gets more complicated.
			combo.setText(combo.getItem(0));
			gd= new GridData(GridData.FILL_BOTH);
			gd.grabExcessHorizontalSpace= true;
			combo.setLayoutData(gd);
			combo.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					fSort= (combo.getSelectionIndex() == methodIndex);
				}
			});	
			return composite;
		}
		
		private Composite addVisibilityAndModifiersChoices(Composite buttonComposite) {
			// Add visibility and modifiers buttons: http://bugs.eclipse.org/bugs/show_bug.cgi?id=35870
			IVisibilityChangeListener visibilityChangeListener = new IVisibilityChangeListener(){
				public void visibilityChanged(int newVisibility) {
					fVisibility= newVisibility;
				}
				public void modifierChanged(int modifier, boolean isChecked) {	
					switch (modifier) {
						case Modifier.FINAL: fFinal= isChecked; return;
						case Modifier.SYNCHRONIZED: fSynchronized= isChecked; return;
						case Modifier.NATIVE: fNative= isChecked; return;
						default: return;
					}
				}
			};
			int correctVisibility= Modifier.PUBLIC;
			int[] availableVisibilities= new int[]{Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.NONE};
			int[] availableModifiers= new int[] {Modifier.FINAL, Modifier.SYNCHRONIZED, Modifier.NATIVE};
			
			Composite visibilityComposite= VisibilityControlUtil.createVisibilityControlAndModifiers(buttonComposite, visibilityChangeListener, availableVisibilities, correctVisibility, availableModifiers);
			((GridData)visibilityComposite.getLayoutData()).horizontalSpan = 4;		
			
			return visibilityComposite;				
		}
		
		private Object[] getGetterSetterElements(boolean isGetter){
			Object[] allFields= fContentProvider.getElements(null);
			Set result= new HashSet();
			for (int i = 0; i < allFields.length; i++) {
				IField field= (IField)allFields[i];
				GetterSetterEntry[] entries= getEntries(field);
				for (int j = 0; j < entries.length; j++) {
					AddGetterSetterAction.GetterSetterEntry entry= entries[j];
					if (entry.fGetterEntry == isGetter)
						result.add(entry);
				}
			}
			return result.toArray();
		}

		private GetterSetterEntry[] getEntries(IField field) {
			List result= Arrays.asList(fContentProvider.getChildren(field));
			return (GetterSetterEntry[]) result.toArray(new GetterSetterEntry[result.size()]);
		}
		
		protected Composite createSelectionButtons(Composite composite) {
			Composite buttonComposite= super.createSelectionButtons(composite);

			GridLayout layout = new GridLayout();
			buttonComposite.setLayout(layout);						

			createGetterSetterButtons(buttonComposite);
			
			layout.marginHeight= 0;
			layout.marginWidth= 0;						
			layout.numColumns= 1;
			
			return buttonComposite;
		}	

	}

	private static class GetterSetterEntry {
		public final IField fField;
		public final boolean fGetterEntry;

		GetterSetterEntry(IField field, boolean isGetterEntry){
			fField= field;
			fGetterEntry= isGetterEntry;
		}
	}

}
