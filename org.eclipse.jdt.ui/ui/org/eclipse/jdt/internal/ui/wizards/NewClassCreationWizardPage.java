/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.codemanipulation.IImportsStructure;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class NewClassCreationWizardPage extends TypePage {
	
	private final static String PAGE_NAME= "NewClassCreationWizardPage"; //$NON-NLS-1$
	
	private SelectionButtonDialogFieldGroup fMethodStubsButtons;
	
	public NewClassCreationWizardPage(IWorkspaceRoot root) {
		super(true, PAGE_NAME, root);
		
		setTitle(NewWizardMessages.getString("NewClassCreationWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewClassCreationWizardPage.description")); //$NON-NLS-1$
		
		String[] buttonNames3= new String[] {
			NewWizardMessages.getString("NewClassCreationWizardPage.methods.main"), NewWizardMessages.getString("NewClassCreationWizardPage.methods.constructors"), //$NON-NLS-1$ //$NON-NLS-2$
			NewWizardMessages.getString("NewClassCreationWizardPage.methods.inherited") //$NON-NLS-1$
		};		
		fMethodStubsButtons= new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames3, 1);
		fMethodStubsButtons.setLabelText(NewWizardMessages.getString("NewClassCreationWizardPage.methods.label"));		 //$NON-NLS-1$
	}

	// -------- Initialization ---------

	/**
	 * Should be called from the wizard with the input element.
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);

		initContainerPage(jelem);
		initTypePage(jelem);
		updateStatus(findMostSevereStatus());
		
		fMethodStubsButtons.setSelection(0, false);
		fMethodStubsButtons.setSelection(1, false);
		fMethodStubsButtons.setSelection(2, true);
	}

	// ------ validation --------
	
	/**
	 * Finds the most severe error (if there is one)
	 */
	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] {
			fContainerStatus,
			isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
			fTypeNameStatus,
			fModifierStatus,
			fSuperClassStatus,
			fSuperInterfacesStatus
		});
	}
	
	/*
	 * @see ContainerPage#handleFieldChanged
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		
		updateStatus(findMostSevereStatus());
	}
	
	
	// ------ ui --------
	
	/*
	 * @see WizardPage#createControl
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		
		int nColumns= 4;
		
		MGridLayout layout= new MGridLayout();
		layout.minimumWidth= SWTUtil.convertWidthInCharsToPixels(80, composite);
		layout.numColumns= nColumns;		
		composite.setLayout(layout);
		
		createContainerControls(composite, nColumns);	
		createPackageControls(composite, nColumns);	
		createEnclosingTypeControls(composite, nColumns);
				
		createSeparator(composite, nColumns);
		
		createTypeNameControls(composite, nColumns);
		createModifierControls(composite, nColumns);

		// createSeparator(composite, nColumns);
				
		createSuperClassControls(composite, nColumns);
		createSuperInterfacesControls(composite, nColumns);
				
		//createSeparator(composite, nColumns);
		
		createMethodStubSelectionControls(composite, nColumns);
		
		setControl(composite);
		
		setFocus();
		WorkbenchHelp.setHelp(composite, new DialogPageContextComputer(this, IJavaHelpContextIds.NEW_CLASS_WIZARD_PAGE));	
	}
	
	protected void createMethodStubSelectionControls(Composite composite, int nColumns) {
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getLabelControl(composite), nColumns);
		DialogField.createEmptySpace(composite);
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getSelectionButtonsGroup(composite), nColumns - 1);	
	}	
	
	// ---- creation ----------------
	
	/*
	 * @see TypePage#evalMethods
	 */
	protected String[] evalMethods(IType type, IImportsStructure imports, IProgressMonitor monitor) throws CoreException {
		List newMethods= new ArrayList();
		
		boolean doConstr= fMethodStubsButtons.isSelected(1);
		boolean doInherited= fMethodStubsButtons.isSelected(2);
		String[] meth= constructInheritedMethods(type, doConstr, doInherited, imports, new SubProgressMonitor(monitor, 1));
		for (int i= 0; i < meth.length; i++) {
			newMethods.add(meth[i]);
		}
		if (monitor != null) {
			monitor.done();
		}
		
		if (fMethodStubsButtons.isSelected(0)) {
			String main= "public static void main(String[] args) {}"; //$NON-NLS-1$
			newMethods.add(main);
		}		
		
		return (String[]) newMethods.toArray(new String[newMethods.size()]);
	}
	
}