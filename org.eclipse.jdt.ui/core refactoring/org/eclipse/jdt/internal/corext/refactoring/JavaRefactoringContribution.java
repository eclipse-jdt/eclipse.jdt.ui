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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.IRefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

/**
 * Partial implementation of a java refactoring contribution.
 * 
 * @since 3.2
 */
public abstract class JavaRefactoringContribution implements IRefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public RefactoringArguments createArguments(final RefactoringDescriptor descriptor) {
		Assert.isNotNull(descriptor);
		final JavaRefactoringArguments arguments= new JavaRefactoringArguments();
		if (descriptor instanceof JavaRefactoringDescriptor) {
			final JavaRefactoringDescriptor extended= (JavaRefactoringDescriptor) descriptor;
			final Map map= extended.getArguments();
			for (final Iterator iterator= map.entrySet().iterator(); iterator.hasNext();) {
				final Map.Entry entry= (Entry) iterator.next();
				final String name= (String) entry.getKey();
				final String value= (String) entry.getValue();
				if (name != null && !"".equals(name) && value != null) //$NON-NLS-1$
					arguments.setAttribute(name, value);
			}
		}
		return arguments;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringDescriptor createDescriptor(final String id, final String project, final String description, final String comment, final Map arguments, final int flags) {
		return new JavaRefactoringDescriptor(id, project, description, comment, arguments, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	public Map getArguments(final RefactoringDescriptor descriptor) {
		Assert.isNotNull(descriptor);
		if (descriptor instanceof JavaRefactoringDescriptor)
			return ((JavaRefactoringDescriptor) descriptor).getArguments();
		return null;
	}
}