/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved.   This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
 
Contributors:
	Daniel Megert - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.jarpackager;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.JavaStatusConstants;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Utility for the Jar packager
 */
public class JarPackagerUtil {

	// Constants
	static final String JAR_EXTENSION= "jar"; //$NON-NLS-1$
	static final String DESCRIPTION_EXTENSION= "jardesc"; //$NON-NLS-1$

	public static boolean askToCreateDirectory(final Shell parent) {
		if (parent == null)
			return false;
		return queryDialog(parent, JarPackagerMessages.getString("JarPackage.confirmCreate.title"), JarPackagerMessages.getString("JarPackage.confirmCreate.message")); //$NON-NLS-2$ //$NON-NLS-1$
	}

	/**
	 * Computes and returns the elements as resources.
	 * The underlying resource is used for Java elements.
	 * 
	 * @return a List with the selected resources
	 */
	public static List asResources(Object[] fSelectedElements) {
		if (fSelectedElements == null)
			return null;
		List selectedResources= new ArrayList(fSelectedElements.length);
		for (int i= 0; i < fSelectedElements.length; i++) {
			Object element= fSelectedElements[i];
			if (element instanceof IJavaElement) {
				try {
					selectedResources.add(((IJavaElement)element).getUnderlyingResource());
				} catch (JavaModelException ex) {
					ExceptionHandler.log(ex, "Failed to get underlying resource of java element"); //$NON-NLS-1$
				}
			}
			else if (element instanceof IResource)
				selectedResources.add(element);
		}
		return selectedResources;
	}

	public static boolean askForOverwritePermission(final Shell parent, String filePath) {
		if (parent == null)
			return false;
		return queryDialog(parent, JarPackagerMessages.getString("JarPackage.confirmReplace.title"), JarPackagerMessages.getFormattedString("JarPackage.confirmReplace.message", filePath)); //$NON-NLS-2$ //$NON-NLS-1$
	}

	/**
	 * Checks if the manifest file can be overwritten.
	 * If the JAR package setting does not allow to overwrite the manifest
	 * then a dialog will ask the user again.
	 * 
	 * @param	parent	the parent for the dialog,
	 * 			or <code>null</code> if no dialog should be presented
	 * @return	<code>true</code> if it is OK to create the JAR
	 */
	static boolean canOverwrite(Shell parent, IFile file) {
		if (file.isAccessible())
			return true;
		return askForOverwritePermission(parent, file.getFullPath().toString());
	}

	/**
	 * Gets the name of the manifest's main class
	 * 
	 * @return a string with the name
	 */
	static String getMainClassName(JarPackageData jarPackage) {
		if (jarPackage.getManifestMainClass() == null)
			return ""; //$NON-NLS-1$
		else
			return jarPackage.getManifestMainClass().getFullyQualifiedName();
	}


	private static boolean queryDialog(final Shell parent, final String title, final String message) {
		Display display= parent.getDisplay();
		if (display == null || display.isDisposed())
			return false;
		final boolean[] returnValue= new boolean[1];
		Runnable runnable= new Runnable() {
			public void run() {
				returnValue[0]= MessageDialog.openQuestion(parent, title, message);
			}
		};
		display.syncExec(runnable);	
		return returnValue[0];
	}
	
	/**
	 * Creates a <code>CoreException</code> with the given parameters.
	 * 
	 * @param	message	a string with the message
	 * @param	ex		the exception to be wrapped or <code>null</code> if none
	 * @return a CoreException
	 */
	public static CoreException createCoreException(String message, Exception ex) {
		if (message == null)
			message= ""; //$NON-NLS-1$
		return new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, JavaStatusConstants.INTERNAL_ERROR, message, ex));
	}
}
