/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardWorkbenchAction;

public class OpenProjectWizardAction extends AbstractOpenWizardWorkbenchAction {

	public OpenProjectWizardAction() {
	}
	
	public OpenProjectWizardAction(IWorkbench workbench, String label, Class[] acceptedTypes) {
		super(workbench, label, acceptedTypes, true);
	}
	
	protected Wizard createWizard() { 
		return new NewProjectCreationWizard(); 
	}	
}