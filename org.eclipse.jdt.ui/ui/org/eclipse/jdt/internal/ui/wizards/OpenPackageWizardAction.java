/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardWorkbenchAction;


public class OpenPackageWizardAction extends AbstractOpenWizardWorkbenchAction {
	public OpenPackageWizardAction() {
	}
	
	public OpenPackageWizardAction(IWorkbench workbench, String label, Class[] acceptedTypes) {
		super(workbench, label, acceptedTypes, false);
	}
	
	protected Wizard createWizard() { 
		return new NewPackageCreationWizard();
	}
	
	protected boolean shouldAcceptElement(Object obj) { 
		return NewGroup.isOnBuildPath(obj) && !NewGroup.isInArchive(obj);
	}
}

