/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TableViewer;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Combo;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Table;

/*
 * The page for setting java runtime
 */
public class VMPropertyPage extends JavaProjectPropertyPage {
	private VMSelector fVMSelector;
	
	public VMPropertyPage() {
		fVMSelector= new VMSelector();
	}
	
	protected Control createJavaContents(Composite ancestor) {
		noDefaultAndApplyButton();
		Control vmSelector= fVMSelector.createContents(ancestor);
		fVMSelector.initFromProject(getJavaProject());
		return vmSelector;
	}

	
	public boolean performOk() {
		IJavaProject project= getJavaProject();
		if (project != null) {
			try {
				JavaRuntime.setVM(project, fVMSelector.getSelectedVM());
			} catch (CoreException e) {
			}
		}
		return true;
	}
}