/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de bug: 26293, 27889 
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

 
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

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
	private Text fTestText;
	private Button fSearchButton;
	private final Image fTestIcon= createImage("obj16/test.gif"); //$NON-NLS-1$
	private String fOriginalTestMethodName;
	private Label fTestMethodLabel;
	private Text fContainerText;
	private IJavaElement fContainerElement;
	private final ILabelProvider fJavaElementLabelProvider= new JavaElementLabelProvider();

	private Button fContainerSearchButton;
	private Button fTestContainerRadioButton;
	private Button fTestRadioButton;
	private Label fTestLabel; 
	
	/**
	 * @see ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
		topLayout.numColumns= 3;
		comp.setLayout(topLayout);		
		
		Label label = new Label(comp, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		createSingleTestSection(comp);
		createTestContainerSelectionGroup(comp);
		
		label = new Label(comp, SWT.NONE);
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		createKeepAliveGroup(comp);
		Dialog.applyDialogFont(comp);
		validatePage();
	}
	
	protected void createSingleTestSection(Composite comp) {
		fTestRadioButton= new Button(comp, SWT.RADIO);
		fTestRadioButton.setText(JUnitMessages.getString("JUnitMainTab.label.oneTest")); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fTestRadioButton.setLayoutData(gd); 
		fTestRadioButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fTestRadioButton.getSelection())
					testModeChanged();
			}
		});
		
		fProjLabel = new Label(comp, SWT.NONE);
		fProjLabel.setText(JUnitMessages.getString("JUnitMainTab.label.project")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalIndent = 25;
		fProjLabel.setLayoutData(gd);
		
		fProjText= new Text(comp, SWT.SINGLE | SWT.BORDER);
		fProjText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProjText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				validatePage();
				updateLaunchConfigurationDialog();				
				fSearchButton.setEnabled(fTestRadioButton.getSelection() && fProjText.getText().length() > 0);
			}
		});
			
		fProjButton = new Button(comp, SWT.PUSH);
		fProjButton.setText(JUnitMessages.getString("JUnitMainTab.label.browse")); //$NON-NLS-1$
		fProjButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleProjectButtonSelected();
			}
		});
		setButtonGridData(fProjButton);
		
		fTestLabel = new Label(comp, SWT.NONE);
		gd = new GridData();
		gd.horizontalIndent = 25;
		fTestLabel.setLayoutData(gd);
		fTestLabel.setText(JUnitMessages.getString("JUnitMainTab.label.test")); //$NON-NLS-1$
		
	
		fTestText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		fTestText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTestText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				validatePage();
				updateLaunchConfigurationDialog();
			}
		});
		
		fSearchButton = new Button(comp, SWT.PUSH);
		fSearchButton.setEnabled(fProjText.getText().length() > 0);		
		fSearchButton.setText(JUnitMessages.getString("JUnitMainTab.label.search")); //$NON-NLS-1$
		fSearchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleSearchButtonSelected();
			}
		});
		setButtonGridData(fSearchButton);
		
		new Label(comp, SWT.NONE);
		
		fTestMethodLabel= new Label(comp, SWT.NONE);
		fTestMethodLabel.setText("");  //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalSpan = 2;
		fTestMethodLabel.setLayoutData(gd);
		
	}

	protected void createTestContainerSelectionGroup(Composite comp) {
		fTestContainerRadioButton= new Button(comp, SWT.RADIO);
		fTestContainerRadioButton.setText(JUnitMessages.getString("JUnitMainTab.label.containerTest")); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fTestContainerRadioButton.setLayoutData(gd);
		fTestContainerRadioButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (fTestContainerRadioButton.getSelection())
					testModeChanged();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		fContainerText = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= 25;
		gd.horizontalSpan = 2;
		fContainerText.setLayoutData(gd);
		fContainerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fContainerSearchButton = new Button(comp, SWT.PUSH);
		fContainerSearchButton.setText(JUnitMessages.getString("JUnitMainTab.label.search")); //$NON-NLS-1$
		fContainerSearchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleContainerSearchButtonSelected();
			}
		});
		setButtonGridData(fContainerSearchButton);	
	}

	private void handleContainerSearchButtonSelected() {
		IJavaElement javaElement= chooseContainer(fContainerElement);
		if (javaElement != null)
			setContainerElement(javaElement);
	}

	private void setContainerElement(IJavaElement javaElement) {
		fContainerElement= javaElement;
		fContainerText.setText(getPresentationName(javaElement));
		validatePage();
		updateLaunchConfigurationDialog();
	}

	public void createKeepAliveGroup(Composite comp) {
		GridData gd;
		fKeepRunning = new Button(comp, SWT.CHECK);
		fKeepRunning.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		fKeepRunning.setText(JUnitMessages.getString("JUnitMainTab.label.keeprunning")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalSpan= 2;
		fKeepRunning.setLayoutData(gd);
	}


	
	protected static Image createImage(String path) {
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(JUnitPlugin.makeIconFileURL(path));
			return id.createImage();
		} catch (MalformedURLException e) {
			// fall through
		}  
		return null;
	}


	/**
	 * @see ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		updateProjectFromConfig(config);
		String containerHandle= ""; //$NON-NLS-1$
		try {
			containerHandle = config.getAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, ""); //$NON-NLS-1$
		} catch (CoreException ce) {			
		}
		
		if (containerHandle.length() > 0)
			updateTestContainerFromConfig(config);
		else
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
		String projectName= ""; //$NON-NLS-1$
		try {
			projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
		}
		fProjText.setText(projectName);
	}
	
	protected void updateTestTypeFromConfig(ILaunchConfiguration config) {
		String testTypeName= ""; //$NON-NLS-1$
		fOriginalTestMethodName= ""; //$NON-NLS-1$
		try {
			testTypeName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""); //$NON-NLS-1$
			fOriginalTestMethodName = config.getAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, ""); //$NON-NLS-1$
		} catch (CoreException ce) {			
		}
		fTestRadioButton.setSelection(true);
		setEnableSingleTestGroup(true);
		setEnableContainerTestGroup(false);		
		fTestContainerRadioButton.setSelection(false);
		fTestText.setText(testTypeName);
		fContainerText.setText(""); //$NON-NLS-1$
		setTestMethodLabel(fOriginalTestMethodName);
	}

	private void setTestMethodLabel(String testMethodName) {
		if (!"".equals(testMethodName)) { //$NON-NLS-1$
			fTestMethodLabel.setText(JUnitMessages.getString("JUnitMainTab.label.method")+fOriginalTestMethodName); //$NON-NLS-1$
		} else {
			fTestMethodLabel.setText(""); //$NON-NLS-1$
		}
	}

	protected void updateTestContainerFromConfig(ILaunchConfiguration config) {
		String containerHandle= ""; //$NON-NLS-1$
		try {
			containerHandle = config.getAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, ""); //$NON-NLS-1$
			if (containerHandle.length() > 0) {
				fContainerElement= JavaCore.create(containerHandle);
			}
		} catch (CoreException ce) {			
		}
		fTestContainerRadioButton.setSelection(true);
		setEnableSingleTestGroup(false);
		setEnableContainerTestGroup(true);				
		fTestRadioButton.setSelection(false);
		if (fContainerElement != null)
			fContainerText.setText(getPresentationName(fContainerElement));
		fTestText.setText(""); //$NON-NLS-1$
	}
	/**
	 * @see ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (fTestContainerRadioButton.getSelection() && fContainerElement != null) {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fContainerElement.getJavaProject().getElementName());
			config.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, fContainerElement.getHandleIdentifier());
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""); //$NON-NLS-1$
		} else {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText());
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, fTestText.getText());
			config.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, ""); //$NON-NLS-1$			
		}
		config.setAttribute(JUnitBaseLaunchConfiguration.ATTR_KEEPRUNNING, fKeepRunning.getSelection());
		config.setAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, fOriginalTestMethodName);
		String testMethod= config.contentsEqual(config.getOriginal()) //workaround for bug 65399
			? fOriginalTestMethodName
			: ""; //$NON-NLS-1$
		config.setAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, testMethod);
		setTestMethodLabel(testMethod);
	}

	/**
	 * @see ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		super.dispose();
		fTestIcon.dispose();
		fJavaElementLabelProvider.dispose();
	}

	/**
	 * @see AbstractLaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fTestIcon;
	}

	/**
	 * Show a dialog that lists all main types
	 */
	protected void handleSearchButtonSelected() {
		Shell shell = getShell();
		
		IJavaProject javaProject = getJavaProject();
		
		IType[] types= new IType[0];
		boolean[] radioSetting= new boolean[2];
		try {
			// fix for 66922 Wrong radio behaviour when switching
			// remember the selected radio button
			radioSetting[0]= fTestRadioButton.getSelection();
			radioSetting[1]= fTestContainerRadioButton.getSelection();
			
			types= TestSearchEngine.findTests(getLaunchConfigurationDialog(), new Object[] {javaProject}); 
		} catch (InterruptedException e) {
			setErrorMessage(e.getMessage());
			return;
		} catch (InvocationTargetException e) {
			JUnitPlugin.log(e.getTargetException());
			return;
		} finally {
			fTestRadioButton.setSelection(radioSetting[0]);
			fTestContainerRadioButton.setSelection(radioSetting[1]);
		}

		SelectionDialog dialog = new TestSelectionDialog(shell, types);
		dialog.setTitle(JUnitMessages.getString("JUnitMainTab.testdialog.title")); //$NON-NLS-1$
		dialog.setMessage(JUnitMessages.getString("JUnitMainTab.testdialog.message")); //$NON-NLS-1$
		if (dialog.open() == Window.CANCEL) {
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
		dialog.setTitle(JUnitMessages.getString("JUnitMainTab.projectdialog.title")); //$NON-NLS-1$
		dialog.setMessage(JUnitMessages.getString("JUnitMainTab.projectdialog.message")); //$NON-NLS-1$
		dialog.setElements(projects);
		
		IJavaProject javaProject = getJavaProject();
		if (javaProject != null) {
			dialog.setInitialSelections(new Object[] { javaProject });
		}
		if (dialog.open() == Window.OK) {			
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
	 * @see ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {		
		return getErrorMessage() == null;
	}
	
	private void testModeChanged() {
		boolean isSingleTestMode= fTestRadioButton.getSelection();
		setEnableSingleTestGroup(isSingleTestMode);
		setEnableContainerTestGroup(!isSingleTestMode);
		if (!isSingleTestMode && fContainerText.getText().length() == 0) {
			IJavaProject javaProject= getJavaModel().getJavaProject(fProjText.getText());
			if (javaProject != null && javaProject.exists())
				setContainerElement(javaProject);
		}
		validatePage();
		updateLaunchConfigurationDialog();
	}

	private void validatePage() {
		setErrorMessage(null);
		setMessage(null);
		
		if (fTestContainerRadioButton.getSelection()) {
			if (fContainerElement == null)
				setErrorMessage(JUnitMessages.getString("JUnitMainTab.error.noContainer")); //$NON-NLS-1$
			return;
		} 
		
		String projectName = fProjText.getText().trim();
		if (projectName.length() == 0) {
			setErrorMessage(JUnitMessages.getString("JUnitMainTab.error.projectnotdefined")); //$NON-NLS-1$
			return;
		}
			
		IProject project = getWorkspaceRoot().getProject(projectName);
		if (!project.exists()) {
			setErrorMessage(JUnitMessages.getString("JUnitMainTab.error.projectnotexists")); //$NON-NLS-1$
			return;
		}
		
		try {
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				setErrorMessage(JUnitMessages.getString("JUnitMainTab.error.notJavaProject")); //$NON-NLS-1$
				return;
			}
			IJavaProject jProject = getJavaProject();
			String className = fTestText.getText().trim();
			if (className.length() == 0) {
				setErrorMessage(JUnitMessages.getString("JUnitMainTab.error.testnotdefined")); //$NON-NLS-1$
				return;
			}
		} catch (Exception e) {
		}
	}

	private void setEnableContainerTestGroup(boolean enabled) {
		fContainerSearchButton.setEnabled(enabled);
		fContainerText.setEnabled(enabled);
	}

	private void setEnableSingleTestGroup(boolean enabled) {
		fProjLabel.setEnabled(enabled);
		fProjText.setEnabled(enabled);
		fProjButton.setEnabled(enabled);
		fTestLabel.setEnabled(enabled);
		fTestText.setEnabled(enabled);
		fSearchButton.setEnabled(enabled && fProjText.getText().length() > 0);
		fTestMethodLabel.setEnabled(enabled);
	}

	/**
	 * @see ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IJavaElement javaElement = getContext();
		if (javaElement != null) {
			initializeJavaProject(javaElement, config);
		} else {
			// We set empty attributes for project & main type so that when one config is
			// compared to another, the existence of empty attributes doesn't cause an
			// incorrect result (the performApply() method can result in empty values
			// for these attributes being set on a config if there is nothing in the
			// corresponding text boxes)
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
			config.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, ""); //$NON-NLS-1$
		}
		initializeTestAttributes(javaElement, config);
	}

	private void initializeTestAttributes(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		if (javaElement != null && javaElement.getElementType() < IJavaElement.COMPILATION_UNIT) 
			initializeTestContainer(javaElement, config);
		else
			initializeTestType(javaElement, config);
	}

	private void initializeTestContainer(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, javaElement.getHandleIdentifier());
		initializeName(config, javaElement.getElementName());
	}

	private void initializeName(ILaunchConfigurationWorkingCopy config, String name) {
		if (name == null) {
			name= ""; //$NON-NLS-1$
		}
		if (name.length() > 0) {
			int index = name.lastIndexOf('.');
			if (index > 0) {
				name = name.substring(index + 1);
			}
			name= getLaunchConfigurationDialog().generateName(name);
			config.rename(name);
		}
	}

	/**
	 * Set the main type & name attributes on the working copy based on the IJavaElement
	 */
	protected void initializeTestType(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		String name= ""; //$NON-NLS-1$
		try {
			// we only do a search for compilation units or class files or 
			// or source references
			if ((javaElement instanceof ICompilationUnit) || 
				(javaElement instanceof ISourceReference) ||
				(javaElement instanceof IClassFile)) {
		
				IType[] types = TestSearchEngine.findTests(new Object[] {javaElement});
				if ((types == null) || (types.length < 1)) {
					return;
				}
				// Simply grab the first main type found in the searched element
				name = types[0].getFullyQualifiedName();
			}	
		} catch (InterruptedException ie) {
		} catch (InvocationTargetException ite) {
		}
		if (name == null)
			name= ""; //$NON-NLS-1$
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, name);
		initializeName(config, name);
	}
	
	/**
	 * @see ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return JUnitMessages.getString("JUnitMainTab.tab.label"); //$NON-NLS-1$
	}
	
	private IJavaElement chooseContainer(IJavaElement initElement) {
		Class[] acceptedClasses= new Class[] { IPackageFragmentRoot.class, IJavaProject.class, IPackageFragment.class };
		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false) {
			public boolean isSelectedValid(Object element) {
				return true;
			}
		};
		
		acceptedClasses= new Class[] { IJavaModel.class, IPackageFragmentRoot.class, IJavaProject.class, IPackageFragment.class };
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses) {
			public boolean select(Viewer viewer, Object parent, Object element) {
				return super.select(viewer, parent, element);
			}
		};		

		StandardJavaElementContentProvider provider= new StandardJavaElementContentProvider();
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT); 
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), labelProvider, provider);
		dialog.setValidator(validator);
		dialog.setSorter(new JavaElementSorter());
		dialog.setTitle(JUnitMessages.getString("JUnitMainTab.folderdialog.title"));  //$NON-NLS-1$
		dialog.setMessage(JUnitMessages.getString("JUnitMainTab.folderdialog.message"));  //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(JavaCore.create(getWorkspaceRoot()));
		dialog.setInitialSelection(initElement);
		dialog.setAllowMultiple(false);
		
		if (dialog.open() == Window.OK) {
			Object element= dialog.getFirstResult();
			return (IJavaElement)element;
		}
		return null;
	}
	
	private String getPresentationName(IJavaElement element) {
		return fJavaElementLabelProvider.getText(element);
	}
}
