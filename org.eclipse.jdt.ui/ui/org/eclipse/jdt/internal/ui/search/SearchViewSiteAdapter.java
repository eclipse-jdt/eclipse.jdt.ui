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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.search.ui.ISearchResultViewEntry;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

/**
 * This class adapts a Search view site.
 * It converts selection of Search view entries to
 * be a selection of Java elements.
 * 
 * @since 2.0
 */
class SearchViewSiteAdapter extends PlatformObject implements IWorkbenchPartSite, IAdaptable {
	
	private ISelectionProvider fProvider;
	private IWorkbenchSite fSite;
	
	private ISelectionChangedListener fListener;

	public SearchViewSiteAdapter(IWorkbenchSite site){
		fSite= site;
		setSelectionProvider(site.getSelectionProvider());
	}
	
	public IWorkbenchPage getPage() {
		return fSite.getPage();
	}

	public ISelectionProvider getSelectionProvider() {
		return fProvider;
	}

	public Shell getShell() {
		return JavaPlugin.getActiveWorkbenchShell();
	}

	public IWorkbenchWindow getWorkbenchWindow() {
		return fSite.getWorkbenchWindow();
	}
	
	public void setSelectionProvider(final ISelectionProvider provider) {
		Assert.isNotNull(provider);
		fProvider= 
			new ISelectionProvider() {
				public void addSelectionChangedListener(final ISelectionChangedListener listener) {
					fListener=
						new ISelectionChangedListener() {
							public void selectionChanged(SelectionChangedEvent event) {
								listener.selectionChanged(new SelectionChangedEvent(fProvider, convertSelection(event.getSelection())));
							}
						};
					provider.addSelectionChangedListener(fListener);
				}
				public ISelection getSelection() {
					return convertSelection(provider.getSelection());
				}
				public void removeSelectionChangedListener(ISelectionChangedListener listener) {
					provider.removeSelectionChangedListener(fListener);
				}
				public void setSelection(ISelection selection) {
				}
			};
	}

	private ISelection convertSelection(ISelection selection) {
		Object element= SelectionUtil.getSingleElement(selection);
		if (element instanceof ISearchResultViewEntry) {
			IMarker marker= ((ISearchResultViewEntry)element).getSelectedMarker();
			if (marker != null && marker.exists())
				try {
					IJavaElement je= JavaCore.create((String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));
					if (je != null)
						return new StructuredSelection(je);
				} catch (CoreException ex) {
					ExceptionHandler.log(ex, SearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-1$
				}
		}
		return StructuredSelection.EMPTY;		
	}
	
	// --------- only empty stubs below ---------
		
	/*
	 * @see org.eclipse.ui.IWorkbenchPartSite#getId()
	 */
	public String getId() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPartSite#getKeyBindingService()
	 */
	public IKeyBindingService getKeyBindingService() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPartSite#getPluginId()
	 */
	public String getPluginId() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPartSite#getRegisteredName()
	 */
	public String getRegisteredName() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPartSite#registerContextMenu(MenuManager, ISelectionProvider)
	 */
	public void registerContextMenu(MenuManager menuManager, ISelectionProvider selectionProvider) {
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPartSite#registerContextMenu(String, MenuManager, ISelectionProvider)
	 */
	public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider) {
	}
}
