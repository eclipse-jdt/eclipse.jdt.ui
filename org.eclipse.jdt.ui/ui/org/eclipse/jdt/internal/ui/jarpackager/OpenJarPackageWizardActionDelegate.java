/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * This action delegate opens the JAR Package Wizard and initializes
 * it with the selected JAR package description.
 */
public class OpenJarPackageWizardActionDelegate extends JarPackageActionDelegate {

	private static final int SIZING_WIZARD_WIDTH= 470;
	private static final int SIZING_WIZARD_HEIGHT= 550;

	/*
	 * @see IActionDelegate
	 */
	public void run(IAction action) {
		Shell parent= getShell();
		JarPackage jarPackage= readJarPackage(getDescriptionFile(getSelection()));
		JarPackageWizard wizard= new JarPackageWizard();
		wizard.init(getWorkbench(), jarPackage);
		WizardDialog dialog= new WizardDialog(parent, wizard);
		dialog.create();
		dialog.getShell().setSize(SIZING_WIZARD_WIDTH, SIZING_WIZARD_HEIGHT);
		dialog.open();
	}
}