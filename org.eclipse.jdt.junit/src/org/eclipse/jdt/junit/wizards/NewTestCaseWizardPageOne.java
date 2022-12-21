/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Sebastian Davids, sdavids@gmx.de - bug 38507
 *     Sebastian Davids, sdavids@gmx.de - 113998 [JUnit] New Test Case Wizard: Class Under Test Dialog -- allow Enums
 *     Kris De Volder <kris.de.volder@gmail.com> - Allow changing the default superclass in NewTestCaseWizardPageOne - https://bugs.eclipse.org/312204
 *******************************************************************************/
package org.eclipse.jdt.junit.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.manipulation.CodeGeneration;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;
import org.eclipse.jdt.internal.junit.ui.IJUnitHelpContextIds;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility.GenStubSettings;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jdt.internal.junit.wizards.MethodStubsSelectionButtonGroup;
import org.eclipse.jdt.internal.junit.wizards.WizardMessages;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;


/**
 * The class <code>NewTestCaseWizardPageOne</code> contains controls and validation routines
 * for the first page of the  'New JUnit TestCase Wizard'.
 * <p>
 * Clients can use the page as-is and add it to their own wizard, or extend it to modify
 * validation or add and remove controls.
 * </p>
 *
 * @since 3.1
 */
public class NewTestCaseWizardPageOne extends NewTypeWizardPage {

	private final static String PAGE_NAME= "NewTestCaseCreationWizardPage"; //$NON-NLS-1$


	/** Field ID of the class under test field. */
	public final static String CLASS_UNDER_TEST= PAGE_NAME + ".classundertest"; //$NON-NLS-1$

	/**
	 * Field ID of the JUnit version radio buttons
	 * @since 3.2
	 */
	public final static String JUNIT4TOGGLE= PAGE_NAME + ".junit4toggle"; //$NON-NLS-1$

	private static final String COMPLIANCE_PAGE_ID= "org.eclipse.jdt.ui.propertyPages.CompliancePreferencePage"; //$NON-NLS-1$
	private static final String BUILD_PATH_PAGE_ID= "org.eclipse.jdt.ui.propertyPages.BuildPathsPropertyPage"; //$NON-NLS-1$
	private static final String BUILD_PATH_KEY_ADD_ENTRY= "add_classpath_entry"; //$NON-NLS-1$
	private static final String BUILD_PATH_BLOCK= "block_until_buildpath_applied"; //$NON-NLS-1$

	private static final String KEY_NO_LINK= "PropertyAndPreferencePage.nolink"; //$NON-NLS-1$

	private final static String QUESTION_MARK_TAG= "Q"; //$NON-NLS-1$
	private final static String OF_TAG= "Of"; //$NON-NLS-1$

	private final static String TEST_SUFFIX= "Test"; //$NON-NLS-1$
	private final static String PREFIX= "test"; //$NON-NLS-1$

	private final static String STORE_SETUP= PAGE_NAME + ".USE_SETUP";	//$NON-NLS-1$
	private final static String STORE_TEARDOWN= PAGE_NAME + ".USE_TEARDOWN"; //$NON-NLS-1$
	private final static String STORE_SETUP_CLASS= PAGE_NAME + ".USE_SETUPCLASS";	//$NON-NLS-1$
	private final static String STORE_TEARDOWN_CLASS= PAGE_NAME + ".USE_TEARDOWNCLASS"; //$NON-NLS-1$
	private final static String STORE_CONSTRUCTOR= PAGE_NAME + ".USE_CONSTRUCTOR"; //$NON-NLS-1$


	private final static int IDX_SETUP_CLASS= 0;
	private final static int IDX_TEARDOWN_CLASS= 1;
	private final static int IDX_SETUP= 2;
	private final static int IDX_TEARDOWN= 3;
	private final static int IDX_CONSTRUCTOR= 4;

	private NewTestCaseWizardPageTwo fPage2;
	private MethodStubsSelectionButtonGroup fMethodStubsButtons;

	private String fClassUnderTestText; // model
	private IType fClassUnderTest; // resolved model, can be null

	private Text fClassUnderTestControl; // control
	private IStatus fClassUnderTestStatus; // status

	private Button fClassUnderTestButton;
	private JavaTypeCompletionProcessor fClassToTestCompletionProcessor;


	private Button fJUnit3Button;
	private Button fJUnit4Button;
	private Button fJUnit5Button;

	private IStatus fJUnitStatus; // status
	private boolean fIsJUnitEnabled;
	private Link fLink;
	private Label fImage;

	private JUnitVersion fJUnitVersion= JUnitVersion.VERSION_3;

	/**
	 * Available JUnit versions.
	 *
	 * @since 3.11
	 */
	public enum JUnitVersion {
		VERSION_3(new String[] {
				/* IDX_SETUP_CLASS */ WizardMessages.NewTestCaseWizardPageOne_methodStub_setUpBeforeClass,
				/* IDX_TEARDOWN_CLASS */ WizardMessages.NewTestCaseWizardPageOne_methodStub_tearDownAfterClass,
				/* IDX_SETUP */ WizardMessages.NewTestCaseWizardPageOne_methodStub_setUp,
				/* IDX_TEARDOWN */ WizardMessages.NewTestCaseWizardPageOne_methodStub_tearDown,
				/* IDX_CONSTRUCTOR */ WizardMessages.NewTestCaseWizardPageOne_methodStub_constructor
		}),
		VERSION_4(new String[] {
				/* IDX_SETUP_CLASS */ WizardMessages.NewJ4TestCaseWizardPageOne_methodStub_setUpBeforeClass,
				/* IDX_TEARDOWN_CLASS */ WizardMessages.NewJ4TestCaseWizardPageOne_methodStub_tearDownAfterClass,
				/* IDX_SETUP */ WizardMessages.NewJ4TestCaseWizardPageOne_methodStub_setUp,
				/* IDX_TEARDOWN */ WizardMessages.NewJ4TestCaseWizardPageOne_methodStub_tearDown,
				/* IDX_CONSTRUCTOR */ WizardMessages.NewTestCaseWizardPageOne_methodStub_constructor
		}),
		VERSION_5(new String[] {
				/* IDX_SETUP_CLASS */ WizardMessages.NewJ5TestCaseWizardPageOne_methodStub_setUpBeforeClass,
				/* IDX_TEARDOWN_CLASS */ WizardMessages.NewJ5TestCaseWizardPageOne_methodStub_tearDownAfterClass,
				/* IDX_SETUP */ WizardMessages.NewJ5TestCaseWizardPageOne_methodStub_setUp,
				/* IDX_TEARDOWN */ WizardMessages.NewJ5TestCaseWizardPageOne_methodStub_tearDown,
				/* IDX_CONSTRUCTOR */ WizardMessages.NewTestCaseWizardPageOne_methodStub_constructor
		});

		/**
		 * @since 3.12
		 */
		public final String[] buttonNames;

		JUnitVersion(String[] buttonNames) {
			this.buttonNames= buttonNames;
		}
	}

	/**
	 * Creates a new <code>NewTestCaseCreationWizardPage</code>.
	 * @param page2 The second page
	 *
	 * @since 3.1
	 */
	public NewTestCaseWizardPageOne(NewTestCaseWizardPageTwo page2) {
		super(true, PAGE_NAME);
		fPage2= page2;

		setTitle(WizardMessages.NewTestCaseWizardPageOne_title);
		setDescription(WizardMessages.NewTestCaseWizardPageOne_description);

		enableCommentControl(true);

		fMethodStubsButtons= new MethodStubsSelectionButtonGroup(SWT.CHECK, fJUnitVersion, 2) {
			@Override
			protected void doWidgetSelected(SelectionEvent e) {
				super.doWidgetSelected(e);
				saveWidgetValues();
			}
		};
		fMethodStubsButtons.setLabelText(WizardMessages.NewTestCaseWizardPageOne_method_Stub_label);

		fClassToTestCompletionProcessor= new JavaTypeCompletionProcessor(false, false, true);

		fClassUnderTestStatus= new JUnitStatus();

		fClassUnderTestText= ""; //$NON-NLS-1$

		fJUnitStatus= new JUnitStatus();
		setJUnitVersion(fJUnitVersion);
	}

	/**
	 * Initialized the page with the current selection
	 * @param selection The selection
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement element= getInitialJavaElement(selection);

		initContainerPage(element, true);
		initTypePage(element);
		// put default class to test
		if (element != null) {
			IType classToTest= null;
			// evaluate the enclosing type
			IType typeInCompUnit= (IType) element.getAncestor(IJavaElement.TYPE);
			if (typeInCompUnit != null) {
				if (typeInCompUnit.getCompilationUnit() != null) {
					classToTest= typeInCompUnit;
				}
			} else {
				ICompilationUnit cu= (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (cu != null)
					classToTest= cu.findPrimaryType();
				else {
					if (element instanceof IClassFile) {
						try {
							IClassFile cf= (IClassFile) element;
							if (cf instanceof IOrdinaryClassFile && cf.isStructureKnown())
								classToTest= ((IOrdinaryClassFile) cf).getType();
						} catch(JavaModelException e) {
							JUnitPlugin.log(e);
						}
					}
				}
			}
			if (classToTest != null) {
				try {
					if (!CoreTestSearchEngine.isTestImplementor(classToTest)) {
						setClassUnderTest(classToTest.getFullyQualifiedName('.'));
					}
				} catch (JavaModelException e) {
					JUnitPlugin.log(e);
				}
			}
		}

		restoreWidgetValues();

		boolean isJunit5= false;
		boolean isJunit4= false;
		if (element != null && element.getElementType() != IJavaElement.JAVA_MODEL) {
			IJavaProject project= element.getJavaProject();
			isJunit5= CoreTestSearchEngine.hasJUnit5TestAnnotation(project);
			if (!isJunit5) {
				isJunit4= CoreTestSearchEngine.hasJUnit4TestAnnotation(project);
				if (!isJunit4) {
					if (!CoreTestSearchEngine.hasTestCaseType(project)) {
						if (JUnitStubUtility.is18OrHigher(project)) {
							isJunit5= true;
						} else if (JUnitStubUtility.is50OrHigher(project)) {
							isJunit4= true;
						}
					}
				}
			}
		}
		if (isJunit5) {
			setJUnitVersion(JUnitVersion.VERSION_5);
		} else if (isJunit4) {
			setJUnitVersion(JUnitVersion.VERSION_4);
		} else {
			setJUnitVersion(JUnitVersion.VERSION_3);
		}
		setEnabled(true);
		updateStatus(getStatusList());
	}

	private IStatus junitStatusChanged() {
		JUnitStatus status= new JUnitStatus();
		return status;
	}

	/**
	 * Specifies if the test should be created as JUnit 4 test.
	 * @param isJUnit4 if set, a JUnit 4 test will be created; otherwise a JUnit 3 test will be
	 *            created
	 * @param isEnabled if <code>true</code>, the modifier fields are editable; otherwise they are
	 *            read-only
	 *
	 * @since 3.2
	 * @deprecated use {@link #setJUnitVersion(JUnitVersion)} and {@link #setEnabled(boolean)}
	 *             instead
	 */
	@Deprecated
	public void setJUnit4(boolean isJUnit4, boolean isEnabled) {
		setEnabled(isEnabled);
		if (isJUnit4) {
			setJUnitVersion(JUnitVersion.VERSION_4);
		} else {
			setJUnitVersion(JUnitVersion.VERSION_3);
		}
	}

	/**
	 * Specifies the JUnit version to create the test.
	 *
	 * @param version the JUnit version
	 * @since 3.11
	 */
	public void setJUnitVersion(JUnitVersion version) {
		internalSetJUnit(version);
		switch (fJUnitVersion) {
			case VERSION_5:
				if (fJUnit5Button != null && !fJUnit5Button.isDisposed()) {
					fJUnit5Button.setSelection(true);
				}
				break;
			case VERSION_4:
				if (fJUnit4Button != null && !fJUnit4Button.isDisposed()) {
					fJUnit4Button.setSelection(true);
				}
				break;
			case VERSION_3:
				if (fJUnit3Button != null && !fJUnit3Button.isDisposed()) {
					fJUnit3Button.setSelection(true);
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Specifies if the JUnit version radio buttons are enabled.
	 *
	 * @param enabled if <code>true</code>, the JUnit version radio buttons are enabled; otherwise they
	 *            are read-only
	 * @since 3.11
	 */
	public void setEnabled(boolean enabled) {
		fIsJUnitEnabled= enabled;
		if (fJUnit5Button != null && !fJUnit5Button.isDisposed()) {
			fJUnit5Button.setEnabled(fIsJUnitEnabled);
		}
		if (fJUnit4Button != null && !fJUnit4Button.isDisposed()) {
			fJUnit4Button.setEnabled(fIsJUnitEnabled);
		}
		if (fJUnit3Button != null && !fJUnit3Button.isDisposed()) {
			fJUnit3Button.setEnabled(fIsJUnitEnabled);
		}
	}

	/**
	 * Returns <code>true</code> if the test should be created as Junit 4 test
	 * @return returns <code>true</code> if the test should be created as Junit 4 test
	 *
	 * @since 3.2
	 * @deprecated use {@link #getJUnitVersion()} instead
	 */
	@Deprecated
	public boolean isJUnit4() {
		return getJUnitVersion() == JUnitVersion.VERSION_4;
	}

	/**
	 * Returns the JUnit version to create the test.
	 *
	 * @return the JUnit version to create the test
	 * @since 3.11
	 */
	public JUnitVersion getJUnitVersion() {
		return fJUnitVersion;
	}

	private void internalSetJUnit(JUnitVersion version) {
		fJUnitVersion= version;
		fMethodStubsButtons.updateButtons(version);
		fJUnitStatus= junitStatusChanged();
		if (isDefaultSuperClass() || "".equals(getSuperClass().trim())) //$NON-NLS-1$
			setSuperClass(getDefaultSuperClassName(), true);
		fSuperClassStatus= superClassChanged(); //validate superclass field when toggled
		handleFieldChanged(JUNIT4TOGGLE);
	}

	/**
	 * Returns whether the super class name is one of the default super class names.
	 *
	 * @return <code>true</code> if the super class name is one of the default super class names,
	 *         <code>false</code> otherwise
	 * @since 3.7
	 */
	private boolean isDefaultSuperClass() {
		String superClass= getSuperClass();
		return superClass.equals(getJUnit3TestSuperclassName()) || "java.lang.Object".equals(superClass); //$NON-NLS-1$
	}

	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (CONTAINER.equals(fieldName)) {
			fClassUnderTestStatus= classUnderTestChanged();
			if (fClassUnderTestButton != null && !fClassUnderTestButton.isDisposed()) {
				fClassUnderTestButton.setEnabled(getPackageFragmentRoot() != null);
			}
			fJUnitStatus= junitStatusChanged();

			updateBuildPathMessage();
		} else if (JUNIT4TOGGLE.equals(fieldName)) {
			updateBuildPathMessage();
			boolean isJUnit3= getJUnitVersion() == JUnitVersion.VERSION_3;
			fMethodStubsButtons.setEnabled(IDX_SETUP_CLASS, !isJUnit3);
			fMethodStubsButtons.setEnabled(IDX_TEARDOWN_CLASS, !isJUnit3);
			fMethodStubsButtons.setEnabled(IDX_CONSTRUCTOR, isJUnit3);
		}
		updateStatus(getStatusList());
	}

	/**
	 * Returns all status to be consider for the validation. Clients can override.
	 * @return The list of status to consider for the validation.
	 */
	protected IStatus[] getStatusList() {
		return new IStatus[] {
				fContainerStatus,
				fPackageStatus,
				fTypeNameStatus,
				fClassUnderTestStatus,
				fModifierStatus,
				fSuperClassStatus,
				fJUnitStatus
		};
	}


	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);

		int nColumns= 4;

		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
		composite.setLayout(layout);
		createJUnit4Controls(composite, nColumns);
		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);
		createSeparator(composite, nColumns);
		createTypeNameControls(composite, nColumns);
		createSuperClassControls(composite, nColumns);
		createMethodStubSelectionControls(composite, nColumns);
		createCommentControls(composite, nColumns);
		createSeparator(composite, nColumns);
		createClassUnderTestControls(composite, nColumns);
		createBuildPathConfigureControls(composite, nColumns);

		setControl(composite);

		//set default and focus
		String classUnderTest= getClassUnderTestText();
		if (classUnderTest.length() > 0) {
			String typeName= getUniqueJavaTypeName(getPackageFragment(), Signature.getSimpleName(classUnderTest)+TEST_SUFFIX);
			setTypeName(typeName, true);
		}

		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJUnitHelpContextIds.NEW_TESTCASE_WIZARD_PAGE);

		setFocus();
	}

	/**
	 * Creates the controls for the method stub selection buttons. Expects a <code>GridLayout</code> with
	 * at least 3 columns.
	 *
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createMethodStubSelectionControls(Composite composite, int nColumns) {
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getLabelControl(composite), nColumns);
		LayoutUtil.createEmptySpace(composite, 1);
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getSelectionButtonsGroup(composite), nColumns - 1);
	}

	/**
	 * Creates the controls for the 'class under test' field. Expects a <code>GridLayout</code> with
	 * at least 3 columns.
	 *
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createClassUnderTestControls(Composite composite, int nColumns) {
		Label classUnderTestLabel= new Label(composite, SWT.LEFT | SWT.WRAP);
		classUnderTestLabel.setFont(composite.getFont());
		classUnderTestLabel.setText(WizardMessages.NewTestCaseWizardPageOne_class_to_test_label);
		classUnderTestLabel.setLayoutData(new GridData());

		fClassUnderTestControl= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fClassUnderTestControl.setEnabled(true);
		fClassUnderTestControl.setFont(composite.getFont());
		fClassUnderTestControl.setText(fClassUnderTestText);
		fClassUnderTestControl.addModifyListener(modifyEvent -> internalSetClassUnderText(((Text) modifyEvent.widget).getText()));
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= true;
		gd.horizontalSpan= nColumns - 2;
		fClassUnderTestControl.setLayoutData(gd);

		fClassUnderTestButton= new Button(composite, SWT.PUSH);
		fClassUnderTestButton.setText(WizardMessages.NewTestCaseWizardPageOne_class_to_test_browse);
		fClassUnderTestButton.setEnabled(getPackageFragmentRoot() != null);
		fClassUnderTestButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				classToTestButtonPressed();
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				classToTestButtonPressed();
			}
		});
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= false;
		gd.horizontalSpan= 1;
		gd.widthHint = LayoutUtil.getButtonWidthHint(fClassUnderTestButton);
		fClassUnderTestButton.setLayoutData(gd);

		ControlContentAssistHelper.createTextContentAssistant(fClassUnderTestControl, fClassToTestCompletionProcessor);
	}

	/**
	 * Creates the controls for the JUnit version radio buttons. Expects a <code>GridLayout</code> with
	 * at least 3 columns.
	 *
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 *
	 * @since 3.2
	 */
	protected void createJUnit4Controls(Composite composite, int nColumns) {
		Composite inner= new Composite(composite, SWT.NONE);
		inner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, nColumns, 1));
		GridLayout layout= new GridLayout(3, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		inner.setLayout(layout);

		JUnitVersion version= getJUnitVersion();

		fJUnit3Button= new Button(inner, SWT.RADIO);
		fJUnit3Button.setText(WizardMessages.NewTestCaseWizardPageOne_junit3_radio_label);
		fJUnit3Button.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 1, 1));
		fJUnit3Button.setSelection(version == JUnitVersion.VERSION_3);
		fJUnit3Button.setEnabled(fIsJUnitEnabled);
		fJUnit3Button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					internalSetJUnit(JUnitVersion.VERSION_3);
				}
			}
		});

		fJUnit4Button= new Button(inner, SWT.RADIO);
		fJUnit4Button.setText(WizardMessages.NewTestCaseWizardPageOne_junit4_radio_label);
		fJUnit4Button.setSelection(version == JUnitVersion.VERSION_4);
		fJUnit4Button.setEnabled(fIsJUnitEnabled);
		fJUnit4Button.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 1, 1));
		fJUnit4Button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					internalSetJUnit(JUnitVersion.VERSION_4);
				}
			}
		});

		fJUnit5Button= new Button(inner, SWT.RADIO);
		fJUnit5Button.setText(WizardMessages.NewTestCaseWizardPageOne_junit5_radio_label);
		fJUnit5Button.setSelection(version == JUnitVersion.VERSION_5);
		fJUnit5Button.setEnabled(fIsJUnitEnabled);
		fJUnit5Button.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 1, 1));
		fJUnit5Button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					internalSetJUnit(JUnitVersion.VERSION_5);
				}
			}
		});
	}

	/**
	 * Creates the controls for the JUnit 4 toggle control. Expects a <code>GridLayout</code> with
	 * at least 3 columns.
	 *
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 *
	 * @since 3.2
	 */
	protected void createBuildPathConfigureControls(Composite composite, int nColumns) {
		Composite inner= new Composite(composite, SWT.NONE);
		inner.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, nColumns, 1));
		GridLayout layout= new GridLayout(2, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		inner.setLayout(layout);

		fImage= new Label(inner, SWT.NONE);
		fImage.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
		fImage.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 1, 1));

		fLink= new Link(inner, SWT.WRAP);
		fLink.setText("\n\n"); //$NON-NLS-1$
		fLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performBuildpathConfiguration(e.text);
			}
		});
		GridData gd= new GridData(GridData.FILL, GridData.BEGINNING, true, false, 1, 1);
		gd.widthHint= convertWidthInCharsToPixels(60);
		fLink.setLayoutData(gd);
		updateBuildPathMessage();
	}

	private void performBuildpathConfiguration(Object data) {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return; // should not happen. Link shouldn't be visible
		}
		IJavaProject javaProject= root.getJavaProject();

		if ("a3".equals(data)) { // add and configure JUnit 3 //$NON-NLS-1$
			String id= BUILD_PATH_PAGE_ID;
			Map<String, Object> input= new HashMap<>();
			IClasspathEntry newEntry= BuildPathSupport.getJUnit3ClasspathEntry();
			input.put(BUILD_PATH_KEY_ADD_ENTRY, newEntry);
			input.put(BUILD_PATH_BLOCK, Boolean.TRUE);
			PreferencesUtil.createPropertyDialogOn(getShell(), javaProject, id, new String[] { id }, input).open();
		} else if ("a4".equals(data)) { // add and configure JUnit 4 //$NON-NLS-1$
			String id= BUILD_PATH_PAGE_ID;
			Map<String, Object> input= new HashMap<>();
			IClasspathEntry newEntry= BuildPathSupport.getJUnit4ClasspathEntry();
			input.put(BUILD_PATH_KEY_ADD_ENTRY, newEntry);
			input.put(BUILD_PATH_BLOCK, Boolean.TRUE);
			PreferencesUtil.createPropertyDialogOn(getShell(), javaProject, id, new String[] { id }, input).open();
		} else if ("b".equals(data)) { // open build path //$NON-NLS-1$
			String id= BUILD_PATH_PAGE_ID;
			Map<String, Object> input= new HashMap<>();
			input.put(BUILD_PATH_BLOCK, Boolean.TRUE);
			PreferencesUtil.createPropertyDialogOn(getShell(), javaProject, id, new String[] { id }, input).open();
		} else if ("c".equals(data)) { // open compliance //$NON-NLS-1$
			String buildPath= BUILD_PATH_PAGE_ID;
			String complianceId= COMPLIANCE_PAGE_ID;
			Map<String, Boolean> input= new HashMap<>();
			input.put(BUILD_PATH_BLOCK, Boolean.TRUE);
			input.put(KEY_NO_LINK, Boolean.TRUE);
			PreferencesUtil.createPropertyDialogOn(getShell(), javaProject, complianceId, new String[] { buildPath, complianceId  }, data).open();
		}

		updateBuildPathMessage();
	}

	private void updateBuildPathMessage() {
		if (fLink == null || fLink.isDisposed()) {
			return;
		}

		String message= null;
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null) {
			IJavaProject project= root.getJavaProject();
			if (project.exists()) {
				if (fJUnitVersion == JUnitVersion.VERSION_4) {
					if (!JUnitStubUtility.is50OrHigher(project)) {
						message= WizardMessages.NewTestCaseWizardPageOne_linkedtext_java5required;
					}
				} else if (fJUnitVersion == JUnitVersion.VERSION_5) {
					if (!JUnitStubUtility.is18OrHigher(project)) {
						message= WizardMessages.NewTestCaseWizardPageOne_linkedtext_java8required;
					}
				}
			}
		}
		fLink.setVisible(message != null);
		fImage.setVisible(message != null);

		if (message != null) {
			fLink.setText(message);
		}
	}


	private void classToTestButtonPressed() {
		IType type= chooseClassToTestType();
		if (type != null) {
			setClassUnderTest(type.getFullyQualifiedName('.'));
		}
	}

	private IType chooseClassToTestType() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null)
			return null;

		IJavaElement[] elements= new IJavaElement[] { root.getJavaProject() };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);

		try {
			SelectionDialog dialog= JavaUI.createTypeDialog(getShell(), getWizard().getContainer(), scope, IJavaElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS, false, getClassUnderTestText());
			dialog.setTitle(WizardMessages.NewTestCaseWizardPageOne_class_to_test_dialog_title);
			dialog.setMessage(WizardMessages.NewTestCaseWizardPageOne_class_to_test_dialog_message);
			if (dialog.open() == Window.OK) {
				Object[] resultArray= dialog.getResult();
				if (resultArray != null && resultArray.length > 0)
					return (IType) resultArray[0];
			}
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		}
		return null;
	}

	@Override
	protected IStatus packageChanged() {
		IStatus status= super.packageChanged();
		fClassToTestCompletionProcessor.setPackageFragment(getPackageFragment());
		return status;
	}

	/**
	 * Hook method that gets called when the class under test has changed. The method class under test
	 * returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 *
	 * @return the status of the validation
	 */
	protected IStatus classUnderTestChanged() {
		JUnitStatus status= new JUnitStatus();


		fClassUnderTest= null;

		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return status;
		}

		String classToTestName= getClassUnderTestText();
		if (classToTestName.length() == 0) {
			return status;
		}

		IStatus val= JavaConventionsUtil.validateJavaTypeName(classToTestName, root);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(WizardMessages.NewTestCaseWizardPageOne_error_class_to_test_not_valid);
			return status;
		}

		IPackageFragment pack= getPackageFragment(); // can be null
		try {
			IType type= resolveClassNameToType(root.getJavaProject(), pack, classToTestName);
			if (type == null) {
				status.setError(WizardMessages.NewTestCaseWizardPageOne_error_class_to_test_not_exist);
				return status;
			}
			if (type.isInterface()) {
				status.setWarning(Messages.format(WizardMessages.NewTestCaseWizardPageOne_warning_class_to_test_is_interface, BasicElementLabels.getJavaElementName(classToTestName)));
			}

			if (pack != null && !JUnitStubUtility.isVisible(type, pack)) {
				status.setWarning(Messages.format(WizardMessages.NewTestCaseWizardPageOne_warning_class_to_test_not_visible, BasicElementLabels.getJavaElementName(classToTestName)));
			}
			fClassUnderTest= type;
			fPage2.setClassUnderTest(fClassUnderTest);
		} catch (JavaModelException e) {
			status.setError(WizardMessages.NewTestCaseWizardPageOne_error_class_to_test_not_valid);
		}
		return status;
	}

	/**
	 * Returns the content of the class to test text field.
	 *
	 * @return the name of the class to test
	 */
	public String getClassUnderTestText() {
		return fClassUnderTestText;
	}

	/**
	 * Returns the class to be tested.
	 *
	 * 	@return the class under test or <code>null</code> if the entered values are not valid
	 */
	public IType getClassUnderTest() {
		return fClassUnderTest;
	}

	/**
	 * Sets the name of the class under test.
	 *
	 * @param name The name to set
	 */
	public void setClassUnderTest(String name) {
		if (fClassUnderTestControl != null && !fClassUnderTestControl.isDisposed()) {
			fClassUnderTestControl.setText(name);
		}
		internalSetClassUnderText(name);
	}

	private void internalSetClassUnderText(String name) {
		fClassUnderTestText= name;
		fClassUnderTestStatus= classUnderTestChanged();
		handleFieldChanged(CLASS_UNDER_TEST);
	}

	@Override
	protected void createTypeMembers(IType type, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		if (fMethodStubsButtons.isSelected(IDX_CONSTRUCTOR))
			createConstructor(type, imports);

		if (fMethodStubsButtons.isSelected(IDX_SETUP_CLASS)) {
			createSetUpClass(type, imports);
		}

		if (fMethodStubsButtons.isSelected(IDX_TEARDOWN_CLASS)) {
			createTearDownClass(type, imports);
		}

		if (fMethodStubsButtons.isSelected(IDX_SETUP)) {
			createSetUp(type, imports);
		}

		if (fMethodStubsButtons.isSelected(IDX_TEARDOWN)) {
			createTearDown(type, imports);
		}

		if (fClassUnderTest != null || fJUnitVersion != JUnitVersion.VERSION_3) {
			createTestMethodStubs(type, imports);
		}

		if (fJUnitVersion == JUnitVersion.VERSION_4) {
			imports.addStaticImport("org.junit.Assert", "*", false); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (fJUnitVersion == JUnitVersion.VERSION_5) {
			imports.addStaticImport("org.junit.jupiter.api.Assertions", "*", false); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	private void createConstructor(IType type, ImportsManager imports) throws CoreException {
		ITypeHierarchy typeHierarchy= null;
		IType[] superTypes= null;
		String content;
		IMethod methodTemplate= null;
		if (type.exists()) {
			typeHierarchy= type.newSupertypeHierarchy(null);
			superTypes= typeHierarchy.getAllSuperclasses(type);
			for (IType superType : superTypes) {
				if (superType.exists()) {
					IMethod constrMethod= superType.getMethod(superType.getElementName(), new String[] {"Ljava.lang.String;"}); //$NON-NLS-1$
					if (constrMethod.exists() && constrMethod.isConstructor()) {
						methodTemplate= constrMethod;
						break;
					}
				}
			}
		}
		GenStubSettings settings= JUnitStubUtility.getCodeGenerationSettings(type.getJavaProject());
		settings.createComments= isAddComments();

		if (methodTemplate != null) {
			settings.callSuper= true;
			settings.methodOverwrites= true;
			content= JUnitStubUtility.genStub(type.getCompilationUnit(), getTypeName(), methodTemplate, settings, null, imports);
		} else {
			final String delimiter= getLineDelimiter();
			StringBuilder buffer= new StringBuilder(32);
			buffer.append("public "); //$NON-NLS-1$
			buffer.append(getTypeName());
			buffer.append('(');
			if (fJUnitVersion == JUnitVersion.VERSION_3) {
				buffer.append(imports.addImport("java.lang.String")).append(" name"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			buffer.append(") {"); //$NON-NLS-1$
			buffer.append(delimiter);
			if (fJUnitVersion == JUnitVersion.VERSION_3) {
				buffer.append("super(name);").append(delimiter); //$NON-NLS-1$
			}
			buffer.append('}');
			buffer.append(delimiter);
			content= buffer.toString();
		}
		type.createMethod(content, null, true, null);
	}

	private IMethod findInHierarchy(IType type, String methodName) throws JavaModelException {
		ITypeHierarchy typeHierarchy= null;
		IType[] superTypes= null;
		if (type.exists()) {
			typeHierarchy= type.newSupertypeHierarchy(null);
			superTypes= typeHierarchy.getAllSuperclasses(type);
			for (IType superType : superTypes) {
				if (superType.exists()) {
					IMethod testMethod= superType.getMethod(methodName, new String[] {});
					if (testMethod.exists()) {
						return testMethod;
					}
				}
			}
		}
		return null;
	}

	private void createSetupStubs(IType type, String methodName, boolean isStatic, String annotationType, ImportsManager imports) throws CoreException {
		String content= null;
		IMethod methodTemplate= findInHierarchy(type, methodName);
		String annotation= null;
		if (fJUnitVersion != JUnitVersion.VERSION_3) {
			annotation= '@' + imports.addImport(annotationType);
		}

		GenStubSettings settings= JUnitStubUtility.getCodeGenerationSettings(type.getJavaProject());
		settings.createComments= isAddComments();

		if (methodTemplate != null) {
			settings.callSuper= true;
			settings.methodOverwrites= true;
			content= JUnitStubUtility.genStub(type.getCompilationUnit(), getTypeName(), methodTemplate, settings, annotation, imports);
		} else {
			final String delimiter= getLineDelimiter();
			StringBuilder buffer= new StringBuilder();
			if (settings.createComments) {
				String[] excSignature= { Signature.createTypeSignature("java.lang.Exception", true) }; //$NON-NLS-1$
				String comment= CodeGeneration.getMethodComment(type.getCompilationUnit(), type.getElementName(), methodName, new String[0], excSignature, Signature.SIG_VOID, null, delimiter);
				if (comment != null) {
					buffer.append(comment);
				}
			}
			if (annotation != null) {
				buffer.append(annotation).append(delimiter);
			}

			if (fJUnitVersion == JUnitVersion.VERSION_4) {
				buffer.append("public "); //$NON-NLS-1$
			} else if (fJUnitVersion == JUnitVersion.VERSION_3) {
				buffer.append("protected "); //$NON-NLS-1$
			}
			if (isStatic) {
				buffer.append("static "); //$NON-NLS-1$
			}
			buffer.append("void "); //$NON-NLS-1$
			buffer.append(methodName);
			buffer.append("() throws "); //$NON-NLS-1$
			buffer.append(imports.addImport("java.lang.Exception")); //$NON-NLS-1$
			buffer.append(" {}"); //$NON-NLS-1$
			buffer.append(delimiter);
			content= buffer.toString();
		}
		type.createMethod(content, null, false, null);
	}



	private void createSetUp(IType type, ImportsManager imports) throws CoreException {
		String annotationType= fJUnitVersion == JUnitVersion.VERSION_4 ? "org.junit.Before" : "org.junit.jupiter.api.BeforeEach"; //$NON-NLS-1$ //$NON-NLS-2$
		createSetupStubs(type, "setUp", false, annotationType, imports); //$NON-NLS-1$
	}

	private void createTearDown(IType type, ImportsManager imports) throws CoreException {
		String annotationType= fJUnitVersion == JUnitVersion.VERSION_4 ? "org.junit.After" : "org.junit.jupiter.api.AfterEach"; //$NON-NLS-1$ //$NON-NLS-2$
		createSetupStubs(type, "tearDown", false, annotationType, imports); //$NON-NLS-1$
	}

	private void createSetUpClass(IType type, ImportsManager imports) throws CoreException {
		String annotationType= fJUnitVersion == JUnitVersion.VERSION_4 ? "org.junit.BeforeClass" : "org.junit.jupiter.api.BeforeAll"; //$NON-NLS-1$ //$NON-NLS-2$
		createSetupStubs(type, "setUpBeforeClass", true, annotationType, imports); //$NON-NLS-1$
	}

	private void createTearDownClass(IType type, ImportsManager imports) throws CoreException {
		String annotationType= fJUnitVersion == JUnitVersion.VERSION_4 ? "org.junit.AfterClass" : "org.junit.jupiter.api.AfterAll"; //$NON-NLS-1$ //$NON-NLS-2$
		createSetupStubs(type, "tearDownAfterClass", true, annotationType, imports); //$NON-NLS-1$
	}

	private void createTestMethodStubs(IType type, ImportsManager imports) throws CoreException {
		IMethod[] methods= fPage2.getCheckedMethods();
		if (methods.length == 0) {
			if (fJUnitVersion != JUnitVersion.VERSION_3) {
				List<String> names= new ArrayList<>();
				createTestMethod(type, imports, null, null, names);
			}
			return;
		}
		/* find overloaded methods */
		IMethod[] allMethodsArray= fPage2.getAllMethods();
		List<IMethod> allMethods= new ArrayList<>(Arrays.asList(allMethodsArray));
		List<IMethod> overloadedMethods= getOverloadedMethods(allMethods);

		/* used when for example both sum and Sum methods are present. Then
		 * sum -> testSum
		 * Sum -> testSum1
		 */
		List<String> names= new ArrayList<>();
		for (IMethod method : methods) {
			createTestMethod(type, imports, method, overloadedMethods, names);
		}
	}

	/**
	 * Creates a test method.
	 *
	 * @param type the type to create the method
	 * @param imports the imports manager
	 * @param method the method or <code>null</code>
	 * @param overloadedMethods the list of overloaded methods or <code>null</code>
	 * @param names the list of method names
	 * @throws CoreException if the element could not be created
	 * @since 3.7
	 */
	private void createTestMethod(IType type, ImportsManager imports, IMethod method, List<IMethod> overloadedMethods, List<String> names) throws CoreException {
		StringBuffer buffer= new StringBuffer();
		StringBuffer name;
		if (method != null) {
			String elementName= method.getElementName();
			name= new StringBuffer(PREFIX).append(Character.toUpperCase(elementName.charAt(0))).append(elementName.substring(1));
			final boolean contains= overloadedMethods.contains(method);
			if (contains)
				appendParameterNamesToMethodName(name, method.getParameterTypes());
		} else {
			name= new StringBuffer(PREFIX);
		}

		replaceIllegalCharacters(name);
		/* void foo(java.lang.StringBuffer sb) {}
		 *  void foo(mypackage1.StringBuffer sb) {}
		 *  void foo(mypackage2.StringBuffer sb) {}
		 * ->
		 *  testFooStringBuffer()
		 *  testFooStringBuffer1()
		 *  testFooStringBuffer2()
		 */
		String testName= name.toString();
		if (names.contains(testName)) {
			int suffix= 1;
			while (names.contains(testName + Integer.toString(suffix)))
				suffix++;
			name.append(Integer.toString(suffix));
		}
		testName= name.toString();
		names.add(testName);

		if (isAddComments() && method != null) {
			appendMethodComment(buffer, method);
		}
		if (fJUnitVersion == JUnitVersion.VERSION_4) {
			ISourceRange typeSourceRange= type.getSourceRange();
			int pos= typeSourceRange.getOffset() + typeSourceRange.getLength() - 1;
			buffer.append('@').append(imports.addImport(JUnitCorePlugin.JUNIT4_ANNOTATION_NAME, pos)).append(getLineDelimiter());
		} else if (fJUnitVersion == JUnitVersion.VERSION_5) {
			ISourceRange typeSourceRange= type.getSourceRange();
			int pos= typeSourceRange.getOffset() + typeSourceRange.getLength() - 1;
			buffer.append('@').append(imports.addImport(JUnitCorePlugin.JUNIT5_JUPITER_TEST_ANNOTATION_NAME, pos)).append(getLineDelimiter());
		}

		if (fJUnitVersion != JUnitVersion.VERSION_5) {
			buffer.append("public ");//$NON-NLS-1$
		}
		if (fPage2.getCreateFinalMethodStubsButtonSelection())
			buffer.append("final "); //$NON-NLS-1$
		buffer.append("void ");//$NON-NLS-1$
		buffer.append(testName);
		buffer.append("()");//$NON-NLS-1$
		appendTestMethodBody(buffer, type.getCompilationUnit());
		type.createMethod(buffer.toString(), null, false, null);
	}

	private void replaceIllegalCharacters(StringBuffer buffer) {
		char character= 0;
		for (int index= buffer.length() - 1; index >= 0; index--) {
			character= buffer.charAt(index);
			if (Character.isWhitespace(character))
				buffer.deleteCharAt(index);
			else if (character == '<')
				buffer.replace(index, index + 1, OF_TAG);
			else if (character == '?')
				buffer.replace(index, index + 1, QUESTION_MARK_TAG);
			else if (!Character.isJavaIdentifierPart(character)) {
				// Check for surrogates
				if (!Character.isSurrogate(character)) {
					/*
					 * XXX: Here we should create the code point and test whether
					 * it is a Java identifier part. Currently this is not possible
					 * because java.lang.Character in 1.4 does not support surrogates
					 * and because com.ibm.icu.lang.UCharacter.isJavaIdentifierPart(int)
					 * is not correctly implemented.
					 */
					buffer.deleteCharAt(index);
				}

			}
		}
	}

	private String getLineDelimiter() throws JavaModelException{
		IType classToTest= getClassUnderTest();

		if (classToTest != null && classToTest.exists() && classToTest.getCompilationUnit() != null)
			return classToTest.getCompilationUnit().findRecommendedLineSeparator();

		return getPackageFragment().findRecommendedLineSeparator();
	}

	private void appendTestMethodBody(StringBuffer buffer, ICompilationUnit targetCu) throws CoreException {
		final String delimiter= getLineDelimiter();
		buffer.append('{').append(delimiter);
		String todoTask= ""; //$NON-NLS-1$
		if (fPage2.isCreateTasks()) {
			String todoTaskTag= JUnitStubUtility.getTodoTaskTag(targetCu.getJavaProject());
			if (todoTaskTag != null) {
				todoTask= " // " + todoTaskTag; //$NON-NLS-1$
			}
		}
		String message= WizardMessages.NewTestCaseWizardPageOne_not_yet_implemented_string;
		buffer.append(Messages.format("fail(\"{0}\");", message)).append(todoTask).append(delimiter); //$NON-NLS-1$

		buffer.append('}').append(delimiter);
	}

	private void appendParameterNamesToMethodName(StringBuffer buffer, String[] parameters) {
		for (String parameter : parameters) {
			final StringBuilder buf= new StringBuilder(Signature.getSimpleName(Signature.toString(Signature.getElementType(parameter))));
			final char character= buf.charAt(0);
			if (buf.length() > 0 && !Character.isUpperCase(character))
				buf.setCharAt(0, Character.toUpperCase(character));
			buffer.append(buf.toString());
			for (int j= 0, arrayCount= Signature.getArrayCount(parameter); j < arrayCount; j++) {
				buffer.append("Array"); //$NON-NLS-1$
			}
		}
	}

	private void appendMethodComment(StringBuffer buffer, IMethod method) throws JavaModelException {
		final String delimiter= getLineDelimiter();
		final StringBuffer buf= new StringBuffer("{@link "); //$NON-NLS-1$
		JavaElementLabels.getTypeLabel(method.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED, buf);
		buf.append('#');
		buf.append(method.getElementName());
		buf.append('(');
		String[] paramTypes= JUnitStubUtility.getParameterTypeNamesForSeeTag(method);
		for (int i= 0; i < paramTypes.length; i++) {
			if (i != 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(paramTypes[i]);

		}
		buf.append(')');
		buf.append('}');

		buffer.append("/**");//$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append(" * ");//$NON-NLS-1$
		buffer.append(Messages.format(WizardMessages.NewTestCaseWizardPageOne_comment_class_to_test, buf.toString()));
		buffer.append(delimiter);
		buffer.append(" */");//$NON-NLS-1$
		buffer.append(delimiter);
	}


	private List<IMethod> getOverloadedMethods(List<IMethod> allMethods) {
		List<IMethod> overloadedMethods= new ArrayList<>();
		for (int i= 0; i < allMethods.size(); i++) {
			IMethod current= allMethods.get(i);
			String currentName= current.getElementName();
			boolean currentAdded= false;
			for (ListIterator<IMethod> iter= allMethods.listIterator(i+1); iter.hasNext(); ) {
				IMethod iterMethod= iter.next();
				if (iterMethod.getElementName().equals(currentName)) {
					//method is overloaded
					if (!currentAdded) {
						overloadedMethods.add(current);
						currentAdded= true;
					}
					overloadedMethods.add(iterMethod);
					iter.remove();
				}
			}
		}
		return overloadedMethods;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible) {
			saveWidgetValues();
		}

		//if (visible) setFocus();
	}

	/**
	 * The method is called when the container has changed to validate if the project
	 * is suited for the JUnit test class. Clients can override to modify or remove that validation.
	 *
	 * @return the status of the validation
	 */
	protected IStatus validateIfJUnitProject() {
		JUnitStatus status= new JUnitStatus();
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null) {
			try {
				IJavaProject project= root.getJavaProject();
				if (project.exists()) {
					boolean noMatch= false;
					if (fJUnitVersion != null) {
						switch (fJUnitVersion) {
							case VERSION_5:
								if (!JUnitStubUtility.is18OrHigher(project)) {
									status.setError(WizardMessages.NewTestCaseWizardPageOne_error_java8required);
									return status;
								}
								if (project.findType(JUnitCorePlugin.JUNIT5_TESTABLE_ANNOTATION_NAME) == null) {
									status.setWarning(WizardMessages.NewTestCaseWizardPageOne__error_junit5NotOnbuildpath);
									return status;
								}
								break;
							case VERSION_4:
								if (!JUnitStubUtility.is50OrHigher(project)) {
									status.setError(WizardMessages.NewTestCaseWizardPageOne_error_java5required);
									return status;
								}
								if (project.findType(JUnitCorePlugin.JUNIT4_ANNOTATION_NAME) == null) {
									status.setWarning(WizardMessages.NewTestCaseWizardPageOne__error_junit4NotOnbuildpath);
									return status;
								}
								break;
								//$CASES-OMITTED$
							default:
								noMatch= true;
								break;
						}
						if (noMatch) {
							if (project.findType(JUnitCorePlugin.TEST_SUPERCLASS_NAME) == null) {
								status.setWarning(WizardMessages.NewTestCaseWizardPageOne_error_junitNotOnbuildpath);
								return status;
							}
						}
					}
				}
			} catch (JavaModelException e) {
			}
		}
		return status;
	}

	@Override
	protected IStatus superClassChanged() {
		IStatus stat= super.superClassChanged();
		if (stat.getSeverity() != IStatus.OK)
			return stat;
		String superClassName= getSuperClass();
		JUnitStatus status= new JUnitStatus();
		boolean isJUnit3= fJUnitVersion == JUnitVersion.VERSION_3;
		if (superClassName == null || superClassName.trim().isEmpty()) {
			if (isJUnit3)
				status.setError(WizardMessages.NewTestCaseWizardPageOne_error_superclass_empty);
			return status;
		}
		if (!isJUnit3 && "java.lang.Object".equals(superClassName)) //$NON-NLS-1$
			return status;
		if (getPackageFragmentRoot() != null) {
			try {
				IType type= resolveClassNameToType(getPackageFragmentRoot().getJavaProject(), getPackageFragment(), superClassName);
				if (type == null) {
					status.setWarning(WizardMessages.NewTestCaseWizardPageOne_error_superclass_not_exist);
					return status;
				}
				if (type.isInterface()) {
					status.setError(WizardMessages.NewTestCaseWizardPageOne_error_superclass_is_interface);
					return status;
				}
				if (isJUnit3 && !CoreTestSearchEngine.isTestImplementor(type)) { // TODO: expensive!
					status.setError(Messages.format(WizardMessages.NewTestCaseWizardPageOne_error_superclass_not_implementing_test_interface, BasicElementLabels.getJavaElementName(JUnitCorePlugin.TEST_INTERFACE_NAME)));
					return status;
				}
			} catch (JavaModelException e) {
				JUnitPlugin.log(e);
			}
		}
		return status;
	}

	@Override
	public boolean canFlipToNextPage() {
		return super.canFlipToNextPage() && getClassUnderTest() != null;
	}

	private IType resolveClassNameToType(IJavaProject jproject, IPackageFragment pack, String classToTestName) throws JavaModelException {
		if (!jproject.exists()) {
			return null;
		}

		IType type= jproject.findType(classToTestName);

		// search in current package
		if (type == null && pack != null && !pack.isDefaultPackage()) {
			type= jproject.findType(pack.getElementName(), classToTestName);
		}

		// search in java.lang
		if (type == null) {
			type= jproject.findType("java.lang", classToTestName); //$NON-NLS-1$
		}
		return type;
	}

	/**
	 *	Use the dialog store to restore widget values to the values that they held
	 *	last time this wizard was used to completion
	 */
	private void restoreWidgetValues() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			fMethodStubsButtons.setSelection(IDX_SETUP, settings.getBoolean(STORE_SETUP));
			fMethodStubsButtons.setSelection(IDX_TEARDOWN, settings.getBoolean(STORE_TEARDOWN));
			fMethodStubsButtons.setSelection(IDX_SETUP_CLASS, settings.getBoolean(STORE_SETUP_CLASS));
			fMethodStubsButtons.setSelection(IDX_TEARDOWN_CLASS, settings.getBoolean(STORE_TEARDOWN_CLASS));
			fMethodStubsButtons.setSelection(IDX_CONSTRUCTOR, settings.getBoolean(STORE_CONSTRUCTOR));
		} else {
			fMethodStubsButtons.setSelection(IDX_SETUP, false); //setUp
			fMethodStubsButtons.setSelection(IDX_TEARDOWN, false); //tearDown
			fMethodStubsButtons.setSelection(IDX_SETUP_CLASS, false); //setUpBeforeClass
			fMethodStubsButtons.setSelection(IDX_TEARDOWN_CLASS, false); //setUpAfterClass
			fMethodStubsButtons.setSelection(IDX_CONSTRUCTOR, false); //constructor
		}
	}

	/**
	 * 	Since Finish was pressed, write widget values to the dialog store so that they
	 *	will persist into the next invocation of this wizard page
	 */
	private void saveWidgetValues() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			settings.put(STORE_SETUP, fMethodStubsButtons.isSelected(IDX_SETUP));
			settings.put(STORE_TEARDOWN, fMethodStubsButtons.isSelected(IDX_TEARDOWN));
			settings.put(STORE_SETUP_CLASS, fMethodStubsButtons.isSelected(IDX_SETUP_CLASS));
			settings.put(STORE_TEARDOWN_CLASS, fMethodStubsButtons.isSelected(IDX_TEARDOWN_CLASS));
			settings.put(STORE_CONSTRUCTOR, fMethodStubsButtons.isSelected(IDX_CONSTRUCTOR));
		}
	}

	/**
	 * Hook method that is called to determine the name of the superclass set for
	 * a JUnit 3 style test case. By default, the name of the JUnit 3 TestCase class is
	 * returned. Implementors can override this behavior to return the name of a
	 * subclass instead.
	 *
	 * @return the fully qualified name of a subclass of the JUnit 3 TestCase class.
	 *
	 * @since 3.7
	 */
	protected String getJUnit3TestSuperclassName() {
		return JUnitCorePlugin.TEST_SUPERCLASS_NAME;
	}

	/**
	 * Returns the default value for the super class field.
	 *
	 * @return the default value for the super class field
	 * @since 3.7
	 */
	private String getDefaultSuperClassName() {
		return fJUnitVersion != JUnitVersion.VERSION_3 ? "java.lang.Object" : getJUnit3TestSuperclassName(); //$NON-NLS-1$
	}

	/**
	 * @since 3.11
	 */
	@Override
	public int getModifiers() {
		int modifiers= super.getModifiers();
		if (fJUnitVersion == JUnitVersion.VERSION_5) {
			modifiers&= ~F_PUBLIC;
		}
		return modifiers;
	}
}
