/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

public class OpenPackageWizardAction extends AbstractOpenWizardAction {
	
	public OpenPackageWizardAction() {
	}
	
	public OpenPackageWizardAction(String label, Class[] acceptedTypes) {
		super(label, acceptedTypes, false);
	}
	
	protected Wizard createWizard() { 
		return new NewPackageCreationWizard();
	}
	
	protected boolean shouldAcceptElement(Object obj) { 
		return isOnBuildPath(obj) && !isInArchive(obj);
	}
}

