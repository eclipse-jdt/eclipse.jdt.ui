/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de bug: 26293, 27889
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *     Robert Konigsberg <konigsberg@google.com> - [JUnit] Improve discoverability of the ability to run a single method under JUnit Tests - https://bugs.eclipse.org/bugs/show_bug.cgi?id=285637
 *******************************************************************************/
package org.eclipse.jdt.junit.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchIncludeExcludeTagsDialog;
import org.eclipse.jdt.internal.junit.launcher.JUnitMigrationDelegate;
import org.eclipse.jdt.internal.junit.launcher.TestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.ui.IJUnitHelpContextIds;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;


/**
 * The launch configuration tab for JUnit.
 * <p>
 * This class may be instantiated but is not intended to be subclassed.
 * </p>
 *
 * @since 3.3
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class JUnitLaunchConfigurationTab extends AbstractLaunchConfigurationTab {

	// Project UI widgets
	private Label fProjLabel;

	private Text fProjText;

	private Button fProjButton;

	private Button fKeepRunning;

	// Test class UI widgets
	private Text fTestText;

	private Button fSearchButton;

	private final Image fTestIcon= createImage("obj16/test.svg"); //$NON-NLS-1$

	private String fOriginalTestMethodName;

	private Label fTestMethodLabel;

	private Text fTestMethodText;

	private Button fTestMethodSearchButton;

	private Text fContainerText;

	private IJavaElement fContainerElement;

	private final ILabelProvider fJavaElementLabelProvider= new JavaElementLabelProvider();

	private Button fContainerSearchButton;

	private Button fTestContainerRadioButton;

	private Button fTestRadioButton;

	private Label fTestLabel;

	private Label fIncludeExcludeTagsLabel;

	private Button fIncludeExcludeTagsButton;

	private ComboViewer fTestLoaderViewer;

	private ILaunchConfiguration fLaunchConfiguration;

	private boolean fIsValid= true;

	private TestMethodsCache fTestMethodsCache= new TestMethodsCache();

	/**
	 * Creates a JUnit launch configuration tab.
	 */
	public JUnitLaunchConfigurationTab() {
	}


	@Override
	public void createControl(Composite parent) {
		Composite comp= new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout= new GridLayout();
		topLayout.numColumns= 3;
		comp.setLayout(topLayout);

		createSingleTestSection(comp);
		createSpacer(comp);

		createTestContainerSelectionGroup(comp);
		createSpacer(comp);

		createTagsGroup(comp);
		createSpacer(comp);

		createTestLoaderGroup(comp);
		createSpacer(comp);

		createKeepAliveGroup(comp);
		Dialog.applyDialogFont(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJUnitHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_JUNIT_MAIN_TAB);
		validatePage();
	}

	private void createTagsGroup(Composite comp) {
		GridData gd;

		fIncludeExcludeTagsLabel= new Label(comp, SWT.NONE);
		fIncludeExcludeTagsLabel.setText(JUnitMessages.JUnitLaunchConfigurationTab_addtag_text);
		gd= new GridData();
		gd.horizontalSpan= 1;
		fIncludeExcludeTagsLabel.setLayoutData(gd);
		fIncludeExcludeTagsButton= new Button(comp, SWT.PUSH);
		fIncludeExcludeTagsButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_addtag_label);
		fIncludeExcludeTagsButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				configureIncludeExcludeTags();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		gd= new GridData();
		gd.horizontalSpan= 1;
		fIncludeExcludeTagsButton.setLayoutData(gd);
	}

	private void createTestLoaderGroup(Composite comp) {
		Label loaderLabel= new Label(comp, SWT.NONE);
		loaderLabel.setText(JUnitMessages.JUnitLaunchConfigurationTab_Test_Loader);
		GridData gd= new GridData();
		gd.horizontalIndent= 0;
		loaderLabel.setLayoutData(gd);

		fTestLoaderViewer= new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		fTestLoaderViewer.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		ArrayList<TestKind> items= TestKindRegistry.getDefault().getAllKinds();
		fTestLoaderViewer.setContentProvider(ArrayContentProvider.getInstance());
		fTestLoaderViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((TestKind) element).getDisplayName();
			}
		});
		fTestLoaderViewer.setInput(items);
		fTestLoaderViewer.addSelectionChangedListener(event -> {
			setEnableTagsGroup(event);
			try (var __= fTestMethodsCache.runNestedCancelable()) {
				calculateMethodsCache();
				validatePage();
			}
			updateLaunchConfigurationDialog();
		});
	}

	private void setEnableTagsGroup(SelectionChangedEvent event) {
		ISelection selection= event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object first= ss.getFirstElement();
				if (first instanceof ITestKind) {
					boolean isJUnit5= TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(((ITestKind) first).getId());
					fIncludeExcludeTagsButton.setEnabled(isJUnit5);
				}
			}
		}
	}

	private void createSpacer(Composite comp) {
		Label label= new Label(comp, SWT.NONE);
		GridData gd= new GridData();
		gd.horizontalSpan= 3;
		label.setLayoutData(gd);
	}

	private void createSingleTestSection(Composite comp) {
		fTestRadioButton= new Button(comp, SWT.RADIO);
		fTestRadioButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_oneTest);
		GridData gd= new GridData();
		gd.horizontalSpan= 3;
		fTestRadioButton.setLayoutData(gd);
		fTestRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fTestRadioButton.getSelection())
					testModeChanged();
			}
		});

		fProjLabel= new Label(comp, SWT.NONE);
		fProjLabel.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_project);
		gd= new GridData();
		gd.horizontalIndent= 25;
		fProjLabel.setLayoutData(gd);

		fProjText= new Text(comp, SWT.SINGLE | SWT.BORDER);
		fProjText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProjText.addModifyListener(evt -> {
			try (var __= fTestMethodsCache.runNestedCancelable()) {
				calculateMethodsCache();
				validatePage();
			}
			updateLaunchConfigurationDialog();
			fSearchButton.setEnabled(fTestRadioButton.getSelection() && fProjText.getText().length() > 0);
		});

		fProjButton= new Button(comp, SWT.PUSH);
		fProjButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_browse);
		fProjButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleProjectButtonSelected();
			}
		});
		setButtonGridData(fProjButton);

		fTestLabel= new Label(comp, SWT.NONE);
		gd= new GridData();
		gd.horizontalIndent= 25;
		fTestLabel.setLayoutData(gd);
		fTestLabel.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_test);


		fTestText= new Text(comp, SWT.SINGLE | SWT.BORDER);
		fTestText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTestText.addModifyListener(evt -> {
			try (var __= fTestMethodsCache.runNestedCancelable()) {
				calculateMethodsCache();
				validatePage();
			}
			updateLaunchConfigurationDialog();
		});

		fSearchButton= new Button(comp, SWT.PUSH);
		fSearchButton.setEnabled(fProjText.getText().length() > 0);
		fSearchButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_search);
		fSearchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleSearchButtonSelected();
			}
		});
		setButtonGridData(fSearchButton);

		fTestMethodLabel= new Label(comp, SWT.NONE);
		gd= new GridData();
		gd.horizontalIndent= 25;
		fTestMethodLabel.setLayoutData(gd);
		fTestMethodLabel.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_method);


		fTestMethodText= new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		fTestMethodText.setLayoutData(gd);

		fTestMethodText.addModifyListener(evt -> {
			validatePage();
			updateLaunchConfigurationDialog();
		});
		fTestMethodText.setMessage(JUnitMessages.JUnitLaunchConfigurationTab_all_methods_text);


		fTestMethodSearchButton= new Button(comp, SWT.PUSH);
		fTestMethodSearchButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_search_method);
		fTestMethodSearchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleTestMethodSearchButtonSelected();
			}
		});

		setButtonGridData(fTestMethodSearchButton);
	}

	private void createTestContainerSelectionGroup(Composite comp) {
		fTestContainerRadioButton= new Button(comp, SWT.RADIO);
		fTestContainerRadioButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_containerTest);
		GridData gd= new GridData();
		gd.horizontalSpan= 3;
		fTestContainerRadioButton.setLayoutData(gd);
		fTestContainerRadioButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fTestContainerRadioButton.getSelection())
					testModeChanged();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		fContainerText= new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		SWTUtil.fixReadonlyTextBackground(fContainerText);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= 25;
		gd.horizontalSpan= 2;
		fContainerText.setLayoutData(gd);
		fContainerText.addModifyListener(evt -> updateLaunchConfigurationDialog());

		fContainerSearchButton= new Button(comp, SWT.PUSH);
		fContainerSearchButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_search);
		fContainerSearchButton.addSelectionListener(new SelectionAdapter() {
			@Override
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

	private void createKeepAliveGroup(Composite comp) {
		GridData gd;
		fKeepRunning= new Button(comp, SWT.CHECK);
		fKeepRunning.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		fKeepRunning.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_keeprunning);
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalSpan= 2;
		fKeepRunning.setLayoutData(gd);
	}

	private static Image createImage(String path) {
		return JUnitPlugin.getImageDescriptor(path).createImage();
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		try (var __= fTestMethodsCache.runNestedCancelable()) {
			fLaunchConfiguration= config;

			updateProjectFromConfig(config);
			String containerHandle= ""; //$NON-NLS-1$
			try {
				containerHandle= config.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, ""); //$NON-NLS-1$
			} catch (CoreException ce) {
			}

			if (containerHandle.length() > 0) {
				updateTestContainerFromConfig(config);
			} else {
				updateTestTypeFromConfig(config);
			}
			updateKeepRunning(config);
			updateTestLoaderFromConfig(config);

			calculateMethodsCache();
			validatePage();
		}
	}


	private void updateTestLoaderFromConfig(ILaunchConfiguration config) {
		ITestKind testKind= JUnitLaunchConfigurationConstants.getTestRunnerKind(config);
		if (testKind.isNull()) {
			if (fContainerElement != null) {
				testKind= TestKindRegistry.getContainerTestKind(fContainerElement);
			}
			if (testKind.isNull()) {
				testKind= TestKindRegistry.getDefault().getKind(TestKindRegistry.JUNIT3_TEST_KIND_ID);
			}
		}
		try (var __= fTestMethodsCache.runNestedCancelable()) {
			fTestLoaderViewer.setSelection(new StructuredSelection(testKind));
		}
	}

	private TestKind getSelectedTestKind() {
		IStructuredSelection selection= (IStructuredSelection) fTestLoaderViewer.getSelection();
		return (TestKind) selection.getFirstElement();
	}

	private void updateKeepRunning(ILaunchConfiguration config) {
		boolean running= false;
		try {
			running= config.getAttribute(JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING, false);
		} catch (CoreException ce) {
		}
		fKeepRunning.setSelection(running);
	}

	private void updateProjectFromConfig(ILaunchConfiguration config) {
		String projectName= ""; //$NON-NLS-1$
		try {
			projectName= config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
		}
		try (var __= fTestMethodsCache.runNestedCancelable()) {
			fProjText.setText(projectName);
		}
	}

	private void updateTestTypeFromConfig(ILaunchConfiguration config) {
		String testTypeName= ""; //$NON-NLS-1$
		fOriginalTestMethodName= ""; //$NON-NLS-1$
		try {
			testTypeName= config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""); //$NON-NLS-1$
			fOriginalTestMethodName= config.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
		}
		fTestRadioButton.setSelection(true);
		setEnableSingleTestGroup(true);
		setEnableContainerTestGroup(false);
		fTestContainerRadioButton.setSelection(false);

		try (var __= fTestMethodsCache.runNestedCancelable()) {
			fTestText.setText(testTypeName);
			fContainerText.setText(""); //$NON-NLS-1$
			fTestMethodText.setText(fOriginalTestMethodName);
		}
	}

	private void updateTestContainerFromConfig(ILaunchConfiguration config) {
		String containerHandle= ""; //$NON-NLS-1$
		IJavaElement containerElement= null;
		try {
			containerHandle= config.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, ""); //$NON-NLS-1$
			if (containerHandle.length() > 0) {
				containerElement= JavaCore.create(containerHandle);
			}
		} catch (CoreException ce) {
		}
		if (containerElement != null)
			fContainerElement= containerElement;
		fTestContainerRadioButton.setSelection(true);
		setEnableSingleTestGroup(false);
		setEnableContainerTestGroup(true);
		fTestRadioButton.setSelection(false);
		if (fContainerElement != null)
			fContainerText.setText(getPresentationName(fContainerElement));

		try (var __= fTestMethodsCache.runNestedCancelable()) {
			fTestText.setText(""); //$NON-NLS-1$
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (fTestContainerRadioButton.getSelection() && fContainerElement != null) {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fContainerElement.getJavaProject().getElementName());
			config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, fContainerElement.getHandleIdentifier());
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""); //$NON-NLS-1$
			//workaround for bug 65399
			config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, ""); //$NON-NLS-1$
		} else {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText());
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, fTestText.getText());
			config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, ""); //$NON-NLS-1$
			config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, fTestMethodText.getText());
		}
		config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING, fKeepRunning.getSelection());
		try {
			mapResources(config);
		} catch (CoreException e) {
			JUnitPlugin.log(e.getStatus());
		}
		IStructuredSelection testKindSelection= (IStructuredSelection) fTestLoaderViewer.getSelection();
		if (!testKindSelection.isEmpty()) {
			TestKind testKind= (TestKind) testKindSelection.getFirstElement();
			config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, testKind.getId());
		}
	}

	private void mapResources(ILaunchConfigurationWorkingCopy config) throws CoreException {
		JUnitMigrationDelegate.mapResources(config);
	}


	@Override
	public void dispose() {
		super.dispose();
		fTestIcon.dispose();
		fJavaElementLabelProvider.dispose();
	}

	@Override
	public Image getImage() {
		return fTestIcon;
	}

	/*
	 * Show a dialog that lists all main types
	 */
	private void handleSearchButtonSelected() {
		Shell shell= getShell();

		IJavaProject javaProject= getJavaProject();

		final Set<IType> types;
		boolean[] radioSetting= new boolean[2];
		try {
			// fix for 66922 Wrong radio behaviour when switching
			// remember the selected radio button
			radioSetting[0]= fTestRadioButton.getSelection();
			radioSetting[1]= fTestContainerRadioButton.getSelection();

			types= TestSearchEngine.findTests(getLaunchConfigurationDialog(), javaProject, getSelectedTestKind());
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

		final HashSet<String> typeLookup= new HashSet<>();
		for (IType type : types) {
			typeLookup.add(type.getPackageFragment().getElementName() + '/' + type.getTypeQualifiedName('.'));
		}
		SelectionDialog dialog= null;
		try {
			dialog= JavaUI.createTypeDialog(shell,
					getLaunchConfigurationDialog(),
					SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject }, IJavaSearchScope.SOURCES),
					IJavaElementSearchConstants.CONSIDER_ALL_TYPES,
					false,
					"**", //$NON-NLS-1$
					new TypeSelectionExtension() {
						@Override
						public ITypeInfoFilterExtension getFilterExtension() {
							return requestor -> {
								StringBuilder buf= new StringBuilder();
								buf.append(requestor.getPackageName()).append('/');
								String enclosingName= requestor.getEnclosingName();
								if (enclosingName.length() > 0)
									buf.append(enclosingName).append('.');
								buf.append(requestor.getTypeName());
								return typeLookup.contains(buf.toString());
							};
						}
					});
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
			return;
		}

		dialog.setTitle(JUnitMessages.JUnitLaunchConfigurationTab_testdialog_title);
		dialog.setMessage(JUnitMessages.JUnitLaunchConfigurationTab_testdialog_message);
		if (dialog.open() == Window.CANCEL) {
			return;
		}

		Object[] results= dialog.getResult();
		if ((results == null) || (results.length < 1)) {
			return;
		}
		IType type= (IType) results[0];

		if (type != null) {
			try (var __= fTestMethodsCache.runNestedCancelable()) {
				fTestText.setText(type.getFullyQualifiedName('.'));
				javaProject= type.getJavaProject();
				fProjText.setText(javaProject.getElementName());
			}
		}
	}

	/*
	 * Show a dialog that lets the user select a project.  This in turn provides
	 * context for the main type, allowing the user to key a main type name, or
	 * constraining the search for main types to the specified project.
	 */
	private void handleProjectButtonSelected() {
		IJavaProject project= chooseJavaProject();
		if (project == null) {
			return;
		}

		try (var __= fTestMethodsCache.runNestedCancelable()) {
			String projectName= project.getElementName();
			fProjText.setText(projectName);
		}
	}

	private void handleTestMethodSearchButtonSelected() {
		try (var __= fTestMethodsCache.runNestedCancelable()) {
			IJavaProject javaProject= getJavaProject();
			IType testType= javaProject.findType(fTestText.getText());
			Set<String> methodNames= getMethodsForType(javaProject, testType, getSelectedTestKind());

			// I can't put this logic inside getMethodsForType because that would cause
			// a bug, making it necessary to cancel the search twice when first opening
			// a JUnit configuration
			if (methodNames.isEmpty()) {
				calculateMethodsCache();
				methodNames= getMethodsForType(javaProject, testType, getSelectedTestKind());
			}

			if (fTestMethodsCache.isCanceled()) {
				return;
			}

			String methodName= chooseMethodName(methodNames);

			if (methodName != null) {
				fTestMethodText.setText(methodName);
			}
			validatePage();
			updateLaunchConfigurationDialog();
		} catch (JavaModelException e) {
			JUnitPlugin.log(e.getStatus());
		}
	}

	private Set<String> getMethodsForType(IJavaProject javaProject, IType type, TestKind testKind) {
		if (javaProject == null || type == null || testKind == null)
			return Collections.emptySet();

		String testKindId= testKind.getId();
		String methodsCacheKey= getMethodsCacheKey(javaProject, type, testKindId);
		if (fTestMethodsCache.containsKey(methodsCacheKey)) {
			return fTestMethodsCache.get(methodsCacheKey);
		}

		return Collections.emptySet();
	}

	private void calculateMethodsCache() {
		fTestMethodText.setEnabled(false);

		if (fTestMethodsCache.isCanceled()) {
			return;
		}

		try {
			IJavaProject javaProject= getJavaProject();

			if (javaProject == null) {
				// can't find methods if the project
				return;
			}

			IType testClass= javaProject.findType(fTestText.getText());

			if (testClass == null) {
				// can't find methods if the class doesn't exist
				return;
			}

			TestKind testKind= getSelectedTestKind();

			if (testKind == null) {
				// no need to search for methods if the type (JUnit3/4/5) is not set
				return;
			}

			String methodsCacheKey= getMethodsCacheKey(javaProject, testClass, testKind.getId());

			if (fTestMethodsCache.containsKey(methodsCacheKey)) {
				// no need to recalculate since the source code can't change while the dialog is open.
				fTestMethodText.setEnabled(true);
				return;
			}

			fTestMethodsCache.put(methodsCacheKey, //
					TestSearchEngine.findTestMethods(getLaunchConfigurationDialog(), javaProject, testClass, testKind));

			// calculation successful, reactivate the UI
			fTestMethodText.setEnabled(true);
		} catch (InvocationTargetException | JavaModelException e) {
			JUnitPlugin.log(e);
		} catch (InterruptedException e) {
			// the user probably canceled the operation. Sadly there is no way to know it for sure since ModalContext::run
			// doesn't throw the original OperationCanceledException, it throws a new InterruptedException.
			fTestMethodsCache.setCanceled(true);
		}
	}

	private String getMethodsCacheKey(IJavaProject javaProject, IType type, String testKindId) {
		return javaProject.getElementName() + '\n' + type.getFullyQualifiedName() + '\n' + testKindId;
	}

	private String chooseMethodName(Set<String> methodNames) {
		Shell shell= getShell();

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new LabelProvider());
		dialog.setMessage(Messages.format(JUnitMessages.JUnitLaunchConfigurationTab_select_method_header, fTestText.getText()));
		dialog.setTitle(JUnitMessages.JUnitLaunchConfigurationTab_select_method_title);

		int methodCount= methodNames.size();
		String[] elements= new String[methodCount + 1];
		methodNames.toArray(elements);
		elements[methodCount]= JUnitMessages.JUnitLaunchConfigurationTab_all_methods_text;

		dialog.setElements(elements);

		String methodName= fTestMethodText.getText();

		if (methodNames.contains(methodName)) {
			dialog.setInitialSelections(methodName);
		}

		dialog.setAllowDuplicates(false);
		dialog.setMultipleSelection(false);
		if (dialog.open() == Window.OK) {
			String result= (String) dialog.getFirstResult();
			return (result == null || result.equals(JUnitMessages.JUnitLaunchConfigurationTab_all_methods_text))
					? "" //$NON-NLS-1$
					: result;
		}
		return null;
	}

	/*
	 * Realize a Java Project selection dialog and return the first selected project,
	 * or null if there was none.
	 */
	private IJavaProject chooseJavaProject() {
		IJavaProject[] projects;
		try {
			projects= JavaCore.create(getWorkspaceRoot()).getJavaProjects();
		} catch (JavaModelException e) {
			JUnitPlugin.log(e.getStatus());
			projects= new IJavaProject[0];
		}

		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle(JUnitMessages.JUnitLaunchConfigurationTab_projectdialog_title);
		dialog.setMessage(JUnitMessages.JUnitLaunchConfigurationTab_projectdialog_message);
		dialog.setElements(projects);

		IJavaProject javaProject= getJavaProject();
		if (javaProject != null) {
			dialog.setInitialSelections(javaProject);
		}
		if (dialog.open() == Window.OK) {
			return (IJavaProject) dialog.getFirstResult();
		}
		return null;
	}

	/*
	 * Return the IJavaProject corresponding to the project name in the project name
	 * text field, or null if the text does not match a project name.
	 */
	private IJavaProject getJavaProject() {
		String projectName= fProjText.getText().trim();
		if (projectName.length() < 1) {
			return null;
		}
		return getJavaModel().getJavaProject(projectName);
	}

	/*
	 * Convenience method to get the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/*
	 * Convenience method to get access to the java model.
	 */
	private IJavaModel getJavaModel() {
		return JavaCore.create(getWorkspaceRoot());
	}


	@Override
	public boolean isValid(ILaunchConfiguration config) {
		validatePage();
		return fIsValid;
	}

	private void testModeChanged() {
		boolean isSingleTestMode= fTestRadioButton.getSelection();
		setEnableSingleTestGroup(isSingleTestMode);
		setEnableContainerTestGroup(!isSingleTestMode);
		if (!isSingleTestMode && fContainerText.getText().length() == 0) {
			String projText= fProjText.getText();
			if (Path.EMPTY.isValidSegment(projText)) {
				IJavaProject javaProject= getJavaModel().getJavaProject(projText);
				if (javaProject != null && javaProject.exists())
					setContainerElement(javaProject);
			}
		}
		validatePage();
		updateLaunchConfigurationDialog();
	}

	/*
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#setErrorMessage(java.lang.String)
	 * @since 3.6
	 */
	@Override
	protected void setErrorMessage(String errorMessage) {
		fIsValid= errorMessage == null;
		if (fTestMethodsCache.isCanceled()) {
			super.setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_operation_canceled +
					(errorMessage != null ? (" " + errorMessage) : "")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			super.setErrorMessage(errorMessage);
		}
	}

	private void validatePage() {

		setErrorMessage(null);
		setMessage(null);

		if (fTestContainerRadioButton.getSelection()) {
			if (fContainerElement == null) {
				setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_noContainer);
				return;
			}
			validateJavaProject(fContainerElement.getJavaProject());

		} else {
			String projectName= fProjText.getText().trim();
			if (projectName.length() == 0) {
				setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_projectnotdefined);
				return;
			}

			IStatus status= ResourcesPlugin.getWorkspace().validatePath(IPath.SEPARATOR + projectName, IResource.PROJECT);
			if (!status.isOK() || !Path.ROOT.isValidSegment(projectName)) {
				setErrorMessage(Messages.format(JUnitMessages.JUnitLaunchConfigurationTab_error_invalidProjectName, BasicElementLabels.getResourceName(projectName)));
				return;
			}

			IProject project= getWorkspaceRoot().getProject(projectName);
			if (!project.exists()) {
				setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_projectnotexists);
				return;
			}
			IJavaProject javaProject= JavaCore.create(project);
			validateJavaProject(javaProject);

			try {
				if (!project.hasNature(JavaCore.NATURE_ID)) {
					setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_notJavaProject);
					return;
				}
				String className= fTestText.getText().trim();
				if (className.length() == 0) {
					setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_testnotdefined);
					return;
				}
				IType type= javaProject.findType(className);
				if (type == null) {
					setErrorMessage(Messages.format(JUnitMessages.JUnitLaunchConfigurationTab_error_test_class_not_found, new String[] { className, projectName }));
					return;
				}
				String methodName= fTestMethodText.getText();
				if (methodName.length() > 0) {
					Set<String> methodsForType= getMethodsForType(javaProject, type, getSelectedTestKind());
					if (!methodsForType.contains(methodName)) {
						setErrorMessage(Messages.format(JUnitMessages.JUnitLaunchConfigurationTab_error_test_method_not_found, new String[] { className, methodName, projectName }));
						return;
					}
				}
			} catch (CoreException e) {
				JUnitPlugin.log(e);
			}
		}
	}

	private void validateJavaProject(IJavaProject javaProject) {
		TestKind testKind= getSelectedTestKind();
		if (testKind != null) {
			if (!TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(testKind.getId()) && !CoreTestSearchEngine.hasTestCaseType(javaProject)) {
				setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_testcasenotonpath);
				return;
			}

			String msg= JUnitMessages.JUnitLaunchConfigurationTab_error_testannotationnotonpath;
			if (TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind.getId()) && !CoreTestSearchEngine.hasJUnit4TestAnnotation(javaProject)) {
				setErrorMessage(Messages.format(msg, JUnitCorePlugin.JUNIT4_ANNOTATION_NAME));
				return;
			}
			if (TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(testKind.getId()) && !CoreTestSearchEngine.hasJUnit5TestAnnotation(javaProject)) {
				setErrorMessage(Messages.format(msg, JUnitCorePlugin.JUNIT5_TESTABLE_ANNOTATION_NAME));
				return;
			}
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
		boolean projectTextHasContents= fProjText.getText().length() > 0;
		fSearchButton.setEnabled(enabled && projectTextHasContents);
		fTestMethodLabel.setEnabled(enabled);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IJavaElement javaElement= getContext();
		if (javaElement != null) {
			initializeJavaProject(javaElement, config);
		} else {
			// We set empty attributes for project & main type so that when one config is
			// compared to another, the existence of empty attributes doesn't cause an
			// incorrect result (the performApply() method can result in empty values
			// for these attributes being set on a config if there is nothing in the
			// corresponding text boxes)
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
			config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, ""); //$NON-NLS-1$
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
		config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, javaElement.getHandleIdentifier());
		initializeName(config, javaElement.getElementName());
	}

	private void initializeName(ILaunchConfigurationWorkingCopy config, String name) {
		if (name == null) {
			name= ""; //$NON-NLS-1$
		}
		if (name.length() > 0) {
			IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
			boolean useQualification= preferenceStore.getBoolean(PreferenceConstants.LAUNCH_NAME_FULLY_QUALIFIED_FOR_JUNIT_TEST);
			if (!useQualification) {
				int index= name.lastIndexOf('.');
				if (index > 0) {
					name= name.substring(index + 1);
				}
			}
			name= getLaunchConfigurationDialog().generateName(name);
			config.rename(name);
		}
	}

	/*
	 * Set the main type & name attributes on the working copy based on the IJavaElement
	 */
	private void initializeTestType(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		String name= ""; //$NON-NLS-1$
		String testKindId= null;
		try {
			// we only do a search for compilation units or class files or
			// or source references
			if (javaElement instanceof ISourceReference) {
				ITestKind testKind= TestKindRegistry.getContainerTestKind(javaElement);
				testKindId= testKind.getId();

				var types= TestSearchEngine.findTests(getLaunchConfigurationDialog(), javaElement, testKind);
				if ((types == null) || (types.isEmpty())) {
					return;
				}
				// Simply grab the first main type found in the searched element
				name= types.iterator().next().getFullyQualifiedName('.');

			}
		} catch (InterruptedException | InvocationTargetException ite) {
		}
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, name);
		if (testKindId != null)
			config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, testKindId);
		initializeName(config, name);
		boolean isRunWithJUnitPlatform= TestKindRegistry.isRunWithJUnitPlatform(javaElement);
		if (isRunWithJUnitPlatform) {
			config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_RUN_WITH_JUNIT_PLATFORM_ANNOTATION, true);
		}
	}

	@Override
	public String getName() {
		return JUnitMessages.JUnitLaunchConfigurationTab_tab_label;
	}

	private IJavaElement chooseContainer(IJavaElement initElement) {
		Class<?>[] acceptedClasses= new Class[] { IPackageFragmentRoot.class, IJavaProject.class, IPackageFragment.class };
		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false) {
			@Override
			public boolean isSelectedValid(Object element) {
				return true;
			}
		};

		acceptedClasses= new Class[] { IJavaModel.class, IPackageFragmentRoot.class, IJavaProject.class, IPackageFragment.class };
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses) {
			@Override
			public boolean select(Viewer viewer, Object parent, Object element) {
				if (element instanceof IPackageFragmentRoot && ((IPackageFragmentRoot) element).isArchive())
					return false;
				return super.select(viewer, parent, element);
			}
		};

		StandardJavaElementContentProvider provider= new StandardJavaElementContentProvider() {
			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof IPackageFragment) {
					return false;
				}
				return super.hasChildren(element);
			}
		};
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), labelProvider, provider);
		dialog.setValidator(validator);
		dialog.setComparator(new JavaElementComparator());
		dialog.setTitle(JUnitMessages.JUnitLaunchConfigurationTab_folderdialog_title);
		dialog.setMessage(JUnitMessages.JUnitLaunchConfigurationTab_folderdialog_message);
		dialog.addFilter(filter);
		dialog.setInput(JavaCore.create(getWorkspaceRoot()));
		dialog.setInitialSelection(initElement);
		dialog.setAllowMultiple(false);

		if (dialog.open() == Window.OK) {
			Object element= dialog.getFirstResult();
			return (IJavaElement) element;
		}
		return null;
	}

	private String getPresentationName(IJavaElement element) {
		return fJavaElementLabelProvider.getText(element);
	}

	/*
	 * Returns the current Java element context from which to initialize
	 * default settings, or <code>null</code> if none.
	 *
	 * @return Java element context.
	 */
	private IJavaElement getContext() {
		IWorkbenchWindow activeWorkbenchWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			return null;
		}
		IWorkbenchPage page= activeWorkbenchWindow.getActivePage();
		if (page != null) {
			ISelection selection= page.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss= (IStructuredSelection) selection;
				if (!ss.isEmpty()) {
					Object obj= ss.getFirstElement();
					if (obj instanceof IJavaElement) {
						return (IJavaElement) obj;
					}
					if (obj instanceof IResource) {
						IJavaElement je= JavaCore.create((IResource) obj);
						if (je == null) {
							IProject pro= ((IResource) obj).getProject();
							je= JavaCore.create(pro);
						}
						if (je != null) {
							return je;
						}
					}
				}
			}
			IEditorPart part= page.getActiveEditor();
			if (part != null) {
				IEditorInput input= part.getEditorInput();
				return input.getAdapter(IJavaElement.class);
			}
		}
		return null;
	}

	private void initializeJavaProject(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		IJavaProject javaProject= javaElement.getJavaProject();
		String name= null;
		if (javaProject != null && javaProject.exists()) {
			name= javaProject.getElementName();
		}
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, name);
	}

	private void setButtonGridData(Button button) {
		GridData gridData= new GridData();
		button.setLayoutData(gridData);
		LayoutUtil.setButtonDimensionHint(button);
	}

	@Override
	public String getId() {
		return "org.eclipse.jdt.junit.JUnitLaunchConfigurationTab"; //$NON-NLS-1$
	}

	private void configureIncludeExcludeTags() {
		JUnitLaunchIncludeExcludeTagsDialog dialog= new JUnitLaunchIncludeExcludeTagsDialog(getShell(), fLaunchConfiguration);

		if (dialog.open() == Window.OK) {
			try {
				ILaunchConfigurationWorkingCopy workingCopy= fLaunchConfiguration.getWorkingCopy();
				workingCopy.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_HAS_INCLUDE_TAGS, dialog.hasIncludeTags());
				workingCopy.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_HAS_EXCLUDE_TAGS, dialog.hasExcludeTags());
				workingCopy.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_INCLUDE_TAGS, dialog.getIncludeTags());
				workingCopy.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_EXCLUDE_TAGS, dialog.getExcludeTags());
				workingCopy.doSave();
				validatePage();
				updateLaunchConfigurationDialog();
			} catch (CoreException e) {
			}
		}
	}
}
