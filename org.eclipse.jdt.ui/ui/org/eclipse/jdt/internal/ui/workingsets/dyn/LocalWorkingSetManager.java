/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets.dyn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.IWorkbenchConstants;

/**
 * A working set manager stores working sets and provides property change
 * notification when a working set is added or removed. Working sets are
 * persisted whenever one is added or removed.
 * 
 * @see IWorkingSetManager
 * @since 2.0
 */
public class LocalWorkingSetManager extends AbstractWorkingSetManager {

	public static final String TAG_WORKING_SET_REFERENCE= "workingSetReference"; //$NON-NLS-1$
	public static final String TAG_WORKING_SET_ORDER= "workingSetOrder"; //$NON-NLS-1$

	public static final String CHANGE_WORKING_SET_MANAGER_CONTENT_CHANGED= "workingSetManagerContentChanged"; //$NON-NLS-1$

	private List fWorkingSets= new ArrayList();

	public LocalWorkingSetManager() {
	}

	public LocalWorkingSetManager(IMemento memento) {
		restoreState(memento);
	}

	public void addWorkingSet(IWorkingSet workingSet) {
		Assert.isTrue(!fWorkingSets.contains(workingSet), "working set already registered"); //$NON-NLS-1$
		fWorkingSets.add(workingSet);
		firePropertyChange(IWorkingSetManager.CHANGE_WORKING_SET_ADD, null, workingSet);
	}

	public void removeWorkingSet(IWorkingSet workingSet) {
		boolean workingSetRemoved= fWorkingSets.remove(workingSet);
		if (workingSetRemoved) {
			firePropertyChange(IWorkingSetManager.CHANGE_WORKING_SET_REMOVE, workingSet, null);
		}
	}

	public void dispose() {
		IWorkingSet[] globals= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
		for (Iterator iter= fWorkingSets.iterator(); iter.hasNext();) {
			IWorkingSet element= (IWorkingSet)iter.next();
			if (!isGlobal(globals, element) && element instanceof IDynamicWorkingSet) {
				((IDynamicWorkingSet)element).dispose();
			}
		}
	}

	private boolean isGlobal(IWorkingSet[] globals, IWorkingSet element) {
		for (int i= 0; i < globals.length; i++) {
			if (globals[i] == element)
				return true;
		}
		return false;
	}

	public boolean contains(IWorkingSet ws) {
		return fWorkingSets.contains(ws);
	}

	public IWorkingSet getWorkingSetByName(String name) {
		for (Iterator iter= fWorkingSets.iterator(); iter.hasNext();) {
			IWorkingSet workingSet= (IWorkingSet)iter.next();
			if (name.equals(workingSet.getName()))
				return workingSet;
		}
		return null;
	}

	public IWorkingSet getWorkingSetById(String id) {
		for (Iterator iter= fWorkingSets.iterator(); iter.hasNext();) {
			IWorkingSet workingSet= (IWorkingSet)iter.next();
			if (id.equals(workingSet.getId()))
				return workingSet;
		}
		return null;
	}

	public IWorkingSet[] getWorkingSets() {
		return (IWorkingSet[])fWorkingSets.toArray(new IWorkingSet[fWorkingSets.size()]);
	}

	public void setWorkingSets(IWorkingSet[] ws) {
		if (hasChanged(ws)) {
			fWorkingSets= new ArrayList(Arrays.asList(ws));
			firePropertyChange(CHANGE_WORKING_SET_MANAGER_CONTENT_CHANGED, this, this);
		}
	}

	private boolean hasChanged(IWorkingSet[] ws) {
		if (ws.length != fWorkingSets.size())
			return true;
		for (int i= 0; i < ws.length; i++) {
			if (!ws[i].equals(fWorkingSets.get(i)))
				return true;
		}
		return false;
	}

	public void saveState(IMemento memento) {
		for (Iterator iter= fWorkingSets.iterator(); iter.hasNext();) {
			IWorkingSet workingSet= (IWorkingSet)iter.next();
			IMemento m= memento.createChild(TAG_WORKING_SET_ORDER);
			m.putString(IWorkbenchConstants.TAG_NAME, workingSet.getName());
		}

		List globalWorkingSets= new ArrayList(Arrays.asList(PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets()));

		for (Iterator iter= fWorkingSets.iterator(); iter.hasNext();) {
			IWorkingSet workingSet= (IWorkingSet)iter.next();

			if (globalWorkingSets.contains(workingSet)) {
				IMemento workingSetMemento= memento.createChild(TAG_WORKING_SET_REFERENCE);
				workingSetMemento.putString(IWorkbenchConstants.TAG_NAME, workingSet.getName());
			} else {
				IMemento workingSetMemento= memento.createChild(IWorkbenchConstants.TAG_WORKING_SET);
				workingSetMemento.putString(IWorkbenchConstants.TAG_FACTORY_ID, workingSet.getFactoryId());
				workingSet.saveState(workingSetMemento);
			}
		}
	}

	private void restoreState(IMemento memento) {
		IMemento[] children= memento.getChildren(TAG_WORKING_SET_ORDER);
		String[] order= new String[children.length];
		for (int i= 0; i < children.length; i++) {
			order[i]= children[i].getString(IWorkbenchConstants.TAG_NAME);
		}
		Map wsm= new HashMap();
		children= memento.getChildren(TAG_WORKING_SET_REFERENCE);
		for (int i= 0; i < children.length; i++) {
			String name= children[i].getString(IWorkbenchConstants.TAG_NAME);
			IWorkingSet reference= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
			if (reference != null)
				wsm.put(reference.getName(), reference);
		}
		children= memento.getChildren(IWorkbenchConstants.TAG_WORKING_SET);
		for (int i= 0; i < children.length; i++) {
			IWorkingSet ws= restoreWorkingSet(children[i]);
			if (ws != null)
				wsm.put(ws.getName(), ws);
		}
		fWorkingSets= new ArrayList(wsm.size());
		for (int i= 0; i < order.length; i++) {
			Object ws= wsm.get(order[i]);
			if (ws != null)
				fWorkingSets.add(ws);
		}
	}

	/**
	 * Persists all working sets and fires a property change event for the
	 * changed working set. Should only be called by
	 * org.eclipse.ui.internal.WorkingSet.
	 * 
	 * @param changedWorkingSet the working set that has changed
	 * @param propertyChangeId the changed property. one of
	 *        CHANGE_WORKING_SET_CONTENT_CHANGE and
	 *        CHANGE_WORKING_SET_NAME_CHANGE
	 */
	public void workingSetChanged(IWorkingSet changedWorkingSet, String propertyChangeId) {
		firePropertyChange(propertyChangeId, null, changedWorkingSet);
	}
}