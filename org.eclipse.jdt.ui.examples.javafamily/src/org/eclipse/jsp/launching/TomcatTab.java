/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp.launching;

 
import java.io.File;

import org.eclipse.jsp.JspPluginImages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.variables.VariablesPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Specifies the install location of Tomcat.
 */
public class TomcatTab extends AbstractLaunchConfigurationTab {
		
		
	// Tomcat location
	private Button fBrowseButton;
	private Text fTomcatDir;
	
	// WebApp location
	private Button fProjectButton;
	private Text fProjectText;
	
	/**
	 * Constructs a new Tomcat tab
	 */
	public TomcatTab() {
		super();
	}
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		
		Font font = parent.getFont();
				
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout workingDirLayout = new GridLayout();
		workingDirLayout.numColumns = 3;
		workingDirLayout.marginHeight = 0;
		workingDirLayout.marginWidth = 0;
		composite.setLayout(workingDirLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(gd);
		composite.setFont(font);
		setControl(composite);
		
		createVerticalSpacer(composite, 3);
				
		Label label = new Label(composite, SWT.NONE);
		label.setText(LaunchingMessages.TomcatTab_3);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		label.setFont(font);
				
		fTomcatDir = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fTomcatDir.setLayoutData(gd);
		fTomcatDir.setFont(font);
		fTomcatDir.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fBrowseButton = createPushButton(composite, LaunchingMessages.TomcatTab_21, null);
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleTomcatBrowseButtonSelected();
			}
		});
		
		createVerticalSpacer(composite, 3);
		
		label = new Label(composite, SWT.NONE);
		label.setText(LaunchingMessages.TomcatTab_22);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		label.setFont(font);
				
		fProjectText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fProjectText.setLayoutData(gd);
		fProjectText.setFont(font);
		fProjectText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fProjectButton = createPushButton(composite, LaunchingMessages.TomcatTab_23, null);
		fProjectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleProjectBrowseButtonSelected();
			}
		});
	}
		
	/**
	 * Show a dialog that lets the user select a project
	 * from the workspace
	 */
	protected void handleProjectBrowseButtonSelected() {
		ILabelProvider lp= new WorkbenchLabelProvider();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), lp);
		dialog.setElements(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		dialog.setMultipleSelection(false);
		dialog.setTitle(LaunchingMessages.TomcatTab_28);
		dialog.setMessage(LaunchingMessages.TomcatTab_29);
		if (dialog.open() == Window.OK) {
			Object[] elements= dialog.getResult();
			if (elements != null && elements.length == 1) {
				fProjectText.setText(((IResource)elements[0]).getName());
			}
		}
		
	}

	/**
	 * Show a dialog that lets the user select a tomcat install directory
	 */
	protected void handleTomcatBrowseButtonSelected() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setMessage(LaunchingMessages.TomcatTab_4);
		String currentWorkingDir = fTomcatDir.getText();
		if (!currentWorkingDir.trim().equals("")) { //$NON-NLS-1$
			File path = new File(currentWorkingDir);
			if (path.exists()) {
				dialog.setFilterPath(currentWorkingDir);
			}
		}
		
		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			fTomcatDir.setText(selectedDirectory);
		}
	}
					
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		// empty implementation
	}
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		setErrorMessage(null);
		setMessage(null);
		
		String workingDirPath = fTomcatDir.getText().trim();
		// resolve variables (if any)
		String expansion;
		try {
			expansion = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(workingDirPath);
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
			return false;
		}
		if (workingDirPath.length() > 0) {
			File dir = new File(expansion);
			if (!dir.exists()) {
				setErrorMessage(LaunchingMessages.TomcatTab_5);
				return false;
			}
			if (!dir.isDirectory()) {
				setErrorMessage(LaunchingMessages.TomcatTab_6);
				return false;
			}
		}
		
		String projectName = fProjectText.getText().trim();
		if (projectName.length() > 0) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (!project.exists()) {
				setErrorMessage(LaunchingMessages.TomcatTab_30);
				return false;
			}
		}
					
		return true;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(TomcatLaunchDelegate.ATTR_CATALINA_HOME, "${catalina_home}"); //$NON-NLS-1$
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, TomcatLaunchDelegate.ID_TOMCAT_CLASSPATH_PROVIDER);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "org.apache.catalina.startup.Bootstrap"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fTomcatDir.setText(configuration.getAttribute(TomcatLaunchDelegate.ATTR_CATALINA_HOME, "")); //$NON-NLS-1$
			fProjectText.setText(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")); //$NON-NLS-1$
			if (configuration.isWorkingCopy()) {
				// set VM args
				ILaunchConfigurationWorkingCopy workingCopy = (ILaunchConfigurationWorkingCopy)configuration;
				String home = TomcatLaunchDelegate.getCatalinaHome();
				IPath endorsed = new Path(home).append("common").append("endorsed");  //$NON-NLS-1$//$NON-NLS-2$
				IPath temp = new Path(home).append("temp"); //$NON-NLS-1$
				StringBuffer args = new StringBuffer();
				args.append("-Djava.endorsed.dirs=\""); //$NON-NLS-1$
				args.append(endorsed.toOSString());
				args.append("\" "); //$NON-NLS-1$
				args.append("-Dcatalina.base=\""); //$NON-NLS-1$
				args.append(home);
				args.append("\" "); //$NON-NLS-1$
				args.append("-Dcatalina.home=\""); //$NON-NLS-1$
				args.append(home);
				args.append("\" "); //$NON-NLS-1$
				args.append("-Djava.io.tmpdir=\""); //$NON-NLS-1$
				args.append(temp.toOSString());
				args.append("\"");  //$NON-NLS-1$
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, args.toString());
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "start"); //$NON-NLS-1$
			}
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
			DebugPlugin.log(e);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(TomcatLaunchDelegate.ATTR_CATALINA_HOME, getAttributeValueFrom(fTomcatDir));
		String projectName = getAttributeValueFrom(fProjectText);
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
	}

	/**
	 * Returns the string in the text widget, or <code>null</code> if empty.
	 * 
	 * @param text the text field
	 * @return text or <code>null</code>
	 */
	protected String getAttributeValueFrom(Text text) {
		String content = text.getText().trim();
		if (content.length() > 0) {
			return content;
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return LaunchingMessages.TomcatTab_7;
	}
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return JspPluginImages.getImage(JspPluginImages.IMG_OBJ_TOMCAT);
	}

}

