/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.text.MessageFormat;import java.util.ArrayList;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.resources.IFolder;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.IPath;import org.eclipse.jface.util.Assert;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ITreeContentProvider;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.ViewerFilter;import org.eclipse.ui.model.WorkbenchContentProvider;import org.eclipse.ui.model.WorkbenchLabelProvider;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;import org.eclipse.jdt.internal.ui.dialogs.TypedViewerFilter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;import org.eclipse.jdt.internal.ui.wizards.swt.MGridUtil;

public class SourceContainerWorkbookPage extends BuildPathBasePage {

	private static final String FOLDERS= "SourceContainerWorkbookPage.folders";
	private static final String ADDNEW= "SourceContainerWorkbookPage.folders.addnew.button";
	private static final String ADDEXISTING= "SourceContainerWorkbookPage.folders.addnew.addexisting.button";
	
	private static final String RBUTTON1= "SourceContainerWorkbookPage.rb1";
	private static final String RBUTTON2= "SourceContainerWorkbookPage.rb2";
	
	private static final String DIALOG_EXI_SRCFOLDER= "SourceContainerWorkbookPage.ExistingSourceFolderDialog";
	private static final String DIALOG_NEW_SRCFOLDER= "SourceContainerWorkbookPage.NewSourceFolderDialog";	
	
	private ListDialogField fClassPathList;
	private IJavaProject fCurrJProject;
	private IPath fProjPath;
	
	private Shell fShell;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	private SelectionButtonDialogField fProjectRadioButton;
	private SelectionButtonDialogField fFolderRadioButton;
	private ListDialogField fFoldersList;
	private CPListElement fProjectCPEntry;
	
	private boolean fIsProjSelected;

	public SourceContainerWorkbookPage(IWorkspaceRoot root, ListDialogField classPathList, boolean isNewProject) {
		fWorkspaceRoot= root;
		fClassPathList= classPathList;
		fProjectCPEntry= null;
		
		fShell= null;
				
		SourceContainerAdapter adapter= new SourceContainerAdapter();
				
		fProjectRadioButton= new SelectionButtonDialogField(SWT.RADIO);
		fProjectRadioButton.setDialogFieldListener(adapter);
		fProjectRadioButton.setLabelText(getResourceString(RBUTTON1 + ".label"));
						
		fFolderRadioButton= new SelectionButtonDialogField(SWT.RADIO);
		fFolderRadioButton.setDialogFieldListener(adapter);
		fFolderRadioButton.setLabelText(getResourceString(RBUTTON2 + ".label"));
		
		String[] buttonLabels;
		if (isNewProject) {
			buttonLabels= new String[] { getResourceString(ADDNEW) };
		} else {
			buttonLabels= new String[] { getResourceString(ADDNEW), getResourceString(ADDEXISTING) };
		}
		
		fFoldersList= new ListDialogField(adapter, buttonLabels, new CPListLabelProvider(), 0);
		fFoldersList.setDialogFieldListener(adapter);
		fFoldersList.setLabelText(getResourceString(FOLDERS + ".label"));
		fFoldersList.setRemoveButtonLabel(getResourceString(FOLDERS + ".remove.button"));
				
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
		
	// -------- Resource Bundle ---------
	
	private final String getResourceString(String key) {
		return JavaPlugin.getResourceString(key);
	}
	
	private final String getFormattedString(String key, String arg) {
		String str= getResourceString(key);
		return MessageFormat.format(str, new String[] { arg });
	}	
	
	
	public Control getControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		
		MGridLayout layout= new MGridLayout();
		layout.minimumWidth= 350;
		layout.minimumHeight= 0;
		layout.numColumns= 2;		
		composite.setLayout(layout);
		
		fProjectRadioButton.doFillIntoGrid(composite, 2);
		fFolderRadioButton.doFillIntoGrid(composite, 2);
		Control control= fFoldersList.getListControl(composite);
		MGridData gd= MGridUtil.createFill();
		gd.horizontalIndent= 10;
		control.setLayoutData(gd);
		
		control= fFoldersList.getButtonBox(composite);
		gd= new MGridData(gd.VERTICAL_ALIGN_FILL + gd.HORIZONTAL_ALIGN_FILL);
		control.setLayoutData(gd);
		
		fFoldersList.setButtonsMinWidth(110);
		fFoldersList.getTableViewer().setSorter(new CPListElementSorter());
		
		fShell= parent.getShell();
		
		return composite;
	}
	
	private class SourceContainerAdapter implements IListAdapter, IDialogFieldListener {
	
		// -------- IListAdapter --------
			
		public void customButtonPressed(DialogField field, int index) {
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
				}
			}
		}
		
		public void selectionChanged(DialogField field) {}
		
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			if (fCurrJProject == null) {
				// not initialized
				return;
			}
			
			if (field == fFolderRadioButton) {
				if (fFolderRadioButton.isSelected()) {
					fIsProjSelected= false;
					updateClasspathList();
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
		String title= getResourceString(DIALOG_NEW_SRCFOLDER + ".title");
		NewContainerDialog dialog= new NewContainerDialog(fShell, title, proj, getFilteredExistingContainerEntries());
		dialog.setMessage(getFormattedString(DIALOG_NEW_SRCFOLDER + ".description", fProjPath.toString()));
		if (dialog.open() == dialog.OK) {
			IFolder folder= dialog.getFolder();
			return newCPSourceElement(folder);
		}
		return null;
	}
			
			
	private CPListElement[] chooseSourceContainers() {	
		Class[] acceptedClasses= new Class[] { IFolder.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
			
		acceptedClasses= new Class[] { IFolder.class };
		List rejectedElements= getFilteredExistingContainerEntries();
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, rejectedElements);	
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(fShell, lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(getResourceString(DIALOG_EXI_SRCFOLDER + ".title"));
		dialog.setMessage(getResourceString(DIALOG_EXI_SRCFOLDER + ".description"));
		dialog.addFilter(filter);
		
		IProject proj= fCurrJProject.getProject();
		if (dialog.open(proj, null) == dialog.OK) {
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
	
	private List getFilteredExistingContainerEntries() {
		List res= new ArrayList();
		List cplist= fFoldersList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			res.add(elem.getResource());		
		}
		return res;
	}
	
	private CPListElement newCPSourceElement(IResource res) {
		Assert.isNotNull(res);
		return new CPListElement(IClasspathEntry.CPE_SOURCE, res.getFullPath(), res);
	}
	
	/**
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

	/**
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		if (!fIsProjSelected) {
			filterSelection(selElements, IClasspathEntry.CPE_SOURCE);
			fFoldersList.selectElements(new StructuredSelection(selElements));
		}
	}	
	
		


}