/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.team.core.mapping.ISynchronizationContext;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import org.eclipse.compare.structuremergeviewer.ICompareInput;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.ui.refactoring.model.AbstractSynchronizationCompareAdapter;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Java-aware synchronization compare adapter.
 *
 * @since 3.2
 */
public final class JavaSynchronizationCompareAdapter extends AbstractSynchronizationCompareAdapter {

	/** The modelProviderId name */
	private static final String MODEL_PROVIDER_ID= "modelProviderId"; //$NON-NLS-1$

	/** The modelProviders name */
	private static final String MODEL_PROVIDERS= "modelProviders"; //$NON-NLS-1$

	/** The resourcePath name */
	private static final String RESOURCE_PATH= "resourcePath"; //$NON-NLS-1$

	/** The resourceType name */
	private static final String RESOURCE_TYPE= "resourceType"; //$NON-NLS-1$

	/** The resources name */
	private static final String RESOURCES= "resources"; //$NON-NLS-1$

	/** The workingSetName name */
	private static final String WORKING_SET_NAME= "workingSetName"; //$NON-NLS-1$

	/** The workingSets name */
	private static final String WORKING_SETS= "workingSets"; //$NON-NLS-1$

	@Override
	public ICompareInput asCompareInput(final ISynchronizationContext context, final Object element) {
		if (element instanceof RefactoringDescriptorProxy)
			return super.asCompareInput(context, element);
		final IResource resource= JavaModelProvider.getResource(element);
		if (resource != null)
			return super.asCompareInput(context, resource);
		return null;
	}

	@Override
	public ResourceMapping[] restore(final IMemento memento) {
		final List<ResourceMapping> result= new ArrayList<>();
		for (IMemento child : memento.getChildren(RESOURCES)) {
			final Integer typeInt= child.getInteger(RESOURCE_TYPE);
			if (typeInt == null)
				continue;
			final String pathString= child.getString(RESOURCE_PATH);
			if (pathString == null)
				continue;
			IResource resource= null;
			final IPath path= new Path(pathString);
			final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			switch (typeInt) {
				case IResource.ROOT:
					resource= root;
					break;
				case IResource.PROJECT:
					resource= root.getProject(path.lastSegment());
					break;
				case IResource.FILE:
					resource= root.getFile(path);
					break;
				case IResource.FOLDER:
					resource= root.getFolder(path);
					break;
			}
			if (resource != null) {
				final ResourceMapping mapping= JavaSynchronizationContentProvider.getResourceMapping(resource);
				if (mapping != null)
					result.add(mapping);
			}
		}
		for (IMemento child : memento.getChildren(WORKING_SETS)) {
			final String name= child.getString(WORKING_SET_NAME);
			if (name == null)
				continue;
			final IWorkingSet set= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
			if (set != null) {
				final ResourceMapping mapping= JavaSynchronizationContentProvider.getResourceMapping(set);
				if (mapping != null)
					result.add(mapping);
			}
		}
		for (IMemento child : memento.getChildren(MODEL_PROVIDERS)) {
			final String id= child.getString(MODEL_PROVIDER_ID);
			if (id == null)
				continue;
			final IModelProviderDescriptor descriptor= ModelProvider.getModelProviderDescriptor(id);
			if (descriptor == null)
				continue;
			try {
				final ModelProvider provider= descriptor.getModelProvider();
				if (provider != null) {
					final ResourceMapping mapping= JavaSynchronizationContentProvider.getResourceMapping(provider);
					if (mapping != null)
						result.add(mapping);
				}
			} catch (CoreException event) {
				JavaPlugin.log(event);
			}
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	@Override
	public void save(final ResourceMapping[] mappings, final IMemento memento) {
		for (ResourceMapping mapping : mappings) {
			final Object object= mapping.getModelObject();
			if (object instanceof IJavaElement) {
				final IJavaElement element= (IJavaElement) object;
				final IResource resource= element.getAdapter(IResource.class);
				if (resource != null) {
					final IMemento child= memento.createChild(RESOURCES);
					child.putInteger(RESOURCE_TYPE, resource.getType());
					child.putString(RESOURCE_PATH, resource.getFullPath().toString());
				}
			}
			if (object instanceof IResource) {
				final IResource resource= (IResource) object;
				final IMemento child= memento.createChild(RESOURCES);
				child.putInteger(RESOURCE_TYPE, resource.getType());
				child.putString(RESOURCE_PATH, resource.getFullPath().toString());
			} else if (object instanceof IWorkingSet)
				memento.createChild(WORKING_SETS).putString(WORKING_SET_NAME, ((IWorkingSet) object).getName());
			else if (object instanceof ModelProvider)
				memento.createChild(MODEL_PROVIDERS).putString(MODEL_PROVIDER_ID, ((ModelProvider) object).getId());
		}
	}
}
