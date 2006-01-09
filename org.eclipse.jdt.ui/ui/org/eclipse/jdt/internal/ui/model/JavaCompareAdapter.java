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

import org.eclipse.core.resources.IResource;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;

import org.eclipse.team.core.mapping.ISynchronizationContext;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.ui.refactoring.model.AbstractRefactoringCompareAdapter;

/**
 * Java-aware compare adapter.
 * 
 * @since 3.2
 */
public final class JavaCompareAdapter extends AbstractRefactoringCompareAdapter {

	/**
	 * {@inheritDoc}
	 */
	public ICompareInput asCompareInput(final ISynchronizationContext context, final Object element) {
		if (element instanceof RefactoringDescriptorProxy)
			return super.asCompareInput(context, element);
		final IResource resource= JavaModelProvider.getResource(element);
		if (resource != null)
			return super.asCompareInput(context, resource);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	protected int getKind(final ISynchronizationContext context, final RefactoringDescriptorProxy proxy) {
		return Differencer.ADDITION | Differencer.LEFT;
	}
}