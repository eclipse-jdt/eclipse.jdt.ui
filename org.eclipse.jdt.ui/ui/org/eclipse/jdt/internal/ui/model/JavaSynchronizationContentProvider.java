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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.ui.refactoring.model.AbstractSynchronizationContentProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Java-aware synchronization content provider.
 *
 * @since 3.2
 */
public final class JavaSynchronizationContentProvider extends AbstractSynchronizationContentProvider implements IPipelinedTreeContentProvider {

	/** The refactorings folder */
//	private static final String NAME_REFACTORING_FOLDER= ".refactorings"; //$NON-NLS-1$

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
		if (mapping != null)
			try {
				return mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, new NullProgressMonitor());
			} catch (final CoreException exception) {
				JavaPlugin.log(exception);
			}
		return new ResourceTraversal[0];
	}

	/** The content provider, or <code>null</code> */
	private ITreeContentProvider fContentProvider= null;

	/** The model root, or <code>null</code> */
	private Object fModelRoot= null;

	/**
	 * Returns the java element associated with the project.
	 *
	 * @param project the project, can be <code>null</code>
	 * @return the associated java element, or <code>null</code> if the project is not a Java
	 *         project
	 */
	private IJavaProject asJavaProject(final IProject project) {
		if (project == null)
			return null;

		try {
			if (project.getDescription().hasNature(JavaCore.NATURE_ID))
				return JavaCore.create(project);
		} catch (final CoreException exception) {
			// Only log the error for projects that are accessible (i.e. exist and are open)
			if (project.isAccessible())
				JavaPlugin.log(exception);
		}
		return null;
	}

	/**
	 * Converts the shape modification to use java elements.
	 *
	 * @param modification
	 *            the shape modification to convert
	 */
	private void convertToJavaElements(final PipelinedShapeModification modification) {
		final Object parent= modification.getParent();
		if (parent instanceof IResource) {
			final IJavaElement project= asJavaProject(((IResource) parent).getProject());
			if (project != null) {
				modification.getChildren().clear();
				return;
			}
		}
		if (parent instanceof ISynchronizationContext) {
			final Set<IJavaElement> result= new HashSet<>();
			for (final Iterator<IAdaptable> iterator= modification.getChildren().iterator(); iterator.hasNext();) {
				final Object element= iterator.next();
				if (element instanceof IProject) {
					final IJavaElement project= asJavaProject((IProject) element);
					if (project != null) {
						iterator.remove();
						result.add(project);
					}
				}
			}
			modification.getChildren().addAll(result);
		}
	}

	/**
	 * Converts the viewer update to use java elements.
	 *
	 * @param update
	 *            the viewer update to convert
	 * @return <code>true</code> if any elements have been converted,
	 *         <code>false</code> otherwise
	 */
	private boolean convertToJavaElements(final PipelinedViewerUpdate update) {
		final Set<IJavaElement> result= new HashSet<>();
		for (final Iterator<IAdaptable> iterator= update.getRefreshTargets().iterator(); iterator.hasNext();) {
			final Object element= iterator.next();
			if (element instanceof IProject) {
				final IJavaElement project= asJavaProject((IProject) element);
				if (project != null) {
					iterator.remove();
					result.add(project);
				}
			}
		}
		update.getRefreshTargets().addAll(result);
		return !result.isEmpty();
	}

	@Override
	public void diffsChanged(final IDiffChangeEvent event, final IProgressMonitor monitor) {
		syncExec(() -> handleChange(event), getViewer().getControl());
	}

	/**
	 * Returns all the existing projects that contain additions,
	 * removals or deletions.
	 *
	 * @param event
	 *            the event
	 * @return the projects that contain changes
	 */
	private IJavaProject[] getChangedProjects(final IDiffChangeEvent event) {
		final Set<IJavaProject> result= new HashSet<>();
		for (IDiff change : event.getChanges()) {
			final IResource resource = ResourceDiffTree.getResourceFor(change);
			if (resource != null) {
				final IJavaProject project= asJavaProject(resource.getProject());
				if (project != null)
					result.add(project);
			}
		}
		for (IDiff addition : event.getAdditions()) {
			final IResource resource = ResourceDiffTree.getResourceFor(addition);
			if (resource != null) {
				final IJavaProject project= asJavaProject(resource.getProject());
				if (project != null)
					result.add(project);
			}
		}
		for (IPath path : event.getRemovals()) {
			if (path.segmentCount() > 0) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
				// Only consider projects that still exist
				if (project.exists()) {
					final IJavaProject javaProject= asJavaProject(project.getProject());
					if (javaProject != null)
						result.add(javaProject);
				}
			}
		}
		return result.toArray(new IJavaProject[result.size()]);
	}

	@Override
	protected Object[] getChildrenInContext(final ISynchronizationContext context, final Object parent, final Object[] children) {
		final Object[] elements= super.getChildrenInContext(context, parent, children);
		if (parent instanceof IPackageFragment)
			return getPackageFragmentChildren(context, parent, elements);
		else if (parent instanceof IPackageFragmentRoot)
			return getPackageFragmentRootChildren(context, parent, elements);
		else if (parent instanceof IJavaProject)
			return getJavaProjectChildren(context, parent, elements);
		else if (parent instanceof RefactoringHistory)
			return ((RefactoringHistory) parent).getDescriptors();
		// It may be the case that the elements are folders that have a corresponding
		// source folder in which case they should be filtered out
		return getFilteredElements(parent, elements);
	}

	/**
	 * Returns the filtered elements.
	 *
	 * @param parent
	 *            the parent element
	 * @param children
	 *            the child elements
	 * @return the filtered elements
	 */
	private Object[] getFilteredElements(final Object parent, final Object[] children) {
		final List<Object> result= new ArrayList<>(children.length);
		for (Object child : children) {
			if (child instanceof IFolder) {
				if (!(JavaCore.create((IFolder) child) instanceof IPackageFragmentRoot)) {
					result.add(child);
				}
			} else {
				result.add(child);
			}
		}
		return result.toArray();
	}

	@Override
	protected ITreeContentProvider getDelegateContentProvider() {
		if (fContentProvider == null)
			fContentProvider= new JavaModelContentProvider();
		return fContentProvider;
	}

	/**
	 * Returns the projects that used to have changes in the diff tree
	 * but have been deleted from the workspace.
	 *
	 * @param event
	 *            the event
	 * @return the deleted projects
	 */
	private Set<IProject> getDeletedProjects(final IDiffChangeEvent event) {
		final Set<IProject> result= new HashSet<>();
		for (IPath path : event.getRemovals()) {
			if (path.segmentCount() > 0) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
				if (!project.isAccessible())
					result.add(project);
			}
		}
		return result;
	}

	/**
	 * Since the this content provider overrides the resource content provider,
	 * this method is only invoked when the resource content provider is
	 * disabled. In this case, we still want the Java projects to appear at the
	 * root of the view.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public Object[] getElements(Object parent) {
		if (parent instanceof ISynchronizationContext)
			// Put the resource projects directly under the context
			parent= getModelRoot();
		return super.getElements(parent);
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
		final LinkedList<Object> list= new LinkedList<>();
		for (Object child : children) {
			if (child instanceof IPackageFragment) {
				final IPackageFragment fragment= (IPackageFragment) child;
				if (getChildren(fragment).length == 0)
					continue;
			}
			// We need to check whether a folder has non-fragment children (bug 138767)
			if (child instanceof IFolder) {
				IFolder folder= (IFolder) child;
				if (getChildren(folder).length == 0)
					continue;
			}
			list.add(child);
		}

		final IResource resource= JavaModelProvider.getResource(parent);
		if (resource != null) {
			final IResourceDiffTree tree= context.getDiffTree();
			for (IResource child : tree.members(resource)) {
				if (isVisible(context, child)) {
					if (hasPhantomFolder(tree, child) && ! containsAsResource(list, child)) {
						// Add any phantom resources that are visible
						list.add(child);
					}


//					if (members[index] instanceof IFolder) {
//						final IFolder folder= (IFolder) members[index];
//						if (folder.getName().equals(NAME_REFACTORING_FOLDER)) {
//							final RefactoringHistory history= getRefactorings(context, (IProject) resource, null);
//							if (!history.isEmpty()) {
//								list.remove(folder);
//								list.addFirst(history);
//							}
//						}
//					}
				}
			}

		}
		return list.toArray(new Object[list.size()]);
	}

	private boolean containsAsResource(List<Object> list, IResource child) {
		for (Object element : list) {
			if (child.equals(element))
				return true;
			if (element instanceof IJavaElement) {
				IJavaElement javaChild = (IJavaElement)element;
				try {
					if (child.equals(javaChild.getCorrespondingResource()))
						return true;
				} catch (JavaModelException e) {
					continue;
				}
			}
		}
		return false;
	}

	private boolean hasPhantomFolder(IResourceDiffTree tree, IResource child) {
		if (!child.exists())
			return true;
		final boolean[] found = new boolean[] { false };
		tree.accept(child.getFullPath(), delta -> {
			IResource treeResource = ResourceDiffTree.getResourceFor(delta);
			if (treeResource.getType()==IResource.FILE && !treeResource.getParent().exists()){
				found[0] = true;
				return false;
			}

			return true;
		}, IResource.DEPTH_INFINITE);
		return found[0];
	}


	@Override
	protected String getModelProviderId() {
		return JavaModelProvider.JAVA_MODEL_PROVIDER_ID;
	}

	@Override
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
		final Set<Object> set= new HashSet<>(Arrays.asList(children));
		IPackageFragment packageFragment= (IPackageFragment) parent;
		IResource resource= packageFragment.getResource();
		if (resource != null) {
			final IResourceDiffTree tree= context.getDiffTree();
			for (IResource member : tree.members(resource)) {
				final int type= member.getType();
				if (type == IResource.FILE) {
					final IDiff diff= tree.getDiff(member);
					if (diff != null && isVisible(diff)) {
						if (isInScope(context.getScope(), parent, member)) {
							final IJavaElement element= JavaCore.create(member);
							if (element == null) {
								if (!packageFragment.isDefaultPackage()) {
									set.add(member); //
								}
							} else {
								set.add(element);
							}
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
		final Set<Object> set= new HashSet<>();
		for (Object child : children) {
			if (child instanceof IPackageFragment) {
				IPackageFragment fragment = (IPackageFragment) child;
				if (fragment.isOpen() && getChildren(fragment).length == 0)
					// Don't add the default package unless it has children
					continue;
			}
			set.add(child);
		}
		final IResource resource= JavaModelProvider.getResource(parent);
		if (resource != null) {
			final IResourceDiffTree tree= context.getDiffTree();
			for (IResource member : tree.members(resource)) {
				final int type= member.getType();
				final boolean contained= isInScope(context.getScope(), parent, member);
				final boolean visible= isVisible(context, member);
				if (type == IResource.FILE && contained && visible) {
					// If the file is not a compilation unit add it.
					// (compilation units are always children of packages so they
					// don't need to be added here)
					final IJavaElement element= JavaCore.create((IFile) member);
					if (element == null) {
						set.add(member);
					}
				} else if (type == IResource.FOLDER && contained && visible && tree.getDiff(member) != null) {
					// If the folder is out-of-sync, add it
					final IJavaElement element= JavaCore.create(member);
					if (element != null)
						set.add(element);
				}
				if (type == IResource.FOLDER) {
					// If the folder contains java elements, add it
					final IFolder folder= (IFolder) member;
					tree.accept(folder.getFullPath(), diff -> {
						if (isVisible(diff)) {
							final IResource current= tree.getResource(diff);
							if (current != null) {
								final int kind= current.getType();
								if (kind == IResource.FILE) {
									final IJavaElement element1= JavaCore.create(current.getParent());
									if (element1 != null)
										set.add(element1);
								} else {
									final IJavaElement element2= JavaCore.create(current);
									if (element2 != null)
										set.add(element2);
								}
							}
						}
						return true;
					}, IResource.DEPTH_INFINITE);
				}
			}
			return set.toArray(new Object[set.size()]);
		}
		return children;
	}

	@Override
	public void getPipelinedChildren(final Object parent, final Set children) {
		if (parent instanceof ISynchronizationContext) {
			// When a context is the root, the resource content provider returns
			// projects as direct children. We should replace any projects that
			// are Java projects with an IJavaProject
			final Set<IJavaElement> result= new HashSet<>(children.size());
			for (final Iterator<Object> iterator= children.iterator(); iterator.hasNext();) {
				final Object element= iterator.next();
				if (element instanceof IProject) {
					final IJavaElement java= asJavaProject((IProject) element);
					if (java != null) {
						iterator.remove();
						result.add(java);
					}
				}
				if (element instanceof IFolder) {
					IFolder folder= (IFolder) element;
					IJavaElement javaElement= JavaCore.create(folder);
					// If the folder is also a package, don't show it
					// as a folder since it will be shown as a package
					if (javaElement instanceof IPackageFragmentRoot) {
						iterator.remove();
					}
				}
			}
			children.addAll(result);
		} else if (parent instanceof ISynchronizationScope) {
			// When the root is a scope, we should return the
			// Java model provider so all model providers appear
			// at the root of the viewer.
			children.add(getModelProvider());
		} else if (parent instanceof IFolder) {
			// Remove any children that are also source folders so they
			// don't appear twice
			for (final Iterator<Object> iterator= children.iterator(); iterator.hasNext();) {
				final Object element= iterator.next();
				if (element instanceof IFolder) {
					IFolder folder = (IFolder) element;
					IJavaElement javaElement = JavaCore.create(folder);
					if (javaElement instanceof IPackageFragmentRoot) {
						iterator.remove();
					}
				}
			}
		}
	}

	@Override
	public void getPipelinedElements(final Object element, final Set elements) {
		getPipelinedChildren(element, elements);
	}

	@Override
	public Object getPipelinedParent(final Object element, final Object parent) {
		if (element instanceof IJavaElement)
			return getParent(element);
		return parent;
	}

	@Override
	protected ResourceTraversal[] getTraversals(final ISynchronizationContext context, final Object object) {
		return getResourceTraversals(object);
	}

	/**
	 * Returns the visible projects.
	 *
	 * @return the visible projects
	 */
	private Set<IJavaProject> getVisibleProjects() {
		final Set<IJavaProject> result= new HashSet<>();
		for (TreeItem child : ((TreeViewer) getViewer()).getTree().getItems()) {
			final Object data= child.getData();
			if (data instanceof IJavaProject)
				result.add((IJavaProject) data);
		}
		return result;
	}

	/**
	 * Handles a diff change event.
	 *
	 * @param event
	 *            the event
	 */
	private void handleChange(final IDiffChangeEvent event) {
		final Set<IJavaProject> existing= getVisibleProjects();
		// Get all existing and open projects that contain changes
		// and determine what needs to be done to the project
		// (i.e. add, remove or refresh)
		final IJavaProject[] changed= getChangedProjects(event);
		final List<IJavaProject> refreshes= new ArrayList<>(changed.length);
		final List<IJavaProject> additions= new ArrayList<>(changed.length);
		final List<IJavaProject> removals= new ArrayList<>(changed.length);
		for (IJavaProject project : changed) {
			if (hasVisibleChanges(event.getTree(), project)) {
				if (existing.contains(project))
					refreshes.add(project);
				else
					additions.add(project);
			} else
				removals.add(project);
		}
		// Remove any java projects that correspond to deleted or closed projects
		final Set<IProject> removed= getDeletedProjects(event);
		for (IJavaProject element : existing) {
			if (removed.contains(element.getResource()))
				removals.add(element);
		}

		if (!removals.isEmpty() || !additions.isEmpty() || !refreshes.isEmpty()) {
			final TreeViewer viewer= (TreeViewer) getViewer();
			final Tree tree= viewer.getTree();
			try {
				tree.setRedraw(false);
				if (!additions.isEmpty())
					viewer.add(viewer.getInput(), additions.toArray());
				if (!removals.isEmpty())
					viewer.remove(viewer.getInput(), removals.toArray());
				if (!refreshes.isEmpty()) {
					for (IJavaProject iJavaProject : refreshes)
						viewer.refresh(iJavaProject);
				}
			} finally {
				tree.setRedraw(true);
			}
		}
	}

	@Override
	public boolean hasChildren(final Object element) {
		if (element instanceof ICompilationUnit || element instanceof IFile || element instanceof RefactoringDescriptorProxy || element instanceof RefactoringDescriptor)
			return false;
		return super.hasChildren(element);
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
			for (IResource root : roots) {
				if (path.equals(root.getFullPath().removeLastSegments(1))) {
					return true;
				}
			}
			return false;
		}
		for (IResource root : roots) {
			if (path.isPrefixOf(root.getFullPath())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Has the java project visible changes?
	 *
	 * @param tree
	 *            the diff tree
	 * @param project
	 *            the java project
	 * @return <code>true</code> if it has visible changes, <code>false</code>
	 *         otherwise
	 */
	private boolean hasVisibleChanges(final IDiffTree tree, final IJavaProject project) {
		return tree.hasMatchingDiffs(project.getResource().getFullPath(), new FastDiffFilter() {

			@Override
			public boolean select(final IDiff diff) {
				return isVisible(diff);
			}
		});
	}

	@Override
	public PipelinedShapeModification interceptAdd(final PipelinedShapeModification modification) {
		convertToJavaElements(modification);
		return modification;
	}

	@Override
	public boolean interceptRefresh(final PipelinedViewerUpdate update) {
		return convertToJavaElements(update);
	}

	@Override
	public PipelinedShapeModification interceptRemove(final PipelinedShapeModification modification) {
		convertToJavaElements(modification);
		return modification;
	}

	@Override
	public boolean interceptUpdate(final PipelinedViewerUpdate anUpdateSynchronization) {
		return convertToJavaElements(anUpdateSynchronization);
	}

	@Override
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

	/**
	 * Executes the given runnable.
	 *
	 * @param runnable
	 *            the runnable
	 * @param control
	 *            the control
	 */
	private void syncExec(final Runnable runnable, final Control control) {
		if (control != null && !control.isDisposed())
			control.getDisplay().syncExec(() -> {
				if (!control.isDisposed())
					BusyIndicator.showWhile(control.getDisplay(), runnable);
			});
	}
}
