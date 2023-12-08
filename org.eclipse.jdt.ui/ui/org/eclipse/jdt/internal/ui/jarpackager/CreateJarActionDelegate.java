/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.jarpackager.IJarDescriptionReader;
import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class CreateJarActionDelegate extends JarPackageActionDelegate {

	/*
	 * @see IActionDelegate
	 */
	@Override
	public void run(IAction action) {
		IFile[] descriptions= getDescriptionFiles(getSelection());
		MultiStatus mergedStatus;
		int length= descriptions.length;
		if (length < 1)
			return;

		// Create read multi status
		String message;
		if (length > 1)
			message= JarPackagerMessages.JarFileExportOperation_creationOfSomeJARsFailed;
		else
			message= JarPackagerMessages.JarFileExportOperation_jarCreationFailed;
		MultiStatus readStatus= new MultiStatus(JavaPlugin.getPluginId(), 0, message, null);
		JarPackageData[] jarPackages= readJarPackages(descriptions, readStatus);
		if (jarPackages.length > 0) {
			IStatus status= export(jarPackages);
			if (status == null)
				return; // cancelled
			if (readStatus.getSeverity() == IStatus.ERROR)
				message= readStatus.getMessage();
			else
				message= status.getMessage();
			// Create new status because we want another message - no API to set message
			mergedStatus= new MultiStatus(JavaPlugin.getPluginId(), status.getCode(), readStatus.getChildren(), message, null);
			mergedStatus.merge(status);
		} else
			mergedStatus= readStatus;

		if (!mergedStatus.isOK())
			ErrorDialog.openError(getShell(), JarPackagerMessages.CreateJarActionDelegate_jarExport_title, null, mergedStatus);
	}

	private JarPackageData[] readJarPackages(IFile[] descriptions, MultiStatus readStatus) {
		List<JarPackageData> jarPackagesList= new ArrayList<>(descriptions.length);
		for (IFile description : descriptions) {
			JarPackageData jarPackage= readJarPackage(description, readStatus);
			if (jarPackage != null)
				jarPackagesList.add(jarPackage);
		}
		return jarPackagesList.toArray(new JarPackageData[jarPackagesList.size()]);
	}

	private IStatus export(JarPackageData[] jarPackages) {
		Shell shell= getShell();
		IJarExportRunnable op= jarPackages[0].createJarExportRunnable(jarPackages, shell);
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, true, op);
			//PlatformUI.getWorkbench().getProgressService().run(false, true, op); // see bug 118152
		} catch (InvocationTargetException ex) {
			if (ex.getTargetException() != null) {
				ExceptionHandler.handle(ex, shell, JarPackagerMessages.CreateJarActionDelegate_jarExportError_title, JarPackagerMessages.CreateJarActionDelegate_jarExportError_message);
				return null;
			}
		} catch (InterruptedException e) {
			// do nothing on cancel
			return null;
		}
		return op.getStatus();
	}

	/**
	 * Reads the JAR package spec from file.
	 * @param description the description file
	 * @param readStatus status
	 * @return returns the read the JAR package spec
	 */
	protected JarPackageData readJarPackage(IFile description, MultiStatus readStatus) {
		Assert.isLegal(description.isAccessible());
		Assert.isNotNull(description.getFileExtension());
		Assert.isLegal(JarPackagerUtil.DESCRIPTION_EXTENSION.equals(description.getFileExtension()));
		JarPackageData jarPackage= new JarPackageData();
		IJarDescriptionReader reader= null;
		try (InputStream contents= description.getContents()) {
			 reader= jarPackage.createJarDescriptionReader(contents);
			// Do not save - only generate JAR
			reader.read(jarPackage);
			jarPackage.setSaveManifest(false);
			jarPackage.setSaveDescription(false);
		} catch (IOException close) {
			throw new RuntimeException(close);
		} catch (CoreException ex) {
				String message= Messages.format(JarPackagerMessages.JarFileExportOperation_errorReadingFile, new Object[] {BasicElementLabels.getPathLabel(description.getFullPath(), false), ex.getStatus().getMessage()});
				addToStatus(readStatus, message, ex);
				return null;
		} finally {
			if (reader != null)
				// AddWarnings
				readStatus.addAll(reader.getStatus());
			try {
				if (reader != null)
					reader.close();
			}
			catch (CoreException ex) {
				String message= Messages.format(JarPackagerMessages.JarFileExportOperation_errorClosingJarPackageDescriptionReader, BasicElementLabels.getPathLabel(description.getFullPath(), false));
				addToStatus(readStatus, message, ex);
			}
		}
		return jarPackage;
	}

	private void addToStatus(MultiStatus multiStatus, String defaultMessage, CoreException ex) {
		IStatus status= ex.getStatus();
		String message= ex.getLocalizedMessage();
		if (message == null || message.length() < 1)
			status= new Status(status.getSeverity(), status.getPlugin(), status.getCode(), defaultMessage, ex);
		multiStatus.add(status);
	}
}
