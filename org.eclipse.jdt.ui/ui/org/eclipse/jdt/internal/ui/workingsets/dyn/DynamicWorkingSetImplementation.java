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
import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IPersistableElement;

/**
 * The actual implementation class of a dynamic working set.
 * <p>
 * This class should be subclassed by clients wishing to provide a  
 * dynamic working set.
 * </p>
 * @since 3.1
 */
public abstract class DynamicWorkingSetImplementation extends PlatformObject implements IPersistableElement {
	
	private IDynamicWorkingSet workingSet;
	
	/**
	 * Initializes the working set implementation with its
	 * proxy.
	 * <p>
	 * This method is for internal use only.
	 * </p> 
	 * 
	 * @param ws the proxy of this working set implementation
	 */
	public void init(IDynamicWorkingSet ws) {
		Assert.isNotNull(ws);
		Assert.isTrue(ws instanceof DynamicWorkingSet);
		workingSet= ws;
	}
	
	/**
	 * Returns the working set proxy
	 * 
	 * @return the working set
	 */
	public IDynamicWorkingSet getWorkingSet() {
		return workingSet;
	}
	
	protected void fireContentChanged() {
		((DynamicWorkingSet)workingSet).contentChanged();
	}
	
	public boolean loadEager() {
		return false;
	}
	
	public abstract IAdaptable[] getElements();
	
	public abstract boolean isEditable();
	
	public abstract void dispose();
	
	public abstract String getId();
}
