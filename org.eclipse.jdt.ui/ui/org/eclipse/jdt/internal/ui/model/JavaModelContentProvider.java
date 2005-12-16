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
package org.eclipse.jdt.internal.ui.model;

import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

/**
 * Content provider for Java models.
 * 
 * @since 3.2
 */
public final class JavaModelContentProvider extends StandardJavaElementContentProvider {

	/**
	 * Creates a new java model content provider.
	 */
	public JavaModelContentProvider() {
		super(true);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object[] getChildren(final Object element) {
		if (element instanceof JavaProjectSettings)
			return NO_CHILDREN;
		else if (element instanceof RefactoringHistory)
			return ((RefactoringHistory) element).getDescriptors();
		else
			return super.getChildren(element);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasChildren(final Object element) {
		if (element instanceof JavaProjectSettings)
			return false;
		else if (element instanceof RefactoringHistory)
			return true;
		else
			return super.hasChildren(element);
	}
}