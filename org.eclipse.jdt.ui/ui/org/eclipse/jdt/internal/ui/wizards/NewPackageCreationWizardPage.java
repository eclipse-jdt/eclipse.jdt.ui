/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaConventions;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class NewPackageCreationWizardPage extends ContainerPage {
	
	private static final String PAGE_NAME= "NewPackageCreationWizardPage";
	
	protected static final String PACKAGE= "NewPackageCreationWizardPage.package";
	
	private static final String PK_INVALIDPACK_ERROR= "NewPackageCreationWizardPage.error.InvalidPackageName";
	private static final String PK_INVALIDPACK_WARNING= "NewPackageCreationWizardPage.warning.DiscouragedPackageName";
	private static final String PK_PACKEXISTS_WARNING= "NewPackageCreationWizardPage.warning.PackageExists";
	private static final String PK_ISBINFOLDER_ERROR= "NewPackageCreationWizardPage.error.IsOutputFolder";

	private StringDialogField fPackageDialogField;
	
	/**
	 * Status of last validation of the package field
	 */
	protected IStatus fPackageStatus;
	
	private IPackageFragment fCreatedPackageFragment;
	
	public NewPackageCreationWizardPage(IWorkspaceRoot root) {
		super(PAGE_NAME, root);
		fCreatedPackageFragment= null;

		PackageFieldAdapter adapter= new PackageFieldAdapter();
		
		fPackageDialogField= new StringDialogField();
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(getResourceString(PACKAGE + ".label"));
		
		fPackageStatus= new StatusInfo();
	}

	// -------- Initialization ---------


	/**
	 * Should be called from the wizard with the input element.
	 */
	public final void init(IStructuredSelection selection) {
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
		initContainerPage(jelem);
		setPackageText("");
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
		return StatusTool.getMoreSevere(fContainerStatus, fPackageStatus);
	}	
		
	// ----------- validation ----------
			
	/**
	 * Verify the input for the package field
	 */
	private IStatus packageChanged() {
		StatusInfo status= new StatusInfo();
		String packName= fPackageDialogField.getText();
		if (!"".equals(packName)) {
			IStatus val= JavaConventions.validatePackageName(packName);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(getFormattedString(PK_INVALIDPACK_ERROR, val.getMessage()));
				return status;
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(getFormattedString(PK_INVALIDPACK_WARNING, val.getMessage()));
			}
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
						status.setError(getResourceString(PK_ISBINFOLDER_ERROR));
						return status;
					}
				}
			} catch (JavaModelException e) {
				// show 'not exist' error message
			}			

			if (pack.exists()) {
				status.setWarning(getResourceString(PK_PACKEXISTS_WARNING));
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