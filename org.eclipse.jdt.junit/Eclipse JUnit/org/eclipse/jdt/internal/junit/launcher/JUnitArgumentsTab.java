package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.debug.ui.JavaDebugUI;

/**
 * This tab appears for local java launch configurations and allows the user to edit
 * program arguments, VM arguments, and the working directory attributes.
 */
public class JUnitArgumentsTab implements ILaunchConfigurationTab {

	// The launch configuration dialog that owns this tab
	private ILaunchConfigurationDialog fLaunchConfigurationDialog;
	
	// Flag that when true, prevents the owning dialog's status area from getting updated.
	// Used when multiple config attributes are getting updated at once.
	private boolean fBatchUpdate = false;
	
	// Program arguments UI widgets
//	private Label fPrgmArgumentsLabel;
//	private Text fPrgmArgumentsText;

	// VM arguments UI widgets
	private Label fVMArgumentsLabel;
	private Text fVMArgumentsText;
	
	// Working directory UI widgets
	private Label fWorkingDirLabel;
	private Text fWorkingDirText;
	private Button fWorkingDirBrowseButton;
	
	// The launch config working copy providing the values shown on this tab
	private ILaunchConfigurationWorkingCopy fWorkingCopy;

	private static final String EMPTY_STRING = "";
	
	protected void setLaunchDialog(ILaunchConfigurationDialog dialog) {
		fLaunchConfigurationDialog = dialog;
	}
	
	protected ILaunchConfigurationDialog getLaunchDialog() {
		return fLaunchConfigurationDialog;
	}
	
	protected void setWorkingCopy(ILaunchConfigurationWorkingCopy workingCopy) {
		fWorkingCopy = workingCopy;
	}
	
	protected ILaunchConfigurationWorkingCopy getWorkingCopy() {
		return fWorkingCopy;
	}
	
	/**
	 * @see ILaunchConfigurationTab#createTabControl(TabItem)
	 */
	public Control createTabControl(ILaunchConfigurationDialog dialog, TabItem tabItem) {
		setLaunchDialog(dialog);
		
		Composite comp = new Composite(tabItem.getParent(), SWT.NONE);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);		
		GridData gd;
		
		createVerticalSpacer(comp);
				
		Composite workingDirComp = new Composite(comp, SWT.NONE);
		GridLayout workingDirLayout = new GridLayout();
		workingDirLayout.numColumns = 2;
		workingDirLayout.marginHeight = 0;
		workingDirLayout.marginWidth = 0;
		workingDirComp.setLayout(workingDirLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		workingDirComp.setLayoutData(gd);
		
		fWorkingDirLabel = new Label(workingDirComp, SWT.NONE);
		fWorkingDirLabel.setText("Working directory:");
		gd = new GridData();
		gd.horizontalSpan = 2;
		fWorkingDirLabel.setLayoutData(gd);
		
		fWorkingDirText = new Text(workingDirComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fWorkingDirText.setLayoutData(gd);
		fWorkingDirText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateConfigFromWorkingDirectory();
			}
		});
		
		fWorkingDirBrowseButton = new Button(workingDirComp, SWT.PUSH);
		fWorkingDirBrowseButton.setText("Browse");
		fWorkingDirBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleWorkingDirBrowseButtonSelected();
			}
		});
		
		createVerticalSpacer(comp);
				
//		fPrgmArgumentsLabel = new Label(comp, SWT.NONE);
//		fPrgmArgumentsLabel.setText("Program arguments:");
//						
//		fPrgmArgumentsText = new Text(comp, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
//		gd = new GridData(GridData.FILL_HORIZONTAL);
//		gd.heightHint = 40;
//		fPrgmArgumentsText.setLayoutData(gd);
//		fPrgmArgumentsText.addModifyListener(new ModifyListener() {
//			public void modifyText(ModifyEvent evt) {
//				updateConfigFromPgmArgs();
//			}
//		});
		
		fVMArgumentsLabel = new Label(comp, SWT.NONE);
		fVMArgumentsLabel.setText("VM arguments:");
		
		fVMArgumentsText = new Text(comp, SWT.MULTI | SWT.WRAP| SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		fVMArgumentsText.setLayoutData(gd);	
		fVMArgumentsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateConfigFromVMArgs();
			}
		});	
				
		return comp;
	}
	
	/**
	 * @see ILaunchConfigurationTab#setLaunchConfiguration(ILaunchConfigurationWorkingCopy)
	 */
	public void setLaunchConfiguration(ILaunchConfigurationWorkingCopy launchConfiguration) {
		if (launchConfiguration.equals(getWorkingCopy())) {
			return;
		}
		
		setBatchUpdate(true);
		updateWidgetsFromConfig(launchConfiguration);
		setBatchUpdate(false);

		setWorkingCopy(launchConfiguration);
	}
	
	/**
	 * Set values for all UI widgets in this tab using values kept in the specified
	 * launch configuration.
	 */
	protected void updateWidgetsFromConfig(ILaunchConfiguration config) {
		//updatePgmArgsFromConfig(config);
		updateVMArgsFromConfig(config);
		updateWorkingDirectoryFromConfig(config);
	}
	
//	protected void updatePgmArgsFromConfig(ILaunchConfiguration config) {
//		try {
//			String pgmArgs = config.getAttribute(JavaDebugUI.PROGRAM_ARGUMENTS_ATTR, EMPTY_STRING);
//			fPrgmArgumentsText.setText(pgmArgs);
//		} catch (CoreException ce) {			
//		}
//	}
	
	protected void updateVMArgsFromConfig(ILaunchConfiguration config) {
		try {
			String vmArgs = config.getAttribute(JavaDebugUI.VM_ARGUMENTS_ATTR, EMPTY_STRING);
			fVMArgumentsText.setText(vmArgs);
		} catch (CoreException ce) {			
		}
	}
	
	protected void updateWorkingDirectoryFromConfig(ILaunchConfiguration config) {
		try {
			String workingDir = config.getAttribute(JavaDebugUI.WORKING_DIRECTORY_ATTR, EMPTY_STRING);
			fWorkingDirText.setText(workingDir);
		} catch (CoreException ce) {			
		}		
	}
	
//	protected void updateConfigFromPgmArgs() {
//		if (getWorkingCopy() != null) {
//			String pgmArgs = fPrgmArgumentsText.getText();
//			getWorkingCopy().setAttribute(JavaDebugUI.PROGRAM_ARGUMENTS_ATTR, pgmArgs);
//			refreshStatus();
//		}
//	}
	
	protected void updateConfigFromVMArgs() {
		if (getWorkingCopy() != null) {
			String vmArgs = fVMArgumentsText.getText();
			getWorkingCopy().setAttribute(JavaDebugUI.VM_ARGUMENTS_ATTR, vmArgs);
			refreshStatus();
		}
	}
	
	protected void updateConfigFromWorkingDirectory() {
		if (getWorkingCopy() != null) {
			String workingDir = fWorkingDirText.getText();
			getWorkingCopy().setAttribute(JavaDebugUI.WORKING_DIRECTORY_ATTR, workingDir);
			refreshStatus();
		}
	}
	
	protected void refreshStatus() {
		if (!isBatchUpdate()) {
			getLaunchDialog().refreshStatus();
		}
	}
	
	/**
	 * @see ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
	}
	
	protected void setBatchUpdate(boolean update) {
		fBatchUpdate = update;
	}
	
	protected boolean isBatchUpdate() {
		return fBatchUpdate;
	}

	/**
	 * Create some empty space 
	 */
	protected void createVerticalSpacer(Composite comp) {
		new Label(comp, SWT.NONE);
	}
	
	/**
	 * Show a dialog that lets the user select a working directory
	 */
	protected void handleWorkingDirBrowseButtonSelected() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setMessage("Select a working directory for the launch configuration");
		String currentWorkingDir = fWorkingDirText.getText();
		if (!currentWorkingDir.trim().equals("")) {
			File path = new File(currentWorkingDir);
			if (path.exists()) {
				dialog.setFilterPath(currentWorkingDir);
			}			
		}
		
		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			fWorkingDirText.setText(selectedDirectory);
		}		
	}
	
	/**
	 * Convenience method to get the shell.  It is important that the shell be the one 
	 * associated with the launch configuration dialog, and not the active workbench
	 * window.
	 */
	private Shell getShell() {
		return fWorkingDirLabel.getShell();
	}
}