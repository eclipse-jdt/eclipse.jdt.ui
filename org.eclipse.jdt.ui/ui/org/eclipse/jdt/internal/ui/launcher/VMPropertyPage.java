/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.launching.JavaRuntime;

/*
 * The page for setting java runtime
 */
public class VMPropertyPage extends JavaProjectPropertyPage {
	private Combo fCombo;
	
	protected static final String DESCRIPTION="vm_propertypage.description";
	protected static final String NO_JAVA="vm_propertypage.no_java";
	
	protected Control createJavaContents(Composite parent) {
		noDefaultAndApplyButton();
		IJavaProject project= getJavaProject();
		fCombo= new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		String[] vms= JavaRuntime.getJavaRuntimes();
		
		for (int i= 0; i < vms.length; i++) {
			fCombo.add(vms[i]);
		}
		int index=0;
		try {
			String vm= JavaRuntime.getJavaRuntime(project);
			if (vm == null)
				vm= JavaPlugin.getDefault().getPreferenceStore().getString(VMPreferencePage.PREF_VM);
			if (vm != null)
				index= fCombo.indexOf(vm);
		} catch (CoreException e) {
		}
		fCombo.select(index);
		return fCombo;
	};
	
	public boolean performOk() {
		IJavaProject project= getJavaProject();
		if (project != null) {
			try {
				JavaRuntime.setJavaRuntime(project, fCombo.getText());
			} catch (CoreException e) {
			}
		}
		return true;
	}
	
	private IProject getProject() {
		Object o= getElement();
		if (o instanceof IProject)
			return (IProject)o;
		if (o instanceof IJavaProject) {
			return ((IJavaProject)o).getProject();
		}
		return null;
	}
						
	public void setElement(IAdaptable element) {
		super.setElement(element);
		IJavaProject project= getJavaProject();
		if (project != null) {
			setDescription(JavaLaunchUtils.getResourceString(DESCRIPTION));
		}
	}
}