/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 213638 [jar exporter] create ANT build file for current settings
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.io.File;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

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
		Runnable runnable= new Runnable() {
			public void run() {
				returnValue[0]= MessageDialog.openQuestion(parent, title, message);
			}
		};
		display.syncExec(runnable);
		
		return returnValue[0];
	}

}
