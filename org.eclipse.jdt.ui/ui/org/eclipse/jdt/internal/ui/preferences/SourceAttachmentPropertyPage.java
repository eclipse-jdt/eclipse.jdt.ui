/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.IStatusInfoChangeListener;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;

public class SourceAttachmentPropertyPage extends PropertyPage implements IStatusInfoChangeListener {

	private static final String SSPP_NOLIBRARY= "SourceAttachmentPropertyPage.nolibrary";
	private static final String OP_ERROR_PREFIX= "SourceAttachmentPropertyPage.op_error.";	

	private static final String DIALOG_ADDTOBUILDPATH= "SourceAttachmentPropertyPage.addtobpdialog";	

	private SourceAttachmentBlock fSourceAttachmentBlock;
	private IPackageFragmentRoot fRoot;

	public SourceAttachmentPropertyPage() {
		fSourceAttachmentBlock= null;
		fRoot= null;
	}
	
	/**
	 * @see PreferencePage#createContents
	 */
	protected Control createContents(Composite composite) {
		fRoot= getJARPackageFragmentRoot();
		if (fRoot != null) {
			try {
				IPath path= fRoot.getSourceAttachmentPath();
				IPath prefix= fRoot.getSourceAttachmentRootPath();
				URL jdocLocation= JavaDocAccess.getJavaDocLocation(fRoot);
				IProject proj= fRoot.getJavaProject().getProject();		
				fSourceAttachmentBlock= new SourceAttachmentBlock(proj, this, fRoot.getPath(), path, prefix, jdocLocation);
				return fSourceAttachmentBlock.createControl(composite);				
			} catch (CoreException e) {
				ErrorDialog.openError(getShell(), "Error", "", e.getStatus());
			}		
		}
		Label label= new Label(composite, SWT.LEFT + SWT.WRAP);
		label.setText(JavaPlugin.getResourceString(SSPP_NOLIBRARY));
		label.setFont(composite.getFont());
		return label;
	}

	/**
	 * @see IPreferencePage#performOk
	 */
	public boolean performOk() {
		if (fSourceAttachmentBlock != null) {
			try {
				if (!JavaModelUtility.isOnBuildPath(fRoot)) {
					if (fSourceAttachmentBlock.getSourceAttachmentPath() == null) {
						return true;
					} else if (!putJarOnClasspath()) {
						// ignore changes and return
						return true;
					}
				}
			} catch (JavaModelException e) {
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				return true;	
			}
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						IPath newPath= fSourceAttachmentBlock.getSourceAttachmentPath();
						IPath newRoot= fSourceAttachmentBlock.getSourceAttachmentRootPath();							
						fRoot.attachSource(newPath, newRoot, monitor);
						URL jdocLocation= fSourceAttachmentBlock.getJavaDocLocation();
						JavaDocAccess.setJavaDocLocation(fRoot, jdocLocation);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			try {
				new ProgressMonitorDialog(getShell()).run(true, true, runnable);
			} catch (InvocationTargetException e) {
				if (!ExceptionHandler.handle(e.getTargetException(), getShell(), JavaPlugin.getResourceBundle(), OP_ERROR_PREFIX)) {
					MessageDialog.openError(getShell(), "Error", e.getMessage());
				}
				return false;
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}
	
	private boolean putJarOnClasspath() {
		String title= JavaPlugin.getResourceString(DIALOG_ADDTOBUILDPATH + ".title");
		String message= JavaPlugin.getResourceString(DIALOG_ADDTOBUILDPATH + ".message");
		MessageDialog dialog= new MessageDialog(getShell(), title, null, message, SWT.ICON_QUESTION,
	 			new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL } , 0
	 	);
	 	if (dialog.open() != dialog.OK) {
	 		return false;
	 	}
		IJavaProject jproject= fRoot.getJavaProject();
		try {
			IClasspathEntry[] entries= jproject.getClasspath();
			IClasspathEntry[] newEntries= new IClasspathEntry[entries.length + 1];
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[entries.length]= jproject.newLibraryEntry(fRoot.getPath());
			jproject.setClasspath(newEntries, null);
			return true;
		} catch (JavaModelException e) {
			ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
		}
		return false;
	}	
	
	private IPackageFragmentRoot getJARPackageFragmentRoot() {
		// try to find it as Java element (needed for external jars)
		IAdaptable adaptable= getElement();
		IJavaElement elem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
		if (elem instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) elem;
			if (root.isArchive()) {
				return root;
			} else {
				return null;
			}
		}
		// not on classpath or not in a java project
		IResource resource= (IResource) adaptable.getAdapter(IResource.class);
		if (resource instanceof IFile) {
			IProject proj= resource.getProject();
			try {
				if (proj.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject jproject= JavaCore.create(proj);
					return jproject.getPackageFragmentRoot(resource);
				}
			} catch (CoreException e) {
				ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
			}
		}
		return null;		
	}
	
	// ------- IStatusInfoChangeListener --------
	
	public void statusInfoChanged(StatusInfo status) {
		setValid(!status.isError());
		if (status.isOK()) {
			setErrorMessage(null);
			setMessage(null);
		} else if (status.isWarning()) {
			setErrorMessage(null);
			setMessage(status.getMessage());
		} else {
			setMessage(null);
			setErrorMessage(status.getMessage());
		}
	}	

}