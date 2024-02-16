/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.util;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.util.Resources;

import org.eclipse.jdt.internal.ui.JavaUIMessages;

/**
 * Helper class to check if a set of <code>IJavaElement</code> objects can be
 * modified by an operation.
 *
 * @since 	2.1
 */
public class ElementValidator {

	private ElementValidator() {
		// no instance
	}

	/**
	 * Checks if the given element is in sync with the underlying file system.
	 *
	 * @param element the element to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * element is not in sync
	 * @param title a dialog's title used to present a dialog to the user if the
	 * element is not in sync
	 * @return boolean <code>true</code> if the element is in sync with the file
	 * system. Otherwise <code>false</code> is returned
	 */
	public static boolean checkInSync(IAdaptable element, Shell parent,String title) {
		return checkInSync(new IAdaptable[] {element}, parent, title);
	}

	/**
	 * Checks if the given array of elements is in sync with the underlying file
	 * system.
	 *
	 * @param elements the array of elements to be checked
	 * @param parent a parent shell used to present a dialog to the user if
	 * one of the elements is not in sync
	 * @param title a dialog's title used to present a dialog to the user if
	 * one of the elements is not in sync
	 * @return boolean <code>true</code> if the all elements are in sync with
	 * the file system. Otherwise <code>false</code> is returned
	 */
	public static boolean checkInSync(IAdaptable[] elements, Shell parent, String title) {
		return checkInSync(getResources(elements), parent, title);
	}

	/**
	 * Checks if the given element is read-only and if so the methods tries
	 * to make the element writable by calling validate edit. If
	 * <code>validateEdit</code> was able to make the file writable the method
	 * additionally checks if the file has been changed by calling
	 * <code>validateEdit</code>.
	 *
	 * @param element the element to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * check fails
	 * @param title a dialog's title used to present a dialog to the user if the
	 * check fails
	 * @return boolean <code>true</code> if the element is writable and its
	 * content didn't change by calling <code>validateEdit</code>. Otherwise
	 * <code>false</code> is returned
	 *
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean checkValidateEdit(IJavaElement element, Shell parent, String title) {
		return checkValidateEdit(new IJavaElement[] {element}, parent, title);
	}

	/**
	 * Checks if the given elements are read-only and if so the methods tries to
	 * make the element writable by calling <code>validateEdit</code>. If
	 * <code>validateEdit</code> was able to make the file writable the method
	 * additionally checks if the file has been changed by calling
	 * <code>validateEdit</code>.
	 *
	 * @param elements the elements to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * check fails
	 * @param title a dialog's title used to present a dialog to the user if the
	 * check fails
	 * @return boolean <code>true</code> if all elements are writable and their
	 * content didn't change by calling <code>validateEdit</code>. Otherwise
	 * <code>false</code> is returned
	 *
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean checkValidateEdit(IJavaElement[] elements, Shell parent, String title) {
		return checkValidateEdit(getResources(elements), parent, title);
	}

	/**
	 * Checks a combination of <code>checkInSync</code> and
	 * <code>checkValidateEdit</code> depending of the value of
	 * <code>editor</code>. If <code>editor</code> is <code>true</code> only
	 * <code>checkValidateEdit</code> is performed since the editor does a in
	 * sync check on focus change. If <code>editor</code> is <code>false</code>
	 * both checks are performed.
	 *
	 * @param element the element to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * check fails
	 * @param title a dialog's title used to present a dialog to the user if the
	 * check fails
	 * @param editor specifies if we are in the editor
	 * @return boolean <code>true</code> if the element passed the checks.
	 * Otherwise <code>false</code> is returned
	 *
	 * @see #checkInSync(IAdaptable, Shell, String)
	 * @see #checkValidateEdit(IJavaElement, Shell, String)
	 */
	public static boolean check(IJavaElement element, Shell parent, String title, boolean editor) {
		return check(new IJavaElement[] {element}, parent, title, editor);
	}

	/**
	 * Checks a combination of <code>checkInSync</code> and
	 * <code>checkValidateEdit</code> depending of the value of
	 * <code>editor</code>. If <code>editor</code> is <code>true</code> only
	 * <code>checkValidateEdit</code> is performed since the editor does a in
	 * sync check on focus change. If <code>editor</code> is <code>false</code>
	 * both checks are performed.
	 *
	 * @param elements the elements to be checked
	 * @param parent a parent shell used to present a dialog to the user if the
	 * check fails
	 * @param title a dialog's title used to present a dialog to the user if the
	 * check fails
	 * @param editor specifies if we are in the editor
	 * @return boolean <code>true</code> if all elements pass the checks.
	 * Otherwise <code>false</code> is returned
	 *
	 * @see #checkInSync(IAdaptable[], Shell, String)
	 * @see #checkValidateEdit(IJavaElement[], Shell, String)
	 */
	public static boolean check(IJavaElement[] elements, Shell parent,String title, boolean editor) {
		IResource[] resources= getResources(elements);
		if (!editor && !checkInSync(resources, parent, title))
			return false;
		return checkValidateEdit(resources, parent, title);
	}

	private static boolean checkInSync(IResource[] resources, Shell parent, String title) {
		IStatus status= Resources.checkInSync(resources);
		if (!status.isOK()) {
			boolean autoRefresh= Platform.getPreferencesService().getBoolean(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, false, null);
			if (autoRefresh) {
				for (IResource resource : resources) {
					try {
						resource.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					} catch (CoreException e) {
						break;
					}
					status= Resources.checkInSync(resources);
				}
			}
		}
		if (status.isOK())
			return true;
		ErrorDialog.openError(parent, title,
			JavaUIMessages.ElementValidator_cannotPerform,
			status);
		return false;
	}

	private static boolean checkValidateEdit(IResource[] resources, Shell parent, String title) {
		IStatus status= Resources.makeCommittable(resources, parent);
		if (!status.isOK()) {
			ErrorDialog.openError(parent, title,
				JavaUIMessages.ElementValidator_cannotPerform,
				status);
			return false;
		}
		return true;
	}

	private static IResource[] getResources(IAdaptable[] elements) {
		Set<IResource> result= new HashSet<>();
		for (IAdaptable element : elements) {
			IResource resource= null;
			if (element instanceof IJavaElement) {
				IJavaElement je= (IJavaElement)element;
				ICompilationUnit cu= (ICompilationUnit)je.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (cu != null) {
					je= cu.getPrimary();
				}
				resource= je.getResource();
			} else {
				resource= element.getAdapter(IResource.class);
			}
			if (resource != null)
				result.add(resource);
		}
		return result.toArray(new IResource[result.size()]);
	}
}
