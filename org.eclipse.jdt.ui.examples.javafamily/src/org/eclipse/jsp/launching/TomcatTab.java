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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jsp.JspPluginImages;
import org.eclipse.jsp.JspUIPlugin;
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
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * Specifies the install location of Tomcat.
 */
public class TomcatTab extends AbstractLaunchConfigurationTab {
		
		
	// Tomcat location
	private Button fBrowseButton;
	private Text fTomcatDir;
	
	// WebApp location
	private Button fWebAppButton;
	private Text fWebAppDir;
	
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
		
		fBrowseButton = createPushButton(composite, LaunchingMessages.getString("TomcatTab.21"), null); //$NON-NLS-1$
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleTomcatBrowseButtonSelected();
			}
		});
		
		createVerticalSpacer(composite, 3);
		
		label = new Label(composite, SWT.NONE);
		label.setText(LaunchingMessages.getString("TomcatTab.22")); //$NON-NLS-1$
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		label.setFont(font);
				
		fWebAppDir = new Text(composite, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fWebAppDir.setLayoutData(gd);
		fWebAppDir.setFont(font);
		fWebAppDir.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fWebAppButton = createPushButton(composite, LaunchingMessages.getString("TomcatTab.23"), null); //$NON-NLS-1$
		fWebAppButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleWebAppBrowseButtonSelected();
			}
		});		
	}
		
	/**
	 * Show a dialog that lets the user select a root WebApp directory
	 * from the workspace
	 */
	protected void handleWebAppBrowseButtonSelected() {
		ISelectionStatusValidator validator= new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection.length != 1) {
					return new Status(IStatus.ERROR, JspUIPlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, LaunchingMessages.getString("TomcatTab.24"), null); //$NON-NLS-1$
				}
				if (!(selection[0] instanceof IContainer)) {
					return new Status(IStatus.ERROR, JspUIPlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, LaunchingMessages.getString("TomcatTab.25"), null); //$NON-NLS-1$
				}
				// check for "WEB-INF"
				IContainer container = (IContainer)selection[0];
				if (!container.getFolder(new Path("WEB-INF")).exists()) { //$NON-NLS-1$
					return new Status(IStatus.WARNING, JspUIPlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, LaunchingMessages.getString("TomcatTab.27"), null); //$NON-NLS-1$
				}		
				return new Status(IStatus.OK, JspUIPlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, "", null); //$NON-NLS-1$
			}			
		};
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return element instanceof IContainer;
			}
		});
		dialog.setValidator(validator);
		dialog.setTitle(LaunchingMessages.getString("TomcatTab.28")); //$NON-NLS-1$
		dialog.setMessage(LaunchingMessages.getString("TomcatTab.29")); //$NON-NLS-1$
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());	

		if (dialog.open() == Window.OK) {
			Object[] elements= dialog.getResult();
			if (elements != null && elements.length == 1) {
				fWebAppDir.setText(((IResource)elements[0]).getFullPath().toString());
			}
		}
		
	}

	/**
	 * Show a dialog that lets the user select a tomcat install directory
	 */
	protected void handleTomcatBrowseButtonSelected() {
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
				setErrorMessage(LaunchingMessages.getString("TomcatTab.5")); //$NON-NLS-1$
				return false;
			}
			if (!dir.isDirectory()) {
				setErrorMessage(LaunchingMessages.getString("TomcatTab.6")); //$NON-NLS-1$
				return false;
			}
		}	
		
		String webappDirPath = fWebAppDir.getText().trim();
		if (webappDirPath.length() > 0) {
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(webappDirPath));
			if (resource == null) {
				setErrorMessage(LaunchingMessages.getString("TomcatTab.30")); //$NON-NLS-1$
				return false;
			}
			if (!(resource instanceof IContainer)) {
				setErrorMessage(LaunchingMessages.getString("TomcatTab.31")); //$NON-NLS-1$
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
			fWebAppDir.setText(configuration.getAttribute(TomcatLaunchDelegate.ATTR_WEB_APP_ROOT, "")); //$NON-NLS-1$
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
		String webApp = getAttributeValueFrom(fWebAppDir);
		configuration.setAttribute(TomcatLaunchDelegate.ATTR_WEB_APP_ROOT, webApp);
		// set project (if there is one)
		if (webApp != null) {
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(webApp));
			if (resource != null) {
				configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, resource.getProject().getName());
			}
		}		
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

