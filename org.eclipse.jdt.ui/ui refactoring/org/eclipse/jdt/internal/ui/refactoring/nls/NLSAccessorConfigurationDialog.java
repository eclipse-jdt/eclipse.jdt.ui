/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.progress.IProgressService;

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
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class NLSAccessorConfigurationDialog extends StatusDialog {


	private SourceFirstPackageSelectionDialogField fResourceBundlePackage;
	private StringButtonDialogField fResourceBundleFile;
	private SourceFirstPackageSelectionDialogField fAccessorPackage;
	private StringDialogField fAccessorClassName;
	private StringDialogField fSubstitutionPattern;

	private NLSRefactoring fRefactoring;
	
	private IStatus[] fStati;
	
	private static final int IDX_ACCESSOR_CLASS= 0;
	private static final int IDX_ACCESSOR_PACKAGE= 1;
	private static final int IDX_SUBST_PATTERN= 2;
	private static final int IDX_BUNDLE_NAME= 3;
	private static final int IDX_BUNDLE_PACKAGE= 4;

	private class AccessorAdapter implements IDialogFieldListener, IStringButtonAdapter {
		public void dialogFieldChanged(DialogField field) {
			validateAll();
		}

		public void changeControlPressed(DialogField field) {
			if (field == fResourceBundleFile) {
				browseForPropertyFile();
			} else if (field == fAccessorClassName) {
				browseForAccessorClass();
			}
		}
	}
	
	
	public NLSAccessorConfigurationDialog(Shell parent, NLSRefactoring refactoring) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		fRefactoring= refactoring;
		fStati= new IStatus[] { StatusInfo.OK_STATUS, StatusInfo.OK_STATUS, StatusInfo.OK_STATUS, StatusInfo.OK_STATUS, StatusInfo.OK_STATUS };
		
		setTitle(NLSUIMessages.getString("NLSAccessorConfigurationDialog.title")); //$NON-NLS-1$

		AccessorAdapter updateListener= new AccessorAdapter();

		ICompilationUnit cu= refactoring.getCu();

		fAccessorPackage= new SourceFirstPackageSelectionDialogField(NLSUIMessages.getString("NLSAccessorConfigurationDialog.accessor.path"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.accessor.package"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.browse1"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.browse2"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.default_package"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.accessor.dialog.title"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.accessor.dialog.message"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.accessor.dialog.emtpyMessage"), //$NON-NLS-1$
				cu, updateListener, refactoring.getAccessorClassPackage());

		fAccessorClassName= createStringButtonField(NLSUIMessages.getString("NLSAccessorConfigurationDialog.className"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.browse6"), updateListener); //$NON-NLS-1$
		fSubstitutionPattern= createStringField(NLSUIMessages.getString("NLSAccessorConfigurationDialog.substitutionPattern"), updateListener); //$NON-NLS-1$

		fResourceBundlePackage= new SourceFirstPackageSelectionDialogField(NLSUIMessages.getString("NLSAccessorConfigurationDialog.property.path"), //$NON-NLS-1$ 
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.property.package"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.browse3"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.browse4"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.default_package"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.property.dialog.title"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.property.dialog.message"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.property.dialog.emptyMessage"), //$NON-NLS-1$
				cu, updateListener, fRefactoring.getResourceBundlePackage());

		fResourceBundleFile= createStringButtonField(NLSUIMessages.getString("NLSAccessorConfigurationDialog.property_file_name"), //$NON-NLS-1$
				NLSUIMessages.getString("NLSAccessorConfigurationDialog.browse5"), updateListener); //$NON-NLS-1$

		initFields();
	}

	private void initFields() {
		initAccessorClassFields();
		String resourceBundleName= fRefactoring.getResourceBundleName();
		fResourceBundleFile.setText(resourceBundleName != null ? resourceBundleName : NLSRefactoring.getDefaultPropertiesFilename());
	}

	private void initAccessorClassFields() {
		String accessorClassName= fRefactoring.getAccessorClassName();

		if (accessorClassName == null) {
			accessorClassName= NLSRefactoring.DEFAULT_ACCESSOR_CLASSNAME;
		}
		fAccessorClassName.setText(accessorClassName);

		fSubstitutionPattern.setText(fRefactoring.getSubstitutionPattern());
	}

	protected Control createDialogArea(Composite ancestor) {
		Composite parent= (Composite) super.createDialogArea(ancestor);

		final int nOfColumns= 4;

		initializeDialogUnits(ancestor);

		GridLayout layout= (GridLayout) parent.getLayout();
		layout.numColumns= nOfColumns;
		parent.setLayout(layout);

		createAccessorPart(parent, nOfColumns, convertWidthInCharsToPixels(40));

		Separator s= new Separator(SWT.SEPARATOR | SWT.HORIZONTAL);
		s.doFillIntoGrid(parent, nOfColumns);

		createPropertyPart(parent, nOfColumns, convertWidthInCharsToPixels(40));

		Dialog.applyDialogFont(parent);
		WorkbenchHelp.setHelp(parent, IJavaHelpContextIds.EXTERNALIZE_WIZARD_PROPERTIES_FILE_PAGE);
		validateAll();
		return parent;
	}


	private void createAccessorPart(Composite parent, final int nOfColumns, int textWidth) {

		createLabel(parent, NLSUIMessages.getString("NLSAccessorConfigurationDialog.resourceBundle.title"), nOfColumns); //$NON-NLS-1$
		fAccessorPackage.createControl(parent, nOfColumns, textWidth);

		fAccessorClassName.doFillIntoGrid(parent, nOfColumns);
		LayoutUtil.setWidthHint(fAccessorClassName.getTextControl(null), convertWidthInCharsToPixels(60));

		fSubstitutionPattern.doFillIntoGrid(parent, nOfColumns);
		LayoutUtil.setWidthHint(fSubstitutionPattern.getTextControl(null), convertWidthInCharsToPixels(60));
	}

	private void createPropertyPart(Composite parent, final int nOfColumns, final int textWidth) {
		Separator label= new Separator(SWT.NONE);
		((Label) label.getSeparator(parent)).setText(NLSUIMessages.getString("NLSAccessorConfigurationDialog.property_location")); //$NON-NLS-1$
		label.doFillIntoGrid(parent, nOfColumns, 20);
		fResourceBundlePackage.createControl(parent, nOfColumns, textWidth);

		fResourceBundleFile.doFillIntoGrid(parent, nOfColumns);
		LayoutUtil.setWidthHint(fResourceBundleFile.getTextControl(null), convertWidthInCharsToPixels(60));
	}

	private void createLabel(Composite parent, final String text, final int N_OF_COLUMNS) {
		Separator label= new Separator(SWT.NONE);
		((Label) label.getSeparator(parent)).setText(text);
		label.doFillIntoGrid(parent, N_OF_COLUMNS, 20);
	}

	private void browseForPropertyFile() {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider());
		dialog.setIgnoreCase(false);
		dialog.setTitle(NLSUIMessages.getString("NLSAccessorConfigurationDialog.Property_File_Selection")); //$NON-NLS-1$
		dialog.setMessage(NLSUIMessages.getString("NLSAccessorConfigurationDialog.Choose_the_property_file")); //$NON-NLS-1$
		dialog.setElements(createFileListInput());
		dialog.setFilter('*' + NLSRefactoring.PROPERTY_FILE_EXT);
		if (dialog.open() == Window.OK) {
			IFile selectedFile= (IFile) dialog.getFirstResult();
			if (selectedFile != null)
				fResourceBundleFile.setText(selectedFile.getName());
		}
	}

	protected void browseForAccessorClass() {
		IProgressService service= PlatformUI.getWorkbench().getProgressService();
		IPackageFragmentRoot root= fAccessorPackage.getSelectedFragmentRoot();
		
		IJavaSearchScope scope= root != null ? SearchEngine.createJavaSearchScope(new IJavaElement[] { root }) : SearchEngine.createWorkspaceScope();
		
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), service, IJavaSearchConstants.CLASS, scope);
		dialog.setIgnoreCase(true);
		dialog.setTitle(NLSUIMessages.getString("NLSAccessorConfigurationDialog.Accessor_Selection")); //$NON-NLS-1$
		dialog.setMessage(NLSUIMessages.getString("NLSAccessorConfigurationDialog.Choose_the_accessor_file")); //$NON-NLS-1$
		dialog.setFilter("*Messages"); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {
			IType selectedType= (IType) dialog.getFirstResult();
			if (selectedType != null) {
				fAccessorClassName.setText(selectedType.getElementName());
				fAccessorPackage.setSelected(selectedType.getPackageFragment());
			}
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
			ExceptionHandler.handle(e, NLSUIMessages.getString("NLSAccessorConfigurationDialog.externalizing"), NLSUIMessages //$NON-NLS-1$
					.getString("NLSAccessorConfigurationDialog.exception")); //$NON-NLS-1$
			return new Object[0];
		}
	}

	private static boolean isPropertyFile(Object o) {
		if (!(o instanceof IFile))
			return false;
		IFile file= (IFile) o;
		return (NLSRefactoring.PROPERTY_FILE_EXT.equals('.' + file.getFileExtension()));
	}

	/**
	 * checks all entered values delegates to the specific validate methods these methods
	 * update the refactoring
	 */
	private void validateAll() {
		validateSubstitutionPattern();

		validateAccessorClassName();
		checkPackageFragment();

		validatePropertyFilename();
		validatePropertyPackage();
		
		updateStatus(StatusUtil.getMostSevere(fStati));
	}

	private void validateAccessorClassName() {
		String className= fAccessorClassName.getText();

		IStatus status= JavaConventions.validateJavaTypeName(className);
		if (status.getSeverity() == IStatus.ERROR) {
			setInvalid(IDX_ACCESSOR_CLASS, status.getMessage());
			return;
		}

		if (className.indexOf('.') != -1) {
			setInvalid(IDX_ACCESSOR_CLASS, NLSUIMessages.getString("NLSAccessorConfigurationDialog.no_dot")); //$NON-NLS-1$
			return;
		}

		setValid(IDX_ACCESSOR_CLASS);
	}

	private void validatePropertyFilename() {
		String fileName= fResourceBundleFile.getText();
		if ((fileName == null) || (fileName.length() == 0)) {
			setInvalid(IDX_BUNDLE_NAME, NLSUIMessages.getString("NLSAccessorConfigurationDialog.enter_name")); //$NON-NLS-1$
			return;
		}

		if (!fileName.endsWith(NLSRefactoring.PROPERTY_FILE_EXT)) {
			setInvalid(IDX_BUNDLE_NAME, NLSUIMessages.getFormattedString("NLSAccessorConfigurationDialog.file_name_must_end", NLSRefactoring.PROPERTY_FILE_EXT)); //$NON-NLS-1$
			return;
		}

		setValid(IDX_BUNDLE_NAME);
	}

	private void validatePropertyPackage() {
		
		IPackageFragmentRoot root= fResourceBundlePackage.getSelectedFragmentRoot();
		if ((root == null) || !root.exists()) {
			setInvalid(IDX_BUNDLE_PACKAGE, NLSUIMessages.getString("NLSAccessorConfigurationDialog.property.package.root.invalid")); //$NON-NLS-1$
			return;
		}

		IPackageFragment fragment= fResourceBundlePackage.getSelected();
		if ((fragment == null) || !fragment.exists()) {
			setInvalid(IDX_BUNDLE_PACKAGE, NLSUIMessages.getString("NLSAccessorConfigurationDialog.property.package.invalid")); //$NON-NLS-1$
			return;
		}
		
		String pkgName= fragment.getElementName();

		IStatus status= JavaConventions.validatePackageName(pkgName);
		if ((pkgName.length() > 0) && (status.getSeverity() == IStatus.ERROR)) {
			setInvalid(IDX_BUNDLE_PACKAGE, status.getMessage());
			return;
		}

		IPath pkgPath= new Path(pkgName.replace('.', IPath.SEPARATOR)).makeRelative();

		IJavaProject project= fRefactoring.getCu().getJavaProject();
		try {
			IJavaElement element= project.findElement(pkgPath);
			if (element == null || !element.exists()) {
				setInvalid(IDX_BUNDLE_PACKAGE, NLSUIMessages.getString("NLSAccessorConfigurationDialog.must_exist")); //$NON-NLS-1$
				return;
			}
			IPackageFragment fPkgFragment= (IPackageFragment) element;
			if (!PackageBrowseAdapter.canAddPackage(fPkgFragment)) {
				setInvalid(IDX_BUNDLE_PACKAGE, NLSUIMessages.getString("NLSAccessorConfigurationDialog.incorrect_package")); //$NON-NLS-1$
				return;
			}
			if (!PackageBrowseAdapter.canAddPackageRoot((IPackageFragmentRoot) fPkgFragment.getParent())) {
				setInvalid(IDX_BUNDLE_PACKAGE, NLSUIMessages.getString("NLSAccessorConfigurationDialog.incorrect_package")); //$NON-NLS-1$
				return;
			}
		} catch (JavaModelException e) {
			setInvalid(IDX_BUNDLE_PACKAGE, e.getStatus().getMessage());
			return;
		}

		setValid(IDX_BUNDLE_PACKAGE);
	}

	private void checkPackageFragment() {
		IPackageFragmentRoot root= fAccessorPackage.getSelectedFragmentRoot();
		if ((root == null) || !root.exists()) {
			setInvalid(IDX_ACCESSOR_PACKAGE, NLSUIMessages.getString("NLSAccessorConfigurationDialog.accessor.package.root.invalid")); //$NON-NLS-1$
			return;
		}

		IPackageFragment fragment= fAccessorPackage.getSelected();
		if ((fragment == null) || !fragment.exists()) {
			setInvalid(IDX_ACCESSOR_PACKAGE, NLSUIMessages.getString("NLSAccessorConfigurationDialog.accessor.package.invalid")); //$NON-NLS-1$
			return;
		}
		setValid(IDX_ACCESSOR_PACKAGE);
	}

	private void validateSubstitutionPattern() {
		if ((fSubstitutionPattern.getText() == null) || (fSubstitutionPattern.getText().length() == 0)) {
			setInvalid(IDX_SUBST_PATTERN, NLSUIMessages.getString("NLSAccessorConfigurationDialog.substitution.pattern.missing")); //$NON-NLS-1$
		} else {
			setValid(IDX_SUBST_PATTERN);
		}
	}

	private void setInvalid(int idx, String msg) {
		fStati[idx]= new StatusInfo(IStatus.ERROR, msg);
	}

	private void setValid(int idx) {
		fStati[idx]= StatusInfo.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		updateRefactoring();
		super.okPressed();
	}


	void updateRefactoring() {
		NLSRefactoring refactoring= fRefactoring;

		refactoring.setAccessorClassPackage(fAccessorPackage.getSelected());
		refactoring.setAccessorClassName(fAccessorClassName.getText());

		refactoring.setResourceBundleName(fResourceBundleFile.getText());
		refactoring.setResourceBundlePackage(fResourceBundlePackage.getSelected());

		refactoring.setSubstitutionPattern(fSubstitutionPattern.getText());
	}

	private StringDialogField createStringField(String label, AccessorAdapter updateListener) {
		StringDialogField field= new StringDialogField();
		field.setDialogFieldListener(updateListener);
		field.setLabelText(label);
		return field;
	}

	private StringButtonDialogField createStringButtonField(String label, String button, AccessorAdapter adapter) {
		StringButtonDialogField field= new StringButtonDialogField(adapter);
		field.setDialogFieldListener(adapter);
		field.setLabelText(label);
		field.setButtonLabel(button);
		return field;
	}


}
