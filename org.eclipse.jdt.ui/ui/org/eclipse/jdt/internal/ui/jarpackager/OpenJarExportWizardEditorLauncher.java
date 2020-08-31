/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IEditorLauncher;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.jarpackager.IJarDescriptionReader;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * This editor launcher opens the JAR Export Wizard and initializes it with the selected JAR package
 * description (a *.jardesc file).
 *
 * @since 3.5
 */
public class OpenJarExportWizardEditorLauncher implements IEditorLauncher {

	/*
	 * @see org.eclipse.ui.IEditorLauncher#open(org.eclipse.core.runtime.IPath)
	 */
	@Override
	public void open(IPath filePath) {

		IJarDescriptionReader reader= null;
		JarPackageData jarPackage= null;
		try {
			File file= filePath.toFile();
			if (!file.isFile() || !file.getName().endsWith('.' + JarPackagerUtil.DESCRIPTION_EXTENSION)) {
				openErrorDialog(Messages.format(JarPackagerMessages.OpenJarPackageWizardDelegate_onlyJardesc, JarPackagerUtil.DESCRIPTION_EXTENSION));
				return;
			}

			jarPackage= new JarPackageData();
			try (InputStream is= new FileInputStream(file)){
				reader= jarPackage.createJarDescriptionReader(is);
				reader.read(jarPackage);
			} finally {
				if (reader != null)
					reader.close();
			}
		} catch (IOException | CoreException ex) {
			openErrorDialog(ex.getLocalizedMessage());
			return;
		}

		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		if (reader != null && !reader.getStatus().isOK())
			ErrorDialog.openError(parent, JarPackagerMessages.OpenJarPackageWizardDelegate_jarDescriptionReaderWarnings_title, null, reader.getStatus());
		JarPackageWizard wizard= new JarPackageWizard();
		wizard.init(PlatformUI.getWorkbench(), jarPackage);
		WizardDialog dialog= new WizardDialog(parent, wizard);
		dialog.create();
		dialog.open();
	}

	private void openErrorDialog(String errorDetail) {
		MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JarPackagerMessages.OpenJarPackageWizardDelegate_error_openJarPackager_title,
				JarPackagerMessages.OpenJarPackageWizardDelegate_error_openJarPackager_message + errorDetail);
	}
}
