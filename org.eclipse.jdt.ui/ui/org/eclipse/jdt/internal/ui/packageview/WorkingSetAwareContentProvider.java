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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

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
	public Object getParent(Object child) {
		Object result= fWorkingSetModel.getParent(child);
		if (result != null)
			return result;
		return super.getParent(child);
	}

	/**
	 * {@inheritDoc}
	 */
	/* package */ int handleAffectedChildren(IJavaElementDelta delta, IJavaElement element) throws JavaModelException {
		int result= super.handleAffectedChildren(delta, element);
		/*
		if ((result & PARENT_REFRESH) != 0 || (result & GRANT_PARENT_REFRESH) != 0) {
			postRefresh(fWorkingSetModel.getAllParents(element), true);
		}
		*/
		return result;
	}
	
	private void workingSetModelChanged(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (WorkingSetModel.CHANGE_WORKING_SET_MODEL_CONTENT.equals(property)) {
			postRefresh(fWorkingSetModel);
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {
			postRefresh(event.getNewValue());
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(property)) {
			postRefresh(event.getNewValue());
		}
	}
}
