/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.IStatusInfoChangeListener;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.packageview.PackageViewerSorter;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class NewPackageRootCreationWizardPage extends NewElementWizardPage {
		
	private static final String PAGE_NAME= "NewPackageRootCreationWizardPage";
	
	private static final String ROOT= PAGE_NAME + ".root";
	private static final String PROJECT= PAGE_NAME + ".project";
	private static final String EDITCP= PAGE_NAME + ".editclasspath";
		
	private static final String ROOT_DIALOG= PAGE_NAME + ".ChooseExistingRootDialog";
	private static final String PROJECT_DIALOG= PAGE_NAME + ".ChooseProjectDialog";
	private static final String EDITCP_DIALOG= PAGE_NAME + ".EditClassPathDialog";

	private static final String ERROR_ROOT_ENTERNAME= PAGE_NAME + ".error.EnterRootName";
	private static final String ERROR_ROOT_INVALIDNAME= PAGE_NAME + ".error.InvalidRootName";
	private static final String ERROR_ROOT_NOFOLDER= PAGE_NAME + ".error.NotAFolder";
	private static final String ERROR_ROOT_ALREADYEXISTS= PAGE_NAME + ".error.AlreadyExisting";
	private static final String ERROR_ROOT_WILLOVERLAP= PAGE_NAME + ".error.WillOverlap";

	private static final String ERROR_PROJECT_ENTERNAME= PAGE_NAME + ".error.EnterProjectName";
	private static final String ERROR_PROJECT_INVALIDNAME= PAGE_NAME + ".error.InvalidProjectPath";
	private static final String ERROR_PROJECT_NOJAVAPROJECT= PAGE_NAME + ".error.NotAJavaProject";
	private static final String ERROR_PROJECT_PROJECTNOTEXISTS= PAGE_NAME + ".error.ProjectNotExists";

	private StringButtonDialogField fProjectField;
	private StatusInfo fProjectStatus;
	
	private StringButtonDialogField fRootDialogField;
	private StatusInfo fRootStatus;
	
	private SelectionButtonDialogField fEditClassPathField;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	private IJavaProject fCurrJProject;
	private IClasspathEntry[] fEntries;
	
	private IPackageFragmentRoot fCreatedRoot;
	
	public NewPackageRootCreationWizardPage(IWorkspaceRoot root) {
		super(PAGE_NAME, JavaPlugin.getResourceBundle());
		fWorkspaceRoot= root;
		
		RootFieldAdapter adapter= new RootFieldAdapter();
		
		fProjectField= new StringButtonDialogField(adapter);
		fProjectField.setDialogFieldListener(adapter);
		fProjectField.setLabelText(getResourceString(PROJECT + ".label"));
		fProjectField.setButtonLabel(getResourceString(PROJECT + ".button"));	
		
		fRootDialogField= new StringButtonDialogField(adapter);
		fRootDialogField.setDialogFieldListener(adapter);
		fRootDialogField.setLabelText(getResourceString(ROOT + ".label"));
		fRootDialogField.setButtonLabel(getResourceString(ROOT + ".button"));
		
		fEditClassPathField= new SelectionButtonDialogField(SWT.PUSH);
		fEditClassPathField.setDialogFieldListener(adapter);
		fEditClassPathField.setLabelText(getResourceString(EDITCP + ".button"));		
		
		fRootStatus= new StatusInfo();
		fProjectStatus= new StatusInfo();
	}
			
	// -------- Initialization ---------
		
	public final void init(IStructuredSelection selection) {
		if (selection == null || selection.isEmpty()) {
			setDefaultAttributes();
			return;
		}
		
		Object selectedElement= selection.getFirstElement();
		
		String projPath= null;
		
		if (selectedElement instanceof IResource) {
			IProject proj= ((IResource)selectedElement).getProject();
			if (proj != null) {
				projPath= proj.getFullPath().toString();
			}	
		} else if (selectedElement instanceof IJavaElement) {
			IJavaProject jproject= ((IJavaElement)selectedElement).getJavaProject();
			if (jproject != null) {
				projPath= jproject.getProject().getFullPath().toString();
			}
		}
		
		if (projPath != null) {
			fProjectField.setText(projPath);
			fRootDialogField.setText("");
		} else {
			setDefaultAttributes();
		}
	}
	
	private void setDefaultAttributes() {
		String projPath= "";
		
		try {
			// find the first java project
			IProject[] projects= fWorkspaceRoot.getProjects();
			for (int i= 0; i < projects.length; i++) {
				IProject proj= projects[i];
				if (proj.hasNature(JavaCore.NATURE_ID)) {
					projPath= proj.getFullPath().toString();
					break;
				}
			}					
		} catch (CoreException e) {
			// ignore here
		}
		fProjectField.setText(projPath);
		fRootDialogField.setText("");		
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
	}	
		
	// -------- ContainerFieldAdapter --------

	private class RootFieldAdapter implements IStringButtonAdapter, IDialogFieldListener {

		// -------- IStringButtonAdapter
		
		public void changeControlPressed(DialogField field) {
			if (field == fRootDialogField) {
				IFolder folder= chooseFolder();
				if (folder != null) {
					IPath path= folder.getFullPath().removeFirstSegments(1);
					fRootDialogField.setText(path.toString());
				}
			} else if (field == fProjectField) {
				IJavaProject jproject= chooseProject();
				if (jproject != null) {
					IPath path= jproject.getProject().getFullPath();
					fProjectField.setText(path.toString());
				}
			}
	
		}
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
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
	}
	
	
	private void updateProjectStatus() {
		fCurrJProject= null;
		
		String str= fProjectField.getText();
		if ("".equals(str)) {
			fProjectStatus.setError(getResourceString(ERROR_PROJECT_ENTERNAME));
			return;
		}
		if (!fWorkspaceRoot.getWorkspace().validatePath(str, IResource.PROJECT).isOK()) {
			fProjectStatus.setError(getResourceString(ERROR_PROJECT_INVALIDNAME));
			return;
		}
		IPath path= new Path(str);
		IProject project= fWorkspaceRoot.getProject(path.toString());
		if (!project.exists()) {
			fProjectStatus.setError(getResourceString(ERROR_PROJECT_PROJECTNOTEXISTS));
			return;
		}
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				fCurrJProject= (IJavaProject)project.getNature(JavaCore.NATURE_ID);
				fEntries= fCurrJProject.getClasspath();
				fProjectStatus.setOK();
				return;
			}
		} catch (CoreException e) {
			// go to error
			fCurrJProject= null;
		}	
		fProjectStatus.setError(getResourceString(ERROR_PROJECT_NOJAVAPROJECT));
	}

	
	
	private void updateRootStatus() {
		fRootDialogField.enableButton(fCurrJProject != null);
		if (fCurrJProject == null) {
			return;
		}
		String str= fRootDialogField.getText();
		if ("".equals(str)) {
			fRootStatus.setError(getFormattedString(ERROR_ROOT_ENTERNAME, fCurrJProject.getProject().getFullPath().toString()));
		} else {
			IPath path= fCurrJProject.getProject().getFullPath().append(str);
			if (!fWorkspaceRoot.getWorkspace().validatePath(path.toString(), IResource.FOLDER).isOK()) {
				fRootStatus.setError(getResourceString(ERROR_ROOT_INVALIDNAME));
			} else {
				IResource res= fWorkspaceRoot.findMember(path);
				if (res != null) {
					if (res.getType() != IResource.FOLDER) {
						fRootStatus.setError(getResourceString(ERROR_ROOT_NOFOLDER));
						return;
					}
				}
				for (int i= 0; i < fEntries.length; i++) {
					IClasspathEntry curr= fEntries[i];
					if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						IPath entryPath= curr.getPath();
						
						if (path.equals(entryPath)) {
							fRootStatus.setError(getResourceString(ERROR_ROOT_ALREADYEXISTS));
							return;
						}
						if (JavaConventions.isOverlappingRoots(path, entryPath)) {
							fRootStatus.setError(getFormattedString(ERROR_ROOT_WILLOVERLAP, entryPath.toString()));
							return;
						}
						
					}
				}
				fRootStatus.setOK();
			}
		}
	}	
				
	protected StatusInfo findMostSevereStatus() {
		return fProjectStatus.getMoreSevere(fRootStatus);
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
		
		IClasspathEntry[] entries= fCurrJProject.getClasspath();
		int nEntries= entries.length;
		IClasspathEntry[] newEntries= new IClasspathEntry[nEntries + 1];
		for (int i= 0; i < nEntries; i++) {
			newEntries[i]= entries[i];
		}
		newEntries[nEntries]= fCurrJProject.newSourceEntry(path);
		fCurrJProject.setClasspath(newEntries, monitor);
		
		return fCurrJProject.getPackageFragmentRoot(folder);
	}
	
	// ------------- choose dialogs
	
	private IFolder chooseFolder() {	
		Class[] acceptedClasses= new Class[] { IFolder.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		List notWanted= getFilteredExistingContainerEntries();
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, notWanted);	
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(getResourceString(ROOT_DIALOG + ".title"));
		dialog.setMessage(getResourceString(ROOT_DIALOG + ".description"));
		dialog.addFilter(filter);
		
		IProject proj= fCurrJProject.getProject();
		if (dialog.open(proj, null) == dialog.OK) {
			return (IFolder) dialog.getPrimaryResult();
		}			
		return null;		
	}

	private IJavaProject chooseProject() {	
		Class[] acceptedClasses= new Class[] { IJavaProject.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		ViewerFilter filter= new TypedViewerFilter(new Class[] { IJavaModel.class, IJavaProject.class });	
		
		JavaElementContentProvider contentProvider= new JavaElementContentProvider();
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), labelProvider, contentProvider);
		dialog.setValidator(validator);
		dialog.setSorter(new PackageViewerSorter());
		dialog.setTitle(getResourceString(PROJECT_DIALOG + ".title"));
		dialog.setMessage(getResourceString(PROJECT_DIALOG + ".description"));
		dialog.addFilter(filter);
		
		Object javaModel= JavaCore.create(fWorkspaceRoot);
		
		if (dialog.open(javaModel, fCurrJProject) == dialog.OK) {			
			return (IJavaProject) dialog.getPrimaryResult();
		}			
		return null;		
	}
	
	
	// a dialog containing the class path dialog
	private class EditClassPathDialog extends StatusDialog implements IStatusInfoChangeListener {
		
		private BuildPathsBlock fBuildPathsBlock;
		
		public EditClassPathDialog(Shell parent) {
			super(parent);
			fBuildPathsBlock= new BuildPathsBlock(fWorkspaceRoot, this, false);	
		}
				
		public void create() {
			super.create();
			fBuildPathsBlock.init(fCurrJProject.getProject(), false);
		}		
				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);			
			Control inner= fBuildPathsBlock.createControl(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			return composite;
		}		
		
		
		public void statusInfoChanged(StatusInfo status) {
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
	}
	
	private boolean showClassPathPropertyPage() {
		EditClassPathDialog dialog= new EditClassPathDialog(getShell());
		dialog.setTitle(getResourceString(EDITCP_DIALOG + ".title"));
		return (dialog.open() == EditClassPathDialog.OK);
	}

	
	private List getFilteredExistingContainerEntries() {
		List res= new ArrayList();
		if (fCurrJProject == null) {
			return res;
		}
		
		try {
			IFolder folder= fWorkspaceRoot.getFolder(fCurrJProject.getOutputLocation());
			res.add(folder);
		} catch (JavaModelException e) {
			// ignore it here
		}	
		
		for (int i= 0; i < fEntries.length; i++) {
			IClasspathEntry elem= fEntries[i];
			if (elem.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				// beware of projects in the path
				// 1G472TV: ITPJUI:WINNT - Walkback in NewSourceContainerWizard
				if (elem.getPath().segmentCount() > 1) {
					IFolder folder= fWorkspaceRoot.getFolder(elem.getPath());
					res.add(folder);
				}
			}		
		}
		return res;
	}	
		
}