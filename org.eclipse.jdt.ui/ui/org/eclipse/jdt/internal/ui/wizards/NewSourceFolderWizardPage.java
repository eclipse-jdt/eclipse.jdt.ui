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
package org.eclipse.jdt.internal.ui.wizards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;


public class NewSourceFolderWizardPage extends NewElementWizardPage {
		
	private static final String PAGE_NAME= "NewSourceFolderWizardPage"; //$NON-NLS-1$

	private StringButtonDialogField fProjectField;
	private StatusInfo fProjectStatus;
	
	private StringButtonDialogField fRootDialogField;
	private StatusInfo fRootStatus;
	
	private SelectionButtonDialogField fExcludeInOthersFields;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	private IJavaProject fCurrJProject;
	private IClasspathEntry[] fEntries;
	private IPath fOutputLocation;
	
	private IClasspathEntry[] fNewEntries;
	private IPath fNewOutputLocation;	
	
	private boolean fIsProjectAsSourceFolder;
	
	private IPackageFragmentRoot fCreatedRoot;
	
	public NewSourceFolderWizardPage() {
		super(PAGE_NAME);
		
		setTitle(NewWizardMessages.getString("NewSourceFolderWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewSourceFolderWizardPage.description"));		 //$NON-NLS-1$
		
		fWorkspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		
		RootFieldAdapter adapter= new RootFieldAdapter();
		
		fProjectField= new StringButtonDialogField(adapter);
		fProjectField.setDialogFieldListener(adapter);
		fProjectField.setLabelText(NewWizardMessages.getString("NewSourceFolderWizardPage.project.label")); //$NON-NLS-1$
		fProjectField.setButtonLabel(NewWizardMessages.getString("NewSourceFolderWizardPage.project.button"));	 //$NON-NLS-1$
		
		fRootDialogField= new StringButtonDialogField(adapter);
		fRootDialogField.setDialogFieldListener(adapter);
		fRootDialogField.setLabelText(NewWizardMessages.getString("NewSourceFolderWizardPage.root.label")); //$NON-NLS-1$
		fRootDialogField.setButtonLabel(NewWizardMessages.getString("NewSourceFolderWizardPage.root.button")); //$NON-NLS-1$
		
		fExcludeInOthersFields= new SelectionButtonDialogField(SWT.CHECK);
		fExcludeInOthersFields.setDialogFieldListener(adapter);
		fExcludeInOthersFields.setLabelText(NewWizardMessages.getString("NewSourceFolderWizardPage.exclude.label")); //$NON-NLS-1$
		
		fExcludeInOthersFields.setEnabled(JavaCore.ENABLED.equals(JavaCore.getOption(JavaCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS)));
		
		fRootStatus= new StatusInfo();
		fProjectStatus= new StatusInfo();
	}
			
	// -------- Initialization ---------
		
	public void init(IStructuredSelection selection) {
		if (selection == null || selection.isEmpty()) {
			setDefaultAttributes();
			return;
		}
		
		Object selectedElement= selection.getFirstElement();
		if (selectedElement == null) {
			selectedElement= EditorUtility.getActiveEditorJavaInput();
		}				
		
		String projPath= null;
		
		if (selectedElement instanceof IResource) {
			IProject proj= ((IResource)selectedElement).getProject();
			if (proj != null) {
				projPath= proj.getFullPath().makeRelative().toString();
			}	
		} else if (selectedElement instanceof IJavaElement) {
			IJavaProject jproject= ((IJavaElement)selectedElement).getJavaProject();
			if (jproject != null) {
				projPath= jproject.getProject().getFullPath().makeRelative().toString();
			}
		}	
		
		if (projPath != null) {
			fProjectField.setText(projPath);
			fRootDialogField.setText(""); //$NON-NLS-1$
		} else {
			setDefaultAttributes();
		}
	}
	
	private void setDefaultAttributes() {
		String projPath= ""; //$NON-NLS-1$
		
		try {
			// find the first java project
			IProject[] projects= fWorkspaceRoot.getProjects();
			for (int i= 0; i < projects.length; i++) {
				IProject proj= projects[i];
				if (proj.hasNature(JavaCore.NATURE_ID)) {
					projPath= proj.getFullPath().makeRelative().toString();
					break;
				}
			}					
		} catch (CoreException e) {
			// ignore here
		}
		fProjectField.setText(projPath);
		fRootDialogField.setText("");		 //$NON-NLS-1$
	}
	

	// -------- UI Creation ---------

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
			
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;	
		layout.numColumns= 3;
		composite.setLayout(layout);
				
		fProjectField.doFillIntoGrid(composite, 3);	
		fRootDialogField.doFillIntoGrid(composite, 3);
		fExcludeInOthersFields.doFillIntoGrid(composite, 3);
		
		int maxFieldWidth= convertWidthInCharsToPixels(40);
		LayoutUtil.setWidthHint(fProjectField.getTextControl(null), maxFieldWidth);
		LayoutUtil.setHorizontalGrabbing(fProjectField.getTextControl(null));	
		LayoutUtil.setWidthHint(fRootDialogField.getTextControl(null), maxFieldWidth);	
			
		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.NEW_PACKAGEROOT_WIZARD_PAGE);		
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fRootDialogField.setFocus();
		}
	}	
		
	// -------- ContainerFieldAdapter --------

	private class RootFieldAdapter implements IStringButtonAdapter, IDialogFieldListener {

		// -------- IStringButtonAdapter
		public void changeControlPressed(DialogField field) {
			packRootChangeControlPressed(field);
		}
		
		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
			packRootDialogFieldChanged(field);
		}
	}
	protected void packRootChangeControlPressed(DialogField field) {
		if (field == fRootDialogField) {
			IPath initialPath= new Path(fRootDialogField.getText());
			String title= NewWizardMessages.getString("NewSourceFolderWizardPage.ChooseExistingRootDialog.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("NewSourceFolderWizardPage.ChooseExistingRootDialog.description"); //$NON-NLS-1$
			IFolder folder= chooseFolder(title, message, initialPath);
			if (folder != null) {
				IPath path= folder.getFullPath().removeFirstSegments(1);
				fRootDialogField.setText(path.toString());
			}
		} else if (field == fProjectField) {
			IJavaProject jproject= chooseProject();
			if (jproject != null) {
				IPath path= jproject.getProject().getFullPath().makeRelative();
				fProjectField.setText(path.toString());
			}
		} 
	}	
	
	protected void packRootDialogFieldChanged(DialogField field) {
		if (field == fRootDialogField) {
			updateRootStatus();
		} else if (field == fProjectField) {
			updateProjectStatus();
			updateRootStatus();
		} else if (field == fExcludeInOthersFields) {
			updateRootStatus();
		}
		updateStatus(new IStatus[] { fProjectStatus, fRootStatus });
	}
	
	
	private void updateProjectStatus() {
		fCurrJProject= null;
		fIsProjectAsSourceFolder= false;
		
		String str= fProjectField.getText();
		if (str.length() == 0) {
			fProjectStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.EnterProjectName")); //$NON-NLS-1$
			return;
		}
		IPath path= new Path(str);
		if (path.segmentCount() != 1) {
			fProjectStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.InvalidProjectPath")); //$NON-NLS-1$
			return;
		}
		IProject project= fWorkspaceRoot.getProject(path.toString());
		if (!project.exists()) {
			fProjectStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.ProjectNotExists")); //$NON-NLS-1$
			return;
		}
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				fCurrJProject= JavaCore.create(project);
				fEntries= fCurrJProject.getRawClasspath();
				fOutputLocation= fCurrJProject.getOutputLocation();
				fProjectStatus.setOK();
				return;
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			fCurrJProject= null;
		}	
		fProjectStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.NotAJavaProject")); //$NON-NLS-1$
	}

	private void updateRootStatus() {
		fRootDialogField.enableButton(fCurrJProject != null);
		fIsProjectAsSourceFolder= false;
		if (fCurrJProject == null) {
			return;
		}
		fRootStatus.setOK();
		
		IPath projPath= fCurrJProject.getProject().getFullPath();
		String str= fRootDialogField.getText();
		if (str.length() == 0) {
			fRootStatus.setError(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.error.EnterRootName", fCurrJProject.getProject().getFullPath().toString())); //$NON-NLS-1$
		} else {
			IPath path= projPath.append(str);
			IStatus validate= fWorkspaceRoot.getWorkspace().validatePath(path.toString(), IResource.FOLDER);
			if (validate.matches(IStatus.ERROR)) {
				fRootStatus.setError(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.error.InvalidRootName", validate.getMessage())); //$NON-NLS-1$
			} else {
				IResource res= fWorkspaceRoot.findMember(path);
				if (res != null) {
					if (res.getType() != IResource.FOLDER) {
						fRootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.NotAFolder")); //$NON-NLS-1$
						return;
					}
				}
				ArrayList newEntries= new ArrayList(fEntries.length + 1);
				int projectEntryIndex= -1;
				
				for (int i= 0; i < fEntries.length; i++) {
					IClasspathEntry curr= fEntries[i];
					if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if (path.equals(curr.getPath())) {
							fRootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.AlreadyExisting")); //$NON-NLS-1$
							return;
						}
						if (projPath.equals(curr.getPath())) {
							projectEntryIndex= i;
						}	
					}
					newEntries.add(curr);
				}
				
				IClasspathEntry newEntry= JavaCore.newSourceEntry(path);
				
				Set modified= new HashSet();				
				if (fExcludeInOthersFields.isSelected()) {
					addExclusionPatterns(newEntry, newEntries, modified);
					newEntries.add(JavaCore.newSourceEntry(path));
				} else {
					if (projectEntryIndex != -1) {
						fIsProjectAsSourceFolder= true;
						newEntries.set(projectEntryIndex, newEntry);
					} else {
						newEntries.add(JavaCore.newSourceEntry(path));
					}
				}
					
				fNewEntries= (IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]);
				fNewOutputLocation= fOutputLocation;

				IJavaModelStatus status= JavaConventions.validateClasspath(fCurrJProject, fNewEntries, fNewOutputLocation);
				if (!status.isOK()) {
					if (fOutputLocation.equals(projPath)) {
						fNewOutputLocation= projPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
						IStatus status2= JavaConventions.validateClasspath(fCurrJProject, fNewEntries, fNewOutputLocation);
						if (status2.isOK()) {
							if (fIsProjectAsSourceFolder) {
								fRootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceSFandOL", fNewOutputLocation.makeRelative().toString())); //$NON-NLS-1$
							} else {
								fRootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceOL", fNewOutputLocation.makeRelative().toString())); //$NON-NLS-1$
							}
							return;
						}
					}
					fRootStatus.setError(status.getMessage());
					return;
				} else if (fIsProjectAsSourceFolder) {
					fRootStatus.setInfo(NewWizardMessages.getString("NewSourceFolderWizardPage.warning.ReplaceSF")); //$NON-NLS-1$
					return;
				}
				if (!modified.isEmpty()) {
					fRootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.AddedExclusions", String.valueOf(modified.size()))); //$NON-NLS-1$
					return;
				}
			}
		}
	}
	
	private void addExclusionPatterns(IClasspathEntry newEntry, List existing, Set modifiedEntries) {
		IPath entryPath= newEntry.getPath();
		for (int i= 0; i < existing.size(); i++) {
			IClasspathEntry curr= (IClasspathEntry) existing.get(i);
			IPath currPath= curr.getPath();
			if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE && currPath.isPrefixOf(entryPath)) {
				IPath[] exclusionFilters= curr.getExclusionPatterns();
				if (!JavaModelUtil.isExcludedPath(entryPath, exclusionFilters)) {
					IPath pathToExclude= entryPath.removeFirstSegments(currPath.segmentCount()).addTrailingSeparator();
					IPath[] newExclusionFilters= new IPath[exclusionFilters.length + 1];
					System.arraycopy(exclusionFilters, 0, newExclusionFilters, 0, exclusionFilters.length);
					newExclusionFilters[exclusionFilters.length]= pathToExclude;
					
					IClasspathEntry updated= JavaCore.newSourceEntry(currPath, newExclusionFilters, curr.getOutputLocation());
					existing.set(i, updated);
					modifiedEntries.add(updated);
				}
			}
		}
	}	
	
	// ---- creation ----------------
	
	public IPackageFragmentRoot getNewPackageFragmentRoot() {
		return fCreatedRoot;
	}
	
	public IResource getCorrespondingResource() {
		return fCurrJProject.getProject().getFolder(fRootDialogField.getText());
	}
	
	public void createPackageFragmentRoot(IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask(NewWizardMessages.getString("NewSourceFolderWizardPage.operation"), 3); //$NON-NLS-1$
		try {
			IPath projPath= fCurrJProject.getProject().getFullPath();
			if (fOutputLocation.equals(projPath) && !fNewOutputLocation.equals(projPath)) {
				if (BuildPathsBlock.hasClassfiles(fCurrJProject.getProject())) {
					if (BuildPathsBlock.getRemoveOldBinariesQuery(getShell()).doQuery(projPath)) {
						BuildPathsBlock.removeOldClassfiles(fCurrJProject.getProject());
					}
				}
			}		
			
			String relPath= fRootDialogField.getText();
				
			IFolder folder= fCurrJProject.getProject().getFolder(relPath);
			if (!folder.exists()) {
				CoreUtility.createFolder(folder, true, true, new SubProgressMonitor(monitor, 1));			
			}
			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}
			
			fCurrJProject.setRawClasspath(fNewEntries, fNewOutputLocation, new SubProgressMonitor(monitor, 2));
	
			fCreatedRoot= fCurrJProject.getPackageFragmentRoot(folder);
		} finally {
			monitor.done();
		}
	}
		
	// ------------- choose dialogs
	
	private IFolder chooseFolder(String title, String message, IPath initialPath) {	
		Class[] acceptedClasses= new Class[] { IFolder.class };
		ISelectionStatusValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, null);	
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		IProject currProject= fCurrJProject.getProject();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setInput(currProject);
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
		IResource res= currProject.findMember(initialPath);
		if (res != null) {
			dialog.setInitialSelection(res);
		}

		if (dialog.open() == Window.OK) {
			return (IFolder) dialog.getFirstResult();
		}			
		return null;		
	}
	
	private IJavaProject chooseProject() {
		IJavaProject[] projects;
		try {
			projects= JavaCore.create(fWorkspaceRoot).getJavaProjects();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			projects= new IJavaProject[0];
		}
		
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle(NewWizardMessages.getString("NewSourceFolderWizardPage.ChooseProjectDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("NewSourceFolderWizardPage.ChooseProjectDialog.description")); //$NON-NLS-1$
		dialog.setElements(projects);
		dialog.setInitialSelections(new Object[] { fCurrJProject });
		if (dialog.open() == Window.OK) {			
			return (IJavaProject) dialog.getFirstResult();
		}			
		return null;		
	}
				
}
