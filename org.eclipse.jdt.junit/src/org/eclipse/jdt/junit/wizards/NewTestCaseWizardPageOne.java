/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids - bug 38507
 *******************************************************************************/
package org.eclipse.jdt.junit.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.IJUnitHelpContextIds;
import org.eclipse.jdt.internal.junit.ui.JUnitAddLibraryProposal;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility.GenStubSettings;
import org.eclipse.jdt.internal.junit.wizards.MethodStubsSelectionButtonGroup;
import org.eclipse.jdt.internal.junit.wizards.WizardMessages;

/**
 * The class <code>NewTestCaseWizardPageOne</code> contains controls and validation routines 
 * for the first page of the  'New JUnit TestCase Wizard'.
 * 
 * Clients can use the page as-is and add it to their own wizard, or extend it to modify
 * validation or add and remove controls.
 * 
 * @since 3.1
 */
public class NewTestCaseWizardPageOne extends NewTypeWizardPage {

	private final static String PAGE_NAME= "NewTestCaseCreationWizardPage"; //$NON-NLS-1$

	
	/** Field ID of the class under test field. */
	public final static String CLASS_UNDER_TEST= PAGE_NAME + ".classundertest"; //$NON-NLS-1$

	private final static String QUESTION_MARK_TAG= "Q"; //$NON-NLS-1$
	private final static String OF_TAG= "Of"; //$NON-NLS-1$

	private final static String TEST_SUFFIX= "Test"; //$NON-NLS-1$
	private final static String SETUP= "setUp"; //$NON-NLS-1$
	private final static String TEARDOWN= "tearDown"; //$NON-NLS-1$
	private final static String PREFIX= "test"; //$NON-NLS-1$

	private final static String STORE_GENERATE_MAIN= PAGE_NAME + ".GENERATE_MAIN"; //$NON-NLS-1$
	private final static String STORE_USE_TESTRUNNER= PAGE_NAME + ".USE_TESTRUNNER";	//$NON-NLS-1$
	private final static String STORE_TESTRUNNER_TYPE= PAGE_NAME + ".TESTRUNNER_TYPE"; //$NON-NLS-1$
	
	private NewTestCaseWizardPageTwo fPage2;
	private MethodStubsSelectionButtonGroup fMethodStubsButtons;

	private String fClassUnderTestText; // model
	private IType fClassUnderTest; // resolved model, can be null
	
	private Text fClassUnderTestControl; // control
	private IStatus fClassUnderTestStatus; // status
	
	private Button fClassUnderTestButton;
	private JavaTypeCompletionProcessor fClassToTestCompletionProcessor;

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
		
		String[] buttonNames= new String[] {
			"&public static void main(String[] args)", //$NON-NLS-1$
			WizardMessages.NewTestCaseWizardPageOne_methodStub_testRunner, 
			WizardMessages.NewTestCaseWizardPageOne_methodStub_setUp, 
			WizardMessages.NewTestCaseWizardPageOne_methodStub_tearDown, 
			WizardMessages.NewTestCaseWizardPageOne_methodStub_constructor
		};
		
		fMethodStubsButtons= new MethodStubsSelectionButtonGroup(SWT.CHECK, buttonNames, 1);
		fMethodStubsButtons.setLabelText(WizardMessages.NewTestCaseWizardPageOne_method_Stub_label); 
		
		fClassToTestCompletionProcessor= new JavaTypeCompletionProcessor(false, false); //$NON-NLS-1$

		fClassUnderTestStatus= new JUnitStatus();
		
		fClassUnderTestText= ""; //$NON-NLS-1$
	}

	/**
	 * Initialized the page with the current selection
	 * @param selection The selection
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement element= getInitialJavaElement(selection);

		initContainerPage(element);
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
							if (cf.isStructureKnown())
								classToTest= cf.getType();
						} catch(JavaModelException e) {
							JUnitPlugin.log(e);
						}
					}					
				}
			}
			if (classToTest != null) {
				try {
					if (!TestSearchEngine.isTestImplementor(classToTest)) {
						setClassUnderTest(classToTest.getFullyQualifiedName('.'));
					}
				} catch (JavaModelException e) {
					JUnitPlugin.log(e);
				}
			}
		}
		fMethodStubsButtons.setSelection(0, false); //main
		fMethodStubsButtons.setSelection(1, false); //add textrunner
		fMethodStubsButtons.setEnabled(1, false); //add text
		fMethodStubsButtons.setSelection(2, false); //setUp
		fMethodStubsButtons.setSelection(3, false); //tearDown
		fMethodStubsButtons.setSelection(4, false); //constructor
		
		updateStatus(getStatusList());
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewContainerWizardPage#handleFieldChanged(String)
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName.equals(CONTAINER)) {
			fClassUnderTestStatus= classUnderTestChanged();
			if (fClassUnderTestButton != null && !fClassUnderTestButton.isDisposed()) {
				fClassUnderTestButton.setEnabled(getPackageFragmentRoot() != null);
			}
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
				fSuperClassStatus
		};
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		
		int nColumns= 4;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;		
		composite.setLayout(layout);

		createContainerControls(composite, nColumns);	
		createPackageControls(composite, nColumns);
		createSeparator(composite, nColumns);
		createTypeNameControls(composite, nColumns);		
		createSuperClassControls(composite, nColumns);
		createMethodStubSelectionControls(composite, nColumns);
		setSuperClass(JUnitPlugin.TEST_SUPERCLASS_NAME, true);
		createSeparator(composite, nColumns);
		createClassUnderTestControls(composite, nColumns);
		
		setControl(composite);
			
		//set default and focus
		String classUnderTest= getClassUnderTestText();
		if (classUnderTest.length() > 0) {
			setTypeName(Signature.getSimpleName(classUnderTest)+TEST_SUFFIX, true);
		}
		restoreWidgetValues();
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJUnitHelpContextIds.NEW_TESTCASE_WIZARD_PAGE);	

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
		fClassUnderTestControl.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				internalSetClassUnderText(((Text) e.widget).getText());
			}
		});
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= true;
		gd.horizontalSpan= nColumns - 2;
		fClassUnderTestControl.setLayoutData(gd);
		
		fClassUnderTestButton= new Button(composite, SWT.PUSH);
		fClassUnderTestButton.setText(WizardMessages.NewTestCaseWizardPageOne_class_to_test_browse); 
		fClassUnderTestButton.setEnabled(true);
		fClassUnderTestButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				classToTestButtonPressed();
			}
			public void widgetSelected(SelectionEvent e) {
				classToTestButtonPressed();
			}
		});	
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= false;
		gd.horizontalSpan= 1;
		gd.widthHint = SWTUtil.getButtonWidthHint(fClassUnderTestButton);		
		fClassUnderTestButton.setLayoutData(gd);

		ControlContentAssistHelper.createTextContentAssistant(fClassUnderTestControl, fClassToTestCompletionProcessor);
		setFocus();
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
			SelectionDialog dialog= JavaUI.createTypeDialog(getShell(), getWizard().getContainer(), scope, IJavaElementSearchConstants.CONSIDER_CLASSES, false, getClassUnderTestText());
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

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#packageChanged()
	 */
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
		IStatus val= JavaConventions.validateJavaTypeName(classToTestName);
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
				status.setWarning(Messages.format(WizardMessages.NewTestCaseWizardPageOne_warning_class_to_test_is_interface,classToTestName)); 
			}
			
			if (pack != null && !JUnitStubUtility.isVisible(type, pack)) {
				status.setWarning(Messages.format(WizardMessages.NewTestCaseWizardPageOne_warning_class_to_test_not_visible, new String[] {(type.isInterface())?WizardMessages.NewTestCaseWizardPageOne_Interface:WizardMessages.NewTestCaseWizardPageOne_Class , classToTestName})); 
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

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#createTypeMembers(org.eclipse.jdt.core.IType, org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void createTypeMembers(IType type, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		if (fMethodStubsButtons.isSelected(0)) 
			createMain(type);
		
		if (fMethodStubsButtons.isSelected(4))
			createConstructor(type, imports); 	
		
		if (fMethodStubsButtons.isSelected(2)) {
			createSetUp(type, imports);
		}
		
		if (fMethodStubsButtons.isSelected(3)) {
			createTearDown(type, imports);
		}

		if (fClassUnderTest != null) {
			createTestMethodStubs(type);
		}
	}

	private void createConstructor(IType type, ImportsManager imports) throws JavaModelException {
		ITypeHierarchy typeHierarchy= null;
		IType[] superTypes= null;
		String content= ""; //$NON-NLS-1$
		IMethod methodTemplate= null;
		if (type.exists()) {
			typeHierarchy= type.newSupertypeHierarchy(null);
			superTypes= typeHierarchy.getAllSuperclasses(type);
			for (int i= 0; i < superTypes.length; i++) {
				if (superTypes[i].exists()) {
					IMethod constrMethod= superTypes[i].getMethod(superTypes[i].getElementName(), new String[] {"Ljava.lang.String;"}); //$NON-NLS-1$
					if (constrMethod.exists() && constrMethod.isConstructor()) {
						methodTemplate= constrMethod;
						break;
					}
				}
			}
		}
		if (methodTemplate != null) {
			GenStubSettings settings= JUnitStubUtility.getCodeGenerationSettings(type.getJavaProject());
			settings.fCallSuper= true;				
			settings.fMethodOverwrites= true;
			content= JUnitStubUtility.genStub(getTypeName(), methodTemplate, settings, imports);
		} else {
			final String delimiter= getLineDelimiter();
			StringBuffer buffer= new StringBuffer(32);
			buffer.append("public "); //$NON-NLS-1$
			buffer.append(getTypeName());
			buffer.append("("); //$NON-NLS-1$
			buffer.append(imports.addImport("java.lang.String")); //$NON-NLS-1$
			buffer.append(" name) {"); //$NON-NLS-1$
			buffer.append(delimiter);
			buffer.append("super(name);"); //$NON-NLS-1$
			buffer.append(delimiter);
			buffer.append("}"); //$NON-NLS-1$
			buffer.append(delimiter);
			content += buffer.toString();
		}
		type.createMethod(content, null, true, null);	
	}

	private void createMain(IType type) throws JavaModelException {
		type.createMethod(fMethodStubsButtons.getMainMethod(getTypeName()), null, false, null);	
	}

	private void createSetUp(IType type, ImportsManager imports) throws JavaModelException {
		ITypeHierarchy typeHierarchy= null;
		IType[] superTypes= null;
		String content= ""; //$NON-NLS-1$
		IMethod methodTemplate= null;
		if (type.exists()) {
			typeHierarchy= type.newSupertypeHierarchy(null);
			superTypes= typeHierarchy.getAllSuperclasses(type);
			for (int i= 0; i < superTypes.length; i++) {
				if (superTypes[i].exists()) {
					IMethod testMethod= superTypes[i].getMethod(SETUP, new String[] {});
					if (testMethod.exists()) {
						methodTemplate= testMethod;
						break;
					}
				}
			}
		}
		
		GenStubSettings settings= JUnitStubUtility.getCodeGenerationSettings(type.getJavaProject());
		if (methodTemplate != null) {
			settings.fCallSuper= true;
			settings.fMethodOverwrites= true;
			content= JUnitStubUtility.genStub(getTypeName(), methodTemplate, settings, imports);
		} else {
			final String delimiter= getLineDelimiter();
			StringBuffer buffer= new StringBuffer();
			if (settings.createComments) {
				buffer.append("/**"); //$NON-NLS-1$
				buffer.append(delimiter);
				buffer.append(" * Sets up the fixture, for example, open a network connection."); //$NON-NLS-1$
				buffer.append(delimiter);
				buffer.append(" * This method is called before a test is executed."); //$NON-NLS-1$
				buffer.append(delimiter);
				buffer.append(delimiter);
				buffer.append(" * @throws "); //$NON-NLS-1$
				buffer.append(imports.addImport("java.lang.Exception")); //$NON-NLS-1$
				buffer.append(delimiter);
				buffer.append(" */"); //$NON-NLS-1$
				buffer.append(delimiter);
			}
			buffer.append("protected void "); //$NON-NLS-1$
			buffer.append(SETUP);
			buffer.append("() throws "); //$NON-NLS-1$
			buffer.append(imports.addImport("java.lang.Exception")); //$NON-NLS-1$
			buffer.append(" {}"); //$NON-NLS-1$
			buffer.append(delimiter);
		}
		type.createMethod(content, null, false, null);
	}
	
	private void createTearDown(IType type, ImportsManager imports) throws JavaModelException {
		ITypeHierarchy typeHierarchy= null;
		IType[] superTypes= null;
		String tearDown= ""; //$NON-NLS-1$
		IMethod methodTemplate= null;
		if (type.exists()) {
			typeHierarchy= type.newSupertypeHierarchy(null);
			superTypes= typeHierarchy.getAllSuperclasses(type);
			for (int i= 0; i < superTypes.length; i++) {
				if (superTypes[i].exists()) {
					IMethod testM= superTypes[i].getMethod(TEARDOWN, new String[] {});
					if (testM.exists()) {
						methodTemplate= testM;
						break;
					}
				}
			}
		}
		if (methodTemplate != null) {
			GenStubSettings settings= JUnitStubUtility.getCodeGenerationSettings(type.getJavaProject());
			settings.fCallSuper= true;
			settings.fMethodOverwrites= true;
			tearDown= JUnitStubUtility.genStub(getTypeName(), methodTemplate, settings, imports);
			type.createMethod(tearDown, null, false, null);	
		}				
	}

	private void createTestMethodStubs(IType type) throws JavaModelException {
		IMethod[] methods= fPage2.getCheckedMethods();
		if (methods.length == 0)
			return;
		/* find overloaded methods */
		IMethod[] allMethodsArray= fPage2.getAllMethods();
		List allMethods= new ArrayList();
		allMethods.addAll(Arrays.asList(allMethodsArray));
		List overloadedMethods= getOveloadedMethods(allMethods);
			
		/* used when for example both sum and Sum methods are present. Then
		 * sum -> testSum
		 * Sum -> testSum1
		 */
		List names= new ArrayList();				
			for (int i = 0; i < methods.length; i++) {
			IMethod method= methods[i];
			String elementName= method.getElementName();
			StringBuffer name= new StringBuffer(PREFIX).append(Character.toUpperCase(elementName.charAt(0))).append(elementName.substring(1));
			StringBuffer buffer= new StringBuffer();
	
			final boolean contains= overloadedMethods.contains(method);
			if (contains)
				appendParameterNamesToMethodName(name, method.getParameterTypes());

			replaceIllegalCharacters(name);
			/* Should I for examples have methods
			 * 	void foo(java.lang.StringBuffer sb) {}
			 *  void foo(mypackage1.StringBuffer sb) {}
			 *  void foo(mypackage2.StringBuffer sb) {}
			 * I will get in the test class:
			 *  testFooStringBuffer()
			 *  testFooStringBuffer1()
			 *  testFooStringBuffer2()
			 */
			String result= name.toString();
			if (names.contains(result)) {
				int suffix= 1;
				while (names.contains(result + Integer.toString(suffix)))
					suffix++;
				name.append(Integer.toString(suffix));
			}
			result= name.toString();
			names.add(result);
			appendMethodComment(buffer, method);
			buffer.append("public ");//$NON-NLS-1$ 
			if (fPage2.getCreateFinalMethodStubsButtonSelection())
				buffer.append("final "); //$NON-NLS-1$
			buffer.append("void ");//$NON-NLS-1$ 
			buffer.append(result);
			buffer.append("()");//$NON-NLS-1$ 
			try {
				appendTestMethodBody(buffer, result, method);
			} catch (CoreException exception) {
				throw new JavaModelException(exception);
			}
			type.createMethod(buffer.toString(), null, false, null);	
		}
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
			else if (!Character.isJavaIdentifierPart(character))
				buffer.deleteCharAt(index);
		}
	}

	private String getLineDelimiter(){
		IType classToTest= getClassUnderTest();
		
		if (classToTest != null && classToTest.exists())
			return StubUtility.getLineDelimiterUsed(classToTest);
		
		return StubUtility.getLineDelimiterUsed(getPackageFragment());
	}

	private void appendTestMethodBody(StringBuffer buffer, String name, IMethod method) throws CoreException {
		final String delimiter= getLineDelimiter();
		buffer.append("{").append(delimiter); //$NON-NLS-1$
		if (fPage2.isCreateTasks()) {
			final String content= StubUtility.getMethodBodyContent(false, method.getJavaProject(), CLASS_UNDER_TEST, name, "", delimiter); //$NON-NLS-1$
			if (content != null && content.length() > 0)
				buffer.append(content);
		}
		buffer.append(delimiter).append("}").append(delimiter).append(delimiter); //$NON-NLS-1$
	}

	private void appendParameterNamesToMethodName(StringBuffer buffer, String[] parameters) {
		for (int i= 0; i < parameters.length; i++) {
			final StringBuffer buf= new StringBuffer(Signature.getSimpleName(Signature.toString(Signature.getElementType(parameters[i]))));
			final char character= buf.charAt(0);
			if (buf.length() > 0 && !Character.isUpperCase(character))
				buf.setCharAt(0, Character.toUpperCase(character));
			buffer.append(buf.toString());
			for (int j= 0, arrayCount= Signature.getArrayCount(parameters[i]); j < arrayCount; j++) {
				buffer.append("Array"); //$NON-NLS-1$
			}
		}
	}

	private void appendMethodComment(StringBuffer buffer, IMethod method) throws JavaModelException {
		final String delimiter= getLineDelimiter();
		final StringBuffer buf= new StringBuffer(16);
		JavaElementLabels.getMethodLabel(method, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.M_FULLY_QUALIFIED, buf);
		buffer.append("/*");//$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append(" * ");//$NON-NLS-1$
		buffer.append(Messages.format(WizardMessages.NewTestCaseWizardPageOne_comment_class_to_test, buf.toString()));
		buffer.append(delimiter);
		buffer.append(" */");//$NON-NLS-1$
		buffer.append(delimiter);
	}

	private List getOveloadedMethods(List allMethods) {
		List overloadedMethods= new ArrayList();
		for (int i= 0; i < allMethods.size(); i++) {
			IMethod current= (IMethod) allMethods.get(i);
			String currentName= current.getElementName();
			boolean currentAdded= false;
			for (ListIterator iter= allMethods.listIterator(i+1); iter.hasNext(); ) {
				IMethod iterMethod= (IMethod) iter.next();
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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible) {
			saveWidgetValues();
		}
		
		//if (visible) setFocus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#containerChanged()
	 */
	protected IStatus containerChanged() {
		IStatus containerStatus= super.containerChanged();
		if (!containerStatus.matches(IStatus.ERROR)) {
			IStatus projectStatus= validateIfJUnitProject();
			if (!projectStatus.isOK()) {
				return projectStatus;
			}
		}
		return containerStatus;
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
		if (root == null)
			return status;
		
		IJavaProject jp= root.getJavaProject();
		
		try {
			if (jp.findType(JUnitPlugin.TEST_SUPERCLASS_NAME) != null)
				return status;
		} catch (JavaModelException e) {
		}
		if (MessageDialog.openQuestion(getShell(), WizardMessages.NewTestCaseWizardPageOne_not_on_buildpath_title, WizardMessages.NewTestCaseWizardPageOne_not_on_buildpath_message)) { 
			try {
				JUnitAddLibraryProposal.addJUnitToBuildPath(getShell(), jp);
				return status;
			} catch(JavaModelException e) {
				ErrorDialog.openError(getShell(), WizardMessages.NewTestCaseWizardPageOne_cannot_add_title, WizardMessages.NewTestCaseWizardPageOne_cannot_add_message, e.getStatus()); 
			}	
		}
		status.setWarning(WizardMessages.NewTestCaseWizardPageOne_error_junitNotOnbuildpath); 
		return status;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#superClassChanged()
	 */
	protected IStatus superClassChanged() {
		// replaces the super class validation of of the normal type wizard
		
		String superClassName= getSuperClass();
		JUnitStatus status= new JUnitStatus();
		if (superClassName == null || superClassName.trim().equals("")) { //$NON-NLS-1$
			status.setError(WizardMessages.NewTestCaseWizardPageOne_error_superclass_empty); 
			return status;	
		}
		if (getPackageFragmentRoot() != null) { //$NON-NLS-1$
			try {
				IType type= resolveClassNameToType(getPackageFragmentRoot().getJavaProject(), getPackageFragment(), superClassName);	
				if (type == null) {
					/* TODO: is this a warning or error? */
					status.setWarning(WizardMessages.NewTestCaseWizardPageOne_error_superclass_not_exist); 
					return status;	
				}
				if (type.isInterface()) {
					status.setError(WizardMessages.NewTestCaseWizardPageOne_error_superclass_is_interface); 
					return status;
				}
				if (!TestSearchEngine.isTestImplementor(type)) { // TODO: expensive!
					status.setError(Messages.format(WizardMessages.NewTestCaseWizardPageOne_error_superclass_not_implementing_test_interface, JUnitPlugin.TEST_INTERFACE_NAME)); 
					return status;
				}
			} catch (JavaModelException e) {
				JUnitPlugin.log(e);
			}
		}
		return status;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		return super.canFlipToNextPage() && getClassUnderTest() != null;
	}

	private IType resolveClassNameToType(IJavaProject jproject, IPackageFragment pack, String classToTestName) throws JavaModelException {
		
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
			boolean generateMain= settings.getBoolean(STORE_GENERATE_MAIN);
			fMethodStubsButtons.setSelection(0, generateMain);
			fMethodStubsButtons.setEnabled(1, generateMain);
			fMethodStubsButtons.setSelection(1,settings.getBoolean(STORE_USE_TESTRUNNER));
			try {
				fMethodStubsButtons.setComboSelection(settings.getInt(STORE_TESTRUNNER_TYPE));
			} catch(NumberFormatException e) {}
		}		
	}	

	/**
	 * 	Since Finish was pressed, write widget values to the dialog store so that they
	 *	will persist into the next invocation of this wizard page
	 */
	private void saveWidgetValues() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			settings.put(STORE_GENERATE_MAIN, fMethodStubsButtons.isSelected(0));
			settings.put(STORE_USE_TESTRUNNER, fMethodStubsButtons.isSelected(1));
			settings.put(STORE_TESTRUNNER_TYPE, fMethodStubsButtons.getComboSelection());
		}
	}

}
