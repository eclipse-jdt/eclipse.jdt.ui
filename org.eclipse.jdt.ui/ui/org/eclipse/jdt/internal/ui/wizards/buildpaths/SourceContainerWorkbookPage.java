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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.NewFolderDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

public class SourceContainerWorkbookPage extends BuildPathBasePage {

	private ListDialogField fClassPathList;
	private IJavaProject fCurrJProject;
	private IPath fProjPath;
	
	private Control fSWTControl;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	private TreeListDialogField fFoldersList;
	
	private StringDialogField fOutputLocationField;
	
	private SelectionButtonDialogField fUseFolderOutputs;
	
	private final int IDX_ADD= 0;
	private final int IDX_EDIT= 2;
	private final int IDX_REMOVE= 3;	

	public SourceContainerWorkbookPage(IWorkspaceRoot root, ListDialogField classPathList, StringDialogField outputLocationField) {
		fWorkspaceRoot= root;
		fClassPathList= classPathList;
	
		fOutputLocationField= outputLocationField;
		
		fSWTControl= null;
				
		SourceContainerAdapter adapter= new SourceContainerAdapter();
					
		String[] buttonLabels;

		buttonLabels= new String[] { 
			/* 0 = IDX_ADDEXIST */ NewWizardMessages.getString("SourceContainerWorkbookPage.folders.add.button"), //$NON-NLS-1$
			/* 1 */ null,
			/* 2 = IDX_EDIT */ NewWizardMessages.getString("SourceContainerWorkbookPage.folders.edit.button"), //$NON-NLS-1$
			/* 3 = IDX_REMOVE */ NewWizardMessages.getString("SourceContainerWorkbookPage.folders.remove.button") //$NON-NLS-1$
		};
		
		fFoldersList= new TreeListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fFoldersList.setDialogFieldListener(adapter);
		fFoldersList.setLabelText(NewWizardMessages.getString("SourceContainerWorkbookPage.folders.label")); //$NON-NLS-1$
		
		fFoldersList.setViewerSorter(new CPListElementSorter());
		fFoldersList.enableButton(IDX_EDIT, false);
		
		fUseFolderOutputs= new SelectionButtonDialogField(SWT.CHECK);
		fUseFolderOutputs.setSelection(false);
		fUseFolderOutputs.setLabelText(NewWizardMessages.getString("SourceContainerWorkbookPage.folders.check")); //$NON-NLS-1$
		fUseFolderOutputs.setDialogFieldListener(adapter);
	}
	
	public void init(IJavaProject jproject) {
		fCurrJProject= jproject;
		fProjPath= fCurrJProject.getProject().getFullPath();	
		updateFoldersList();
	}
	
	private void updateFoldersList() {	
		ArrayList folders= new ArrayList();
	
		boolean useFolderOutputs= false;
		List cpelements= fClassPathList.getElements();
		for (int i= 0; i < cpelements.size(); i++) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				folders.add(cpe);
				boolean hasOutputFolder= (cpe.getAttribute(CPListElement.OUTPUT) != null);
				if (hasOutputFolder) {
					useFolderOutputs= true;
				}

			}
		}
		fFoldersList.setElements(folders);
		fUseFolderOutputs.setSelection(useFolderOutputs);
		
		for (int i= 0; i < folders.size(); i++) {
			CPListElement cpe= (CPListElement) folders.get(i);
			IPath[] patterns= (IPath[]) cpe.getAttribute(CPListElement.EXCLUSION);
			boolean hasOutputFolder= (cpe.getAttribute(CPListElement.OUTPUT) != null);
			if (patterns.length > 0 || hasOutputFolder) {
				fFoldersList.expandElement(cpe, 3);
			}				
		}
	}			
	
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		Composite composite= new Composite(parent, SWT.NONE);
		
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fFoldersList, fUseFolderOutputs }, true);
		LayoutUtil.setHorizontalGrabbing(fFoldersList.getTreeControl(null));
		
		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fFoldersList.setButtonsMinWidth(buttonBarWidth);
			
		fSWTControl= composite;
		
		// expand
		List elements= fFoldersList.getElements();
		for (int i= 0; i < elements.size(); i++) {
			CPListElement elem= (CPListElement) elements.get(i);
			IPath[] patterns= (IPath[]) elem.getAttribute(CPListElement.EXCLUSION);
			IPath output= (IPath) elem.getAttribute(CPListElement.OUTPUT);
			if (patterns.length > 0 || output != null) {
				fFoldersList.expandElement(elem, 3);
			}
		}
		return composite;
	}
	
	private Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();
	}
	
	
	private class SourceContainerAdapter implements ITreeListAdapter, IDialogFieldListener {
	
		private final Object[] EMPTY_ARR= new Object[0];
		
		// -------- IListAdapter --------
		public void customButtonPressed(TreeListDialogField field, int index) {
			sourcePageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(TreeListDialogField field) {
			sourcePageSelectionChanged(field);
		}
		
		public void doubleClicked(TreeListDialogField field) {
			sourcePageDoubleClicked(field);
		}
		
		public void keyPressed(TreeListDialogField field, KeyEvent event) {
			sourcePageKeyPressed(field, event);
		}	

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (element instanceof CPListElement) {
				return ((CPListElement) element).getChildren(!fUseFolderOutputs.isSelected());
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
			return (element instanceof CPListElement);
		}		
		
		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			sourcePageDialogFieldChanged(field);
		}

	}
	
	protected void sourcePageKeyPressed(TreeListDialogField field, KeyEvent event) {
		if (field == fFoldersList) {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				List selection= field.getSelectedElements();
				if (canRemove(selection)) {
					removeEntry();
				}
			}
		}	
	}	
	
	protected void sourcePageDoubleClicked(TreeListDialogField field) {
		if (field == fFoldersList) {
			List selection= field.getSelectedElements();
			if (canEdit(selection)) {
				editEntry();
			}
		}
	}
	
	private boolean hasFolders(IContainer container) {
		try {
			IResource[] members= container.members();
			for (int i= 0; i < members.length; i++) {
				if (members[i] instanceof IContainer) {
					return true;
				}
			}
		} catch (CoreException e) {
			// ignore
		}
		return false;
	}
	
	
	protected void sourcePageCustomButtonPressed(DialogField field, int index) {
		if (field == fFoldersList) {
			if (index == IDX_ADD) {
				List elementsToAdd= new ArrayList(10);
				IProject project= fCurrJProject.getProject();
				if (project.exists()) {
					if (hasFolders(project)) {
						CPListElement[] srcentries= openSourceContainerDialog(null);
						if (srcentries != null) {
							for (int i= 0; i < srcentries.length; i++) {
								elementsToAdd.add(srcentries[i]);
							}
						}
					} else {
						CPListElement entry= openNewSourceContainerDialog(null, true);
						if (entry != null) {
							elementsToAdd.add(entry);
						}	
					}
				} else {
					CPListElement entry= openNewSourceContainerDialog(null, false);
					if (entry != null) {
						elementsToAdd.add(entry);
					}
				}
				if (!elementsToAdd.isEmpty()) {
					if (fFoldersList.getSize() == 1) {
						CPListElement existing= (CPListElement) fFoldersList.getElement(0);
						if (existing.getResource() instanceof IProject) {
							askForChangingBuildPathDialog(existing);
						}
					}
					HashSet modifiedElements= new HashSet();
					askForAddingExclusionPatternsDialog(elementsToAdd, modifiedElements);
					
					fFoldersList.addElements(elementsToAdd);
					fFoldersList.postSetSelection(new StructuredSelection(elementsToAdd));
					
					if (!modifiedElements.isEmpty()) {
						for (Iterator iter= modifiedElements.iterator(); iter.hasNext();) {
							Object elem= iter.next();
							fFoldersList.refresh(elem);
							fFoldersList.expandElement(elem, 3);
						}
					}

				}				
			} else if (index == IDX_EDIT) {
				editEntry();
			} else if (index == IDX_REMOVE) {
				removeEntry();
			}
		}
	}

	private void editEntry() {
		List selElements= fFoldersList.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		Object elem= selElements.get(0);
		if (fFoldersList.getIndexOfElement(elem) != -1) {
			editElementEntry((CPListElement) elem);
		} else if (elem instanceof CPListElementAttribute) {
			editAttributeEntry((CPListElementAttribute) elem);
		}
	}

	private void editElementEntry(CPListElement elem) {
		CPListElement res= null;
		
		res= openNewSourceContainerDialog(elem, true);
		
		if (res != null) {
			fFoldersList.replaceElement(elem, res);
		}
	}

	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		if (key.equals(CPListElement.OUTPUT)) {
			CPListElement selElement=  elem.getParent();
			OutputLocationDialog dialog= new OutputLocationDialog(getShell(), selElement);
			if (dialog.open() == Window.OK) {
				selElement.setAttribute(CPListElement.OUTPUT, dialog.getOutputLocation());
				fFoldersList.refresh();
				fClassPathList.dialogFieldChanged(); // validate
			}
		} else if (key.equals(CPListElement.EXCLUSION)) {
			CPListElement selElement= elem.getParent();
			ExclusionPatternDialog dialog= new ExclusionPatternDialog(getShell(), selElement);
			if (dialog.open() == Window.OK) {
				selElement.setAttribute(CPListElement.EXCLUSION, dialog.getExclusionPattern());
				fFoldersList.refresh();
				fClassPathList.dialogFieldChanged(); // validate
			}		
		}
	}


	
	protected void sourcePageSelectionChanged(DialogField field) {
		List selected= fFoldersList.getSelectedElements();
		fFoldersList.enableButton(IDX_EDIT, canEdit(selected));
		fFoldersList.enableButton(IDX_REMOVE, canRemove(selected));
	}
	
	private void removeEntry() {
		List selElements= fFoldersList.getSelectedElements();
		for (int i= selElements.size() - 1; i >= 0 ; i--) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				String key= attrib.getKey();
				Object value= key.equals(CPListElement.EXCLUSION) ? new Path[0] : null;
				attrib.getParent().setAttribute(key, value);
				selElements.remove(i);
			}
		}
		if (selElements.isEmpty()) {
			fFoldersList.refresh();
			fClassPathList.dialogFieldChanged(); // validate
		} else {
			fFoldersList.removeElements(selElements);
		}
	}
	
	private boolean canRemove(List selElements) {
		if (selElements.size() == 0) {
			return false;
		}
		for (int i= 0; i < selElements.size(); i++) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				if (attrib.getKey().equals(CPListElement.EXCLUSION)) {
					if (((IPath[]) attrib.getValue()).length == 0) {
						return false;
					}
				} else if (attrib.getValue() == null) {
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
	
	private boolean canEdit(List selElements) {
		if (selElements.size() != 1) {
			return false;
		}
		Object elem= selElements.get(0);
		if (elem instanceof CPListElement) {
			return false;
		}
		if (elem instanceof CPListElementAttribute) {
			return true;
		}
		return false;
	}	
	
	private void sourcePageDialogFieldChanged(DialogField field) {
		if (fCurrJProject == null) {
			// not initialized
			return;
		}
		
		if (field == fUseFolderOutputs) {
			if (!fUseFolderOutputs.isSelected()) {
				int nFolders= fFoldersList.getSize();
				for (int i= 0; i < nFolders; i++) {
					CPListElement cpe= (CPListElement) fFoldersList.getElement(i);
					cpe.setAttribute(CPListElement.OUTPUT, null);
				}
			}
			fFoldersList.refresh();
		} else if (field == fFoldersList) {
			updateClasspathList();
		}
	}	
	
		
	private void updateClasspathList() {
		List srcelements= fFoldersList.getElements();
		
		List cpelements= fClassPathList.getElements();
		int nEntries= cpelements.size();
		// backwards, as entries will be deleted
		int lastRemovePos= nEntries;
		int afterLastSourcePos= 0;
		for (int i= nEntries - 1; i >= 0; i--) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (isEntryKind(kind)) {
				if (!srcelements.remove(cpe)) {
					cpelements.remove(i);
					lastRemovePos= i;
				} else if (lastRemovePos == nEntries) {
					afterLastSourcePos= i + 1;
				}
			}
		}

		if (!srcelements.isEmpty()) {
			int insertPos= Math.min(afterLastSourcePos, lastRemovePos);
			cpelements.addAll(insertPos, srcelements);
		}
		
		if (lastRemovePos != nEntries || !srcelements.isEmpty()) {
			fClassPathList.setElements(cpelements);
		}
	}
		
	private CPListElement openNewSourceContainerDialog(CPListElement existing, boolean includeLinked) {	
		if (includeLinked) {
			NewFolderDialog dialog= new NewFolderDialog(getShell(), fCurrJProject.getProject());
			if (dialog.open() == Window.OK) {
				IResource createdFolder= (IResource) dialog.getResult()[0];
				return newCPSourceElement(createdFolder);
			}
			return null;
		} else {
			String title= (existing == null) ? NewWizardMessages.getString("SourceContainerWorkbookPage.NewSourceFolderDialog.new.title") : NewWizardMessages.getString("SourceContainerWorkbookPage.NewSourceFolderDialog.edit.title"); //$NON-NLS-1$ //$NON-NLS-2$
	
			IProject proj= fCurrJProject.getProject();
			NewSourceFolderDialog dialog= new NewSourceFolderDialog(getShell(), title, proj, getExistingContainers(existing), existing);
			dialog.setMessage(NewWizardMessages.getFormattedString("SourceContainerWorkbookPage.NewSourceFolderDialog.description", fProjPath.toString())); //$NON-NLS-1$
			if (dialog.open() == Window.OK) {
				IResource folder= dialog.getSourceFolder();
				return newCPSourceElement(folder);
			}
			return null;
		}
	}
	
	
	
	/**
	 * Asks to change the output folder to 'proj/bin' when no source folders were existing
	 */ 
	private void askForChangingBuildPathDialog(CPListElement existing) {
		IPath outputFolder= new Path(fOutputLocationField.getText());
		
		IPath newOutputFolder= null;
		String message;
		if (outputFolder.segmentCount() == 1) {
			String outputFolderName= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME);
			newOutputFolder= outputFolder.append(outputFolderName);
			message= NewWizardMessages.getFormattedString("SourceContainerWorkbookPage.ChangeOutputLocationDialog.project_and_output.message", newOutputFolder); //$NON-NLS-1$
		} else {
			message= NewWizardMessages.getString("SourceContainerWorkbookPage.ChangeOutputLocationDialog.project.message"); //$NON-NLS-1$
		}
		String title= NewWizardMessages.getString("SourceContainerWorkbookPage.ChangeOutputLocationDialog.title"); //$NON-NLS-1$
		if (MessageDialog.openQuestion(getShell(), title, message)) {
			fFoldersList.removeElement(existing);
			if (newOutputFolder != null) {
				fOutputLocationField.setText(newOutputFolder.toString());
			}
		}			
	}
	
	private void askForAddingExclusionPatternsDialog(List newEntries, Set modifiedEntries) {
		fixNestingConflicts(newEntries, fFoldersList.getElements(), modifiedEntries);
		if (!modifiedEntries.isEmpty()) {
			String title= NewWizardMessages.getString("SourceContainerWorkbookPage.exclusion_added.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("SourceContainerWorkbookPage.exclusion_added.message"); //$NON-NLS-1$
			MessageDialog.openInformation(getShell(), title, message);
		}
	}
	
	private CPListElement[] openSourceContainerDialog(CPListElement existing) {
		
		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };
		List existingContainers= getExistingContainers(null);
		
		IProject[] allProjects= fWorkspaceRoot.getProjects();
		ArrayList rejectedElements= new ArrayList(allProjects.length);
		IProject currProject= fCurrJProject.getProject();
		for (int i= 0; i < allProjects.length; i++) {
			if (!allProjects[i].equals(currProject)) {
				rejectedElements.add(allProjects[i]);
			}
		}
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, rejectedElements.toArray());
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new BaseWorkbenchContentProvider();

		String title= (existing == null) ? NewWizardMessages.getString("SourceContainerWorkbookPage.ExistingSourceFolderDialog.new.title") : NewWizardMessages.getString("SourceContainerWorkbookPage.ExistingSourceFolderDialog.edit.title"); //$NON-NLS-1$ //$NON-NLS-2$
		String message= (existing == null) ? NewWizardMessages.getString("SourceContainerWorkbookPage.ExistingSourceFolderDialog.new.description") : NewWizardMessages.getString("SourceContainerWorkbookPage.ExistingSourceFolderDialog.edit.description"); //$NON-NLS-1$ //$NON-NLS-2$

		MultipleFolderSelectionDialog dialog= new MultipleFolderSelectionDialog(getShell(), lp, cp);
		dialog.setExisting(existingContainers.toArray());
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setInput(fCurrJProject.getProject().getParent());
		if (existing == null) {
			dialog.setInitialFocus(fCurrJProject.getProject());
		} else {
			dialog.setInitialFocus(existing.getResource());
		}		
		if (dialog.open() == Window.OK) {
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
	
	private List getExistingContainers(CPListElement existing) {
		List res= new ArrayList();
		List cplist= fFoldersList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			if (elem != existing) {
				IResource resource= elem.getResource();
				if (resource instanceof IContainer) { // defensive code
					res.add(resource);	
				}
			}
		}
		return res;
	}
	
	private CPListElement newCPSourceElement(IResource res) {
		Assert.isNotNull(res);
		return new CPListElement(fCurrJProject, IClasspathEntry.CPE_SOURCE, res.getFullPath(), res);
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fFoldersList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		fFoldersList.selectElements(new StructuredSelection(selElements));
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
	 */
	public boolean isEntryKind(int kind) {
		return kind == IClasspathEntry.CPE_SOURCE;
	}	

}
