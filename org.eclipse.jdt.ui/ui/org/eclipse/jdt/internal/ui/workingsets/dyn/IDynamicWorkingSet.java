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

import org.eclipse.jface.util.IPropertyChangeListener;

import org.eclipse.ui.IWorkingSet;

/**
 * The set of elements of a dynamic working set can dynamically
 * change during its life time.
 * <p>
 * For compatibility reasons a dynamic working set is a sub type
 * of a working set. However due to its nature the method 
 * {@link org.eclipse.ui.IWorkingSet#setElements(IAdaptable[])} does
 * not make sense for dynamic working sets. Dynamic working sets
 * may throw an {@link java.lang.UnsupportedOperationException} when
 * this method is called.
 * </p>
 * <p>
 * To ensure lazy loading of dynamic working sets during startup its
 * implementation is split into a proxy and a real implementation.
 * Therefore the actual implementation of a dynamic working set is
 * provided through a subclass of {@link DynamicWorkingSetImplementation} 
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @Since 3.1
 */
public interface IDynamicWorkingSet extends IWorkingSet {

	public static final String PROPERTY_IMPLEMENTATION_CREATED= "implementationCreated";
	
	/**
	 * Returns the implementation object. Returns <code>null</code>
	 * if the implementation hasn't been created yet.
	 * 
	 * @return the implementation or <code>null</code>
	 */
	public DynamicWorkingSetImplementation getImplementation();
	
	/**
	 * Returns whether this working set can be edit or not.
	 * 
	 * @return <code>true</code> if the working set can be
	 *  edit; otherwise <code>false</code> is returned
	 */
	public boolean isEditable();

	/**
	 * Call by the working set manager when the working set is
	 * removed from the manager. If the working set is managed
	 * by some other client, the client is responsible for
	 * calling dispose when the working set is now longer needed.
	 * <p>
	 * Typical implementations use this hook to unregister 
	 * listeners.
	 * </p>
	 */
	public void dispose();
	
    /**
     * Adds a property change listener.
     * 
     * @param listener the property change listener to add
     */
	public void addPropertyChangeListener(IPropertyChangeListener listener);
	
    /**
     * Removes the property change listener.
     * 
     * @param listener the property change listener to remove
     */
	public void removePropertyChangeListener(IPropertyChangeListener listener);
}
