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
package org.eclipse.jdt.internal.ui.refactoring.nls;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.UserInputWizardPage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

/**
 * validateall and the specific validate methods check the userinput when perform finish
 * is called the refactoring is updated with the given values TODO ... NLSRefactoring
 * should be such a good citizenthat it can work without settings from the ui side (e.g.
 * dont add wizardpage2)
 * 
 * initial values of the dialogfields are guessed by the refactoring and provided via the
 * nlshint class. if no hints are found the dialogsettings are used. if no dialog settings
 * are found built in defaults are used.
 */
class ExternalizeWizardPage2 extends UserInputWizardPage {

	public static final String PAGE_NAME= "NLSWizardPage2"; //$NON-NLS-1$

	private OrderedMap fErrorMap= new OrderedMap();

	private SourceFirstPackageSelectionDialogField fResourceBundlePackage;

	private StringButtonDialogField fResourceBundleFile;

	private SourceFirstPackageSelectionDialogField fAccessorPackage;

	private StringDialogField fAccessorClassName;

	private StringDialogField fSubstitutionPattern;

	private boolean fCodePatternInSync;

	//  private Label fSyncVisualizer;

	public ExternalizeWizardPage2(NLSRefactoring refactoring) {
		super(PAGE_NAME, true);

		IDialogFieldListener updateListener= new IDialogFieldListener() {

			public void dialogFieldChanged(DialogField field) {
				validateAll();
			}
		};

		ICompilationUnit cu= refactoring.getCu();
		IJavaProject root= cu.getJavaProject();

		NLSHint hint= refactoring.getNlsHint();

		fAccessorPackage= new SourceFirstPackageSelectionDialogField(NLSUIMessages.getString("wizardPage2.accessor.path"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.accessor.package"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.browse1"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.browse2"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.default_package"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.accessor.dialog.title"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.accessor.dialog.message"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.accessor.dialog.emtpyMessage"), //$NON-NLS-1$
			cu, root, updateListener, hint.getMessageClassPackage());

		fAccessorClassName= createStringField(NLSUIMessages.getString("wizardPage2.className")); //$NON-NLS-1$
		fSubstitutionPattern= createStringField(NLSUIMessages.getString("wizardPage2.substitutionPattern")); //$NON-NLS-1$

		fResourceBundlePackage= new SourceFirstPackageSelectionDialogField(
			NLSUIMessages.getString("wizardPage2.property.path"), //$NON-NLS-1$ 
			NLSUIMessages.getString("wizardPage2.property.package"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.browse3"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.browse4"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.default_package"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.property.dialog.title"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.property.dialog.message"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.property.dialog.emptyMessage"), //$NON-NLS-1$
			cu, root, updateListener, hint.getResourceBundlePackage());

		fResourceBundleFile= createStringButtonField(NLSUIMessages.getString("wizardPage2.property_file_name"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.browse5"), createPropertyFileBrowseAdapter()); //$NON-NLS-1$

		initFields(refactoring, hint);
	}

	private void initFields(NLSRefactoring refactoring, NLSHint hints) {
		initAccessorClassFields(refactoring, hints);
		String resourceBundleHint= hints.getResourceBundle();
		fResourceBundleFile.setText(resourceBundleHint != null ? resourceBundleHint : NLSRefactoring
			.getDefaultPropertiesFilename());
	}

	private void initAccessorClassFields(NLSRefactoring refactoring, NLSHint hints) {
		String accessorClassName= hints.getMessageClass();

		if (accessorClassName == null) {
			accessorClassName= NLSRefactoring.DEFAULT_ACCESSOR_CLASSNAME;
		}
		fAccessorClassName.setText(accessorClassName);

		refactoring.setAccessorClassName(accessorClassName);

		fSubstitutionPattern.setText(refactoring.getDefaultSubstitutionPattern());
	}

	public void createControl(Composite ancestor) {
		final int nOfColumns= 4;

		initializeDialogUnits(ancestor);

		Composite parent= new Composite(ancestor, SWT.NONE);

		GridLayout layout= new GridLayout();
		layout.numColumns= nOfColumns;
		parent.setLayout(layout);

		createAccessorPart(parent, nOfColumns, convertWidthInCharsToPixels(40));

		Separator s= new Separator(SWT.SEPARATOR | SWT.HORIZONTAL);
		s.doFillIntoGrid(parent, nOfColumns);

		createPropertyPart(parent, nOfColumns, convertWidthInCharsToPixels(40));

		setControl(parent);

		Dialog.applyDialogFont(parent);
		WorkbenchHelp.setHelp(parent, IJavaHelpContextIds.EXTERNALIZE_WIZARD_PROPERTIES_FILE_PAGE);
		validateAll();
	}

	private void createAccessorPart(Composite parent, final int nOfColumns, int textWidth) {

		createLabel(parent, NLSUIMessages.getString("wizardPage2.resourceBundle.title"), nOfColumns); //$NON-NLS-1$
		fAccessorPackage.createControl(parent, nOfColumns, textWidth);

		fAccessorClassName.doFillIntoGrid(parent, nOfColumns);
		fAccessorClassName.setDialogFieldListener(new IDialogFieldListener() {

			public void dialogFieldChanged(DialogField field) {
				validateAccessorClassName();
			}
		});

		//    fSyncVisualizer = new Label(parent, SWT.NONE);
		//    GridData gd = new GridData(GridData.FILL_BOTH);
		//    gd.verticalSpan = 1;
		//    fSyncVisualizer.setLayoutData(gd);

		fSubstitutionPattern.doFillIntoGrid(parent, nOfColumns);
		fSubstitutionPattern.setDialogFieldListener(new IDialogFieldListener() {

			public void dialogFieldChanged(DialogField field) {
				updateSyncState();
			}
		});

	}

	private void createPropertyPart(Composite parent, final int nOfColumns, final int textWidth) {
		Separator label= new Separator(SWT.NONE);
		((Label)label.getSeparator(parent)).setText(NLSUIMessages.getString("wizardPage2.property_location")); //$NON-NLS-1$
		label.doFillIntoGrid(parent, nOfColumns, 20);
		fResourceBundlePackage.createControl(parent, nOfColumns, textWidth);

		fResourceBundleFile.doFillIntoGrid(parent, nOfColumns);
		fResourceBundleFile.setDialogFieldListener(new IDialogFieldListener() {

			public void dialogFieldChanged(DialogField field) {
				validatePropertyFilename();
			}
		});
	}

	private void updateSyncState() {
		String theDefault= getNLSRefactoring().getDefaultSubstitutionPattern();
		String current= fSubstitutionPattern.getText();
		fCodePatternInSync= current.equals(theDefault);
	}

	private void createLabel(Composite parent, final String text, final int N_OF_COLUMNS) {
		Separator label= new Separator(SWT.NONE);
		((Label)label.getSeparator(parent)).setText(text);
		label.doFillIntoGrid(parent, N_OF_COLUMNS, 20);
	}

	private void browseForPropertyFile() {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider());
		dialog.setIgnoreCase(false);
		dialog.setTitle(NLSUIMessages.getString("wizardPage2.Property_File_Selection")); //$NON-NLS-1$
		dialog.setMessage(NLSUIMessages.getString("wizardPage2.Choose_the_property_file")); //$NON-NLS-1$
		dialog.setElements(createFileListInput());
		dialog.setFilter('*' + NLSRefactoring.PROPERTY_FILE_EXT);
		if (dialog.open() == Window.OK) {
			IFile selectedFile= (IFile)dialog.getFirstResult();
			if (selectedFile != null)
				fResourceBundleFile.setText(selectedFile.getName());
		}
	}

	private Object[] createFileListInput() {
		try {

			IPackageFragment fPkgFragment= fResourceBundlePackage.getSelected();
			if (fPkgFragment == null)
				return new Object[0];
			List result= new ArrayList(1);
			Object[] nonjava= fPkgFragment.getNonJavaResources();
			for (int i= 0; i < nonjava.length; i++) {
				if (isPropertyFile(nonjava[i]))
					result.add(nonjava[i]);
			}
			return result.toArray();

		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, NLSUIMessages.getString("wizardPage2.externalizing"), NLSUIMessages //$NON-NLS-1$
				.getString("wizardPage2.exception")); //$NON-NLS-1$
			return new Object[0];
		}
	}

	private static boolean isPropertyFile(Object o) {
		if (!(o instanceof IFile))
			return false;
		IFile file= (IFile)o;
		return (NLSRefactoring.PROPERTY_FILE_EXT.equals('.' + file.getFileExtension()));
	}

	/**
	 * checks all entered values delegates to the specific validate methods these methods
	 * update the refactoring
	 */
	private void validateAll() {
		validateSubstitutionPattern();

		validateAccessorClassName();
		checkPackageFragment(fAccessorPackage, NLSUIMessages.getString("wizardPage2.accessor.package.root.invalid"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.accessor.package.invalid")); //$NON-NLS-1$

		validatePropertyFilename();
		validatePropertyPackage();
	}

	private void validateAccessorClassName() {
		if (fAccessorClassName != null) {
			String className= fAccessorClassName.getText();

			getNLSRefactoring().setAccessorClassName(className);

			if (fCodePatternInSync == true) {
				fSubstitutionPattern.setText(getNLSRefactoring().getSubstitutionPattern());
			} else {
				updateSyncState();
			}

			IStatus status= JavaConventions.validateJavaTypeName(className);
			if (status.getSeverity() == IStatus.ERROR) {
				setInvalid(fAccessorClassName, status.getMessage());
				return;
			}

			if (className.indexOf('.') != -1) {
				setInvalid(fAccessorClassName, NLSUIMessages.getString("wizardPage2.no_dot")); //$NON-NLS-1$
				return;
			}

			setValid(fAccessorClassName);
		}
	}

	private void validatePropertyFilename() {
		if (fResourceBundleFile != null) {
			String fileName= fResourceBundleFile.getText();
			if ((fileName == null) || (fileName.length() == 0)) {
				setInvalid(fResourceBundleFile, NLSUIMessages.getString("wizardPage2.enter_name")); //$NON-NLS-1$
				return;
			}

			if (!fileName.endsWith(NLSRefactoring.PROPERTY_FILE_EXT)) {
				setInvalid(fResourceBundleFile, NLSUIMessages.getString("wizardPage2.file_name_must_end") //$NON-NLS-1$
					+ NLSRefactoring.PROPERTY_FILE_EXT + "\"."); //$NON-NLS-1$
				return;
			}

			setValid(fResourceBundleFile);
		}
	}

	private void validatePropertyPackage() {
		if (!checkPackageFragment(fResourceBundlePackage, NLSUIMessages.getString("wizardPage2.property.package.root.invalid"), //$NON-NLS-1$
			NLSUIMessages.getString("wizardPage2.property.package.invalid"))) { //$NON-NLS-1$
			return;
		}

		IPackageFragment help= fResourceBundlePackage.getSelected();
		String pkgName= help.getElementName();

		IStatus status= JavaConventions.validatePackageName(pkgName);
		if ((pkgName.length() > 0) && (status.getSeverity() == IStatus.ERROR)) {
			setInvalid(fResourceBundlePackage, status.getMessage());
			return;
		}

		IPath pkgPath= new Path(pkgName.replace('.', IPath.SEPARATOR)).makeRelative();

		IJavaProject project= getNLSRefactoring().getCu().getJavaProject();
		try {
			IJavaElement element= project.findElement(pkgPath);
			if (element == null || !element.exists()) {
				setInvalid(fResourceBundlePackage, NLSUIMessages.getString("wizardPage2.must_exist")); //$NON-NLS-1$
				return;
			}
			IPackageFragment fPkgFragment= (IPackageFragment)element;
			if (!PackageBrowseAdapter.canAddPackage(fPkgFragment)) {
				setInvalid(fResourceBundlePackage, NLSUIMessages.getString("wizardPage2.incorrect_package")); //$NON-NLS-1$
				return;
			}
			if (!PackageBrowseAdapter.canAddPackageRoot((IPackageFragmentRoot)fPkgFragment.getParent())) {
				setInvalid(fResourceBundlePackage, NLSUIMessages.getString("wizardPage2.incorrect_package")); //$NON-NLS-1$
				return;
			}
		} catch (JavaModelException e) {
			setInvalid(fResourceBundlePackage, e.getStatus().getMessage());
			return;
		}

		setValid(fResourceBundlePackage);
	}

	private boolean checkPackageFragment(SourceFirstPackageSelectionDialogField selector, String invalidRoot, String invalidFragment) {
		IPackageFragmentRoot root= selector.getSelectedFragmentRoot();
		if ((root == null) || (root.exists() == false)) {
			setInvalid(selector, invalidRoot);
			return false;
		}

		IPackageFragment fragment= selector.getSelected();
		if ((fragment == null) || (fragment.exists() == false)) {
			setInvalid(selector, invalidFragment);
			return false;
		} else {
			setValid(selector);
			return true;
		}
	}

	private void validateSubstitutionPattern() {
		if ((fSubstitutionPattern.getText() == null) || (fSubstitutionPattern.getText().length() == 0)) {
			setInvalid(fSubstitutionPattern, NLSUIMessages.getString("wizardPage2.substitution.pattern.missing")); //$NON-NLS-1$
		} else {
			setValid(fSubstitutionPattern);
		}
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

	public boolean performFinish() {
		updateRefactoring();
		return super.performFinish();
	}

	public IWizardPage getNextPage() {
		updateRefactoring();
		return super.getNextPage();
	}

	public void dispose() {
		// TODO why nulling them???
		// widgets will be disposed. only need to null'em
		fSubstitutionPattern= null;
		fErrorMap= null;
		fResourceBundleFile= null;
		fResourceBundlePackage= null;
		super.dispose();
	}

	void updateRefactoring() {
		NLSRefactoring refac= getNLSRefactoring();

		refac.setPropertyFilePath(fResourceBundlePackage.getSelected().getPath().append(fResourceBundleFile.getText()));

		refac.setAccessorPackage(fAccessorPackage.getSelected());
		refac.setAccessorClassName(fAccessorClassName.getText());

		refac.setSubstitutionPattern(fSubstitutionPattern.getText());
	}

	private StringDialogField createStringField(String label) {
		StringDialogField field= new StringDialogField();
		field.setLabelText(label);
		return field;
	}

	private StringButtonDialogField createStringButtonField(String label, String button, IStringButtonAdapter adapter) {
		StringButtonDialogField field= new StringButtonDialogField(adapter);
		field.setLabelText(label);
		field.setButtonLabel(button);
		return field;
	}

	private IStringButtonAdapter createPropertyFileBrowseAdapter() {
		return new IStringButtonAdapter() {

			public void changeControlPressed(DialogField field) {
				browseForPropertyFile();
			}
		};
	}
}