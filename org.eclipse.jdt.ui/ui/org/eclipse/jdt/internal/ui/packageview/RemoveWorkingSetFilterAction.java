/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.packageview;import java.util.Arrays;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.search.ui.IWorkingSet;import org.eclipse.search.ui.SearchUI;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.ui.actions.SelectionProviderAction;import org.eclipse.ui.dialogs.SelectionDialog;
/**
 * Removes a working set filter.
 */
class RemoveWorkingSetFilterAction extends SelectionProviderAction {

	private PackageExplorerPart fPackagesView; 
	private Shell fShell;
	public RemoveWorkingSetFilterAction(Shell shell, PackageExplorerPart packagesView, String label) {
		super(packagesView.getViewer(), label);
		fPackagesView= packagesView;		fShell= shell;		setEnabled(fPackagesView.getWorkingSetFilter().getWorkingSet() != null);		
	}
	
	/*
	 * Implementation of method defined on <code>IAction</code>.
	 */
	public void run() {		fPackagesView.setWorkingSet(null);		fPackagesView.getViewer().getControl().setRedraw(false);
		fPackagesView.getViewer().refresh();
		fPackagesView.getViewer().getControl().setRedraw(true);
	}
}
