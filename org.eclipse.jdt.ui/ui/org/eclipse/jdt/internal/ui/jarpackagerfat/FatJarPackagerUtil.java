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
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 213638 [jar exporter] create ANT build file for current settings
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 219530 [jar application] add Jar-in-Jar ClassLoader option
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.io.File;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * Utility methods for Runnable JAR Import/Export.
 */
public final class FatJarPackagerUtil {

	static final String ANTSCRIPT_EXTENSION= "xml"; //$NON-NLS-1$

	private FatJarPackagerUtil() {
		// Do nothing
	}

	public static boolean askToCreateAntScriptDirectory(final Shell parent, File directory) {
		if (parent == null)
			return false;

		return queryDialog(parent, FatJarPackagerMessages.FatJarPackage_confirmCreate_title, Messages.format(FatJarPackagerMessages.FatJarPackageAntScript_confirmCreate_message, BasicElementLabels.getPathLabel(directory)));
	}

	private static boolean queryDialog(final Shell parent, final String title, final String message) {
		Display display= parent.getDisplay();
		if (display == null || display.isDisposed())
			return false;

		final boolean[] returnValue= new boolean[1];
		Runnable runnable= () -> returnValue[0]= MessageDialog.openQuestion(parent, title, message);
		display.syncExec(runnable);

		return returnValue[0];
	}

	/**
	 * Increments [nr] for files in the format "[name][_nr][.ext]".<br>
	 * Increment of "[name][.ext]" is "[name]_2[.ext]<br>
	 * Increment of "[name]_2[.ext]" is "[name]_3[.ext]<br>
	 * [.ext] might be empty<br>
	 *
	 * @param fileName the file name to increment
	 * @return incremented filename
	 */
	public static String nextNumberedFileName(String fileName) {
		String name;
		String number;
		String ext;
		if (fileName.matches(".*[_]\\d+[.][^.]*")) { //$NON-NLS-1$
			name= fileName.replaceFirst("(.*)[_](\\d+)([.][^.]*)", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
			number= fileName.replaceFirst("(.*)[_](\\d+)([.][^.]*)", "$2"); //$NON-NLS-1$ //$NON-NLS-2$
			ext= fileName.replaceFirst("(.*)[_](\\d+)([.][^.]*)", "$3"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (fileName.matches(".*[.][^.]*")) { //$NON-NLS-1$
			name= fileName.replaceFirst("(.*)([.][^.]*)", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
			number= "1"; //$NON-NLS-1$
			ext= fileName.replaceFirst("(.*)([.][^.]*)", "$2"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			name= fileName;
			number= "1"; //$NON-NLS-1$
			ext= ""; //$NON-NLS-1$
		}
		fileName= name + "_" + (Integer.parseInt(number) + 1) + ext; //$NON-NLS-1$
		return fileName;
	}

}
