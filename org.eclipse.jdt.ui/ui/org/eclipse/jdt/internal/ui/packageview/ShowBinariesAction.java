/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jdt.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;

/**
 * The ShowBinariesAction is the class that adds the binary project filter to a PackagesView.
 */
class ShowBinariesAction extends SelectionProviderAction {

	private PackageExplorerPart fPackagesView; 
	private Shell fShell;
	
	/**
	 * Create a new filter action
	 * @param shell the shell that will be used for the list selection
	 * @param packages the PackagesExplorerPart
	 * @param label the label for the action
	 */
	public ShowBinariesAction(Shell shell, PackageExplorerPart packagesView, String label) {
		super(packagesView.getViewer(), label);
		fPackagesView= packagesView;
		BinaryProjectFilter filter= fPackagesView.getBinaryFilter();
		setChecked(filter.getShowBinaries());		
		updateToolTipText();
		setEnabled(true);
		fShell= shell;
	}
	
	/**
	 * Implementation of method defined on <code>IAction</code>.
	 */
	public void run() {
		BinaryProjectFilter filter= fPackagesView.getBinaryFilter();
		filter.setShowBinaries(isChecked());
		updateToolTipText();
		saveInPreferences();
		
		fPackagesView.getViewer().getControl().setRedraw(false);
		fPackagesView.getViewer().refresh();
		fPackagesView.getViewer().getControl().setRedraw(true);
	}
	
	private void updateToolTipText() {
		BinaryProjectFilter filter= fPackagesView.getBinaryFilter();
		if (filter.getShowBinaries())
			setToolTipText(PackagesMessages.getString("ShowBinaries.hideBinaryProjects")); //$NON-NLS-1$
		else 
			setToolTipText(PackagesMessages.getString("ShowBinaries.showBinaryProjects")); //$NON-NLS-1$
	}
	
	/**
	 * Save the filter state in the preferences
	 */
	private void saveInPreferences() {
		JavaPlugin plugin= JavaPlugin.getDefault();
	
		plugin.getPreferenceStore().setValue(
			PackageExplorerPart.TAG_SHOWBINARIES,
			fPackagesView.getBinaryFilter().getShowBinaries());
	}
}
