/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.IStatusInfoChangeListener;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.viewsupport.ResourceFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class LibrariesWorkbookPage extends BuildPathBasePage {
	
	private static final String LIBRARIES= "LibrariesWorkbookPage.libraries";
	
	private static final String ADDNEW= "LibrariesWorkbookPage.libraries.addnew.button";
	private static final String ADDEXISTING= "LibrariesWorkbookPage.libraries.addexisting.button";
	private static final String ADDJAR= "LibrariesWorkbookPage.libraries.addjar.button";
	private static final String ADDEXTERNAL= "LibrariesWorkbookPage.libraries.addextjar.button";
	private static final String SETSOURCE= "LibrariesWorkbookPage.libraries.setsource.button";

	private static final String DIALOG_EXI_CLASSFOLDER= "LibrariesWorkbookPage.ExistingClassFolderDialog";
	private static final String DIALOG_NEW_CLASSFOLDER= "LibrariesWorkbookPage.NewClassFolderDialog";
	private static final String DIALOG_JAR_ARCHIVE= 	"LibrariesWorkbookPage.JARArchiveDialog";
	private static final String DIALOG_SOURCE_ANNOT= 	"LibrariesWorkbookPage.SourceAttachmentDialog";
	
	
	private static final String DIALOGSTORE_EXTJAR_PATH= 	"LibrariesWorkbookPage.ExtJARPath";	
	
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
			null, JavaPlugin.getResourceString(SETSOURCE)
		};		
				
		LibrariesAdapter adapter= new LibrariesAdapter();
				
		fLibrariesList= new ListDialogField(adapter, buttonLabels, new CPListLabelProvider(), 0);
		fLibrariesList.setDialogFieldListener(adapter);
		fLibrariesList.setLabelText(JavaPlugin.getResourceString(LIBRARIES + ".label"));
		fLibrariesList.setRemoveButtonLabel(JavaPlugin.getResourceString(LIBRARIES + ".remove.button"));
	
		fLibrariesList.enableCustomButton(5, false);

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
			if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
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
		fLibrariesList.enableCustomButton(4, false);
		
		fShell= parent.getShell();
				
		return composite;
	}
	
	private Shell getShell() {
		return fShell;
	}
	
	
	private class LibrariesAdapter implements IDialogFieldListener, IListAdapter {
		
		// -------- IListAdapter --------
			
		public void customButtonPressed(DialogField field, int index) {
			List elementsToAdd= new ArrayList(10);
			CPListElement[] libentries;
			switch (index) {
			case 0: /* add new */
				CPListElement libentry= createNewClassContainer();
				if (libentry != null) {
					elementsToAdd.add(libentry);
				}
				break;
			case 1: /* add existing */
				libentries= chooseClassContainers();
				if (libentries != null) {
					for (int i= 0; i < libentries.length; i++) {
						elementsToAdd.add(libentries[i]);
					}
				}
				break;
			case 2: /* add jar */
				libentries= chooseJarFiles();
				if (libentries != null) {
					for (int i= 0; i < libentries.length; i++) {
						elementsToAdd.add(libentries[i]);
					}
				}
				break;
			case 3: /* add external jar */
				IPath projpath= fCurrJProject.getProject().getFullPath();
				Object lastUsedPath= fDialogSettings.get(DIALOGSTORE_EXTJAR_PATH);
				if (lastUsedPath == null) {
					lastUsedPath= projpath;
				}			
				IPath[] paths= chooseExtJarFile(lastUsedPath.toString());
				if (paths != null) {
					for (int i= 0; i < paths.length; i++) {
						IPath path= paths[i];
						CPListElement element= findPathInList(path);
						if (element == null) {
							// does not exist yet
							IFile file= fWorkspaceRoot.getFileForLocation(path);
							if (file != null) {
								path= file.getFullPath();  // modify path if internal
							}
							elementsToAdd.add(new CPListElement(fCurrJProject.newLibraryEntry(path), file));
						}
					}
					if (paths.length > 0) {
						fDialogSettings.put(DIALOGSTORE_EXTJAR_PATH, paths[0].removeLastSegments(1).toOSString());
					}
				}
				break;
			case 5: /* set source attachment */
				List selElements= fLibrariesList.getSelectedElements();
				CPListElement selElement= (CPListElement) selElements.get(0);
				IPath attachPath= selElement.getSourceAttachmentPath();
				IPath attachRoot= selElement.getSourceAttachmentRootPath();
				URL jdocLocation= selElement.getJavaDocLocation();
				
				SourceAttachmentDialog dialog= new SourceAttachmentDialog(getShell(), selElement.getPath(), attachPath, attachRoot, jdocLocation);
				if (dialog.open() == dialog.OK) {
					selElement.setSourceAttachment(dialog.getSourceAttachmentPath(), dialog.getSourceAttachmentRootPath());
					selElement.setJavaDocLocation(dialog.getJavaDocLocation());
					fLibrariesList.refresh();
					fClassPathList.refresh();
				}
				break;
			}						
			if (!elementsToAdd.isEmpty()) {
				fLibrariesList.addElements(elementsToAdd);
				fLibrariesList.postSetSelection(new StructuredSelection(elementsToAdd));
			}
		}
		
		private CPListElement findPathInList(IPath path) {
			List cplist= fLibrariesList.getElements();
			for (int i= cplist.size() -1; i >= 0; i--) {
				CPListElement curr= (CPListElement)cplist.get(i);
				if (curr.getPath().equals(path)) {
					return curr;
				}
			}
			return null;
		}
		
		public void selectionChanged(DialogField field) {
			List selElements= fLibrariesList.getSelectedElements();
			fLibrariesList.enableCustomButton(5, canDoSourceAttachment(selElements));
		}
		
		private boolean canDoSourceAttachment(List selElements) {
			if (selElements != null && selElements.size() == 1) {
				CPListElement elem= (CPListElement) selElements.get(0);
				return (!(elem.getResource() instanceof IFolder));
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
			if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
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
		return new CPListElement(fCurrJProject.newLibraryEntry(res.getFullPath()), res);
	};

	private IPath[] chooseExtJarFile(String initPath) {
		FileDialog dialog= new FileDialog(getShell(), SWT.MULTI);
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"});
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			String[] fileNames= dialog.getFileNames();
			String filterPath= dialog.getFilterPath();
			IPath[] paths= new IPath[fileNames.length];
			for (int i= 0; i < paths.length; i++) {
				paths[i]= new Path(filterPath).append(fileNames[i]);
			}
			return paths;
		}
		return null;
	}
	
	// a dialog to set the source attachment properties
	private class SourceAttachmentDialog extends StatusDialog implements IStatusInfoChangeListener {
		
		private SourceAttachmentBlock fSourceAttachmentBlock;
				
		public SourceAttachmentDialog(Shell parent, IPath jarPath, IPath sourceFile, IPath prefix, URL jdocLocation) {
			super(parent);
			setTitle(JavaPlugin.getFormattedString(DIALOG_SOURCE_ANNOT + ".title", jarPath.toString()));
					
			IProject proj= fCurrJProject.getProject();
			fSourceAttachmentBlock= new SourceAttachmentBlock(proj, this, jarPath, sourceFile, prefix, jdocLocation);
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
		
		public void statusInfoChanged(StatusInfo status) {
			updateStatus(status);
		}
		
		public IPath getSourceAttachmentPath() {
			return fSourceAttachmentBlock.getSourceAttachmentPath();
		}
		
		public IPath getSourceAttachmentRootPath() {
			return fSourceAttachmentBlock.getSourceAttachmentRootPath();
		}
		
		public URL getJavaDocLocation() {
			return fSourceAttachmentBlock.getJavaDocLocation();
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
		filterSelection(selElements, IClasspathEntry.CPE_LIBRARY);
		fLibrariesList.selectElements(new StructuredSelection(selElements));
	}	


}