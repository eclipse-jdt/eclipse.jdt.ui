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
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.JdtHackFinder;


/**
 * Show the current selection in the Navigator view 
 */
public class ShowInNavigatorAction extends SelectionProviderAction {
	
	public ShowInNavigatorAction(ISelectionProvider viewer) {
		super(viewer, "Show in &Navigator");
		setDescription("Show the selected object(s) in the navigator view");
	}

	/**
	 * Perform the action
	 */
	public void run() {
		Iterator elements= getStructuredSelection().iterator();
		List v= new ArrayList();
		while (elements.hasNext()) {
			Object o= elements.next();
			if (o instanceof IAdaptable) 
				v.add(((IAdaptable)o).getAdapter(IResource.class));
			if (o instanceof IResource)
				v.add(o);
		}
		IWorkbenchPage page= JavaPlugin.getActivePage();
		try {
			IViewPart view= page.showView(IPageLayout.ID_RES_NAV);
			if (view instanceof ISetSelectionTarget) {
				ISelection selection= new StructuredSelection(v);
				((ISetSelectionTarget)view).selectReveal(selection);
			}
		} catch (PartInitException e) {
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), "Can't open navigator", e.getMessage());
		}
	}
		
	public void selectionChanged(IStructuredSelection selection) {
		if (selection.isEmpty()) {
			setEnabled(false);
			return;
		}
		Iterator elements= selection.iterator();
		boolean enabled= false;
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
