/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridLayout;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;

/**
 * Wizard page for creating a new class. This class is not intended to be subclassed.
 * To implement a different kind of type wizard, extend <code>NewTypeWizardPage</code>.
 * @since 2.0
 */
public class NewClassWizardPage extends NewTypeWizardPage {
	
	private final static String PAGE_NAME= "NewClassWizardPage"; //$NON-NLS-1$
	
	private final static String SETTINGS_CREATEMAIN= "create_main"; //$NON-NLS-1$
	private final static String SETTINGS_CREATECONSTR= "create_constructor"; //$NON-NLS-1$
	private final static String SETTINGS_CREATEUNIMPLEMENTED= "create_unimplemented"; //$NON-NLS-1$
	
	private SelectionButtonDialogFieldGroup fMethodStubsButtons;
	
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
	 * Called from the wizard with the initial selection.
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
			
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.NEW_CLASS_WIZARD_PAGE);	
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
		
	
	private void createMethodStubSelectionControls(Composite composite, int nColumns) {
		Control labelControl= fMethodStubsButtons.getLabelControl(composite);
		LayoutUtil.setHorizontalSpan(labelControl, nColumns);
		
		DialogField.createEmptySpace(composite);
		
		Control buttonGroup= fMethodStubsButtons.getSelectionButtonsGroup(composite);
		LayoutUtil.setHorizontalSpan(buttonGroup, nColumns - 1);	
	}
	
	/**
	 * Returns the current selection of the 'Create Main' checkbox.
	 */
	public boolean isCreateMain() {
		return fMethodStubsButtons.isSelected(0);
	}

	/**
	 * Returns the current selection of the 'Create Constructors' checkbox.
	 */
	public boolean isCreateConstructors() {
		return fMethodStubsButtons.isSelected(1);
	}
	
	/**
	 * Returns the current selection of the 'Create inherited abstract methods' checkbox.
	 */
	public boolean isCreateInherited() {
		return fMethodStubsButtons.isSelected(2);
	}

	/**
	 * Sets the selection of the method stub buttons.
	 * @param createMain Selection of the 'Create Main' checkbox.
	 * @param createConstructors Selection of the 'Create Constructors' checkbox.
	 * @param createInherited Selection of the 'Create inherited abstract methods' checkbox.
	 * @param canBeModified Selects if the method stub buttons can be changed by the user
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
		List newMethods= new ArrayList();
		
		boolean doMain= isCreateMain();
		boolean doConstr= isCreateConstructors();
		boolean doInherited= isCreateInherited();
		createInheritedMethods(type, doConstr, doInherited, imports, new SubProgressMonitor(monitor, 1));

		if (doMain) {
			String main= "public static void main(String[] args) {}"; //$NON-NLS-1$
			type.createMethod(main, null, false, null);
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