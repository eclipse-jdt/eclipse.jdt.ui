/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;import java.net.URL;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.ui.dialogs.PropertyPage;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;

public class SourceAttachmentPropertyPage extends PropertyPage implements IStatusChangeListener {

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
				IJavaProject jproject= fRoot.getJavaProject();
				IClasspathEntry[] entries= jproject.getRawClasspath();
				
				int index= findClasspathEntry(entries, fRoot.getPath());
				IPath path= null;
				IPath prefix= null;
				if (index != -1) {
					path= entries[index].getSourceAttachmentPath();
					prefix= entries[index].getSourceAttachmentRootPath();
				}
				fSourceAttachmentBlock= new SourceAttachmentBlock(jproject.getProject(), this, fRoot.getPath(), path, prefix);
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
				IJavaProject jproject= fRoot.getJavaProject();
				
				IPath attachPath= fSourceAttachmentBlock.getSourceAttachmentPath();
				IPath attachRoot= fSourceAttachmentBlock.getSourceAttachmentRootPath();				
				
				IClasspathEntry[] entries= jproject.getRawClasspath();
				if (!modifyClasspathEntry(entries, attachPath, attachRoot)) {
					// root not found in classpath
					if (fSourceAttachmentBlock.getSourceAttachmentPath() == null) {
						return true;
					} else if (!putJarOnClasspath()) {
						// ignore changes and return
						return true;
					}
					// put new on class path
					int nEntries= entries.length;
					IClasspathEntry[] incrEntries= new IClasspathEntry[nEntries + 1];
					System.arraycopy(entries, 0, incrEntries, 0, nEntries);
					incrEntries[nEntries]= JavaCore.newLibraryEntry(fRoot.getPath(), attachPath, attachRoot);
					entries= incrEntries;
				}
				final IClasspathEntry[] newEntries= entries;
				
				IRunnableWithProgress runnable= new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException {
						try {
							IJavaProject jproject= fRoot.getJavaProject();
							jproject.setRawClasspath(newEntries, monitor);
						} catch (JavaModelException e) {
							throw new InvocationTargetException(e);
						}
					}
				};				
				new ProgressMonitorDialog(getShell()).run(true, true, runnable);
			} catch (JavaModelException e) {
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				return false;							
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
	
	private int findClasspathEntry(IClasspathEntry[] entries, IPath path) {
		for (int i= 0; i < entries.length; i++) {
			IClasspathEntry curr= entries[i];
			if (curr.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				curr= curr.getResolvedEntry();
			}
			if (curr.getEntryKind() == IClasspathEntry.CPE_LIBRARY && path.equals(curr.getPath())) {
				return i;
			}
		}
		return -1;
	}
			
	private boolean modifyClasspathEntry(IClasspathEntry[] entries, IPath attachPath, IPath attachRoot) {
		int index= findClasspathEntry(entries, fRoot.getPath());
		if (index != -1) {
			IClasspathEntry old= entries[index];
			if (old.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				entries[index]= JavaCore.newVariableEntry(old.getPath(), attachPath, attachRoot);
			} else {
				entries[index]= JavaCore.newLibraryEntry(old.getPath(), attachPath, attachRoot);
			}
			return true;
		}
		return false;
	}		
	
	private boolean putJarOnClasspath() {
		String title= JavaPlugin.getResourceString(DIALOG_ADDTOBUILDPATH + ".title");
		String message= JavaPlugin.getResourceString(DIALOG_ADDTOBUILDPATH + ".message");
		MessageDialog dialog= new MessageDialog(getShell(), title, null, message, SWT.ICON_QUESTION,
	 			new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL } , 0
	 	);
	 	return (dialog.open() == dialog.OK);
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
	
	protected SourceAttachmentBlock getSourceAttachmentBlock(){
		return fSourceAttachmentBlock;
	}
	
	// ------- IStatusChangeListener --------
	
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusTool.applyToStatusLine(this, status);
	}	

}