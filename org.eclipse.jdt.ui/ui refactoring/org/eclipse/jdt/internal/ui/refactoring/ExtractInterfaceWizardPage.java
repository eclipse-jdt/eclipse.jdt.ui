/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.types.ExtractInterfaceRefactoring;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;

public class ExtractInterfaceWizardPage extends TextInputWizardPage {
	
	private static final String PREFIX= "Refactoring.ExtractInterface.inputPage.";
	
	private CheckedListDialogField fMethodList;
	
	public ExtractInterfaceWizardPage() {
		super(true);
	}
	
	/* (non-JavaDoc)
	 * Method declared in EditorSavingWizardPage.
	 */
	protected DialogField[] createDialogFields() {
		return new DialogField[] { createStringDialogField(), createMethodList(), getEditorList() };
	}	
	
	private CheckedListDialogField createMethodList(){
		LabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		CheckedListDialogField list= new CheckedListDialogField(null, null, labelProvider, 0);
		list.setCheckAllButtonLabel(RefactoringResources.getResourceString(PREFIX + "checkAll"));
		list.setUncheckAllButtonLabel(RefactoringResources.getResourceString(PREFIX + "uncheckAll"));
		list.setLabelText(RefactoringResources.getResourceString(PREFIX + "methods"));
		fMethodList= list;
		return list;
	}
	
	protected RefactoringStatus validatePage(){
		ExtractInterfaceRefactoring ref= (ExtractInterfaceRefactoring)getRefactoring();		
		ref.setInterfaceName(getNewName());
		List list= fMethodList.getCheckedElements();
		ref.setMethods((IMethod[])list.toArray(new IMethod[list.size()]));
		return ref.checkUserInput();
	}
	
	private void setUpMethodList(){
		if (fMethodList == null)
			createMethodList();
		List methods= getMethods();
		fMethodList.setEnabled(methods.size() != 0);
		fMethodList.setElements(methods); 
		fMethodList.checkAll(true);
	}
	
	/* (non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public void setVisible(boolean visible) {
		if (visible){
			setUpMethodList();
		}	
		super.setVisible(visible);
	}

	private List getMethods(){
		ExtractInterfaceRefactoring ref= (ExtractInterfaceRefactoring)getRefactoring();
		IType type= ref.getType();
		try{
			IMethod[] methods= type.getMethods();
			ArrayList list= new ArrayList(5);
			for (int i=0; i < methods.length; i++){
				if (isAPIMethod(methods[i]))
					list.add(methods[i]);
			}
			return list;
		} catch (JavaModelException e){
			//cannot propagate
			return null;
		}
	}
	
	private static boolean isAPIMethod(IMethod method) throws JavaModelException{ 
		return method.exists() 
			&& (! method.isConstructor())
			&& Flags.isPublic(method.getFlags())
			&& (! Flags.isStatic(method.getFlags()));		
	}
}