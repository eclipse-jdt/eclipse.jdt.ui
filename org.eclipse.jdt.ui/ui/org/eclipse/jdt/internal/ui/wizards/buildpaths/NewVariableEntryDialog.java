/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class NewVariableEntryDialog extends StatusDialog {

	private class VariableSelectionListener implements IDoubleClickListener, ISelectionChangedListener {
		public void doubleClick(DoubleClickEvent event) {
			doDoubleClick();
		}

		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged();
		}
	}
	
	private final int EXTEND_ID= IDialogConstants.CLIENT_ID;

	private VariableBlock fVariableBlock;

	private Button fExtensionButton;
	private Button fOkButton;
	
	private IPath[] fResultPaths;
	private String fTitle;
	
	private boolean fFirstInvocation= true;
	
	/**
	 * @deprecated Use NewVariableEntryDialog(Shell) and setTitle instead	 */
	public NewVariableEntryDialog(Shell parent, String title, Object exsting) {
		this(parent);
		setTitle(title);
	}
	
			
	public NewVariableEntryDialog(Shell parent) {
		super(parent);
		int shellStyle= getShellStyle();
		setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);
		
		fVariableBlock= new VariableBlock(false, null);
		fResultPaths= null;
	}
	
	/* (non-Javadoc)
	 * @see Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		WorkbenchHelp.setHelp(shell, IJavaHelpContextIds.NEW_VARIABLE_ENTRY_DIALOG);
	}	
			
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);
		VariableSelectionListener listener= new VariableSelectionListener();
		
		Composite composite= (Composite) super.createDialogArea(parent);
		Control control= fVariableBlock.createContents(composite);
		GridData data= new GridData(GridData.FILL_BOTH);
		data.widthHint= convertWidthInCharsToPixels(80);
		data.heightHint= convertHeightInCharsToPixels(15);
		control.setLayoutData(data);
		
		fVariableBlock.addDoubleClickListener(listener);
		fVariableBlock.addSelectionChangedListener(listener);
		
		return composite;
	}
	
	/**
	 * @see Dialog#createButtonsForButtonBar(Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		fOkButton= createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		fExtensionButton= createButton(parent, EXTEND_ID, NewWizardMessages.getString("NewVariableEntryDialog.addextension.button"), false); //$NON-NLS-1$
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}	
	
	
	protected void okPressed() {
		fVariableBlock.performOk();
		super.okPressed();
	}
	
	public IPath[] getResult() {
		return fResultPaths;
	}
	

	/*
 	 * @see IDoubleClickListener#doubleClick(DoubleClickEvent)
 	 */
	private void doDoubleClick() {
		if (fOkButton.isEnabled()) {
			okPressed();
		} else if (fExtensionButton.isEnabled()) {
			buttonPressed(EXTEND_ID);
		}
	}
	
	private void doSelectionChanged() {
		boolean isValidSelection= true;
		StatusInfo status= new StatusInfo();
		
		List selected= fVariableBlock.getSelectedElements();
		int nSelected= selected.size();
		
		boolean canExtend= false;
		
		if (nSelected > 0) {
			fResultPaths= new Path[nSelected];
			for (int i= 0; i < nSelected; i++) {
				CPVariableElement curr= (CPVariableElement) selected.get(i);
				fResultPaths[i]= new Path(curr.getName());
				if (!curr.getPath().toFile().isFile()) {
					isValidSelection= false;
					status.setInfo(NewWizardMessages.getString("NewVariableEntryDialog.info.isfolder")); //$NON-NLS-1$
				}
			}
		} else {
			isValidSelection= false;
			status.setInfo(NewWizardMessages.getString("NewVariableEntryDialog.info.noselection")); //$NON-NLS-1$
		}
		if (isValidSelection && nSelected > 1) {
			String str= NewWizardMessages.getFormattedString("NewVariableEntryDialog.info.selected", String.valueOf(nSelected)); //$NON-NLS-1$
			status.setInfo(str);
		}
		
		fExtensionButton.setEnabled(nSelected == 1 && !isValidSelection);
		fOkButton.setEnabled(isValidSelection);
		updateStatus(status);
	}
	
	private IPath[] chooseExtensions(CPVariableElement elem) {
		File file= elem.getPath().toFile();

		JARFileSelectionDialog dialog= new JARFileSelectionDialog(getShell(), true);
		dialog.setTitle(NewWizardMessages.getString("NewVariableEntryDialog.ExtensionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getFormattedString("NewVariableEntryDialog.ExtensionDialog.description", elem.getName())); //$NON-NLS-1$
		dialog.setInput(file);
		if (dialog.open() == dialog.OK) {
			Object[] selected= dialog.getResult();
			IPath[] paths= new IPath[selected.length];
			for (int i= 0; i < selected.length; i++) {
				IPath filePath= new Path(((File) selected[i]).getPath());
				IPath resPath=  new Path(elem.getName());
				for (int k= elem.getPath().segmentCount(); k < filePath.segmentCount(); k++) {
					resPath= resPath.append(filePath.segment(k));
				}
				paths[i]= resPath;
			}
			return paths;
		}
		return null;
	}

	/*
	 * @see Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId ==  EXTEND_ID) {
			List selected= fVariableBlock.getSelectedElements();
			if (selected.size() == 1) {
				IPath[] extendedPaths= chooseExtensions((CPVariableElement) selected.get(0));
				if (extendedPaths != null) {
					fResultPaths= extendedPaths;
					super.buttonPressed(IDialogConstants.OK_ID);
				}
			}
		} else {
			super.buttonPressed(buttonId);
		}
	}



}