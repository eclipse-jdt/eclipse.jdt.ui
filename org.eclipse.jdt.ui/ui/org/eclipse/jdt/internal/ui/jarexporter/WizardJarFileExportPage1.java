package org.eclipse.jdt.internal.ui.jarexporter;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 *	Page 1 of the base resource export-to-jar wizard
 */
/*package*/
class WizardJarFileExportPage1 extends WizardZipFileExportPage1 {

	// widgets
	private Button createManifestCheckbox;

	// dialog store id constants
	private final static String STORE_CREATE_MANIFEST_ID= "WizardJarFileExportPage1.STORE_CREATE_MANIFEST_ID";
	private final static String STORE_DESTINATION_NAMES_ID= "WizardJarFileExportPage1.STORE_DESTINATION_NAMES_ID";
	private final static String STORE_OVERWRITE_EXISTING_FILE_ID= "WizardJarFileExportPage1.STORE_OVERWRITE_EXISTING_FILE_ID";
	private final static String STORE_CREATE_STRUCTURE_ID= "WizardJarFileExportPage1.STORE_CREATE_STRUCTURE_ID";
	private final static String STORE_COMPRESS_CONTENTS_ID= "WizardJarFileExportPage1.STORE_COMPRESS_CONTENTS_ID";
	/**
	 *	Create an instance of this class
	 */
	public WizardJarFileExportPage1(IStructuredSelection selection) {
		super("jarFileExportPage1", selection);
		setTitle("JAR file");
		setDescription("Export resources to a JAR file on the local file system.");
	}

	/**
	 *	Create the export options specification widgets.
	 *
	 *	@param parent org.eclipse.swt.widgets.Composite
	 */
	protected void createOptionsGroup(Composite parent) {
		super.createOptionsGroup(parent);

		Composite optionsGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		optionsGroup.setLayout(layout);

		createManifestCheckbox= new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
		createManifestCheckbox.setText("Create manifest file");
		createManifestCheckbox.setSelection(true);

		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			createManifestCheckbox.setSelection(getDialogSettings().getBoolean(STORE_CREATE_MANIFEST_ID));
		}
	}

	/**
	 *  Export the passed resource and recursively export all of its child resources
	 *  (iff it's a container).  Answer a boolean indicating success.
	 *
	 *  @return boolean
	 */
	protected boolean executeExportOperation(ZipFileExportOperation op) {
		op.setGenerateManifestFile(createManifestCheckbox.getSelection());
		return super.executeExportOperation(op);
	}

	/**
	 *	Answer the string to display in self as the destination type
	 *
	 *	@return java.lang.String
	 */
	protected String getDestinationLabel() {
		return "JAR file:";
	}

	/**
	 *	Answer the suffix that files exported from this wizard must have.
	 *	If this suffix is a file extension (which is typically the case)
	 *	then it must include the leading period character.
	 *
	 *	@return java.lang.String
	 */
	protected String getOutputSuffix() {
		return ".jar";
	}

	/**
	 *	Hook method for saving widget values for restoration by the next instance
	 *	of this class.
	 */
	protected void internalSaveWidgetValues() {
		// update directory names history
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			String[] directoryNames= settings.getArray(STORE_DESTINATION_NAMES_ID);
			if (directoryNames == null)
				directoryNames= new String[0];

			directoryNames= addToHistory(directoryNames, getDestinationValue());

			settings.put(STORE_DESTINATION_NAMES_ID, directoryNames);

			// options
			settings.put(STORE_OVERWRITE_EXISTING_FILE_ID, overwriteExistingFileCheckbox.getSelection());

			settings.put(STORE_CREATE_STRUCTURE_ID, createDirectoryStructureCheckbox.getSelection());

			settings.put(STORE_COMPRESS_CONTENTS_ID, compressContentsCheckbox.getSelection());

			settings.put(STORE_CREATE_MANIFEST_ID, createManifestCheckbox.getSelection());

		}
	}

	/**
	 *	Hook method for restoring widget values to the values that they held
	 *	last time this wizard was used to completion.
	 */
	protected void restoreWidgetValues() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			String[] directoryNames= settings.getArray(STORE_DESTINATION_NAMES_ID);
			if (directoryNames == null)
				return; // ie.- no settings stored

			// destination
			setDestinationValue(directoryNames[0]);
			for (int i= 0; i < directoryNames.length; i++)
				addDestinationItem(directoryNames[i]);

			// options
			overwriteExistingFileCheckbox.setSelection(settings.getBoolean(STORE_OVERWRITE_EXISTING_FILE_ID));

			createDirectoryStructureCheckbox.setSelection(settings.getBoolean(STORE_CREATE_STRUCTURE_ID));

			compressContentsCheckbox.setSelection(settings.getBoolean(STORE_COMPRESS_CONTENTS_ID));

			createManifestCheckbox.setSelection(settings.getBoolean(STORE_CREATE_MANIFEST_ID));

		}
	}

}
