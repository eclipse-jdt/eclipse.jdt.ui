/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.help.DialogPageContextComputer;
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

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class NewSourceFolderCreationWizardPage extends NewElementWizardPage {
		
	private static final String PAGE_NAME= "NewSourceFolderCreationWizardPage"; //$NON-NLS-1$

	private StringButtonDialogField fProjectField;
	private StatusInfo fProjectStatus;
	
	private StringButtonDialogField fRootDialogField;
	private StatusInfo fRootStatus;
	
	private SelectionButtonDialogField fEditClassPathField;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	private IJavaProject fCurrJProject;
	private IClasspathEntry[] fEntries;
	private IPath fOutputLocation;
	
	private IPackageFragmentRoot fCreatedRoot;
	
	public NewSourceFolderCreationWizardPage(IWorkspaceRoot root) {
		super(PAGE_NAME);
		
		setTitle(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.description"));		 //$NON-NLS-1$
		
		fWorkspaceRoot= root;
		
		RootFieldAdapter adapter= new RootFieldAdapter();
		
		fProjectField= new StringButtonDialogField(adapter);
		fProjectField.setDialogFieldListener(adapter);
		fProjectField.setLabelText(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.project.label")); //$NON-NLS-1$
		fProjectField.setButtonLabel(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.project.button"));	 //$NON-NLS-1$
		
		fRootDialogField= new StringButtonDialogField(adapter);
		fRootDialogField.setDialogFieldListener(adapter);
		fRootDialogField.setLabelText(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.root.label")); //$NON-NLS-1$
		fRootDialogField.setButtonLabel(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.root.button")); //$NON-NLS-1$
		
		fEditClassPathField= new SelectionButtonDialogField(SWT.PUSH);
		fEditClassPathField.setDialogFieldListener(adapter);
		fEditClassPathField.setLabelText(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.editclasspath.button"));		 //$NON-NLS-1$
		
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

	/**
	 * @see WizardPage#createControl
	 */	
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
			
		MGridLayout layout= new MGridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;	
		layout.minimumWidth= 400;
		layout.minimumHeight= 350;
		layout.numColumns= 3;
		composite.setLayout(layout);
		
		fProjectField.doFillIntoGrid(composite, 3);		
		fRootDialogField.doFillIntoGrid(composite, 3);
		fRootDialogField.setFocus();
			
		(new Separator()).doFillIntoGrid(composite, 3);
		
		fEditClassPathField.doFillIntoGrid(composite, 3);
		Control control= fEditClassPathField.getSelectionButton(null);
		MGridData gd= (MGridData) control.getLayoutData();
		gd.verticalAlignment= MGridData.END;
		gd.horizontalAlignment= MGridData.BEGINNING;		
		setControl(composite);
		WorkbenchHelp.setHelp(composite, new DialogPageContextComputer(this, IJavaHelpContextIds.NEW_PACKAGEROOT_WIZARD_PAGE));		
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
			IFolder folder= chooseFolder();
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
		} else if (field == fEditClassPathField) {
			if (showClassPathPropertyPage()) {
				updateProjectStatus();
				updateRootStatus();
			}
		}
		updateStatus(findMostSevereStatus());
	}
	
	
	private void updateProjectStatus() {
		fCurrJProject= null;
		
		String str= fProjectField.getText();
		if ("".equals(str)) { //$NON-NLS-1$
			fProjectStatus.setError(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.error.EnterProjectName")); //$NON-NLS-1$
			return;
		}
		IPath path= new Path(str);
		if (path.segmentCount() != 1) {
			fProjectStatus.setError(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.error.InvalidProjectPath")); //$NON-NLS-1$
			return;
		}
		IProject project= fWorkspaceRoot.getProject(path.toString());
		if (!project.exists()) {
			fProjectStatus.setError(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.error.ProjectNotExists")); //$NON-NLS-1$
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
		fProjectStatus.setError(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.error.NotAJavaProject")); //$NON-NLS-1$
	}

	
	
	private void updateRootStatus() {
		fRootDialogField.enableButton(fCurrJProject != null);
		if (fCurrJProject == null) {
			return;
		}
		String str= fRootDialogField.getText();
		if (str.length() == 0) {
			fRootStatus.setError(NewWizardMessages.getFormattedString("NewSourceFolderCreationWizardPage.error.EnterRootName", fCurrJProject.getProject().getFullPath().toString())); //$NON-NLS-1$
		} else {
			IPath path= fCurrJProject.getProject().getFullPath().append(str);
			if (!fWorkspaceRoot.getWorkspace().validatePath(path.toString(), IResource.FOLDER).isOK()) {
				fRootStatus.setError(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.error.InvalidRootName")); //$NON-NLS-1$
			} else {
				IResource res= fWorkspaceRoot.findMember(path);
				if (res != null) {
					if (res.getType() != IResource.FOLDER) {
						fRootStatus.setError(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.error.NotAFolder")); //$NON-NLS-1$
						return;
					}
				}
				IClasspathEntry[] newEntries= new IClasspathEntry[fEntries.length + 1];
				for (int i= 0; i < fEntries.length; i++) {
					IClasspathEntry curr= fEntries[i];
					if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if (path.equals(curr.getPath())) {
							fRootStatus.setError(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.error.AlreadyExisting")); //$NON-NLS-1$
							return;
						}
					}
					newEntries[i]= curr;
				}
				newEntries[fEntries.length]= JavaCore.newSourceEntry(path);
								
				IStatus status= JavaConventions.validateClasspath(fCurrJProject, newEntries, fOutputLocation);
				if (!status.isOK()) {
					fRootStatus.setError(status.getMessage());
					return;
				}
				fRootStatus.setOK();
			}
		}
	}	
				
	protected IStatus findMostSevereStatus() {
		return StatusUtil.getMoreSevere(fProjectStatus, fRootStatus);
	}
	
	// ---- creation ----------------
	/** 
	 * @see NewElementWizardPage#getRunnable
	 */	
	public IRunnableWithProgress getRunnable() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					fCreatedRoot= createPackageFragmentRoot(monitor, getShell());
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 
			}
		};
	}
	
	protected IPackageFragmentRoot getNewPackageFragmentRoot() {
		return fCreatedRoot;
	}
	

	
	protected IPackageFragmentRoot createPackageFragmentRoot(IProgressMonitor monitor, Shell shell) throws CoreException, JavaModelException {
		String relPath= fRootDialogField.getText();
			
		IFolder folder= fCurrJProject.getProject().getFolder(relPath);
		IPath path= folder.getFullPath();
		if (!folder.exists()) {
			CoreUtility.createFolder(folder, true, true, monitor);			
		}
		
		IClasspathEntry[] entries= fCurrJProject.getRawClasspath();
		int nEntries= entries.length;
		IClasspathEntry[] newEntries= new IClasspathEntry[nEntries + 1];
		for (int i= 0; i < nEntries; i++) {
			newEntries[i]= entries[i];
		}
		newEntries[nEntries]= JavaCore.newSourceEntry(path);
		fCurrJProject.setRawClasspath(newEntries, monitor);
		
		return fCurrJProject.getPackageFragmentRoot(folder);
	}
	
	// ------------- choose dialogs
	
	private IFolder chooseFolder() {	
		Class[] acceptedClasses= new Class[] { IFolder.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		Object[] notWanted= getFilteredExistingContainerEntries();
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, notWanted);	
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.ChooseExistingRootDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.ChooseExistingRootDialog.description")); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(fCurrJProject.getProject());
		IResource res= fWorkspaceRoot.findMember(new Path(fRootDialogField.getText()));
		if (res != null) {
			dialog.setInitialSelection(res);
		}

		if (dialog.open() == dialog.OK) {
			return (IFolder) dialog.getFirstResult();
		}			
		return null;		
	}

	private IJavaProject chooseProject() {
		IJavaProject[] projects;
		try {
			projects= JavaCore.create(fWorkspaceRoot).getJavaProjects();
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
			projects= new IJavaProject[0];
		}
		
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.ChooseProjectDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.ChooseProjectDialog.description")); //$NON-NLS-1$
		dialog.setElements(projects);
		dialog.setInitialSelections(new Object[] { fCurrJProject });
		if (dialog.open() == dialog.OK) {			
			return (IJavaProject) dialog.getFirstResult();
		}			
		return null;		
	}
	
	
	// a dialog containing the class path dialog
	private class EditClassPathDialog extends StatusDialog implements IStatusChangeListener {
		
		private BuildPathsBlock fBuildPathsBlock;
		
		public EditClassPathDialog(Shell parent) {
			super(parent);
			fBuildPathsBlock= new BuildPathsBlock(fWorkspaceRoot, this, false);	
		}
				
		public void create() {
			super.create();
			fBuildPathsBlock.init(fCurrJProject, null, null);
		}		
				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);			
			Control inner= fBuildPathsBlock.createControl(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			return composite;
		}		
		
		
		public void statusChanged(IStatus status) {
			updateStatus(status);
		}
		
		protected void buttonPressed(int buttonId) {
			if (buttonId == IDialogConstants.OK_ID) {
				IRunnableWithProgress runnable= fBuildPathsBlock.getRunnable();
				if (invokeRunnable(runnable)) {
					setReturnCode(OK);
				} else {
					setReturnCode(CANCEL);
				}
			}
			close();
		}
		
		private boolean invokeRunnable(IRunnableWithProgress runnable) {
			IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(runnable);
			try {
				getWizard().getContainer().run(false, true, op);
			} catch (InvocationTargetException e) {
				Shell shell= getShell();
				String title= NewWizardMessages.getString("NewSourceFolderCreationWizardPage.op_error.title"); //$NON-NLS-1$
				String message= NewWizardMessages.getString("NewSourceFolderCreationWizardPage.op_error.message");				 //$NON-NLS-1$
				ExceptionHandler.handle(e, shell, title, message);
				return false;
			} catch  (InterruptedException e) {
				return false;
			}
			return true;
		}		
	}
	
	private boolean showClassPathPropertyPage() {
		EditClassPathDialog dialog= new EditClassPathDialog(getShell());
		dialog.setTitle(NewWizardMessages.getString("NewSourceFolderCreationWizardPage.EditClassPathDialog.title")); //$NON-NLS-1$
		return (dialog.open() == EditClassPathDialog.OK);
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
			JavaPlugin.log(e.getStatus());
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