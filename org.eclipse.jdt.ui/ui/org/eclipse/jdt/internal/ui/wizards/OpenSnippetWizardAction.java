/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

public class OpenSnippetWizardAction extends AbstractOpenWizardAction {

	public OpenSnippetWizardAction() {
	}
	
	public OpenSnippetWizardAction(String label, Class[] acceptedTypes) {
		super(label, acceptedTypes, false);
	}
	
	protected Wizard createWizard() { 
		return new NewSnippetFileCreationWizard(); 
	}
	
	protected boolean shouldAcceptElement(Object obj) { 
		return !NewGroup.isInArchive(obj);
	}
}