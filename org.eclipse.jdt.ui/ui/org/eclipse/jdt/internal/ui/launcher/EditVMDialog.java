package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.jdt.launching.IVM;import org.eclipse.swt.widgets.Shell;

public class EditVMDialog extends AddVMDialog {
	private IVM fVM;
	/**
	 * Constructor for EditVMDialog
	 */
	public EditVMDialog(Shell shell, IVM vm) {
		super(shell, vm.getVMType());
		fVM= vm;
	}

	protected void validateVMName() {
		if (fVM.getName().equals(fVMName.getText()))
			return;
		super.validateVMName();
	}

}
