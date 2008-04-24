/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.IOException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jdt.ui.jarpackager.IJarDescriptionReader;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.xml.sax.SAXException;

/**
 * This action delegate opens the JAR Package Wizard and initializes
 * it with the selected JAR package description.
 */
public class OpenJarPackageWizardActionDelegate extends JarPackageActionDelegate {

	private IJarDescriptionReader fReader;

	/*
	 * @see IActionDelegate
	 */
	public void run(IAction action) {
		Shell parent= getShell();
		JarPackageData jarPackage= null;
		String errorDetail= null;
		try {
			jarPackage= readJarPackage(getDescriptionFile(getSelection()));			
		} catch (IOException ex) {
			errorDetail= ex.getLocalizedMessage();
			MessageDialog.openError(parent, JarPackagerMessages.OpenJarPackageWizardDelegate_error_openJarPackager_title, JarPackagerMessages.OpenJarPackageWizardDelegate_error_openJarPackager_message + errorDetail); 
			return;
		} catch (CoreException ex) {
			errorDetail= ex.getLocalizedMessage();
			MessageDialog.openError(parent, JarPackagerMessages.OpenJarPackageWizardDelegate_error_openJarPackager_title, JarPackagerMessages.OpenJarPackageWizardDelegate_error_openJarPackager_message + errorDetail); 
			return;
		} catch (SAXException ex) {
			errorDetail= JarPackagerMessages.OpenJarPackageWizardDelegate_badXmlFormat + ex.getLocalizedMessage(); 
			MessageDialog.openError(parent, JarPackagerMessages.OpenJarPackageWizardDelegate_error_openJarPackager_title, JarPackagerMessages.OpenJarPackageWizardDelegate_error_openJarPackager_message + errorDetail); 
			return;
		}

		if (fReader != null && !fReader.getStatus().isOK())
			ErrorDialog.openError(parent, JarPackagerMessages.OpenJarPackageWizardDelegate_jarDescriptionReaderWarnings_title, null, fReader.getStatus()); 
		JarPackageWizard wizard= new JarPackageWizard();
		wizard.init(getWorkbench(), jarPackage);
		WizardDialog dialog= new WizardDialog(parent, wizard);
		dialog.create();
		dialog.open();
	}
	
	/**
	 * Reads the JAR package spec from file.
	 * @param description 
	 * @return the JAR package spec
	 * @throws CoreException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	private JarPackageData readJarPackage(IFile description) throws CoreException, IOException, SAXException {
		Assert.isLegal(description.isAccessible());
		Assert.isNotNull(description.getFileExtension());
		Assert.isLegal(description.getFileExtension().equals(JarPackagerUtil.DESCRIPTION_EXTENSION));
		JarPackageData jarPackage= new JarPackageData();
		try {
			fReader= jarPackage.createJarDescriptionReader(description.getContents());
			fReader.read(jarPackage);
		} finally {
			if (fReader != null)
				fReader.close();
		}
		return jarPackage;
	}
}
