package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.swt.widgets.Shell;

public class EditVMDialog extends AddVMDialog {
	private IVMInstall fVM;
	/**
	 * Constructor for EditVMDialog
	 */
	public EditVMDialog(Shell shell, IVMInstall vm) {
		super(shell, vm.getVMInstallType());
		fVM= vm;
	}

	protected void validateVMName() {
		if (fVM.getName().equals(fVMName.getText()))
			return;
		super.validateVMName();
	}

}
