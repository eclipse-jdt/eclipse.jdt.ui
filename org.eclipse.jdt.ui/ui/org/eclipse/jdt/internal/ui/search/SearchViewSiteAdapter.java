/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPage;
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
 */
class SearchViewSiteAdapter implements IWorkbenchSite {
	
	private ISelectionProvider fProvider;
	private IWorkbenchSite fSite;

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
				public void addSelectionChangedListener(ISelectionChangedListener listener) {
				}
				public ISelection getSelection() {
					return convertSelection(provider.getSelection());
				}
				public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				}
				public void setSelection(ISelection selection) {
				}
			};
	}

	private ISelection convertSelection(ISelection selection) {
		Object element= SelectionUtil.getSingleElement(selection);
		if (element instanceof ISearchResultViewEntry) {
			IMarker marker= ((ISearchResultViewEntry)element).getSelectedMarker();
			try {
				IJavaElement je= JavaCore.create((String) ((IMarker) marker).getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));
				if (je != null)
					return new StructuredSelection(je);
			} catch (CoreException ex) {
				ExceptionHandler.log(ex, SearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-1$
				return null;
			}
		}
		return StructuredSelection.EMPTY;		
	}
}
