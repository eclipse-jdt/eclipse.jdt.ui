/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardWorkbenchAction;

public class OpenClassWizardAction extends AbstractOpenWizardWorkbenchAction {

	public OpenClassWizardAction() {
	}
	
	public OpenClassWizardAction(IWorkbench workbench, String label, Class[] acceptedTypes) {
		super(workbench, label, acceptedTypes, false);
	}
	
	protected Wizard createWizard() { 
		return new NewClassCreationWizard(); 
	}
	
	protected boolean shouldAcceptElement(Object obj) { 
		return NewGroup.isOnBuildPath(obj) && !NewGroup.isInArchive(obj);
	}
}