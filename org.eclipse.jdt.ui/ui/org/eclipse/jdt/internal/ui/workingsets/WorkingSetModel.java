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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IElementComparer;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;


public class WorkingSetModel {
	
	public static final IElementComparer COMPARER= new WorkingSetComparar();
	
	public static final String WORKING_SET_MODEL_CHANGED= WorkingSetModel.class.getName() + ".model_changed"; //$NON-NLS-1$
	
	private static final String TAG_WORKING_SET= WorkingSetModel.class.getName() + ".workingSet"; //$NON-NLS-1$
	private static final String TAG_WORKING_SET_NAME= WorkingSetModel.class.getName() + ".workingSet_name"; //$NON-NLS-1$
	
	private List fWorkingSets; 
	private ListenerList fListeners;
	private IPropertyChangeListener fWorkingSetManagerListener;

	private ElementMapper fElementMapper= new ElementMapper();
	
	private static class WorkingSetComparar implements IElementComparer {
		public boolean equals(Object o1, Object o2) {
			IWorkingSet w1= o1 instanceof IWorkingSet ? (IWorkingSet)o1 : null;
			IWorkingSet w2= o2 instanceof IWorkingSet ? (IWorkingSet)o2 : null;
			if (w1 == null || w2 == null)
				return o1.equals(o2);
			return w1 == w2;
		}
		public int hashCode(Object element) {
			if (element instanceof IWorkingSet)
				return System.identityHashCode(element);
			return element.hashCode();
		}
	}
	
	private static class ElementMapper {
		private Map fElementToWorkingSet= new HashMap();
		private Map fWorkingSetToElement= new HashMap();
		
		private Map fResourceToWorkingSet= new HashMap();

		public void clear() {
			fElementToWorkingSet.clear();
			fWorkingSetToElement.clear();
		}
		public IAdaptable[] put(IWorkingSet ws) {
			IAdaptable[] elements= ws.getElements();
			fWorkingSetToElement.put(new Integer(System.identityHashCode(ws)), elements);
			for (int i= 0; i < elements.length; i++) {
				IAdaptable element= elements[i];
				if (!fElementToWorkingSet.containsKey(element)) {
					fElementToWorkingSet.put(element, ws);
				}
				IResource resource= (IResource)element.getAdapter(IResource.class);
				if (resource != null && !fResourceToWorkingSet.containsKey(resource)) {
					fResourceToWorkingSet.put(resource, ws);
				}
			}
			return elements;
		}
		public IAdaptable[] remove(IWorkingSet ws) {
			IAdaptable[] elements= (IAdaptable[])fWorkingSetToElement.remove(new Integer(System.identityHashCode(ws)));
			if (elements != null) {
				for (int i= 0; i < elements.length; i++) {
					IAdaptable element= elements[i];
					fElementToWorkingSet.remove(element);
					IResource resource= (IResource)element.getAdapter(IResource.class);
					if (resource != null) {
						fResourceToWorkingSet.remove(resource);
					}
				}
			}
			return elements;
		}
		public IWorkingSet getWorkingSet(Object element) {
			return (IWorkingSet)fElementToWorkingSet.get(element);
		}
		public IWorkingSet getWorkingSetForResource(IResource resource) {
			return (IWorkingSet)fResourceToWorkingSet.get(resource);
		}
	}
	
	public WorkingSetModel() {
		fListeners= new ListenerList();
		fWorkingSetManagerListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				workingSetManagerChanged(event);
			}
		};
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(fWorkingSetManagerListener);
	}
	
	public void dispose() {
		if (fWorkingSetManagerListener != null) {
			PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(fWorkingSetManagerListener);
			fWorkingSetManagerListener= null;
		}
	}
	
	/**
     * Adds a property change listener.
     * 
     * @param listener the property change listener to add
     */
    public void addPropertyChangeListener(IPropertyChangeListener listener) {
    	fListeners.add(listener);
    }
    
    /**
     * Removes the property change listener.
     * 
     * @param listener the property change listener to remove
     */
    public void removePropertyChangeListener(IPropertyChangeListener listener) {
    	fListeners.remove(listener);
    }
    
    public IWorkingSet[] getWorkingSets() {
    	return (IWorkingSet[])fWorkingSets.toArray(new IWorkingSet[fWorkingSets.size()]);
    }
    
    public Object[] getChildren(IWorkingSet workingSet) {
    	return fElementMapper.put(workingSet);
    }
    
    public Object getParent(Object element) {
    	if (fWorkingSets.contains(element))
    		return this;
    	return fElementMapper.getWorkingSet(element);
    }
    
    public Object[] addWorkingSets(Object[] elements) {
    	List result= null;
    	for (int i= 0; i < elements.length; i++) {
    		Object element= elements[i];
    		IWorkingSet set= null;
			if (element instanceof IResource) {
    			set= fElementMapper.getWorkingSetForResource((IResource)element);
    		} else {
    			set= fElementMapper.getWorkingSet(element);
    		}
			if (set != null) {
				if (result == null)
					result= new ArrayList(Arrays.asList(elements));
				result.add(set);
			}
		}
    	if (result == null)
    		return elements;
    	return result.toArray();
    }
    
    /* package */ void setWorkingSets(IWorkingSet[] workingSets) {
    	fWorkingSets= new ArrayList(Arrays.asList(workingSets));
    	fElementMapper.clear();
    	PropertyChangeEvent event= new PropertyChangeEvent(this, WORKING_SET_MODEL_CHANGED, null, this);
    	fireEvent(event);
    }
	
	public void restoreState(IMemento memento) {
		fWorkingSets= new ArrayList();
		IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
		IMemento[] workingSets= memento == null ? null : memento.getChildren(TAG_WORKING_SET);
		if (workingSets == null || workingSets.length == 0) {
			fWorkingSets.addAll(Arrays.asList(workingSetManager.getWorkingSets()));
			return;
		}
		for (int i= 0; i < workingSets.length; i++) {
			IMemento workingSet= workingSets[i];
			fWorkingSets.add(workingSetManager.getWorkingSet(workingSet.getString(TAG_WORKING_SET_NAME)));
		}
	}
	
	public void saveState(IMemento memento) {
		for (Iterator iter= fWorkingSets.iterator(); iter.hasNext();) {
			IWorkingSet workingSet= (IWorkingSet)iter.next();
			IMemento wsm= memento.createChild(TAG_WORKING_SET);
			wsm.putString(TAG_WORKING_SET_NAME, workingSet.getName());
		}
	}
	
    private void workingSetManagerChanged(PropertyChangeEvent event) {
		if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(event.getProperty())) {
			IWorkingSet workingSet= (IWorkingSet)event.getNewValue();
			IAdaptable[] elements= fElementMapper.remove(workingSet);
			if (elements != null) {
				fireEvent(event);
			}
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(event.getProperty())) {
			// don't know what to do yet.
		}
	}
    
    private void fireEvent(PropertyChangeEvent event) {
    	Object[] listeners= fListeners.getListeners();
    	for (int i= 0; i < listeners.length; i++) {
			((IPropertyChangeListener)listeners[i]).propertyChange(event);
		}
    }
}
