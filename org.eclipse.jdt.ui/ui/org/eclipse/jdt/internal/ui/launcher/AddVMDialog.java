/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.launcher;
import java.io.File;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.DirectoryDialog;

public class AddVMDialog extends StatusDialog {
	protected IVMInstallType fVMType;
	
	protected StringButtonDialogField fJDKRoot;
	protected StringDialogField fVMName;
	protected StringDialogField fDebuggerTimeout;
	
	public AddVMDialog(Shell shell, IVMInstallType type) {
		super(shell);
		
		fVMType= type;
		
		fVMName= new StringDialogField();
		fVMName.setLabelText("VM Install Name");
		fVMName.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				validateVMName();
			}
		});
		
		fJDKRoot= new StringButtonDialogField(new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				browseForInstallDir();
			}
		});
		fJDKRoot.setLabelText("Root Directory");
		fJDKRoot.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				validateJDKLocation();
			}
		});
		fJDKRoot.setButtonLabel("Browse");
		
		fDebuggerTimeout= new StringDialogField();
		fDebuggerTimeout.setLabelText("Debugger Timeout");
		fDebuggerTimeout.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				validateDebuggerTimeout();
			}
		});
		
	}
	
	public String getVMName() {
		return fVMName.getText();
	}
	
	public void setVMName(String name) {
		fVMName.setText(name);
	}
	
	public void setInstallLocation(File location) {
		fJDKRoot.setText(location.getAbsolutePath());
	}
	
	public File getInstallLocation() {
		return new File(fJDKRoot.getText());
	}
	
	public void setTimeout(int timeout) {
		fDebuggerTimeout.setText(String.valueOf(timeout));
	}
	
	public int getTimeout() {
		return Integer.valueOf(fDebuggerTimeout.getText()).intValue();
	}
	
	protected Control createDialogArea(Composite ancestor) {
		Composite parent= new Composite(ancestor, SWT.NULL);
		MGridLayout layout= new MGridLayout();
		layout.numColumns= 3;
		layout.minimumWidth= convertWidthInCharsToPixels(80);
		parent.setLayout(layout);
		
		fVMName.doFillIntoGrid(parent, 3);
		fJDKRoot.doFillIntoGrid(parent, 3);
		fDebuggerTimeout.doFillIntoGrid(parent, 3);
		
		return parent;
	}
	
	public void create() {
		super.create();
		fVMName.setFocus();
	}
	
	private void validateJDKLocation() {
		updateStatus(fVMType.validateInstallLocation(new File(fJDKRoot.getText())));
	}

	protected void validateVMName() {
		IVMInstall[] vms= fVMType.getVMInstalls();
		for (int i= 0; i < vms.length; i++) {
			if (vms[i].getName().equals(fVMName.getText())) {
				updateStatus(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "The name is already ussed", null));
			}
		}
	}
	
	protected void validateDebuggerTimeout() {
		String timeoutText= fDebuggerTimeout.getText();
		long timeout= 0;
		try {
			timeout= Long.valueOf(timeoutText).longValue();
		} catch (NumberFormatException e) {
			updateStatus(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "The timeout value must be a number", e));
			return;
		}
		if (timeout < 0) {
			updateStatus(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "The timeout value must be >= 0", null));
			return;
		}
		if (timeout > Integer.MAX_VALUE) {
			updateStatus(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "The timeout value must be <= "+Integer.MAX_VALUE, null));
			return;
		}
		updateStatus(new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null));
			
	}
	
	private void browseForInstallDir() {
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setFilterPath(fJDKRoot.getText());
		dialog.setMessage("Select the root directory the VM Installation");
		String newPath= dialog.open();
		if  (newPath != null)
			fJDKRoot.setText(newPath);
	}
}
