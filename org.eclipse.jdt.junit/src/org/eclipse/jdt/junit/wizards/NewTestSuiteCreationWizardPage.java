/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
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
import org.eclipse.jdt.internal.corext.codemanipulation.IImportsStructure;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
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

/**
 * Wizard page to select the test classes to include
 * in the test suite.
 */
public class NewTestSuiteCreationWizardPage extends NewTypeWizardPage {

	private final static String PAGE_NAME= "NewTestSuiteCreationWizardPage"; //$NON-NLS-1$
	private final static String CLASSES_IN_SUITE= PAGE_NAME + ".classesinsuite"; //$NON-NLS-1$
	private final static String SUITE_NAME= PAGE_NAME + ".suitename"; //$NON-NLS-1$
	private final static String SELECTED_CLASSES_LABEL_TEXT_ONE= " class selected."; //$NON-NLS-1$
	private final static String SELECTED_CLASSES_LABEL_TEXT_MANY= " classes selected.";	 //$NON-NLS-1$
	
	private IPackageFragment fCurrentPackage;
	private CheckboxTableViewer fClassesInSuiteTable;	
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Label fSelectedClassesLabel;

	private Label fSuiteNameLabel;
	private Text fSuiteNameText;
	private String fSuiteNameTextInitialValue;
	
	private boolean fUpdatedExistingClassButton;

	public static final String startMarker= "//$JUnit-BEGIN$"; //$NON-NLS-1$
	public static final String endMarker= "//$JUnit-END$"; //$NON-NLS-1$

	protected IStatus fClassesInSuiteStatus;
	protected IStatus fSuiteNameStatus;
	
	public NewTestSuiteCreationWizardPage() {
		super(true, PAGE_NAME);

		fSuiteNameStatus= new JUnitStatus();
		fSuiteNameTextInitialValue= ""; //$NON-NLS-1$
		setTitle(Messages.getString("NewTestSuiteWizPage.title")); //$NON-NLS-1$
		setDescription(Messages.getString("NewTestSuiteWizPage.description")); //$NON-NLS-1$
		fClassesInSuiteStatus= new JUnitStatus();
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
		createSuiteNameControl(composite, nColumns);
		setTypeName("AllTests",true); //$NON-NLS-1$
		createSeparator(composite, nColumns);
		createClassesInSuiteControl(composite, nColumns);
		setControl(composite);	
	}

	/**
	 * Should be called from the wizard with the input element.
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);
		initContainerPage(jelem);
		initTypePage(jelem);
		doStatusUpdate();
	}
	
	/**
	 * @see NewContainerWizardPage#handleFieldChanged
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName.equals(PACKAGE) || fieldName.equals(CONTAINER)) {
			if (fieldName.equals(PACKAGE))
				fPackageStatus= packageChanged();
			updateClassesInSuiteTable();
		} else if (fieldName.equals(CLASSES_IN_SUITE)) {
			fClassesInSuiteStatus= classesInSuiteChanged();
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
		
		// the mode severe status will be displayed and the ok button enabled/disabled.
		updateStatus(status);
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();		
			updateClassesInSuiteTable();
		}
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
			fCurrentPackage= pack;			
			fClassesInSuiteTable.setInput(pack);
			fClassesInSuiteTable.setAllChecked(true);
			updateSelectedClassesLabel();	
		}
	}
	
	protected void createClassesInSuiteControl(Composite parent, int nColumns) {
		if (fClassesInSuiteTable == null) {

			Label label = new Label(parent, SWT.LEFT);
			label.setText(Messages.getString("NewTestSuiteWizPage.classes_in_suite.label")); //$NON-NLS-1$
			GridData gd= new GridData();
			gd.horizontalAlignment = GridData.FILL;
			gd.horizontalSpan = nColumns;
			label.setLayoutData(gd);

			fClassesInSuiteTable= CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
			gd= new GridData(GridData.FILL_BOTH);
			gd.heightHint= 200;
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
			fSelectAllButton.setText(Messages.getString("NewTestSuiteWizPage.selectAll")); //$NON-NLS-1$
			GridData bgd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			fSelectAllButton.setLayoutData(bgd);
			fSelectAllButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fClassesInSuiteTable.setAllChecked(true);
					handleFieldChanged(CLASSES_IN_SUITE);
				}
			});
	
			fDeselectAllButton= new Button(buttonContainer, SWT.PUSH);
			fDeselectAllButton.setText(Messages.getString("NewTestSuiteWizPage.deselectAll")); //$NON-NLS-1$
			bgd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
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

	public static class ClassesInSuitContentProvider implements IStructuredContentProvider {
			
		private Object[] fTypes;
			
		public ClassesInSuitContentProvider() {
			super();
		}
		
		public Object[] getElements(Object parent) {
			try {
				if (parent instanceof IPackageFragment) {
					IPackageFragment pack= (IPackageFragment) parent;
					ICompilationUnit[] cuArray= pack.getCompilationUnits();
					ArrayList typesArrayList= new ArrayList();
					for (int i= 0; i < cuArray.length; i++) {
						ICompilationUnit cu= cuArray[i];
						IType[] types= cu.getTypes();
						for (int j= 0; j < types.length; j++) {
							if (TestSearchEngine.isTestImplementor(types[j]))	
								typesArrayList.add(types[j]);
						}
					}
					return typesArrayList.toArray();
				}
			} catch (JavaModelException e) {
				JUnitPlugin.log(e);
			}
			return new Object[0];
		}
		public void dispose() {
		}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	/*
	 * @see TypePage#evalMethods
	 */
	protected void createTypeMembers(IType type, IImportsStructure imports, IProgressMonitor monitor) throws CoreException {
		writeImports(imports);
		type.createMethod(getSuiteMethodString(), null, false, null);	
	}

	public String getSuiteMethodString() throws JavaModelException {
		IPackageFragment pack= getPackageFragment();
		String packName= pack.getElementName();
		StringBuffer suite= new StringBuffer("public static Test suite () {TestSuite suite= new TestSuite(\"Test for "+((packName.equals(""))?"default package":packName)+"\");\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		suite.append(getUpdatableString());
		suite.append("\nreturn suite;}"); //$NON-NLS-1$
		return suite.toString();
	}
	
	public String getUpdatableString() throws JavaModelException {
		StringBuffer suite= new StringBuffer();
		suite.append(startMarker+"\n"); //$NON-NLS-1$
		Object[] checkedObjects= fClassesInSuiteTable.getCheckedElements();
		for (int i= 0; i < checkedObjects.length; i++) {
			if (checkedObjects[i] instanceof IType) {
				IType testType= (IType) checkedObjects[i];
				IMethod suiteMethod= testType.getMethod("suite", new String[] {}); //$NON-NLS-1$
				if (!suiteMethod.exists()) {
					suite.append("suite.addTest(new TestSuite("+testType.getElementName()+".class));"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					suite.append("suite.addTest("+testType.getElementName()+".suite());"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		suite.append("\n"+endMarker); //$NON-NLS-1$
		return suite.toString();
	}

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
		try {
			IPackageFragment pack= getPackageFragment();
			ICompilationUnit cu= pack.getCompilationUnit(getTypeName() + ".java"); //$NON-NLS-1$
	
			if (!cu.exists()) {
				createType(monitor);
				fUpdatedExistingClassButton= false;
				return;
			}
			
			IType suiteType= cu.getType(getTypeName());
			monitor.beginTask(Messages.getString("NewTestSuiteWizPage.createType.beginTask"), 10); //$NON-NLS-1$
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
					//int start= source.toString().indexOf(startMarker) --> int start= source.indexOf(startMarker);
					int start= source.toString().indexOf(startMarker);
					if (start > -1) {
						//using JDK 1.4
						//int end= source.toString().indexOf(endMarker, start) --> int end= source.indexOf(endMarker, start)
						int end= source.toString().indexOf(endMarker, start);
						if (end > -1) {
							monitor.subTask(Messages.getString("NewTestSuiteWizPage.createType.updating.suite_method")); //$NON-NLS-1$
							monitor.worked(1);
							end += endMarker.length();
							source.replace(start, end, getUpdatableString());
							buf.replace(range.getOffset(), range.getLength(), source.toString());
							cu.reconcile(null);
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
					MessageDialog.openError(getShell(), Messages.getString("NewTestSuiteWizPage.createType.updateErrorDialog.title"), Messages.getString("NewTestSuiteWizPage.createType.updateErrorDialog.message")); //$NON-NLS-1$ //$NON-NLS-2$
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
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		}
	}

	public boolean hasUpdatedExistingClass() {
		return fUpdatedExistingClassButton;
	}
	
	private IStatus classesInSuiteChanged() {
		JUnitStatus status= new JUnitStatus();
		if (fClassesInSuiteTable.getCheckedElements().length <= 0)
			status.setWarning(Messages.getString("NewTestSuiteWizPage.classes_in_suite.error.no_testclasses_selected")); //$NON-NLS-1$
		return status;
	}
	
	private void updateSelectedClassesLabel() {
		int noOfClassesChecked= fClassesInSuiteTable.getCheckedElements().length;
			fSelectedClassesLabel.setText(noOfClassesChecked+((noOfClassesChecked==1)?SELECTED_CLASSES_LABEL_TEXT_ONE:SELECTED_CLASSES_LABEL_TEXT_MANY));
	}

	protected void createSuiteNameControl(Composite composite, int nColumns) {
		fSuiteNameLabel= new Label(composite, SWT.LEFT | SWT.WRAP);
		fSuiteNameLabel.setFont(composite.getFont());
		fSuiteNameLabel.setText(Messages.getString("NewTestSuiteWizPage.suiteName.text")); //$NON-NLS-1$
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
		gd.horizontalAlignment= gd.FILL;
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
			status.setError(Messages.getString("NewTestSuiteWizPage.typeName.error.name_empty")); //$NON-NLS-1$
			return status;
		}
		if (typeName.indexOf('.') != -1) {
			status.setError(Messages.getString("NewTestSuiteWizPage.typeName.error.name_qualified")); //$NON-NLS-1$
			return status;
		}
		IStatus val= JavaConventions.validateJavaTypeName(typeName);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(Messages.getString("NewTestSuiteWizPage.typeName.error.name_not_valid")+val.getMessage()); //$NON-NLS-1$
			return status;
		} else if (val.getSeverity() == IStatus.WARNING) {
			status.setWarning(Messages.getString("NewTestSuiteWizPage.typeName.error.name.name_discouraged")+val.getMessage()); //$NON-NLS-1$
			// continue checking
		}		

		IPackageFragment pack= getPackageFragment();
		if (pack != null) {
			ICompilationUnit cu= pack.getCompilationUnit(typeName + ".java"); //$NON-NLS-1$
			if (cu.exists()) {
				status.setWarning(Messages.getString("NewTestSuiteWizPage.typeName.warning.already_exists")); //$NON-NLS-1$
				return status;
			}
		}
		return status;
	}

	/**
	 * Sets the focus.
	 */		
	protected void setFocus() {
		fSuiteNameText.setFocus();
	}
	
	public void setCheckedElements(Object[] elements) {
		fClassesInSuiteTable.setCheckedElements(elements);
	}
	
	protected void cannotUpdateSuiteError() {
		MessageDialog.openError(getShell(), Messages.getString("NewTestSuiteWizPage.cannotUpdateDialog.title"), //$NON-NLS-1$
			Messages.getFormattedString("NewTestSuiteWizPage.cannotUpdateDialog.message", new String[] {startMarker, endMarker})); //$NON-NLS-1$

	}

	private void writeImports(IImportsStructure imports) {
		imports.addImport("junit.framework.Test"); //$NON-NLS-1$
		imports.addImport("junit.framework.TestSuite");		 //$NON-NLS-1$
	}
	
//	/**
//	 * Creates a type using the current field values.
//	 */
//	public void createType(IProgressMonitor monitor) throws CoreException, InterruptedException {		
//		monitor.beginTask(Messages.getString("NewTestSuiteWizPage.createType.beginTask"), 10); //$NON-NLS-1$
//		
//		IPackageFragmentRoot root= getPackageFragmentRoot();
//		IPackageFragment pack= getPackageFragment();
//		if (pack == null) {
//			pack= root.getPackageFragment(""); //$NON-NLS-1$
//		}
//		
//		if (!pack.exists()) {
//			String packName= pack.getElementName();
//			pack= root.createPackageFragment(packName, true, null);
//		}		
//		
//		monitor.worked(1);
//		
//		String clName= getTypeName();
//		
//		boolean isInnerClass= isEnclosingTypeSelected();
//		
//		IType createdType;
//		ImportsStructure imports;
//		int indent= 0;
//
//		String[] prefOrder= ImportOrganizePreferencePage.getImportOrderPreference();
//		int threshold= ImportOrganizePreferencePage.getImportNumberThreshold();			
//		
//		String lineDelimiter= null;	
//
//			ICompilationUnit parentCU= pack.getCompilationUnit(clName + ".java"); //$NON-NLS-1$
//
//			imports= new ImportsStructure(parentCU, prefOrder, threshold, false);
//			
//			lineDelimiter= JUnitStubUtility.getLineDelimiterUsed(parentCU);
//			
//			String content= createTypeBody(imports, lineDelimiter, parentCU);
//			createdType= parentCU.createType(content, null, false, new SubProgressMonitor(monitor, 5));
//		
//		// add imports for superclass/interfaces, so the type can be parsed correctly
////		writeImports(imports);	
//		imports.create(true, new SubProgressMonitor(monitor, 1));
//		
//		String[] methods= evalMethods(createdType, imports, new SubProgressMonitor(monitor, 1));
//		if (methods.length > 0) {
//			for (int i= 0; i < methods.length; i++) {
//				createdType.createMethod(methods[i], null, false, null);
//			}
//			// add imports
//			imports.create(!isInnerClass, null);
//		} 
//		monitor.worked(1);
//		
//		ICompilationUnit cu= createdType.getCompilationUnit();	
//		ISourceRange range;
//		if (isInnerClass) {
//			synchronized(cu) {
//				cu.reconcile();
//			}
//			range= createdType.getSourceRange();
//		} else {
//			range= cu.getSourceRange();
//		}
//		
//		IBuffer buf= cu.getBuffer();
//		String originalContent= buf.getText(range.getOffset(), range.getLength());
//		String formattedContent= JUnitStubUtility.codeFormat(originalContent, indent, lineDelimiter);
//		buf.replace(range.getOffset(), range.getLength(), formattedContent);
//		if (!isInnerClass) {
//			String fileComment= getFileComment(cu);
//			if (fileComment != null) {
//				buf.replace(0, 0, fileComment + lineDelimiter);
//			}
//			buf.save(new SubProgressMonitor(monitor, 1), false);
//		} else {
//			monitor.worked(1);
//		}
//		fCreatedType= createdType;
//		monitor.done();
//	}	
//
//	/*
//	 * Called from createType to construct the source for this type
//	 */		
//	private String createTypeBody(IImportsStructure imports, String lineDelimiter, ICompilationUnit parentCU) {	
//		StringBuffer buf= new StringBuffer();
//		String typeComment= getTypeComment(parentCU);
//		if (typeComment != null) {
//			buf.append(typeComment);
//			buf.append(lineDelimiter);
//		}
//		
//		int modifiers= getModifiers();
//		buf.append(Flags.toString(modifiers));
//		if (modifiers != 0) {
//			buf.append(' ');
//		}
//		buf.append("class "); //$NON-NLS-1$
//		buf.append(getTypeName());
//		buf.append(" {"); //$NON-NLS-1$
//		buf.append(lineDelimiter);
//		buf.append(lineDelimiter);
//		buf.append('}');
//		buf.append(lineDelimiter);
//		return buf.toString();
//	}
//
//	public IType getCreatedType() {
//		return fCreatedType;
//	}
}
