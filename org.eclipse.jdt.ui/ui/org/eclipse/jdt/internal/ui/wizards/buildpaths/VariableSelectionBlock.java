package org.eclipse.jdt.internal.ui.wizards.buildpaths;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.CLabel;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.FileDialog;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class VariableSelectionBlock {
	
	private static final String PAGE_NAME= "VariableSelectionBlock";
	private static final String VARIABLE= PAGE_NAME + ".variable";
	private static final String EXTENSION= PAGE_NAME + ".extension";
	
	private static final String FULLPATHLABEL= PAGE_NAME + ".fullpath.label";

	private static final String ERR_NAMENOTEXISTS= PAGE_NAME + ".error.namenotexists";
	private static final String ERR_PATHEXISTS= PAGE_NAME + ".error.pathexists";

	private static final String DIALOG_CHOOSEVARIABLE= PAGE_NAME + ".variabledialog";
	private static final String DIALOG_EDITVARIABLE= PAGE_NAME + ".editvariabledialog";

	private List fExistingPaths;
	
	private StringButtonDialogField fVariableField;
	private StringButtonDialogField fExtensionField;
	
	private CLabel fFullPath;
	
	private IStatus fVariableStatus;
	private IStatus fExistsStatus;
	
	private String fVariable;
	private IStatusChangeListener fContext;
	
	/**
	 * Constructor for VariableSelectionBlock
	 */
	public VariableSelectionBlock(IStatusChangeListener context, List existingPaths, String varInitValue) {	
		fContext= context;
		fExistingPaths= existingPaths;
		
			
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

		if (varInitValue == null) {
			varInitValue= "";
		}
		
		fVariableField.setText(varInitValue);
		fExtensionField.setText("");
		
		updateFullTextField();
	}
	
	public IPath getVariable() {
		return new Path(fVariable).append(fExtensionField.getText());
	}
	
	public Control createControl(Composite parent) {		
		Composite inner= new Composite(parent, SWT.NONE);
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
		label.setText(JavaPlugin.getResourceString(FULLPATHLABEL));
		
		fFullPath= new CLabel(inner, SWT.NONE);
		fFullPath.setLayoutData(new MGridData(MGridData.HORIZONTAL_ALIGN_FILL));
		DialogField.createEmptySpace(inner);
		
		fVariableField.postSetFocusOnDialogField(parent.getDisplay());
		
		updateFullTextField();
		
		return inner;
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
		if (field == fVariableField) {
			fVariableStatus= variableUpdated();
		}
		fExistsStatus= getExistsStatus();
		updateFullTextField();
		
		fContext.statusChanged(StatusTool.getMoreSevere(fVariableStatus, fExistsStatus));
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
	
	private Shell getShell() {
		return JavaPlugin.getActiveWorkbenchShell();
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
		ChooseVariableDialog dialog= new ChooseVariableDialog(getShell());
		if (dialog.open() == dialog.OK) {
			return 	dialog.getSelectedVariable();
		}
		
		return null;
		
	}		
	
	
	private class ChooseVariableDialog extends StatusDialog implements IStatusChangeListener {
		private VariableBlock fVariableBlock;
				
		public ChooseVariableDialog(Shell parent) {
			super(parent);
			setTitle(JavaPlugin.getResourceString(DIALOG_CHOOSEVARIABLE + ".title"));
			
			fVariableBlock= new VariableBlock(this, true);
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
		
		public String getSelectedVariable() {
			return fVariableBlock.getSelectedVariable();
		}
		
		/**
	 	 * @see IStatusChangeListener#statusChanged(IStatus)
	 	 */
		public void statusChanged(IStatus status) {
			updateStatus(status);
			
		}
	}	

}
