package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.resources.IContainer;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.NewFolderDialog;

/**
  */
public class FolderSelectionDialog extends ElementTreeSelectionDialog implements ISelectionChangedListener {

	private static final int NEW_ID= IDialogConstants.CLIENT_ID;
	private Button fNewFolderButton;
	private IContainer fSelectedContainer;

	/**
	 * Constructor for FolderSelectionDialog.
	 * @param parent
	 * @param labelProvider
	 * @param contentProvider
	 */
	public FolderSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		fNewFolderButton= createButton(parent, NEW_ID, "New Folder...", false);	
		super.createButtonsForButtonBar(parent);
	}
	
	/**
	 * Method updateNewFolderButtonState.
	 */
	private void updateNewFolderButtonState() {
		IStructuredSelection selection= (IStructuredSelection) getTreeViewer().getSelection();
		fSelectedContainer= null;
		if (selection.size() == 1) {
			Object first= selection.getFirstElement();
			if (first instanceof IContainer) {
				fSelectedContainer= (IContainer) first;
			}
		}
		fNewFolderButton.setEnabled(fSelectedContainer != null);
	}	
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == NEW_ID && fSelectedContainer != null) {
			NewFolderDialog dialog= new NewFolderDialog(getShell(), fSelectedContainer);
			if (dialog.open() == NewFolderDialog.OK) {
				TreeViewer treeViewer= getTreeViewer();
				treeViewer.refresh(fSelectedContainer);
				Object createdFolder= dialog.getResult()[0];
				treeViewer.reveal(createdFolder);
				treeViewer.setSelection(new StructuredSelection(createdFolder));
			}
		}
		super.buttonPressed(buttonId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Control control= super.createContents(parent);
		getTreeViewer().addSelectionChangedListener(this);
		return control;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		updateNewFolderButtonState();
	}
	


}
