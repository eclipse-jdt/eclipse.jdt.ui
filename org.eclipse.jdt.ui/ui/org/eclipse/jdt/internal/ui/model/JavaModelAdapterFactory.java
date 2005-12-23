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

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.team.core.mapping.IResourceMappingMerger;

import org.eclipse.team.ui.mapping.ICompareAdapter;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.core.refactoring.model.AbstractRefactoringModelProvider;

/**
 * Adaptor factory for model support.
 * 
 * @since 3.2
 */
public final class JavaModelAdapterFactory implements IAdapterFactory {

	/**
	 * {@inheritDoc}
	 */
	public Object getAdapter(final Object adaptable, final Class adapter) {
		if (adaptable instanceof AbstractRefactoringModelProvider) {
			if (adapter == IResourceMappingMerger.class)
				return new JavaModelMerger((ModelProvider) adaptable);
			else if (adapter == ICompareAdapter.class)
				return new JavaCompareAdapter();
		} else if (adaptable instanceof RefactoringHistory) {
			if (adapter == ResourceMapping.class)
				return new JavaRefactoringHistoryResourceMapping((RefactoringHistory) adaptable);
			else if (adapter == IResource.class)
				return new JavaRefactoringHistoryResourceMapping((RefactoringHistory) adaptable).getResource();
		} else if (adaptable instanceof RefactoringDescriptorProxy) {
			if (adapter == ResourceMapping.class)
				return new JavaRefactoringDescriptorResourceMapping((RefactoringDescriptorProxy) adaptable);
		} else if (adaptable instanceof JavaProjectSettings) {
			if (adapter == ResourceMapping.class)
				return new JavaProjectSettingsResourceMapping((JavaProjectSettings) adaptable);
			else if (adapter == IResource.class)
				return ((JavaProjectSettings) adaptable).getResource();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class[] getAdapterList() {
		return new Class[] { IResourceMappingMerger.class, ResourceMapping.class, ICompareAdapter.class, IResource.class};
	}
}