/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreePath;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;

public class WorkingSetAwareContentProvider extends PackageExplorerContentProvider implements IMultiElementTreeContentProvider {

	private WorkingSetModel fWorkingSetModel;
	private IPropertyChangeListener fListener;

	public WorkingSetAwareContentProvider(boolean provideMembers, WorkingSetModel model) {
		super(provideMembers);
		fWorkingSetModel= model;
		fListener= this::workingSetModelChanged;
		fWorkingSetModel.addPropertyChangeListener(fListener);
	}

	@Override
	public void dispose() {
		fWorkingSetModel.removePropertyChangeListener(fListener);
		super.dispose();
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IWorkingSet)
			return true;
		return super.hasChildren(element);
	}

	@Override
	public Object[] getChildren(Object element) {
		Object[] children;
		if (element instanceof WorkingSetModel) {
			Assert.isTrue(fWorkingSetModel == element);
			return fWorkingSetModel.getActiveWorkingSets();
		} else if (element instanceof IWorkingSet) {
			children= getWorkingSetChildren((IWorkingSet)element);
		} else {
			children= super.getChildren(element);
		}
		return children;
	}

	private Object[] getWorkingSetChildren(IWorkingSet set) {
		IAdaptable[] children= fWorkingSetModel.getChildren(set);
		Set<IAdaptable> result= new HashSet<>(children.length);
		for (IAdaptable child : children) {
			if (child instanceof IProject) {
				processResource((IProject) child, result); // also add closed projects
			} else if (child instanceof IResource) {
				IProject project= ((IResource) child).getProject();
				if (project.isOpen()) {
					processResource((IResource) child, result);
				}
			} else if (child instanceof IJavaProject) {
				result.add(child); // also add closed projects
			} else if (child instanceof IJavaElement) {
				IJavaElement elem= (IJavaElement) child;
				IProject project= getProject(elem);
				if (project != null && project.isOpen()) {
					result.add(elem);
				}
			} else {
				IProject project= child.getAdapter(IProject.class);
				if (project != null) {
					processResource(project, result);
				}
			}
		}
		return result.toArray();
	}

	private void processResource(IResource resource, Collection<IAdaptable> result) {
		IJavaElement elem= JavaCore.create(resource);
		if (elem != null && elem.exists()) {
			result.add(elem);
		} else {
			result.add(resource);
		}
	}

	private IProject getProject(IJavaElement element) {
		IJavaProject project= element.getJavaProject();
		if (project == null)
			return null;
		return project.getProject();
	}

	@Override
	public TreePath[] getTreePaths(Object element) {
		if (element instanceof IWorkingSet) {
			TreePath path= new TreePath(new Object[] {element});
			return new TreePath[] {path};
		}
		List<Object> modelParents= getModelPath(element);
		List<TreePath> result= new ArrayList<>();
		for (int i= 0; i < modelParents.size(); i++) {
			result.addAll(getTreePaths(modelParents, i));
		}
		return result.toArray(new TreePath[result.size()]);
	}

	private List<Object> getModelPath(Object element) {
		List<Object> result= new ArrayList<>();
		result.add(element);
		Object parent= super.getParent(element);
		Object input= getViewerInput();
		// stop at input or on JavaModel. We never visualize it anyway.
		while (parent != null && !parent.equals(input) && !(parent instanceof IJavaModel)) {
			result.add(parent);
			parent= super.getParent(parent);
		}
		Collections.reverse(result);
		return result;
	}

	private List<TreePath> getTreePaths(List<Object> modelParents, int index) {
		List<TreePath> result= new ArrayList<>();
		Object input= getViewerInput();
		Object element= modelParents.get(index);
		for (Object parent : fWorkingSetModel.getAllParents(element)) {
			List<Object> chain= new ArrayList<>();
			if (!parent.equals(input)) {
				chain.add(parent);
			}
			for (int m= index; m < modelParents.size(); m++) {
				chain.add(modelParents.get(m));
			}
			result.add(new TreePath(chain.toArray()));
		}
		return result;
	}

	@Override
	public Object getParent(Object child) {
		Object[] parents= fWorkingSetModel.getAllParents(child);
		if(parents.length == 0)
			return super.getParent(child);
		Object first= parents[0];
		return first;
	}

	@Override
	protected void augmentElementToRefresh(List<Object> toRefresh, int relation, Object affectedElement) {
		// we are refreshing the JavaModel and are in working set mode.
		if (JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).equals(affectedElement)) {
			toRefresh.remove(affectedElement);
			toRefresh.add(fWorkingSetModel);
		} else if (relation == GRANT_PARENT) {
			Object parent= internalGetParent(affectedElement);
			if (parent != null) {
				toRefresh.addAll(Arrays.asList(fWorkingSetModel.getAllParents(parent)));
			}
		}
		List<IAdaptable> nonProjetTopLevelElemens= fWorkingSetModel.getNonProjectTopLevelElements();
		if (nonProjetTopLevelElemens.isEmpty())
			return;
		List<Object> toAdd= new ArrayList<>();
		for (IAdaptable iAdaptable : nonProjetTopLevelElemens) {
			Object element= iAdaptable;
			if (isChildOf(element, toRefresh))
				toAdd.add(element);
		}
		toRefresh.addAll(toAdd);
	}

	private void workingSetModelChanged(PropertyChangeEvent event) {
		String property= event.getProperty();
		Object newValue= event.getNewValue();
		List<Object> toRefresh= new ArrayList<>(1);
		if (property != null) {
			switch (property) {
				case WorkingSetModel.CHANGE_WORKING_SET_MODEL_CONTENT:
					toRefresh.add(fWorkingSetModel);
					break;
				case IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE:
				case IWorkingSetManager.CHANGE_WORKING_SET_LABEL_CHANGE:
					toRefresh.add(newValue);
					break;
				default:
					break;
			}
		}
		ArrayList<Runnable> runnables= new ArrayList<>();
		postRefresh(toRefresh, true, runnables);
		executeRunnables(runnables);
	}

	private boolean isChildOf(Object element, List<Object> potentialParents) {
		// Calling super get parent to bypass working set mapping
		Object parent= super.getParent(element);
		if (parent == null)
			return false;
		for (Object potentialParent : potentialParents) {
			while(parent != null) {
				if (parent.equals(potentialParent))
					return true;
				parent= super.getParent(parent);
			}

		}
		return false;
	}
}