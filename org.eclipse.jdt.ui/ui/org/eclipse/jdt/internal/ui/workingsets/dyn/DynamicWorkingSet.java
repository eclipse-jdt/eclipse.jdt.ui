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
package org.eclipse.jdt.internal.ui.workingsets.dyn;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.IWorkbenchConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.WorkingSetManager;

public class DynamicWorkingSet extends AbstractWorkingSet implements IDynamicWorkingSet {

    private static final String FACTORY_ID = "org.eclipse.ui.internal.DynamicWorkingSet"; //$NON-NLS-1$
    
    private static final String TAG_IMPLEMENTATION= "dynamicWorkingSetImplementation"; //$NON-NLS-1$
    private static final String TAG_IMPLEMENTATION_ID= "dynamicWorkingSetImplementationId"; //$NON-NLS-1$
    private static final String TAG_LOAD_IMPLEMENTATION_EAGER= "dynamicWorkingSetLoadImplementationEager"; //$NON-NLS-1$
    
	private DynamicWorkingSetImplementation implementation;
	
	private IMemento workingSetMemento;
	
	private String editPageId;
	
	private ListenerList fListeners;
	
    /**
     * Creates a new dynamic working set.
     * 
     * @param name the name of the new working set. Should not have 
     * 	leading or trailing whitespace.
     * @param impl the actual provider of the elements of the
     *  working set
     */
    /* package */ DynamicWorkingSet(String name, DynamicWorkingSetImplementation impl) {
    	super(name);
    	Assert.isNotNull(impl, "Implementation must not be null"); //$NON-NLS-1$
    	implementation= impl;
    	implementation.init(this);
    }
    
    /* package */ DynamicWorkingSet(String name, IMemento memento) {
    	super(name);
    	Assert.isNotNull(memento, "memento must not be null"); //$NON-NLS-1$
    	workingSetMemento= memento;
    	if (new Boolean(workingSetMemento.getString(TAG_LOAD_IMPLEMENTATION_EAGER)).booleanValue())
    		restoreImplementation();
    }

    /**
	 * {@inheritDoc}
	 */
	public DynamicWorkingSetImplementation getImplementation() {
		return implementation;
	}
    
	/**
	 * {@inheritDoc}
	 */
	public void setId(String pageId) {
	    editPageId = pageId;
	    if (editPageId == null) {
	    	editPageId= "org.eclipse.ui.resourceWorkingSetPage"; //$NON-NLS-1$
	    }
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId() {
		if (editPageId != null)
			return editPageId;
		if (implementation != null)
			return implementation.getId();
	    return workingSetMemento.getString(TAG_IMPLEMENTATION_ID);
	}

	/**
	 * {@inheritDoc}
	 */
	public IAdaptable[] getElements() {
		if (implementation == null)
			restoreImplementation();
		return implementation.getElements();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setElements(IAdaptable[] elements) {
		throw new UnsupportedOperationException("Can't set the content of a dynamic working set"); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	public String getFactoryId() {
		return FACTORY_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (fListeners == null)
			fListeners= new ListenerList();
		fListeners.add(listener);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		if (fListeners == null)
			return;
		fListeners.remove(listener);
		if (fListeners.size() == 0)
			fListeners= null;
	}
	
	private void firePropertyChange(String property, Object oldItem, Object newItem) {
		if (fListeners == null)
			return;
		PropertyChangeEvent event= new PropertyChangeEvent(this, property, oldItem, newItem);
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((IPropertyChangeListener)listeners[i]).propertyChange(event);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		if (implementation != null) {
			memento.putString(TAG_LOAD_IMPLEMENTATION_EAGER, Boolean.toString(implementation.loadEager()));
			IMemento implementationMemento= memento.createChild(TAG_IMPLEMENTATION);
			implementationMemento.putString(TAG_IMPLEMENTATION_ID, implementation.getId());
			implementationMemento.putString(IWorkbenchConstants.TAG_FACTORY_ID, implementation.getFactoryId());
			implementation.saveState(implementationMemento);
		} else {
			memento.putMemento(workingSetMemento);
		}
	}
	
	protected void restoreImplementation() {
		super.restoreState(workingSetMemento);
		IMemento implementationMemento= workingSetMemento.getChild(TAG_IMPLEMENTATION);
		String factoryID= implementationMemento.getString(IWorkbenchConstants.TAG_FACTORY_ID);
		IElementFactory factory = PlatformUI.getWorkbench().getElementFactory(factoryID);
		implementation= (DynamicWorkingSetImplementation)factory.createElement(implementationMemento);
		implementation.init(this);
		firePropertyChange(PROPERTY_IMPLEMENTATION_CREATED, null, implementation);
		workingSetMemento= null;
	}
	
	public void contentChanged() {
        WorkingSetManager workingSetManager = (WorkingSetManager)WorkbenchPlugin.getDefault().getWorkingSetManager();
        workingSetManager.workingSetChanged(this, IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE);
	}

	public boolean isEditable() {
		if (implementation == null)
			restoreImplementation();
		return implementation.isEditable();
	}
	
	public void dispose() {
		if (implementation != null)
			implementation.dispose();
	}
}
