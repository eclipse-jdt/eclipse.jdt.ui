package org.eclipse.jdt.internal.ui.javaeditor;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;

	/**
	 * A dialog to attach source to a jar file.
	 *
	 * copied from org.eclipse.jdt.internal.ui.wizards.buildpaths.LibrariesWorkbookPage.SourceAttachmentDialog.
	 */
	public class SourceAttachmentDialog extends StatusDialog implements IStatusChangeListener {
		
		private SourceAttachmentBlock fSourceAttachmentBlock;
		private IPackageFragmentRoot fRoot;
				
		public SourceAttachmentDialog(Shell parent, IPackageFragmentRoot root) throws JavaModelException {
			super(parent);

			fRoot= root;

			IClasspathEntry entry= fRoot.getRawClasspathEntry();			
			setTitle(JavaEditorMessages.getFormattedString("SourceAttachmentDialog.title", entry.getPath().toString())); //$NON-NLS-1$
			fSourceAttachmentBlock= new SourceAttachmentBlock(ResourcesPlugin.getWorkspace().getRoot(), this, entry);
		}
		
		/*
		 * @see Windows#configureShell
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.SOURCE_ATTACHMENT_DIALOG);
		}		
				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);
						
			Control inner= fSourceAttachmentBlock.createControl(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			return composite;
		}
		
		public void statusChanged(IStatus status) {
			updateStatus(status);
		}
		
		public IPath getSourceAttachmentPath() {
			return fSourceAttachmentBlock.getSourceAttachmentPath();
		}
		
		public IPath getSourceAttachmentRootPath() {
			return fSourceAttachmentBlock.getSourceAttachmentRootPath();
		}
		
		protected void okPressed() {
			super.okPressed();
			
			try {
				IJavaProject project= fRoot.getJavaProject();				
				IRunnableWithProgress runnable= fSourceAttachmentBlock.getRunnable(project, getShell());
				new ProgressMonitorDialog(getShell()).run(true, true, runnable);						

			} catch (InvocationTargetException e) {
				String title= JavaEditorMessages.getString("SourceAttachmentDialog.error.title"); //$NON-NLS-1$
				String message= JavaEditorMessages.getString("SourceAttachmentDialog.error.message"); //$NON-NLS-1$
				ExceptionHandler.handle(e, getShell(), title, message);

			} catch (InterruptedException e) {
				// cancelled
			}	
		}
	}	