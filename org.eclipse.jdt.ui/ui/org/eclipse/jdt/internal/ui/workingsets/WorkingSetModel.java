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

import org.eclipse.jdt.internal.ui.workingsets.dyn.IDynamicWorkingSet;
import org.eclipse.jdt.internal.ui.workingsets.dyn.LocalWorkingSetManager;
import org.eclipse.jdt.internal.ui.workingsets.dyn.WorkingSetManagerExt;


public class WorkingSetModel {
	
	public static final IElementComparer COMPARER= new WorkingSetComparar();
	
	private static final String TAG_ACTIVE_WORKING_SETS= WorkingSetModel.class.getName() + ".workingSets.active"; //$NON-NLS-1$
	private static final String TAG_INACTIVE_WORKING_SETS= WorkingSetModel.class.getName() + ".workingSets.inactive"; //$NON-NLS-1$
	
	private LocalWorkingSetManager fInactiveWorkingSets;
	private LocalWorkingSetManager fActiveWorkingSets;
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
			fResourceToWorkingSet.clear();
		}
		public void put(IWorkingSet ws) {
			Integer workingSetKey= new Integer(System.identityHashCode(ws));
			if (fWorkingSetToElement.containsKey(workingSetKey))
				return;
			IAdaptable[] elements= ws.getElements();
			fWorkingSetToElement.put(workingSetKey, elements);
			for (int i= 0; i < elements.length; i++) {
				IAdaptable element= elements[i];
				addToMap(fElementToWorkingSet, element, ws);
				IResource resource= (IResource)element.getAdapter(IResource.class);
				if (resource != null) {
					addToMap(fResourceToWorkingSet, resource, ws);
				}
			}
		}
		public void putAll(IWorkingSet[] workingSets) {
			if (fWorkingSetToElement.size() == workingSets.length)
				return;
			for (int i= 0; i < workingSets.length; i++) {
				put(workingSets[i]);
			}
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
		public IWorkingSet getFirstWorkingSet(Object element) {
			return (IWorkingSet)getFirstElement(fElementToWorkingSet, element);
		}
		public List getAllWorkingSets(Object element) {
			return getAllElements(fElementToWorkingSet, element);
		}
		public IWorkingSet getFirstWorkingSetForResource(IResource resource) {
			return (IWorkingSet)getFirstElement(fResourceToWorkingSet, resource);
		}
		public List getAllWorkingSetsForResource(IResource resource) {
			return getAllElements(fResourceToWorkingSet, resource);
		}
		private void addToMap(Map map, IAdaptable key, IWorkingSet value) {
			Object obj= map.get(key);
			if (obj == null) {
				map.put(key, value);
			} else if (obj instanceof IWorkingSet) {
				List l= new ArrayList(2);
				l.add(obj);
				l.add(value);
				map.put(key, l);
				
			} else if (obj instanceof List) {
				((List)obj).add(value);
			}
		}
		private Object getFirstElement(Map map, Object key) {
			Object obj= map.get(key);
			if (obj instanceof List) 
				return ((List)obj).get(0);
			return obj;
		}
		private List getAllElements(Map map, Object key) {
			Object obj= map.get(key);
			if (obj instanceof List)
				return (List)obj;
			if (obj == null)
				return new ArrayList(0);
			List result= new ArrayList(1);
			result.add(obj);
			return result;
		}
	}
	
	public WorkingSetModel() {
		fActiveWorkingSets= new LocalWorkingSetManager();
    	IDynamicWorkingSet history= WorkingSetManagerExt.
			createDynamicWorkingSet("History", new HistoryWorkingSet());
    	history.setId("org.eclipse.jdt.internal.ui.HistoryWorkingSet"); //$NON-NLS-1$
    	fActiveWorkingSets.addWorkingSet(history);
		IDynamicWorkingSet others= WorkingSetManagerExt.
			createDynamicWorkingSet("Others", new OthersWorkingSet(this));
    	fActiveWorkingSets.addWorkingSet(others);
    	fInactiveWorkingSets= new LocalWorkingSetManager();
		initialize();
	}
	
	public WorkingSetModel(IMemento memento) {
		restoreState(memento);
		IWorkingSet ws= fActiveWorkingSets.getWorkingSetById(OthersWorkingSet.ID);
		if (ws == null)
			ws= fInactiveWorkingSets.getWorkingSetById(OthersWorkingSet.ID);
		if (ws != null) {
			((IDynamicWorkingSet)ws).addPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					if (IDynamicWorkingSet.PROPERTY_IMPLEMENTATION_CREATED.equals(event.getProperty())) {
						((OthersWorkingSet)event.getNewValue()).init(WorkingSetModel.this);
					}
				}
			});
		}
		initialize();
	}
	
	private void initialize() {
		fListeners= new ListenerList();
		fWorkingSetManagerListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				workingSetManagerChanged(event);
			}
		};
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(fWorkingSetManagerListener);
		fActiveWorkingSets.addPropertyChangeListener(fWorkingSetManagerListener);
	}
	
	public void dispose() {
		if (fWorkingSetManagerListener != null) {
			PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(fWorkingSetManagerListener);
			fActiveWorkingSets.removePropertyChangeListener(fWorkingSetManagerListener);
			fActiveWorkingSets.dispose();
			fInactiveWorkingSets.dispose();
			fWorkingSetManagerListener= null;
		}
		
	}
	
	//---- model relationships ---------------------------------------
	
    public Object[] getChildren(IWorkingSet workingSet) {
    	fElementMapper.put(workingSet);
    	return workingSet.getElements();
    }
    
    public Object getParent(Object element) {
    	if (element instanceof IWorkingSet && fActiveWorkingSets.contains((IWorkingSet)element))
    		return this;
    	return fElementMapper.getFirstWorkingSet(element);
    }
    
    public Object[] getAllParents(Object element) {
    	if (element instanceof IWorkingSet && fActiveWorkingSets.contains((IWorkingSet)element))
    		return new Object[] {this};
    	return fElementMapper.getAllWorkingSets(element).toArray();
    }
    
    public Object[] addWorkingSets(Object[] elements) {
    	fElementMapper.putAll(fActiveWorkingSets.getWorkingSets());
    	List result= null;
    	for (int i= 0; i < elements.length; i++) {
    		Object element= elements[i];
    		List sets= null;
			if (element instanceof IResource) {
    			sets= fElementMapper.getAllWorkingSetsForResource((IResource)element);
    		} else {
    			sets= fElementMapper.getAllWorkingSets(element);
    		}
			if (sets != null && sets.size() > 0) {
				if (result == null)
					result= new ArrayList(Arrays.asList(elements));
				result.addAll(sets);
			}
		}
    	if (result == null)
    		return elements;
    	return result.toArray();
    }
    
    //---- working set management -----------------------------------
    
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
    
    public IWorkingSet[] getActiveWorkingSets() {
    	return fActiveWorkingSets.getWorkingSets();
    }
    
    public IWorkingSet[] getAllWorkingSets() {
    	List result= new ArrayList();
    	result.addAll(Arrays.asList(fActiveWorkingSets.getWorkingSets()));
    	result.addAll(Arrays.asList(fInactiveWorkingSets.getWorkingSets()));
    	IWorkingSet[] globals= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
    	for (int i= 0; i < globals.length; i++) {
			if ("org.eclipse.jdt.ui.JavaWorkingSetPage".equals(globals[i].getId()) && !result.contains(globals[i])) //$NON-NLS-1$
				result.add(globals[i]);
		}
    	return (IWorkingSet[])result.toArray(new IWorkingSet[result.size()]);
    }
    
    
    /* package */ void setActiveWorkingSets(IWorkingSet[] workingSets) {
    	List newInactive= new ArrayList(Arrays.asList(fActiveWorkingSets.getWorkingSets()));
    	for (int i= 0; i < workingSets.length; i++) {
			newInactive.remove(workingSets[i]);
			fInactiveWorkingSets.removeWorkingSet(workingSets[i]);
		}
    	fActiveWorkingSets.setWorkingSets(workingSets);
    	for (Iterator iter= newInactive.iterator(); iter.hasNext();) {
			fInactiveWorkingSets.addWorkingSet((IWorkingSet)iter.next());
		}
    }
	
	public void saveState(IMemento memento) {
		fActiveWorkingSets.saveState(memento.createChild(TAG_ACTIVE_WORKING_SETS));
		fInactiveWorkingSets.saveState(memento.createChild(TAG_INACTIVE_WORKING_SETS));
	}
	
	private void restoreState(IMemento memento) {
		fActiveWorkingSets= new LocalWorkingSetManager(memento.getChild(TAG_ACTIVE_WORKING_SETS));
		fInactiveWorkingSets= new LocalWorkingSetManager(memento.getChild(TAG_INACTIVE_WORKING_SETS));
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
		} else if (LocalWorkingSetManager.CHANGE_WORKING_SET_MANAGER_CONTENT_CHANGED.equals(event.getProperty())) {
			fireEvent(event);
		}
	}
    
    void fireEvent(PropertyChangeEvent event) {
    	Object[] listeners= fListeners.getListeners();
    	for (int i= 0; i < listeners.length; i++) {
			((IPropertyChangeListener)listeners[i]).propertyChange(event);
		}
    }
}
