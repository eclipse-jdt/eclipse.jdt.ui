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
package org.eclipse.jdt.core.refactoring.descriptors;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.refactoring.descriptors.DescriptorMessages;

/**
 * Partial implementation of a java refactoring descriptor.
 * <p>
 * This class provides features common to all Java refactorings.
 * </p>
 * <p>
 * Note: this class is not intended to be extended outside the refactoring
 * framework.
 * </p>
 * 
 * @since 3.3
 */
public abstract class JavaRefactoringDescriptor extends RefactoringDescriptor {

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
	 * the argument name, e.g. element1. The indices of this argument are non
	 * zero-based.
	 * </p>
	 */
	protected static final String ATTRIBUTE_ELEMENT= "element"; //$NON-NLS-1$

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
	protected static final String ATTRIBUTE_INPUT= "input"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>name</code>.
	 * <p>
	 * This argument should be used to name the element being refactored. The
	 * value of this argument may be shown in the user interface.
	 * </p>
	 */
	protected static final String ATTRIBUTE_NAME= "name"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>selection</code>.
	 * <p>
	 * This argument should be used to describe user input selections within a
	 * text file. The value of this argument has the format "offset length".
	 * </p>
	 */
	protected static final String ATTRIBUTE_SELECTION= "selection"; //$NON-NLS-1$

	/** The version attribute */
	protected static final String ATTRIBUTE_VERSION= "version"; //$NON-NLS-1$

	/**
	 * Constant describing the jar migration flag (value: <code>65536</code>).
	 * <p>
	 * Clients should set this flag to indicate that the refactoring can be
	 * stored to a JAR file in order to be accessible to the Migrate JAR File
	 * refactoring, regardless whether there is a source attachment to the JAR
	 * file or not. If this flag is set, <code>JAR_REFACTORING</code> should
	 * be set as well.
	 * </p>
	 * 
	 * @see #JAR_REFACTORING
	 */
	public static final int JAR_MIGRATION= 1 << 16;

	/**
	 * Constant describing the jar refactoring flag (value: <code>524288</code>).
	 * <p>
	 * Clients should set this flag to indicate that the refactoring in
	 * principle can be performed on binary elements originating from a JAR
	 * file. Refactorings which are able to run on binary elements, but require
	 * a correctly configured source attachment to work must set the
	 * <code>JAR_SOURCE_ATTACHMENT</code> flag as well.
	 * </p>
	 * 
	 * @see #JAR_SOURCE_ATTACHMENT
	 */
	public static final int JAR_REFACTORING= 1 << 19;

	/**
	 * Constant describing the jar source attachment flag (value:
	 * <code>262144</code>).
	 * <p>
	 * Clients should set this flag to indicate that the refactoring can be
	 * performed on binary elements originating from a JAR file if and only if
	 * it has a correctly configured source attachment.
	 * </p>
	 * 
	 * @see #JAR_REFACTORING
	 */
	public static final int JAR_SOURCE_ATTACHMENT= 1 << 18;

	/** The version value <code>1.0</code> */
	protected static final String VALUE_VERSION_1_0= "1.0"; //$NON-NLS-1$

	/**
	 * Converts the specified element to an input handle.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param element
	 *            the element
	 * @return a corresponding input handle
	 */
	protected static String elementToHandle(final String project, final IJavaElement element) {
		final String handle= element.getHandleIdentifier();
		if (project != null && !(element instanceof IJavaProject)) {
			final String id= element.getJavaProject().getHandleIdentifier();
			return handle.substring(id.length());
		}
		return handle;
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
	protected static IJavaElement handleToElement(final String project, final String handle) {
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
	protected static IJavaElement handleToElement(final String project, final String handle, final boolean check) {
		return handleToElement(null, project, handle, check);
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
	protected static IJavaElement handleToElement(final WorkingCopyOwner owner, final String project, final String handle, final boolean check) {
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
	protected static IResource handleToResource(final String project, final String handle) {
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
	protected static String resourceToHandle(final String project, final IResource resource) {
		if (project != null && !"".equals(project)) //$NON-NLS-1$
			return resource.getProjectRelativePath().toPortableString();
		return resource.getFullPath().toPortableString();
	}

	/** The argument map */
	protected final Map fArguments= new HashMap();

	/**
	 * Creates a new java refactoring descriptor.
	 * 
	 * @param id
	 *            the unique id of the refactoring
	 */
	protected JavaRefactoringDescriptor(final String id) {
		super(id, null, DescriptorMessages.JavaRefactoringDescriptor_not_available, null, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		fArguments.put(ATTRIBUTE_VERSION, VALUE_VERSION_1_0);
	}

	/**
	 * {@inheritDoc}
	 */
	public Refactoring createRefactoring(final RefactoringStatus status) throws CoreException {
		Refactoring refactoring= null;
		final String id= getID();
		final RefactoringContribution contribution= RefactoringCore.getRefactoringContribution(id);
		if (contribution != null) {
			final RefactoringDescriptor descriptor= contribution.createDescriptor(id, getProject(), getDescription(), getComment(), fArguments, getFlags());
			if (descriptor != null) {
				refactoring= descriptor.createRefactoring(status);
			} else
				JavaManipulationPlugin.log(new Status(IStatus.ERROR, JavaManipulationPlugin.getPluginId(), 0, MessageFormat.format(DescriptorMessages.JavaRefactoringDescriptor_no_resulting_descriptor, new Object[] { id}), null));
		}
		return refactoring;
	}

	/**
	 * Returns the argument map of this refactoring descriptor.
	 * <p>
	 * The returned map is a copy of the argument map. Modifying the result does
	 * not change the refactoring descriptor itself.
	 * </p>
	 * <p>
	 * Note: This API must not be extended or reimplemented and should not be
	 * called from outside the refactoring framework.
	 * </p>
	 * 
	 * @return the argument map
	 */
	protected Map getArguments() {
		return new HashMap(fArguments);
	}

	/**
	 * Sets the details comment of this refactoring.
	 * <p>
	 * This information is used in the user interface to show additional details
	 * about the performed refactoring. The default is to use no details
	 * comment.
	 * </p>
	 * 
	 * @param comment
	 *            the details comment to set, or <code>null</code> to set no
	 *            details comment
	 * 
	 * @see #getComment()
	 */
	public void setComment(final String comment) {
		super.setComment(comment);
	}

	/**
	 * Sets the description of this refactoring.
	 * <p>
	 * This information is used to label a refactoring in the user interface.
	 * The default is an unspecified, but legal description.
	 * </p>
	 * 
	 * @param description
	 *            the non-empty description of the refactoring to set
	 * 
	 * @see #getDescription()
	 */
	public void setDescription(final String description) {
		super.setDescription(description);
	}

	/**
	 * Sets the flags of this refactoring.
	 * <p>
	 * The default is
	 * <code>RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE</code>,
	 * unless overridden by a concrete subclass. Clients may use refactoring
	 * flags to indicate special capabilities of Java refactorings.
	 * </p>
	 * 
	 * @param flags
	 *            the flags to set, or <code>RefactoringDescriptor.NONE</code>
	 *            to clear the flags
	 * 
	 * @see #getFlags()
	 * 
	 * @see RefactoringDescriptor#NONE
	 * @see RefactoringDescriptor#STRUCTURAL_CHANGE
	 * @see RefactoringDescriptor#BREAKING_CHANGE
	 * @see RefactoringDescriptor#MULTI_CHANGE
	 * 
	 * @see #JAR_MIGRATION
	 * @see #JAR_REFACTORING
	 * @see #JAR_SOURCE_ATTACHMENT
	 */
	public void setFlags(final int flags) {
		super.setFlags(flags);
	}

	/**
	 * Sets the project name of this refactoring.
	 * <p>
	 * The default is to associate the refactoring with the workspace.
	 * Subclasses should call this method with the project name associated with
	 * the refactoring's input elements, if available.
	 * </p>
	 * 
	 * @param project
	 *            the non-empty project name to set, or <code>null</code> for
	 *            the workspace
	 * 
	 * @see #getProject()
	 */
	public void setProject(final String project) {
		super.setProject(project);
	}
}