/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;

import org.eclipse.jdt.internal.ui.workingsets.HistoryWorkingSetUpdater;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;

public class WorkingSetAwareContentProvider extends PackageExplorerContentProvider implements IMultiElementTreeContentProvider {

	private WorkingSetModel fWorkingSetModel;
	private IPropertyChangeListener fListener;
	
	public WorkingSetAwareContentProvider(PackageExplorerPart part, boolean provideMembers, WorkingSetModel model) {
		super(part, provideMembers);
		fWorkingSetModel= model;
		fListener= new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						workingSetModelChanged(event);
					}
				};
		fWorkingSetModel.addPropertyChangeListener(fListener);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		fWorkingSetModel.removePropertyChangeListener(fListener);
		super.dispose();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof IWorkingSet)
			return true;
		return super.hasChildren(element);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object[] getChildren(Object element) {
		Object[] children;
		if (element instanceof WorkingSetModel) {
			Assert.isTrue(fWorkingSetModel == element);
			return fWorkingSetModel.getActiveWorkingSets();
		} else if (element instanceof IWorkingSet) {
			children= fWorkingSetModel.getChildren((IWorkingSet)element);
		} else {
			children= super.getChildren(element);
		}
		return children;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public TreePath[] getTreePaths(Object element) {
		if (element instanceof IWorkingSet) {
			TreePath path= new TreePath(new Object[] {element});
			return new TreePath[] {path};
		}
		List modelParents= getModelPath(element);
		List result= new ArrayList();
		for (int i= 0; i < modelParents.size(); i++) {
			result.addAll(getTreePaths(modelParents, i));
		}
		return (TreePath[])result.toArray(new TreePath[result.size()]);
	}
	
	private List getModelPath(Object element) {
		List result= new ArrayList();
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
	
	private List/*<TreePath>*/ getTreePaths(List modelParents, int index) {
		List result= new ArrayList();
		Object input= getViewerInput();
		Object element= modelParents.get(index);
		Object[] parents= fWorkingSetModel.getAllParents(element);
		for (int i= 0; i < parents.length; i++) {
			List chain= new ArrayList();
			if (!parents[i].equals(input))
				chain.add(parents[i]);
			for (int m= index; m < modelParents.size(); m++) {
				chain.add(modelParents.get(m));
			}
			result.add(new TreePath(chain.toArray()));
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object getParent(Object child) {
		Object[] parents= fWorkingSetModel.getAllParents(child);
		if(parents.length == 0)
			return super.getParent(child);
		Object first= parents[0];
		if (first instanceof IWorkingSet && HistoryWorkingSetUpdater.ID.equals(((IWorkingSet)first).getId())) {
			if (parents.length > 1) {
				return parents[1];
			} else {
				return super.getParent(child);
			}
		}
		return first;
	}
	
	protected void augmentElementToRefresh(List toRefresh, int relation, Object affectedElement) {
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
	}
	
	private void workingSetModelChanged(PropertyChangeEvent event) {
		String property= event.getProperty();
		Object newValue= event.getNewValue();
		List toRefresh= new ArrayList(1);
		if (WorkingSetModel.CHANGE_WORKING_SET_MODEL_CONTENT.equals(property)) {
			toRefresh.add(fWorkingSetModel);
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {
			toRefresh.add(newValue);
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(property)) {
			toRefresh.add(newValue);
		}
		postRefresh(toRefresh, true);
	}
}
