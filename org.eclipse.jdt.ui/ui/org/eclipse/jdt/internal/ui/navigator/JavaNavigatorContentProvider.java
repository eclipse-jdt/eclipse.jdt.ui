/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.navigator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.navigator.IExtensionStateConstants.Values;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;

public class JavaNavigatorContentProvider extends
		PackageExplorerContentProvider implements IPipelinedTreeContentProvider {

	public JavaNavigatorContentProvider() {
		super(false);
	}

	public JavaNavigatorContentProvider(boolean provideMembers) {
		super(provideMembers);
	}

	public static final String JDT_EXTENSION_ID = "org.eclipse.jdt.ui.javaContent"; //$NON-NLS-1$

	private IExtensionStateModel fStateModel;

	private IPropertyChangeListener fLayoutPropertyListener;

	@Override
	public void init(ICommonContentExtensionSite commonContentExtensionSite) {
		IExtensionStateModel stateModel = commonContentExtensionSite
				.getExtensionStateModel();
		IMemento memento = commonContentExtensionSite.getMemento();

		fStateModel = stateModel;
		restoreState(memento);
		fLayoutPropertyListener = event -> {
			if (Values.IS_LAYOUT_FLAT.equals(event.getProperty())) {
				if (event.getNewValue() != null) {
					boolean newValue1 = ((Boolean) event.getNewValue()) ? true : false;
					setIsFlatLayout(newValue1);
				}
			} else if (Values.IS_LIBRARIES_NODE_SHOWN.equals(event.getProperty())) {
				if (event.getNewValue() != null) {
					boolean newValue2 = ((Boolean) event.getNewValue());
					setShowLibrariesNode(newValue2);
				}
			}
		};
		fStateModel.addPropertyChangeListener(fLayoutPropertyListener);

		setIsFlatLayout(fStateModel.getBooleanProperty(Values.IS_LAYOUT_FLAT));
		setShowLibrariesNode(fStateModel.getBooleanProperty(Values.IS_LIBRARIES_NODE_SHOWN));

		IPreferenceStore store = PreferenceConstants.getPreferenceStore();
		boolean showCUChildren = store
				.getBoolean(PreferenceConstants.SHOW_CU_CHILDREN);
		setProvideMembers(showCUChildren);
	}

	@Override
	public void dispose() {
		super.dispose();
		fStateModel.removePropertyChangeListener(fLayoutPropertyListener);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		super.inputChanged(viewer, oldInput, findInputElement(newInput));
	}

	@Override
	public Object getParent(Object element) {
		Object parent= super.getParent(element);
		if (parent instanceof IJavaModel) {
			return ((IJavaModel)parent).getWorkspace().getRoot();
		}
		if (parent instanceof IJavaProject) {
			return ((IJavaProject)parent).getProject();
		}
		return parent;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot) {
			IWorkspaceRoot root = (IWorkspaceRoot) inputElement;
			return filterResourceProjects(root.getProjects());
		} else if (inputElement instanceof IJavaModel) {
			return filterResourceProjects(((IJavaModel) inputElement).getWorkspace().getRoot().getProjects());
		}
		if (inputElement instanceof IProject) {
			return super.getElements(JavaCore.create((IProject)inputElement));
		}
		return super.getElements(inputElement);
	}

	private static IProject[] filterResourceProjects(IProject[] projects) {
		List<IProject> filteredProjects= new ArrayList<>(projects.length);
		for (IProject project : projects) {
			if (!project.isOpen() || isJavaProject(project))
				filteredProjects.add(project);
		}
		return filteredProjects.toArray(new IProject[filteredProjects.size()]);
	}

	private static boolean isJavaProject(IProject project) {
		try {
			return project.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return false;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IProject) {
			return ((IProject) element).isAccessible();
		}
		return super.hasChildren(element);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IWorkspaceRoot) {
			IWorkspaceRoot root = (IWorkspaceRoot) parentElement;
			return filterResourceProjects(root.getProjects());
		}
		if (parentElement instanceof IProject) {
			return super.getChildren(JavaCore.create((IProject)parentElement));
		}
		return super.getChildren(parentElement);
	}

	private Object findInputElement(Object newInput) {
		if (newInput instanceof IWorkspaceRoot) {
			return JavaCore.create((IWorkspaceRoot) newInput);
		}
		return newInput;
	}

	@Override
	public void restoreState(IMemento memento) {

	}

	@Override
	public void saveState(IMemento memento) {

	}

	@Override
	public void getPipelinedChildren(Object parent, Set currentChildren) {
		customize(getChildren(parent), currentChildren);
	}

	@Override
	public void getPipelinedElements(Object input, Set currentElements) {
		customize(getElements(input), currentElements);
	}

	@Override
	public Object getPipelinedParent(Object object, Object suggestedParent) {
		return getParent(object);
	}

	@Override
	public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {

		Object parent= addModification.getParent();

		if (parent instanceof IJavaProject) {
			addModification.setParent(((IJavaProject)parent).getProject());
		}

		if (parent instanceof IWorkspaceRoot) {
			deconvertJavaProjects(addModification);
		}

		convertToJavaElements(addModification);
		return addModification;
	}

	@Override
	public PipelinedShapeModification interceptRemove(
			PipelinedShapeModification removeModification) {
		deconvertJavaProjects(removeModification);
		convertToJavaElements(removeModification.getChildren());
		return removeModification;
	}

	private void deconvertJavaProjects(PipelinedShapeModification modification) {
		Set<IProject> convertedChildren = new LinkedHashSet<>();
		for (Iterator<IAdaptable> iterator = modification.getChildren().iterator(); iterator.hasNext();) {
			Object added = iterator.next();
			if(added instanceof IJavaProject) {
				iterator.remove();
				convertedChildren.add(((IJavaProject)added).getProject());
			}
		}
		modification.getChildren().addAll(convertedChildren);
	}

	/**
	 * Converts the shape modification to use Java elements.
	 *
	 *
	 * @param modification
	 *            the shape modification to convert
	 * @return returns true if the conversion took place
	 */
	private boolean convertToJavaElements(PipelinedShapeModification modification) {
		Object parent = modification.getParent();
		// As of 3.3, we no longer re-parent additions to IProject.
		if (parent instanceof IContainer) {
			IJavaElement element = convert((IContainer) parent);
			if (element != null) {
				// we don't convert the root
				if( !(element instanceof IJavaModel) && !(element instanceof IJavaProject))
					modification.setParent(element);
				return convertToJavaElements(modification.getChildren());

			}
		}
		return false;
	}

	private static IJavaElement convert(IResource resource) {
		IJavaProject javaProject= JavaCore.create(resource.getProject());
		if (javaProject == null) {
			return null;
		}
		IJavaElement javaElement= JavaCore.create(resource, javaProject);
		if (javaElement == null || !javaElement.exists()) {
			return null;
		}
		return javaElement;
	}

	/**
	 * Converts the shape modification to use Java elements.
	 *
	 *
	 * @param currentChildren
	 *            The set of current children that would be contributed or refreshed in the viewer.
	 * @return returns true if the conversion took place
	 */
	private boolean convertToJavaElements(Set<Object> currentChildren) {

		LinkedHashSet<Object> convertedChildren = new LinkedHashSet<>();
		for (Iterator<Object> childrenItr = currentChildren.iterator(); childrenItr
				.hasNext();) {
			Object child = childrenItr.next();
			// only convert IFolders and IFiles
			if (child instanceof IFolder || child instanceof IFile) {
				IJavaElement newChild = convert((IResource) child);
				if (newChild != null) {
					IJavaProject javaProject= newChild.getJavaProject();
					if (javaProject != null && javaProject.isOnClasspath(newChild)) {
						childrenItr.remove();
						convertedChildren.add(newChild);
					}
				}
			} else if (child instanceof IJavaProject) {
				childrenItr.remove();
				convertedChildren.add( ((IJavaProject)child).getProject());
			}
		}
		if (!convertedChildren.isEmpty()) {
			currentChildren.addAll(convertedChildren);
			return true;
		}
		return false;

	}

	/**
	 * Adapted from the Common Navigator Content Provider
	 *
	 * @param javaElements the java elements
	 * @param proposedChildren the proposed children
	 */
	private void customize(Object[] javaElements, Set<Object> proposedChildren) {
		List<?> elementList= Arrays.asList(javaElements);
		for (Object element : proposedChildren) {
			IResource resource= null;
			if (element instanceof IResource) {
				resource= (IResource)element;
			} else if (element instanceof IAdaptable) {
				resource= ((IAdaptable)element).getAdapter(IResource.class);
			}
			if (resource != null) {
				int i= elementList.indexOf(resource);
				if (i >= 0) {
					javaElements[i]= null;
				}
			}
		}
		for (Object element : javaElements) {
			if (element instanceof IJavaElement) {
				IJavaElement cElement= (IJavaElement)element;
				IResource resource= cElement.getResource();
				if (resource != null) {
					proposedChildren.remove(resource);
				}
				proposedChildren.add(element);
			} else if (element != null) {
				proposedChildren.add(element);
			}
		}
	}



	@Override
	public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
		return convertToJavaElements(refreshSynchronization.getRefreshTargets());

	}

	@Override
	public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
		return convertToJavaElements(updateSynchronization.getRefreshTargets());
	}

	@Override
	protected void postAdd(final Object parent, final Object element, Collection<Runnable> runnables) {
		if (parent instanceof IJavaModel)
			super.postAdd(((IJavaModel) parent).getWorkspace().getRoot(), element, runnables);
		else if (parent instanceof IJavaProject)
			super.postAdd( ((IJavaProject)parent).getProject(), element, runnables);
		else
			super.postAdd(parent, element, runnables);
	}


	@Override
	protected void postRefresh(final List<Object> toRefresh, final boolean updateLabels, Collection<Runnable> runnables) {
		int size= toRefresh.size();
		for (int i= 0; i < size; i++) {
			Object element= toRefresh.get(i);
			if (element instanceof IJavaProject) {
				toRefresh.set(i, ((IJavaProject) element).getProject());
			}
		}
		for (Iterator<Object> iter = toRefresh.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof IJavaModel) {
				iter.remove();
				toRefresh.add(((IJavaModel)element).getWorkspace().getRoot());
				super.postRefresh(toRefresh, updateLabels, runnables);
				return;
			}
		}
		super.postRefresh(toRefresh, updateLabels, runnables);
	}

}
