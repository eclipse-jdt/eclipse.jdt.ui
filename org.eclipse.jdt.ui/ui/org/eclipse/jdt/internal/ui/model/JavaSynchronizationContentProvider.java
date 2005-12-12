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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.IExtensionStateModel;

import org.eclipse.team.core.diff.IDiffNode;
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.mapping.ISynchronizationContext;

import org.eclipse.team.ui.mapping.SynchronizationContentProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Java-aware synchronization content provider.
 * 
 * @since 3.2
 */
public final class JavaSynchronizationContentProvider extends SynchronizationContentProvider {

	/**
	 * Returns the deltas associated with the element.
	 * 
	 * @param context
	 *            the synchronization context
	 * @param element
	 *            the element
	 * @return an array of diff nodes
	 */
	public static IDiffNode[] getDeltas(final ISynchronizationContext context, final Object element) {
		return context.getDiffTree().getDiffs(getResourceTraversals(element));
	}

	/**
	 * Returns the resource mapping for the element.
	 * 
	 * @param element
	 *            the element to get the resource mapping
	 * @return the resource mapping
	 */
	private static ResourceMapping getResourceMapping(final Object element) {
		if (element instanceof IJavaElement)
			return JavaElementResourceMapping.create((IJavaElement) element);
		if (element instanceof IAdaptable) {
			final IAdaptable adaptable= (IAdaptable) element;
			final Object adapted= adaptable.getAdapter(ResourceMapping.class);
			if (adapted instanceof ResourceMapping)
				return (ResourceMapping) adapted;
		}
		return null;
	}

	/**
	 * Returns the resource traversals for the element.
	 * 
	 * @param element
	 *            the element to get the resource traversals
	 * @return the resource traversals
	 */
	private static ResourceTraversal[] getResourceTraversals(final Object element) {
		final ResourceMapping mapping= getResourceMapping(element);
		if (mapping != null) {
			try {
				return mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, new NullProgressMonitor());
			} catch (CoreException exception) {
				JavaPlugin.log(exception);
				if (element instanceof ICompilationUnit) {
					final ICompilationUnit unit= (ICompilationUnit) element;
					return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { unit.getResource() }, IResource.DEPTH_ZERO, IResource.NONE) };
				} else if (element instanceof IPackageFragment) {
					final IPackageFragment fragment= (IPackageFragment) element;
					return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { fragment.getResource() }, IResource.DEPTH_ONE, IResource.NONE) };
				}
			}
		}
		return new ResourceTraversal[0];
	}

	/** The content provider, or <code>null</code> */
	private ITreeContentProvider fContentProvider= null;

	/** The model root, or <code>null</code> */
	private Object fModelRoot= null;

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		super.dispose();
		final ISynchronizationContext context= getContext();
		if (context != null)
			context.getDiffTree().removeDiffChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object[] getChildrenInScope(final Object parent, final Object[] children) {
		final Object[] elements= super.getChildrenInScope(parent, children);
		final ISynchronizationContext context= getContext();
		if (context != null) {
			if (parent instanceof IPackageFragment) {
				final Set set= new HashSet();
				for (int index= 0; index < elements.length; index++)
					set.add(elements[index]);
				final IResource resource= ((IPackageFragment) parent).getResource();
				if (resource != null) {
					final IResource[] members= context.getDiffTree().members(resource);
					for (int index= 0; index < members.length; index++) {
						final IDiffNode node= context.getDiffTree().getDiff(members[index]);
						if (node != null) {
							if (members[index].getType() == IResource.FILE && isInScope(parent, members[index]))
								set.add(JavaCore.create(members[index]));
						}
					}
				}
				return set.toArray(new Object[set.size()]);
			} else if (parent instanceof IPackageFragmentRoot) {
				final Set set= new HashSet();
				for (int index= 0; index < elements.length; index++)
					set.add(elements[index]);
				final IResource resource= JavaModelProvider.getResource(parent);
				if (resource != null) {
					final IResource[] members= context.getDiffTree().members(resource);
					for (int index= 0; index < members.length; index++) {
						if (members[index].getType() == IResource.FILE && isInScope(parent, members[index]))
							set.add(JavaCore.create((IFile) members[index]));
						else if (members[index].getType() == IResource.FOLDER && isInScope(parent, members[index]))
							set.add(JavaCore.create(members[index]));
						if (members[index] instanceof IFolder) {
							try {
								context.getDiffTree().accept(((IFolder) members[index]).getFullPath(), new IDiffVisitor() {

									public final boolean visit(final IDiffNode node) throws CoreException {
										final IResource current= context.getDiffTree().getResource(node);
										if (current.getType() == IResource.FILE)
											set.add(JavaCore.create(current.getParent()));
										else
											set.add(JavaCore.create(current));
										return true;
									}
								}, IResource.DEPTH_INFINITE);
							} catch (CoreException exception) {
								JavaPlugin.log(exception);
							}
						}
					}
					return set.toArray(new Object[set.size()]);
				}
			}
		}
		return elements;
	}

	/**
	 * {@inheritDoc}
	 */
	protected ITreeContentProvider getDelegateContentProvider() {
		if (fContentProvider == null)
			fContentProvider= new JavaModelContentProvider();
		return fContentProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getModelProviderId() {
		return JavaModelProvider.JAVA_MODEL_PROVIDER_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object getModelRoot() {
		if (fModelRoot == null)
			fModelRoot= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		return fModelRoot;
	}

	/**
	 * {@inheritDoc}
	 */
	protected ResourceTraversal[] getTraversals(final Object element) {
		return getResourceTraversals(element);
	}

	/**
	 * Returns whether the element has some children in the current scope.
	 * 
	 * @param element
	 *            the element
	 * @param resource
	 *            the resource
	 * @return <code>true</code> if it has some children, <code>false</code>
	 *         otherwise
	 */
	private boolean hasChildrenInScope(final Object element, final IResource resource) {
		final IResource[] roots= getScope().getRoots();
		if (element instanceof IPackageFragment) {
			for (int index= 0; index < roots.length; index++)
				if (resource.getFullPath().equals((roots[index].getFullPath().removeLastSegments(1))))
					return true;
			return false;
		}
		for (int index= 0; index < roots.length; index++) {
			if (resource.getFullPath().isPrefixOf(roots[index].getFullPath()))
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void init(final IExtensionStateModel model, final IMemento memento) {
		super.init(model, memento);
		final ISynchronizationContext context= getContext();
		if (context != null)
			context.getDiffTree().addDiffChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean isInScope(final Object parent, final Object element) {
		final IResource resource= JavaModelProvider.getResource(element);
		if (resource == null)
			return false;
		if (getScope().contains(resource))
			return true;
		if (hasChildrenInScope(element, resource))
			return true;
		return false;
	}
}
