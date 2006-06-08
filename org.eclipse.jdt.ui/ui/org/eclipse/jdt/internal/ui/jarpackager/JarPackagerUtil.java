/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.zip.CRC32;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Utility methods for JAR Import/Export.
 */
public final class JarPackagerUtil {

	static final String JAR_EXTENSION= "jar"; //$NON-NLS-1$
	static final String DESCRIPTION_EXTENSION= "jardesc"; //$NON-NLS-1$

	private static final String META_INF_ENTRY= "META-INF"; //$NON-NLS-1$
	private static final String REFACTORINGS_ENTRY= META_INF_ENTRY + "/REFACTORINGS.XML"; //$NON-NLS-1$

	private JarPackagerUtil() {
		// Do nothing
	}

	public static boolean askToCreateDirectory(final Shell parent, File directory) {
		if (parent == null)
			return false;
		return queryDialog(parent, JarPackagerMessages.JarPackage_confirmCreate_title, Messages.format(JarPackagerMessages.JarPackage_confirmCreate_message, directory.toString())); 
	}

	/**
	 * Returns the name of the refactorings zip entry.
	 * 
	 * @return the name of the refactorings zip entry
	 * 
	 * @since 3.2
	 */
	public static String getRefactoringsEntry() {
		return REFACTORINGS_ENTRY;
	}

	/**
	 * Returns the name of the deprecations zip entry for the specified file.
	 * 
	 * @param name
	 *            the name of the file
	 * @return the name of the deprecations zip entry
	 * 
	 * @since 3.2
	 */
	public static String getDeprecationEntry(final String name) {
		return META_INF_ENTRY + "/" + name; //$NON-NLS-1$
	}

	/**
	 * Returns the name of the meta entry.
	 * 
	 * @return the name of the meta entry
	 * 
	 * @since 3.2
	 */
	public static String getMetaEntry() {
		return META_INF_ENTRY;
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
				selectedResources.add(((IJavaElement)element).getResource());
			}
			else if (element instanceof IResource)
				selectedResources.add(element);
		}
		return selectedResources;
	}

	public static boolean askForOverwritePermission(final Shell parent, String filePath) {
		if (parent == null)
			return false;
		return queryDialog(parent, JarPackagerMessages.JarPackage_confirmReplace_title, Messages.format(JarPackagerMessages.JarPackage_confirmReplace_message, filePath)); 
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
		return new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IJavaStatusConstants.INTERNAL_ERROR, message, ex));
	}

	/**
	 * Tells whether the specified manifest main class is valid.
	 * 
	 * @return <code>true</code> if a main class is specified and valid
	 */
	public static boolean isMainClassValid(JarPackageData data, IRunnableContext context) {
		if (data == null)
			return false;
		
		IType mainClass= data.getManifestMainClass();
		if (mainClass == null)
			// no main class specified
			return true;

		try {
			// Check if main method is in scope
			IFile file= (IFile)mainClass.getResource();
			if (file == null || !contains(asResources(data.getElements()), file))
				return false;

			// Test if it has a main method
			return JavaModelUtil.hasMainMethod(mainClass);
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}
		return false;
	}
	
	static boolean contains(List resources, IFile file) {
		if (resources == null || file == null)
			return false;
			
		if (resources.contains(file))
			return true;
		
		Iterator iter= resources.iterator();
		while (iter.hasNext()) {
			IResource resource= (IResource)iter.next();
			if (resource != null && resource.getType() != IResource.FILE) {
				List children= null;
				try {
					children= Arrays.asList(((IContainer)resource).members());
				} catch (CoreException ex) {
					// ignore this folder
					continue;
				}
				if (children != null && contains(children, file))
					return true;
			}
		}
		return false;
	}

	/**
	 * Calculates the crc and size of the resource and updates the entry.
	 * 
	 * @param entry
	 *            the jar entry to update
	 * @param stream
	 *            the input stream
	 * @param buffer
	 *            a shared buffer to store temporary data
	 * 
	 * @throws IOException
	 *             if an input/output error occurs
	 */
	public static void calculateCrcAndSize(final JarEntry entry, final InputStream stream, final byte[] buffer) throws IOException {
		int size= 0;
		final CRC32 crc= new CRC32();
		int count;
		try {
			while ((count= stream.read(buffer, 0, buffer.length)) != -1) {
				crc.update(buffer, 0, count);
				size+= count;
			}
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException exception) {
					// Do nothing
				}
			}
		}
		entry.setSize(size);
		entry.setCrc(crc.getValue());
	}
}