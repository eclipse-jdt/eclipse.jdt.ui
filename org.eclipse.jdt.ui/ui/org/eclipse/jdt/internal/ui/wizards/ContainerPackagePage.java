/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonStatusDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridUtil;

public abstract class ContainerPackagePage extends ContainerPage {
	
	private static final String PAGE_NAME= "ContainerPackagePage";
		
	protected static final String PACKAGE= PAGE_NAME + ".package";	
	protected static final String ENCLOSING= PAGE_NAME + ".enclosing";
	protected static final String ENCLOSINGSELECTION= ENCLOSING + ".selection";
	
	private static final String STATUS_DEFAULT= PAGE_NAME + ".default";
	
	private static final String ERROR_PACKAGE_INVALIDNAME= PAGE_NAME + ".error.InvalidPackageName";
	private static final String ERROR_PACKAGE_CLASHOUTPUTLOCATION= PAGE_NAME + ".error.ClashOutputLocation";
	private static final String WARNING_PACKAGE_DISCOURAGEDNAME= PAGE_NAME + ".warning.DiscouragedPackageName";
	
	private static final String ERROR_ENCLOSING_ENTERNAME= PAGE_NAME + ".error.EnclosingTypeEnterName";
	private static final String ERROR_ENCLOSING_NOTEXISTS= PAGE_NAME + ".error.EnclosingTypeNotExists";
	private static final String ERROR_ENCLOSING_PARENTISBINARY= PAGE_NAME + ".error.EnclosingNotInCU";

	private static final String PACKAGE_DIALOG= PAGE_NAME + ".ChoosePackageDialog";
	private static final String ENCLOSING_DIALOG= PAGE_NAME + ".ChooseEnclosingTypeDialog";
	
	
	private StringButtonStatusDialogField fPackageDialogField;
	private StatusInfo fPackageStatus;
	
	private SelectionButtonDialogField fEnclosingTypeSelection;
	private StringButtonDialogField fEnclosingTypeDialogField;
	
	private StatusInfo fEnclosingTypeStatus;
	
	private boolean fCanModifyPackage;
	private boolean fCanModifyEnclosingType;
	
	private IPackageFragment fCurrPackage;
	private IType fCurrEnclosingType;
	
	public ContainerPackagePage(String name, IWorkspaceRoot root) {
		super(name, root);
		
		PackageFieldAdapter adapter= new PackageFieldAdapter();
						
		fPackageDialogField= new StringButtonStatusDialogField(adapter);
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(getResourceString(PACKAGE + ".label"));
		fPackageDialogField.setButtonLabel(getResourceString(PACKAGE + ".button"));
		fPackageDialogField.setStatusWidthHint(getResourceString(STATUS_DEFAULT));
				
		fEnclosingTypeSelection= new SelectionButtonDialogField(SWT.CHECK);
		fEnclosingTypeSelection.setDialogFieldListener(adapter);
		fEnclosingTypeSelection.setLabelText(getResourceString(ENCLOSINGSELECTION + ".label"));
		
		fEnclosingTypeDialogField= new StringButtonDialogField(adapter);
		fEnclosingTypeDialogField.setDialogFieldListener(adapter);
		fEnclosingTypeDialogField.setLabelText(getResourceString(ENCLOSING + ".label"));
		fEnclosingTypeDialogField.setButtonLabel(getResourceString(ENCLOSING + ".button"));
		
		fPackageStatus= new StatusInfo();
		fEnclosingTypeStatus= new StatusInfo();
		
		fCanModifyPackage= true;
		fCanModifyEnclosingType= true;
		updateEnableState();
	}
		
	// -------- initialization ---------
	
	/**
	 * Initialize all fields for the given java element as input
	 * @see ContainerPage#initFields
	 */	
	protected void initFields(IJavaElement selection) {
		super.initFields(selection);
		IPackageFragment pack= (IPackageFragment) JavaModelUtility.findElementOfKind(selection, IJavaElement.PACKAGE_FRAGMENT);		
		if (pack != null) {
			fPackageDialogField.setText(pack.getElementName());
		} else {
			fPackageDialogField.setText("");
		}
			
		fCurrEnclosingType= null;
		fEnclosingTypeDialogField.setText("");
		fEnclosingTypeSelection.setSelection(false);
	}
	
	/**
	 * Called when default attributes have to be set.
	 * @see ContainerPage#setDefaultAttributes
	 */
	protected void setDefaultAttributes() {
		super.setDefaultAttributes();
		fCurrEnclosingType= null;
		fEnclosingTypeDialogField.setText("");
		
		fCurrPackage= null;
		fPackageDialogField.setText("");
		fEnclosingTypeSelection.setSelection(false);
	}
	
	// ------- UI ----------
	
	protected void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, 4);
	}
	
	protected void createEnclosingTypeControls(Composite composite, int nColumns) {
		fEnclosingTypeSelection.doFillIntoGrid(composite, 1);
		
		Control c= fEnclosingTypeDialogField.getTextControl(composite);
		c.setLayoutData(MGridUtil.createHorizontalFill());
		LayoutUtil.setHorizontalSpan(c, 2);
		c= fEnclosingTypeDialogField.getChangeControl(composite);
		c.setLayoutData(new MGridData(MGridData.HORIZONTAL_ALIGN_FILL));
	}
	
	// -------- PackageFieldAdapter --------

	private class PackageFieldAdapter implements IStringButtonAdapter, IDialogFieldListener {
		
		//  -------- IStringButtonAdapter
	
		public void changeControlPressed(DialogField field) {
			if (field == fPackageDialogField) {
				IPackageFragment pack= choosePackage();	
				if (pack != null) {
					fPackageDialogField.setText(pack.getElementName());
				}
			} else { // (field == fEnclosingTypeDialogField)
				IType type= chooseEnclosingType();
				if (type != null) {
					fEnclosingTypeDialogField.setText(JavaModelUtility.getFullyQualifiedName(type));
				}
			}
				
		}
		
		//  -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			String fieldName= null;
			if (field == fPackageDialogField) {
				updatePackageStatus();
				updatePackageStatusLabel();
				fieldName= PACKAGE;
			} else if (field == fEnclosingTypeDialogField) {
				updateEnclosingTypeStatus();
				fieldName= ENCLOSING;
			} else if (field == fEnclosingTypeSelection) {
				updateEnableState();
				fieldName= ENCLOSINGSELECTION;
			}		
			// tell all others
			fieldUpdated(fieldName);
		}
	}	
	
	// -------- update message ----------------
	
	/**
	 * Called when a dialog field on this page changed
	 * @see ContainerPage#fieldUpdated
	 * Overridden (extended) by sub types	 
	 */		
	protected void fieldUpdated(String fieldName) {
		super.fieldUpdated(fieldName);
		if (fieldName == CONTAINER) {
			updatePackageStatus();
			updateEnclosingTypeStatus();
		}
	}
		
	// ----------- validation ----------
	
	protected StatusInfo getPackageStatus() {
		return fPackageStatus;
	}
	
	protected StatusInfo getEnclosingTypeStatus() {
		if (isEnclosingTypeSelected()) {
			return fEnclosingTypeStatus;
		} else {
			return null;
		}
	}	
		
	/**
	 * Verify if the package input field is valid
	 */
	private void updatePackageStatus() {
		fPackageDialogField.enableButton(getPackageFragmentRoot() != null);
		fPackageStatus.setOK();
		
		String packName= fPackageDialogField.getText();
		if (!"".equals(packName)) {
			IStatus val= JavaConventions.validatePackageName(packName);
			if (val.getSeverity() == IStatus.ERROR) {
				fPackageStatus.setError(getFormattedString(ERROR_PACKAGE_INVALIDNAME, val.getMessage()));
				return;
			} else if (val.getSeverity() == IStatus.WARNING) {
				fPackageStatus.setWarning(getFormattedString(WARNING_PACKAGE_DISCOURAGEDNAME, val.getMessage()));
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
						fPackageStatus.setError(getResourceString(ERROR_PACKAGE_CLASHOUTPUTLOCATION));
						return;
					}
				}
			} catch (JavaModelException e) {
			}
			
			fCurrPackage= pack;
		} else {
			fPackageStatus.setError("root undef");
		}
	}

	/**
	 * Update the 'default' label next to the package field
	 */	
	private void updatePackageStatusLabel() {
		String packName= fPackageDialogField.getText();
		
		if ("".equals(packName)) {
			fPackageDialogField.setStatus(getResourceString(STATUS_DEFAULT));
		} else {
			fPackageDialogField.setStatus("");
		}
	}
	
	private void updateEnableState() {
		boolean enclosing= isEnclosingTypeSelected();
		fPackageDialogField.setEnabled(fCanModifyPackage && !enclosing);
		fEnclosingTypeDialogField.setEnabled(fCanModifyEnclosingType && enclosing);
	}	

	/**
	 * Verify if the enclosing type input field is valid
	 */
	private void updateEnclosingTypeStatus() {
		fCurrEnclosingType= null;
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		
		fEnclosingTypeDialogField.enableButton(root != null);
		if (root == null) {
			return;
		}
		
		String enclName= fEnclosingTypeDialogField.getText();
		if ("".equals(enclName)) {
			fEnclosingTypeStatus.setError(getResourceString(ERROR_ENCLOSING_ENTERNAME));
			return;
		}
		try {
			IType type= JavaModelUtility.findType(root.getJavaProject(), enclName);
			if (type == null) {
				fEnclosingTypeStatus.setError(getResourceString(ERROR_ENCLOSING_NOTEXISTS));
				return;
			}

			if (type.getCompilationUnit() == null) {
				fEnclosingTypeStatus.setError(getResourceString(ERROR_ENCLOSING_PARENTISBINARY));
				return;
			}
			fCurrEnclosingType= type;
			fEnclosingTypeStatus.setOK();
		} catch (JavaModelException e) {
			fEnclosingTypeStatus.setError(getResourceString(ERROR_ENCLOSING_NOTEXISTS));
			JavaPlugin.getDefault().getLog().log(e.getStatus());
		}
	}
	
	// ---- set / get ----------------
	
	/**
	 * Returns the package fragment corresponding to the current input
	 * (Can be null)
	 */
	protected IPackageFragment getPackageFragment() {
		if (!isEnclosingTypeSelected()) {
			return fCurrPackage;
		} else {
			if (fCurrEnclosingType != null) {
				return fCurrEnclosingType.getPackageFragment();
			}
		}
		return null;
	}
	
	protected void setPackageFragment(IPackageFragment pack, boolean canBeModified) {
		fCurrPackage= pack;
		fCanModifyPackage= canBeModified;
		String str= (pack == null) ? "" : pack.getElementName();
		fPackageDialogField.setText(str);
		updateEnableState();
	}	

	/**
	 * Returns the encloding type corresponding to the current input
	 * (Can be null if enclosing type is not selected)
	 */
	public IType getEnclosingType() {
		if (isEnclosingTypeSelected()) {
			return fCurrEnclosingType;
		}
		return null;
	}
	
	public void setEnclosingType(IType type, boolean canBeModified) {
		fCurrEnclosingType= type;
		fCanModifyEnclosingType= canBeModified;
		String str= (type == null) ? "" : JavaModelUtility.getFullyQualifiedName(type);
		fEnclosingTypeDialogField.setText(str);
		updateEnableState();
	}
	
	protected boolean isEnclosingTypeSelected() {
		return fEnclosingTypeSelection.isSelected();
	}
	
	protected void setEnclosingTypeSelection(boolean isSelected, boolean canBeModified) {
		fEnclosingTypeSelection.setSelection(isSelected);
		fEnclosingTypeSelection.setEnabled(canBeModified);
		updateEnableState();
	}	
	
		
	// ---- creation ----------------
		
	protected IPackageFragment createPackage(IProgressMonitor monitor) throws CoreException, InterruptedException {		
		IPackageFragmentRoot root= createContainer(monitor);
		IPackageFragment pack= getPackageFragment();
		if (pack == null) {
			pack= root.getPackageFragment("");
		}

		if (!pack.exists()) {
			String packName= pack.getElementName();
			pack= root.createPackageFragment(packName, true, monitor);
		}
		return pack;
	}
	
	// ---- dialog
	
	private IPackageFragment choosePackage() {
		IPackageFragmentRoot froot= getPackageFragmentRoot();
		IJavaElement[] packages= null;
		try {
			if (froot != null) {
				packages= froot.getChildren();
			}
		} catch (JavaModelException e) {
		}
		if (packages == null) {
			packages= new IJavaElement[0];
		}
		
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), false, false);
		dialog.setTitle(getResourceString(PACKAGE_DIALOG + ".title"));
		dialog.setMessage(getResourceString(PACKAGE_DIALOG + ".description"));
		dialog.setEmptyListMessage(getResourceString(PACKAGE_DIALOG + ".empty"));
		if (dialog.open(packages) == dialog.OK) {;
			return (IPackageFragment) dialog.getPrimaryResult();
		}
		return null;
	}
	
	private IType chooseEnclosingType() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return null;
		}

		IResource[] resources= new IResource[] { root.getJavaProject().getProject() };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(resources);
		scope.setIncludesBinaries(false);
		scope.setIncludesClasspaths(false);	
			
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), scope, IJavaElementSearchConstants.CONSIDER_TYPES, false, false);
		dialog.setTitle(getResourceString(ENCLOSING_DIALOG + ".title"));
		dialog.setMessage(getResourceString(ENCLOSING_DIALOG + ".description"));
		if (dialog.open() == dialog.OK) {	
			return (IType) dialog.getPrimaryResult();
		}
		return null;
	}	
		
}