/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.actions;


import org.eclipse.jface.action.Action;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;

/**
 * Shows a given type in the supertypes view 
 */
public class ShowTypeHierarchyOnTypeAction extends Action {
	
	private IType fType;
	
	/**
	 * The caller is responsible to set up any properties of the action
	 * except the label passed into the constructor
	 */
	public ShowTypeHierarchyOnTypeAction(String label, String description, IType type) {
		super(label);
		fType= type;
		setDescription(description);
	}

	/**
	 * Perform the action
	 */
	public void run() {
		IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		try {
			TypeHierarchyViewPart view= (TypeHierarchyViewPart)page.showView(JavaUI.ID_TYPE_HIERARCHY);
			view.setInput(fType);
		} catch (PartInitException e) {
		}
	}
}
