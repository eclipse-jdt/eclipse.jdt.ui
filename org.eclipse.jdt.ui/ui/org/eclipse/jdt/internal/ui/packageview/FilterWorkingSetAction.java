/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.packageview;import java.util.Arrays;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.search.ui.IWorkingSet;import org.eclipse.search.ui.SearchUI;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.ui.actions.SelectionProviderAction;import org.eclipse.ui.dialogs.SelectionDialog;
/**
 * Show a dialog to select the working set for filtering the packages view. */
class FilterWorkingSetAction extends SelectionProviderAction {

	private PackageExplorerPart fPackagesView; 
	private Shell fShell;
	/**
	 * Create a new filter action
	 * @param shell the shell that will be used for the list selection
	 * @param packages the PackagesExplorerPart
	 * @param label the label for the action
	 */
	public FilterWorkingSetAction(Shell shell, PackageExplorerPart packagesView, String label) {
		super(packagesView.getViewer(), label);
		fPackagesView= packagesView;		fShell= shell;
		setEnabled(true);
	}
	
	/*
	 * Implementation of method defined on <code>IAction</code>.
	 */
	public void run() {		SelectionDialog dialog= SearchUI.createWorkingSetDialog(fShell);		if (dialog.open() == dialog.OK) {			IWorkingSet ws= (IWorkingSet)dialog.getResult()[0]; 			fPackagesView.setWorkingSet(ws);		} 		fPackagesView.getViewer().getControl().setRedraw(false);
		fPackagesView.getViewer().refresh();
		fPackagesView.getViewer().getControl().setRedraw(true);
	}
}
