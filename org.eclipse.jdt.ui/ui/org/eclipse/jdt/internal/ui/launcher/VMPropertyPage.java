/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Listener;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;

/*
 * The page for setting java runtime
 */
public class VMPropertyPage extends JavaProjectPropertyPage {
	private VMSelector fVMSelector;
	private Control fVMSelectorWidget;
	private Button fUseDefault;
	private Button fUseCustom;
	private boolean fFirstTime= true;
	
	public VMPropertyPage() {
		fVMSelector= new VMSelector();
	}
	
	protected Control createJavaContents(Composite ancestor) {
		noDefaultAndApplyButton();
		Composite parent= new Composite(ancestor, SWT.NULL);
		parent.setLayout(new GridLayout());
		
		fUseDefault= new Button(parent, SWT.RADIO);
		fUseDefault.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fUseDefault.setText(LauncherMessages.getString("vmPropertyPage.useDefaultJRE")); //$NON-NLS-1$
		fUseDefault.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				enableCustom(false);
			}
		});
		
		fUseCustom= new Button(parent, SWT.RADIO);
		fUseCustom.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fUseCustom.setText(LauncherMessages.getString("vmPropertyPage.useCustomJRE")); //$NON-NLS-1$
		fUseCustom.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				enableCustom(true);
			}
		});

		fVMSelectorWidget= fVMSelector.createContents(parent);
		fVMSelector.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setValid(fVMSelector.validateSelection(event.getSelection()));
			}
		});
		fVMSelectorWidget.setLayoutData(new GridData(GridData.FILL_BOTH));
		WorkbenchHelp.setHelp(ancestor, new DialogPageContextComputer(this, IJavaHelpContextIds.LAUNCH_JRE_PROPERYY_PAGE));			
		return parent;
	}
	
	/**
	 * Must be called after createContents
	 */ 
	public void initFromProject(IJavaProject project) {
		IVMInstall vm= null;
		try {
			vm= JavaRuntime.getVMInstall(project);
		} catch (CoreException e) {
		}
		enableCustom(vm != null);
		fUseCustom.setSelection(vm != null);
		fUseDefault.setSelection(vm == null);
		if (vm == null) {
			vm= JavaRuntime.getDefaultVMInstall();
		}
		fVMSelector.selectVM(vm);
	}
	
	protected boolean performJavaOk() {
		IJavaProject project= getJavaProject();
		if (project != null) {
			try {
				IVMInstall vm= null;
				if (fUseCustom.getSelection()) 
					 vm= fVMSelector.getSelectedVM();
				JavaRuntime.setVM(project, vm);
			} catch (CoreException e) {
			}
		}
		return true;
	}
	
	public void setElement(IAdaptable element) {
		super.setElement(element);
		if (getJavaProject() != null && isOpenProject())
			setDescription(LauncherMessages.getString("vmPropertyPage.description")); //$NON-NLS-1$
	} 
	
	public void setVisible(boolean visible) {
		if (visible && fFirstTime) {
			fFirstTime= false;
			setTitle(LauncherMessages.getString("vmPropertyPage.title")); //$NON-NLS-1$
			// fix: 1GET9NJ: ITPJUI:ALL - NPE looking at JRE properties for closed project		
			if (isCreated())
				initFromProject(getJavaProject());
			// end fix.
		}
		super.setVisible(visible);
	}
	
	private void enableCustom(boolean useCustom) {
		fVMSelectorWidget.setEnabled(useCustom);
	}
}