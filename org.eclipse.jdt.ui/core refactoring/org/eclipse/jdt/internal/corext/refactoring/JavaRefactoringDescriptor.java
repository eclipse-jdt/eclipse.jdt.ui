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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

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
	public static final String ELEMENT= "element"; //$NON-NLS-1$

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
	public static final String INPUT= "input"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>name</code>.
	 * <p>
	 * This argument should be used to name the element being refactored. The
	 * value of this argument may be shown in the user interface.
	 * </p>
	 */
	public static final String NAME= "name"; //$NON-NLS-1$

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
		fArguments= Collections.unmodifiableMap(new HashMap(arguments));
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
	 * {@inheritDoc}
	 */
	public RefactoringArguments createArguments() {
		final JavaRefactoringArguments arguments= new JavaRefactoringArguments();
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
	public Refactoring createRefactoring() throws CoreException {
		if (fContribution != null)
			return fContribution.createRefactoring(this);
		final RefactoringContribution contribution= RefactoringCore.getRefactoringContribution(getID());
		if (contribution instanceof JavaRefactoringContribution) {
			fContribution= (JavaRefactoringContribution) contribution;
			return fContribution.createRefactoring(this);
		}
		return null;
	}

	/**
	 * Returns the argument map
	 * 
	 * @return the argument map.
	 */
	public Map getArguments() {
		return fArguments;
	}
}