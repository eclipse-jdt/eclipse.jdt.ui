/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class SourceContainerWorkbookPage extends BuildPathBasePage {

	private ListDialogField fClassPathList;
	private IJavaProject fCurrJProject;
	private IPath fProjPath;
	
	private Control fSWTControl;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	private SelectionButtonDialogField fProjectRadioButton;
	private SelectionButtonDialogField fFolderRadioButton;
	private ListDialogField fFoldersList;
	private CPListElement fProjectCPEntry;
	
	private StringDialogField fOutputLocationField;
	
	private boolean fIsProjSelected;

	public SourceContainerWorkbookPage(IWorkspaceRoot root, ListDialogField classPathList, StringDialogField outputLocationField, boolean isNewProject) {
		fWorkspaceRoot= root;
		fClassPathList= classPathList;
		fProjectCPEntry= null;
		fOutputLocationField= outputLocationField;
		
		fSWTControl= null;
				
		SourceContainerAdapter adapter= new SourceContainerAdapter();
				
		fProjectRadioButton= new SelectionButtonDialogField(SWT.RADIO);
		fProjectRadioButton.setDialogFieldListener(adapter);
		fProjectRadioButton.setLabelText(NewWizardMessages.getString("SourceContainerWorkbookPage.rb1.label")); //$NON-NLS-1$
						
		fFolderRadioButton= new SelectionButtonDialogField(SWT.RADIO);
		fFolderRadioButton.setDialogFieldListener(adapter);
		fFolderRadioButton.setLabelText(NewWizardMessages.getString("SourceContainerWorkbookPage.rb2.label")); //$NON-NLS-1$
		
		String[] buttonLabels;
		int removeIndex;
		if (isNewProject) {
			buttonLabels= new String[] { 
				/* 0 */ NewWizardMessages.getString("SourceContainerWorkbookPage.folders.addnew.button"), //$NON-NLS-1$
				/* 1 */ null,
				/* 2 */ NewWizardMessages.getString("SourceContainerWorkbookPage.folders.remove.button") //$NON-NLS-1$
			};
			removeIndex= 2;
		} else {
			buttonLabels= new String[] { 
				/* 0 */ NewWizardMessages.getString("SourceContainerWorkbookPage.folders.addnew.button"), //$NON-NLS-1$
				/* 1 */ NewWizardMessages.getString("SourceContainerWorkbookPage.folders.addnew.addexisting.button"), //$NON-NLS-1$
				/* 2 */ null,
				/* 3 */ NewWizardMessages.getString("SourceContainerWorkbookPage.folders.remove.button") //$NON-NLS-1$
			};
			removeIndex= 3;
		}
		
		fFoldersList= new ListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fFoldersList.setDialogFieldListener(adapter);
		fFoldersList.setLabelText(NewWizardMessages.getString("SourceContainerWorkbookPage.folders.label")); //$NON-NLS-1$
		fFoldersList.setRemoveButtonIndex(removeIndex);
		
		fFoldersList.setViewerSorter(new CPListElementSorter());
				
		fFolderRadioButton.attachDialogField(fFoldersList);	
	}
	
	public void init(IJavaProject jproject) {
		fCurrJProject= jproject;
		fProjPath= fCurrJProject.getProject().getFullPath();	
		updateFoldersList();
	}
	
	private void updateFoldersList() {	
		fIsProjSelected= false;
		List srcelements= new ArrayList(fClassPathList.getSize());
		
		List cpelements= fClassPathList.getElements();
		for (int i= 0; i < cpelements.size(); i++) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				if (fProjPath.equals(cpe.getPath())) {
					srcelements.clear();
					// remember the entry to ensure a unique CPListElement for the project-cpentry
					fProjectCPEntry= cpe;
					fIsProjSelected= true;
					break;
				} else {
					srcelements.add(cpe);
				}
			}
		}
		fFoldersList.setElements(srcelements);
		
		// fix for 1G47IYV: ITPJUI:WINNT - Both radio buttons get selected in Project properties
		fFolderRadioButton.setSelection(!fIsProjSelected);
		fProjectRadioButton.setSelection(fIsProjSelected);
	}			
	
	public Control getControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		
		MGridLayout layout= new MGridLayout();
		layout.minimumWidth= SWTUtil.convertWidthInCharsToPixels(80, composite);
		layout.numColumns= 2;		
		composite.setLayout(layout);
		
		fProjectRadioButton.doFillIntoGrid(composite, 2);
		fFolderRadioButton.doFillIntoGrid(composite, 2);
		
		Control control= fFoldersList.getListControl(composite);
		MGridData gd= new MGridData(MGridData.FILL_BOTH);
		gd.horizontalIndent= SWTUtil.convertWidthInCharsToPixels(2, composite);
		gd.widthHint= SWTUtil.convertWidthInCharsToPixels(50, composite);
		gd.heightHint= SWTUtil.convertWidthInCharsToPixels(15, composite);
		control.setLayoutData(gd);
		
		control= fFoldersList.getButtonBox(composite);
		gd= new MGridData(gd.VERTICAL_ALIGN_FILL + gd.HORIZONTAL_ALIGN_FILL);
		control.setLayoutData(gd);
		
		int buttonBarWidth= SWTUtil.convertWidthInCharsToPixels(24, composite);
		fFoldersList.setButtonsMinWidth(buttonBarWidth);
	
		fSWTControl= composite;
		
		return composite;
	}
	
	private Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();
	}
	
	
	private class SourceContainerAdapter implements IListAdapter, IDialogFieldListener {
	
		// -------- IListAdapter --------
		public void customButtonPressed(DialogField field, int index) {
			sourcePageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(DialogField field) {}
		
		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			sourcePageDialogFieldChanged(field);
		}
	}
	
	private void sourcePageCustomButtonPressed(DialogField field, int index) {
		if (field == fFoldersList) {
			List elementsToAdd= new ArrayList(10);
			switch (index) {
			case 0: /* add new */
				CPListElement srcentry= createNewSourceContainer();
				if (srcentry != null) {
					elementsToAdd.add(srcentry);
				}
				break;
			case 1: /* add existing */
				CPListElement[] srcentries= chooseSourceContainers();
				if (srcentries != null) {
					for (int i= 0; i < srcentries.length; i++) {
						elementsToAdd.add(srcentries[i]);
					}
				}
				break;
			}
			if (!elementsToAdd.isEmpty()) {
				fFoldersList.addElements(elementsToAdd);
				fFoldersList.postSetSelection(new StructuredSelection(elementsToAdd));
				// if no source folders up to now
				if (fFoldersList.getSize() == elementsToAdd.size()) {
					askForChangingBuildPathDialog();
				}
			}
		}
	}
	
	private void sourcePageDialogFieldChanged(DialogField field) {
		if (fCurrJProject == null) {
			// not initialized
			return;
		}
		
		if (field == fFolderRadioButton) {
			if (fFolderRadioButton.isSelected()) {
				fIsProjSelected= false;
				updateClasspathList();
				if (fFoldersList.getSize() > 0) {
					askForChangingBuildPathDialog();
				}
			}
		} else if (field == fProjectRadioButton) {
			if (fProjectRadioButton.isSelected()) {
				fIsProjSelected= true;
				updateClasspathList();
			}
		} else if (field == fFoldersList) {
			updateClasspathList();
		}
	}	
	
		
	private void updateClasspathList() {
		List cpelements= fClassPathList.getElements();
		
		List srcelements;
		if (fIsProjSelected) {
			srcelements= new ArrayList(1);
			if (fProjectCPEntry == null) {
				// never initialized before: create a new one
				fProjectCPEntry= newCPSourceElement(fCurrJProject.getProject());
			}
			srcelements.add(fProjectCPEntry);
		} else {
			srcelements= fFoldersList.getElements();
		}
		boolean changeDone= false;
		// backwards, as entries will be deleted
		for (int i= cpelements.size() - 1; i >= 0 ; i--) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				// if it is a source folder, but not one of the accepted entries, remove it
				// at the same time, for the entries seen, remove them from the accepted list
				if (!srcelements.remove(cpe)) {
					cpelements.remove(i);
					changeDone= true;
				}	
			}
		}
		for (int i= 0; i < srcelements.size(); i++) {
			cpelements.add(srcelements.get(i));
		}
		if (changeDone || (srcelements.size() > 0)) {
			fClassPathList.setElements(cpelements);
		}
	}
		
	private CPListElement createNewSourceContainer() {	
		IProject proj= fCurrJProject.getProject();
		String title= NewWizardMessages.getString("SourceContainerWorkbookPage.NewSourceFolderDialog.title"); //$NON-NLS-1$
		NewContainerDialog dialog= new NewContainerDialog(getShell(), title, proj, getExistingContainers());
		dialog.setMessage(NewWizardMessages.getFormattedString("SourceContainerWorkbookPage.NewSourceFolderDialog.description", fProjPath.toString())); //$NON-NLS-1$
		if (dialog.open() == dialog.OK) {
			IFolder folder= dialog.getFolder();
			return newCPSourceElement(folder);
		}
		return null;
	}
	
	/**
	 * Asks to change the output folder to 'proj/bin' when no source folders were existing
	 */ 
	private void askForChangingBuildPathDialog() {
		IPath outputFolder= new Path(fOutputLocationField.getText());
		if (outputFolder.segmentCount() == 1) {
			IPath newPath= outputFolder.append("bin");
			String title= NewWizardMessages.getString("SourceContainerWorkbookPage.ChangeOutputLocationDialog.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getFormattedString("SourceContainerWorkbookPage.ChangeOutputLocationDialog.message", newPath); //$NON-NLS-1$
			if (MessageDialog.openQuestion(getShell(), title, message)) {
				fOutputLocationField.setText(newPath.toString());
			}
		}
	}
	
			
			
	private CPListElement[] chooseSourceContainers() {	
		Class[] acceptedClasses= new Class[] { IFolder.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
			
		acceptedClasses= new Class[] { IFolder.class };
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, getExistingContainers());	
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(NewWizardMessages.getString("SourceContainerWorkbookPage.ExistingSourceFolderDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("SourceContainerWorkbookPage.ExistingSourceFolderDialog.description")); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(fCurrJProject.getProject());
		if (dialog.open() == dialog.OK) {
			Object[] elements= dialog.getResult();
			CPListElement[] res= new CPListElement[elements.length];
			for (int i= 0; i < res.length; i++) {
				IResource elem= (IResource)elements[i];
				res[i]= newCPSourceElement(elem);
			}
			return res;
		}
		return null;		
	}
	
	private IContainer[] getExistingContainers() {
		List res= new ArrayList();
		List cplist= fFoldersList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			IResource resource= elem.getResource();
			if (resource instanceof IContainer) { // defensive code
				res.add(resource);	
			}
		}
		return (IContainer[]) res.toArray(new IContainer[res.size()]);
	}
	
	private CPListElement newCPSourceElement(IResource res) {
		Assert.isNotNull(res);
		return new CPListElement(IClasspathEntry.CPE_SOURCE, res.getFullPath(), res);
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		if (fIsProjSelected) {
			ArrayList list= new ArrayList(1);
			list.add(fProjectCPEntry);
			return list;
		} else {
			return fFoldersList.getSelectedElements();
		}
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		if (!fIsProjSelected) {
			filterSelection(selElements, IClasspathEntry.CPE_SOURCE);
			fFoldersList.selectElements(new StructuredSelection(selElements));
		}
	}	
	
		


}