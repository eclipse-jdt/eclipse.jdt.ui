/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.io.File;import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.List;import java.util.ResourceBundle;import org.eclipse.swt.SWT;import org.eclipse.swt.dnd.DND;import org.eclipse.swt.dnd.DropTargetEvent;import org.eclipse.swt.dnd.FileTransfer;import org.eclipse.swt.dnd.Transfer;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.Dialog;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.viewers.AbstractTreeViewer;import org.eclipse.core.resources.IContainer;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.ui.dialogs.IOverwriteQuery;import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;import org.eclipse.ui.wizards.datatransfer.ImportOperation;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dnd.JdtTreeViewerDropAdapter;import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.Utilities;

/**
 * Adapter to handle file drop from other applications like Windows Explorer.
 */ 
public class FileTransferDropAdapter extends JdtTreeViewerDropAdapter implements IOverwriteQuery, TransferDropTargetListener {

	private final static String PREFIX= "PackageViewer.dragAndDrop.fileDrop.";
	private final static String ERROR_PREFIX= PREFIX + "error.";
	
	public FileTransferDropAdapter(AbstractTreeViewer viewer) {
		super(viewer, SWT.NONE);
	}

	//---- IOverwriteQuery ------------------------------------------------------------

	public String queryOverwrite(String file) {
		String[] returnCodes= {YES, NO, ALL, CANCEL};
		int returnVal= openDialog(getViewer().getControl(), file);
		return returnVal < 0 ? CANCEL : returnCodes[returnVal];
	}	
	
	private int openDialog(final Control control, final String file) {
		final int[] result= { Dialog.CANCEL };
		control.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= JavaPlugin.getResourceString(PREFIX + "override.title");
				String msg= JavaPlugin.getFormattedString(PREFIX + "override.message", file);
				String[] options= {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL};
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
		
		if (!(isPackageFragment || isJavaProject || isPackageFragmentRoot)) {
			return;
		}
			
		IJavaElement element= (IJavaElement)target;
		if (!element.isReadOnly()) {
			event.detail= DND.DROP_COPY;
		}
			
		return;	
	}

	public void drop(Object javaTarget, DropTargetEvent event) {
		int operation= event.detail;
		
		event.detail= DND.DROP_NONE;
		Object data= event.data;
		if (data == null || operation != DND.DROP_COPY)
			return;
			
		IContainer target= null;
		try {
			target= (IContainer)((IJavaElement)javaTarget).getCorrespondingResource();	
		} catch (JavaModelException e) {
			return;
		}
		
		List files= checkFiles(Utilities.getShell(event.widget),(String[])data, target.getLocation());
		if (files.size() > 0) {
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			
			ImportOperation op= new ImportOperation(target.getFullPath(), FileSystemStructureProvider.INSTANCE, this, files);
			op.setCreateContainerStructure(false);
									
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
			
			try {
				dialog.run(true, true, op);
			} catch (InvocationTargetException e) {
				ResourceBundle bundle= JavaPlugin.getResourceBundle();
				if (!ExceptionHandler.handle(e, bundle, ERROR_PREFIX)) {
					ExceptionHandler.log(e, bundle, ERROR_PREFIX);
				}
				return;
			} catch (InterruptedException e) {
				return;
			}
			// Special case since ImportOperation doesn't throw a CoreException on
			// failure.
			IStatus status= op.getStatus();
			if (! status.isOK()) {
				ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(),
					JavaPlugin.getResourceString(ERROR_PREFIX + "title"),
					JavaPlugin.getResourceString(ERROR_PREFIX + "message"),
					status, IStatus.ERROR | IStatus.WARNING);
				return;
			}
			// The files we have really handled.
			event.data= files.toArray(new String[files.size()]);
			// Import always performs a copy.
			event.detail= DND.DROP_COPY;
			return;
		} else {
			event.data= null;
			return;
		}
	}
	
	private List checkFiles(Shell shell, String[] files, IPath target) {
		List result= new ArrayList(10);
		int lengthCompare= target.segmentCount() + 1;
		for (int i= 0; i < files.length; i++) {
			String file= files[i];
			IPath path= new Path(file);
			String error= null;
			if (path.equals(target)) {
				error= JavaPlugin.getFormattedString(ERROR_PREFIX + "identical.message", target.lastSegment());
			} else if (path.isPrefixOf(target)) {
				error= JavaPlugin.getFormattedString(ERROR_PREFIX + "parent.message", path.lastSegment());
			}
			if (error != null) {
				MessageDialog.openError(shell, JavaPlugin.getResourceString(ERROR_PREFIX + "title"), error);
				return new ArrayList(0);
			}
			// A copy onto itself ?
			if (target.isPrefixOf(path) && lengthCompare == path.segmentCount())
				continue;
			result.add(new File(file));
		}
		return result;
	}	
}