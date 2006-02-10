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
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;

import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.ui.refactoring.model.AbstractSynchronizationContentProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
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
public final class JavaSynchronizationContentProvider extends AbstractSynchronizationContentProvider {

	/** The refactorings folder */
	private static final String NAME_REFACTORING_FOLDER= ".refactorings"; //$NON-NLS-1$

	/**
	 * Returns the diffs associated with the element.
	 * 
	 * @param context
	 *            the synchronization context
	 * @param element
	 *            the element
	 * @return an array of diffs
	 */
	static IDiff[] getDiffs(final ISynchronizationContext context, final Object element) {
		return context.getDiffTree().getDiffs(getResourceTraversals(element));
	}

	/**
	 * Returns the resource mapping for the element.
	 * 
	 * @param element
	 *            the element to get the resource mapping
	 * @return the resource mapping
	 */
	static ResourceMapping getResourceMapping(final Object element) {
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
	static ResourceTraversal[] getResourceTraversals(final Object element) {
		final ResourceMapping mapping= getResourceMapping(element);
		if (mapping != null) {
			try {
				return mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, new NullProgressMonitor());
			} catch (CoreException exception) {
				JavaPlugin.log(exception);
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
	protected Object[] getChildrenInContext(final ISynchronizationContext context, final Object parent, final Object[] children) {
		final Object[] elements= super.getChildrenInContext(context, parent, children);
		if (parent instanceof IPackageFragment)
			return getPackageFragmentChildren(context, parent, elements);
		else if (parent instanceof IPackageFragmentRoot)
			return getPackageFragmentRootChildren(context, parent, elements);
		else if (parent instanceof IJavaProject)
			return getJavaProjectChildren(context, parent, elements);
		else if (parent instanceof JavaProjectSettings)
			return getProjectSettingsChildren(context, parent, elements);
		else if (parent instanceof RefactoringHistory)
			return ((RefactoringHistory) parent).getDescriptors();
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
	 * Returns the java project children in the current scope.
	 * 
	 * @param context
	 *            the synchronization context
	 * @param parent
	 *            the parent element
	 * @param children
	 *            the child elements
	 * @return the java project children
	 */
	private Object[] getJavaProjectChildren(final ISynchronizationContext context, final Object parent, final Object[] children) {
		final LinkedList list= new LinkedList();
		for (int index= 0; index < children.length; index++)
			list.add(children[index]);
		final IResource resource= JavaModelProvider.getResource(parent);
		if (resource != null) {
			final IResourceDiffTree tree= context.getDiffTree();
			final IResource[] members= tree.members(resource);
			for (int index= 0; index < members.length; index++) {
				final int type= members[index].getType();
				if (type == IResource.FOLDER) {
					if (isInScope(context.getScope(), parent, members[index])) {
						final String name= members[index].getName();
						if (name.equals(JavaProjectSettings.NAME_SETTINGS_FOLDER)) {
							list.remove(members[index]);
							list.addFirst(new JavaProjectSettings((IJavaProject) parent));
						} else if (name.equals(NAME_REFACTORING_FOLDER)) {
							final RefactoringHistory history= getRefactorings(context, (IProject) resource, null);
							if (!history.isEmpty()) {
								list.remove(members[index]);
								list.addFirst(history);
							}
						}
					}
				}
			}
		}
		return list.toArray(new Object[list.size()]);
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
	 * Returns the package fragment children in the current scope.
	 * 
	 * @param context
	 *            the synchronization context
	 * @param parent
	 *            the parent element
	 * @param children
	 *            the child elements
	 * @return the package fragment children
	 */
	private Object[] getPackageFragmentChildren(final ISynchronizationContext context, final Object parent, final Object[] children) {
		final Set set= new HashSet();
		for (int index= 0; index < children.length; index++)
			set.add(children[index]);
		final IResource resource= ((IPackageFragment) parent).getResource();
		if (resource != null) {
			final IResourceDiffTree tree= context.getDiffTree();
			final IResource[] members= tree.members(resource);
			for (int index= 0; index < members.length; index++) {
				final int type= members[index].getType();
				if (type == IResource.FILE) {
					final IDiff diff= tree.getDiff(members[index]);
					if (diff != null && isVisible(diff)) {
						if (isInScope(context.getScope(), parent, members[index])) {
							final IJavaElement element= JavaCore.create(members[index]);
							if (element != null)
								set.add(element);
						}
					}
				}
			}
		}
		return set.toArray(new Object[set.size()]);
	}

	/**
	 * Returns the package fragment root children in the current scope.
	 * 
	 * @param context
	 *            the synchronization context
	 * @param parent
	 *            the parent element
	 * @param children
	 *            the child elements
	 * @return the package fragment root children
	 */
	private Object[] getPackageFragmentRootChildren(final ISynchronizationContext context, final Object parent, final Object[] children) {
		final Set set= new HashSet();
		for (int index= 0; index < children.length; index++)
			set.add(children[index]);
		final IResource resource= JavaModelProvider.getResource(parent);
		if (resource != null) {
			final IResourceDiffTree tree= context.getDiffTree();
			final IResource[] members= tree.members(resource);
			for (int index= 0; index < members.length; index++) {
				final int type= members[index].getType();
				final boolean contained= isInScope(context.getScope(), parent, members[index]);
				final boolean visible= isVisible(context, members[index]);
				if (type == IResource.FILE && contained && visible) {
					final IJavaElement element= JavaCore.create((IFile) members[index]);
					if (element != null)
						set.add(element);
				} else if (type == IResource.FOLDER && contained && visible) {
					final IJavaElement element= JavaCore.create(members[index]);
					if (element != null)
						set.add(element);
				}
				if (type == IResource.FOLDER) {
					final IFolder folder= (IFolder) members[index];
					tree.accept(folder.getFullPath(), new IDiffVisitor() {

						public final boolean visit(final IDiff diff) {
							if (isVisible(diff)) {
								final IResource current= tree.getResource(diff);
								if (current != null) {
									final int kind= current.getType();
									if (kind == IResource.FILE) {
										final IJavaElement element= JavaCore.create(current.getParent());
										if (element != null)
											set.add(element);
									} else {
										final IJavaElement element= JavaCore.create(current);
										if (element != null)
											set.add(element);
									}
								}
							}
							return true;
						}
					}, IResource.DEPTH_INFINITE);
				}
			}
			return set.toArray(new Object[set.size()]);
		}
		return children;
	}

	/**
	 * Returns the project settings children in the current scope.
	 * 
	 * @param context
	 *            the synchronization context
	 * @param parent
	 *            the parent element
	 * @param children
	 *            the child elements
	 * @return the project settings children
	 */
	private Object[] getProjectSettingsChildren(final ISynchronizationContext context, final Object parent, final Object[] children) {
		final Set set= new HashSet();
		for (int index= 0; index < children.length; index++)
			set.add(children[index]);
		final IResource resource= JavaModelProvider.getResource(parent);
		if (resource != null) {
			final IResourceDiffTree tree= context.getDiffTree();
			final IResource[] members= tree.members(resource);
			for (int index= 0; index < members.length; index++) {
				final int type= members[index].getType();
				if (type == IResource.FILE) {
					final IDiff diff= tree.getDiff(members[index]);
					if (diff != null && isVisible(diff)) {
						if (isInScope(context.getScope(), parent, members[index]))
							set.add(members[index]);
					}
				}
			}
		}
		return set.toArray(new Object[set.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	protected ResourceTraversal[] getTraversals(final ISynchronizationContext context, final Object object) {
		return getResourceTraversals(object);
	}

	/**
	 * Returns whether the element has some children in the current scope.
	 * 
	 * @param scope
	 *            the synchronization scope
	 * @param element
	 *            the element
	 * @param resource
	 *            the resource
	 * @return <code>true</code> if it has some children, <code>false</code>
	 *         otherwise
	 */
	private boolean hasChildrenInScope(final ISynchronizationScope scope, final Object element, final IResource resource) {
		final IResource[] roots= scope.getRoots();
		final IPath path= resource.getFullPath();
		if (element instanceof IPackageFragment) {
			for (int index= 0; index < roots.length; index++)
				if (path.equals(roots[index].getFullPath().removeLastSegments(1)))
					return true;
			return false;
		}
		for (int index= 0; index < roots.length; index++) {
			if (path.isPrefixOf(roots[index].getFullPath()))
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean isInScope(final ISynchronizationScope scope, final Object parent, final Object element) {
		final IResource resource= JavaModelProvider.getResource(element);
		if (resource == null)
			return false;
		if (scope.contains(resource))
			return true;
		if (hasChildrenInScope(scope, element, resource))
			return true;
		return false;
	}
}