/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.dialogs.ListDialog;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

/**
 * Helper class to check if a set of <tt>IJavaElement</tt> objects can be
 * modified by an operation.
 * 
 * @since 	2.1
 */
public class ElementValidator {

	private ElementValidator() {
		// no instance
	}

	public static boolean checkInSync(IAdaptable element, Shell parent,String title) {
		return checkInSync(new IAdaptable[] {element}, parent, title);
	}
	
	public static boolean checkInSync(IAdaptable[] elements, Shell parent, String title) {
		return checkInSync(getResources(elements), parent, title);
	}
	
	public static boolean checkValidateEdit(IJavaElement element, Shell parent, String title) {
		return checkValidateEdit(new IJavaElement[] {element}, parent, title);
	}
	
	public static boolean checkValidateEdit(IJavaElement[] elements, Shell parent, String title) {
		return checkValidateEdit(getResources(elements), parent, title);
	}
	
	public static boolean check(IJavaElement element, Shell parent, String title, boolean editor) {
		return check(new IJavaElement[] {element}, parent, title, editor);
	}
	
	public static boolean check(IJavaElement[] elements, Shell parent,String title, boolean editor) {
		IResource[] resources= getResources(elements);
		if (!editor && !checkInSync(resources, parent, title))
			return false;
		return checkValidateEdit(resources, parent, title);
	}

	private static boolean checkInSync(IResource[] resources, Shell parent, String title) {
		List unsynchronized= new ArrayList();
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			if (!resource.isSynchronized(IResource.DEPTH_INFINITE)) {
				unsynchronized.add(resource);
			}			
		}
		if (!unsynchronized.isEmpty()) {
			openDoesNotExist(parent, title, unsynchronized);
			return false;
		}
		return true;
	}

	private static boolean checkValidateEdit(IResource[] resources, Shell parent, String title) {
		List readOnlyFiles= new ArrayList();
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			if (resource.getType() == IResource.FILE && resource.isReadOnly())	
				readOnlyFiles.add(resource);
		}
		
		Map oldTimeStamps= createModificationStampMap(resources);
		IStatus status= ResourcesPlugin.getWorkspace().validateEdit(
			(IFile[]) readOnlyFiles.toArray(new IFile[readOnlyFiles.size()]), parent);
		if (!status.isOK()) {
			ErrorDialog.openError(parent, title, 
				JavaUIMessages.getString("ElementValidator.cannotPerform"), //$NON-NLS-1$
				status);
			return false;			
		}
		MultiStatus modified= new MultiStatus(
			JavaPlugin.getPluginId(),
			IJavaStatusConstants.VALIDATE_EDIT_CHANGED_CONTENT,
			JavaUIMessages.getString("ElementValidator.modifiedResources"), null); //$NON-NLS-1$
		Map newTimeStamps= createModificationStampMap(resources);
		for (Iterator iter= oldTimeStamps.keySet().iterator(); iter.hasNext();) {
			IFile file= (IFile) iter.next();
			if (!oldTimeStamps.get(file).equals(newTimeStamps.get(file)))
				modified.merge(JavaUIStatus.createError(
					IJavaStatusConstants.VALIDATE_EDIT_CHANGED_CONTENT, 
					JavaUIMessages.getFormattedString("ElementValidator.fileModified", file.getFullPath().toString()), //$NON-NLS-1$ 
					null));
		}
		if (!status.isOK()) {
			ErrorDialog.openError(parent, title, 
				JavaUIMessages.getString("ElementValidator.cannotPerform"), //$NON-NLS-1$
				status);
			return false;			
		}
		return true;
	}
	
	private static IResource[] getResources(IAdaptable[] elements) {
		Set result= new HashSet();
		for (int i= 0; i < elements.length; i++) {
			IAdaptable element= elements[i];
			IResource resource= null;
			if (element instanceof IJavaElement) {
				IJavaElement je= (IJavaElement)element;
				ICompilationUnit cu= (ICompilationUnit)je.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (cu != null) {
					if (cu.isWorkingCopy())
						cu= (ICompilationUnit)cu.getOriginalElement();
					je= cu;
				}
				resource= je.getResource();
			} else {
				resource= (IResource)element.getAdapter(IResource.class);
			}
			if (resource != null)
				result.add(resource);
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}
	
	private static Map createModificationStampMap(IResource[] resources){
		Map map= new HashMap();
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			map.put(resource, new Long(resource.getModificationStamp()));
		}
		return map;
	}

	private static void openDoesNotExist(Shell parent, String title, List resources) {
		ListDialog dialog= new ListDialog(parent);
		dialog.setLabelProvider(new JavaElementLabelProvider());
		dialog.setInput(resources);
		dialog.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return ((List)inputElement).toArray();
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		dialog.setTitle(title);
		dialog.setMessage(resources.size() == 1 
			? JavaUIMessages.getString("ElementValidator.outOfSync.single") //$NON-NLS-1$
			: JavaUIMessages.getString("ElementValidator.outOfSync.multiple")); //$NON-NLS-1$
		dialog.open();
	}
	
}
