/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.IOException;import org.eclipse.core.runtime.CoreException;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.action.IAction;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.wizard.WizardDialog;import org.xml.sax.SAXException;import org.eclipse.jdt.internal.ui.dialogs.ProblemDialog;

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
		JarPackage jarPackage= null;
		String errorDetail= null;
		try {
			jarPackage= readJarPackage(getDescriptionFile(getSelection()));			
		} catch (IOException ex) {
			errorDetail= ex.getMessage();
		} catch (CoreException ex) {
			errorDetail= ex.getMessage();
		} catch (SAXException ex) {
			errorDetail= "Bad XML format: " + ex.getMessage();
		}
		// Handle exceptions
		if (jarPackage == null) {
			MessageDialog.openError(parent, "Open JAR Packager Error", "Reading JAR package description from file failed.\n" + errorDetail);
			return;
		}
		if (jarPackage.logWarnings() && getReader() != null && !getReader().getWarnings().isOK())
			ProblemDialog.open(parent, "JAR Description Reader Warnings", null, getReader().getWarnings());
		JarPackageWizard wizard= new JarPackageWizard();
		wizard.init(getWorkbench(), jarPackage);
		WizardDialog dialog= new WizardDialog(parent, wizard);
		dialog.create();
		dialog.getShell().setSize(SIZING_WIZARD_WIDTH, SIZING_WIZARD_HEIGHT);
		dialog.open();
	}
}