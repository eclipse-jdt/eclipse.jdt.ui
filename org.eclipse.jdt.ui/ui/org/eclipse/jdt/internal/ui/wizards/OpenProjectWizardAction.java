/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

public class OpenProjectWizardAction extends AbstractOpenWizardAction {

	public OpenProjectWizardAction() {
	}
	
	public OpenProjectWizardAction(String label, Class[] acceptedTypes) {
		super(label, acceptedTypes, true);
	}
	
	protected Wizard createWizard() { 
		return new NewProjectCreationWizard(); 
	}	
	/*
	 * @see AbstractOpenWizardAction#showWorkspaceEmptyWizard()
	 */
	protected boolean checkWorkspaceNotEmpty() {
		return true;
	}

}