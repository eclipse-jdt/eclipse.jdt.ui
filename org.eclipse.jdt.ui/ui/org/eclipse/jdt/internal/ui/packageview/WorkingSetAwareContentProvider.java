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
import java.util.List;

import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;

import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;

public class WorkingSetAwareContentProvider extends PackageExplorerContentProvider {

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
	public Object[] getChildren(Object element) {
		Object[] children;
		if (element instanceof WorkingSetModel) {
			Assert.isTrue(fWorkingSetModel == element);
			return fWorkingSetModel.getWorkingSets();
		} else if (element instanceof IWorkingSet) {
			children= fWorkingSetModel.getChildren((IWorkingSet)element);
		} else {
			children= super.getChildren(element);
		}
		TreeViewer viewer= fPart.getViewer();
		List result= new ArrayList();
		for (int i= 0; i < children.length; i++) {
			Object child= children[i];
			TreeItem item;
			if ((item= (TreeItem)viewer.testFindItem(child)) == null || item.getParentItem().getData() == element) {
				result.add(child);
			}
		}
		return result.toArray();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object getParent(Object child) {
		Object result= fWorkingSetModel.getParent(child);
		if (result != null)
			return result;
		return super.getParent(child);
	}
	
	private void workingSetModelChanged(PropertyChangeEvent event) {
		if (WorkingSetModel.WORKING_SET_MODEL_CHANGED.equals(event.getProperty())) {
			postRefresh(fWorkingSetModel);
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(event.getProperty())) {
			postRefresh(event.getNewValue());
		}
	}
}
