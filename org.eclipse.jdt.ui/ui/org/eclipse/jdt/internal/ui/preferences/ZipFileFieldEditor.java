/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.IOException;
import java.util.zip.ZipFile;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;

/**
 * A FieldEditor used to find a package root in zip files
 */

class ZipFileFieldEditor extends StringButtonFieldEditor {

	private static final String KEY_PACKAGE_ROOT_LABEL= "org.eclipse.jdt.ui.build.jdk.prefix.label";
	private static final String PROP_PREFIX= "org.eclipse.jdt.ui.build.jdk.prefix";
	private static final String KEY_ERROR_TITLE= "org.eclipse.jdt.ui.build.jdk.error.title";
	private static final String KEY_ERROR_MESSAGE= "org.eclipse.jdt.ui.build.jdk.error.message";

	private Button fChangeButton;
	private String fFileName;
	
	//fix for: 1G45E1D: ITPJUI:WIN - Jar - Package Root Selection Dialog
	private static final String ROOT_SELECTION_DIALOG_TITLE= "PackageRootSelectionDialog.title";
	private static final String ROOT_SELECTION_DIALOG_MESSAGE= "PackageRootSelectionDialog.message";
	
	ZipFileFieldEditor(Composite parent) {
		super(PROP_PREFIX, JavaPlugin.getResourceString(KEY_PACKAGE_ROOT_LABEL), parent);
		setChangeButtonText(JFaceResources.getString("openBrowse"));
		getTextControl().setEditable(false);
	}

	/**
	 * Fills the field editor's basic controls into the given parent.
	 * @private
	 */
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		super.doFillIntoGrid(parent, numColumns);
		fChangeButton= getChangeControl(parent);
	}

	void setEnabled(boolean enabled) {
		if (fChangeButton != null)
			fChangeButton.setEnabled(enabled);
	}

	void setZipFileName(String fileName) {
		fFileName= fileName;
	}

	/**
	  * The field editor's change button has been pressed.
	  * Returns the selected package root 
	  * @private
	  */
	protected String changePressed() {
		try {
			ZipFile zipFile= new ZipFile(fFileName);
			ZipContentProvider contentProvider= new ZipContentProvider(getStringValue(), zipFile);
			ElementTreeSelectionDialog d= new ElementTreeSelectionDialog(getShell(), "", null, new ZipLabelProvider(), contentProvider, false, true); 
			d.setMessage(JavaPlugin.getResourceString(ROOT_SELECTION_DIALOG_MESSAGE));
			//fix for: 1G45E1D: ITPJUI:WIN - Jar - Package Root Selection Dialog
			d.setTitle(JavaPlugin.getResourceString(ROOT_SELECTION_DIALOG_TITLE));		
			if (d.open(zipFile, contentProvider.getSelectedNode()) == d.OK) {
				Object res= d.getSelectedElement();
				if (res != null) {
					return res.toString();
				}
			}	
		} catch (IOException e) {
			String title= JavaPlugin.getResourceString(KEY_ERROR_TITLE);
			String message= JavaPlugin.getResourceString(KEY_ERROR_MESSAGE);
			MessageDialog.openError(getShell(), title, message + " " + fFileName);
		}
		return null;
	}
}


