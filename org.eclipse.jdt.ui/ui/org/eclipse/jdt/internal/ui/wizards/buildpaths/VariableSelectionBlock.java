/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.wizards.buildpaths;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.CLabel;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.FileDialog;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class VariableSelectionBlock {
	
	private List fExistingPaths;
	
	private StringButtonDialogField fVariableField;
	private StringButtonDialogField fExtensionField;
	
	private CLabel fFullPath;
	
	private IStatus fVariableStatus;
	private IStatus fExistsStatus;
	
	private String fVariable;
	private IStatusChangeListener fContext;
	
	private boolean fIsEmptyAllowed;
	
	private String fLastVariableSelection;
	
	/**
	 * Constructor for VariableSelectionBlock
	 */
	public VariableSelectionBlock(IStatusChangeListener context, List existingPaths, IPath varPath, String lastVarSelection, boolean emptyAllowed) {	
		fContext= context;
		fExistingPaths= existingPaths;
		fIsEmptyAllowed= emptyAllowed;
		fLastVariableSelection= lastVarSelection;
			
		fVariableStatus= new StatusInfo();
		fExistsStatus= new StatusInfo();
		
		VariableSelectionAdapter adapter= new VariableSelectionAdapter();
		fVariableField= new StringButtonDialogField(adapter);
		fVariableField.setDialogFieldListener(adapter);
		fVariableField.setLabelText(NewWizardMessages.getString("VariableSelectionBlock.variable.label")); //$NON-NLS-1$
		fVariableField.setButtonLabel(NewWizardMessages.getString("VariableSelectionBlock.variable.button")); //$NON-NLS-1$

		fExtensionField= new StringButtonDialogField(adapter);
		fExtensionField.setDialogFieldListener(adapter);
		fExtensionField.setLabelText(NewWizardMessages.getString("VariableSelectionBlock.extension.label")); //$NON-NLS-1$
		fExtensionField.setButtonLabel(NewWizardMessages.getString("VariableSelectionBlock.extension.button")); //$NON-NLS-1$

		if (varPath != null) {
			fVariableField.setText(varPath.segment(0));
			fExtensionField.setText(varPath.removeFirstSegments(1).toString());
		} else {
			fVariableField.setText(""); //$NON-NLS-1$
			fExtensionField.setText(""); //$NON-NLS-1$
		}
		updateFullTextField();
	}
	
	public IPath getVariablePath() {
		if (fVariable != null) {
			return new Path(fVariable).append(fExtensionField.getText());
		}
		return null;
	}
	
	public IPath getResolvedPath() {
		if (fVariable != null) {
			IPath entryPath= JavaCore.getClasspathVariable(fVariable);
			if (entryPath != null) {
				return entryPath.append(fExtensionField.getText());
			}
		}
		return null;
	}	
		
	public void doFillIntoGrid(Composite inner, int nColumns) {
		fVariableField.doFillIntoGrid(inner, nColumns);	
		
		fExtensionField.doFillIntoGrid(inner, nColumns);
		
		Label label= new Label(inner, SWT.LEFT);
		label.setLayoutData(new MGridData());
		label.setText(NewWizardMessages.getString("VariableSelectionBlock.fullpath.label")); //$NON-NLS-1$
		
		fFullPath= new CLabel(inner, SWT.NONE);
		fFullPath.setLayoutData(new MGridData(MGridData.HORIZONTAL_ALIGN_FILL));
		DialogField.createEmptySpace(inner, nColumns - 2);
		updateFullTextField();
		
	}
	
	public void setFocus(Display display) {
		fVariableField.postSetFocusOnDialogField(display);
	}

	
	public Control createControl(Composite parent) {		
		Composite inner= new Composite(parent, SWT.NONE);
		MGridLayout layout= new MGridLayout();
		layout.minimumWidth= 420;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 3;
		inner.setLayout(layout);
		
		doFillIntoGrid(inner, 3);
		
		setFocus(parent.getDisplay());
		
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
	
	protected IStatus variableUpdated() {
		fVariable= null;
		
		StatusInfo status= new StatusInfo();
		String name= fVariableField.getText();
		if (name.length() == 0) {
			if (!fIsEmptyAllowed) {
				status.setError(NewWizardMessages.getString("VariableSelectionBlock.error.entername")); //$NON-NLS-1$
			} else {
				fVariable= ""; //$NON-NLS-1$
			}
		} else if (JavaCore.getClasspathVariable(name) == null) {
			status.setError(NewWizardMessages.getString("VariableSelectionBlock.error.namenotexists")); //$NON-NLS-1$
		} else {
			fVariable= name;
		}
		fExtensionField.enableButton(fVariable != null);
		return status;
	}	
		
	private IStatus getExistsStatus() {
		StatusInfo status= new StatusInfo();
		if (fVariable != null) {
			IPath path= getVariablePath();
			
			if (path != null) {				if (findPath(path)) {
					status.setError(NewWizardMessages.getString("VariableSelectionBlock.error.pathexists")); //$NON-NLS-1$
				} else if (!path.toFile().exists()) {					status.setError(NewWizardMessages.getString("VariableSelectionBlock.warning.pathnotexists")); //$NON-NLS-1$				}			}
		}
		return status;
	}
	
	private boolean findPath(IPath path) {
		for (int i= fExistingPaths.size() -1; i >=0; i--) {
			IPath curr= (IPath) fExistingPaths.get(i);
			if (curr.equals(path)) {
				return true;
			}
		}
		return false;
	}	

	private void updateFullTextField() {
		if (fFullPath != null && !fFullPath.isDisposed()) {
			IPath resolvedPath= getResolvedPath();
			if (resolvedPath != null) {
				fFullPath.setText(resolvedPath.toString());
			} else {
				fFullPath.setText(""); //$NON-NLS-1$
			}
		}
	}
	
	private Shell getShell() {
		return JavaPlugin.getActiveWorkbenchShell();
	}	
	
	private IPath chooseExtJar() {
		String lastUsedPath= ""; //$NON-NLS-1$
		IPath entryPath= getResolvedPath();
		if (entryPath != null) {
			String fileExt= entryPath.getFileExtension();
			if ("zip".equals(fileExt) || "jar".equals(fileExt)) { //$NON-NLS-1$ //$NON-NLS-2$
				lastUsedPath= entryPath.removeLastSegments(1).toOSString();
			} else {
				lastUsedPath= entryPath.toOSString();
			}
		}
		
		FileDialog dialog= new FileDialog(getShell(), SWT.SINGLE);
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"}); //$NON-NLS-1$
		dialog.setFilterPath(lastUsedPath);		dialog.setText(NewWizardMessages.getString("VariableSelectionBlock.ExtJarDialog.title")); //$NON-NLS-1$
		String res= dialog.open();
		if (res == null) {
			return null;
		}
		IPath resPath= new Path(res).makeAbsolute();
		if (!entryPath.isPrefixOf(resPath)) {
			return new Path(resPath.lastSegment());
		} else {
			return resPath.removeFirstSegments(entryPath.segmentCount()).setDevice(null);
		}
	}

	private String chooseVariable() {
		String selecteVariable= (fVariable != null) ? fVariable : fLastVariableSelection;
		ChooseVariableDialog dialog= new ChooseVariableDialog(getShell(), selecteVariable);
		if (dialog.open() == dialog.OK) {
			return dialog.getSelectedVariable();
		}
		
		return null;
	}
		

}
