/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Show the current selection in the Navigator view 
 * 
 * @deprecated Use action from package org.eclipse.jdt.ui.actions
 */
public class ShowInNavigatorAction extends SelectionProviderAction {
	
	public ShowInNavigatorAction(ISelectionProvider viewer) {
		super(viewer, PackagesMessages.getString("ShowInNavigator.label")); //$NON-NLS-1$
		setDescription(PackagesMessages.getString("ShowInNavigator.description")); //$NON-NLS-1$
	}

	/**
	 * Perform the action
	 */
	public void run() {
		List v= collectResources();
		IWorkbenchPage page= JavaPlugin.getActivePage();
		try {
			IViewPart view= page.showView(IPageLayout.ID_RES_NAV);
			if (view instanceof ISetSelectionTarget) {
				ISelection selection= new StructuredSelection(v);
				((ISetSelectionTarget)view).selectReveal(selection);
			}
		} catch (PartInitException e) {
			ExceptionHandler.handle(e, JavaPlugin.getActiveWorkbenchShell(), PackagesMessages.getString("ShowInNavigator.error"), e.getMessage()); //$NON-NLS-1$
		}
	}

	protected List collectResources() {
		Iterator elements= getStructuredSelection().iterator();
		List v= new ArrayList();
		while (elements.hasNext()) {
			Object o= elements.next();
			if (o instanceof IAdaptable) 
				v.add(((IAdaptable)o).getAdapter(IResource.class));
			if (o instanceof IResource)
				v.add(o);
		}
		return v;
	}
		
	public void selectionChanged(IStructuredSelection selection) {
		if (selection.isEmpty()) {
			setEnabled(false);
			return;
		}
		Iterator elements= selection.iterator();
		if (elements.hasNext()) {
			Object o= elements.next();
			if ((o instanceof IAdaptable) &&  
				(((IAdaptable)o).getAdapter(IResource.class) != null)) {
				setEnabled(true);
				return;
			}
		}
		setEnabled(false);
	}
}
