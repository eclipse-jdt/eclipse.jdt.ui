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
package org.eclipse.jdt.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;

/**
 * Wizard page to  create a new class. 
 * <p>
 * Note: This class is not intended to be subclassed. To implement a different kind of 
 * a new class wizard page, extend <code>NewTypeWizardPage</code>.
 * </p>
 * 
 * @since 2.0
 */
public class NewClassWizardPage extends NewTypeWizardPage {
	
	private final static String PAGE_NAME= "NewClassWizardPage"; //$NON-NLS-1$
	
	private final static String SETTINGS_CREATEMAIN= "create_main"; //$NON-NLS-1$
	private final static String SETTINGS_CREATECONSTR= "create_constructor"; //$NON-NLS-1$
	private final static String SETTINGS_CREATEUNIMPLEMENTED= "create_unimplemented"; //$NON-NLS-1$
	
	private SelectionButtonDialogFieldGroup fMethodStubsButtons;
	
	/**
	 * Creates a new <code>NewClassWizardPage</code>
	 */
	public NewClassWizardPage() {
		super(true, PAGE_NAME);
		
		setTitle(NewWizardMessages.getString("NewClassWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewClassWizardPage.description")); //$NON-NLS-1$
		
		String[] buttonNames3= new String[] {
			NewWizardMessages.getString("NewClassWizardPage.methods.main"), NewWizardMessages.getString("NewClassWizardPage.methods.constructors"), //$NON-NLS-1$ //$NON-NLS-2$
			NewWizardMessages.getString("NewClassWizardPage.methods.inherited") //$NON-NLS-1$
		};		
		fMethodStubsButtons= new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames3, 1);
		fMethodStubsButtons.setLabelText(NewWizardMessages.getString("NewClassWizardPage.methods.label"));		 //$NON-NLS-1$
	}
	
	// -------- Initialization ---------
	
	/**
	 * The wizard owning this page is responsible for calling this method with the
	 * current selection. The selection is used to initialize the fields of the wizard 
	 * page.
	 * 
	 * @param selection used to initialize the fields
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);
		initContainerPage(jelem);
		initTypePage(jelem);
		doStatusUpdate();
		
		boolean createMain= false;
		boolean createConstructors= false;
		boolean createUnimplemented= true;
		IDialogSettings section= getDialogSettings().getSection(PAGE_NAME);
		if (section != null) {
			createMain= section.getBoolean(SETTINGS_CREATEMAIN);
			createConstructors= section.getBoolean(SETTINGS_CREATECONSTR);
			createUnimplemented= section.getBoolean(SETTINGS_CREATEUNIMPLEMENTED);
		}
		
		setMethodStubSelection(createMain, createConstructors, createUnimplemented, true);
	}
	
	// ------ validation --------
	private void doStatusUpdate() {
		// status of all used components
		IStatus[] status= new IStatus[] {
			fContainerStatus,
			isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
			fTypeNameStatus,
			fModifierStatus,
			fSuperClassStatus,
			fSuperInterfacesStatus
		};
		
		// the mode severe status will be displayed and the ok button enabled/disabled.
		updateStatus(status);
	}
	
	
	/*
	 * @see NewContainerWizardPage#handleFieldChanged
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		
		doStatusUpdate();
	}
	
	
	// ------ ui --------
	
	/*
	 * @see WizardPage#createControl
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		
		int nColumns= 4;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;		
		composite.setLayout(layout);
		
		// pick & choose the wanted UI components
		
		createContainerControls(composite, nColumns);	
		createPackageControls(composite, nColumns);	
		createEnclosingTypeControls(composite, nColumns);
				
		createSeparator(composite, nColumns);
		
		createTypeNameControls(composite, nColumns);
		createModifierControls(composite, nColumns);
			
		createSuperClassControls(composite, nColumns);
		createSuperInterfacesControls(composite, nColumns);
				
		createMethodStubSelectionControls(composite, nColumns);
		
		setControl(composite);
			
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.NEW_CLASS_WIZARD_PAGE);	
	}
	
	/*
	 * @see WizardPage#becomesVisible
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
		}
	}
		
	
	private void createMethodStubSelectionControls(Composite composite, int nColumns) {
		Control labelControl= fMethodStubsButtons.getLabelControl(composite);
		LayoutUtil.setHorizontalSpan(labelControl, nColumns);
		
		DialogField.createEmptySpace(composite);
		
		Control buttonGroup= fMethodStubsButtons.getSelectionButtonsGroup(composite);
		LayoutUtil.setHorizontalSpan(buttonGroup, nColumns - 1);	
	}
	
	/**
	 * Returns the current selection state of the 'Create Main' checkbox.
	 * 
	 * @return the selection state of the 'Create Main' checkbox
	 */
	public boolean isCreateMain() {
		return fMethodStubsButtons.isSelected(0);
	}

	/**
	 * Returns the current selection state of the 'Create Constructors' checkbox.
	 * 
	 * @return the selection state of the 'Create Constructors' checkbox
	 */
	public boolean isCreateConstructors() {
		return fMethodStubsButtons.isSelected(1);
	}
	
	/**
	 * Returns the current selection state of the 'Create inherited abstract methods' 
	 * checkbox.
	 * 
	 * @return the selection state of the 'Create inherited abstract methods' checkbox
	 */
	public boolean isCreateInherited() {
		return fMethodStubsButtons.isSelected(2);
	}

	/**
	 * Sets the selection state of the method stub checkboxes.
	 * 
	 * @param createMain initial selection state of the 'Create Main' checkbox.
	 * @param createConstructors initial selection state of the 'Create Constructors' checkbox.
	 * @param createInherited initial selection state of the 'Create inherited abstract methods' checkbox.
	 * @param canBeModified if <code>true</code> the method stub checkboxes can be changed by 
	 * the user. If <code>false</code> the buttons are "read-only"
	 */
	public void setMethodStubSelection(boolean createMain, boolean createConstructors, boolean createInherited, boolean canBeModified) {
		fMethodStubsButtons.setSelection(0, createMain);
		fMethodStubsButtons.setSelection(1, createConstructors);
		fMethodStubsButtons.setSelection(2, createInherited);
		
		fMethodStubsButtons.setEnabled(canBeModified);
	}	
	
	// ---- creation ----------------
	
	/*
	 * @see NewTypeWizardPage#createTypeMembers
	 */
	protected void createTypeMembers(IType type, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		boolean doMain= isCreateMain();
		boolean doConstr= isCreateConstructors();
		boolean doInherited= isCreateInherited();
		createInheritedMethods(type, doConstr, doInherited, imports, new SubProgressMonitor(monitor, 1));

		if (doMain) {
			StringBuffer buf= new StringBuffer();
			buf.append("public static void main("); //$NON-NLS-1$
			buf.append(imports.addImport("java.lang.String")); //$NON-NLS-1$
			buf.append("[] args) {}"); //$NON-NLS-1$
			type.createMethod(buf.toString(), null, false, null);
		}
		
		IDialogSettings section= getDialogSettings().getSection(PAGE_NAME);
		if (section == null) {
			section= getDialogSettings().addNewSection(PAGE_NAME);
		}
		section.put(SETTINGS_CREATEMAIN, doMain);
		section.put(SETTINGS_CREATECONSTR, doConstr);
		section.put(SETTINGS_CREATEUNIMPLEMENTED, doInherited);
		
		if (monitor != null) {
			monitor.done();
		}	
	}
	
}
