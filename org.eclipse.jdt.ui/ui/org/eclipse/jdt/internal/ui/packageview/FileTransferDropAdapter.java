/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.resources.IContainer;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;

import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.dialogs.IOverwriteQuery;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;

/**
 * Adapter to handle file drop from other applications.
 */ 
class FileTransferDropAdapter extends JdtViewerDropAdapter implements IOverwriteQuery, TransferDropTargetListener {
	
	FileTransferDropAdapter(AbstractTreeViewer viewer) {
		super(viewer, DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND);
	}

	//---- IOverwriteQuery ------------------------------------------------------------

	public String queryOverwrite(String file) {
		// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19367
		String[] returnCodes= {YES, ALL, NO, CANCEL};
		int returnVal= openDialog(getViewer().getControl(), file);
		return returnVal < 0 ? CANCEL : returnCodes[returnVal];
	}	
	
	private int openDialog(final Control control, final String file) {
		final int[] result= { Dialog.CANCEL };
		control.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= PackagesMessages.getString("DropAdapter.question"); //$NON-NLS-1$
				String msg= PackagesMessages.getFormattedString("DropAdapter.alreadyExists", file); //$NON-NLS-1$
				String[] options= {IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
				MessageDialog dialog= new MessageDialog(control.getShell(), title, null, msg, MessageDialog.QUESTION, options, 0);
				result[0]= dialog.open();
			}
		});
		return result[0];
	}
		
	//---- TransferDropTargetListener interface ---------------------------------------
	
	public Transfer getTransfer() {
		return FileTransfer.getInstance();
	}

	//---- Actual DND -----------------------------------------------------------------
	
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		
		boolean isPackageFragment= target instanceof IPackageFragment;
		boolean isJavaProject= target instanceof IJavaProject;
		boolean isPackageFragmentRoot= target instanceof IPackageFragmentRoot;
		boolean isContainer= target instanceof IContainer;
		
		if (!(isPackageFragment || isJavaProject || isPackageFragmentRoot || isContainer)) 
			return;
			
		if (isContainer) {
			IContainer container= (IContainer)target;
			if (!container.isReadOnly())
				event.detail= DND.DROP_COPY;
		} else {
			IJavaElement element= (IJavaElement)target;
			if (!element.isReadOnly()) 
				event.detail= DND.DROP_COPY;
		}
			
		return;	
	}

	public void drop(Object dropTarget, DropTargetEvent event) {
		int operation= event.detail;
		
		event.detail= DND.DROP_NONE;
		Object data= event.data;
		if (data == null || operation != DND.DROP_COPY)
			return;
			
		IContainer target= null;
		if (dropTarget instanceof IContainer) {
			target= (IContainer)dropTarget;
		} else {
			try {
				target= (IContainer)((IJavaElement)dropTarget).getCorrespondingResource();	
			} catch (JavaModelException e) {
			}
		}
		if (target == null)
			return;
		
		new CopyFilesAndFoldersOperation(JavaPlugin.getActiveWorkbenchShell()).copyFiles((String[])data, target);
		// Import always performs a copy.
		event.detail= DND.DROP_COPY;
	}
}