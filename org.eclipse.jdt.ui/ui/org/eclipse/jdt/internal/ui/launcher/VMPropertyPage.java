/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

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
		setTitle(LauncherMessages.getString("vmPropertyPage.title")); //$NON-NLS-1$
		noDefaultAndApplyButton();
	}
	
	protected Control createJavaContents(Composite ancestor) {		
		Composite parent= new Composite(ancestor, SWT.NULL);
		parent.setLayout(new GridLayout());
		
		SelectionListener listener= new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {				
			}

			public void widgetSelected(SelectionEvent e) {
				buttonSelected(e.widget);
			}
		};
								
		fUseDefault= new Button(parent, SWT.RADIO);
		fUseDefault.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fUseDefault.setText(LauncherMessages.getString("vmPropertyPage.useDefaultJRE")); //$NON-NLS-1$
		fUseDefault.addSelectionListener(listener);
		
		fUseCustom= new Button(parent, SWT.RADIO);
		fUseCustom.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fUseCustom.setText(LauncherMessages.getString("vmPropertyPage.useCustomJRE")); //$NON-NLS-1$
		fUseCustom.addSelectionListener(listener);

		fVMSelectorWidget= fVMSelector.createContents(parent);
		fVMSelector.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				listSelectionChanged(event.getSelection());
			}
		});
		fVMSelectorWidget.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		initFromProject(getJavaProject());
		
		WorkbenchHelp.setHelp(ancestor, new DialogPageContextComputer(this, IJavaHelpContextIds.LAUNCH_JRE_PROPERYY_PAGE));			
		return parent;
	}
	
	private void buttonSelected(Widget widget) {
		fVMSelectorWidget.setEnabled(widget == fUseCustom);
	}
	
	private void listSelectionChanged(ISelection newSelection) {
		setValid(fVMSelector.validateSelection(newSelection));
	}	
	
	/**
	 * Called to initialize the dialog
	 */ 
	private void initFromProject(IJavaProject project) {
		IVMInstall vm= null;
		try {
			vm= JavaRuntime.getVMInstall(project);
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
		}
		fVMSelectorWidget.setEnabled(vm != null);
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
				JavaPlugin.log(e.getStatus());
			}
		}
		return true;
	}

}