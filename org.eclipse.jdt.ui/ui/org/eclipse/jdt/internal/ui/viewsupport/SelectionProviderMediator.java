/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;


public class SelectionProviderMediator implements ISelectionProvider {
	
	private Viewer[] fViewers;
	private Listener fListener;
	
	private Viewer fViewerInFocus;
	private ArrayList fSelectionChangedListeners;
	
	public SelectionProviderMediator(Viewer[] viewers) {
		fViewers= viewers;
		fListener= new Listener();
		installListeners();
		
		fSelectionChangedListeners= null;
		fViewerInFocus= null;
	}
	
	private void installListeners() {		
		for (int i= 0; i < fViewers.length; i++) {
			Viewer viewer= fViewers[i];
			viewer.addSelectionChangedListener(fListener);
			Control control= viewer.getControl();
			Assert.isNotNull(control);
			control.addFocusListener(fListener);	
		}
	}
	
	/**
	 * @see ISelectionProvider#addSelectionChangedListener
	 */
	 
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if (fSelectionChangedListeners == null) {
			fSelectionChangedListeners= new ArrayList(4);
		}
		
		if (!fSelectionChangedListeners.contains(listener)) {
			fSelectionChangedListeners.add(listener);
		}
	}
	
	/**
	 * @see ISelectionProvider#removeSelectionChangedListener
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		if (fSelectionChangedListeners != null) {
			fSelectionChangedListeners.add(listener);
		}
	}	
		
		
	/**
	 * @see ISelectionProvider#getSelection
	 */
	public ISelection getSelection() {
		if (fViewerInFocus != null) {
			return fViewerInFocus.getSelection();
		} else {
			return StructuredSelection.EMPTY;
		}
	}
	
	/**
	 * @see ISelectionProvider#setSelection
	 */
	public void setSelection(ISelection selection) {
		if (fViewerInFocus != null) {
			fViewerInFocus.setSelection(selection);
		}
	}	
	
	private void fireSelectionChanged() {
		if (fSelectionChangedListeners != null) {
			SelectionChangedEvent event= new SelectionChangedEvent(this, getSelection());
			
			for (int i= 0; i < fSelectionChangedListeners.size(); i++) {
				ISelectionChangedListener listener= (ISelectionChangedListener)fSelectionChangedListeners.get(i);
				listener.selectionChanged(event);
			}
		}
	}
	
	private void focusChanged(Viewer viewer) {
		if (viewer != fViewerInFocus) {
			fViewerInFocus= viewer;
			fireSelectionChanged();
		}
	}
	
	private class Listener implements ISelectionChangedListener, FocusListener {
		
		/**
	 	 * @see ISelectionChangedListener#selectionChanged
	 	 */		
		public void selectionChanged(SelectionChangedEvent event) {
			ISelectionProvider provider= event.getSelectionProvider();
			if (provider == fViewerInFocus) {
				fireSelectionChanged();
			}
		}
		
	    /**
	     * @see FocusListener#focusGained
	     */
	    public void focusGained(FocusEvent e) {    	
	    	Widget control= e.widget;
	    	for (int i= 0; i < fViewers.length; i++) {
	    		if (fViewers[i].getControl() == control) {
	    			focusChanged(fViewers[i]);
	    			return;
	    		}
	    	}
	    }
	
	    /**
	     * @see FocusListener#focusLost
	     */
	    public void focusLost(FocusEvent e) {
	    	focusChanged(null);
	    }		
		
		
	}
		
		
	

}