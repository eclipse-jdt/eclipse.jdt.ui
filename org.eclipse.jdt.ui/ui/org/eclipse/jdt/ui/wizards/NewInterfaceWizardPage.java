/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.wizards;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * Wizard page for a new interface. This class is not intended to be subclassed.
 * To implement a different kind of new type wizard, extend <code>NewTypeWizardPage</code>.
 * @since 2.0
 */
public class NewInterfaceWizardPage extends NewTypeWizardPage {
	
	private final static String PAGE_NAME= "NewInterfaceWizardPage"; //$NON-NLS-1$
		
	public NewInterfaceWizardPage() {
		super(false, PAGE_NAME);
		
		setTitle(NewWizardMessages.getString("NewInterfaceWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewInterfaceWizardPage.description"));			 //$NON-NLS-1$
	}

	// -------- Initialization ---------

	/**
	 * Called from the wizard with the initial selection.
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
		
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.NEW_INTERFACE_WIZARD_PAGE);		
	}

	/*
	 * @see WizardPage#becomesVisible
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			setFocus();
		}
		super.setVisible(visible);
	}	
	
}