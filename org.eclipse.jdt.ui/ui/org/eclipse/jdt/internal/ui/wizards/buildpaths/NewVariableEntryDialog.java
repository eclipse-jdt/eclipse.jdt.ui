/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class NewVariableEntryDialog extends Dialog {

	private class VariableSelectionListener implements IDoubleClickListener, ISelectionChangedListener {
		public void doubleClick(DoubleClickEvent event) {
			doDoubleClick();
		}

		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged();
		}
	}
	
	private static class FileLabelProvider extends LabelProvider {
	
		private final Image IMG_FOLDER= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		private final Image IMG_FILE= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
	
		public Image getImage(Object element) {
			if (element instanceof File) {
				File curr= (File) element;
				if (curr.isDirectory()) {
					return IMG_FOLDER;
				} else {
					return IMG_FILE;
				}
			}
			return null;
		}
	
		public String getText(Object element) {
			if (element instanceof File) {
				return ((File) element).getName();
			}
			return super.getText(element);
		}
	}
	
	private static class FileContentProvider implements ITreeContentProvider {
		
		private final Object[] EMPTY= new Object[0];
	
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof File) {
				File[] children= ((File) parentElement).listFiles();
				if (children != null) {
					return children;
				}
			}
			return EMPTY;
		}
	
		public Object getParent(Object element) {
			if (element instanceof File) {
				return ((File) element).getParentFile();
			}
			return null;
		}
	
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}
	
		public Object[] getElements(Object element) {
			return getChildren(element);
		}
	
		public void dispose() {
		}
	
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	
	}	

	private VariableBlock fVariableBlock;

	private Button fExtensionButton;
	private Button fOkButton;
	
	private IPath[] fResultPaths;
	
	private IPath fExistingPath;
	private String fTitle;
	
			
	public NewVariableEntryDialog(Shell parent, String title, IPath existingPath) {
		super(parent);
		fVariableBlock= new VariableBlock(false, existingPath == null ? null : existingPath.segment(1));
		fResultPaths= null;
		fExistingPath= existingPath;
		fTitle= title;
	}
	
	/* (non-Javadoc)
	 * @see Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		shell.setText(fTitle);
		super.configureShell(shell);
	}	
			
	protected Control createDialogArea(Composite parent) {
		VariableSelectionListener listener= new VariableSelectionListener();
		
		Composite composite= (Composite)super.createDialogArea(parent);
		fVariableBlock.createContents(composite);
		fVariableBlock.addDoubleClickListener(listener);
		fVariableBlock.addSelectionChangedListener(listener);
		return composite;
	}
	
	/**
	 * @see Dialog#createButtonsForButtonBar(Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		fOkButton= createButton(parent, IDialogConstants.OK_ID, NewWizardMessages.getString("NewVariableEntryDialog.add.button"), true); //$NON-NLS-1$
		fExtensionButton= createButton(parent, IDialogConstants.CLIENT_ID, NewWizardMessages.getString("NewVariableEntryDialog.addextension.button"), false); //$NON-NLS-1$
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
			buttonPressed(IDialogConstants.CLIENT_ID);
		}
	}
	
	private void doSelectionChanged() {
		boolean isValidSelection= true;
		
		List selected= fVariableBlock.getSelectedElements();
		int nSelected= selected.size();
		
		boolean canExtend= false;
		
		if (nSelected > 0) {
			if (fExistingPath != null && nSelected != 1) {
				isValidSelection= false;
			} else {
				fResultPaths= new Path[nSelected];
				for (int i= 0; i < nSelected; i++) {
					CPVariableElement curr= (CPVariableElement) selected.get(i);
					fResultPaths[i]= new Path(curr.getName());
					if (!curr.getPath().toFile().isFile()) {
						isValidSelection= false;
					}
						
				}
			}
		} else {
			isValidSelection= false;
		}
		fExtensionButton.setEnabled(nSelected == 1 && !isValidSelection);
		fOkButton.setEnabled(isValidSelection);
	}
	
	private IPath[] chooseExtensions(CPVariableElement elem) {
		File file= elem.getPath().toFile();

		ILabelProvider lp= new FileLabelProvider();
		ITreeContentProvider cp= new FileContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setTitle(NewWizardMessages.getString("NewVariableEntryDialog.ExtensionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getFormattedString("NewVariableEntryDialog.ExtensionDialog.description", elem.getName())); //$NON-NLS-1$
		dialog.setInput(file);
		dialog.setAllowMultiple(fExistingPath == null);
		if (fExistingPath != null) {
			dialog.setInitialSelection(fExistingPath.toFile());
		}
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
		if (buttonId ==  IDialogConstants.CLIENT_ID) {
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