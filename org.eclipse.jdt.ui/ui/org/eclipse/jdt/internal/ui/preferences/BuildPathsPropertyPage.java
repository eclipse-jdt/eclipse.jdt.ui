/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.FillLayout;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;import org.eclipse.ui.dialogs.PropertyPage;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

public class BuildPathsPropertyPage extends PropertyPage implements IStatusChangeListener {
	
	private static final String BPP_NOJAVAPROJECT= "BuildPathsPropertyPage.nojavaproject";
	private static final String BPP_CLOSEDPROJECT= "BuildPathsPropertyPage.closedproject";
	
	private static final String OP_ERROR_PREFIX= "BuildPathsPropertyPage.op_error.";
	
	private BuildPathsBlock fBuildPathsBlock;
	private Composite fNoJavaProjectComposite;
	
	protected Control createContents(Composite parent) {
		// ensure the page has no special buttons
		noDefaultAndApplyButton();		
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
		IProject project= getProject();
		if (!project.isOpen()) {
			createForClosedProject(composite);	
		} else if (!isJavaProject(project)) {
			createWithoutJava(composite);
		} else {
			createWithJava(composite);
		}
		fNoJavaProjectComposite= composite;
		
		WorkbenchHelp.setHelp(parent, new DialogPageContextComputer(this, IJavaHelpContextIds.BUILD_PATH_PROPERTY_PAGE));
		return composite;
	}
	
	private void createWithJava(Composite parent) {
		IWorkspaceRoot root= JavaPlugin.getWorkspace().getRoot();
		fBuildPathsBlock= new BuildPathsBlock(root, this, false);
		fBuildPathsBlock.createControl(parent);
		fBuildPathsBlock.init(getProject(), false);
	}
	
	private void createWithoutJava(Composite parent) {
		Label label= new Label(parent, SWT.LEFT);
		label.setText(JavaPlugin.getResourceString(BPP_NOJAVAPROJECT));
		label.setFont(parent.getFont());
		
		fBuildPathsBlock= null;
		setValid(true);
	}
	
	private void createForClosedProject(Composite parent) {
		Label label= new Label(parent, SWT.LEFT);
		label.setText(JavaPlugin.getResourceString(BPP_CLOSEDPROJECT));
		label.setFont(parent.getFont());
		
		fBuildPathsBlock= null;
		setValid(true);
	}
	
	/**
	 * @see IPreferencePage#performOk
	 */
	public boolean performOk() {
		if (fBuildPathsBlock != null) {
			fBuildPathsBlock.init(getProject(), false);
			IRunnableWithProgress runnable= fBuildPathsBlock.getRunnable();

			IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(runnable);
			Shell shell= getControl().getShell();
			try {
				new ProgressMonitorDialog(shell).run(true, true, op);
			} catch (InvocationTargetException e) {
				if (!ExceptionHandler.handle(e.getTargetException(), shell, JavaPlugin.getResourceBundle(), OP_ERROR_PREFIX)) {
					MessageDialog.openError(shell, "Error", e.getMessage());
				}
				return false;
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}
	
	private IProject getProject() {
		Object element= getElement();
		if (element instanceof IProject) {
			return (IProject)element;
		} else if (element instanceof IJavaProject) {
			return ((IJavaProject)element).getProject();
		}
		return null;
	}
	
	private boolean isJavaProject(IProject proj) {
		try {
			return proj.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			ErrorDialog.openError(getControl().getShell(), "Error", null, e.getStatus());
		}
		return false;
	}				
	
	
	// ------- IStatusChangeListener --------
	
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusTool.applyToStatusLine(this, status);
	}

}