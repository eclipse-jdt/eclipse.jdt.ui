/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardAction;

public class OpenInterfaceWizardAction extends AbstractOpenWizardAction {

	public OpenInterfaceWizardAction() {
	}
	
	public OpenInterfaceWizardAction(String label, Class[] acceptedTypes) {
		super(label, acceptedTypes, false);
	}
	
	protected Wizard createWizard() { 
		return new NewInterfaceCreationWizard(); 
	}
	
	protected boolean shouldAcceptElement(Object obj) { 
		return NewGroup.isOnBuildPath(obj) && !NewGroup.isInArchive(obj);
	}
}