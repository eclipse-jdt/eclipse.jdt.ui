/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.core.runtime.IStatus;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jdt.launching.LibraryLocation;

public class EditVMDialog extends AddVMDialog {
	private IVMInstall fVM;
	/**
	 * Constructor for EditVMDialog
	 */
	public EditVMDialog(VMPreferencePage page, IVMInstallType[] vmTypes, IVMInstall vm) {
		super(page, vmTypes, vm.getVMInstallType());
		fVM= vm;
	}

	protected IStatus validateVMName() {
		if (fVM.getName().equals(fVMName.getText()))
			return new StatusInfo();
		return super.validateVMName();
	}		protected void initializeFields() {		fVMTypeCombo.setEnabled(false);		fVMName.setText(fVM.getName());		fJDKRoot.setText(fVM.getInstallLocation().getAbsolutePath());		fDebuggerTimeout.setText(String.valueOf(fVM.getDebuggerTimeout()));		LibraryLocation desc= fVM.getLibraryLocation();		fUseDefaultLibrary.setSelection(desc == null);		if (desc == null) {			desc= getVMType().getDefaultLibraryLocation(fVM.getInstallLocation());			useDefaultSystemLibrary();		} else {			useCustomSystemLibrary();			setSystemLibraryFields(desc);		}					}	
	protected void doOkPressed() {		setFieldValuesToVM(fVM);	}}
