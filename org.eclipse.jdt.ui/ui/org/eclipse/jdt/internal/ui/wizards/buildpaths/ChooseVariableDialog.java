/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.lang.reflect.InvocationTargetException;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.viewers.DoubleClickEvent;import org.eclipse.jface.viewers.IDoubleClickListener;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;public class ChooseVariableDialog extends StatusDialog implements IStatusChangeListener, IDoubleClickListener {

	private static final String PAGE_NAME= "ChooseVariableDialog";
	private static final String DIALOG_CHOOSEVARIABLE= PAGE_NAME + ".variabledialog";
	private VariableBlock fVariableBlock;
			
	public ChooseVariableDialog(Shell parent, String lastVariableSelection) {
		super(parent);
		setTitle(JavaPlugin.getResourceString(DIALOG_CHOOSEVARIABLE + ".title"));
		fVariableBlock= new VariableBlock(this, true, lastVariableSelection);
	}
			
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		fVariableBlock.createContents(composite);
		fVariableBlock.addDoubleClickListener(this);
		return composite;
	}
	
	protected void okPressed() {
		fVariableBlock.performOk();
		super.okPressed();
	}
	
	public String getSelectedVariable() {
		return fVariableBlock.getSelectedVariable();
	}
	
	/**
 	 * @see IStatusChangeListener#statusChanged(IStatus)
 	 */
	public void statusChanged(IStatus status) {
		updateStatus(status);
		
	}
	
	/**
 	 * @see IDoubleClickListener#doubleClick(DoubleClickEvent)
 	 */
	public void doubleClick(DoubleClickEvent event) {
		if (getStatus().isOK()) {
			okPressed();
		}
	}

}