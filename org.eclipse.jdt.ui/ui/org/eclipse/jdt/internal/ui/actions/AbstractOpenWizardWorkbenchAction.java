/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;


public abstract class AbstractOpenWizardWorkbenchAction extends AbstractOpenWizardAction implements IWorkbenchWindowActionDelegate{
	
	public AbstractOpenWizardWorkbenchAction(IWorkbench workbench, String label, boolean acceptEmptySelection) {
		super(workbench, label, null, acceptEmptySelection);
	}
	
	public AbstractOpenWizardWorkbenchAction(IWorkbench workbench, String label, Class[] activatedOnTypes, boolean acceptEmptySelection) {
		super(workbench, label, null, acceptEmptySelection);
	}

	protected AbstractOpenWizardWorkbenchAction() {
	}

	/**
	 * @see IActionDelegate#run
	 */
	public void run(IAction action) {
		run();
	}

	/**
	 * @see AbstractOpenWizardAction#dispose
	 */	
	public void dispose() {
		// do nothing.
		setWorkbench(null);
	}

	/**
	 * @see AbstractOpenWizardAction#init
	 */		
	public void init(IWorkbenchWindow window) {
		setWorkbench(window.getWorkbench());
	}
	
	/**
	 * @see IActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing. Action doesn't depend on selection.
	}	

}