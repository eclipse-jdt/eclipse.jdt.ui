/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class OpenSnippetWizardAction extends AbstractOpenWizardAction {

	public OpenSnippetWizardAction() {
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_SNIPPET_WIZARD_ACTION);
	}
	
	public OpenSnippetWizardAction(String label, Class[] acceptedTypes) {
		super(label, acceptedTypes, false);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_SNIPPET_WIZARD_ACTION);
	}
	
	protected Wizard createWizard() { 
		return new NewSnippetFileCreationWizard(); 
	}
	
	protected boolean shouldAcceptElement(Object obj) { 
		return !isInArchive(obj);
	}
}