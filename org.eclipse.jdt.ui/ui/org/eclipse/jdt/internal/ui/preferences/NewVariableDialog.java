/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.FileDialog;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jdt.core.JavaConventions;import org.eclipse.jdt.internal.ui.IUIConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class NewVariableDialog extends StatusDialog {
	
	private static final String PAGE_NAME= "NewVariableDialog";
	private static final String TITLE_EDIT= PAGE_NAME + ".titlenew";
	private static final String TITLE_NEW= PAGE_NAME + ".titleedit";
	
	private static final String NAME= PAGE_NAME + ".name";
	private static final String PATH= PAGE_NAME + ".path";

	private static final String ERR_INVALIDPATH= PAGE_NAME + ".error.invalidpath";
	private static final String ERR_INVALIDNAME= PAGE_NAME + ".error.invalidname";
	private static final String ERR_NAMEEXISTS= PAGE_NAME + ".error.nameexists";
	
	private static final String DIALOG_EXTJARDIALOG= PAGE_NAME + ".extjardialog";
	
	private IDialogSettings fDialogSettings;
	
	private StringDialogField fNameField;
	private StatusInfo fNameStatus;	
	
	private StringButtonDialogField fPathField;
	private StatusInfo fPathStatus;
	
	private CPVariableElement fElement;
	
	private List fExistingNames;
		
	public NewVariableDialog(Shell parent, CPVariableElement element, List existingNames) {
		super(parent);
		setTitle(JavaPlugin.getResourceString((element == null) ? TITLE_NEW : TITLE_EDIT));
		
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		fElement= element;
		
		fNameStatus= new StatusInfo();
		fPathStatus= new StatusInfo();
		
		NewVariableAdapter adapter= new NewVariableAdapter();
		fNameField= new StringDialogField();
		fNameField.setDialogFieldListener(adapter);
		fNameField.setLabelText(JavaPlugin.getResourceString(NAME + ".label"));

		fPathField= new StringButtonDialogField(adapter);
		fPathField.setDialogFieldListener(adapter);
		fPathField.setLabelText(JavaPlugin.getResourceString(PATH + ".label"));
		fPathField.setButtonLabel(JavaPlugin.getResourceString(PATH + ".button"));
		
		fExistingNames= existingNames;
		
		
		if (element != null) {
			fNameField.setText(element.getName());
			fPathField.setText(element.getPath().toString());
			fExistingNames.remove(element.getName());
		} else {
			fNameField.setText("");
			fPathField.setText("");
		}
	}
	
	public CPVariableElement getClasspathElement() {
		return new CPVariableElement(fNameField.getText(), new Path(fPathField.getText()));
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		int marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		int marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fNameField, fPathField }, false, 420, 0, marginWidth, marginHeight);
		
		DialogField focusField= (fElement == null) ? fNameField : fPathField;
		focusField.postSetFocusOnDialogField(parent.getDisplay());
		return composite;
	}

		
	// -------- NewVariableAdapter --------

	private class NewVariableAdapter implements IDialogFieldListener, IStringButtonAdapter {
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			doFieldUpdated(field);
		}
		
		// -------- IStringButtonAdapter
		
		public void changeControlPressed(DialogField field) {
			if (field == fPathField) {
				IPath path= chooseExtJarFile();
				if (path != null) {
					fPathField.setText(path.toString());
				}
			}	
		}
		
	}
	
	protected void doFieldUpdated(DialogField field) {	
		if (field == fNameField) {
			fNameStatus= nameUpdated();
		} else if (field == fPathField) {
			fPathStatus= pathUpdated();
		}		
		updateStatus(fNameStatus.getMoreSevere(fPathStatus));
	}		
	
	protected StatusInfo nameUpdated() {
		StatusInfo status= new StatusInfo();
		String name= fNameField.getText();
		if (name.length() == 0) {
			status.setError("");
			return status;
		}
		IStatus val= JavaConventions.validateIdentifier(name);
		if (val.matches(IStatus.ERROR)) {
			status.setError(JavaPlugin.getFormattedString(ERR_INVALIDNAME, val.getMessage()));
		} else if (nameExists(name)) {
			status.setError(JavaPlugin.getResourceString(ERR_NAMEEXISTS));
		}
		return status;
	}
	
	private boolean nameExists(String name) {
		for (int i= 0; i < fExistingNames.size(); i++) {
			CPVariableElement elem= (CPVariableElement)fExistingNames.get(i);
			if (name.equals(elem.getName())){
				return true;
			}
		}
		return false;
	}
	
	
	protected StatusInfo pathUpdated() {
		StatusInfo status= new StatusInfo();
		
		String path= fPathField.getText();
		if (path.length() == 0) {
			status.setError("");
			return status;
		}
		return status;
	}
	
	/*
	 * Open a dialog to choose a jar from the file system
	 */
	private IPath chooseExtJarFile() {
		String initPath= fPathField.getText();
		if (initPath.length() == 0) {		
			initPath= fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
			if (initPath == null) {
				initPath= "";
			}
		}
		
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(JavaPlugin.getResourceString(DIALOG_EXTJARDIALOG + ".text"));
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"});
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, dialog.getFilterPath());
			return new Path(res);
		}
		return null;
	}	
	
		
	
}