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

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;
import org.eclipse.ui.dialogs.IWorkingSetPage;
import org.eclipse.ui.internal.IWorkbenchConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.WorkingSet;
import org.eclipse.ui.internal.dialogs.WorkingSetEditWizard;
import org.eclipse.ui.internal.registry.WorkingSetRegistry;


public class AbstractWorkingSetManager {

	private ListenerList propertyChangeListeners= new ListenerList();

	public void addPropertyChangeListener(IPropertyChangeListener listener) {
	    propertyChangeListeners.add(listener);
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
	    propertyChangeListeners.remove(listener);
	}

	/**
	 * Notify property change listeners about a change to the list of  working sets.
	 * @param changeId  one of  IWorkingSetManager#CHANGE_WORKING_SET_ADD  IWorkingSetManager#CHANGE_WORKING_SET_REMOVE IWorkingSetManager#CHANGE_WORKING_SET_CONTENT_CHANGE  IWorkingSetManager#CHANGE_WORKING_SET_NAME_CHANGE
	 * @param oldValue  the removed working set or null if a working set  was added or changed.
	 * @param newValue  the new or changed working set or null if a working  set was removed.
	 */
	protected void firePropertyChange(String changeId, Object oldValue, Object newValue) {
	    final PropertyChangeEvent event = new PropertyChangeEvent(this,
	            changeId, oldValue, newValue);
	
	    Display.getDefault().syncExec(new Runnable() {
	        public void run() {
	            Object[] listeners = propertyChangeListeners.getListeners();
	            for (int i = 0; i < listeners.length; i++) {
	                ((IPropertyChangeListener) listeners[i])
	                        .propertyChange(event);
	            }
	        }
	    });
	}

	public IWorkingSet createWorkingSet(String name, IAdaptable[] elements) {
	    return new WorkingSet(name, elements);
	}

	/**
	 * @see org.eclipse.ui.IWorkingSetManager#createWorkingSetEditWizard(org.eclipse.ui.IWorkingSet)
	 * @since  2.1
	 */
	public IWorkingSetEditWizard createWorkingSetEditWizard(IWorkingSet workingSet) {
	    String editPageId = workingSet.getId();
	    WorkingSetRegistry registry = WorkbenchPlugin.getDefault()
	            .getWorkingSetRegistry();
	    IWorkingSetPage editPage = null;
	
	    if (editPageId != null) {
	        editPage = registry.getWorkingSetPage(editPageId);
	    }
	    if (editPage == null) {
	        editPage = registry.getDefaultWorkingSetPage();
	        if (editPage == null) {
	            return null;
	        }
	    }
	    WorkingSetEditWizard editWizard = new WorkingSetEditWizard(editPage);
	    editWizard.setSelection(workingSet);
	    return editWizard;
	}
	
     /**
     * Recreates a working set from the persistence store.
     * 
     * @param memento the persistence store
     * @return the working set created from the memento or null if
     * 	creation failed.
     */
    protected IWorkingSet restoreWorkingSet(IMemento memento) {
        String factoryID = memento
                .getString(IWorkbenchConstants.TAG_FACTORY_ID);

        if (factoryID == null) {
            // if the factory id was not set in the memento
            // then assume that the memento was created using
            // IMemento.saveState, and should be restored using WorkingSetFactory
        	
        	// TODO should be a reference to WorkingSet.FACTORY_ID; 
            factoryID = "org.eclipse.ui.internal.WorkingSetFactory"; //$NON-NLS-1$ 
            	
        }
        IElementFactory factory = PlatformUI.getWorkbench().getElementFactory(
                factoryID);
        if (factory == null) {
            WorkbenchPlugin
                    .log("Unable to restore working set - cannot instantiate factory: " + factoryID); //$NON-NLS-1$
            return null;
        }
        IAdaptable adaptable = factory.createElement(memento);
        if (adaptable == null) {
            WorkbenchPlugin
                    .log("Unable to restore working set - cannot instantiate working set: " + factoryID); //$NON-NLS-1$
            return null;
        }
        if ((adaptable instanceof IWorkingSet) == false) {
            WorkbenchPlugin
                    .log("Unable to restore working set - element is not an IWorkingSet: " + factoryID); //$NON-NLS-1$
            return null;
        }
        return (IWorkingSet) adaptable;
    }
}
