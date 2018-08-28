/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.model;

import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;

/**
 * Adaptor factory for model support.
 *
 * @since 3.2
 */
public final class JavaModelAdapterFactory implements IAdapterFactory {

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Object adaptable, Class<T> adapter) {
		if (adaptable instanceof JavaModelProvider) {
			if (adapter == IResourceMappingMerger.class)
				return (T) new JavaModelMerger((ModelProvider) adaptable);
			else if (adapter == ISynchronizationCompareAdapter.class)
				return (T) new JavaSynchronizationCompareAdapter();
		} else if (adaptable instanceof RefactoringHistory) {
			if (adapter == ResourceMapping.class)
				return (T) new JavaRefactoringHistoryResourceMapping((RefactoringHistory) adaptable);
			else if (adapter == IResource.class)
				return (T) new JavaRefactoringHistoryResourceMapping((RefactoringHistory) adaptable).getResource();
		} else if (adaptable instanceof RefactoringDescriptorProxy) {
			if (adapter == ResourceMapping.class)
				return (T) new JavaRefactoringDescriptorResourceMapping((RefactoringDescriptorProxy) adaptable);
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { ResourceMapping.class, ISynchronizationCompareAdapter.class, IResource.class};
	}
}
