/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.PortingFinder;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizardPage;
import org.eclipse.jdt.launching.JavaRuntime;

/*
 * The page for setting the default java runtime preference.
 */
public class VMWizardPage extends NewElementWizardPage {	
	protected static final String NAME= "VMWizardPage";
	protected static final String VM_CHOICE= "vm_selection.";
	protected static final String ERROR_SET_VM= "error.set_vm.";

	private Combo fCombo;
	private WizardNewProjectCreationPage fMainPage;

	public VMWizardPage(WizardNewProjectCreationPage mainPage) {
		super(NAME, JavaLaunchUtils.getResourceBundle());
		fMainPage= mainPage;
	}
	/**
	 * @see WizardPage#createContents
	 */
	public void createControl(Composite ancestor) {
		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout l= new GridLayout();
		l.numColumns= 2;
		parent.setLayout(l);

		Label label= new Label(parent, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(JavaLaunchUtils.getResourceString(NAME+"."+VM_CHOICE+"label"));
		
		fCombo= new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		fCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		String[] vms= JavaRuntime.getJavaRuntimes();
		for (int i= 0; i < vms.length; i++) {
			fCombo.add(vms[i]);
		}
		if (fCombo.getItemCount() > 0)
			fCombo.select(0);
	}

	public void setVisible(boolean visible) {
		if (visible)
			updateStatus(new StatusInfo());
		super.setVisible(visible);
	}
	
	public boolean finish() {
		try {
			String vm= null;
			if (fCombo != null) {
				vm= fCombo.getText();
			} else {
				String[] vms= JavaRuntime.getJavaRuntimes();
				if (vms.length > 0) {
					vm= vms[0];
				}
			}
			if (vm != null)
				JavaRuntime.setJavaRuntime(JavaCore.create(fMainPage.getProjectHandle()), vm);
		} catch (CoreException e) {
			PortingFinder.toBeDone("to be fixed: not possible to show error dialog (not in ui thread)");

			String title= getResourceString(NAME+"."+ERROR_SET_VM+"title");
			String msg= getResourceString(NAME+"."+ERROR_SET_VM+"label");
			ErrorDialog.openError(getWizard().getContainer().getShell(), title, msg, e.getStatus());
		}
		return true;
	}
	
	/**
	 * @see NewElementWizardPage#getRunnable()
	 */
	public IRunnableWithProgress getRunnable() {
		return null;
	}

}
