/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.refactoring.descriptors;

import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;

public class JavaRefactoringDescriptorUtil {

	private static final String LOWER_CASE_FALSE= Boolean.FALSE.toString().toLowerCase();
	private static final String LOWER_CASE_TRUE= LOWER_CASE_FALSE;

	/**
	 * Converts the specified element to an input handle.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param element
	 *            the element
	 * @return a corresponding input handle
	 */
	public static String elementToHandle(final String project, final IJavaElement element) {
		final String handle= element.getHandleIdentifier();
		if (project != null && !(element instanceof IJavaProject)) {
			final String id= element.getJavaProject().getHandleIdentifier();
			return handle.substring(id.length());
		}
		return handle;
	}

	/**
	 * Converts the specified resource to an input handle.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param resource
	 *            the resource
	 * 
	 * @return the input handle
	 */
	public static String resourceToHandle(final String project, final IResource resource) {
		if (project != null && !"".equals(project)) //$NON-NLS-1$
			return resource.getProjectRelativePath().toPortableString();
		return resource.getFullPath().toPortableString();
	}

	/**
	 * Converts an input handle back to the corresponding java element.
	 * 
	 * @param owner
	 *            the working copy owner
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param handle
	 *            the input handle
	 * @param check
	 *            <code>true</code> to check for existence of the element,
	 *            <code>false</code> otherwise
	 * @return the corresponding java element, or <code>null</code> if no such
	 *         element exists
	 */
	public static IJavaElement handleToElement(final WorkingCopyOwner owner, final String project, final String handle, final boolean check) {
		IJavaElement element= null;
		if (owner != null)
			element= JavaCore.create(handle, owner);
		else
			element= JavaCore.create(handle);
		if (element == null && project != null) {
			final IJavaProject javaProject= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProject(project);
			final String identifier= javaProject.getHandleIdentifier();
			if (owner != null)
				element= JavaCore.create(identifier + handle, owner);
			else
				element= JavaCore.create(identifier + handle);
		}
		if (check && element instanceof IMethod) {
			final IMethod method= (IMethod) element;
			final IMethod[] methods= method.getDeclaringType().findMethods(method);
			if (methods != null && methods.length > 0)
				element= methods[0];
		}
		if (element != null && (!check || element.exists()))
			return element;
		return null;
	}

	/**
	 * Converts an input handle with the given prefix back to the corresponding
	 * resource.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param handle
	 *            the input handle
	 * 
	 * @return the corresponding resource, or <code>null</code> if no such
	 *         resource exists
	 */
	public static IResource handleToResource(final String project, final String handle) {
		final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		if ("".equals(handle)) //$NON-NLS-1$
			return null;
		final IPath path= Path.fromPortableString(handle);
		if (path == null)
			return null;
		if (project != null && !"".equals(project)) //$NON-NLS-1$
			return root.getProject(project).findMember(path);
		return root.findMember(path);
	}


	public static String getString(Map map, String attribute, boolean allowNull) {
		Object object= map.get(attribute);
		if (object == null) {
			if (allowNull)
				return null;
			throw new IllegalArgumentException("The map does not contain the attribute '" + attribute + "'");  //$NON-NLS-1$//$NON-NLS-2$
		}
		if (object instanceof String) {
			String value= (String) object;
			return value;
		}
		throw new IllegalArgumentException("The provided map does not contain a string for attribute '" + attribute + "'"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String getString(Map map, String attribute) {
		return getString(map, attribute, false);
	}

	public static String getString(Map map, String attribute, int index) {
		return getString(map, getAttributeName(attribute, index));
	}

	public static int getInt(Map map, String attribute) {
		String value= getString(map, attribute);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("The attribute '" + attribute + "' does not contain a valid int '" + value + "'");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
	}

	public static int[] getIntArray(Map map, String attribute, int count) {
		int[] result= new int[count];
		for (int i= 0; i < count; i++) {
			result[i]= getInt(map, getAttributeName(attribute, i));
		}
		return result;
	}

	public static String getAttributeName(String attribute, int index) {
		return attribute + index;
	}

	public static IJavaElement getJavaElement(Map map, String attribute, String project) {
		String handle= getString(map, attribute);
		return handleToElement(null, project, handle, true);
	}

	public static boolean[] getBoolArray(Map map, String attribute, int count) {
		boolean[] result= new boolean[count];
		for (int i= 0; i < count; i++) {
			result[i]= getBoolean(map, getAttributeName(attribute, i));
		}
		return result;
	}

	public static boolean getBoolean(Map map, String attribute) {
		String value= getString(map, attribute).toLowerCase();
		//Boolean.valueOf(value) does not complain about wrong values
		if (LOWER_CASE_TRUE.equals(value))
			return true;
		if (LOWER_CASE_FALSE.equals(value))
			return false;
		throw new IllegalArgumentException("The attribute '" + attribute + "' does not contain a valid boolean '" + value + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static void setJavaElement(Map arguments, String attribute, String project, IJavaElement element) {
		setString(arguments, attribute, elementToHandle(project, element));
	}

	public static void setInteger(Map arguments, String attribute, int value) {
		setString(arguments, attribute, Integer.toString(value));
	}

	public static void setInteger(Map arguments, String attribute, int value, int index) {
		setInteger(arguments, getAttributeName(attribute, index), value);
	}

	public static void setBoolean(Map arguments, String attribute, boolean value) {
		setString(arguments, attribute, value ? LOWER_CASE_TRUE : LOWER_CASE_FALSE);
	}

	public static void setBoolean(Map arguments, String attribute, boolean value, int index) {
		setBoolean(arguments, getAttributeName(attribute, index), value);
	}

	public static void setString(Map arguments, String attribute, String value, int index) {
		setString(arguments, getAttributeName(attribute, index), value);
	}

	public static void setString(Map arguments, String attribute, String value) {
		if (attribute == null || "".equals(attribute) || attribute.indexOf(' ') != -1) //$NON-NLS-1$
			throw new IllegalArgumentException("Attribute '" + attribute + "' is not valid");  //$NON-NLS-1$//$NON-NLS-2$
		if (value != null)
			arguments.put(attribute, value);
	}

}
