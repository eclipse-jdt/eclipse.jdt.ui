/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentDialog;

/**
 * Access point for dialogs dealing with the configuration of settings related
 * to the Java build path. 
 * This class provides static methods for:
 * <ul>
 *  <li> User interface for the configuration of source attachments</li>
 *  <li> User interface for the configuration of Javadoc location</li>
 * </ul>
 * <p>
 * This class provides static methods; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
 */
public final class BuildPathDialogs {

	private  BuildPathDialogs() {
	}
	
	/**
	 * Shows the UI for configuring source attachments. <code>null</code> is returned
	 * if the user cancels the dialog. The dialog does not apply any changes, this is the
	 * clients task.
	 * @param shell The parent shell for the dialog
	 * @param entry The entry to edit. The kind of the classpath entry must be either
	 * <code>IClasspathEntry.CPE_LIBRARY</code> or <code>IClasspathEntry.CPE_VARIABLE</code>.
	 * @return Returns the resulting classpath entry containing a potentially modified source attachment path and
	 * source attachment root. The resulting entry can be used to replace the original entry on the classpath.
	 * Note that the dialog does not make any changes on the passed entry not on the classpath that
	 * contains it.
	 */
	public static IClasspathEntry configureSourceAttachment(Shell shell, IClasspathEntry entry) {
		Assert.isNotNull(entry);
		Assert.isTrue(entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY || entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE);
		
		SourceAttachmentDialog dialog=  new SourceAttachmentDialog(shell, entry);
		if (dialog.open() == Window.OK) {
			return dialog.getResult();
		}
		return null;
	}
	

}
