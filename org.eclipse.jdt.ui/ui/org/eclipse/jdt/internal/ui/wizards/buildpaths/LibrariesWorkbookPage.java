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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

public class LibrariesWorkbookPage extends BuildPathBasePage {
	
	private ListDialogField fClassPathList;
	private IJavaProject fCurrJProject;
	
	private TreeListDialogField fLibrariesList;
	private IWorkspaceRoot fWorkspaceRoot;
	
	private IDialogSettings fDialogSettings;
	
	private Control fSWTControl;

	private final int IDX_ADDJAR= 0;
	private final int IDX_ADDEXT= 1;
	private final int IDX_ADDVAR= 2;
	private final int IDX_ADDLIB= 3;
	private final int IDX_ADDFOL= 4;
	
	private final int IDX_EDIT= 6;
	private final int IDX_REMOVE= 7;
	
		
	public LibrariesWorkbookPage(IWorkspaceRoot root, ListDialogField classPathList) {
		fClassPathList= classPathList;
		fWorkspaceRoot= root;
		fSWTControl= null;
		
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		String[] buttonLabels= new String[] { 
			/* IDX_ADDJAR*/ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addjar.button"),	//$NON-NLS-1$
			/* IDX_ADDEXT */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addextjar.button"), //$NON-NLS-1$
			/* IDX_ADDVAR */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addvariable.button"), //$NON-NLS-1$
			/* IDX_ADDLIB */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addlibrary.button"), //$NON-NLS-1$
			/* IDX_ADDFOL */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addclassfolder.button"), //$NON-NLS-1$
			/* */ null,  
			/* IDX_EDIT */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.edit.button"), //$NON-NLS-1$
			/* IDX_REMOVE */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.remove.button") //$NON-NLS-1$
		};		
				
		LibrariesAdapter adapter= new LibrariesAdapter();
				
		fLibrariesList= new TreeListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fLibrariesList.setDialogFieldListener(adapter);
		fLibrariesList.setLabelText(NewWizardMessages.getString("LibrariesWorkbookPage.libraries.label")); //$NON-NLS-1$

		fLibrariesList.enableButton(IDX_REMOVE, false);
		fLibrariesList.enableButton(IDX_EDIT, false);
		
		fLibrariesList.setViewerSorter(new CPListElementSorter());

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
			if (isEntryKind(cpe.getEntryKind())) {
				libelements.add(cpe);
			}
		}
		fLibrariesList.setElements(libelements);
	}		
		
	// -------- ui creation
	
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
			
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fLibrariesList }, true);
		LayoutUtil.setHorizontalGrabbing(fLibrariesList.getTreeControl(null));
		
		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fLibrariesList.setButtonsMinWidth(buttonBarWidth);
		
		fLibrariesList.getTreeViewer().setSorter(new CPListElementSorter());
		
		fSWTControl= composite;
				
		return composite;
	}
	
	private Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();
	}
	
	
	private class LibrariesAdapter implements IDialogFieldListener, ITreeListAdapter {
		
		private final Object[] EMPTY_ARR= new Object[0];
		
		// -------- IListAdapter --------
		public void customButtonPressed(TreeListDialogField field, int index) {
			libaryPageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(TreeListDialogField field) {
			libaryPageSelectionChanged(field);
		}
		
		public void doubleClicked(TreeListDialogField field) {
			libaryPageDoubleClicked(field);
		}
		
		public void keyPressed(TreeListDialogField field, KeyEvent event) {
			libaryPageKeyPressed(field, event);
		}

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (element instanceof CPListElement) {
				return ((CPListElement) element).getChildren(false);
			}
			return EMPTY_ARR;
		}

		public Object getParent(TreeListDialogField field, Object element) {
			if (element instanceof CPListElementAttribute) {
				return ((CPListElementAttribute) element).getParent();
			}
			return null;
		}

		public boolean hasChildren(TreeListDialogField field, Object element) {
			return getChildren(field, element).length > 0;
		}		
			
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			libaryPageDialogFieldChanged(field);
		}
	}
	
	private void libaryPageCustomButtonPressed(DialogField field, int index) {
		CPListElement[] libentries= null;
		switch (index) {
		case IDX_ADDJAR: /* add jar */
			libentries= openJarFileDialog(null);
			break;
		case IDX_ADDEXT: /* add external jar */
			libentries= openExtJarFileDialog(null);
			break;
		case IDX_ADDVAR: /* add variable */
			libentries= openVariableSelectionDialog(null);
			break;
		case IDX_ADDLIB: /* addvanced */
			libentries= openContainerSelectionDialog(null);
			break;
		case IDX_ADDFOL: /* addvanced */
			libentries= openClassFolderDialog(null);
			break;			
		case IDX_EDIT: /* edit */
			editEntry();
			return;
		case IDX_REMOVE: /* remove */
			removeEntry();
			return;			
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
					curr.setAttribute(CPListElement.SOURCEATTACHMENT, BuildPathSupport.guessSourceAttachment(curr));
					curr.setAttribute(CPListElement.JAVADOC, JavaUI.getLibraryJavadocLocation(curr.getPath()));
				}
			}
			if (!elementsToAdd.isEmpty()) {
				askForAddingExclusionPatternsDialog(elementsToAdd);
			}
			
			fLibrariesList.addElements(elementsToAdd);
			if (index == IDX_ADDLIB) {
				fLibrariesList.refresh();
			}
			fLibrariesList.postSetSelection(new StructuredSelection(libentries));
		}
	}
	
	private void askForAddingExclusionPatternsDialog(List newEntries) {
		HashSet modified= new HashSet();
		fixNestingConflicts(newEntries, fClassPathList.getElements(), modified);
		if (!modified.isEmpty()) {
			String title= NewWizardMessages.getString("LibrariesWorkbookPage.exclusion_added.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("LibrariesWorkbookPage.exclusion_added.message"); //$NON-NLS-1$
			MessageDialog.openInformation(getShell(), title, message);
		}
	}
	
	protected void libaryPageDoubleClicked(TreeListDialogField field) {
		List selection= fLibrariesList.getSelectedElements();
		if (canEdit(selection)) {
			editEntry();
		}
	}

	protected void libaryPageKeyPressed(TreeListDialogField field, KeyEvent event) {
		if (field == fLibrariesList) {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				List selection= field.getSelectedElements();
				if (canRemove(selection)) {
					removeEntry();
				}
			}
		}	
	}	

	private void removeEntry() {
		List selElements= fLibrariesList.getSelectedElements();
		for (int i= selElements.size() - 1; i >= 0 ; i--) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				attrib.getParent().setAttribute(attrib.getKey(), null);
				selElements.remove(i);				
			}
		}
		if (selElements.isEmpty()) {
			fLibrariesList.refresh();
			fClassPathList.dialogFieldChanged(); // validate
		} else {
			fLibrariesList.removeElements(selElements);
		}
	}
	
	private boolean canRemove(List selElements) {
		if (selElements.size() == 0) {
			return false;
		}
		for (int i= 0; i < selElements.size(); i++) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				if (((CPListElementAttribute)elem).getValue() == null) {
					return false;
				}
			} else if (elem instanceof CPListElement) {
				CPListElement curr= (CPListElement) elem;
				if (curr.getParentContainer() != null) {
					return false;
				}
			}
		}
		return true;
	}	

	/**
	 * Method editEntry.
	 */
	private void editEntry() {
		List selElements= fLibrariesList.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		Object elem= selElements.get(0);
		if (fLibrariesList.getIndexOfElement(elem) != -1) {
			editElementEntry((CPListElement) elem);
		} else if (elem instanceof CPListElementAttribute) {
			editAttributeEntry((CPListElementAttribute) elem);
		}
	}
	
	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		if (key.equals(CPListElement.SOURCEATTACHMENT)) {
			CPListElement selElement= elem.getParent();
			
			IPath containerPath= null;
			boolean applyChanges= false;
			Object parentContainer= selElement.getParentContainer();
			if (parentContainer instanceof CPListElement) {
				containerPath= ((CPListElement) parentContainer).getPath();
				applyChanges= true;
			}
			Shell shell= getShell();
			SourceAttachmentDialog dialog= new SourceAttachmentDialog(shell, selElement.getClasspathEntry());
			if (dialog.open() == Window.OK) {
				IClasspathEntry result= dialog.getResult();
				
				if (applyChanges) {
					try {
						IRunnableWithProgress runnable= SourceAttachmentBlock.getRunnable(shell, result, fCurrJProject, containerPath);
						new ProgressMonitorDialog(shell).run(true, true, runnable);

					} catch (InvocationTargetException e) {
						String title= NewWizardMessages.getString("LibrariesWorkbookPage.configurecontainer.error.title"); //$NON-NLS-1$
						String message= NewWizardMessages.getString("LibrariesWorkbookPage.configurecontainer.error.message"); //$NON-NLS-1$
						ExceptionHandler.handle(e, shell, title, message);

					} catch (InterruptedException e) {
						return;
					}
				}
				selElement.setAttribute(CPListElement.SOURCEATTACHMENT, result.getSourceAttachmentPath());
				fLibrariesList.refresh();
				fClassPathList.refresh(); // images
			}
		} else if (key.equals(CPListElement.JAVADOC)) {
			CPListElement selElement= elem.getParent();
			JavadocPropertyDialog dialog= new JavadocPropertyDialog(getShell(), selElement);
			if (dialog.open() == Window.OK) {
				selElement.setAttribute(CPListElement.JAVADOC, dialog.getJavaDocLocation());
				fLibrariesList.refresh();
			}
		}
	}
		
	private void editElementEntry(CPListElement elem) {
		CPListElement[] res= null;
		
		switch (elem.getEntryKind()) {
		case IClasspathEntry.CPE_CONTAINER:
			res= openContainerSelectionDialog(elem);
			break;
		case IClasspathEntry.CPE_LIBRARY:
			IResource resource= elem.getResource();
			if (resource == null) {
				res= openExtJarFileDialog(elem);
			} else if (resource.getType() == IResource.FOLDER) {
				if (resource.exists()) {
					res= openClassFolderDialog(elem);
				} else {
					res= openNewClassFolderDialog(elem);
				} 
			} else if (resource.getType() == IResource.FILE) {
				res= openJarFileDialog(elem);			
			}
			break;
		case IClasspathEntry.CPE_VARIABLE:
			res= openVariableSelectionDialog(elem);
			break;
		}
		if (res != null && res.length > 0) {
			CPListElement curr= res[0];
			curr.setExported(elem.isExported());
			fLibrariesList.replaceElement(elem, curr);
		}		
			
	}

	private void libaryPageSelectionChanged(DialogField field) {
		List selElements= fLibrariesList.getSelectedElements();
		fLibrariesList.enableButton(IDX_EDIT, canEdit(selElements));
		fLibrariesList.enableButton(IDX_REMOVE, canRemove(selElements));
	}
	
	private boolean canEdit(List selElements) {
		if (selElements.size() != 1) {
			return false;
		}
		Object elem= selElements.get(0);
		if (elem instanceof CPListElement) {
			CPListElement curr= (CPListElement) elem;
			return !(curr.getResource() instanceof IFolder) && curr.getParentContainer() == null;
		}
		if (elem instanceof CPListElementAttribute) {
			return true;
		}
		return false;
	}
	
	private void libaryPageDialogFieldChanged(DialogField field) {
		if (fCurrJProject != null) {
			// already initialized
			updateClasspathList();
		}
	}	
		
	private void updateClasspathList() {
		List projelements= fLibrariesList.getElements();
		
		List cpelements= fClassPathList.getElements();
		int nEntries= cpelements.size();
		// backwards, as entries will be deleted
		int lastRemovePos= nEntries;
		for (int i= nEntries - 1; i >= 0; i--) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (isEntryKind(kind)) {
				if (!projelements.remove(cpe)) {
					cpelements.remove(i);
					lastRemovePos= i;
				}	
			}
		}
		
		cpelements.addAll(lastRemovePos, projelements);

		if (lastRemovePos != nEntries || !projelements.isEmpty()) {
			fClassPathList.setElements(cpelements);
		}
	}
	
		
	private CPListElement[] openNewClassFolderDialog(CPListElement existing) {
		String title= (existing == null) ? NewWizardMessages.getString("LibrariesWorkbookPage.NewClassFolderDialog.new.title") : NewWizardMessages.getString("LibrariesWorkbookPage.NewClassFolderDialog.edit.title"); //$NON-NLS-1$ //$NON-NLS-2$
		IProject currProject= fCurrJProject.getProject();
		
		NewContainerDialog dialog= new NewContainerDialog(getShell(), title, currProject, getUsedContainers(existing), existing);
		IPath projpath= currProject.getFullPath();
		dialog.setMessage(NewWizardMessages.getFormattedString("LibrariesWorkbookPage.NewClassFolderDialog.description", projpath.toString())); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {
			IFolder folder= dialog.getFolder();
			return new CPListElement[] { newCPLibraryElement(folder) };
		}
		return null;
	}
			
			
	private CPListElement[] openClassFolderDialog(CPListElement existing) {	

		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };

		IContainer[] usedContainers= getUsedContainers(existing);
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, usedContainers);	
			
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		String title= (existing == null) ? NewWizardMessages.getString("LibrariesWorkbookPage.ExistingClassFolderDialog.new.title") : NewWizardMessages.getString("LibrariesWorkbookPage.ExistingClassFolderDialog.edit.title"); //$NON-NLS-1$ //$NON-NLS-2$
		String message= (existing == null) ? NewWizardMessages.getString("LibrariesWorkbookPage.ExistingClassFolderDialog.new.description") : NewWizardMessages.getString("LibrariesWorkbookPage.ExistingClassFolderDialog.edit.description"); //$NON-NLS-1$ //$NON-NLS-2$

		
		MultipleFolderSelectionDialog dialog= new MultipleFolderSelectionDialog(getShell(), lp, cp);
		dialog.setExisting(usedContainers);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setInput(fWorkspaceRoot);
		if (existing == null) {
			dialog.setInitialFocus(fCurrJProject.getProject());
		} else {
			dialog.setInitialFocus(existing.getResource());
		}
		
		if (dialog.open() == Window.OK) {
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
	
	private CPListElement[] openJarFileDialog(CPListElement existing) {
		Class[] acceptedClasses= new Class[] { IFile.class };
		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, existing == null);
		ViewerFilter filter= new ArchiveFileFilter(getUsedJARFiles(existing), true);
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		String title= (existing == null) ? NewWizardMessages.getString("LibrariesWorkbookPage.JARArchiveDialog.new.title") : NewWizardMessages.getString("LibrariesWorkbookPage.JARArchiveDialog.edit.title"); //$NON-NLS-1$ //$NON-NLS-2$
		String message= (existing == null) ? NewWizardMessages.getString("LibrariesWorkbookPage.JARArchiveDialog.new.description") : NewWizardMessages.getString("LibrariesWorkbookPage.JARArchiveDialog.edit.description"); //$NON-NLS-1$ //$NON-NLS-2$

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setInput(fWorkspaceRoot);
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
		if (existing == null) {
			dialog.setInitialSelection(fCurrJProject.getProject());		
		} else {
			dialog.setInitialSelection(existing.getResource());
		}

		if (dialog.open() == Window.OK) {
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
	
	private IContainer[] getUsedContainers(CPListElement existing) {
		ArrayList res= new ArrayList();
		if (fCurrJProject.exists()) {
			try {
				IPath outputLocation= fCurrJProject.getOutputLocation();
				if (outputLocation != null) {
					IResource resource= fWorkspaceRoot.findMember(outputLocation);
					if (resource instanceof IFolder) { // != Project
						res.add(resource);
					}
				}
			} catch (JavaModelException e) {
				// ignore it here, just log
				JavaPlugin.log(e.getStatus());
			}
		}	
			
		List cplist= fLibrariesList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			if (elem.getEntryKind() == IClasspathEntry.CPE_LIBRARY && (elem != existing)) {
				IResource resource= elem.getResource();
				if (resource instanceof IContainer && !resource.equals(existing)) {
					res.add(resource);
				}
			}
		}
		return (IContainer[]) res.toArray(new IContainer[res.size()]);
	}
	
	private IFile[] getUsedJARFiles(CPListElement existing) {
		List res= new ArrayList();
		List cplist= fLibrariesList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			if (elem.getEntryKind() == IClasspathEntry.CPE_LIBRARY && (elem != existing)) {
				IResource resource= elem.getResource();
				if (resource instanceof IFile) {
					res.add(resource);
				}
			}
		}
		return (IFile[]) res.toArray(new IFile[res.size()]);
	}	
	
	private CPListElement newCPLibraryElement(IResource res) {
		return new CPListElement(fCurrJProject, IClasspathEntry.CPE_LIBRARY, res.getFullPath(), res);
	}

	
	private CPListElement[] openExtJarFileDialog(CPListElement existing) {
		String lastUsedPath;
		if (existing != null) {
			lastUsedPath= existing.getPath().removeLastSegments(1).toOSString();
		} else {
			lastUsedPath= fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
			if (lastUsedPath == null) {
				lastUsedPath= ""; //$NON-NLS-1$
			}
		}
		String title= (existing == null) ? NewWizardMessages.getString("LibrariesWorkbookPage.ExtJARArchiveDialog.new.title") : NewWizardMessages.getString("LibrariesWorkbookPage.ExtJARArchiveDialog.edit.title"); //$NON-NLS-1$ //$NON-NLS-2$
		
		FileDialog dialog= new FileDialog(getShell(), existing == null ? SWT.MULTI : SWT.SINGLE);
		dialog.setText(title);
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"}); //$NON-NLS-1$
		dialog.setFilterPath(lastUsedPath);
		if (existing != null) {
			dialog.setFileName(existing.getPath().lastSegment());
		}
		
		String res= dialog.open();
		if (res == null) {
			return null;
		}
		String[] fileNames= dialog.getFileNames();
		int nChosen= fileNames.length;
			
		IPath filterPath= new Path(dialog.getFilterPath());
		CPListElement[] elems= new CPListElement[nChosen];
		for (int i= 0; i < nChosen; i++) {
			IPath path= filterPath.append(fileNames[i]).makeAbsolute();	
			elems[i]= new CPListElement(fCurrJProject, IClasspathEntry.CPE_LIBRARY, path, null);
		}
		fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, filterPath.toOSString());
		
		return elems;
	}
	
	private CPListElement[] openVariableSelectionDialog(CPListElement existing) {
		if (existing == null) {
			NewVariableEntryDialog dialog= new NewVariableEntryDialog(getShell());
			dialog.setTitle(NewWizardMessages.getString("LibrariesWorkbookPage.VariableSelectionDialog.new.title")); //$NON-NLS-1$
			if (dialog.open() == Window.OK) {
				List existingElements= fLibrariesList.getElements();
				
				IPath[] paths= dialog.getResult();
				ArrayList result= new ArrayList();
				for (int i = 0; i < paths.length; i++) {
					CPListElement elem= new CPListElement(fCurrJProject, IClasspathEntry.CPE_VARIABLE, paths[i], null);
					IPath resolvedPath= JavaCore.getResolvedVariablePath(paths[i]);
					elem.setIsMissing((resolvedPath == null) || !resolvedPath.toFile().exists());
					if (!existingElements.contains(elem)) {
						result.add(elem);
					}
				}
				return (CPListElement[]) result.toArray(new CPListElement[result.size()]);
			}
		} else {
			List existingElements= fLibrariesList.getElements();
			ArrayList existingPaths= new ArrayList(existingElements.size());
			for (int i= 0; i < existingElements.size(); i++) {
				CPListElement elem= (CPListElement) existingElements.get(i);
				if (elem.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
					existingPaths.add(elem.getPath());
				}
			}
			EditVariableEntryDialog dialog= new EditVariableEntryDialog(getShell(), existing.getPath(), existingPaths);
			dialog.setTitle(NewWizardMessages.getString("LibrariesWorkbookPage.VariableSelectionDialog.edit.title")); //$NON-NLS-1$
			if (dialog.open() == Window.OK) {
				CPListElement elem= new CPListElement(fCurrJProject, IClasspathEntry.CPE_VARIABLE, dialog.getPath(), null);
				return new CPListElement[] { elem };
			}
		}
		return null;
	}

	private CPListElement[] openContainerSelectionDialog(CPListElement existing) {
		IClasspathEntry elem= null;
		String title;
		if (existing == null) {
			title= NewWizardMessages.getString("LibrariesWorkbookPage.ContainerDialog.new.title"); //$NON-NLS-1$
		} else {
			title= NewWizardMessages.getString("LibrariesWorkbookPage.ContainerDialog.edit.title"); //$NON-NLS-1$
			elem= existing.getClasspathEntry();
		}
		ClasspathContainerWizard wizard= new ClasspathContainerWizard(elem, fCurrJProject, getRawClasspath());
		wizard.setWindowTitle(title);
		if (ClasspathContainerWizard.openWizard(getShell(), wizard) == Window.OK) {
			IClasspathEntry[] created= wizard.getNewEntries();
			if (created != null) {
				CPListElement[] res= new CPListElement[created.length];
				for (int i= 0; i < res.length; i++) {
					res[i]= new CPListElement(fCurrJProject, IClasspathEntry.CPE_CONTAINER, created[i].getPath(), null);
				}
				return res;
			}
		}			
		return null;
	}
		
	private IClasspathEntry[] getRawClasspath() {
		IClasspathEntry[] currEntries= new IClasspathEntry[fClassPathList.getSize()];
		for (int i= 0; i < currEntries.length; i++) {
			CPListElement curr= (CPListElement) fClassPathList.getElement(i);
			currEntries[i]= curr.getClasspathEntry();
		}
		return currEntries;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
	 */
	public boolean isEntryKind(int kind) {
		return kind == IClasspathEntry.CPE_LIBRARY || kind == IClasspathEntry.CPE_VARIABLE || kind == IClasspathEntry.CPE_CONTAINER;
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fLibrariesList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		fLibrariesList.selectElements(new StructuredSelection(selElements));
	}	


}
