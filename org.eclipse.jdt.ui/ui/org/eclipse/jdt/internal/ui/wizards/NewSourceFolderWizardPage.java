/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.NewJavaProjectPreferencePage;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;


public class NewSourceFolderWizardPage extends NewElementWizardPage {
		
	private static final String PAGE_NAME= "NewSourceFolderWizardPage"; //$NON-NLS-1$

	private StringButtonDialogField fProjectField;
	private StatusInfo fProjectStatus;
	
	private StringButtonDialogField fRootDialogField;
	private StatusInfo fRootStatus;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	private IJavaProject fCurrJProject;
	private IClasspathEntry[] fEntries;
	private IPath fOutputLocation;
	
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
		
		int maxFieldWidth= convertWidthInCharsToPixels(40);
		LayoutUtil.setWidthHint(fProjectField.getTextControl(null), maxFieldWidth);
		LayoutUtil.setHorizontalGrabbing(fProjectField.getTextControl(null));	
		LayoutUtil.setWidthHint(fRootDialogField.getTextControl(null), maxFieldWidth);	
			
		setControl(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.NEW_PACKAGEROOT_WIZARD_PAGE);		
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			fRootDialogField.setFocus();
		}
		super.setVisible(visible);
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
	private void packRootChangeControlPressed(DialogField field) {
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
	
	private void packRootDialogFieldChanged(DialogField field) {
		if (field == fRootDialogField) {
			updateRootStatus();
		} else if (field == fProjectField) {
			updateProjectStatus();
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
				for (int i= 0; i < fEntries.length; i++) {
					IClasspathEntry curr= fEntries[i];
					if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if (path.equals(curr.getPath())) {
							fRootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.AlreadyExisting")); //$NON-NLS-1$
							return;
						}
						if (projPath.equals(curr.getPath())) {
							fIsProjectAsSourceFolder= true;
							curr= JavaCore.newSourceEntry(path);
						}	
					}
					newEntries.add(curr);
				}
				if (!fIsProjectAsSourceFolder) {
					newEntries.add(JavaCore.newSourceEntry(path));
				}
	
				IClasspathEntry[] newEntriesArray= (IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]);

				IStatus status= JavaConventions.validateClasspath(fCurrJProject, newEntriesArray, fOutputLocation);
				if (!status.isOK()) {
					if (fIsProjectAsSourceFolder && fOutputLocation.equals(projPath)) {
						IPath newOutputLocation= projPath.append(NewJavaProjectPreferencePage.getOutputLocationName());
						IStatus status2= JavaConventions.validateClasspath(fCurrJProject, newEntriesArray, newOutputLocation);
						if (status2.isOK()) {
							fRootStatus.setWarning(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceSFandOL", newOutputLocation.toString())); //$NON-NLS-1$
							return;
						}
					}
					fRootStatus.setError(status.getMessage());
				} else if (fIsProjectAsSourceFolder) {
					fRootStatus.setWarning(NewWizardMessages.getString("NewSourceFolderWizardPage.warning.ReplaceSF")); //$NON-NLS-1$
				}					
			}
		}
	}
	
	// ---- creation ----------------
	
	public IPackageFragmentRoot getNewPackageFragmentRoot() {
		return fCreatedRoot;
	}
	
	public void createPackageFragmentRoot(IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}

		Shell shell= getShell();
		String relPath= fRootDialogField.getText();
			
		IFolder folder= fCurrJProject.getProject().getFolder(relPath);
		IPath path= folder.getFullPath();
		if (!folder.exists()) {
			CoreUtility.createFolder(folder, true, true, monitor);			
		}
		
		IClasspathEntry[] entries= fCurrJProject.getRawClasspath();
		IClasspathEntry[] newEntries;
		IPath outputLocation= fOutputLocation;
		
		if (fIsProjectAsSourceFolder) {
			IPath projPath= fCurrJProject.getProject().getFullPath();
			newEntries= new IClasspathEntry[entries.length];
			for (int i= 0; i < newEntries.length; i++) {
				IClasspathEntry curr= entries[i];
				if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE && curr.getPath().equals(projPath)) {		
					curr= JavaCore.newSourceEntry(path);
				}
				newEntries[i]= curr;
			}
			if (outputLocation.equals(projPath)) {
				outputLocation= projPath.append(NewJavaProjectPreferencePage.getOutputLocationName());
				if (BuildPathsBlock.hasClassfiles(fCurrJProject.getProject())) {
					if (BuildPathsBlock.getRemoveOldBinariesQuery(shell).doQuery(projPath)) {
						BuildPathsBlock.removeOldClassfiles(fCurrJProject.getProject());
					}
				}
			}
		} else {
			newEntries= new IClasspathEntry[entries.length + 1];
			int k= entries.length;
			for (int i= k - 1; i >= 0; i--) {
				IClasspathEntry curr= entries[i];
				if (k > i && curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					newEntries[k--]= JavaCore.newSourceEntry(path);
				}
				newEntries[k--]= curr;
			}
			if (k == 0) {
				newEntries[k--]= JavaCore.newSourceEntry(path);
			}
		}		

		fCurrJProject.setRawClasspath(newEntries, outputLocation, monitor);
		
		fCreatedRoot= fCurrJProject.getPackageFragmentRoot(folder);
	}
		
	// ------------- choose dialogs
	
	private IFolder chooseFolder(String title, String message, IPath initialPath) {	
		Class[] acceptedClasses= new Class[] { IFolder.class };
		ISelectionStatusValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		Object[] notWanted= getFilteredExistingContainerEntries();
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, notWanted);	
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		IProject currProject= fCurrJProject.getProject();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setInput(currProject);
		IResource res= currProject.findMember(initialPath);
		if (res != null) {
			dialog.setInitialSelection(res);
		}

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
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
		if (dialog.open() == ElementListSelectionDialog.OK) {			
			return (IJavaProject) dialog.getFirstResult();
		}			
		return null;		
	}
		
	private IContainer[] getFilteredExistingContainerEntries() {
		if (fCurrJProject == null) {
			return new IContainer[0];
		}
		List res= new ArrayList();
		try {
			IResource container= fWorkspaceRoot.findMember(fCurrJProject.getOutputLocation());
			if (container != null) {
				res.add(container);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}	
		
		for (int i= 0; i < fEntries.length; i++) {
			IClasspathEntry elem= fEntries[i];
			if (elem.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IResource container= fWorkspaceRoot.findMember(elem.getPath());
				if (container != null) {
					res.add(container);
				}
			}		
		}
		return (IContainer[]) res.toArray(new IContainer[res.size()]);
	}	
		
}