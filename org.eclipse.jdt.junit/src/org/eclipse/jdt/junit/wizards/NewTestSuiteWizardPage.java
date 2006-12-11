/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.wizards;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.IJUnitHelpContextIds;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jdt.internal.junit.wizards.SuiteClassesContentProvider;
import org.eclipse.jdt.internal.junit.wizards.TestSuiteClassListRange;
import org.eclipse.jdt.internal.junit.wizards.UpdateTestSuite;
import org.eclipse.jdt.internal.junit.wizards.WizardMessages;

/**
 * The class <code>NewTestSuiteWizardPage</code> contains controls and validation routines 
 * for the single page in the 'New JUnit TestSuite Wizard'.
 * 
 * Clients can use the page as-is and add it to their own wizard, or extend it to modify
 * validation or add and remove controls.
 * 
 * @since 3.1
 */
public class NewTestSuiteWizardPage extends NewTypeWizardPage {
	
	private static final String ALL_TESTS= "AllTests"; //$NON-NLS-1$

	public static final String NON_COMMENT_END_MARKER = "$JUnit-END$"; //$NON-NLS-1$

	public static final String NON_COMMENT_START_MARKER = "$JUnit-BEGIN$"; //$NON-NLS-1$

	public static final String COMMENT_START = "//"; //$NON-NLS-1$

	/**
	 * The string used to mark the beginning of the generated code
	 */
	public static final String START_MARKER= COMMENT_START + NON_COMMENT_START_MARKER; 
	
	/**
	 * The string used to mark the end of the generated code
	 */
	public static final String END_MARKER= COMMENT_START + NON_COMMENT_END_MARKER; 

	private final static String PAGE_NAME= "NewTestSuiteCreationWizardPage"; //$NON-NLS-1$
	
	/** Field ID of the class in suite field. */
	public final static String CLASSES_IN_SUITE= PAGE_NAME + ".classesinsuite"; //$NON-NLS-1$

	private CheckboxTableViewer fClassesInSuiteTable;
	private IStatus fClassesInSuiteStatus;

	private Label fSelectedClassesLabel;
	
	private boolean fUpdatedExistingClassButton;

	/**
	 * Creates a new <code>NewTestSuiteWizardPage</code>.
	 */
	public NewTestSuiteWizardPage() {
		super(true, PAGE_NAME);
		setTitle(WizardMessages.NewTestSuiteWizPage_title); 
		setDescription(WizardMessages.NewTestSuiteWizPage_description); 
		
		fClassesInSuiteStatus= new JUnitStatus();
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
		//createSeparator(composite, nColumns);
		createTypeNameControls(composite, nColumns);
		createClassesInSuiteControl(composite, nColumns);
		createMethodStubSelectionControls(composite, nColumns);
		setControl(composite);
		restoreWidgetValues();
		Dialog.applyDialogFont(composite);	
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJUnitHelpContextIds.NEW_TESTSUITE_WIZARD_PAGE);			
	}

	/**
	 * Creates the controls for the method stub selection buttons. Expects a <code>GridLayout</code> with 
	 * at least 3 columns.
	 * 
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */	
	protected void createMethodStubSelectionControls(Composite composite, int nColumns) {
	}	

	/**
	 * Should be called from the wizard with the initial selection.
	 * 
	 * @param selection the initial selection
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);
		initContainerPage(jelem);
		initSuitePage(jelem);
		doStatusUpdate();
	}

	private void initSuitePage(IJavaElement jelem) {
		//parts of NewTypeWizardPage#initTypePage(..), see bug 132748
		
		if (jelem != null) {
			IPackageFragment pack= (IPackageFragment) jelem.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			IJavaProject project= jelem.getJavaProject();
			
			setPackageFragment(pack, true);
			setAddComments(StubUtility.doAddComments(project), true); // from project or workspace
		}
		
		setTypeName(ALL_TESTS, true);
		fTypeNameStatus= typeNameChanged(); //set status on initialization for this dialog - user must know that suite method will be overridden
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewContainerWizardPage#handleFieldChanged(java.lang.String)
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName.equals(PACKAGE) || fieldName.equals(CONTAINER)) {
			updateClassesInSuiteTable();
		} else if (fieldName.equals(CLASSES_IN_SUITE)) {
			fClassesInSuiteStatus= classesInSuiteChanged();
			fTypeNameStatus= typeNameChanged(); //must check this one too
			updateSelectedClassesLabel();
		}

		doStatusUpdate();
	}

	// ------ validation --------
	private void doStatusUpdate() {
		// status of all used components
		IStatus[] status= new IStatus[] {
			fContainerStatus,
			fPackageStatus,
			fTypeNameStatus,
			fClassesInSuiteStatus			
		};
		
		// the most severe status will be displayed and the OK button enabled/disabled.
		updateStatus(status);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();		
			updateClassesInSuiteTable();
			handleAllFieldsChanged();
		} else {
			saveWidgetValues();
		}
	}

	private void handleAllFieldsChanged() {
		handleFieldChanged(PACKAGE);
		handleFieldChanged(CONTAINER);
		handleFieldChanged(CLASSES_IN_SUITE);
		handleFieldChanged(TYPENAME);
	}

	/**
	 * Updates the classes in the suite table.
	 */
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
	
	/**
	 * Creates the controls for the list of classes in the suite. Expects a <code>GridLayout</code> with 
	 * at least 3 columns.
	 * 
	 * @param parent the parent composite
	 * @param nColumns number of columns to span
	 */	
	protected void createClassesInSuiteControl(Composite parent, int nColumns) {
		if (fClassesInSuiteTable == null) {

			Label label = new Label(parent, SWT.LEFT);
			label.setText(WizardMessages.NewTestSuiteWizPage_classes_in_suite_label); 
			GridData gd= new GridData();
			gd.horizontalAlignment = GridData.FILL;
			gd.horizontalSpan= nColumns;
			label.setLayoutData(gd);

			fClassesInSuiteTable= CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
			gd= new GridData(GridData.FILL_BOTH);
			gd.heightHint= 80;
			gd.horizontalSpan= nColumns-1;

			fClassesInSuiteTable.getTable().setLayoutData(gd);
			fClassesInSuiteTable.setContentProvider(new SuiteClassesContentProvider());
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
	
			Button selectAllButton= new Button(buttonContainer, SWT.PUSH);
			selectAllButton.setText(WizardMessages.NewTestSuiteWizPage_selectAll); 
			GridData bgd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			bgd.widthHint = LayoutUtil.getButtonWidthHint(selectAllButton);
			selectAllButton.setLayoutData(bgd);
			selectAllButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fClassesInSuiteTable.setAllChecked(true);
					handleFieldChanged(CLASSES_IN_SUITE);
				}
			});
	
			Button deselectAllButton= new Button(buttonContainer, SWT.PUSH);
			deselectAllButton.setText(WizardMessages.NewTestSuiteWizPage_deselectAll); 
			bgd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			bgd.widthHint = LayoutUtil.getButtonWidthHint(deselectAllButton);
			deselectAllButton.setLayoutData(bgd);
			deselectAllButton.addSelectionListener(new SelectionAdapter() {
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
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#createTypeMembers(org.eclipse.jdt.core.IType, org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void createTypeMembers(IType type, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		writeImports(imports);
		type.createMethod(getSuiteMethodString(), null, false, null);	
	}

	/*
	 * Returns the string content for creating a new suite() method.
	 */
	private String getSuiteMethodString() {
		IPackageFragment pack= getPackageFragment();
		String packName= pack.getElementName();
		StringBuffer suite= new StringBuffer("public static Test suite () {TestSuite suite= new TestSuite(\"Test for "+((packName.equals(""))?"default package":packName)+"\");\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		suite.append(getUpdatableString());
		suite.append("\nreturn suite;}"); //$NON-NLS-1$
		return suite.toString();
	}
	
	private String getUpdatableString() {
		return UpdateTestSuite.getUpdatableString(fClassesInSuiteTable.getCheckedElements());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#createType(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void createType(IProgressMonitor monitor) throws CoreException, InterruptedException {
		IPackageFragment pack= getPackageFragment();
		ICompilationUnit cu= pack.getCompilationUnit(getTypeName() + ".java"); //$NON-NLS-1$
		
		if (!cu.exists()) {
			super.createType(monitor);
			fUpdatedExistingClassButton= false;
		} else {
			updateExistingType(cu, monitor);
			fUpdatedExistingClassButton= true;
		}
	}

	private void updateExistingType(ICompilationUnit cu, IProgressMonitor monitor) throws JavaModelException {
		if (! UpdateTestSuite.checkValidateEditStatus(cu, getShell()))
			return;
		IType suiteType= cu.getType(getTypeName());
		monitor.beginTask(WizardMessages.NewTestSuiteWizPage_createType_beginTask, 10); 
		IMethod suiteMethod= suiteType.getMethod("suite", new String[] {}); //$NON-NLS-1$
		monitor.worked(1);
		String lineDelimiter= cu.findRecommendedLineSeparator();
		if (suiteMethod.exists()) {
			ISourceRange range= suiteMethod.getSourceRange();
			if (range != null) {
				try {
					IDocument fullSource= new Document(cu.getBuffer().getContents());
					String originalContent= fullSource.get(range.getOffset(), range.getLength());
					StringBuffer source= new StringBuffer(originalContent);
					TestSuiteClassListRange classListRange = UpdateTestSuite.getTestSuiteClassListRange(originalContent);
					if (classListRange != null) {
						// TODO: copied
						monitor.subTask(WizardMessages.NewTestSuiteWizPage_createType_updating_suite_method); 
						monitor.worked(1);
						source.replace(classListRange.getStart(), classListRange.getEnd(), getUpdatableString());
						fullSource.replace(range.getOffset(), range.getLength(), source.toString());
						monitor.worked(1);
						String formattedContent= JUnitStubUtility.formatCompilationUnit(cu.getJavaProject(), fullSource.get(), lineDelimiter);
						cu.getBuffer().setContents(formattedContent);
						monitor.worked(1);
						cu.save(new SubProgressMonitor(monitor, 1), false);
					} else {
						cannotUpdateSuiteError();
					}
				} catch (BadLocationException e) {
					Assert.isTrue(false, "Should never happen"); //$NON-NLS-1$
				}
			} else {
				MessageDialog.openError(getShell(), WizardMessages.NewTestSuiteWizPage_createType_updateErrorDialog_title, WizardMessages.NewTestSuiteWizPage_createType_updateErrorDialog_message); 
			}
		} else {
			suiteType.createMethod(getSuiteMethodString(), null, true, monitor);
			String originalContent= cu.getSource();
			monitor.worked(2);
			String formattedContent= JUnitStubUtility.formatCompilationUnit(cu.getJavaProject(), originalContent, lineDelimiter);
			cu.getBuffer().setContents(formattedContent);
			monitor.worked(1);
			cu.save(new SubProgressMonitor(monitor, 1), false);
		}
		monitor.done();
	}

	/**
	 * Returns true iff an existing suite() method has been replaced.
	 * 
	 * @return <code>true</code> is returned if an existing test suite has been replaced
	 */
	public boolean hasUpdatedExistingClass() {
		return fUpdatedExistingClassButton;
	}
	
	private IStatus classesInSuiteChanged() {
		JUnitStatus status= new JUnitStatus();
		if (fClassesInSuiteTable.getCheckedElements().length <= 0)
			status.setWarning(WizardMessages.NewTestSuiteWizPage_classes_in_suite_error_no_testclasses_selected); 
		return status;
	}
	
	private void updateSelectedClassesLabel() {
		int noOfClassesChecked= fClassesInSuiteTable.getCheckedElements().length;
		String key= (noOfClassesChecked==1) ? WizardMessages.NewTestClassWizPage_treeCaption_classSelected : WizardMessages.NewTestClassWizPage_treeCaption_classesSelected;
		fSelectedClassesLabel.setText(Messages.format(key, new Integer(noOfClassesChecked)));
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#typeNameChanged()
	 */
	protected IStatus typeNameChanged() {
		super.typeNameChanged();
		
		JUnitStatus status= new JUnitStatus();
		String typeName= getTypeName();
		// must not be empty
		if (typeName.length() == 0) {
			status.setError(WizardMessages.NewTestSuiteWizPage_typeName_error_name_empty); 
			return status;
		}
		if (typeName.indexOf('.') != -1) {
			status.setError(WizardMessages.NewTestSuiteWizPage_typeName_error_name_qualified); 
			return status;
		}
		
		IJavaProject javaProject= getJavaProject();
		String sourceLevel= javaProject != null ? javaProject.getOption(JavaCore.COMPILER_SOURCE, true) : JavaCore.VERSION_1_3;
		String compliance= javaProject != null ? javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true) : JavaCore.VERSION_1_3;
		
		IStatus val= JavaConventions.validateJavaTypeName(typeName, sourceLevel, compliance);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(WizardMessages.NewTestSuiteWizPage_typeName_error_name_not_valid+val.getMessage()); 
			return status;
		} else if (val.getSeverity() == IStatus.WARNING) {
			status.setWarning(WizardMessages.NewTestSuiteWizPage_typeName_error_name_name_discouraged+val.getMessage()); 
			// continue checking
		}		

		IStatus recursiveSuiteInclusionStatus= checkRecursiveTestSuiteInclusion();
		if (! recursiveSuiteInclusionStatus.isOK())
			return recursiveSuiteInclusionStatus;
			
		IPackageFragment pack= getPackageFragment();
		if (pack != null) {
			ICompilationUnit cu= pack.getCompilationUnit(typeName + ".java"); //$NON-NLS-1$
			if (cu.exists()) {
				status.setWarning(WizardMessages.NewTestSuiteWizPage_typeName_warning_already_exists); 
				return status;
			}
		}
		return status;
	}

	private IStatus checkRecursiveTestSuiteInclusion(){
		if (fClassesInSuiteTable == null)
			return new JUnitStatus();
		String typeName= getTypeName();
		JUnitStatus status= new JUnitStatus();
		Object[] checkedClasses= fClassesInSuiteTable.getCheckedElements();
		for (int i= 0; i < checkedClasses.length; i++) {
			IType checkedClass= (IType)checkedClasses[i];
			if (checkedClass.getElementName().equals(typeName)){
				status.setWarning(WizardMessages.NewTestSuiteCreationWizardPage_infinite_recursion); 
				return status;
			}
		}
		return new JUnitStatus();
	}

	
	private void cannotUpdateSuiteError() {
		MessageDialog.openError(getShell(), WizardMessages.NewTestSuiteWizPage_cannotUpdateDialog_title, 
			Messages.format(WizardMessages.NewTestSuiteWizPage_cannotUpdateDialog_message, new String[] { NewTestSuiteWizardPage.START_MARKER, NewTestSuiteWizardPage.END_MARKER})); 

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
	}	

	/**
	 * 	Since Finish was pressed, write widget values to the dialog store so that they
	 *	will persist into the next invocation of this wizard page
	 */
	private void saveWidgetValues() {
	}	
}
