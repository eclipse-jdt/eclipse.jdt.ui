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

import java.net.URL;

import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ClasspathContainerWizard;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.EditVariableEntryDialog;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.JavadocLocationDialog;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.NewVariableEntryDialog;
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
 * @since 3.0
 */
public final class BuildPathDialogAccess {

	private BuildPathDialogAccess() {
		// do not instantiate
	}
	
	/**
	 * Shows the UI for configuring source attachments. <code>null</code> is returned
	 * if the user cancels the dialog. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog
	 * @param initialEntry The entry to edit. The kind of the classpath entry must be either
	 * <code>IClasspathEntry.CPE_LIBRARY</code> or <code>IClasspathEntry.CPE_VARIABLE</code>.
	 * @return Returns the resulting classpath entry containing a potentially modified source attachment path and
	 * source attachment root. The resulting entry can be used to replace the original entry on the classpath.
	 * Note that the dialog does not make any changes on the passed entry not on the classpath that
	 * contains it.
	 */
	public static IClasspathEntry configureSourceAttachment(Shell shell, IClasspathEntry initialEntry) {
		Assert.isNotNull(initialEntry);
		Assert.isTrue(initialEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY || initialEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE);
		
		SourceAttachmentDialog dialog=  new SourceAttachmentDialog(shell, initialEntry);
		if (dialog.open() == Window.OK) {
			return dialog.getResult();
		}
		return null;
	}
		
	/**
	 * Shows the UI for configuring a javadoc location. <code>null</code> is returned
	 * if the user cancels the dialog. If OK is pressed, an array with the configured URL is
	 * returned. Note that the configured URL can be <code>null</code> when the user
	 * wishes to have no URL location specified. The dialog does not apply any changes.
	 * Use {@link org.eclipse.jdt.ui.JavaUI} to access and configure
	 * Javadoc locations.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param libraryName Name of of the library to which configured javadoc location belongs.
	 * @param initialURL The initial URL or <code>null</code>.
	 * @return Returns an array of size 1 that contains the resulting javadoc location or
	 * <code>null</code> if the dialog has been cancelled. Note that the configured URL can be <code>null</code> when the user
	 * wishes to have no URL location specified.
	 */
	public static URL[] configureJavadocLocation(Shell shell, String libraryName, URL initialURL) {
		Assert.isNotNull(libraryName);
		
		JavadocLocationDialog dialog=  new JavadocLocationDialog(shell, libraryName, initialURL);
		if (dialog.open() == Window.OK) {
			return new URL[] { dialog.getResult() };
		}
		return null;
	}
	
	/**
	 * Shows the UI for configuring a variable classpath entry. See {@link IClasspathEntry#CPE_VARIABLE} for
	 * details about variable classpath entries.
	 * The dialog returns the configured classpath entry path or <code>null</code> if the dialog has
	 * been cancelled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param initialEntryPath The initial variable classpath variable path or <code>null</code> to use
	 * an empty path. 
	 * @param existingPaths An array of paths that are already on the classpath and therefore should not be
	 * selected again.
	 * @return Returns the configures classpath entry path or <code>null</code> if the dialog has
	 * been cancelled.
	 */
	public static IPath configureVariableEntry(Shell shell, IPath initialEntryPath, IPath[] existingPaths) {
		EditVariableEntryDialog dialog= new EditVariableEntryDialog(shell, initialEntryPath, existingPaths);
		if (dialog.open() == Window.OK) {
			return dialog.getPath();
		}
		return null;
	}
	
	/**
	 * Shows the UI for selecting new variable classpath entries. See {@link IClasspathEntry#CPE_VARIABLE} for
	 * details about variable classpath entries.
	 * The dialog returns an array of the selected variable entries or <code>null</code> if the dialog has
	 * been cancelled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param existingPaths An array of paths that are already on the classpath and therefore should not be
	 * selected again.
	 * @return Returns an array of the selected variable entries or <code>null</code> if the dialog has
	 * been cancelled.
	 */
	public static IPath[] chooseVariableEntries(Shell shell, IPath[] existingPaths) {
		NewVariableEntryDialog dialog= new NewVariableEntryDialog(shell);
		if (dialog.open() == Window.OK) {
			return dialog.getResult();
		}
		return null;
	}
	
	/**
	 * Shows the UI to configure a classpath container classpath entry. See {@link IClasspathEntry#CPE_CONTAINER} for
	 * details about variable classpath entries.
	 * The dialog returns the configured classpath entry or <code>null</code> if the dialog has
	 * been cancelled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param initialEntry The initial classpath container entry 
	 * @param project The project the entry belongs to. The project does not have to exist. 
	 * Project can be <code>null</code>.
	 * @param currentClasspath The class path entries currently selected to be set as the projects classpath. This can also
	 * include the entry to be edited. The dialog uses these entries as information only; The user still can make changes after the
	 * the classpath container dialog has been closed or decide to cancel the operation. See {@link IClasspathContainerPageExtension} for
	 * more information.
	 * @return Returns the configured classpath container entry or <code>null</code> if the dialog has
	 * been cancelled by the user
	 */
	public static IClasspathEntry configureContainerEntry(Shell shell, IClasspathEntry initialEntry, IJavaProject project, IClasspathEntry[] currentClasspath) {
		Assert.isNotNull(initialEntry);
		
		ClasspathContainerWizard wizard= new ClasspathContainerWizard(initialEntry, project, currentClasspath);
		if (ClasspathContainerWizard.openWizard(shell, wizard) == Window.OK) {
			IClasspathEntry[] created= wizard.getNewEntries();
			if (created != null && created.length == 1) {
				return created[0];
			}
		}
		return null;
	}
	
	/**
	 * Shows the UI to choose new classpath container classpath entries. See {@link IClasspathEntry#CPE_CONTAINER} for
	 * details about variable classpath entries.
	 * The dialog returns the selected classpath entries or <code>null</code> if the dialog has
	 * been cancelled. The dialog does not apply any changes.
	 * 
	 * @param shell The parent shell for the dialog.
	 * @param project The project the entry belongs to. The project does not have to exist. 
	 * Project can be <code>null</code>.
	 * @param currentClasspath The class path entries currently selected to be set as the projects classpath. This can also
	 * include the entry to be edited. The dialog uses these entries as information only; The user still can make changes after the
	 * the classpath container dialog has been closed or decide to cancel the operation. See {@link IClasspathContainerPageExtension} for
	 * more information.
	 * @return Returns the selected classpath container entry or <code>null</code> if the dialog has
	 * been cancelled by the user
	 */
	public static IClasspathEntry[] chooseContainerEntries(Shell shell, IJavaProject project, IClasspathEntry[] currentClasspath) {
		ClasspathContainerWizard wizard= new ClasspathContainerWizard((IClasspathEntry) null, project, currentClasspath);
		if (ClasspathContainerWizard.openWizard(shell, wizard) == Window.OK) {
			return wizard.getNewEntries();
		}
		return null;
	}
	
}
