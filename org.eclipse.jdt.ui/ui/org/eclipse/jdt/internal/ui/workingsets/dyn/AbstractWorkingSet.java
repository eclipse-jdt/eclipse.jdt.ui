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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.internal.IWorkbenchConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.WorkingSetManager;
import org.eclipse.ui.internal.registry.WorkingSetDescriptor;
import org.eclipse.ui.internal.registry.WorkingSetRegistry;

/**
 * An abstract super implementation for static and dynamic working sets.
 * 
 * @since 3.1
 */
public abstract class AbstractWorkingSet implements IAdaptable, IWorkingSet {

	private String name;
	
    protected AbstractWorkingSet(String name) {
        Assert.isNotNull(name, "name must not be null"); //$NON-NLS-1$
        this.name = name;
	}
	
	public void setName(String newName) {
	
	    Assert.isNotNull(newName, "Working set name must not be null"); //$NON-NLS-1$
	    name = newName;
	    WorkingSetManager workingSetManager = (WorkingSetManager) WorkbenchPlugin
	            .getDefault().getWorkingSetManager();
	    workingSetManager.workingSetChanged(this,
	            IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE);
	}

	public String getName() {
	    return name;
	}

	/**
	 * Returns the working set icon. Currently, this is one of the icons specified in the extensions  of the org.eclipse.ui.workingSets extension point.  The extension is identified using the value returned by <code>getId()</code>.  Returns <code>null</code> if no icon has been specified in the  extension or if <code>getId()</code> returns <code>null</code>. 
	 * @return  the working set icon or <code>null</code>.
	 * @since  2.1 
	 */
	public ImageDescriptor getImage() {
	    WorkingSetRegistry registry = WorkbenchPlugin.getDefault().getWorkingSetRegistry();
	    WorkingSetDescriptor descriptor = null;
	
	    String descriptorId= getId();
	    if (descriptorId == null)
	    	descriptorId = "org.eclipse.ui.resourceWorkingSetPage"; //$NON-NLS-1$
	
		descriptor = registry.getWorkingSetDescriptor(descriptorId);
		if (descriptor == null) {
			return null;
		}
	    return descriptor.getIcon();
	}

	/**
	 * Returns the receiver if the requested type is either IWorkingSet  or IPersistableElement.
	 * @param adapter  the requested type
	 * @return  the receiver if the requested type is either IWorkingSet  or IPersistableElement.
	 */
	public Object getAdapter(Class adapter) {
	    if (adapter == IWorkingSet.class
	            || adapter == IPersistableElement.class) {
	        return this;
	    }
	    return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void saveState(IMemento memento) {
        memento.putString(IWorkbenchConstants.TAG_NAME, name);
        if (getId() != null)
        	memento.putString(IWorkbenchConstants.TAG_EDIT_PAGE_ID, getId());
	}
	
	protected void restoreState(IMemento memento) {
		// name and page id is restored outside and will be set
		// via the corresponding setter methods.
	}
}
