/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.refactoring.UserInputWizardPage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

class ExternalizeWizardPage2 extends UserInputWizardPage {

	private static final String SETTING_PROPFILE= 		"PropertyFile";//$NON-NLS-1$
	private static final String SETTING_PROPPACK= 		"PropertyPackage";//$NON-NLS-1$
	private static final String SETTING_ACCESSORCLASS= 	"AccessorClass";//$NON-NLS-1$
	private static final String SETTING_CODEPATTERN= 	"CodePattern";//$NON-NLS-1$
	private static final String SETTING_CREATEACCESSOR= "CreateAccessorClass";//$NON-NLS-1$
	private static final String SETTING_USEDEFAULTPATTERN=	"UseDefaultPattern";//$NON-NLS-1$
	private static final String SETTING_NEWIMPORT= 		  	"AddImport";//$NON-NLS-1$
	
	private IPackageFragment fPkgFragment;
	public static final String PAGE_NAME= "NLSWizardPage2"; //$NON-NLS-1$
	
	private StringButtonDialogField fPropertyPackage;
	private StringButtonDialogField fPropertyFile;
	private StringDialogField fAccessorClassName;
	private StringDialogField fCodePattern;
	private StringButtonDialogField fNewImport;
	private SelectionButtonDialogField fUseDefaultPattern;
	private SelectionButtonDialogField fCreateAccessorClass;
	private OrderedMap fErrorMap;

	public ExternalizeWizardPage2() {
		super(PAGE_NAME, true);
		fErrorMap= new OrderedMap();
		
		fPropertyPackage= 	createStringButtonField(NLSUIMessages.getString("wizardPage2.package"), NLSUIMessages.getString("wizardPage2.browse1"),  //$NON-NLS-2$ //$NON-NLS-1$
									createPropertyPackageBrowseAdapter());
		fPropertyFile= 		createStringButtonField(NLSUIMessages.getString("wizardPage2.property_file_name"), NLSUIMessages.getString("wizardPage2.browse2"),  //$NON-NLS-2$ //$NON-NLS-1$
									createPropertyFileBrowseAdapter());				
		fUseDefaultPattern= createCheckBoxField(NLSUIMessages.getString("wizardPage2.default_pattern")); //$NON-NLS-1$
		fAccessorClassName= createStringField(NLSUIMessages.getString("wizardPage2.class_name")); //$NON-NLS-1$
		fCodePattern= 		createStringField(NLSUIMessages.getString("wizardPage2.code_pattern")); //$NON-NLS-1$
		fCreateAccessorClass= createCheckBoxField(NLSUIMessages.getString("wizardPage2.create_accessor")); //$NON-NLS-1$
		fNewImport= 		createStringButtonField(NLSUIMessages.getString("wizardPage2.add_import"),  //$NON-NLS-1$
									NLSUIMessages.getString("wizardPage2.browse3"), //$NON-NLS-1$
									createClassBrowseAdapter());
	}
		
	private StringDialogField createStringField(String label) {
		StringDialogField field= new StringDialogField();
		field.setLabelText(label);
		return field;
	}
	
	private SelectionButtonDialogField createCheckBoxField(String label) {
		SelectionButtonDialogField field= new SelectionButtonDialogField(SWT.CHECK);
		field.setLabelText(label);
		return field;
	}
	
	private StringButtonDialogField createStringButtonField(String label, String button, IStringButtonAdapter adapter) {
		StringButtonDialogField field= new StringButtonDialogField(adapter);
		field.setLabelText(label);
		field.setButtonLabel(button);
		return field;	
	}
	
	private IStringButtonAdapter createClassBrowseAdapter(){
		return new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				browseForClassToImport();
			}
		};
	}

	private IStringButtonAdapter createPropertyPackageBrowseAdapter(){
		return new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				browseForPropertyPackage();
			}
		};
	}
	
	private IStringButtonAdapter createPropertyFileBrowseAdapter(){
		return new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				browseForPropertyFile();
			}
		};		
	}
	
	private void initializeDialogField(SelectionButtonDialogField field, String property, boolean def){
		String s= JavaPlugin.getDefault().getDialogSettings().get(property);
		if (s != null)
			field.setSelection(new Boolean(s).booleanValue());
		else
			field.setSelection(def);	
	}
	
	private void initializeDialogField(StringDialogField field, String property, String def){
		String s= JavaPlugin.getDefault().getDialogSettings().get(property);
		if (s != null)
			field.setText(s);
		else	
			field.setText(def);
	}
		
	private void initializeFields() {
		initializeDialogField(fPropertyFile, 		SETTING_PROPFILE, getNLSRefactoring().getDefaultPropertyFileName());	
		initializeDialogField(fPropertyPackage, 	SETTING_PROPPACK, getNLSRefactoring().getDefaultPropertyPackageName());
		initializeDialogField(fCodePattern, 		SETTING_CODEPATTERN, getNLSRefactoring().getCodePattern());
		initializeDialogField(fAccessorClassName, 	SETTING_ACCESSORCLASS, getNLSRefactoring().getAccessorClassName());
		initializeDialogField(fUseDefaultPattern, 	SETTING_USEDEFAULTPATTERN, true);
		initializeDialogField(fNewImport, 			SETTING_NEWIMPORT, ""); //$NON-NLS-1$
		initializeDialogField(fCreateAccessorClass, SETTING_CREATEACCESSOR, getNLSRefactoring().getCreateAccessorClass());
		updateEnabledStates();
	}
	
	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite ancestor) {
		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		parent.setLayout(layout);
		
		Separator label= new Separator(SWT.NONE);
		((Label)label.getSeparator(parent)).setText(NLSUIMessages.getString("wizardPage2.property_location")); //$NON-NLS-1$
		label.doFillIntoGrid(parent, 3, 20);
		
		fPropertyPackage.doFillIntoGrid(parent, 3);
		fPropertyPackage.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				validatePropertyPackage();
			}
		});
		LayoutUtil.setHorizontalGrabbing(fPropertyPackage.getTextControl(null));
		
		fPropertyFile.doFillIntoGrid(parent, 3);
		fPropertyFile.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				validatePropertyFilename();
			}	
		});
		
		Separator s= new Separator(SWT.SEPARATOR | SWT.HORIZONTAL);
		s.doFillIntoGrid(parent, 3);
		
		fCreateAccessorClass.setLabelText(NLSUIMessages.getString("wizardPage2.create_accessor")  //$NON-NLS-1$
										+ "\"" + getPackageName(getCu()) + "\""  //$NON-NLS-2$ //$NON-NLS-1$
										+ NLSUIMessages.getString("wizardPage2.if_needed")); //$NON-NLS-1$
		fCreateAccessorClass.doFillIntoGrid(parent, 3);		
		fCreateAccessorClass.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				toggleAccessorClassCreation();
				updateEnabledStates();
			}	
		});
		
		fAccessorClassName.doFillIntoGrid(parent, 3);
		fAccessorClassName.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				validateAccessorClassName();
			}	
		});		
		
		fUseDefaultPattern.doFillIntoGrid(parent, 3);
		fUseDefaultPattern.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				toggleUseDefaultPattern();
				updateEnabledStates();
			}	
		});
		
		fCodePattern.doFillIntoGrid(parent, 3);
		
		fNewImport.doFillIntoGrid(parent, 3);
		fNewImport.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				validateNewImport();
			}	
		});		

		setControl(parent);
		initializeFields();
		WorkbenchHelp.setHelp(parent, IJavaHelpContextIds.EXTERNALIZE_WIZARD_PROPERTIES_FILE_PAGE);
	}
	
	private static String getPackageName(ICompilationUnit cu){
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		if (pack.isDefaultPackage())
			return NLSUIMessages.getString("wizardPage2.default_package"); //$NON-NLS-1$
		return pack.getElementName();
	}
	
	private void validateNewImport(){
		String importName= fNewImport.getText();
		
		if (importName == null || "".equals(importName.trim())){ //$NON-NLS-1$
			setValid(fNewImport);
			return;
		}	
		
		IStatus status= JavaConventions.validateImportDeclaration(importName);
		if (status.getSeverity() == IStatus.ERROR){
			setInvalid(fNewImport, status.getMessage());
			return;
		}
		setValid(fNewImport);
	}
	
	private void validateAccessorClassName(){
		String className= fAccessorClassName.getText();
		
		getNLSRefactoring().setAccessorClassName(className);
		if (fUseDefaultPattern.isSelected()){
			getNLSRefactoring().setCodePattern(getNLSRefactoring().getDefaultCodePattern());
			fCodePattern.setText(getNLSRefactoring().getCodePattern());
		} else {
			getNLSRefactoring().setCodePattern(fCodePattern.getText());
		}
		
		IStatus status= JavaConventions.validateJavaTypeName(className);
		if (status.getSeverity() == IStatus.ERROR) {
			setInvalid(fAccessorClassName, status.getMessage());
			return;
		}
		
		if (className.indexOf(".") != -1){ //$NON-NLS-1$
			setInvalid(fAccessorClassName, NLSUIMessages.getString("wizardPage2.no_dot")); //$NON-NLS-1$
			return;
		}
		
		setValid(fAccessorClassName);
	}
	
	private void updateEnabledStates(){
		fAccessorClassName.setEnabled(fCreateAccessorClass.isSelected());
		fCodePattern.setEnabled(! fUseDefaultPattern.isSelected());
		fNewImport.setEnabled(!fUseDefaultPattern.isSelected());
		fCreateAccessorClass.setEnabled(fUseDefaultPattern.isSelected());
	}
	
	private void toggleAccessorClassCreation(){
		getNLSRefactoring().setCreateAccessorClass(fCreateAccessorClass.isSelected());
	}
	
	private void toggleUseDefaultPattern(){
		fCreateAccessorClass.setSelection(fUseDefaultPattern.isSelected());
		if (fUseDefaultPattern.isSelected()){
			getNLSRefactoring().setCodePattern(getNLSRefactoring().getDefaultCodePattern());
			fCodePattern.setText(getNLSRefactoring().getCodePattern());
			
			//XXX does not restore after 2 toggles
			fNewImport.setText(""); //$NON-NLS-1$
		}
	}
	
	private void browseForClassToImport(){
		IPackageFragmentRoot root= getPackageFragmentRoot(getCu());
		if (root == null)
			return;

		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[]{root.getJavaProject()});
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), IJavaSearchConstants.CLASS, scope);
		dialog.setTitle(NLSUIMessages.getString("wizardPage2.Class_Selection")); //$NON-NLS-1$
		dialog.setMessage(NLSUIMessages.getString("wizardPage2.Choose_the_type_to_import")); //$NON-NLS-1$
		dialog.setUpperListLabel(NLSUIMessages.getString("wizardPage2.Matching_classes")); //$NON-NLS-1$
		String guessTypeName= fNewImport.getText().substring(fNewImport.getText().lastIndexOf(".") + 1); //$NON-NLS-1$
		dialog.setFilter(guessTypeName);
		if (dialog.open() == Window.OK) {
			IType type= (IType) dialog.getFirstResult();
			if (type != null)
				fNewImport.setText(JavaModelUtil.getFullyQualifiedName(type));	
		}
	}

	private void browseForPropertyFile(){
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), getLabelProvider());
		dialog.setIgnoreCase(false);
		dialog.setTitle(NLSUIMessages.getString("wizardPage2.Property_File_Selection"));  //$NON-NLS-1$
		dialog.setMessage(NLSUIMessages.getString("wizardPage2.Choose_the_property_file")); //$NON-NLS-1$
		dialog.setElements(createFileListInput());
		dialog.setFilter("*" + NLSRefactoring.PROPERTY_FILE_EXT); //$NON-NLS-1$
		if (dialog.open() == ElementListSelectionDialog.OK) { 
			IFile selectedFile= (IFile)dialog.getFirstResult();
			if (selectedFile != null)
				fPropertyFile.setText(selectedFile.getName());						
		}			
	}
	
	private void browseForPropertyPackage(){
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), getLabelProvider());
		dialog.setIgnoreCase(false);
		dialog.setTitle(NLSUIMessages.getString("wizardPage2.package_selection")); //$NON-NLS-1$
		dialog.setMessage(NLSUIMessages.getString("wizardPage2.choose_package")); //$NON-NLS-1$
		dialog.setElements(createPackageListInput());
		dialog.setFilter(""); //$NON-NLS-1$
		if (dialog.open() == ElementListSelectionDialog.OK) { 
			IPackageFragment selectedPackage= (IPackageFragment)dialog.getFirstResult();
			if (selectedPackage != null)
				fPropertyPackage.setText(selectedPackage.getElementName());						
		}		
	}
	
	private static IPackageFragmentRoot getPackageFragmentRoot(ICompilationUnit cu){
		return (IPackageFragmentRoot)cu.getParent().getParent();
	}
	
	private ICompilationUnit getCu(){
		return getNLSRefactoring().getCu();
	}
	
	private Object[] createFileListInput(){
		try{
			if (fPkgFragment == null)
				return new Object[0];
			List result= new ArrayList(1);
			Object[] nonjava= fPkgFragment.getNonJavaResources();
			for (int i= 0; i < nonjava.length; i++){
				if (isPropertyFile(nonjava[i]))
					result.add(nonjava[i]);
			}
			return result.toArray();
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, NLSUIMessages.getString("wizardPage2.externalizing"), NLSUIMessages.getString("wizardPage2.exception")); //$NON-NLS-2$ //$NON-NLS-1$
			return new Object[0];
		}
	}	
	
	private static boolean isPropertyFile(Object o){
		if (! (o instanceof IFile))
			return false;
		IFile file= (IFile)o;
		return (NLSRefactoring.PROPERTY_FILE_EXT.equals("." + file.getFileExtension()));  //$NON-NLS-1$
	}
	
	private Object[] createPackageListInput(){
		try{
			IJavaProject project= getCu().getJavaProject();
			IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
			List result= new ArrayList();
			for (int i= 0; i < roots.length; i++){
				if (canAddPackageRoot(roots[i])){
					result.addAll(getValidPackages(roots[i]));
				}	
			}
			return result.toArray();
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, NLSUIMessages.getString("wizardPage2.externalizing"), NLSUIMessages.getString("wizardPage2.exception")); //$NON-NLS-2$ //$NON-NLS-1$
			return new Object[0];
		}
	}
	
	private List getValidPackages(IPackageFragmentRoot root) throws JavaModelException {
		IJavaElement[] children= null;
		try {
			children= root.getChildren();
		} catch (JavaModelException e){
			return new ArrayList(0);
		}	
		List result= new ArrayList(children.length);
		for (int i= 0; i < children.length; i++){
			if (children[i] instanceof IPackageFragment)
				if (canAddPackage((IPackageFragment)children[i]))
					result.add(children[i]);
		}
		return result;
	}
	
	private static boolean canAddPackageRoot(IPackageFragmentRoot root) throws JavaModelException{
		if (! root.exists())
			return false;
		if (root.isArchive())	
			return false;
		if (root.isExternal())
			return false;
		if (root.isReadOnly())		
			return false;
		if (! root.isStructureKnown())	
			return false;
		return true;	
	}
	
	private static boolean canAddPackage(IPackageFragment p) throws JavaModelException{ 
		if (! p.exists())
			return false;
		if (p.isReadOnly())
			return false;
		if (! p.isStructureKnown())
			return false;
		if (! p.containsJavaResources() && p.getNonJavaResources().length == 0)
			return false;
		return true;	
 	}
	
	private static ILabelProvider getLabelProvider(){
		return new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
	}
	
	private void validatePropertyPackage() {
		String pkgName= fPropertyPackage.getText();
		
		IStatus status= JavaConventions.validatePackageName(pkgName);
		if (!"".equals(pkgName) && status.getSeverity() == IStatus.ERROR) { //$NON-NLS-1$
			setInvalid(fPropertyPackage, status.getMessage());
			return;
		} 
		
		IPath pkgPath= new Path(pkgName.replace('.', IPath.SEPARATOR)).makeRelative();
		IJavaProject project= getCu().getJavaProject();
		try {
			IJavaElement element= project.findElement(pkgPath);
			if (element == null || !element.exists()) {
				setInvalid(fPropertyPackage, NLSUIMessages.getString("wizardPage2.must_exist")); //$NON-NLS-1$
				return;
			}
			fPkgFragment= (IPackageFragment)element;
			if (! canAddPackage(fPkgFragment)){
				setInvalid(fPropertyPackage, NLSUIMessages.getString("wizardPage2.incorrect_package")); //$NON-NLS-1$
				return;
			}
			if (! canAddPackageRoot((IPackageFragmentRoot)fPkgFragment.getParent())){
				setInvalid(fPropertyPackage, NLSUIMessages.getString("wizardPage2.incorrect_package")); //$NON-NLS-1$
				return;
			}
		} catch (JavaModelException e) {
			setInvalid(fPropertyPackage, e.getStatus().getMessage());
			return;
		}
		
		setValid(fPropertyPackage);
	}
	
	private void validatePropertyFilename() {
		String fileName= fPropertyFile.getText();
		if (fileName == null || "".equals(fileName)) { //$NON-NLS-1$
			setInvalid(fPropertyFile, NLSUIMessages.getString("wizardPage2.enter_name")); //$NON-NLS-1$
			return;
		}
		
		if (! fileName.endsWith(NLSRefactoring.PROPERTY_FILE_EXT)){
			setInvalid(fPropertyFile, NLSUIMessages.getString("wizardPage2.file_name_must_end") + NLSRefactoring.PROPERTY_FILE_EXT + "\"."); //$NON-NLS-2$ //$NON-NLS-1$
			return;
		}
		setValid(fPropertyFile);
	}
	
	private void setInvalid(Object field, String msg) {
		fErrorMap.push(field, msg);
		updateErrorMessage();
	}
	
	private void setValid(Object field) {
		fErrorMap.remove(field);
		updateErrorMessage();
	}
	
	private void updateErrorMessage() {
		String msg= (String)fErrorMap.peek();
		setPageComplete(msg == null);
		setErrorMessage(msg);
	}
	
	private NLSRefactoring getNLSRefactoring() {
		return (NLSRefactoring)getRefactoring();
	}
	
	public boolean performFinish(){
		updateRefactoring();
		return super.performFinish();
	}
	
	public IWizardPage getNextPage() {
		updateRefactoring();
		return super.getNextPage(); 
	}
	
	public void dispose(){
		//widgets will be disposed. only need to null'em
		fCodePattern= null;
		fErrorMap= null;
		fPkgFragment= null;
		fPropertyFile= null;
		fPropertyPackage= null;
		fUseDefaultPattern= null;
		fNewImport= null;
		fAccessorClassName= null;
		fCreateAccessorClass= null;
		super.dispose();
	}
	
	private void storeDialogSettings(){
		JavaPlugin.getDefault().getDialogSettings().put(SETTING_PROPFILE, fPropertyFile.getText());
		JavaPlugin.getDefault().getDialogSettings().put(SETTING_PROPPACK, fPropertyPackage.getText());
		JavaPlugin.getDefault().getDialogSettings().put(SETTING_ACCESSORCLASS, fAccessorClassName.getText());
		JavaPlugin.getDefault().getDialogSettings().put(SETTING_CODEPATTERN, fCodePattern.getText());
		JavaPlugin.getDefault().getDialogSettings().put(SETTING_CREATEACCESSOR, fCreateAccessorClass.isSelected());
		JavaPlugin.getDefault().getDialogSettings().put(SETTING_USEDEFAULTPATTERN, fUseDefaultPattern.isSelected());
		JavaPlugin.getDefault().getDialogSettings().put(SETTING_NEWIMPORT, fNewImport.getText());
	}
	
	void updateRefactoring() {
		storeDialogSettings();
		try {
			getNLSRefactoring().setPropertyFilePath(fPkgFragment.getUnderlyingResource().getFullPath().append(fPropertyFile.getText()));
			getNLSRefactoring().setCodePattern(fCodePattern.getText());
			getNLSRefactoring().setAddedImportDeclaration(fNewImport.getText());
			getNLSRefactoring().setCreateAccessorClass(fCreateAccessorClass.isSelected());
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, NLSUIMessages.getString("wizardPage2.externalizing"), NLSUIMessages.getString("wizardPage2.exception_change")); //$NON-NLS-2$ //$NON-NLS-1$
		}
	}
}

