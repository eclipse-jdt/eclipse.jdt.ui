/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;

/**
 * Shows the type hierarchy on a single selected element of type IType or IClassFile 
 */
public class ShowTypeHierarchyAction extends JavaUIAction implements IUpdate {
	
	private ISelectionProvider fSelectionProvider;
	
	public static final String PREFIX= "ShowTypeHierarchyAction.";
	public static final String ERROR_OPEN_VIEW= PREFIX+"error.open_view";
		
	public ShowTypeHierarchyAction(ISelectionProvider selProvider) {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		fSelectionProvider= selProvider;
	}

	/**
	 * Perform the action
	 */
	public void run() {
		ISelection sel= fSelectionProvider.getSelection();
		if (!(sel instanceof IStructuredSelection))
			return;
		Iterator iter= ((IStructuredSelection)sel).iterator();
		if (iter.hasNext()) {
			Object o= iter.next();
			IType type= null;
			if (o instanceof IType) {
				type= (IType)o;
			} else if (o instanceof IClassFile) {
				try {
					type= ((IClassFile)o).getType();
				} catch (JavaModelException e) {
					// not handled here
				}
			} else {
				Display.getCurrent().beep();
				return;
			}
			
			IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
			IWorkbenchPage page= window.getActivePage();
			try {
				IViewPart view= page.showView(JavaUI.ID_TYPE_HIERARCHY);
				((TypeHierarchyViewPart) view).setInput(type);
			} catch (PartInitException x) {
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JavaPlugin.getResourceString(ERROR_OPEN_VIEW), x.getMessage());
			}			
		}
	}
	
	public void update() {
		setEnabled(canActionBeAdded());
	}
	
	public boolean canActionBeAdded() {
		ISelection sel= fSelectionProvider.getSelection();
		if (sel instanceof IStructuredSelection) {
			Iterator iter= ((IStructuredSelection)sel).iterator();
			if (iter.hasNext()) {
				Object obj= iter.next();
				if (obj instanceof IType || obj instanceof IClassFile) {
					return !iter.hasNext();
				}
			}
		}
		return false;
	}

}
