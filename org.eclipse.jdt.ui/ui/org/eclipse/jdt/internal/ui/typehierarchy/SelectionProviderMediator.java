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
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

/**
 * A selection provider for viewparts with more that one viewer.
 * Tracks the focus of the viewers to provide the correct selection.
 */
public class SelectionProviderMediator implements ISelectionProvider {

	private class InternalListener implements ISelectionChangedListener, FocusListener {
		/*
	 	 * @see ISelectionChangedListener#selectionChanged
	 	 */		
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}
		
	    /*
	     * @see FocusListener#focusGained
	     */
	    public void focusGained(FocusEvent e) {    	
	    	doFocusChanged(e.widget);
	    }
	
	    /*
	     * @see FocusListener#focusLost
	     */
	    public void focusLost(FocusEvent e) {
	    	// do not reset due to focus behaviour on GTK
	    	//fViewerInFocus= null;
	    }
	}
	
	private Viewer[] fViewers;
	private InternalListener fListener;
	
	private Viewer fViewerInFocus;
	private ListenerList fSelectionChangedListeners;
	
	/**
	 * @param viewers All viewers that can provide a selection
	 */
	public SelectionProviderMediator(Viewer[] viewers) {
		Assert.isNotNull(viewers);
		fViewers= viewers;
		fListener= new InternalListener();
		fSelectionChangedListeners= new ListenerList(4);
		fViewerInFocus= null;		

		for (int i= 0; i < fViewers.length; i++) {
			Viewer viewer= fViewers[i];
			viewer.addSelectionChangedListener(fListener);
			Control control= viewer.getControl();
			control.addFocusListener(fListener);	
		}
	}
	
	
	private void doFocusChanged(Widget control) {
	    	for (int i= 0; i < fViewers.length; i++) {
	    		if (fViewers[i].getControl() == control) {
	    			propagateFocusChanged(fViewers[i]);
	    			return;
	    		}
	    	}		
	}
	
	private void doSelectionChanged(SelectionChangedEvent event) {
			ISelectionProvider provider= event.getSelectionProvider();
			if (provider == fViewerInFocus) {
				fireSelectionChanged();
			}
	}	
	
	private void propagateFocusChanged(Viewer viewer) {
		if (viewer != fViewerInFocus) { // Ok to compare by idendity
			fViewerInFocus= viewer;
			fireSelectionChanged();
		}
	}
	
	private void fireSelectionChanged() {
		if (fSelectionChangedListeners != null) {
			SelectionChangedEvent event= new SelectionChangedEvent(this, getSelection());
			
			Object[] listeners= fSelectionChangedListeners.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				ISelectionChangedListener listener= (ISelectionChangedListener) listeners[i];
				listener.selectionChanged(event);
			}
		}
	}	
	
	/*
	 * @see ISelectionProvider#addSelectionChangedListener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {	
		fSelectionChangedListeners.add(listener);
	}
	
	/*
	 * @see ISelectionProvider#removeSelectionChangedListener
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.remove(listener);
	}	
		
	/*
	 * @see ISelectionProvider#getSelection
	 */
	public ISelection getSelection() {
		if (fViewerInFocus != null) {
			return fViewerInFocus.getSelection();
		} else {
			return StructuredSelection.EMPTY;
		}
	}
	
	/*
	 * @see ISelectionProvider#setSelection
	 */
	public void setSelection(ISelection selection) {
		if (fViewerInFocus != null) {
			fViewerInFocus.setSelection(selection);
		}
	}

	/**
	 * Returns the viewer in focus or null if no viewer has the focus
	 */	
	public Viewer getViewerInFocus() {
		return fViewerInFocus;
	}
	
}
