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
package org.eclipse.jdt.internal.junit.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.ui.IJUnitHelpContextIds;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jdt.internal.junit.util.SWTUtil;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Wizard page to select the test classes to include
 * in the test suite.
 */
public class NewTestSuiteCreationWizardPage extends NewTypeWizardPage {

	private final static String PAGE_NAME= "NewTestSuiteCreationWizardPage"; //$NON-NLS-1$
	private final static String CLASSES_IN_SUITE= PAGE_NAME + ".classesinsuite"; //$NON-NLS-1$
	private final static String SUITE_NAME= PAGE_NAME + ".suitename"; //$NON-NLS-1$

	protected final static String STORE_GENERATE_MAIN= PAGE_NAME + ".GENERATE_MAIN"; //$NON-NLS-1$
	protected final static String STORE_USE_TESTRUNNER= PAGE_NAME + ".USE_TESTRUNNER";	//$NON-NLS-1$
	protected final static String STORE_TESTRUNNER_TYPE= PAGE_NAME + ".TESTRUNNER_TYPE"; //$NON-NLS-1$


	public static final String START_MARKER= "//$JUnit-BEGIN$"; //$NON-NLS-1$
	public static final String END_MARKER= "//$JUnit-END$"; //$NON-NLS-1$
	
	private CheckboxTableViewer fClassesInSuiteTable;	
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Label fSelectedClassesLabel;

	private Label fSuiteNameLabel;
	private Text fSuiteNameText;
	private String fSuiteNameTextInitialValue;
	private MethodStubsSelectionButtonGroup fMethodStubsButtons;
	
	private boolean fUpdatedExistingClassButton;

	protected IStatus fClassesInSuiteStatus;
	protected IStatus fSuiteNameStatus;
	
	public NewTestSuiteCreationWizardPage() {
		super(true, PAGE_NAME);

		fSuiteNameStatus= new JUnitStatus();
		fSuiteNameTextInitialValue= ""; //$NON-NLS-1$
		setTitle(WizardMessages.getString("NewTestSuiteWizPage.title")); //$NON-NLS-1$
		setDescription(WizardMessages.getString("NewTestSuiteWizPage.description")); //$NON-NLS-1$
		
		String[] buttonNames= new String[] {
			"public static void main(Strin&g[] args)", //$NON-NLS-1$
			/* Add testrunner statement to main Method */
			WizardMessages.getString("NewTestClassWizPage.methodStub.testRunner"), //$NON-NLS-1$
		};
		
		fMethodStubsButtons= new MethodStubsSelectionButtonGroup(SWT.CHECK, buttonNames, 1);
		fMethodStubsButtons.setLabelText(WizardMessages.getString("NewTestClassWizPage2.method.Stub.label")); //$NON-NLS-1$
		fMethodStubsButtons.setUseSuiteInMainForTextRunner(true);
		fClassesInSuiteStatus= new JUnitStatus();
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(Composite)
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
		createSuiteNameControl(composite, nColumns);
		setTypeName("AllTests", true); //$NON-NLS-1$
		createSeparator(composite, nColumns);
		createClassesInSuiteControl(composite, nColumns);
		createMethodStubSelectionControls(composite, nColumns);
		setControl(composite);
		restoreWidgetValues();
		Dialog.applyDialogFont(composite);	
		WorkbenchHelp.setHelp(composite, IJUnitHelpContextIds.NEW_TESTSUITE_WIZARD_PAGE);			
	}

	protected void createMethodStubSelectionControls(Composite composite, int nColumns) {
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getLabelControl(composite), nColumns);
		LayoutUtil.createEmptySpace(composite,1);
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getSelectionButtonsGroup(composite), nColumns - 1);	
	}	

	/**
	 * Should be called from the wizard with the initial selection.
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);
		initContainerPage(jelem);
		initTypePage(jelem);
		doStatusUpdate();

		fMethodStubsButtons.setSelection(0, false); //main
		fMethodStubsButtons.setSelection(1, false); //add textrunner
		fMethodStubsButtons.setEnabled(1, false); //add text
	}
	
	/**
	 * @see org.eclipse.jdt.ui.wizards.NewContainerWizardPage#handleFieldChanged(String)
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName.equals(PACKAGE) || fieldName.equals(CONTAINER)) {
			if (fieldName.equals(PACKAGE))
				fPackageStatus= packageChanged();
			updateClassesInSuiteTable();
		} else if (fieldName.equals(CLASSES_IN_SUITE)) {
			fClassesInSuiteStatus= classesInSuiteChanged();
			fSuiteNameStatus= testSuiteChanged(); //must check this one too
			updateSelectedClassesLabel();
		} else if (fieldName.equals(SUITE_NAME)) {
			fSuiteNameStatus= testSuiteChanged();
		}

		doStatusUpdate();
	}

	// ------ validation --------
	private void doStatusUpdate() {
		// status of all used components
		IStatus[] status= new IStatus[] {
			fContainerStatus,
			fPackageStatus,
			fSuiteNameStatus,
			fClassesInSuiteStatus			
		};
		
		// the most severe status will be displayed and the ok button enabled/disabled.
		updateStatus(status);
	}

	/**
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();		
			updateClassesInSuiteTable();
			handleAllFieldsChanged();
		}
	}

	private void handleAllFieldsChanged() {
		handleFieldChanged(PACKAGE);
		handleFieldChanged(CONTAINER);
		handleFieldChanged(CLASSES_IN_SUITE);
		handleFieldChanged(SUITE_NAME);
	}

	protected void updateClassesInSuiteTable() {
		if (fClassesInSuiteTable != null) {
			IPackageFragment pack= getPackageFragment();
			if (pack == null) {
				IPackageFragmentRoot root= getPackageFragmentRoot();
				if (root != null)
					pack= root.getPackageFragment(""); //$NON-NLS-1$
				else
					return;
			}
			fClassesInSuiteTable.setInput(pack);
			fClassesInSuiteTable.setAllChecked(true);
			updateSelectedClassesLabel();	
		}
	}
	
	protected void createClassesInSuiteControl(Composite parent, int nColumns) {
		if (fClassesInSuiteTable == null) {

			Label label = new Label(parent, SWT.LEFT);
			label.setText(WizardMessages.getString("NewTestSuiteWizPage.classes_in_suite.label")); //$NON-NLS-1$
			GridData gd= new GridData();
			gd.horizontalAlignment = GridData.FILL;
			gd.horizontalSpan= nColumns;
			label.setLayoutData(gd);

			fClassesInSuiteTable= CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
			gd= new GridData(GridData.FILL_BOTH);
			gd.heightHint= 80;
			gd.horizontalSpan= nColumns-1;

			fClassesInSuiteTable.getTable().setLayoutData(gd);
			fClassesInSuiteTable.setContentProvider(new ClassesInSuitContentProvider());
			fClassesInSuiteTable.setLabelProvider(new JavaElementLabelProvider());
			fClassesInSuiteTable.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					handleFieldChanged(CLASSES_IN_SUITE);
				}
			});

			Composite buttonContainer= new Composite(parent, SWT.NONE);
			gd= new GridData(GridData.FILL_VERTICAL);
			buttonContainer.setLayoutData(gd);
			GridLayout buttonLayout= new GridLayout();
			buttonLayout.marginWidth= 0;
			buttonLayout.marginHeight= 0;
			buttonContainer.setLayout(buttonLayout);
	
			fSelectAllButton= new Button(buttonContainer, SWT.PUSH);
			fSelectAllButton.setText(WizardMessages.getString("NewTestSuiteWizPage.selectAll")); //$NON-NLS-1$
			GridData bgd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			bgd.heightHint = SWTUtil.getButtonHeigthHint(fSelectAllButton);
			bgd.widthHint = SWTUtil.getButtonWidthHint(fSelectAllButton);
			fSelectAllButton.setLayoutData(bgd);
			fSelectAllButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fClassesInSuiteTable.setAllChecked(true);
					handleFieldChanged(CLASSES_IN_SUITE);
				}
			});
	
			fDeselectAllButton= new Button(buttonContainer, SWT.PUSH);
			fDeselectAllButton.setText(WizardMessages.getString("NewTestSuiteWizPage.deselectAll")); //$NON-NLS-1$
			bgd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			bgd.heightHint = SWTUtil.getButtonHeigthHint(fDeselectAllButton);
			bgd.widthHint = SWTUtil.getButtonWidthHint(fDeselectAllButton);
			fDeselectAllButton.setLayoutData(bgd);
			fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fClassesInSuiteTable.setAllChecked(false);
					handleFieldChanged(CLASSES_IN_SUITE);
				}
			});

			// No of selected classes label
			fSelectedClassesLabel= new Label(parent, SWT.LEFT | SWT.WRAP);
			fSelectedClassesLabel.setFont(parent.getFont());
			updateSelectedClassesLabel();
			gd = new GridData();
			gd.horizontalSpan = 2;
			fSelectedClassesLabel.setLayoutData(gd);
		}
	}

	static class ClassesInSuitContentProvider implements IStructuredContentProvider {
			
		public Object[] getElements(Object parent) {
			if (! (parent instanceof IPackageFragment))
				return new Object[0];
			IPackageFragment pack= (IPackageFragment) parent;
			if (! pack.exists())
				return new Object[0];
			try {
				ICompilationUnit[] cuArray= pack.getCompilationUnits();
				List typesArrayList= new ArrayList();
				for (int i= 0; i < cuArray.length; i++) {
					ICompilationUnit cu= cuArray[i];
					IType[] types= cu.getTypes();
					for (int j= 0; j < types.length; j++) {
						IType type= types[j];
						if (type.isClass() && ! Flags.isAbstract(type.getFlags()) && TestSearchEngine.isTestImplementor(type))	
							typesArrayList.add(types[j]);
					}
				}
				return typesArrayList.toArray();
			} catch (JavaModelException e) {
				JUnitPlugin.log(e);
				return new Object[0];
			}
		}
		
		public void dispose() {
		}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	/*
	 * @see TypePage#evalMethods
	 */
	protected void createTypeMembers(IType type, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		writeImports(imports);
		if (fMethodStubsButtons.isEnabled() && fMethodStubsButtons.isSelected(0)) 
			createMain(type);
		type.createMethod(getSuiteMethodString(), null, false, null);	
	}

	protected void createMain(IType type) throws JavaModelException {
		type.createMethod(fMethodStubsButtons.getMainMethod(getTypeName()), null, false, null);	
	}

	/**
	 * Returns the string content for creating a new suite() method.
	 */
	public String getSuiteMethodString() {
		IPackageFragment pack= getPackageFragment();
		String packName= pack.getElementName();
		StringBuffer suite= new StringBuffer("public static Test suite () {TestSuite suite= new TestSuite(\"Test for "+((packName.equals(""))?"default package":packName)+"\");\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		suite.append(getUpdatableString());
		suite.append("\nreturn suite;}"); //$NON-NLS-1$
		return suite.toString();
	}
	
	/**
	 * Returns the new code to be included in a new suite() or which replaces old code in an existing suite().
	 */
	public static String getUpdatableString(Object[] selectedClasses) {
		StringBuffer suite= new StringBuffer();
		suite.append(START_MARKER+"\n"); //$NON-NLS-1$
		for (int i= 0; i < selectedClasses.length; i++) {
			if (selectedClasses[i] instanceof IType) {
				IType testType= (IType) selectedClasses[i];
				IMethod suiteMethod= testType.getMethod("suite", new String[] {}); //$NON-NLS-1$
				if (!suiteMethod.exists()) {
					suite.append("suite.addTestSuite("+testType.getElementName()+".class);"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					suite.append("suite.addTest("+testType.getElementName()+".suite());"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		suite.append("\n"+END_MARKER); //$NON-NLS-1$
		return suite.toString();
	}
	
	private String getUpdatableString() {
		return getUpdatableString(fClassesInSuiteTable.getCheckedElements());
	}

	/**
	 * Runnable for replacing an existing suite() method.
	 */
	public IRunnableWithProgress getRunnable() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					updateExistingClass(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 				
			}
		};
	}
	
	protected void updateExistingClass(IProgressMonitor monitor) throws CoreException, InterruptedException {
		IPackageFragment pack= getPackageFragment();
		ICompilationUnit cu= pack.getCompilationUnit(getTypeName() + ".java"); //$NON-NLS-1$
		
		if (!cu.exists()) {
			createType(monitor);
			fUpdatedExistingClassButton= false;
			return;
		}
		
		if (! UpdateTestSuite.checkValidateEditStatus(cu, getShell()))
			return;

		IType suiteType= cu.getType(getTypeName());
		monitor.beginTask(WizardMessages.getString("NewTestSuiteWizPage.createType.beginTask"), 10); //$NON-NLS-1$
		IMethod suiteMethod= suiteType.getMethod("suite", new String[] {}); //$NON-NLS-1$
		monitor.worked(1);
		
		String lineDelimiter= JUnitStubUtility.getLineDelimiterUsed(cu);
		if (suiteMethod.exists()) {
			ISourceRange range= suiteMethod.getSourceRange();
			if (range != null) {
				IBuffer buf= cu.getBuffer();
				String originalContent= buf.getText(range.getOffset(), range.getLength());
				StringBuffer source= new StringBuffer(originalContent);
				//using JDK 1.4
				//int start= source.toString().indexOf(START_MARKER) --> int start= source.indexOf(START_MARKER);
				int start= source.toString().indexOf(START_MARKER);
				if (start > -1) {
					//using JDK 1.4
					//int end= source.toString().indexOf(END_MARKER, start) --> int end= source.indexOf(END_MARKER, start)
					int end= source.toString().indexOf(END_MARKER, start);
					if (end > -1) {
						monitor.subTask(WizardMessages.getString("NewTestSuiteWizPage.createType.updating.suite_method")); //$NON-NLS-1$
						monitor.worked(1);
						end += END_MARKER.length();
						source.replace(start, end, getUpdatableString());
						buf.replace(range.getOffset(), range.getLength(), source.toString());
						cu.reconcile();  
						originalContent= buf.getText(0, buf.getLength());
						monitor.worked(1);
						String formattedContent=
							JUnitStubUtility.codeFormat(originalContent, 0, lineDelimiter);
						buf.replace(0, buf.getLength(), formattedContent);
						monitor.worked(1);
						cu.save(new SubProgressMonitor(monitor, 1), false);
					} else {
						cannotUpdateSuiteError();
					}
				} else {
					cannotUpdateSuiteError();
				}
			} else {
				MessageDialog.openError(getShell(), WizardMessages.getString("NewTestSuiteWizPage.createType.updateErrorDialog.title"), WizardMessages.getString("NewTestSuiteWizPage.createType.updateErrorDialog.message")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			suiteType.createMethod(getSuiteMethodString(), null, true, monitor);
			ISourceRange range= cu.getSourceRange();
			IBuffer buf= cu.getBuffer();
			String originalContent= buf.getText(range.getOffset(), range.getLength());
			monitor.worked(2);
			String formattedContent=
				JUnitStubUtility.codeFormat(originalContent, 0, lineDelimiter);
			buf.replace(range.getOffset(), range.getLength(), formattedContent);
			monitor.worked(1);
			cu.save(new SubProgressMonitor(monitor, 1), false);
		}
		monitor.done();
		fUpdatedExistingClassButton= true;
	}

	/**
	 * Returns true iff an existing suite() method has been replaced.
	 */
	public boolean hasUpdatedExistingClass() {
		return fUpdatedExistingClassButton;
	}
	
	private IStatus classesInSuiteChanged() {
		JUnitStatus status= new JUnitStatus();
		if (fClassesInSuiteTable.getCheckedElements().length <= 0)
			status.setWarning(WizardMessages.getString("NewTestSuiteWizPage.classes_in_suite.error.no_testclasses_selected")); //$NON-NLS-1$
		return status;
	}
	
	private void updateSelectedClassesLabel() {
		int noOfClassesChecked= fClassesInSuiteTable.getCheckedElements().length;
		String key= (noOfClassesChecked==1) ? "NewTestClassWizPage.treeCaption.classSelected" : "NewTestClassWizPage.treeCaption.classesSelected"; //$NON-NLS-1$ //$NON-NLS-2$
		fSelectedClassesLabel.setText(WizardMessages.getFormattedString(key, new Integer(noOfClassesChecked)));
	}

	protected void createSuiteNameControl(Composite composite, int nColumns) {
		fSuiteNameLabel= new Label(composite, SWT.LEFT | SWT.WRAP);
		fSuiteNameLabel.setFont(composite.getFont());
		fSuiteNameLabel.setText(WizardMessages.getString("NewTestSuiteWizPage.suiteName.text")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= 1;
		fSuiteNameLabel.setLayoutData(gd);

		fSuiteNameText= new Text(composite, SWT.SINGLE | SWT.BORDER);
		// moved up due to 1GEUNW2
		fSuiteNameText.setEnabled(true);
		fSuiteNameText.setFont(composite.getFont());
		fSuiteNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				handleFieldChanged(SUITE_NAME);
			}
		});
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= true;
		gd.horizontalSpan= nColumns - 2;
		fSuiteNameText.setLayoutData(gd);
		
		Label space= new Label(composite, SWT.LEFT);
		space.setText(" "); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalSpan= 1;
		space.setLayoutData(gd);		
	}
	
	/**
	 * Gets the type name.
	 */
	public String getTypeName() {
		return (fSuiteNameText==null)?fSuiteNameTextInitialValue:fSuiteNameText.getText();
	}
	
	/**
	 * Sets the type name.
	 * @param canBeModified Selects if the type name can be changed by the user
	 */	
	public void setTypeName(String name, boolean canBeModified) {
		if (fSuiteNameText == null) {
			fSuiteNameTextInitialValue= name;
		} else {
			fSuiteNameText.setText(name);
			fSuiteNameText.setEnabled(canBeModified);
		}
	}	

	/**
	 * Called when the type name has changed.
	 * The method validates the type name and returns the status of the validation.
	 * Can be extended to add more validation
	 */
	protected IStatus testSuiteChanged() {
		JUnitStatus status= new JUnitStatus();
		String typeName= getTypeName();
		// must not be empty
		if (typeName.length() == 0) {
			status.setError(WizardMessages.getString("NewTestSuiteWizPage.typeName.error.name_empty")); //$NON-NLS-1$
			return status;
		}
		if (typeName.indexOf('.') != -1) {
			status.setError(WizardMessages.getString("NewTestSuiteWizPage.typeName.error.name_qualified")); //$NON-NLS-1$
			return status;
		}
		IStatus val= JavaConventions.validateJavaTypeName(typeName);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(WizardMessages.getString("NewTestSuiteWizPage.typeName.error.name_not_valid")+val.getMessage()); //$NON-NLS-1$
			return status;
		} else if (val.getSeverity() == IStatus.WARNING) {
			status.setWarning(WizardMessages.getString("NewTestSuiteWizPage.typeName.error.name.name_discouraged")+val.getMessage()); //$NON-NLS-1$
			// continue checking
		}		

		JUnitStatus recursiveSuiteInclusionStatus= checkRecursiveTestSuiteInclusion();
		if (! recursiveSuiteInclusionStatus.isOK())
			return recursiveSuiteInclusionStatus;
			
		IPackageFragment pack= getPackageFragment();
		if (pack != null) {
			ICompilationUnit cu= pack.getCompilationUnit(typeName + ".java"); //$NON-NLS-1$
			if (cu.exists()) {
				status.setWarning(WizardMessages.getString("NewTestSuiteWizPage.typeName.warning.already_exists")); //$NON-NLS-1$
				fMethodStubsButtons.setEnabled(false);
				return status;
			}
		}
		fMethodStubsButtons.setEnabled(true);
		return status;
	}

	private JUnitStatus checkRecursiveTestSuiteInclusion(){
		if (fClassesInSuiteTable == null)
			return new JUnitStatus();
		String typeName= getTypeName();
		JUnitStatus status= new JUnitStatus();
		Object[] checkedClasses= fClassesInSuiteTable.getCheckedElements();
		for (int i= 0; i < checkedClasses.length; i++) {
			IType checkedClass= (IType)checkedClasses[i];
			if (checkedClass.getElementName().equals(typeName)){
				status.setWarning(WizardMessages.getString("NewTestSuiteCreationWizardPage.infinite_recursion")); //$NON-NLS-1$
				return status;
			}
		}
		return new JUnitStatus();
	}

	/**
	 * Sets the focus.
	 */		
	protected void setFocus() {
		fSuiteNameText.setFocus();
	}

	/**
	 * Sets the classes in <code>elements</code> as checked.
	 */	
	public void setCheckedElements(Object[] elements) {
		fClassesInSuiteTable.setCheckedElements(elements);
	}
	
	protected void cannotUpdateSuiteError() {
		MessageDialog.openError(getShell(), WizardMessages.getString("NewTestSuiteWizPage.cannotUpdateDialog.title"), //$NON-NLS-1$
			WizardMessages.getFormattedString("NewTestSuiteWizPage.cannotUpdateDialog.message", new String[] {START_MARKER, END_MARKER})); //$NON-NLS-1$

	}

	private void writeImports(ImportsManager imports) {
		imports.addImport("junit.framework.Test"); //$NON-NLS-1$
		imports.addImport("junit.framework.TestSuite");		 //$NON-NLS-1$
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
			//The next 2 lines are necessary. Otherwise, if fMethodsStubsButtons is disabled, and USE_TESTRUNNER gets enabled,
			//then the checkbox for USE_TESTRUNNER will be the only enabled component of fMethodsStubsButton
			fMethodStubsButtons.setEnabled(!fMethodStubsButtons.isEnabled());
			fMethodStubsButtons.setEnabled(!fMethodStubsButtons.isEnabled());
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
}
