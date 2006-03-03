/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * Descriptor object of a java refactoring.
 * 
 * @since 3.2
 */
public final class JavaRefactoringDescriptor extends RefactoringDescriptor {

	/**
	 * Predefined argument called <code>element&lt;Number&gt;</code>.
	 * <p>
	 * This argument should be used to describe the elements being refactored.
	 * The value of this argument does not necessarily have to uniquely identify
	 * the elements. However, it must be possible to uniquely identify the
	 * elements using the value of this argument in conjunction with the values
	 * of the other user-defined attributes.
	 * </p>
	 * <p>
	 * The element arguments are simply distinguished by appending a number to
	 * the argument name, eg. element1. The indices of this argument are non
	 * zero-based.
	 * </p>
	 */
	public static final String ATTRIBUTE_ELEMENT= "element"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>input</code>.
	 * <p>
	 * This argument should be used to describe the element being refactored.
	 * The value of this argument does not necessarily have to uniquely identify
	 * the input element. However, it must be possible to uniquely identify the
	 * input element using the value of this argument in conjunction with the
	 * values of the other user-defined attributes.
	 * </p>
	 */
	public static final String ATTRIBUTE_INPUT= "input"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>name</code>.
	 * <p>
	 * This argument should be used to name the element being refactored. The
	 * value of this argument may be shown in the user interface.
	 * </p>
	 */
	public static final String ATTRIBUTE_NAME= "name"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>selection</code>.
	 * <p>
	 * This argument should be used to describe user input selections within a
	 * text file. The value of this argument has the format "offset length".
	 * </p>
	 */
	public static final String ATTRIBUTE_SELECTION= "selection"; //$NON-NLS-1$

	/** The version attribute */
	private static final String ATTRIBUTE_VERSION= "version"; //$NON-NLS-1$

	/** The default package */
	private static final String DEFAULT_PACKAGE= "(default)"; //$NON-NLS-1$

	/**
	 * Constant describing the deprecation resolving flag.
	 * <p>
	 * Clients should set this flag to indicate that the refactoring can used to
	 * resolve deprecation problems of members declared in source.
	 * </p>
	 */
	public static final int DEPRECATION_RESOLVING= 1 << 17;

	/** The field identifier */
	private static final String IDENTIFIER_FIELD= "field"; //$NON-NLS-1$

	/** The initializer identifier */
	private static final String IDENTIFIER_INITIALIZER= "initializer"; //$NON-NLS-1$

	/** The method identifier */
	private static final String IDENTIFIER_METHOD= "method"; //$NON-NLS-1$

	/** The package identifier */
	private static final String IDENTIFIER_PACKAGE= "package"; //$NON-NLS-1$

	/** The project identifier */
	private static final String IDENTIFIER_PROJECT= "project"; //$NON-NLS-1$

	/** The resource identifier */
	private static final String IDENTIFIER_RESOURCE= "resource"; //$NON-NLS-1$

	/** The root identifier */
	private static final String IDENTIFIER_ROOT= "root"; //$NON-NLS-1$

	/** The type identifier */
	private static final String IDENTIFIER_TYPE= "type"; //$NON-NLS-1$

	/** The unit identifier */
	private static final String IDENTIFIER_UNIT= "unit"; //$NON-NLS-1$

	/**
	 * Constant describing the jar deprecation resolving flag.
	 * <p>
	 * Clients should set this flag to indicate that the refactoring can used to
	 * resolve deprecation problems both of binary members contained in JAR
	 * files and members declared in source.
	 * </p>
	 */
	public static final int JAR_DEPRECATION_RESOLVING= 1 << 18;

	/**
	 * Constant describing the importable flag.
	 * <p>
	 * Clients should set this flag to indicate that the refactoring can be
	 * imported from a JAR file.
	 * </p>
	 */
	public static final int JAR_IMPORTABLE= 1 << 16;

	/** The dot separator */
	private static final char SEPARATOR_DOT= '.';

	/** The member separator */
	private static final char SEPARATOR_MEMBER= '#';

	/** The package separator */
	private static final char SEPARATOR_PACKAGE= '$';

	/** The path separator */
	private static final char SEPARATOR_PATH= IPath.SEPARATOR;

	/** The version value 1.0 */
	private static final String VALUE_VERSION_1_0= "1.0"; //$NON-NLS-1$

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
		final int elementType= element.getElementType();
		switch (elementType) {
			case IJavaElement.JAVA_PROJECT:
				return getHandlePrefix(IDENTIFIER_PROJECT) + element.getElementName();
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return resourceToHandle(project, getHandlePrefix(IDENTIFIER_ROOT), element.getResource());
			case IJavaElement.COMPILATION_UNIT:
				return resourceToHandle(project, getHandlePrefix(IDENTIFIER_UNIT), element.getResource());
			case IJavaElement.PACKAGE_FRAGMENT:
				return resourceToHandle(project, getHandlePrefix(IDENTIFIER_PACKAGE), element.getResource());
//			case IJavaElement.TYPE: {
//				final IType type= (IType) element;
//				final StringBuffer buffer= new StringBuffer();
//				buffer.append(getHandlePrefix(IDENTIFIER_TYPE));
//				if (project == null) {
//					buffer.append(type.getJavaProject().getElementName());
//					buffer.append(SEPARATOR_PATH);
//				}
//				final IPackageFragment fragment= type.getPackageFragment();
//				if (fragment.isDefaultPackage())
//					buffer.append(DEFAULT_PACKAGE);
//				else
//					buffer.append(fragment.getElementName());
//				buffer.append(SEPARATOR_PACKAGE);
//				buffer.append(type.getTypeQualifiedName(SEPARATOR_PACKAGE));
//				return buffer.toString();
//			}
		}
		return element.getHandleIdentifier();
	}

	/**
	 * Returns the handle prefix for the corresponding identifier
	 * 
	 * @param identifier
	 *            the identifier
	 * @return the handle prefix
	 */
	private static String getHandlePrefix(final String identifier) {
		Assert.isNotNull(identifier);
		return identifier + "://"; //$NON-NLS-1$
	}

	/**
	 * Converts an input handle back to the corresponding java element.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param handle
	 *            the input handle
	 * @return the corresponding java element, or <code>null</code> if no such
	 *         element exists
	 */
	public static IJavaElement handleToElement(final String project, final String handle) {
		return handleToElement(project, handle, true);
	}

	/**
	 * Converts an input handle back to the corresponding java element.
	 * 
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
	public static IJavaElement handleToElement(final String project, final String handle, final boolean check) {
		IJavaElement element= null;
		if (handle.startsWith(getHandlePrefix(IDENTIFIER_PROJECT))) {
			element= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(handle.substring(getHandlePrefix(IDENTIFIER_PROJECT).length())));
		} else if (handle.startsWith(getHandlePrefix(IDENTIFIER_ROOT))) {
			element= JavaCore.create(handleToResource(project, handle, getHandlePrefix(IDENTIFIER_ROOT)));
		} else if (handle.startsWith(getHandlePrefix(IDENTIFIER_UNIT))) {
			element= JavaCore.create(handleToResource(project, handle, getHandlePrefix(IDENTIFIER_UNIT)));
		} else if (handle.startsWith(getHandlePrefix(IDENTIFIER_PACKAGE))) {
			element= JavaCore.create(handleToResource(project, handle, getHandlePrefix(IDENTIFIER_PACKAGE)));
//		} else if (handle.startsWith(getHandlePrefix(IDENTIFIER_TYPE))) {
//			String string= handle.substring(getHandlePrefix(IDENTIFIER_TYPE).length());
//			String container= project;
//			if (container == null) {
//				final int index= string.indexOf(SEPARATOR_PATH);
//				if (index > 0) {
//					container= string.substring(0, index);
//					if (string.length() >= index + 2)
//						string= string.substring(index + 1);
//					else
//						return null;
//				}
//			}
//			if (container == null)
//				return null;
//			String fragment= null;
//			final int index= string.indexOf(SEPARATOR_PACKAGE);
//			if (index > 0) {
//				fragment= string.substring(0, index);
//				if (string.length() >= index + 2)
//					string= string.substring(index + 1);
//				else
//					return null;
//			}
//			if (fragment == null)
//				return null;
//			final IJavaProject javaProject= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(container));
//			if (javaProject == null || !javaProject.exists())
//				return null;
//			final StringBuffer buffer= new StringBuffer(string);
//			final int length= buffer.length();
//			for (int offset= 0; offset < length; offset++) {
//				final char character= buffer.charAt(offset);
//				if (character == SEPARATOR_PACKAGE && offset < length - 1) {
//					if (!Character.isDigit(buffer.charAt(offset + 1)))
//						buffer.setCharAt(offset, SEPARATOR_DOT);
//				}
//			}
//			try {
//				element= javaProject.findType(fragment, buffer.toString(), new NullProgressMonitor());
//			} catch (JavaModelException exception) {
//				JavaPlugin.log(exception);
//			}
		} else
			element= JavaCore.create(handle);
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
	 * @param prefix
	 *            the prefix
	 * 
	 * @return the corresponding resource, or <code>null</code> if no such
	 *         resource exists
	 */
	private static IResource handleToResource(String project, final String handle, final String prefix) {
		final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		String string= handle;
		if (handle.startsWith(prefix))
			string= string.substring(prefix.length());
		else
			return null;
		if ("".equals(string)) //$NON-NLS-1$
			return null;
		final IPath path= Path.fromPortableString(string);
		if (path == null)
			return null;
		if (project != null && !"".equals(project)) //$NON-NLS-1$
			return root.getProject(project).findMember(path);
		return root.findMember(path);
	}

	/**
	 * Converts the specified resource to a portable path with prefix.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param prefix
	 *            the prefix
	 * @param resource
	 *            the resource
	 * 
	 * @return the prefixed portable path
	 */
	private static String resourceToHandle(final String project, final String prefix, final IResource resource) {
		if (project != null && !"".equals(project)) //$NON-NLS-1$
			return prefix + resource.getProjectRelativePath().toPortableString();
		return prefix + resource.getFullPath().toPortableString();
	}

	/** The map of arguments (element type: &lt;String, String&gt;) */
	private final Map fArguments;

	/** The refactoring contribution, or <code>null</code> */
	private JavaRefactoringContribution fContribution;

	/**
	 * Creates a new java refactoring descriptor.
	 * 
	 * @param contribution
	 *            the refactoring contribution, or <code>null</code>
	 * @param id
	 *            the unique id of the refactoring
	 * @param project
	 *            the project name, or <code>null</code>
	 * @param description
	 *            the description
	 * @param comment
	 *            the comment, or <code>null</code>
	 * @param arguments
	 *            the argument map
	 * @param flags
	 *            the flags
	 */
	public JavaRefactoringDescriptor(final JavaRefactoringContribution contribution, final String id, final String project, final String description, final String comment, final Map arguments, final int flags) {
		super(id, project, description, comment, flags);
		Assert.isNotNull(arguments);
		fContribution= contribution;
		fArguments= arguments;
	}

	/**
	 * Creates a new java refactoring descriptor.
	 * 
	 * @param id
	 *            the unique id of the refactoring
	 * @param project
	 *            the project name, or <code>null</code>
	 * @param description
	 *            the description
	 * @param comment
	 *            the comment, or <code>null</code>
	 * @param arguments
	 *            the argument map
	 * @param flags
	 *            the flags
	 */
	public JavaRefactoringDescriptor(final String id, final String project, final String description, final String comment, final Map arguments, final int flags) {
		this(null, id, project, description, comment, arguments, flags);
	}

	/**
	 * Creates refactoring arguments for this refactoring descriptor.
	 * <p>
	 * This method is used by the refactoring framework to create refactoring
	 * arguments for the refactoring instance represented by this refactoring
	 * descriptor. The result of this method is used as argument to initialize a
	 * refactoring.
	 * </p>
	 * <p>
	 * Note: this method must not be used outside the refactoring framework.
	 * </p>
	 * 
	 * @return the refactoring arguments, or <code>null</code> if this
	 *         refactoring descriptor represents the unknown refactoring, or if
	 *         no refactoring contribution is available for this refactoring
	 *         descriptor
	 */
	public RefactoringArguments createArguments() {
		final JavaRefactoringArguments arguments= new JavaRefactoringArguments(getProject());
		for (final Iterator iterator= fArguments.entrySet().iterator(); iterator.hasNext();) {
			final Map.Entry entry= (Entry) iterator.next();
			final String name= (String) entry.getKey();
			final String value= (String) entry.getValue();
			if (name != null && !"".equals(name) && value != null) //$NON-NLS-1$
				arguments.setAttribute(name, value);
		}
		return arguments;
	}

	/**
	 * {@inheritDoc}
	 */
	public Refactoring createRefactoring(final RefactoringStatus status) throws CoreException {
		Refactoring refactoring= null;
		if (fContribution != null)
			refactoring= fContribution.createRefactoring(this);
		else {
			final RefactoringContribution contribution= RefactoringCore.getRefactoringContribution(getID());
			if (contribution instanceof JavaRefactoringContribution) {
				fContribution= (JavaRefactoringContribution) contribution;
				refactoring= fContribution.createRefactoring(this);
			}
		}
		if (refactoring != null) {
			if (refactoring instanceof IInitializableRefactoringComponent)
				status.merge(((IInitializableRefactoringComponent) refactoring).initialize(createArguments()));
			else
				status.merge(RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_initialization_error, getID())));
		}
		return refactoring;
	}

	/**
	 * Converts the specified element to an input handle.
	 * 
	 * @param element
	 *            the element
	 * @return a corresponding input handle
	 */
	public String elementToHandle(final IJavaElement element) {
		Assert.isNotNull(element);
		return elementToHandle(getProject(), element);
	}

	/**
	 * Returns the argument map
	 * 
	 * @return the argument map.
	 */
	public Map getArguments() {
		final Map map= new HashMap(fArguments);
		map.put(ATTRIBUTE_VERSION, VALUE_VERSION_1_0);
		return map;
	}

	/**
	 * Returns the refactoring contribution.
	 * 
	 * @return the refactoring contribution, or <code>null</code>
	 */
	public JavaRefactoringContribution getContribution() {
		return fContribution;
	}

	/**
	 * Converts an input handle back to the corresponding java element.
	 * 
	 * @param handle
	 *            the input handle
	 * @return the corresponding java element, or <code>null</code> if no such
	 *         element exists
	 */
	public IJavaElement handleToElement(final String handle) {
		Assert.isNotNull(handle);
		Assert.isLegal(!"".equals(handle)); //$NON-NLS-1$
		final String project= getProject();
		return handleToElement(project, handle);
	}

	/**
	 * Converts an input handle back to the corresponding resource.
	 * 
	 * @param handle
	 *            the input handle
	 * @return the corresponding resource, or <code>null</code> if no such
	 *         resource exists
	 */
	public IResource handleToResource(final String handle) {
		Assert.isNotNull(handle);
		Assert.isLegal(!"".equals(handle)); //$NON-NLS-1$
		return handleToResource(getProject(), handle, getHandlePrefix(IDENTIFIER_RESOURCE));
	}

	/**
	 * Converts the specified resource to an input handle.
	 * 
	 * @param resource
	 *            the resource
	 * @return a corresponding input handle
	 */
	public String resourceToHandle(final IResource resource) {
		Assert.isNotNull(resource);
		return resourceToHandle(getProject(), getHandlePrefix(IDENTIFIER_RESOURCE), resource);
	}
}