/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

/**
 * Refactoring arguments which provide the ability to set arguments using
 * key-value pairs of strings.
 * 
 * @see RefactoringContribution
 * @see RefactoringDescriptor
 * 
 * @since 3.2
 */
public final class JavaRefactoringArguments extends RefactoringArguments {

	/** The attribute map (element type: <code>&lt;String, String&gt;</code>) */
	private final Map fAttributes= new HashMap(2);

	/**
	 * Returns the attribute with the specified name.
	 * 
	 * @param name
	 *            the name of the attribute
	 * @return the attribute value, or <code>null</code>
	 */
	public String getAttribute(final String name) {
		return (String) fAttributes.get(name);
	}

	/**
	 * Sets the attribute with the specified name to the indicated value.
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 */
	public void setAttribute(final String name, final String value) {
		Assert.isNotNull(name);
		Assert.isNotNull(value);
		fAttributes.put(name, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return getClass().getName() + fAttributes.toString();
	}
}