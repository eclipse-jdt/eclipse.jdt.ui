/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.JdtHackFinder;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;

/**
 * First page of the refactoring wizard.
 * Presents a list of unsaved editors.
 * Subclass and override <code>createDialogFields</code> to add additional input controls
 */
public class EditorSavingWizardPage extends UserInputWizardPage {

	private CheckedListDialogField fList;
	private boolean fFirstTime= true;
	
	public static final String PAGE_NAME= "EditorSavingPage";
	private static final String PREFIX= PAGE_NAME + ".";
	
	/**
	 * Creates a new editor saving page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 */
	public EditorSavingWizardPage(boolean isLastUserPage) {
		super(PAGE_NAME, isLastUserPage);
		fList= createEditorList();
	}

	/**
	 * Saves all open editors checked in the table viewer. Returns <code>true</code>
	 * if saving was successful. Otherwise <code>false</code> is returned.
	 */	
	boolean saveEditors(){
		List editorsToSave= fList.getCheckedElements();			
		if (editorsToSave.isEmpty())
			return true;
				
		try {
			// Save isn't cancelable.
			JdtHackFinder.fixme("1GCQYJK: ITPUI:WIN2000 - Invalid thread access when saving an editor");
			getWizard().getContainer().run(false, false, createRunnable(editorsToSave));
		} catch (InvocationTargetException e) {
			JdtHackFinder.fixme("1GCZLPL: ITPJUI:WINNT - ExceptionHandler::handle - should accept null as message");
			String msg= e.getMessage();
			if (msg == null) msg= "";
			ExceptionHandler.handle(e, getShell(), "InvocationTargerException", msg);
			setPageComplete(false);
			return false;
		} catch (InterruptedException e) {
			// Can't happen. Operation isn't cancelable.
		}
		fillEditorList();
		return true;
	}
	
	/**
	 * Creates a runnable to be used inside an operation to save all editors.
	 */
	private IRunnableWithProgress createRunnable(final List editorsToSave) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				Iterator iter= editorsToSave.iterator();
				while (iter.hasNext())
					((IEditorPart) iter.next()).doSave(monitor);
			}
		};
	}	
	
	/**
	 * Creates an array of all dialog fields on this page. Override this method to 
	 * create a page with more dialog fields.
	 * @return the array of created dialog fields.
	 */
	protected DialogField[] createDialogFields() {
		return new DialogField[] { fList };
	}
	
	/**
	 * Returns the dialog field presenting the list of unsaved editors.
	 * @return the dialog field showing the list of unsaved editors.
	 */	
	protected CheckedListDialogField getEditorList(){
		return fList;
	}
	
	/* (non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public void setVisible(boolean visible) {
		if (visible){
			getRefactoringWizard().setChange(null);
			if (fFirstTime) {
				fillEditorList();
				fFirstTime= false;
			}
		}	
		super.setVisible(visible);
	}
	
	/* (non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		DialogField[] fields= createDialogFields();	
		LayoutUtil.doDefaultLayout(composite, fields, true);
		setControl(composite);
	}
	
	private CheckedListDialogField createEditorList(){
		CheckedListDialogField list= new CheckedListDialogField(null, null, createLabelProvider(), 0);
		list.setCheckAllButtonLabel(RefactoringResources.getResourceString(PREFIX + "checkAll"));
		list.setUncheckAllButtonLabel(RefactoringResources.getResourceString(PREFIX + "uncheckAll"));
		list.setLabelText(RefactoringResources.getResourceString(PREFIX + "resourcesToSave"));
		return list;
	}
	
	private Control createEditorListViewer(Composite parent){
		Composite comp= new Composite(parent, SWT.NULL);		
		LayoutUtil.doDefaultLayout(comp, new DialogField[] { fList }, true);
		return comp;
	}
	
	private ILabelProvider createLabelProvider() {
		return new LabelProvider() {
			public Image getImage(Object element) {
				return ((IEditorPart) element).getTitleImage();
			}
			public String getText(Object element) {
				return ((IEditorPart) element).getTitle();
			}
		};
	}
	
	private void fillEditorList(){
		List dirtyEditors= Arrays.asList(JavaPlugin.getDirtyEditors());
		fList.setEnabled(dirtyEditors.size() != 0);
		fList.setElements(dirtyEditors); 
		fList.checkAll(true);
	}	
}