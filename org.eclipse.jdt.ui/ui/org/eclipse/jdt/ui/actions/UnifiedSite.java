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
package org.eclipse.jdt.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.IPageSite;

/**
 * API under construction.
 * 
 * Note: this class will go away as soon as the PR http://bugs.eclipse.org/bugs/show_bug.cgi?id=12856
 * is fixed.
 * 
 * @since 2.0
 */
public abstract class UnifiedSite {
	/**
	 * Returns the workbench page containing this site's page.
	 *
	 * @return the workbench page containing this site's page
	 */
	public abstract IWorkbenchPage getPage();
	
	/**
	 * Returns the selection provider for this site's page.
	 *
	 * @return the selection provider, or <code>null</code> if none
	 */
	public abstract ISelectionProvider getSelectionProvider();
	
	/**
	 * Returns the shell containing this site's page.
	 *
	 * @return the shell containing the page's controls
	 */
	public abstract Shell getShell();
	
	/**
	 * Returns the workbench window containing this site's page.
	 *
	 * @return the workbench window containing this site's page
	 */
	public abstract IWorkbenchWindow getWorkbenchWindow();

	public static UnifiedSite create(IPageSite site) {
		return new PageSite(site);
	}

	public static UnifiedSite create(IWorkbenchPartSite site) {
		return new WorkbenchPartSite(site);
	}
	private static class PageSite extends UnifiedSite {
		private IPageSite fSite;
		PageSite(IPageSite site) {
			fSite= site;
		}
		public IWorkbenchPage getPage() {
			return fSite.getPage();
		}
	
		public ISelectionProvider getSelectionProvider() {
			return fSite.getSelectionProvider();
		}
	
		public Shell getShell() {
			return fSite.getShell();
		}
	
		public IWorkbenchWindow getWorkbenchWindow() {
			return fSite.getWorkbenchWindow();
		}
	}
	
	private static class WorkbenchPartSite extends UnifiedSite {
		private IWorkbenchPartSite fSite;
		WorkbenchPartSite(IWorkbenchPartSite site) {
			fSite= site;
		}
		public IWorkbenchPage getPage() {
			return fSite.getPage();
		}
	
		public ISelectionProvider getSelectionProvider() {
			return fSite.getSelectionProvider();
		}
	
		public Shell getShell() {
			return fSite.getShell();
		}
	
		public IWorkbenchWindow getWorkbenchWindow() {
			return fSite.getWorkbenchWindow();
		}
	}
}
