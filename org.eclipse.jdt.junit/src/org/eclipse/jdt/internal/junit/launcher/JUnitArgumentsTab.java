package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.File;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.ui.JavaDebugUI;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
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

/**
 * This tab appears for local java launch configurations and allows the user to edit
 * program arguments, VM arguments, and the working directory attributes.
 */
public class JUnitArgumentsTab extends JUnitLaunchConfigurationTab {
		
	// VM arguments UI widgets
	private Label fVMArgumentsLabel;
	private Text fVMArgumentsText;
	
	// Working directory UI widgets
	private Label fWorkingDirLabel;
	private Text fWorkingDirText;
	private Button fWorkingDirBrowseButton;
	
	private static final String EMPTY_STRING = "";
		
	/**
	 * @see ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
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
		fWorkingDirLabel.setText("Wor&king directory:");
		gd = new GridData();
		gd.horizontalSpan = 2;
		fWorkingDirLabel.setLayoutData(gd);
		
		fWorkingDirText = new Text(workingDirComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fWorkingDirText.setLayoutData(gd);
		fWorkingDirText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				getLaunchConfigurationDialog();
			}
		});
		
		fWorkingDirBrowseButton = new Button(workingDirComp, SWT.PUSH);
		fWorkingDirBrowseButton.setText("&Browse...");
		fWorkingDirBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleWorkingDirBrowseButtonSelected();
			}
		});
		
		createVerticalSpacer(comp);
		
		fVMArgumentsLabel = new Label(comp, SWT.NONE);
		fVMArgumentsLabel.setText("VM ar&guments:");
		
		fVMArgumentsText = new Text(comp, SWT.MULTI | SWT.WRAP| SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		fVMArgumentsText.setLayoutData(gd);	
		fVMArgumentsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				getLaunchConfigurationDialog();
			}
		});	
		
	}
		
	/**
	 * @see JavaLaunchConfigurationTab#updateWidgetsFromConfig(ILaunchConfiguration)
	 */
	protected void updateWidgetsFromConfig(ILaunchConfiguration config) {
		updateVMArgsFromConfig(config);
		updateWorkingDirectoryFromConfig(config);
	}
	
	protected void updateVMArgsFromConfig(ILaunchConfiguration config) {
		try {
			String vmArgs = EMPTY_STRING;
			if (config != null) {
				vmArgs = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, EMPTY_STRING);
			}
			fVMArgumentsText.setText(vmArgs);
		} catch (CoreException ce) {			
		}
	}
	
	protected void updateWorkingDirectoryFromConfig(ILaunchConfiguration config) {
		try {
			String workingDir = EMPTY_STRING;
			if (config != null) {
				workingDir = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, EMPTY_STRING);
			}
			fWorkingDirText.setText(workingDir);
		} catch (CoreException ce) {			
		}		
	}
	
	/**
	 * @see ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
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
	 * @see ILaunchConfigurationTab#isPageComplete()
	 */
	public boolean isValid() {
		
		setErrorMessage(null);
		setMessage(null);
		
		String workingDirPath = fWorkingDirText.getText().trim();
		if (workingDirPath.length() > 0) {
			File dir = new File(workingDirPath);
			if (!dir.exists()) {
				setErrorMessage("Working directory does not exist.");
				return false;
			}
			if (!dir.isDirectory()) {
				setErrorMessage("Working directory is not a directory.");
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Defaults are empty.
	 * 
	 * @see ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String)null);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String)null);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, (String)null);
	}

	/**
	 * @see ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fWorkingDirText.setText(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, ""));
			fVMArgumentsText.setText(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""));
		} catch (CoreException e) {
			setErrorMessage("Exception occurred reading configuration: " + e.getStatus().getMessage());
		}
	}

	/**
	 * @see ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, getAttributeValueFrom(fVMArgumentsText));
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, getAttributeValueFrom(fWorkingDirText));
	}

	/**
	 * Retuns the string in the text widget, or <code>null</code> if empty.
	 * 
	 * @return text or <code>null</code>
	 */
	protected String getAttributeValueFrom(Text text) {
		String content = text.getText().trim();
		if (content.length() > 0) {
			return content;
		}
		return null;
	}
}

