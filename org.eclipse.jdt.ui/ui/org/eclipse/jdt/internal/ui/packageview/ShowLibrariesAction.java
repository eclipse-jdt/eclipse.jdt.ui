/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.packageview;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jface.viewers.LabelProvider;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.actions.SelectionProviderAction;
/**
 * The ShowLibrariesAction is the class that adds the filter views to a PackagesView.
 */
class ShowLibrariesAction extends SelectionProviderAction {

	private PackageExplorerPart fPackagesView; 
	
	/**
	 * Create a new filter action
	 * @param shell the shell that will be used for the list selection
	 * @param packages the PackagesExplorerPart
	 * @param label the label for the action
	 */
	public ShowLibrariesAction(PackageExplorerPart packagesView, String label) {
		super(packagesView.getViewer(), label);
		fPackagesView= packagesView;
		LibraryFilter filter= fPackagesView.getLibraryFilter();
		setChecked(filter.getShowLibraries());		
		updateToolTipText();
		setEnabled(true);
	}
	
	/**
	 * Implementation of method defined on <code>IAction</code>.
	 */
	public void run() {
		fPackagesView.getLibraryFilter().setShowLibraries(isChecked());
		updateToolTipText();
		saveInPreferences();
		
		fPackagesView.getViewer().getControl().setRedraw(false);
		fPackagesView.getViewer().refresh();
		fPackagesView.getViewer().getControl().setRedraw(true);
	}
	
	private void updateToolTipText() {
		if (fPackagesView.getLibraryFilter().getShowLibraries())
			setToolTipText(PackagesMessages.getString("ShowLibraries.hideReferencedLibs")); //$NON-NLS-1$
		else 
			setToolTipText(PackagesMessages.getString("ShowLibraries.showReferencedLibs")); //$NON-NLS-1$
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
