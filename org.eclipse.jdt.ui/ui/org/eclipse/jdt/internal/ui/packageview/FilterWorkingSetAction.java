/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.packageview;import java.util.Arrays;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.search.ui.IWorkingSet;import org.eclipse.search.ui.SearchUI;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.ui.actions.SelectionProviderAction;import org.eclipse.ui.dialogs.SelectionDialog;
/**
 * The ShowLibrariesAction is the class that adds the filter views to a PackagesView.
 */
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
		//LibraryFilter filter= fPackagesView.getLibraryFilter();
		//setChecked(filter.getShowLibraries());		
		//updateToolTipText();
		setEnabled(true);
	}
	
	/**
	 * Implementation of method defined on <code>IAction</code>.
	 */
	public void run() {		SelectionDialog dialog= SearchUI.createWorkingSetDialog(fShell);		/*if (fWorkingSet != null)			dialog.setInitialSelections(new IWorkingSet[] {fWorkingSet});*/		if (dialog.open() == dialog.OK) {			IWorkingSet ws= (IWorkingSet)dialog.getResult()[0]; 			fPackagesView.setWorkingSet(ws);			//setSelectedWorkingSet((IWorkingSet)dialog.getResult()[0]);			//return true;		} else {			// test if selected working set has been removed			/*if (!Arrays.asList(WorkingSet.getWorkingSets()).contains(fWorkingSet)) {				fWorkingSetText.setText("");				fWorkingSet= null;			}*/		}		//return false;
		/*fPackagesView.getLibraryFilter().setShowLibraries(isChecked());
		updateToolTipText();
		saveInPreferences();*/
		
		fPackagesView.getViewer().getControl().setRedraw(false);
		fPackagesView.getViewer().refresh();
		fPackagesView.getViewer().getControl().setRedraw(true);
	}
		
	/**
	 * Save the supplied patterns in the preferences for the UIPlugin.
	 * They are saved in the format patern,pattern,.
	 */
	private void saveInPreferences() {
		JavaPlugin.getDefault().getPreferenceStore().setValue(
			PackageExplorerPart.TAG_SHOWLIBRARIES,
			fPackagesView.getLibraryFilter().getShowLibraries());
	}
}
