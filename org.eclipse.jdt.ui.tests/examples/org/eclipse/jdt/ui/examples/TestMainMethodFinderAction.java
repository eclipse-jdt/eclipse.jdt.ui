/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.examples;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodFinder;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;

public class TestMainMethodFinderAction implements IWorkbenchWindowActionDelegate {

/*
		<action id="org.eclipse.jdt.internal.ui.actions.TestMainMethodFinderAction"
			menubarPath="workbench/wbEnd"
			toolbarPath="Normal/Java"			
			label="TestMainMethodFinderAction"
			tooltip="TestMainMethodFinderAction"
			icon="icons/full/ctool16/opentype.gif"
			class="org.eclipse.jdt.internal.ui.actions.TestMainMethodFinderAction"/>			
*/
	
	private ISelection fLastSelection;
	
	public TestMainMethodFinderAction() {
		fLastSelection= null;	
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */	
	public void run(IAction action) {
		ISelection sel= JavaPlugin.getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (!(sel instanceof IStructuredSelection) || sel.isEmpty()) {
			return;
		}		
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		
		ProgressMonitorDialog progressDialog= new ProgressMonitorDialog(shell);
		try {
			IType[] res= MainMethodFinder.findTargets(progressDialog, ((IStructuredSelection) sel).toArray());
			
			ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
			dialog.setIgnoreCase(false);
			dialog.setTitle("Title"); //$NON-NLS-1$
			dialog.setMessage("Title"); //$NON-NLS-1$
			dialog.setEmptyListMessage("Title"); //$NON-NLS-1$
			dialog.setElements(res);
			
			dialog.open();
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart view) {
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fLastSelection= selection;
	}

	/**
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
	}

}