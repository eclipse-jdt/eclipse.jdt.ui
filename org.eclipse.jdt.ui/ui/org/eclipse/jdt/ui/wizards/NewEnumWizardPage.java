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

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * Wizard page to create a new enum type.
 * <p>
 * Note: This class is not intended to be subclassed. To implement a different kind of 
 * a new enum wizard page, extend <code>NewTypeWizardPage</code>.
 * </p>
 * 
 * @since 3.1
 */
public class NewEnumWizardPage extends NewTypeWizardPage {

    private final static String PAGE_NAME= "NewEnumWizardPage"; //$NON-NLS-1$
    private final static int TYPE = NewTypeWizardPage.ENUM_TYPE;
	
	/**
	 * Create a new <code>NewEnumWizardPage</code>
	 */	
	public NewEnumWizardPage() {
		super(TYPE, PAGE_NAME);
		
		setTitle(NewWizardMessages.getString("NewEnumWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewEnumWizardPage.description")); //$NON-NLS-1$
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
	}		
	
	// ------ validation --------

	private void doStatusUpdate() {
		// all used component status
		IStatus[] status= new IStatus[] {
			fContainerStatus,
			isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
			fTypeNameStatus,
			fModifierStatus,
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
		
		createContainerControls(composite, nColumns);	
		createPackageControls(composite, nColumns);	
		createEnclosingTypeControls(composite, nColumns);
				
		createSeparator(composite, nColumns);
		
		createTypeNameControls(composite, nColumns);
		createModifierControls(composite, nColumns);

		createSuperInterfacesControls(composite, nColumns);
						
		setControl(composite);
		
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.NEW_ENUM_WIZARD_PAGE);		
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
	
}
