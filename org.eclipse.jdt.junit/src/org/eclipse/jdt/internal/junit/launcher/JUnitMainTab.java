package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
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
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
	private Label fTestLabel;
	private Text fTestText;
	private Button fSearchButton;
	private final Image fTestIcon= createImage("obj16/test.gif"); //$NON-NLS-1$
	private Label fTestMethodLabel;
	private Text fContainerText;
	private IJavaElement fContainerElement;

	private Button fContainerSearchButton;
	private Button fTestContainerRadioButton;
	private Button fTestRadioButton; 
				
	/**
	 * @see ILaunchConfigurationTab#createControl(TabItem)
	 */
	public void createControl(Composite parent) {		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
		topLayout.numColumns= 2;
		comp.setLayout(topLayout);		
		
		new Label(comp, SWT.NONE);
		
		createProjectGroup(comp);
		createTestSelectionGroup(comp);
		createTestContainerSelectionGroup(comp);
		createKeepAliveGroup(comp);
	}

	private void createTestContainerSelectionGroup(Composite comp) {
		GridData gd;
		fTestContainerRadioButton= new Button(comp, SWT.RADIO);
		fTestContainerRadioButton.setText(JUnitMessages.getString("JUnitMainTab.label.container")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
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
		gd.horizontalIndent= 20;
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
		if (javaElement != null) {
			fContainerElement= javaElement;
			fContainerText.setText(javaElement.getElementName());
		}
	}

	public void createKeepAliveGroup(Composite comp) {
		GridData gd;
		fKeepRunning = new Button(comp, SWT.CHECK);
		fKeepRunning.setText(JUnitMessages.getString("JUnitMainTab.label.keeprunning")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalSpan= 2;
		fKeepRunning.setLayoutData(gd);
	}

	public void createTestSelectionGroup(Composite comp) {
		GridData gd;
		fTestRadioButton= new Button(comp, SWT.RADIO /*| SWT.LEFT*/);
		fTestRadioButton.setText(JUnitMessages.getString("JUnitMainTab.label.test")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fTestRadioButton.setLayoutData(gd); 
		fTestRadioButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (fTestRadioButton.getSelection())
					testModeChanged();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	
		fTestText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= 20;
		fTestText.setLayoutData(gd);
		fTestText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
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
		fTestMethodLabel= new Label(comp, SWT.NONE);
		fTestMethodLabel.setText("");  //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalSpan = 2;
		gd.horizontalIndent= 20;
		fTestMethodLabel.setLayoutData(gd);
	}

	public void createProjectGroup(Composite comp) {
		GridData gd;
		fProjLabel = new Label(comp, SWT.NONE);
		fProjLabel.setText(JUnitMessages.getString("JUnitMainTab.label.project")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalSpan = 2;
		fProjLabel.setLayoutData(gd);
		
		fProjText= new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjText.setLayoutData(gd);
		fProjText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
				boolean isSingleTestMode= fTestRadioButton.getSelection();				
				fSearchButton.setEnabled(isSingleTestMode && fProjText.getText().length() > 0);
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
		String testMethodName= ""; //$NON-NLS-1$
		try {
			testTypeName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""); //$NON-NLS-1$
			testMethodName = config.getAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, ""); //$NON-NLS-1$
		} catch (CoreException ce) {			
		}
		fTestRadioButton.setSelection(true);
		setEnableSingleTestGroup(true);
		setEnableContainerTestGroup(false);		
		fTestContainerRadioButton.setSelection(false);
		fTestText.setText(testTypeName);
		fContainerText.setText(""); //$NON-NLS-1$
		if (!"".equals(testMethodName)) { //$NON-NLS-1$
			fTestMethodLabel.setText(JUnitMessages.getString("JUnitMainTab.label.method")+testMethodName); //$NON-NLS-1$
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
			fContainerText.setText(fContainerElement.getElementName());
		fTestText.setText(""); //$NON-NLS-1$
	}
	/**
	 * @see ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)fProjText.getText());
		if (fTestContainerRadioButton.getSelection() && fContainerElement != null) {
			config.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, fContainerElement.getHandleIdentifier());
			//bug 26293
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""); //$NON-NLS-1$
		} else {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)fTestText.getText());
			//bug 26293
			config.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, ""); //$NON-NLS-1$			
		}
		config.setAttribute(JUnitBaseLaunchConfiguration.ATTR_KEEPRUNNING, fKeepRunning.getSelection());
	}

	/**
	 * @see ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		super.dispose();
		fTestIcon.dispose();
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
		SelectionDialog dialog = new TestSelectionDialog(shell, new ProgressMonitorDialog(shell), javaProject);
		dialog.setTitle(JUnitMessages.getString("JUnitMainTab.testdialog.title")); //$NON-NLS-1$
		dialog.setMessage(JUnitMessages.getString("JUnitMainTab.testdialog.message")); //$NON-NLS-1$
		if (dialog.open() == SelectionDialog.CANCEL) {
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
		if (dialog.open() == ElementListSelectionDialog.OK) {			
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
		setErrorMessage(null);
		setMessage(null);
		
		String projectName = fProjText.getText().trim();
		if (projectName.length() > 0) {
			if (!ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).exists()) {
				setErrorMessage(JUnitMessages.getString("JUnitMainTab.error.projectnotexists")); //$NON-NLS-1$
				return false;
			}
		}

		String testName = fTestText.getText().trim();
		if (testName.length() == 0 && fContainerElement == null) {
			setErrorMessage(JUnitMessages.getString("JUnitMainTab.error.testnotdefined")); //$NON-NLS-1$
			return false;
		}
		// TO DO should verify that test exists
		return true;
	}
	
	private void testModeChanged() {
		boolean isSingleTestMode= fTestRadioButton.getSelection();
		setEnableSingleTestGroup(isSingleTestMode);
		setEnableContainerTestGroup(!isSingleTestMode);
	}

	private void setEnableContainerTestGroup(boolean enabled) {
		fContainerSearchButton.setEnabled(enabled);
		fContainerText.setEnabled(enabled);
	}

	private void setEnableSingleTestGroup(boolean enabled) {
		fSearchButton.setEnabled(enabled && fProjText.getText().length() > 0);
		fTestText.setEnabled(enabled);
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
		
		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			Object element= dialog.getFirstResult();
			return (IJavaElement)element;
		}
		return null;
	}
}
