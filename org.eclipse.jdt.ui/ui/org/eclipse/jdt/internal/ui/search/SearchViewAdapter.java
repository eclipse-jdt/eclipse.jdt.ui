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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;

/**
 * This class adapts a Search view to return an adapted Search view site.
 * 
 * @see org.eclipse.jdt.internal.ui.search.SearchViewSiteAdapter
 * @since 2.1
 */
class SearchViewAdapter implements IViewPart {
	
	private IWorkbenchPartSite fSite;

	/**
	 * Constructor for SearchViewAdapter.
	 */
	public SearchViewAdapter(IWorkbenchPartSite site) {
		fSite= site;
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#getSite()
	 */
	public IWorkbenchPartSite getSite() {
		return fSite;
	}

	// --------- only empty stubs below ---------

	/*
	 * @see org.eclipse.ui.IViewPart#getViewSite()
	 */
	public IViewSite getViewSite() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IViewPart#init(IViewSite)
	 */
	public void init(IViewSite site) throws PartInitException {
	}

	/*
	 * @see org.eclipse.ui.IViewPart#init(IViewSite, IMemento)
	 */
	public void init(IViewSite site, IMemento memento) throws PartInitException {
	}

	/*
	 * @see org.eclipse.ui.IViewPart#saveState(IMemento)
	 */
	public void saveState(IMemento memento) {
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#addPropertyListener(IPropertyListener)
	 */
	public void addPropertyListener(IPropertyListener listener) {
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#getTitle()
	 */
	public String getTitle() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#getTitleImage()
	 */
	public Image getTitleImage() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#getTitleToolTip()
	 */
	public String getTitleToolTip() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#removePropertyListener(IPropertyListener)
	 */
	public void removePropertyListener(IPropertyListener listener) {
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
	}

	/*
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}
}
