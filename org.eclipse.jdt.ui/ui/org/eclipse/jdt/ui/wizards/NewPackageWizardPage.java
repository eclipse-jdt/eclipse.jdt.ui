/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * Wizard page for a new class. This class is not intended to be subclassed.
 * To implement a different new package wizard, extend <code>ContainerPage</code>.
 * @since 2.0
 */
public class NewPackageWizardPage extends NewContainerWizardPage {
	
	private static final String PAGE_NAME= "NewPackageWizardPage"; //$NON-NLS-1$
	
	private static final String PACKAGE= "NewPackageWizardPage.package"; //$NON-NLS-1$
	
	private StringDialogField fPackageDialogField;
	
	/*
	 * Status of last validation of the package field
	 */
	private IStatus fPackageStatus;
	
	private IPackageFragment fCreatedPackageFragment;
	
	public NewPackageWizardPage() {
		super(PAGE_NAME);
		
		setTitle(NewWizardMessages.getString("NewPackageWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewPackageWizardPage.description"));		 //$NON-NLS-1$
		
		fCreatedPackageFragment= null;

		PackageFieldAdapter adapter= new PackageFieldAdapter();
		
		fPackageDialogField= new StringDialogField();
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(NewWizardMessages.getString("NewPackageWizardPage.package.label")); //$NON-NLS-1$
		
		fPackageStatus= new StatusInfo();
	}

	// -------- Initialization ---------


	/**
	 * Should be called from the wizard with the input element.
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);	
		
		initContainerPage(jelem);
		setPackageText("", true); //$NON-NLS-1$
		updateStatus(new IStatus[] { fContainerStatus, fPackageStatus });		
	}
	
	// -------- UI Creation ---------

	/*
	 * @see WizardPage#createControl
	 */	
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
			
		Composite composite= new Composite(parent, SWT.NONE);
		
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		layout.numColumns= 3;		
		composite.setLayout(layout);
		
		Label label= new Label(composite, SWT.WRAP);
		label.setText(NewWizardMessages.getString("NewPackageWizardPage.info")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.widthHint= convertWidthInCharsToPixels(80);
		gd.horizontalSpan= 3;
		label.setLayoutData(gd);
		
		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);
		
		setControl(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.NEW_PACKAGE_WIZARD_PAGE);
	}
	
	/*
	 * @see WizardPage#becomesVisible
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			setFocus();
		}
		super.setVisible(visible);
	}
	
	/**
	 * Sets the focus on the package name.
	 */		
	protected void setFocus() {
		fPackageDialogField.setFocus();
	}
	

	private void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, nColumns - 1);
		LayoutUtil.setWidthHint(fPackageDialogField.getTextControl(null), getMaxFieldWidth());
		LayoutUtil.setHorizontalGrabbing(fPackageDialogField.getTextControl(null));
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
	 * Called when a dialog field on this page changed.
	 * @see NewContainerCreationPage#fieldUpdated
	 */	
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName == CONTAINER) {
			fPackageStatus= packageChanged();
		}
		// do status line update
		updateStatus(new IStatus[] { fContainerStatus, fPackageStatus });		
	}	
			
	// ----------- validation ----------
			
	/**
	 * Verifies the input for the package field.
	 */
	private IStatus packageChanged() {
		StatusInfo status= new StatusInfo();
		String packName= getPackageText();
		if (packName.length() > 0) {
			IStatus val= JavaConventions.validatePackageName(packName);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(NewWizardMessages.getFormattedString("NewPackageWizardPage.error.InvalidPackageName", val.getMessage())); //$NON-NLS-1$
				return status;
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(NewWizardMessages.getFormattedString("NewPackageWizardPage.warning.DiscouragedPackageName", val.getMessage())); //$NON-NLS-1$
			}
		} else {
			status.setError(NewWizardMessages.getString("NewPackageWizardPage.error.EnterName")); //$NON-NLS-1$
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
						status.setError(NewWizardMessages.getString("NewPackageWizardPage.error.IsOutputFolder")); //$NON-NLS-1$
						return status;
					}
				}		
				if (pack.exists()) {
					if (pack.containsJavaResources() || !pack.hasSubpackages()) {
						status.setError(NewWizardMessages.getString("NewPackageWizardPage.error.PackageExists")); //$NON-NLS-1$
					} else {
						status.setWarning(NewWizardMessages.getString("NewPackageWizardPage.warning.PackageNotShown"));  //$NON-NLS-1$
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			
		}
		return status;
	}

	/*
	 * Returns the content of the package field.
	 */
	public String getPackageText() {
		return fPackageDialogField.getText();
	}

	/*
	 * Sets the content of the package text.
	 */	
	public void setPackageText(String str, boolean canBeModified) {
		fPackageDialogField.setText(str);
		
		fPackageDialogField.setEnabled(canBeModified);
	}
	
		
	// ---- creation ----------------

	/**
	 * Returns a runnable that creates a package using the current settings.
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

	/**
	 * Returns the created package fragment. Only valid after creation has been performed.
	 */	
	public IPackageFragment getNewPackageFragment() {
		return fCreatedPackageFragment;
	}
	
	private void createPackage(IProgressMonitor monitor) throws CoreException, InterruptedException {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		String packName= getPackageText();
		fCreatedPackageFragment= root.createPackageFragment(packName, true, monitor);
	}	
}