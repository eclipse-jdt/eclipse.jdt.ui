/*
 * (c) Copyright IBM Corp. 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;
import org.eclipse.jdt.internal.ui.viewsupport.StandardJavaUILabelProvider;

/**
 * This dialog displays a list of <code>IFile</code> and asks
 * the user to confirm saving all of them.
 * <p>
 * This concrete dialog class can be instantiated as is.
 * It is not intended to be subclassed.
 * </p>
 */
public class ConfirmSaveModifiedResourcesDialog extends MessageDialog {
	
	// String constants for widgets
	private static String TITLE= JarPackagerMessages.getString("ConfirmSaveModifiedResourcesDialog.title"); //$NON-NLS-1$
	private static String MESSAGE= JarPackagerMessages.getString("ConfirmSaveModifiedResourcesDialog.message"); //$NON-NLS-1$

	private TableViewer fList;
	private IFile[] fUnsavedFiles;
	
	public ConfirmSaveModifiedResourcesDialog(Shell parentShell, IFile[] unsavedFiles) {
		super(
			parentShell,
			TITLE,
			null,
			MESSAGE,
			QUESTION,
			new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL },
			0);
		fUnsavedFiles= unsavedFiles;
	}

	protected Control createCustomArea(Composite parent) {
		fList= new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		fList.setContentProvider(new ListContentProvider());
		fList.setLabelProvider(new StandardJavaUILabelProvider(StandardJavaUILabelProvider.DEFAULT_TEXTFLAGS, StandardJavaUILabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS, StandardJavaUILabelProvider.getAdornmentProviders(true, null)));
		fList.setInput(Arrays.asList(fUnsavedFiles));
		Control control= fList.getControl();
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= convertWidthInCharsToPixels(20);
		data.heightHint= convertHeightInCharsToPixels(5);
		control.setLayoutData(data);
		return control;
	}

	/**
	 * Returns the files which are not saved and are part of the given list.
	 * 
	 * @return an array of unsaved files
	 */
	public static IFile[] getUnsavedFiles(List resources) {
		IEditorPart[] dirtyEditors= JavaPlugin.getDirtyEditors();
		Set unsavedFiles= new HashSet(dirtyEditors.length);
		if (dirtyEditors.length > 0) {
			for (int i= 0; i < dirtyEditors.length; i++) {
				if (dirtyEditors[i].getEditorInput() instanceof IFileEditorInput) {
					IFile dirtyFile= ((IFileEditorInput) dirtyEditors[i].getEditorInput()).getFile();
					if (resources.contains(dirtyFile)) {
						unsavedFiles.add(dirtyFile);
					}
				}
			}
		}
		return (IFile[]) unsavedFiles.toArray(new IFile[unsavedFiles.size()]);
	}

	/**
	* Save the given files.
	* 
	* @return true if successful.
	*/
	public static boolean saveModifiedResources(final Shell shell, final IFile[] dirtyFiles) {
		// Get display for further UI operations
		Display display= shell.getDisplay();
		if (display == null || display.isDisposed())
			return false;

		final boolean[] retVal= new boolean[1];
		Runnable runnable= new Runnable() {
			public void run() {
				IWorkspace workspace= ResourcesPlugin.getWorkspace();
				IWorkspaceDescription description= workspace.getDescription();
				boolean autoBuild= description.isAutoBuilding();
				description.setAutoBuilding(false);
				try {
					workspace.setDescription(description);
					// This save operation can not be canceled.
					try {
						new ProgressMonitorDialog(shell).run(false, false, createSaveModifiedResourcesRunnable(dirtyFiles));
						retVal[0]= true;
					} finally {
						description.setAutoBuilding(autoBuild);
						workspace.setDescription(description);
					}

				} catch (InvocationTargetException ex) {
					//addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingModifiedResources"), ex); //$NON-NLS-1$
					JavaPlugin.log(ex);
					retVal[0]= false;
				} catch (InterruptedException ex) {
					Assert.isTrue(false); // Can't happen. Operation isn't cancelable.
					retVal[0]= false;
				} catch (CoreException ex) {
					//addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingModifiedResources"), ex); //$NON-NLS-1$
					JavaPlugin.log(ex);
					retVal[0]= false;
				}

			}
		};
		display.syncExec(runnable);
		return retVal[0];
	}

	private static IRunnableWithProgress createSaveModifiedResourcesRunnable(final IFile[] dirtyFiles) {
		return new IRunnableWithProgress() {
			public void run(final IProgressMonitor pm) {
				IEditorPart[] editorsToSave= JavaPlugin.getDirtyEditors();
				pm.beginTask("Saving modified resources", editorsToSave.length); //$NON-NLS-1$
				try {
					List dirtyFilesList= Arrays.asList(dirtyFiles);
					for (int i= 0; i < editorsToSave.length; i++) {
						if (editorsToSave[i].getEditorInput() instanceof IFileEditorInput) {
							IFile dirtyFile= ((IFileEditorInput) editorsToSave[i].getEditorInput()).getFile();
							if (dirtyFilesList.contains((dirtyFile)))
								editorsToSave[i].doSave(new SubProgressMonitor(pm, 1));
						}
						pm.worked(1);
					}
				} finally {
					pm.done();
				}
			}
		};
	}	
	
}
