/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.net.URL;import java.util.ArrayList;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.FileDialog;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IFolder;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ITreeContentProvider;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.ViewerFilter;import org.eclipse.ui.model.WorkbenchContentProvider;import org.eclipse.ui.model.WorkbenchLabelProvider;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.IUIConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;import org.eclipse.jdt.internal.ui.dialogs.TypedViewerFilter;import org.eclipse.jdt.internal.ui.viewsupport.ResourceFilter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;import org.eclipse.jdt.ui.JavaUI;

public class LibrariesWorkbookPage extends BuildPathBasePage {
	
	private static final String PAGE_NAME= "LibrariesWorkbookPage";
	
	private static final String LIBRARIES= PAGE_NAME + ".libraries";
	
	private static final String ADDNEW=      LIBRARIES + ".addnew.button";
	private static final String ADDEXISTING= LIBRARIES + ".addexisting.button";
	private static final String ADDJAR=      LIBRARIES + ".addjar.button";
	private static final String ADDEXTERNAL= LIBRARIES + ".addextjar.button";
	private static final String ADDVARIABLE= LIBRARIES + ".addvariable.button";
	private static final String SETSOURCE=   LIBRARIES + ".setsource.button";

	private static final String DIALOG_EXI_CLASSFOLDER= PAGE_NAME + ".ExistingClassFolderDialog";
	private static final String DIALOG_NEW_CLASSFOLDER= PAGE_NAME + ".NewClassFolderDialog";
	private static final String DIALOG_JAR_ARCHIVE= 	PAGE_NAME + ".JARArchiveDialog";
	private static final String DIALOG_SOURCE_ANNOT= 	PAGE_NAME + ".SourceAttachmentDialog";
	private static final String DIALOG_VARIABLE_SELECTION= 	PAGE_NAME + ".VariableSelectionDialog";
	
	private static final String DIALOGSTORE_LASTVARIABLE= JavaUI.ID_PLUGIN + ".lastvariable";	
	
	private ListDialogField fClassPathList;
	private IJavaProject fCurrJProject;
	
	private ListDialogField fLibrariesList;
	private IWorkspaceRoot fWorkspaceRoot;
	
	private IDialogSettings fDialogSettings;
	
	private Shell fShell;
		
	public LibrariesWorkbookPage(IWorkspaceRoot root, ListDialogField classPathList) {
		fClassPathList= classPathList;
		fWorkspaceRoot= root;
		fShell= null;
		
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		String[] buttonLabels= new String[] { 
			JavaPlugin.getResourceString(ADDNEW), JavaPlugin.getResourceString(ADDEXISTING),
			JavaPlugin.getResourceString(ADDJAR), JavaPlugin.getResourceString(ADDEXTERNAL),
			JavaPlugin.getResourceString(ADDVARIABLE), null, 
			JavaPlugin.getResourceString(SETSOURCE)
		};		
				
		LibrariesAdapter adapter= new LibrariesAdapter();
				
		fLibrariesList= new ListDialogField(adapter, buttonLabels, new CPListLabelProvider(), 0);
		fLibrariesList.setDialogFieldListener(adapter);
		fLibrariesList.setLabelText(JavaPlugin.getResourceString(LIBRARIES + ".label"));
		fLibrariesList.setRemoveButtonLabel(JavaPlugin.getResourceString(LIBRARIES + ".remove.button"));
	
		fLibrariesList.enableCustomButton(6, false);

	}
		
	public void init(IJavaProject jproject) {
		fCurrJProject= jproject;
		updateLibrariesList();
	}
	
	private void updateLibrariesList() {
		List cpelements= fClassPathList.getElements();
		List libelements= new ArrayList(cpelements.size());
		
		int nElements= cpelements.size();
		for (int i= 0; i < nElements; i++) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (kind == IClasspathEntry.CPE_LIBRARY || kind == IClasspathEntry.CPE_VARIABLE) {
				libelements.add(cpe);
			}
		}
		fLibrariesList.setElements(libelements);
	}		
		
	// -------- ui creation
	
	public Control getControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
			
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fLibrariesList }, true);
			
		MGridLayout layout= (MGridLayout)composite.getLayout();
		layout.marginWidth= 5;
		layout.marginHeight= 5;
		
		fLibrariesList.setButtonsMinWidth(110);
		
		fShell= parent.getShell();
				
		return composite;
	}
	
	private Shell getShell() {
		return fShell;
	}
	
	
	private class LibrariesAdapter implements IDialogFieldListener, IListAdapter {
		
		// -------- IListAdapter --------
			
		public void customButtonPressed(DialogField field, int index) {
			CPListElement[] libentries= null;
			switch (index) {
			case 0: /* add new */
				libentries= new CPListElement[] { createNewClassContainer() };
				break;
			case 1: /* add existing */
				libentries= chooseClassContainers();
				break;
			case 2: /* add jar */
				libentries= chooseJarFiles();
				break;
			case 3: /* add external jar */
				libentries= chooseExtJarFiles();
				break;
			case 4: /* add variable */
				libentries= chooseVariableEntries();
				break;				
			case 6: /* set source attachment */
				List selElements= fLibrariesList.getSelectedElements();
				CPListElement selElement= (CPListElement) selElements.get(0);				
				SourceAttachmentDialog dialog= new SourceAttachmentDialog(getShell(), selElement.getClasspathEntry());
				if (dialog.open() == dialog.OK) {
					selElement.setSourceAttachment(dialog.getSourceAttachmentPath(), dialog.getSourceAttachmentRootPath());
					fLibrariesList.refresh();
					fClassPathList.refresh();
				}
				break;
			}
			if (libentries != null) {
				int nElementsChosen= libentries.length;					
				// remove duplicates
				List cplist= fLibrariesList.getElements();
				List elementsToAdd= new ArrayList(nElementsChosen);
				
				for (int i= 0; i < nElementsChosen; i++) {
					CPListElement curr= libentries[i];
					if (!cplist.contains(curr) && !elementsToAdd.contains(curr)) {
						elementsToAdd.add(curr);
					}
				}
				fLibrariesList.addElements(elementsToAdd);
				fLibrariesList.postSetSelection(new StructuredSelection(libentries));
			}
		}
		
		public void selectionChanged(DialogField field) {
			List selElements= fLibrariesList.getSelectedElements();
			fLibrariesList.enableCustomButton(6, canDoSourceAttachment(selElements));
		}
		
		private boolean canDoSourceAttachment(List selElements) {
			if (selElements != null && selElements.size() == 1) {
				CPListElement elem= (CPListElement) selElements.get(0);
				return (!(elem.getResource() instanceof IFolder) 
					&& elem.getEntryKind() != IClasspathEntry.CPE_VARIABLE);
			}
			return false;
		}
			
			
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			if (fCurrJProject != null) {
				// already initialized
				updateClasspathList();
			}
		}
	}
	
	private void updateClasspathList() {
		List projelements= fLibrariesList.getElements();
		
		boolean remove= false;
		List cpelements= fClassPathList.getElements();
		// backwards, as entries will be deleted
		for (int i= cpelements.size() - 1; i >= 0; i--) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (kind == IClasspathEntry.CPE_LIBRARY || kind == IClasspathEntry.CPE_VARIABLE) {
				if (!projelements.remove(cpe)) {
					cpelements.remove(i);
					remove= true;
				}	
			}
		}
		for (int i= 0; i < projelements.size(); i++) {
			cpelements.add(projelements.get(i));
		}
		if (remove || (projelements.size() > 0)) {
			fClassPathList.setElements(cpelements);
		}
	}
	
		
	private CPListElement createNewClassContainer() {
		String title= JavaPlugin.getResourceString(DIALOG_NEW_CLASSFOLDER + ".title");
		IProject currProject= fCurrJProject.getProject();
		
		NewContainerDialog dialog= new NewContainerDialog(getShell(), title, currProject, getFilteredExistingContainerEntries());
		IPath projpath= currProject.getFullPath();
		dialog.setMessage(JavaPlugin.getFormattedString(DIALOG_NEW_CLASSFOLDER + ".description", projpath.toString()));
		int ret= dialog.open();
		if (ret == dialog.OK) {
			IFolder folder= dialog.getFolder();
			return newCPLibraryElement(folder);
		}
		return null;
	}
			
			
	private CPListElement[] chooseClassContainers() {	
		Class[] acceptedClasses= new Class[] { IFolder.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
			
		acceptedClasses= new Class[] { IProject.class, IFolder.class };
		
		List rejectedFolders= getFilteredExistingContainerEntries();
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, rejectedFolders);	
			
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(JavaPlugin.getResourceString(DIALOG_EXI_CLASSFOLDER + ".title"));
		dialog.setMessage(JavaPlugin.getResourceString(DIALOG_EXI_CLASSFOLDER + ".description"));
		dialog.addFilter(filter);
		
		IProject proj= fCurrJProject.getProject();
		if (dialog.open(fWorkspaceRoot, proj) == dialog.OK) {
			Object[] elements= dialog.getResult();
			CPListElement[] res= new CPListElement[elements.length];
			for (int i= 0; i < res.length; i++) {
				IResource elem= (IResource) elements[i];
				res[i]= newCPLibraryElement(elem);
			}
			return res;
		}
		return null;		
	}
	
	private CPListElement[] chooseJarFiles() {
		Class[] acceptedClasses= new Class[] { IFile.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
		List rejectedJARs= getFilteredExistingJAREntries();
		
		ViewerFilter filter= new ResourceFilter(new String[] { ".jar", ".zip" }, rejectedJARs);
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(JavaPlugin.getResourceString(DIALOG_JAR_ARCHIVE + ".title"));
		dialog.setMessage(JavaPlugin.getResourceString(DIALOG_JAR_ARCHIVE + ".description"));
		dialog.addFilter(filter);
		
		IProject proj= fCurrJProject.getProject();
		if (dialog.open(fWorkspaceRoot, proj) == dialog.OK) {
			Object[] elements= dialog.getResult();
			CPListElement[] res= new CPListElement[elements.length];
			for (int i= 0; i < res.length; i++) {
				IResource elem= (IResource)elements[i];
				res[i]= newCPLibraryElement(elem);
			}
			return res;
		}
		return null;
	}
	
	
	
	private List getFilteredExistingContainerEntries() {
		List res= new ArrayList();
		try {
			IPath outputLocation= fCurrJProject.getOutputLocation();
			if (outputLocation != null) {
				IResource resource= fWorkspaceRoot.findMember(outputLocation);
				if (resource != null) {
					res.add(resource);
				}
			}
		} catch (JavaModelException e) {
			// ignore it here
		}	
			
		List cplist= fLibrariesList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			IResource resource= elem.getResource();
			if (resource instanceof IFolder) {
				res.add(resource);
			}
		}
		return res;
	}
	
	private List getFilteredExistingJAREntries() {
		List res= new ArrayList();
		List cplist= fLibrariesList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			IResource resource= elem.getResource();
			if (resource instanceof IFile) {
				res.add(resource);
			}
		}
		return res;
	}	
	
	private CPListElement newCPLibraryElement(IResource res) {
		return new CPListElement(IClasspathEntry.CPE_LIBRARY, res.getFullPath(), res);
	};

	
	private CPListElement[] chooseExtJarFiles() {
		String lastUsedPath= fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
		if (lastUsedPath == null) {
			lastUsedPath= "";
		}
		FileDialog dialog= new FileDialog(getShell(), SWT.MULTI);
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"});
		dialog.setFilterPath(lastUsedPath);
		String res= dialog.open();
		if (res == null) {
			return null;
		}
		String[] fileNames= dialog.getFileNames();
		int nChosen= fileNames.length;
			
		IPath filterPath= new Path(dialog.getFilterPath());
		CPListElement[] elems= new CPListElement[nChosen];
		for (int i= 0; i < nChosen; i++) {
			IPath path= filterPath.append(fileNames[i]);
			IFile file= fWorkspaceRoot.getFileForLocation(path);
			if (file != null) {
				path= file.getFullPath();  // modify path if internal
			}
			elems[i]= new CPListElement(IClasspathEntry.CPE_LIBRARY, path, file);
		}
		fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, filterPath.toOSString());
		
		return elems;
	}
	
	private CPListElement[] chooseVariableEntries() {
		List existing= fLibrariesList.getElements();
		ArrayList existingPaths= new ArrayList();
		for (int i= 0; i < fLibrariesList.getSize(); i++) {
			CPListElement elem= (CPListElement) fLibrariesList.getElement(i);
			if (elem.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				existingPaths.add(elem.getPath());
			}
		}
		VariableSelectionDialog dialog= new VariableSelectionDialog(getShell(), existing);
		if (dialog.open() == dialog.OK) {
			IPath path= dialog.getVariable();
			return new CPListElement[] { new CPListElement(IClasspathEntry.CPE_VARIABLE, path, null) };
		}
		return null;
	}

	// a dialog to set the source attachment properties
	private class VariableSelectionDialog extends StatusDialog implements IStatusChangeListener {	
		private VariableSelectionBlock fVariableSelectionBlock;
				
		public VariableSelectionDialog(Shell parent, List existingPaths) {
			super(parent);
			setTitle(JavaPlugin.getResourceString(DIALOG_VARIABLE_SELECTION + ".title"));
			fVariableSelectionBlock= new VariableSelectionBlock(this, existingPaths, fDialogSettings.get(DIALOGSTORE_LASTVARIABLE));
		}

		/**
		 * @see StatusDialog#createDialogArea()
		 */				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);
					
			Label message= new Label(composite, SWT.WRAP);
			message.setText(JavaPlugin.getResourceString(DIALOG_VARIABLE_SELECTION + ".message"));
			message.setLayoutData(new GridData());	
						
			Control inner= fVariableSelectionBlock.createControl(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			return composite;
		}
		
		/**
		 * @see Dialog#okPressed()
		 */
		protected void okPressed() {
			fDialogSettings.put(DIALOGSTORE_LASTVARIABLE, getVariable().segment(0));
			super.okPressed();
		}	

		/**
		 * @see IStatusChangeListener#statusChanged()
		 */			
		public void statusChanged(IStatus status) {
			updateStatus(status);
		}
		
		public IPath getVariable() {
			return fVariableSelectionBlock.getVariable();
		}		
	}
				
	// a dialog to set the source attachment properties
	private class SourceAttachmentDialog extends StatusDialog implements IStatusChangeListener {
		
		private SourceAttachmentBlock fSourceAttachmentBlock;
				
		public SourceAttachmentDialog(Shell parent, IClasspathEntry entry) {
			super(parent);
			setTitle(JavaPlugin.getFormattedString(DIALOG_SOURCE_ANNOT + ".title", entry.getPath().toString()));
			
			IProject proj= fCurrJProject.getProject();
			fSourceAttachmentBlock= new SourceAttachmentBlock(proj, this, entry);
		}
				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);
					
			Label message= new Label(composite, SWT.WRAP);
			message.setText(JavaPlugin.getResourceString(DIALOG_SOURCE_ANNOT + ".message"));
			message.setLayoutData(new GridData());	
						
			Control inner= fSourceAttachmentBlock.createControl(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			return composite;
		}
		
		public void statusChanged(IStatus status) {
			updateStatus(status);
		}
		
		public IPath getSourceAttachmentPath() {
			return fSourceAttachmentBlock.getSourceAttachmentPath();
		}
		
		public IPath getSourceAttachmentRootPath() {
			return fSourceAttachmentBlock.getSourceAttachmentRootPath();
		}
		
	}
	
	/**
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fLibrariesList.getSelectedElements();
	}

	/**
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		for (int i= selElements.size()-1; i >= 0; i--) {
			CPListElement curr= (CPListElement) selElements.get(i);
			int kind= curr.getEntryKind();
			if (kind != IClasspathEntry.CPE_LIBRARY && kind != IClasspathEntry.CPE_VARIABLE) {
				selElements.remove(i);
			}
		}
		fLibrariesList.selectElements(new StructuredSelection(selElements));
	}	


}