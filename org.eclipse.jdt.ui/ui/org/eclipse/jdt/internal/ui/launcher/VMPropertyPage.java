/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.launching.JavaRuntime;

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
		fVMSelector.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setValid(fVMSelector.validateSelection(event.getSelection()));
			}
		});
		WorkbenchHelp.setHelp(ancestor, new DialogPageContextComputer(this, IJavaHelpContextIds.LAUNCH_JRE_PROPERYY_PAGE));			
		return vmSelector;
	}

	
	protected boolean performJavaOk() {
		IJavaProject project= getJavaProject();
		if (project != null) {
			try {
				JavaRuntime.setVM(project, fVMSelector.getSelectedVM());
			} catch (CoreException e) {
			}
		}
		return true;
	}
	
	public void setElement(IAdaptable element) {
		super.setElement(element);
		if (getJavaProject() != null && isOpenProject())
			setDescription("Select the JRE for running Java programs");
	} 

}