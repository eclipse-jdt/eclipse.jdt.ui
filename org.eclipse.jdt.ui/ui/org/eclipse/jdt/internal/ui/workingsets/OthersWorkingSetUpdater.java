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
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.IWorkingSetUpdater;
import org.eclipse.ui.PlatformUI;

public class OthersWorkingSetUpdater implements IWorkingSetUpdater {
	
	public static final String ID= "org.eclipse.jdt.internal.ui.OthersWorkingSet";  //$NON-NLS-1$
	
	private IWorkingSet fWorkingSet;
	private WorkingSetModel fWorkingSetModel;
	
	private class ResourceChangeListener implements IResourceChangeListener {
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta= event.getDelta();
			IResourceDelta[] affectedChildren= delta.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.REMOVED, IResource.PROJECT);
			if (affectedChildren.length > 0) {
				updateElements();
			}
		}
	}
	private IResourceChangeListener fResourceChangeListener;
	
	private class WorkingSetListener implements IPropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(event.getProperty())) {
				if (event.getNewValue() != fWorkingSet) {
					updateElements();
				}
			}
		}
	}
	private IPropertyChangeListener fWorkingSetListener;
	
	/**
	 * {@inheritDoc}
	 */
	public void add(IWorkingSet workingSet) {
		Assert.isTrue(fWorkingSet == null);
		fWorkingSet= workingSet;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean remove(IWorkingSet workingSet) {
		Assert.isTrue(fWorkingSet == workingSet);
		fWorkingSet= null;
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean contains(IWorkingSet workingSet) {
		return fWorkingSet == workingSet;
	}
	
	public void init(WorkingSetModel model) {
		fWorkingSetModel= model;
		fResourceChangeListener= new ResourceChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fResourceChangeListener, IResourceChangeEvent.POST_CHANGE);
		fWorkingSetListener= new WorkingSetListener();
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(fWorkingSetListener);
		updateElements();
	}
	
	public void dispose() {
		if (fResourceChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fResourceChangeListener);
			fResourceChangeListener= null;
		}
		if (fWorkingSetListener != null) {
			PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(fWorkingSetListener);
			fWorkingSetListener= null;
		}
		
	}
	
	public void updateElements() {
		List result= new ArrayList();
		Set projects= new HashSet();
		IWorkingSet[] workingSets= fWorkingSetModel.getActiveWorkingSets();
		for (int i= 0; i < workingSets.length; i++) {
			if (workingSets[i] == fWorkingSet) continue;
			IAdaptable[] elements= workingSets[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				IAdaptable element= elements[j];
				IResource resource= (IResource)element.getAdapter(IResource.class);
				if (resource != null && resource.getType() == IResource.PROJECT) {
					projects.add(resource);
				}
			}
		}
		IJavaModel model= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		try {
			IJavaProject[] jProjects= model.getJavaProjects();
			for (int i= 0; i < jProjects.length; i++) {
				if (!projects.contains(jProjects[i].getProject()))
					result.add(jProjects[i]);
			}
			Object[] rProjects= model.getNonJavaResources();
			for (int i= 0; i < rProjects.length; i++) {
				if (!projects.contains(rProjects[i]))
					result.add(rProjects[i]);
			}
		} catch (JavaModelException e) {
		}
		fWorkingSet.setElements((IAdaptable[])result.toArray(new IAdaptable[result.size()]));
	}
}