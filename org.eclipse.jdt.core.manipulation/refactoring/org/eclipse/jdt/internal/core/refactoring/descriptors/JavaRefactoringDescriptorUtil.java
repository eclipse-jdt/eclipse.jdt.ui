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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;

public class JavaRefactoringDescriptorUtil {

	private static final String LOWER_CASE_FALSE= Boolean.FALSE.toString().toLowerCase();
	private static final String LOWER_CASE_TRUE= Boolean.TRUE.toString().toLowerCase();

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
			throw new IllegalArgumentException("The map does not contain the attribute '" + attribute + "'"); //$NON-NLS-1$//$NON-NLS-2$
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

	public static int getInt(Map map, String attribute) {
		String value= getString(map, attribute);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("The attribute '" + attribute + "' does not contain a valid int '" + value + "'"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
	}

	public static int[] getIntArray(Map map, String countAttribute, String arrayAttribute) {
		int count= getInt(map, countAttribute);
		int[] result= new int[count];
		for (int i= 0; i < count; i++) {
			result[i]= getInt(map, getAttributeName(arrayAttribute, i));
		}
		return result;
	}

	public static String getAttributeName(String attribute, int index) {
		return attribute + index;
	}

	public static IJavaElement getJavaElement(Map map, String attribute, String project) {
		return getJavaElement(map, attribute, project, false);
	}

	public static IJavaElement getJavaElement(Map map, String attribute, String project, boolean allowNull) {
		String handle= getString(map, attribute, allowNull);
		if (handle != null)
			return handleToElement(null, project, handle, true);
		return null;
	}
	
	public static IResource getResource(Map map, String attribute, String project) {
		String handle= getString(map, attribute);
		return handleToResource(project, handle);
	}

	public static boolean[] getBooleanArray(Map map, String countAttribute, String arrayAttribute) {
		int count= getInt(map, countAttribute);
		boolean[] result= new boolean[count];
		for (int i= 0; i < count; i++) {
			result[i]= getBoolean(map, getAttributeName(arrayAttribute, i));
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

	public static ISourceRange getSelection(Map map, String attribute) {
		String value= getString(map, attribute);
		String[] split= value.split(" "); //$NON-NLS-1$
		if (split.length != 2)
			throw new IllegalArgumentException("The attribute '" + attribute + "' does not contain valid selection information '" + value + "'"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		try {
			final int offSet= Integer.parseInt(split[0]);
			final int length= Integer.parseInt(split[1]);
			return new ISourceRange() {

				public int getOffset() {
					return offSet;
				}

				public int getLength() {
					return length;
				}

			};
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("The attribute '" + attribute + "' does not contain valid selection information '" + value + "'"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
	}

	public static void setJavaElement(Map arguments, String attribute, String project, IJavaElement element) {
		setString(arguments, attribute, elementToHandle(project, element));
	}

	public static void setResource(Map arguments, String attribute, String project, IResource resource) {
		setString(arguments, attribute, resourceToHandle(project, resource));
	}

	public static void setInt(Map arguments, String attribute, int value) {
		setString(arguments, attribute, Integer.toString(value));
	}

	public static void setBoolean(Map arguments, String attribute, boolean value) {
		setString(arguments, attribute, value ? LOWER_CASE_TRUE : LOWER_CASE_FALSE);
	}

	public static void setString(Map arguments, String attribute, String value) {
		if (attribute == null || "".equals(attribute) || attribute.indexOf(' ') != -1) //$NON-NLS-1$
			throw new IllegalArgumentException("Attribute '" + attribute + "' is not valid"); //$NON-NLS-1$//$NON-NLS-2$
		if (value != null)
			arguments.put(attribute, value);
	}

	public static void setSelection(Map arguments, String attribute, int offset, int length) {
		String value= Integer.toString(offset) + " " + Integer.toString(length); //$NON-NLS-1$
		setString(arguments, attribute, value);
	}

	public static void setResourceArray(Map arguments, String countAttribute, String arrayAttribute, String project, IResource[] resources, int offset) {
		if (countAttribute != null)
			setInt(arguments, countAttribute, resources.length);
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			setResource(arguments, getAttributeName(arrayAttribute, offset + i), project, resource);
		}
	}

	public static void setJavaElementArray(Map arguments, String countAttribute, String arrayAttribute, String project, IJavaElement[] elements, int offset) {
		if (countAttribute != null)
			setInt(arguments, countAttribute, elements.length);
		for (int i= 0; i < elements.length; i++) {
			IJavaElement element= elements[i];
			setJavaElement(arguments, getAttributeName(arrayAttribute, offset + i), project, element);
		}
	}

	/**
	 * 
	 * @param map
	 * @param countAttribute
	 * @param arrayAttribute
	 * @param offset
	 * @param project
	 * @param arrayClass the array component type. The array will safely be castable to arrayClass[] afterward
	 * @return an array of resources
	 */
	public static IResource[] getResourceArray(Map map, String countAttribute, String arrayAttribute, int offset, String project, Class arrayClass) {
		int count= getInt(map, countAttribute);
		IResource[] result= (IResource[]) Array.newInstance(arrayClass, count);
		for (int i= 0; i < count; i++) {
			result[i]= getResource(map, getAttributeName(arrayAttribute, i + offset), project);
		}
		return result;
	}

	/**
	 * 
	 * @param map
	 * @param countAttribute
	 * @param arrayAttribute
	 * @param offset
	 * @param project
	 * @param arrayClass the array component type. The array will safely be castable to arrayClass[] afterward
	 * @return an array of javaelements
	 */
	public static IJavaElement[] getJavaElementArray(Map map, String countAttribute, String arrayAttribute, int offset, String project, Class arrayClass) {
		if (countAttribute != null) {
			int count= getInt(map, countAttribute);
			IJavaElement[] result= (IJavaElement[]) Array.newInstance(arrayClass, count);
			for (int i= 0; i < count; i++) {
				result[i]= getJavaElement(map, getAttributeName(arrayAttribute, i + offset), project);
			}
			return result;
		} else {
			ArrayList result= new ArrayList();
			IJavaElement element= null;
			while ((element= getJavaElement(map, arrayAttribute, project, true)) != null){
				result.add(element);
			}
			return (IJavaElement[]) result.toArray((Object[]) Array.newInstance(arrayClass, result.size()));
		}
	}
}
