/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public abstract class AbstractOpenWizardAction extends Action {

	public static final String WIZARD_TITLE= "AbstractOpenWizardAction.title";

	private IWorkbench fWorkbench;
	
	private Class[] fActivatedOnTypes;
	private boolean fAcceptEmptySelection;
	
	public AbstractOpenWizardAction(IWorkbench workbench, String label, boolean acceptEmptySelection) {
		this(workbench, label, null, acceptEmptySelection);
	}
	
	public AbstractOpenWizardAction(IWorkbench workbench, String label, Class[] activatedOnTypes, boolean acceptEmptySelection) {
		super(label);
		fWorkbench= workbench;
		fActivatedOnTypes= activatedOnTypes;
		fAcceptEmptySelection= acceptEmptySelection;
	}
	
	protected AbstractOpenWizardAction() {
	}
	
	protected IWorkbench getWorkbench() {
		return fWorkbench;
	}
	
	protected void setWorkbench(IWorkbench workbench) {
		fWorkbench= workbench;
	}	
	
	private boolean isOfAcceptedType(Object obj) {
		for (int i= 0; i < fActivatedOnTypes.length; i++) {
			if (fActivatedOnTypes[i].isInstance(obj)) {
				return true;
			}
		}
		return false;
	}
	
	
	private boolean isEnabled(Iterator iter) {
		while (iter.hasNext()) {
			Object obj= iter.next();
			if (!isOfAcceptedType(obj) || !shouldAcceptElement(obj)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * can be overridden to add more checks
	 * obj is guaranteed to be instance of one of the accepted types
	 */
	protected boolean shouldAcceptElement(Object obj) {
		return true;
	}		
		
	/**
	 * Create the specific Wizard
	 * (to be implemented by a subclass)
	 */
	abstract protected Wizard createWizard();


	protected ISelection getCurrentSelection() {
		IWorkbenchWindow window= fWorkbench.getActiveWorkbenchWindow();
		if (window != null) {
			return window.getSelectionService().getSelection();
		}
		return null;
	}

	/**
	 * The user has invoked this action.
	 */
	public void run() {
		Wizard wizard= createWizard();		
		WizardDialog dialog= new WizardDialog(fWorkbench.getActiveWorkbenchWindow().getShell(), wizard);
		dialog.create();
		dialog.getShell().setText(JavaPlugin.getResourceString(WIZARD_TITLE));
		dialog.open();
	}
	
	public boolean canActionBeAdded() {
		ISelection selection= getCurrentSelection();
		if (selection == null || selection.isEmpty()) {
			return fAcceptEmptySelection;
		}
		if (fActivatedOnTypes != null) {
			if (selection instanceof IStructuredSelection) {
				return isEnabled(((IStructuredSelection)selection).iterator());
			}
			return false;
		}
		return true;
	}
	
}
