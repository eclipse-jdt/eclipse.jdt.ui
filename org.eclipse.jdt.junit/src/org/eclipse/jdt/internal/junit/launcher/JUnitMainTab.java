package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.debug.ui.JavaDebugUI;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.debug.ui.launcher.AddVMDialog;
import org.eclipse.jdt.internal.debug.ui.launcher.IAddVMDialogRequestor;
import org.eclipse.jdt.internal.debug.ui.launcher.VMStandin;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.util.*;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

/**
 * This tab appears in the LaunchConfigurationDialog for launch configurations that
 * require Java-specific launching information such as a main type and JRE.
 */
public class JUnitMainTab extends JUnitLaunchConfigurationTab {
	
	// Project UI widgets
	private Label fProjLabel;
	private Text fProjText;
	private Button fProjButton;
	private Button fKeepRunning;
	
	// Test class UI widgets
	private Label fTestLabel;
	private Text fTestText;
	private Button fSearchButton;
				
	/**
	 * @see ILaunchConfigurationTab#createControl(TabItem)
	 */
	public void createControl(Composite parent) {		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
		topLayout.numColumns= 2;
		comp.setLayout(topLayout);		
		GridData gd;
		
		createVerticalSpacer(comp);
		
		fProjLabel = new Label(comp, SWT.NONE);
		fProjLabel.setText("&Project:");
		gd= new GridData();
		gd.horizontalSpan = 2;
		fProjLabel.setLayoutData(gd);
		
		fProjText= new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjText.setLayoutData(gd);
		fProjText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fProjButton = new Button(comp, SWT.PUSH);
		fProjButton.setText("&Browse...");
		fProjButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleProjectButtonSelected();
			}
		});
		setButtonGridData(fProjButton); 
		
		fTestLabel = new Label(comp, SWT.NONE);
		fTestLabel.setText("&Test class:");
		gd = new GridData();
		gd.horizontalSpan = 2;
		fTestLabel.setLayoutData(gd);

		fTestText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fTestText.setLayoutData(gd);
		fTestText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fSearchButton = new Button(comp, SWT.PUSH);
		fSearchButton.setText("&Search...");
		fSearchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleSearchButtonSelected();
			}
		});
		setButtonGridData(fSearchButton);
		
		fKeepRunning = new Button(comp, SWT.CHECK);
		fKeepRunning.setText("&Keep JUnit running after a test run when debugging");
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalSpan= 2;
		fKeepRunning.setLayoutData(gd);
	}
	

	/**
	 * @see ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		updateProjectFromConfig(config);
		updateTestTypeFromConfig(config);
		updateKeepRunning(config);
	}

	private void updateKeepRunning(ILaunchConfiguration config) {
		boolean running= false;
		try {
			running= config.getAttribute(JUnitBaseLaunchConfiguration.ATTR_KEEPRUNNING, false);
		} catch (CoreException ce) {
		}
		fKeepRunning.setSelection(running);	 	
	}
	
	protected void updateProjectFromConfig(ILaunchConfiguration config) {
		String projectName= "";
		try {
			projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		} catch (CoreException ce) {
		}
		fProjText.setText(projectName);
	}
	
	protected void updateTestTypeFromConfig(ILaunchConfiguration config) {
		String testTypeName= "";
		try {
			testTypeName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
		} catch (CoreException ce) {			
		}
		fTestText.setText(testTypeName);		
	}

	/**
	 * @see ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)fProjText.getText());
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)fTestText.getText());
		config.setAttribute(JUnitBaseLaunchConfiguration.ATTR_KEEPRUNNING, fKeepRunning.getSelection());
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
	 * Show a dialog that lists all main types
	 */
	protected void handleSearchButtonSelected() {
		Shell shell = getShell();
		IWorkbenchWindow workbenchWindow = JUnitPlugin.getActiveWorkbenchWindow();
		
		IJavaProject javaProject = getJavaProject();
		
		SelectionDialog dialog = new TestSelectionDialog(shell, getLaunchConfigurationDialog(), javaProject);
		dialog.setTitle("Test Selection");
		dialog.setMessage("Choose a test case or test suite:");
		if (dialog.open() == dialog.CANCEL) {
			return;
		}
		
		Object[] results = dialog.getResult();
		if ((results == null) || (results.length < 1)) {
			return;
		}		
		IType type = (IType)results[0];
		
		if (type != null) {
			fTestText.setText(type.getFullyQualifiedName());
			javaProject = type.getJavaProject();
			fProjText.setText(javaProject.getElementName());
		}
	}
		
	/**
	 * Show a dialog that lets the user select a project.  This in turn provides
	 * context for the main type, allowing the user to key a main type name, or
	 * constraining the search for main types to the specified project.
	 */
	protected void handleProjectButtonSelected() {
		IJavaProject project = chooseJavaProject();
		if (project == null) {
			return;
		}
		
		String projectName = project.getElementName();
		fProjText.setText(projectName);		
	}
	
	/**
	 * Realize a Java Project selection dialog and return the first selected project,
	 * or null if there was none.
	 */
	protected IJavaProject chooseJavaProject() {
		IJavaProject[] projects;
		try {
			projects= JavaCore.create(getWorkspaceRoot()).getJavaProjects();
		} catch (JavaModelException e) {
			JUnitPlugin.log(e.getStatus());
			projects= new IJavaProject[0];
		}
		
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle("Project Selection");
		dialog.setMessage("Choose a project to constrain the search for main types:");
		dialog.setElements(projects);
		
		IJavaProject javaProject = getJavaProject();
		if (javaProject != null) {
			dialog.setInitialSelections(new Object[] { javaProject });
		}
		if (dialog.open() == dialog.OK) {			
			return (IJavaProject) dialog.getFirstResult();
		}			
		return null;		
	}
	
	/**
	 * Return the IJavaProject corresponding to the project name in the project name
	 * text field, or null if the text does not match a project name.
	 */
	protected IJavaProject getJavaProject() {
		String projectName = fProjText.getText().trim();
		if (projectName.length() < 1) {
			return null;
		}
		return getJavaModel().getJavaProject(projectName);		
	}
	
	/**
	 * Convenience method to get the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	/**
	 * Convenience method to get access to the java model.
	 */
	private IJavaModel getJavaModel() {
		return JavaCore.create(getWorkspaceRoot());
	}

	/**
	 * @see ILaunchConfigurationTab#isPageComplete()
	 */
	public boolean isValid() {
		
		setErrorMessage(null);
		setMessage(null);
		
		String name = fProjText.getText().trim();
		if (name.length() > 0) {
			if (!ResourcesPlugin.getWorkspace().getRoot().getProject(name).exists()) {
				setErrorMessage("Project does not exist.");
				return false;
			}
		}

		name = fTestText.getText().trim();
		if (name.length() == 0) {
			setErrorMessage("Test not specified.");
			return false;
		}
		// TO DO should verify that test exists
		return true;
	}
	
	/**
	 * Initialize default attribute values based on the
	 * given Java element.
	 */
	protected void initializeDefaults(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		initializeJavaProject(javaElement, config);
		initializeTestTypeAndName(javaElement, config);
		initializeHardCodedDefaults(config);
	}
	
	/**
	 * @see ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IJavaElement je = getContext();
		if (je == null) {
			initializeHardCodedDefaults(config);
		} else {
			initializeDefaults(je, config);
		}
	}
	
	/**
	 * Set the main type & name attributes on the working copy based on the IJavaElement
	 */
	protected void initializeTestTypeAndName(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		String name= "";
		try {
			// we only do a search for compilation units or class files or 
			// or source references
			if ((javaElement instanceof ICompilationUnit) || 
				(javaElement instanceof ISourceReference) ||
				(javaElement instanceof IClassFile)) {
		
				IType[] types = TestSearchEngine.findTests(new BusyIndicatorRunnableContext(), new Object[] {javaElement});
				if ((types == null) || (types.length < 1)) {
					return;
				}
				// Simply grab the first main type found in the searched element
				name = types[0].getFullyQualifiedName();
			}	
		} catch (InterruptedException ie) {
		} catch (InvocationTargetException ite) {
		}		
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, name);
		int index = name.lastIndexOf('.');
		if (index > 0) {
			name = name.substring(index + 1);
		}
		name= getLaunchConfigurationDialog().generateName(name);
		config.rename(name);
	}
	
	/**
	 * Initialize those attributes whose default values are independent of any context.
	 */
	protected void initializeHardCodedDefaults(ILaunchConfigurationWorkingCopy config) {
		initializeDefaultVM(config);
	}
	
	/**
	 * Set the VM attributes on the working copy based on the workbench default VM.
	 */
	protected void initializeDefaultVM(ILaunchConfigurationWorkingCopy config) {
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		if (vmInstall == null) {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL, (String)null);
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
		} else {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL, vmInstall.getId());
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmInstall.getVMInstallType().getId());
		}
	}
	
	/**
	 * @see ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "&Test";
	}
}
