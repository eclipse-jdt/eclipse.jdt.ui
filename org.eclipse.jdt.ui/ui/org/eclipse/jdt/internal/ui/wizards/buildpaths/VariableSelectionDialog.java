package org.eclipse.jdt.internal.ui.wizards.buildpaths;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.CLabel;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.FileDialog;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class VariableSelectionDialog extends StatusDialog {
	
	private static final String DIALOGSTORE_LASTVARIABLE= JavaUI.ID_PLUGIN + ".lastvariable";

	private static final String PAGE_NAME= "VariableSelectionDialog";
	private static final String TITLE= PAGE_NAME + ".title";
	private static final String VARIABLE= PAGE_NAME + ".variable";
	private static final String EXTENSION= PAGE_NAME + ".extension";
	private static final String FULLPATH= PAGE_NAME + ".fullpath";
	private static final String EDITVARIABLE= PAGE_NAME + ".editvariables";

	private static final String ERR_NAMENOTEXISTS= PAGE_NAME + ".error.namenotexists";
	private static final String ERR_PATHEXISTS= PAGE_NAME + ".error.pathexists";

	private static final String DIALOG_CHOOSEVARIABLE= PAGE_NAME + ".variabledialog";
	private static final String DIALOG_EDITVARIABLE= PAGE_NAME + ".editvariabledialog";


	private List fExistingPaths;
	
	private StringButtonDialogField fVariableField;
	private StringButtonDialogField fExtensionField;
	
	private CLabel fFullPath;
	
	//private StringDialogField fFullPathField; 
	private SelectionButtonDialogField fEditVariablesField;
	
	private IStatus fVariableStatus;
	private IStatus fExistsStatus;
	
	private String fVariable;
	
	private IDialogSettings fDialogSettings;

	/**
	 * Constructor for VariableSelectionDialog
	 */
	public VariableSelectionDialog(Shell parent, List existing) {
		super(parent);
		setTitle(JavaPlugin.getResourceString(TITLE));
		
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		fExistingPaths= existing;
			
		fVariableStatus= new StatusInfo();
		fExistsStatus= new StatusInfo();
		
		VariableSelectionAdapter adapter= new VariableSelectionAdapter();
		fVariableField= new StringButtonDialogField(adapter);
		fVariableField.setDialogFieldListener(adapter);
		fVariableField.setLabelText(JavaPlugin.getResourceString(VARIABLE + ".label"));
		fVariableField.setButtonLabel(JavaPlugin.getResourceString(VARIABLE + ".button"));

		fExtensionField= new StringButtonDialogField(adapter);
		fExtensionField.setDialogFieldListener(adapter);
		fExtensionField.setLabelText(JavaPlugin.getResourceString(EXTENSION + ".label"));
		fExtensionField.setButtonLabel(JavaPlugin.getResourceString(EXTENSION + ".button"));

		//fFullPathField= new StringDialogField();
		//fFullPathField.setLabelText(JavaPlugin.getResourceString(FULLPATH + ".label"));
				
		fEditVariablesField= new SelectionButtonDialogField(SWT.PUSH);
		fEditVariablesField.setDialogFieldListener(adapter);
		fEditVariablesField.setLabelText(JavaPlugin.getResourceString(EDITVARIABLE + ".button"));

		String variable= fDialogSettings.get(DIALOGSTORE_LASTVARIABLE);

		if (variable == null) {
			variable= "";
		}
		
		fVariableField.setText(variable);
		fExtensionField.setText("");
		
		updateFullTextField();
	}
	
	public IPath getVariable() {
		return new Path(fVariable).append(fExtensionField.getText());
	}
	
	/**
	 * @see Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		
		Composite inner= new Composite(composite, SWT.NONE);
		MGridLayout layout= new MGridLayout();
		layout.minimumWidth= 420;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 3;
		inner.setLayout(layout);
		
		fVariableField.doFillIntoGrid(inner, 3);	
		
		fExtensionField.doFillIntoGrid(inner, 3);
		
		Label label= new Label(inner, SWT.LEFT);
		label.setLayoutData(new MGridData());
		label.setText(JavaPlugin.getResourceString(FULLPATH + ".label"));
		
		fFullPath= new CLabel(inner, SWT.NONE);
		fFullPath.setLayoutData(new MGridData(MGridData.HORIZONTAL_ALIGN_FILL));
		DialogField.createEmptySpace(inner);
		
		//fFullPathField.doFillIntoGrid(inner, 2);
		//fFullPathField.getTextControl(null).setEnabled(false);
		//DialogField.createEmptySpace(inner);
		
		fEditVariablesField.getSelectionButton(inner);
		DialogField.createEmptySpace(inner, 2);
		
		
		fVariableField.postSetFocusOnDialogField(parent.getDisplay());
		
		updateFullTextField();
		
		return composite;
	}
	
	/**
	 * @see Dialog#okPressed()
	 */
	protected void okPressed() {
		fDialogSettings.put(DIALOGSTORE_LASTVARIABLE, fVariable);
		super.okPressed();
	}	
	
	// -------- VariableSelectionAdapter --------

	private class VariableSelectionAdapter implements IDialogFieldListener, IStringButtonAdapter {
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			doFieldUpdated(field);
		}
		
		// -------- IStringButtonAdapter
		
		public void changeControlPressed(DialogField field) {
			if (field == fVariableField) {
				String variable= chooseVariable();
				if (variable != null) {
					fVariableField.setText(variable);
				}
			} else if (field == fExtensionField) {
				IPath filePath= chooseExtJar();
				if (filePath != null) {
					fExtensionField.setText(filePath.toString());
				}
			}
		}
		
	}
	
	private void doFieldUpdated(DialogField field) {
		if (field == fEditVariablesField) {
			if (editVariable()) {
				fVariableStatus= variableUpdated();
			} else {
				return;
			}
		} else if (field == fVariableField) {
			fVariableStatus= variableUpdated();
		}
		fExistsStatus= getExistsStatus();
		updateFullTextField();
		
		updateStatus(StatusTool.getMoreSevere(fVariableStatus, fExistsStatus));
	}		
	
	
	private boolean findVariableName(String name) {
		String[] names= JavaCore.getClasspathVariableNames();
		for (int i= 0; i < names.length; i++) {
			if (names[i].equals(name)) {
				return true;
			}
		}
		return false;
	}
	

	protected IStatus variableUpdated() {
		fVariable= null;
		
		StatusInfo status= new StatusInfo();
		String name= fVariableField.getText();
		if (name.length() == 0) {
			status.setError("");
		} else if (!findVariableName(name)) {
			status.setError(JavaPlugin.getResourceString(ERR_NAMENOTEXISTS));
		} else {
			fVariable= name;
		}
		fExtensionField.enableButton(fVariable != null);
		return status;
	}
	
	private boolean findPath(IPath path) {
		for (int i= fExistingPaths.size() -1; i >=0; i--) {
			CPListElement curr= (CPListElement) fExistingPaths.get(i);
			if (curr.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				if (curr.getPath().equals(path)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private IStatus getExistsStatus() {
		StatusInfo status= new StatusInfo();
		if (fVariable != null) {
			IPath path= getVariable();
			if (findPath(path)) {
				status.setError(JavaPlugin.getResourceString(ERR_PATHEXISTS));
			}
		}
		return status;
	}
	
	private IPath getResolvedPath() {
		if (fVariable != null) {
			IPath entryPath= JavaCore.getClasspathVariable(fVariable);
			if (entryPath != null) {
				return entryPath.append(fExtensionField.getText());
			}
		}
		return null;
	}		
	
		

	private void updateFullTextField() {
		if (fFullPath != null && !fFullPath.isDisposed()) {
			IPath resolvedPath= getResolvedPath();
			if (resolvedPath != null) {
				fFullPath.setText(resolvedPath.toString());
			} else {
				fFullPath.setText("");
			}
		}
	}
	
	private IPath chooseExtJar() {
		String lastUsedPath= "";
		IPath entryPath= getResolvedPath();
		if (entryPath != null) {
			String fileExt= entryPath.getFileExtension();
			if ("zip".equals(fileExt) || "jar".equals(fileExt)) {
				entryPath.removeLastSegments(1);
			}
			lastUsedPath= entryPath.toOSString();
		}
		
		FileDialog dialog= new FileDialog(getShell(), SWT.SINGLE);
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"});
		dialog.setFilterPath(lastUsedPath);
		String res= dialog.open();
		if (res == null) {
			return null;
		}
		IPath resPath= new Path(res);
		if (!entryPath.isPrefixOf(resPath)) {
			return new Path(resPath.lastSegment());
		} else {
			return resPath.removeFirstSegments(entryPath.segmentCount()).setDevice(null);
		}
	}

	private String chooseVariable() {
		String[] entries= JavaCore.getClasspathVariableNames();
		int nEntries= entries.length;
		CPVariableElement[] elements= new CPVariableElement[nEntries];
		for (int i= 0; i < nEntries; i++) {
			String curr= entries[i];
			IPath entryPath= JavaCore.getClasspathVariable(curr);
			elements[i]= new CPVariableElement(curr, entryPath);
		}
	
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new CPVariableElementLabelProvider(), false, true);
		dialog.setTitle(JavaPlugin.getResourceString(DIALOG_CHOOSEVARIABLE + ".title"));
		dialog.setMessage(JavaPlugin.getResourceString(DIALOG_CHOOSEVARIABLE + ".description"));
		dialog.setEmptyListMessage(JavaPlugin.getResourceString(DIALOG_CHOOSEVARIABLE + ".empty"));
		if (dialog.open(elements) == dialog.OK) {
			CPVariableElement res= (CPVariableElement) dialog.getPrimaryResult();
			return res.getName();
		}
		return null;
	}
	
	private boolean editVariable() {
		EditVariableDialog dialog= new EditVariableDialog(getShell());
		return (dialog.open() == dialog.OK);
	}
		
	
	
	private class EditVariableDialog extends StatusDialog {
		private VariableBlock fVariableBlock;
				
		public EditVariableDialog(Shell parent) {
			super(parent);
			setTitle(JavaPlugin.getResourceString(DIALOG_EDITVARIABLE + ".title"));
			
			fVariableBlock= new VariableBlock();
		}
				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);
			fVariableBlock.createContents(composite);
			return composite;
		}
		
		protected void okPressed() {
			fVariableBlock.performOk();
			super.okPressed();
		}
				
	}	




}
