/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids - bug 38507
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.junit.ui.IJUnitHelpContextIds;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jdt.internal.junit.util.SWTUtil;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility.GenStubSettings;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * The first page of the TestCase creation wizard. 
 */
public class NewTestCaseCreationWizardPage extends NewTypeWizardPage implements IAboutToRunOperation {

	private final static String PAGE_NAME= "NewTestCaseCreationWizardPage"; //$NON-NLS-1$
	private final static String CLASS_TO_TEST= PAGE_NAME + ".classtotest"; //$NON-NLS-1$
	private final static String TEST_SUFFIX= "Test"; //$NON-NLS-1$
	private final static String SETUP= "setUp"; //$NON-NLS-1$
	private final static String TEARDOWN= "tearDown"; //$NON-NLS-1$

	private final static String STORE_GENERATE_MAIN= PAGE_NAME + ".GENERATE_MAIN"; //$NON-NLS-1$
	private final static String STORE_USE_TESTRUNNER= PAGE_NAME + ".USE_TESTRUNNER";	//$NON-NLS-1$
	private final static String STORE_TESTRUNNER_TYPE= PAGE_NAME + ".TESTRUNNER_TYPE"; //$NON-NLS-1$
	
	private String fDefaultClassToTest;
	private NewTestCaseCreationWizardPage2 fPage2;
	private MethodStubsSelectionButtonGroup fMethodStubsButtons;

	private IType fClassToTest;
	private IStatus fClassToTestStatus;
	private IStatus fTestClassStatus;

	private int fIndexOfFirstTestMethod;

	private Label fClassToTestLabel;
	private Text fClassToTestText;
	private Button fClassToTestButton;
	private JavaTypeCompletionProcessor fClassToTestCompletionProcessor;
	
	private boolean fFirstTime;
	private String fMainMethod;  

	public NewTestCaseCreationWizardPage() {
		super(true, PAGE_NAME);
		fFirstTime= true;
		
		setTitle(WizardMessages.getString("NewTestClassWizPage.title")); //$NON-NLS-1$
		setDescription(WizardMessages.getString("NewTestClassWizPage.description")); //$NON-NLS-1$
		
		String[] buttonNames= new String[] {
			"public static void main(Strin&g[] args)", //$NON-NLS-1$
			/* Add testrunner statement to main Method */
			WizardMessages.getString("NewTestClassWizPage.methodStub.testRunner"), //$NON-NLS-1$
			WizardMessages.getString("NewTestClassWizPage.methodStub.setUp"), //$NON-NLS-1$
			WizardMessages.getString("NewTestClassWizPage.methodStub.tearDown"), //$NON-NLS-1$
			WizardMessages.getString("NewTestClassWizPage.methodStub.constructor") //$NON-NLS-1$
		};
		
		fMethodStubsButtons= new MethodStubsSelectionButtonGroup(SWT.CHECK, buttonNames, 1);
		fMethodStubsButtons.setLabelText(WizardMessages.getString("NewTestClassWizPage.method.Stub.label")); //$NON-NLS-1$
		
		fClassToTestCompletionProcessor= new JavaTypeCompletionProcessor(false, false); //$NON-NLS-1$

		fClassToTestStatus= new JUnitStatus();
		fTestClassStatus= new JUnitStatus();
		
		fDefaultClassToTest= ""; //$NON-NLS-1$
	}

	/**
	 * Should be called from the wizard with the initial selection and the 2nd page of the wizard..
	 */
	public void init(IStructuredSelection selection, NewTestCaseCreationWizardPage2 page2) {
		fPage2= page2;
		IJavaElement element= getInitialJavaElement(selection);

		initContainerPage(element);
		initTypePage(element);
		doStatusUpdate();		
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
						fDefaultClassToTest= classToTest.getFullyQualifiedName();
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
	}
	
	/**
	 * @see org.eclipse.jdt.ui.wizards.NewContainerWizardPage#handleFieldChanged(String)
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName.equals(CLASS_TO_TEST)) {
			fClassToTestStatus= classToTestClassChanged();
		} else if (fieldName.equals(SUPER)) {
			validateSuperClass(); 
			if (!fFirstTime)
				fTestClassStatus= typeNameChanged();	
		} else if (fieldName.equals(TYPENAME)) {
			fTestClassStatus= typeNameChanged();
		} else if (fieldName.equals(PACKAGE) || fieldName.equals(CONTAINER) || fieldName.equals(SUPER)) {
			if (fieldName.equals(PACKAGE))
				fPackageStatus= packageChanged();
			if (!fFirstTime) {
				validateSuperClass();
				fClassToTestStatus= classToTestClassChanged();			
				fTestClassStatus= typeNameChanged();
			}
			if (fieldName.equals(CONTAINER)) {
				validateJUnitOnBuildPath(); 
			}
		}
		doStatusUpdate();
	}

	// ------ validation --------
	private void doStatusUpdate() {
		// status of all used components
		IStatus[] status= new IStatus[] {
			fContainerStatus,
			fPackageStatus,
			fTestClassStatus,
			fClassToTestStatus,
			fModifierStatus,
			fSuperClassStatus
		};
		
		// the mode severe status will be displayed and the ok button enabled/disabled.
		updateStatus(status);
	}

	/*
	 * @see IDialogPage#createControl(Composite)
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
		createClassToTestControls(composite, nColumns);
		
		setControl(composite);
			
		//set default and focus
		if (fDefaultClassToTest.length() > 0) {
			fClassToTestText.setText(fDefaultClassToTest);
			setTypeName(Signature.getSimpleName(fDefaultClassToTest)+TEST_SUFFIX, true);
		}
		restoreWidgetValues();
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IJUnitHelpContextIds.NEW_TESTCASE_WIZARD_PAGE);	

	}

	private void createMethodStubSelectionControls(Composite composite, int nColumns) {
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getLabelControl(composite), nColumns);
		LayoutUtil.createEmptySpace(composite,1);
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getSelectionButtonsGroup(composite), nColumns - 1);	
	}	

	private void createClassToTestControls(Composite composite, int nColumns) {
		fClassToTestLabel= new Label(composite, SWT.LEFT | SWT.WRAP);
		fClassToTestLabel.setFont(composite.getFont());

		fClassToTestLabel.setText(WizardMessages.getString("NewTestClassWizPage.class_to_test.label")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= 1;
		fClassToTestLabel.setLayoutData(gd);

		fClassToTestText= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fClassToTestText.setEnabled(true);
		fClassToTestText.setFont(composite.getFont());
		fClassToTestText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				handleFieldChanged(CLASS_TO_TEST);
			}
		});
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= true;
		gd.horizontalSpan= nColumns - 2;
		fClassToTestText.setLayoutData(gd);
		
		fClassToTestButton= new Button(composite, SWT.PUSH);
		fClassToTestButton.setText(WizardMessages.getString("NewTestClassWizPage.class_to_test.browse")); //$NON-NLS-1$
		fClassToTestButton.setEnabled(true);
		fClassToTestButton.addSelectionListener(new SelectionListener() {
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
		gd.widthHint = SWTUtil.getButtonWidthHint(fClassToTestButton);		
		fClassToTestButton.setLayoutData(gd);

		ControlContentAssistHelper.createTextContentAssistant(fClassToTestText, fClassToTestCompletionProcessor);
	}

	private void classToTestButtonPressed() {
		IType type= chooseClassToTestType();
		if (type != null) {
			fClassToTestText.setText(JavaModelUtil.getFullyQualifiedName(type));
			handleFieldChanged(CLASS_TO_TEST);
		}
	}

	private IType chooseClassToTestType() {	
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) 
			return null;

		IJavaElement[] elements= new IJavaElement[] { root.getJavaProject() };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		
		IType type= null;
		try {		
			SelectionDialog dialog= JavaUI.createTypeDialog(getShell(), getWizard().getContainer(), scope, IJavaElementSearchConstants.CONSIDER_CLASSES, false, getClassToTestText());
			dialog.setTitle(WizardMessages.getString("NewTestClassWizPage.class_to_test.dialog.title")); //$NON-NLS-1$
			dialog.setMessage(WizardMessages.getString("NewTestClassWizPage.class_to_test.dialog.message")); //$NON-NLS-1$
			dialog.open();
			if (dialog.getReturnCode() != Window.OK)
				return type;
			else {
				Object[] resultArray= dialog.getResult();
				if (resultArray != null && resultArray.length > 0)
					type= (IType) resultArray[0];
			}
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		}
		return type;
	}

	/*
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#packageChanged()
	 */
	protected IStatus packageChanged() {
		IStatus status= super.packageChanged();
		fClassToTestCompletionProcessor.setPackageFragment(getPackageFragment());
		return status;
	}
	
	private IStatus classToTestClassChanged() {
		fClassToTestButton.setEnabled(getPackageFragmentRoot() != null);	// sets the test class field?
		IStatus status= validateClassToTest();
		return status;
	}

	/**
	 * Returns the content of the class to test text field.
	 */
	public String getClassToTestText() {
		return fClassToTestText.getText();
	}
	
	/**
	 * Returns the class to be tested.
	 */
	public IType getClassToTest() {
		return fClassToTest;
	}

	/**
	 * Sets the name of the class to test.
	 */		
	public void setClassToTest(String name) {
		fClassToTestText.setText(name);
	}	

	/**
	 * @see NewTypeWizardPage#createTypeMembers(IType, ImportsManager, IProgressMonitor)
	 */
	protected void createTypeMembers(IType type, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		fIndexOfFirstTestMethod= 0;

		if (fMethodStubsButtons.isSelected(0)) 
			createMain(type);
		
		if (fMethodStubsButtons.isSelected(2)) {
			createSetUp(type, imports);
		}
		
		if (fMethodStubsButtons.isSelected(3)) {
			createTearDown(type, imports);
		}

		if (fMethodStubsButtons.isSelected(4))
			createConstructor(type, imports); 		

		if (fClassToTest != null) {
			createTestMethodStubs(type);
		}
	}

	private void createConstructor(IType type, ImportsManager imports) throws JavaModelException {
		ITypeHierarchy typeHierarchy= null;
		IType[] superTypes= null;
		String constr= ""; //$NON-NLS-1$
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
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		if (methodTemplate != null) {
			GenStubSettings genStubSettings= new GenStubSettings(settings);
			genStubSettings.fCallSuper= true;				
			genStubSettings.fMethodOverwrites= true;
			constr= JUnitStubUtility.genStub(getTypeName(), methodTemplate, genStubSettings, imports);
		} else {
			constr += "public "+getTypeName()+"(String name) {" + //$NON-NLS-1$  //$NON-NLS-2$ 
					getLineDelimiter() +
					"super(name);" + //$NON-NLS-1$ 
					getLineDelimiter() +
					"}" + //$NON-NLS-1$ 
					getLineDelimiter() + getLineDelimiter();
		}
		type.createMethod(constr, null, true, null);	
		fIndexOfFirstTestMethod++;
	}

	private void createMain(IType type) throws JavaModelException {
		type.createMethod(fMainMethod, null, false, null);	
		fIndexOfFirstTestMethod++;		
	}

	private void createSetUp(IType type, ImportsManager imports) throws JavaModelException {
		ITypeHierarchy typeHierarchy= null;
		IType[] superTypes= null;
		String setUp= ""; //$NON-NLS-1$
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
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		if (methodTemplate != null) {
			GenStubSettings genStubSettings= new GenStubSettings(settings);
			genStubSettings.fCallSuper= true;				
			genStubSettings.fMethodOverwrites= true;
			setUp= JUnitStubUtility.genStub(getTypeName(), methodTemplate, genStubSettings, imports);
		} else {
			if (settings.createComments)
				setUp= "/**" + //$NON-NLS-1$
					getLineDelimiter() + 
					" * Sets up the fixture, for example, open a network connection." + //$NON-NLS-1$
					getLineDelimiter() +
					" * This method is called before a test is executed." + //$NON-NLS-1$
					getLineDelimiter() +
					" * @throws Exception" + //$NON-NLS-1$
					getLineDelimiter() +
					" */" + //$NON-NLS-1$
					getLineDelimiter(); 
			setUp+= "protected void "+SETUP+"() throws Exception {}" + //$NON-NLS-1$ //$NON-NLS-2$
				getLineDelimiter() + getLineDelimiter(); 
		}
		type.createMethod(setUp, null, false, null);	
		fIndexOfFirstTestMethod++;
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
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		if (methodTemplate != null) {
			GenStubSettings genStubSettings= new GenStubSettings(settings);				
			genStubSettings.fCallSuper= true;
			genStubSettings.fMethodOverwrites= true;
			tearDown= JUnitStubUtility.genStub(getTypeName(), methodTemplate, genStubSettings, imports);
			type.createMethod(tearDown, null, false, null);	
			fIndexOfFirstTestMethod++;
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
		List newMethodsNames= new ArrayList();				
			for (int i = 0; i < methods.length; i++) {
			IMethod testedMethod= methods[i];
			String elementName= testedMethod.getElementName();
			StringBuffer methodName= new StringBuffer(NewTestCaseCreationWizardPage2.PREFIX+Character.toUpperCase(elementName.charAt(0))+elementName.substring(1));
			StringBuffer newMethod= new StringBuffer();
	
			if (overloadedMethods.contains(testedMethod)) {
				appendMethodComment(newMethod, testedMethod);
				String[] params= testedMethod.getParameterTypes();
				appendParameterNamesToMethodName(methodName, params);
			}
			/* Should I for examples have methods
			 * 	void foo(java.lang.StringBuffer sb) {}
			 *  void foo(mypackage1.StringBuffer sb) {}
			 *  void foo(mypackage2.StringBuffer sb) {}
			 * I will get in the test class:
			 *  testFooStringBuffer()
			 *  testFooStringBuffer1()
			 *  testFooStringBuffer2()
			 */
			if (newMethodsNames.contains(methodName.toString())) {
				int suffix= 1;
				while (newMethodsNames.contains(methodName.toString() + Integer.toString(suffix)))
					suffix++;
				methodName.append(Integer.toString(suffix));
			}
			newMethodsNames.add(methodName.toString());
			newMethod.append("public ");//$NON-NLS-1$ 
			if (fPage2.getCreateFinalMethodStubsButtonSelection())
				newMethod.append("final "); //$NON-NLS-1$
			newMethod.append("void ");//$NON-NLS-1$ 
			newMethod.append(methodName.toString());
			newMethod.append("()");//$NON-NLS-1$ 
			appendTestMethodBody(newMethod, testedMethod);
			type.createMethod(newMethod.toString(), null, false, null);	
		}
	}
	
	private String getLineDelimiter(){
		IType classToTest= getClassToTest();
		
		if (classToTest != null && classToTest.exists())
			return JUnitStubUtility.getLineDelimiterUsed(classToTest);
		else	
			return JUnitStubUtility.getLineDelimiterUsed(getPackageFragment());
	}
	
	private void appendTestMethodBody(StringBuffer newMethod, IMethod testedMethod) {
		newMethod.append("{"); //$NON-NLS-1$
		if (createTasks()){
			newMethod.append(getLineDelimiter());
			newMethod.append("//"); //$NON-NLS-1$
			newMethod.append(JUnitStubUtility.getTodoTaskTag(getPackageFragment().getJavaProject()));
			newMethod.append(WizardMessages.getFormattedString("NewTestClassWizPage.marker.message", testedMethod.getElementName())); //$NON-NLS-1$
			newMethod.append(getLineDelimiter());		
		}
		newMethod.append("}").append(getLineDelimiter()).append(getLineDelimiter()); //$NON-NLS-1$
	}

	public void appendParameterNamesToMethodName(StringBuffer methodName, String[] params) {
		for (int i= 0; i < params.length; i++) {
			String param= params[i];
			methodName.append(Signature.getSimpleName(Signature.toString(Signature.getElementType(param))));
			for (int j= 0, arrayCount= Signature.getArrayCount(param); j < arrayCount; j++) {
				methodName.append("Array"); //$NON-NLS-1$
			}
		}
	}

	private void appendMethodComment(StringBuffer newMethod, IMethod method) throws JavaModelException {
		String returnType= Signature.toString(method.getReturnType());
		String body= WizardMessages.getFormattedString("NewTestClassWizPage.comment.class_to_test", new String[]{returnType, method.getElementName()}); //$NON-NLS-1$
		newMethod.append("/*");//$NON-NLS-1$
		newMethod.append(getLineDelimiter());
		newMethod.append(" * ");//$NON-NLS-1$
		newMethod.append(body);
		newMethod.append("(");//$NON-NLS-1$
		String[] paramTypes= method.getParameterTypes();
		if (paramTypes.length > 0) {
			if (paramTypes.length > 1) {
				for (int j= 0; j < paramTypes.length-1; j++) {
					newMethod.append(Signature.toString(paramTypes[j])+", "); //$NON-NLS-1$
				}
			}
			newMethod.append(Signature.toString(paramTypes[paramTypes.length-1]));
		}
		newMethod.append(")");//$NON-NLS-1$
		newMethod.append(getLineDelimiter());
		newMethod.append(" */");//$NON-NLS-1$
		newMethod.append(getLineDelimiter()); 
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

	/**
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible && fFirstTime) {
			if (getClassToTestText().equals("")) //$NON-NLS-1$
				setPageComplete(false);
			else
				handleFieldChanged(CLASS_TO_TEST); //creates error message when wizard is opened if TestCase already exists
			fFirstTime= false;
		}
		
		if (visible) setFocus();
	}

	private void validateJUnitOnBuildPath() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null)
			return;
		IJavaProject jp= root.getJavaProject();
		
		try {
			if (jp.findType(JUnitPlugin.TEST_SUPERCLASS_NAME) != null)
				return;
		} catch (JavaModelException e) {
		}
		if (MessageDialog.openQuestion(getShell(), WizardMessages.getString("NewTestClassWizPage.not_on_buildpath.title"), WizardMessages.getString("NewTestClassWizPage.not_on_buildpath.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
			try {
				addJUnitToBuildPath(getShell(), jp);
				return;
			} catch(JavaModelException e) {
				ErrorDialog.openError(getShell(), WizardMessages.getString("NewTestClassWizPage.cannot_add.title"), WizardMessages.getString("NewTestClassWizPage.cannot_add.message"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
			}	
		}
		JUnitStatus status= new JUnitStatus();
		status.setWarning(WizardMessages.getString("NewTestClassWizPage.error.junitNotOnbuildpath")); //$NON-NLS-1$		
		fContainerStatus= status;
	}
	
	public static void addJUnitToBuildPath(Shell shell, IJavaProject project) throws JavaModelException {
		IProject junitProject= ResourcesPlugin.getWorkspace().getRoot().getProject("org.junit"); //$NON-NLS-1$
		IClasspathEntry entry;
		if (junitProject.exists()) {
			entry= JavaCore.newProjectEntry(junitProject.getFullPath());
		} else {
			IPath junitHome= new Path(JUnitPlugin.JUNIT_HOME);
			IPath sourceHome= new Path("ECLIPSE_HOME"); //$NON-NLS-1$
			entry= JavaCore.newVariableEntry(
				junitHome.append("junit.jar"),   //$NON-NLS-1$
				//TODO: find a better solution than declaring a classpath variable
				sourceHome.append("plugins/org.eclipse.jdt.source_3.0.0/src/org.junit_3.8.1/junitsrc.zip"),  //$NON-NLS-1$
				null
			);
		}
		addToClasspath(shell, project, entry);
	}	
	
	private static void addToClasspath(Shell shell, final IJavaProject project, IClasspathEntry entry) throws JavaModelException {
		IClasspathEntry[] oldEntries= project.getRawClasspath();
		for (int i= 0; i < oldEntries.length; i++) {
			if (oldEntries[i].equals(entry)) {
				return;
			}
		}
		int nEntries= oldEntries.length;
		final IClasspathEntry[] newEntries= new IClasspathEntry[nEntries + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, nEntries);
		newEntries[nEntries]= entry;
		// fix for 64974 OCE in New JUnit Test Case wizard while workspace is locked [JUnit] 
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						project.setRawClasspath(newEntries, monitor);
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof CoreException) {	
				ErrorDialog.openError(shell, WizardMessages.getString("NewTestClassWizPage.cannot_add.title"), WizardMessages.getString("NewTestClassWizPage.cannot_add.message"), ((CoreException)t).getStatus());  //$NON-NLS-1$//$NON-NLS-2$
			}
		} catch (InterruptedException e) {
			return;
		}
	}


	/**
	 * Returns the index of the first method that is a test method, i.e. excluding main, setUp() and tearDown().
	 * If none of the aforementioned method stubs is created, then 0 is returned. As such method stubs are created,
	 * this counter is incremented.
	 */
	public int getIndexOfFirstMethod() {
		return fIndexOfFirstTestMethod;
	}

	private boolean createTasks() {
		return fPage2.getCreateTasksButtonSelection();
	}	

	private void validateSuperClass() {
		fMethodStubsButtons.setEnabled(2, true);//enable setUp() checkbox
		fMethodStubsButtons.setEnabled(3, true);//enable tearDown() checkbox
		String superClassName= getSuperClass();
		if (superClassName == null || superClassName.trim().equals("")) { //$NON-NLS-1$
			fSuperClassStatus= new JUnitStatus();
			((JUnitStatus)fSuperClassStatus).setError(WizardMessages.getString("NewTestClassWizPage.error.superclass.empty")); //$NON-NLS-1$
			return;	
		}
		if (getPackageFragmentRoot() != null) { //$NON-NLS-1$
			try {
				IType type= resolveClassNameToType(getPackageFragmentRoot().getJavaProject(), getPackageFragment(), superClassName);
				JUnitStatus status = new JUnitStatus();				
				if (type == null) {
					/* TODO: is this a warning or error? */
					status.setWarning(WizardMessages.getString("NewTestClassWizPage.error.superclass.not_exist")); //$NON-NLS-1$
					fSuperClassStatus= status;
				} else {
					if (type.isInterface()) {
						status.setError(WizardMessages.getString("NewTestClassWizPage.error.superclass.is_interface")); //$NON-NLS-1$
						fSuperClassStatus= status;
					}
					if (!TestSearchEngine.isTestImplementor(type)) {
						status.setError(WizardMessages.getFormattedString("NewTestClassWizPage.error.superclass.not_implementing_test_interface", JUnitPlugin.TEST_INTERFACE_NAME)); //$NON-NLS-1$
						fSuperClassStatus= status;
					} else {
						IMethod setupMethod= type.getMethod(SETUP, new String[] {});
						IMethod teardownMethod= type.getMethod(TEARDOWN, new String[] {});
						if (setupMethod.exists())
							fMethodStubsButtons.setEnabled(2, !Flags.isFinal(setupMethod.getFlags()));
						if (teardownMethod.exists())
							fMethodStubsButtons.setEnabled(3, !Flags.isFinal(teardownMethod.getFlags()));
					}
				}
			} catch (JavaModelException e) {
				JUnitPlugin.log(e);
			}
		}
	}
		
	/**
	 * @see org.eclipse.jface.wizard.IWizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		return isPageComplete() && getNextPage() != null && isNextPageValid();
	}

	protected boolean isNextPageValid() {
		return !getClassToTestText().equals(""); //$NON-NLS-1$
	}

	private JUnitStatus validateClassToTest() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		IPackageFragment pack= getPackageFragment();
		String classToTestName= getClassToTestText();
		JUnitStatus status= new JUnitStatus();
		
		fClassToTest= null;
		if (classToTestName.length() == 0) {
			return status;
		}
		IStatus val= JavaConventions.validateJavaTypeName(classToTestName);
//		if (!val.isOK()) {
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(WizardMessages.getString("NewTestClassWizPage.error.class_to_test.not_valid")); //$NON-NLS-1$
			return status;
		}
		
		if (root != null) {
			try {		
				IType type= resolveClassNameToType(root.getJavaProject(), pack, classToTestName);
				if (type == null) {
					//status.setWarning("Warning: "+typeLabel+" does not exist in current project.");
					status.setError(WizardMessages.getString("NewTestClassWizPage.error.class_to_test.not_exist")); //$NON-NLS-1$
					return status;
				} else {
					if (type.isInterface()) {
						status.setWarning(WizardMessages.getFormattedString("NewTestClassWizPage.warning.class_to_test.is_interface",classToTestName)); //$NON-NLS-1$
					}
					if (pack != null && !JavaModelUtil.isVisible(type, pack)) {
						status.setWarning(WizardMessages.getFormattedString("NewTestClassWizPage.warning.class_to_test.not_visible", new String[] {(type.isInterface())?WizardMessages.getString("Interface"):WizardMessages.getString("Class") , classToTestName})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
				fClassToTest= type;
			} catch (JavaModelException e) {
				status.setError(WizardMessages.getString("NewTestClassWizPage.error.class_to_test.not_valid")); //$NON-NLS-1$
			}							
		} else {
			status.setError(""); //$NON-NLS-1$
		}
		return status;
	}

	private IType resolveClassNameToType(IJavaProject jproject, IPackageFragment pack, String classToTestName) throws JavaModelException {
		IType type= null;
		if (type == null && pack != null) {
			String packName= pack.getElementName();
			// search in own package
			if (!pack.isDefaultPackage()) {
				type= jproject.findType(packName, classToTestName);
			}
			// search in java.lang
			if (type == null && !"java.lang".equals(packName)) { //$NON-NLS-1$
				type= jproject.findType("java.lang", classToTestName); //$NON-NLS-1$
			}
		}
		// search fully qualified
		if (type == null) {
			type= jproject.findType(classToTestName);
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
	void saveWidgetValues() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			settings.put(STORE_GENERATE_MAIN, fMethodStubsButtons.isSelected(0));
			settings.put(STORE_USE_TESTRUNNER, fMethodStubsButtons.isSelected(1));
			settings.put(STORE_TESTRUNNER_TYPE, fMethodStubsButtons.getComboSelection());
		}
	}

	public void aboutToRunOperation() {
		fMainMethod= fMethodStubsButtons.getMainMethod(getTypeName());	
	}

}
