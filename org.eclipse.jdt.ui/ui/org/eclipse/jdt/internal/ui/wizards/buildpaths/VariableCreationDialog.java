/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.DirectoryDialog;import org.eclipse.swt.widgets.FileDialog;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jdt.core.JavaConventions;import org.eclipse.jdt.internal.ui.IUIConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class VariableCreationDialog extends StatusDialog {
	
	private static final String PAGE_NAME= "VariableCreationDialog";
	private static final String TITLE_EDIT= PAGE_NAME + ".titlenew";
	private static final String TITLE_NEW= PAGE_NAME + ".titleedit";
	
	private static final String NAME= PAGE_NAME + ".name";
	private static final String PATH= PAGE_NAME + ".path";

	private static final String ERR_INVALIDPATH= PAGE_NAME + ".error.invalidpath";
	private static final String ERR_INVALIDNAME= PAGE_NAME + ".error.invalidname";
	private static final String ERR_NAMEEXISTS= PAGE_NAME + ".error.nameexists";
	
	private static final String DIALOG_EXTJARDIALOG= PAGE_NAME + ".extjardialog";
	private static final String DIALOG_EXTDIRDIALOG= PAGE_NAME + ".extdirdialog";
	
	private IDialogSettings fDialogSettings;
	
	private StringDialogField fNameField;
	private StatusInfo fNameStatus;	
	
	private StringButtonDialogField fPathField;
	private StatusInfo fPathStatus;
	private StringButtonDialogField fDirButton;
	
		
	private CPVariableElement fElement;
	
	private List fExistingNames;
		
	public VariableCreationDialog(Shell parent, CPVariableElement element, List existingNames) {
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
		fPathField.setButtonLabel(JavaPlugin.getResourceString(PATH + ".file.button"));
		
		fDirButton= new StringButtonDialogField(adapter);
		fDirButton.setButtonLabel(JavaPlugin.getResourceString(PATH + ".dir.button"));
		
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
		return new CPVariableElement(fNameField.getText(), new Path(fPathField.getText()), false);
	}

	/**
	 * @see Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		
		Composite inner= new Composite(composite, SWT.NONE);
		
		MGridLayout layout= new MGridLayout();
		layout.minimumWidth= 380;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 3;
		inner.setLayout(layout);
		
		fNameField.doFillIntoGrid(inner, 2);
		DialogField.createEmptySpace(inner, 1);
		
		fPathField.doFillIntoGrid(inner, 3);
		DialogField.createEmptySpace(inner, 2);
		fDirButton.getChangeControl(inner);
		
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
			} else if (field == fDirButton) {
				IPath path= chooseExtDirectory();
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
		updateStatus(StatusTool.getMoreSevere(fNameStatus, fPathStatus));
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
		} else if (nameConflict(name)) {
			status.setError(JavaPlugin.getResourceString(ERR_NAMEEXISTS));
		}
		return status;
	}
	
	private boolean nameConflict(String name) {
		if (fElement != null && fElement.getName().equals(name)) {
			return false;
		}
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
	
	
	private String getInitPath() {
		String initPath= fPathField.getText();
		if (initPath.length() == 0) {		
			initPath= fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
			if (initPath == null) {
				initPath= "";
			}
		} else {
			IPath entryPath= new Path(initPath);
			String fileExt= entryPath.getFileExtension();
			if ("zip".equals(fileExt) || "jar".equals(fileExt)) {
				entryPath.removeLastSegments(1);
			}
			initPath= entryPath.toOSString();
		}
		return initPath;
	}		
	
	
	/*
	 * Open a dialog to choose a jar from the file system
	 */
	private IPath chooseExtJarFile() {
		String initPath= getInitPath();
		
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(JavaPlugin.getResourceString(DIALOG_EXTJARDIALOG + ".text"));
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"});
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, dialog.getFilterPath());
			return new Path(res).makeAbsolute();
		}
		return null;
	}
	
	private IPath chooseExtDirectory() {
		String initPath= getInitPath();
		
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setText(JavaPlugin.getResourceString(DIALOG_EXTDIRDIALOG + ".text"));
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, dialog.getFilterPath());
			return new Path(res);
		}
		return null;		
	}
	
		
	
}