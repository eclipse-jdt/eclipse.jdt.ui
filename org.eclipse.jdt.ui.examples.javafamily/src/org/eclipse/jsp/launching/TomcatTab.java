/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp.launching;

 
import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.variables.LaunchVariableUtil;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
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

/**
 * Specifies the install location of Tomcat.
 */
public class TomcatTab extends AbstractLaunchConfigurationTab {
		
		
	private Button fBrowseButton;

	private Text fTomcatDir;

	public TomcatTab() {

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
		label.setText(LaunchingMessages.getString("TomcatTab.3")); //$NON-NLS-1$
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
		
		fBrowseButton = createPushButton(composite, "&Browse...", null); //$NON-NLS-1$
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleBrowseButtonSelected();
			}
		});
		
	}
		
	/**
	 * Show a dialog that lets the user select a directory
	 */
	protected void handleBrowseButtonSelected() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setMessage(LaunchingMessages.getString("TomcatTab.4")); //$NON-NLS-1$
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
	}
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		setErrorMessage(null);
		setMessage(null);
		
		String workingDirPath = fTomcatDir.getText().trim();
		// resolve variables (if any)
		String[] expansion = LaunchVariableUtil.expandStrings(workingDirPath, null, null);
		if (expansion.length == 1) {
			String path = expansion[0];
			if (path != null) {
				workingDirPath = path;
			}
		}
		if (workingDirPath.length() > 0) {
			File dir = new File(workingDirPath);
			if (!dir.exists()) {
				setErrorMessage(LaunchingMessages.getString("TomcatTab.5")); //$NON-NLS-1$
				return false;
			}
			if (!dir.isDirectory()) {
				setErrorMessage(LaunchingMessages.getString("TomcatTab.6")); //$NON-NLS-1$
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
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "org.apache.catalina.startup.Bootstrap");
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fTomcatDir.setText(configuration.getAttribute(TomcatLaunchDelegate.ATTR_CATALINA_HOME, "")); //$NON-NLS-1$
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
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return LaunchingMessages.getString("TomcatTab.7"); //$NON-NLS-1$
	}	
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return JspPluginImages.getImage(JspPluginImages.IMG_OBJ_TOMCAT);
	}	

}

