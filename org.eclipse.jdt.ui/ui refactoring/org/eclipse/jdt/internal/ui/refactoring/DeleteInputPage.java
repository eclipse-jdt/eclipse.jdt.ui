package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.changes.DeleteRefactoring;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class DeleteInputPage extends UserInputWizardPage {
	
	private CheckedListDialogField fDialogField;
	private SelectionButtonDialogField fCheckPrecondtions;
	
	public DeleteInputPage() {
		super("DeletePage", true);
		//FIX ME
		setDescription("description");
	}

	public void createControl(Composite parent) {
		Composite c= new Composite(parent, SWT.NONE);
		setControl(c);

		MGridLayout layout= new MGridLayout();
		layout.numColumns= 3;
		layout.marginWidth= 0;
		c.setLayout(layout);
		
		createLabel(c, "Warning: This operation cannot be undone.", Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				
		fCheckPrecondtions= new SelectionButtonDialogField(SWT.CHECK);
		fCheckPrecondtions.setEnabled(false);
		fCheckPrecondtions.setSelection(false);
		fCheckPrecondtions.setLabelText("Check if deleted elements are used in the program.");
		fCheckPrecondtions.doFillIntoGrid(c, 3);
		
		if (anyProjectsToDelete()){
			createLabel(c, "Select the projects for which you want to delete contents in the file system.", null);
			
			fDialogField= new CheckedListDialogField(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), 0);
			fDialogField.setCheckAllButtonLabel("Check All");
			fDialogField.setUncheckAllButtonLabel("Uncheck All");
			fDialogField.setElements(getProjectsToDelete());
			fDialogField.doFillIntoGrid(c, 3);
		}	
	}
	
	private void createLabel(Composite parent, String text, Color color){
		Separator s= new Separator(SWT.NONE){
			public Control[] doFillIntoGrid(Composite parent, int nColumns) {
				return doFillIntoGrid(parent, nColumns, 20);
			}
		};
		if (color != null)
			s.getSeparator(parent).setForeground(color);
		((Label)s.getSeparator(parent)).setText(text);
		s.doFillIntoGrid(parent, 3);
	}
	
	public boolean performFinish(){
		initializeRefactoring();
		return super.performFinish();
	}
	
	public IWizardPage getNextPage() {
		initializeRefactoring();
		return super.getNextPage();
	}
	
	private void initializeRefactoring(){
		getDeleteRefactoring().setDeleteProjectContents(getCheckedProjects());
		getDeleteRefactoring().setCheckIfUsed(fCheckPrecondtions.isSelected());
	}
	
	private List getProjectsToDelete(){
		List result= new ArrayList();
		for (Iterator iter= getElementsToDelete().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (each instanceof IJavaProject)
				result.add(each);
		}
		return result;
	}
	
	private List getElementsToDelete(){
		return getDeleteRefactoring().getElementsToDelete();
	}
	
	private boolean anyProjectsToDelete(){
		for (Iterator iter= getElementsToDelete().iterator(); iter.hasNext(); ){
			if (iter.next() instanceof IJavaProject)
				return true;
		}
		return false;
	}
	
	private DeleteRefactoring getDeleteRefactoring(){
		return (DeleteRefactoring)getRefactoring();
	}
	
	private List getCheckedProjects(){
		if (fDialogField == null)
			return new ArrayList(0);
			
		List checkedJavaProjects= fDialogField.getCheckedElements();
		if (checkedJavaProjects == null)
			return new ArrayList(0);
			
		List res= new ArrayList(checkedJavaProjects.size());
		for (Iterator iter= checkedJavaProjects.iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (each instanceof IJavaProject)
				res.add(((IJavaProject)each).getProject());
			else if (each instanceof IProject)	
				res.add(each);
			else
				Assert.isTrue(false, "this list should contain only projects");
		}
		return res;
	}
}

