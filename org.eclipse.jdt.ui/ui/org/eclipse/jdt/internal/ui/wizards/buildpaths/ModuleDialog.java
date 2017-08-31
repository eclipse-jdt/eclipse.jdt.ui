/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

public class ModuleDialog extends StatusDialog {

	private static final String NO_NAME= ""; //$NON-NLS-1$

	public class AddExportsLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof ModuleAddExport) {
				ModuleAddExport export = (ModuleAddExport) element;
				switch (columnIndex) {
					case 0: return export.fSourceModule;
					case 1: return export.fPackage;
					case 2: return export.fTargetModules;
					default:
						throw new IllegalArgumentException("Illegal column index "+columnIndex); //$NON-NLS-1$
				}
			}
			return NO_NAME;
		}

	}

	private final SelectionButtonDialogField fIsModuleCheckbox;
	private final ListDialogField<ModuleAddExport> fAddExportsList;
	private final CPListElement fCurrCPElement;
	/** The element(s) targeted by the current CP entry, which will be the source module(s) of the added exports. */
	private IJavaElement[] fJavaElements;

	private static final int IDX_ADD= 0;
	private static final int IDX_EDIT= 1;
	private static final int IDX_REMOVE= 2;


	public ModuleDialog(Shell parent, CPListElement entryToEdit, IJavaElement[] selectedElements) {
		super(parent);

		fCurrCPElement= entryToEdit;

		setTitle(NewWizardMessages.ModuleDialog_title);

		fIsModuleCheckbox= new SelectionButtonDialogField(SWT.CHECK);
		fIsModuleCheckbox.setLabelText(NewWizardMessages.ModuleDialog_defines_modules_label);
		fIsModuleCheckbox.setSelection(entryToEdit.getAttribute(CPListElement.MODULE) != null);
		fIsModuleCheckbox.setDialogFieldListener(new AddExportsAdapter());

		if (fCurrCPElement.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
			IPackageFragmentRoot[] roots= fCurrCPElement.getJavaProject().findPackageFragmentRoots(fCurrCPElement.getClasspathEntry());
			if (roots.length > 1 && roots[0].getModuleDescription() != null)
				fIsModuleCheckbox.setEnabled(false); // assume multi-module container is Java 9 JRE
		}

		fAddExportsList= createListContents(entryToEdit);

		fJavaElements= selectedElements;
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @since 3.4
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	private ListDialogField<ModuleAddExport> createListContents(CPListElement entryToEdit) {
		String label= NewWizardMessages.ModuleDialog_exports_label;
		String[] buttonLabels= new String[] {
				NewWizardMessages.ModuleDialog_exports_add,
				NewWizardMessages.ModuleDialog_exports_edit,
				NewWizardMessages.ModuleDialog_exports_remove
		};

		AddExportsAdapter adapter= new AddExportsAdapter();
		AddExportsLabelProvider labelProvider= new AddExportsLabelProvider();

		ListDialogField<ModuleAddExport> exportsList= new ListDialogField<>(adapter, buttonLabels, labelProvider);
		exportsList.setDialogFieldListener(adapter);

		exportsList.setLabelText(label);
		exportsList.setRemoveButtonIndex(IDX_REMOVE);
		exportsList.enableButton(IDX_EDIT, false);

		ArrayList<ModuleAddExport> elements;
		Object moduleDetails= entryToEdit.getAttribute(CPListElement.MODULE);
		if (moduleDetails instanceof ModuleAddExport[]) {
			ModuleAddExport[] exports= (ModuleAddExport[]) moduleDetails;
			elements= new ArrayList<>(exports.length);
			for (int i= 0; i < exports.length; i++) {
				elements.add(exports[i]);
			}
		} else {
			elements= new ArrayList<>(0);
		}
		exportsList.setElements(elements);
		exportsList.selectFirstElement();
		return exportsList;
	}


	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);

		Composite inner= new Composite(composite, SWT.NONE);
		inner.setFont(composite.getFont());
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label description= new Label(inner, SWT.WRAP);
		
		description.setText(getDescriptionString());
		
		GridData data= new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
		data.widthHint= convertWidthInCharsToPixels(100);
		description.setLayoutData(data);

		fIsModuleCheckbox.doFillIntoGrid(inner, 2);

		ColumnLayoutData[] columnDta= {
				new ColumnWeightData(2),
				new ColumnWeightData(3),
				new ColumnWeightData(2),
		};
		String[] headers= {
				NewWizardMessages.ModuleDialog_source_module_header,
				NewWizardMessages.ModuleDialog_package_header,
				NewWizardMessages.ModuleDialog_target_modules_header
		};
		fAddExportsList.setTableColumns(new ListDialogField.ColumnsDescription(columnDta, headers, true));

		fAddExportsList.doFillIntoGrid(inner, 3);

		LayoutUtil.setHorizontalSpan(fAddExportsList.getLabelControl(null), 2);

		data= (GridData) fAddExportsList.getListControl(null).getLayoutData();
		data.grabExcessHorizontalSpace= true;
		data.heightHint= SWT.DEFAULT;

		applyDialogFont(composite);
		return composite;
	}

	private String getDescriptionString() {
		String desc;
		String name= BasicElementLabels.getResourceName(fCurrCPElement.getPath().lastSegment());
		switch (fCurrCPElement.getEntryKind()) {
			case IClasspathEntry.CPE_CONTAINER:
				try {
					name= JavaElementLabels.getContainerEntryLabel(fCurrCPElement.getPath(), fCurrCPElement.getJavaProject());
				} catch (JavaModelException e) {
					name= BasicElementLabels.getPathLabel(fCurrCPElement.getPath(), false);
				}
				desc= NewWizardMessages.ModuleDialog_container_description;
				break;
			case IClasspathEntry.CPE_PROJECT:
				desc=  NewWizardMessages.ModuleDialog_project_description;
				break;
			default:
				desc=  NewWizardMessages.ModuleDialog_description;
		}

		return Messages.format(desc, name);
	}


	protected void doCustomButtonPressed(ListDialogField<ModuleAddExport> field, int index) {
		if (index == IDX_ADD) {
			addEntry(field);
		} else if (index == IDX_EDIT) {
			editEntry(field);
		}
	}

	protected void doDoubleClicked(ListDialogField<ModuleAddExport> field) {
		editEntry(field);
	}

	protected void doSelectionChanged(ListDialogField<ModuleAddExport> field) {
		List<ModuleAddExport> selected= field.getSelectedElements();
		field.enableButton(IDX_EDIT, canEdit(selected));
		field.enableButton(IDX_REMOVE, selected.size() > 0);
		validate();
	}

	private boolean canEdit(List<ModuleAddExport> selected) {
		return selected.size() == 1;
	}

	private void validate() {
		Set<String> packages= new HashSet<>();
		StatusInfo status= new StatusInfo();
		for (ModuleAddExport export : fAddExportsList.getElements()) {
			if (!packages.add(export.fPackage)) {
				status.setError(Messages.format(NewWizardMessages.ModuleDialog_duplicatePackage_error, export.fPackage));
				break;
			}
		}
		updateStatus(status);
	}

	private void editEntry(ListDialogField<ModuleAddExport> field) {

		List<ModuleAddExport> selElements= field.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		ModuleAddExport export= selElements.get(0);
		ModuleAddExportsDialog dialog= new ModuleAddExportsDialog(getShell(), fJavaElements, export);
		if (dialog.open() == Window.OK) {
			ModuleAddExport newExport= dialog.getExport();
			if (newExport != null) {
				field.replaceElement(export, newExport);
			} else {
				field.removeElement(export);
			}
		}
	}

	private void addEntry(ListDialogField<ModuleAddExport> field) {
		ModuleAddExport initialValue= new ModuleAddExport(getSourceModuleName(), NO_NAME, getCurrentModuleName(), null);
		ModuleAddExportsDialog dialog= new ModuleAddExportsDialog(getShell(), fJavaElements, initialValue);
		if (dialog.open() == Window.OK) {
			ModuleAddExport export= dialog.getExport();
			if (export != null)
				field.addElement(export);
		}
	}

	private String getSourceModuleName() {
		if (fJavaElements == null || fJavaElements.length != 1) {
			return NO_NAME;
		}
		IModuleDescription module= null;
		switch (fJavaElements[0].getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				try {
					module= ((IJavaProject) fJavaElements[0]).getModuleDescription();
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				module= ((IPackageFragmentRoot) fJavaElements[0]).getModuleDescription();
				break;
			default:
				// not applicable
		}
		return module != null ? module.getElementName() : NO_NAME;
	}

	private String getCurrentModuleName() {
		IModuleDescription module= null;
		try {
			module= fCurrCPElement.getJavaProject().getModuleDescription();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return module != null ? module.getElementName() : JavaModelUtil.ALL_UNNAMED;
	}

	// -------- TypeRestrictionAdapter --------

	private class AddExportsAdapter implements IListAdapter<ModuleAddExport>, IDialogFieldListener {
		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#customButtonPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField, int)
		 */
		@Override
		public void customButtonPressed(ListDialogField<ModuleAddExport> field, int index) {
			doCustomButtonPressed(field, index);
		}

		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#selectionChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		@Override
		public void selectionChanged(ListDialogField<ModuleAddExport> field) {
			doSelectionChanged(field);
		}
		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		@Override
		public void doubleClicked(ListDialogField<ModuleAddExport> field) {
			doDoubleClicked(field);
		}

		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		@Override
		public void dialogFieldChanged(DialogField field) {
			if (field == fIsModuleCheckbox) {
				if (!fIsModuleCheckbox.isSelected()) {
					fAddExportsList.enableButton(IDX_ADD, false);
					fAddExportsList.enableButton(IDX_EDIT, false);
					fAddExportsList.enableButton(IDX_REMOVE, false);
				} else {
					List<ModuleAddExport> elements= fAddExportsList.getSelectedElements();
					fAddExportsList.enableButton(IDX_ADD, true);
					fAddExportsList.enableButton(IDX_EDIT, canEdit(elements));
					fAddExportsList.enableButton(IDX_REMOVE, elements.size() > 0);
				}
			}
		}
	}

	public ModuleAddExport[] getAddExports() {
		if (!fIsModuleCheckbox.isSelected())
			return null;
		List<ModuleAddExport> elements= fAddExportsList.getElements();
		return elements.toArray(new ModuleAddExport[elements.size()]);
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		String helpContextId;
		if (fCurrCPElement.getEntryKind() == IClasspathEntry.CPE_PROJECT)
			helpContextId= IJavaHelpContextIds.ACCESS_RULES_DIALOG_COMBINE_RULES; // FIXME
		else
			helpContextId= IJavaHelpContextIds.ACCESS_RULES_DIALOG; // FIXME
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, helpContextId);
	}
}
