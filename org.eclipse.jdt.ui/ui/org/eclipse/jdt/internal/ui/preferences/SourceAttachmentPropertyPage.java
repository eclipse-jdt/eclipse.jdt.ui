/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;

/**
 * Property page to configure a archive's JARs source attachment
 */
public class SourceAttachmentPropertyPage extends PropertyPage implements IStatusChangeListener {

	private SourceAttachmentBlock fSourceAttachmentBlock;
	private IPackageFragmentRoot fJarRoot;

	public SourceAttachmentPropertyPage() {
	}
	
	/*
	 * @see PreferencePage#createContents
	 */
	protected Control createContents(Composite composite) {
		initializeDialogUnits(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.SOURCE_ATTACHMENT_PROPERTY_PAGE);		

		fJarRoot= getJARPackageFragmentRoot();
		if (fJarRoot == null) {
			return createMessageContent(composite, JavaUIMessages.getString("SourceAttachmentPropertyPage.noarchive.message"));  //$NON-NLS-1$
		}
		try {
			IClasspathEntry entry= fJarRoot.getRawClasspathEntry();
			if (entry == null) {
				// use a dummy entry to use for initialization
				entry= JavaCore.newLibraryEntry(fJarRoot.getPath(), null, null);
			} else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				IClasspathContainer container= JavaCore.getClasspathContainer(entry.getPath(), fJarRoot.getJavaProject());
				String containerName= container != null ? container.getDescription() : entry.getPath().toString();
				return createMessageContent(composite, JavaUIMessages.getFormattedString("SourceAttachmentPropertyPage.containerentry.message", containerName));  //$NON-NLS-1$
			}
			IWorkspaceRoot wsroot= fJarRoot.getJavaModel().getWorkspace().getRoot();
			fSourceAttachmentBlock= new SourceAttachmentBlock(wsroot, this, entry);
			return fSourceAttachmentBlock.createControl(composite);				
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return createMessageContent(composite, JavaUIMessages.getString("SourceAttachmentPropertyPage.noarchive.message"));  //$NON-NLS-1$
		}		
	}
	
	private Control createMessageContent(Composite composite, String message) {
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;		
		inner.setLayout(layout);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint= convertWidthInCharsToPixels(80);
		
		Label label= new Label(inner, SWT.LEFT + SWT.WRAP);
		label.setText(message);
		label.setLayoutData(gd);
		return inner;
	}
	

	/*
	 * @see IPreferencePage#performOk
	 */
	public boolean performOk() {
		if (fSourceAttachmentBlock != null) {
			try {
				IRunnableWithProgress runnable= fSourceAttachmentBlock.getRunnable(fJarRoot.getJavaProject(), getShell());		
				new ProgressMonitorDialog(getShell()).run(true, true, runnable);						
			} catch (InvocationTargetException e) {
				String title= JavaUIMessages.getString("SourceAttachmentPropertyPage.error.title"); //$NON-NLS-1$
				String message= JavaUIMessages.getString("SourceAttachmentPropertyPage.error.message"); //$NON-NLS-1$
				ExceptionHandler.handle(e, getShell(), title, message);
				return false;
			} catch (InterruptedException e) {
				// cancelled
				return false;
			}				
		}
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		if (fSourceAttachmentBlock != null) {
			fSourceAttachmentBlock.setDefaults();
		}
		super.performDefaults();
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
				JavaPlugin.log(e.getStatus());
			}
		}
		return null;		
	}
		

	/*
	 * @see IStatusChangeListener#statusChanged
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}	



}