/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.wizards.NewElementWizardPage;import org.eclipse.jdt.launching.IVM;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

/*
 * The page for setting the default java runtime preference.
 */
public class VMWizardPage extends NewElementWizardPage {	
	private WizardNewProjectCreationPage fMainPage;
	private VMSelector fVMSelector;
	protected static final String NAME= "VMWizardPage";
	protected static final String ERROR_SET_VM= "error.set_vm.";	
	
	public VMWizardPage(WizardNewProjectCreationPage mainPage) {
		super(NAME, JavaLaunchUtils.getResourceBundle());
		fMainPage= mainPage;
		fVMSelector= new VMSelector();
	}
	/**
	 * @see WizardPage#createContents
	 */
	public void createControl(Composite ancestor) {
		Control vmSelector= fVMSelector.createContents(ancestor);
		fVMSelector.selectVM(JavaRuntime.getDefaultVM());
		setControl(vmSelector);
	}

	public void setVisible(boolean visible) {
		if (visible)
			updateStatus(new StatusInfo());
		super.setVisible(visible);
	}
	
	public boolean finish() {
		try {
			IVM vm= fVMSelector.getSelectedVM();
			if (vm == null)
				vm= JavaRuntime.getDefaultVM();
			if (vm != null)
				JavaRuntime.setVM(JavaCore.create(fMainPage.getProjectHandle()), vm);
		} catch (CoreException e) {
			String title= getResourceString(NAME+"."+ERROR_SET_VM+"title");
			String msg= getResourceString(NAME+"."+ERROR_SET_VM+"label");
			ExceptionHandler.handle(e, getWizard().getContainer().getShell(), title, msg);
			return false;
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
