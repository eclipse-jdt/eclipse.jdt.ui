/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class NewPackageCreationWizardPage extends ContainerPage {
	
	private static final String PAGE_NAME= "NewPackageCreationWizardPage"; //$NON-NLS-1$
	
	protected static final String PACKAGE= "NewPackageCreationWizardPage.package"; //$NON-NLS-1$
	
	private StringDialogField fPackageDialogField;
	
	/**
	 * Status of last validation of the package field
	 */
	protected IStatus fPackageStatus;
	
	private IPackageFragment fCreatedPackageFragment;
	
	public NewPackageCreationWizardPage(IWorkspaceRoot root) {
		super(PAGE_NAME, root);
		
		setTitle(NewWizardMessages.getString("NewPackageCreationWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewPackageCreationWizardPage.description"));		 //$NON-NLS-1$
		
		fCreatedPackageFragment= null;

		PackageFieldAdapter adapter= new PackageFieldAdapter();
		
		fPackageDialogField= new StringDialogField();
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(NewWizardMessages.getString("NewPackageCreationWizardPage.package.label")); //$NON-NLS-1$
		
		fPackageStatus= new StatusInfo();
	}

	// -------- Initialization ---------


	/**
	 * Should be called from the wizard with the input element.
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= null;
		
		if (selection != null && !selection.isEmpty()) {
			Object selectedElement= selection.getFirstElement();
			if (selectedElement instanceof IAdaptable) {
				IAdaptable adaptable= (IAdaptable) selectedElement;			
				
				jelem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
				if (jelem == null) {
					IResource resource= (IResource) adaptable.getAdapter(IResource.class);
					if (resource != null) {
						IProject proj= resource.getProject();
						if (proj != null) {
							jelem= JavaCore.create(proj);
						}
					}
				}
			}
		}
		if (jelem == null) {
			jelem= EditorUtility.getActiveEditorJavaInput();
		}		
		
		initContainerPage(jelem);
		setPackageText(""); //$NON-NLS-1$
		updateStatus(findMostSevereStatus());
	}
	
	// -------- UI Creation ---------

	/**
	 * @see WizardPage#createControl
	 */	
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		
		int nColumns= 3;
		MGridLayout layout= new MGridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;	
		layout.minimumWidth= 400;
		layout.minimumHeight= 350;
		layout.numColumns= 3;		
		composite.setLayout(layout);
		
		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);
		
		fPackageDialogField.setFocus();
		
		setControl(composite);
		WorkbenchHelp.setHelp(composite, new DialogPageContextComputer(this, IJavaHelpContextIds.NEW_PACKAGE_WIZARD_PAGE));
	}
	
	protected void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, nColumns - 1);
		DialogField.createEmptySpace(composite);		
	}
				
	// -------- PackageFieldAdapter --------

	private class PackageFieldAdapter implements IDialogFieldListener {
		
		// --------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			fPackageStatus= packageChanged();
			// tell all others
			handleFieldChanged(PACKAGE);
		}
	}
		
	// -------- update message ----------------		

	/**
	 * Called when a dialog field on this page changed
	 * @see ContainerPage#fieldUpdated
	 */	
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName == CONTAINER) {
			fPackageStatus= packageChanged();
		}
		// do status line update
		updateStatus(findMostSevereStatus());		
	}	
	
	/**
	 * Finds the most severe error (if there is one)
	 */
	protected IStatus findMostSevereStatus() {
		return StatusUtil.getMoreSevere(fContainerStatus, fPackageStatus);
	}	
		
	// ----------- validation ----------
			
	/**
	 * Verify the input for the package field
	 */
	private IStatus packageChanged() {
		StatusInfo status= new StatusInfo();
		String packName= fPackageDialogField.getText();
		if (!"".equals(packName)) { //$NON-NLS-1$
			IStatus val= JavaConventions.validatePackageName(packName);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(NewWizardMessages.getFormattedString("NewPackageCreationWizardPage.error.InvalidPackageName", val.getMessage())); //$NON-NLS-1$
				return status;
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(NewWizardMessages.getFormattedString("NewPackageCreationWizardPage.warning.DiscouragedPackageName", val.getMessage())); //$NON-NLS-1$
			}
		} else {
			status.setError(NewWizardMessages.getString("NewPackageCreationWizardPage.error.DefaultPackageExists")); //$NON-NLS-1$
			return status;
		}			

		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null) {
			IPackageFragment pack= root.getPackageFragment(packName);
			try {
				IPath rootPath= root.getPath();
				IPath outputPath= root.getJavaProject().getOutputLocation();
				if (rootPath.isPrefixOf(outputPath) && !rootPath.equals(outputPath)) {
					// if the bin folder is inside of our root, dont allow to name a package
					// like the bin folder
					IPath packagePath= pack.getUnderlyingResource().getFullPath();
					if (outputPath.isPrefixOf(packagePath)) {
						status.setError(NewWizardMessages.getString("NewPackageCreationWizardPage.error.IsOutputFolder")); //$NON-NLS-1$
						return status;
					}
				}		
				if (pack.exists()) {
					if (pack.containsJavaResources() || !pack.hasSubpackages()) {
						status.setError(NewWizardMessages.getString("NewPackageCreationWizardPage.error.PackageExists")); //$NON-NLS-1$
					} else {
						status.setWarning(NewWizardMessages.getString("NewPackageCreationWizardPage.warning.PackageNotShown"));  //$NON-NLS-1$
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
			}
			
		}
		return status;
	}
	
	protected String getPackageText() {
		return fPackageDialogField.getText();
	}
	
	protected void setPackageText(String str) {
		fPackageDialogField.setText(str);
	}
	
		
	// ---- creation ----------------

	/**
	 * @see NewElementWizardPage#getRunnable
	 */		
	public IRunnableWithProgress getRunnable() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					createPackage(monitor);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 
			}
		};
	}
	
	public IPackageFragment getNewPackageFragment() {
		return fCreatedPackageFragment;
	}
	
	protected void createPackage(IProgressMonitor monitor) throws JavaModelException, CoreException, InterruptedException {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		String packName= getPackageText();
		fCreatedPackageFragment= root.createPackageFragment(packName, true, monitor);
	}	
}