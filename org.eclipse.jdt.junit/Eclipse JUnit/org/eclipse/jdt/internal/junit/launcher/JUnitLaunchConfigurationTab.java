package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Common function for Java launch configuration tabs.
 */
public abstract class JUnitLaunchConfigurationTab implements ILaunchConfigurationTab {
	
	// this page's control
	private Control fControl;

	// The launch configuration dialog that owns this tab
	private ILaunchConfigurationDialog fLaunchConfigurationDialog;
	
	// Error message and status message
	private String fErrorMessage;
	private String fMessage;
	
	/**
	 * Returns the dialog this page is contained in.
	 * 
	 * @return launch config dialog, or <code>null</code>
	 */
	protected ILaunchConfigurationDialog getLaunchDialog() {
		return fLaunchConfigurationDialog;
	}	
		
	/**
	 * Updates the buttons and message in this page's launch
	 * config dialog. Has no effect if currently performing a
	 * batch update.
	 * 
	 * @see JavaLaunchConfigurationTab#setBatchUpdate(boolean)
	 */
	protected void refreshStatus() {
		getLaunchDialog().updateButtons();
		getLaunchDialog().updateMessage();
	}
				
	/**
	 * @see ILaunchConfigurationTab#getControl()
	 */
	public Control getControl() {
		return fControl;
	}

	/**
	 * Sets this page's control.
	 * @param control The control to set
	 */
	protected void setControl(Control control) {
		fControl = control;
	}

	/**
	 * @see ILaunchConfigurationTab#canChangePage()
	 */
	public boolean okToLeave() {
		return isValid();
	}

	/**
	 * @see ILaunchConfigurationTab#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/**
	 * @see ILaunchConfigurationTab#getMessage()
	 */
	public String getMessage() {
		return fMessage;
	}

	/**
	 * By default, do nothing.
	 * 
	 * @see ILaunchConfigurationTab#launched(ILaunch)
	 */
	public void launched(ILaunch launch) {
	}

	/**
	 * @see ILaunchConfigurationTab#setLaunchConfigurationDialog(ILaunchConfigurationDialog)
	 */
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		fLaunchConfigurationDialog = dialog;
	}
	
	/**
	 * Sets this page's error message, possilby <code>null</code>.
	 * 
	 * @param errorMessage the error message or <code>null</code>
	 */
	protected void setErrorMessage(String errorMessage) {
		fErrorMessage = errorMessage;
	}

	/**
	 * Sets this page's message, possilby <code>null</code>.
	 * 
	 * @param message the message or <code>null</code>
	 */
	protected void setMessage(String message) {
		fMessage = message;
	}
	
	/**
	 * Convenience method to return the launch manager.
	 * 
	 * @return the launch manager
	 */
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}	
	
	/**
	 * Returns the current Java element context from which to initialize
	 * default settings, or <code>null</code> if none.
	 * 
	 * @return Java element context.
	 */
	protected IJavaElement getContext() {
		IWorkbenchPage page = JDIDebugUIPlugin.getActivePage();
		if (page != null) {
			ISelection selection = page.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection)selection;
				if (!ss.isEmpty()) {
					Object obj = ss.getFirstElement();
					if (obj instanceof IJavaElement) {
						return (IJavaElement)obj;
					}
					if (obj instanceof IResource) {
						IJavaElement je = JavaCore.create((IResource)obj);
						if (je == null) {
							IProject pro = ((IResource)obj).getProject();
							je = JavaCore.create(pro);
						}
						if (je != null) {
							return je;
						}
					}
				}
			}
			IEditorPart part = page.getActiveEditor();
			if (part != null) {
				IEditorInput input = part.getEditorInput();
				return (IJavaElement) input.getAdapter(IJavaElement.class);
			}
		}
		return null;
	}
	
	/**
	 * Set the java project attribute based on the IJavaElement.
	 */
	protected void initializeJavaProject(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		IJavaProject javaProject = javaElement.getJavaProject();
		String name = null;
		if (javaProject != null && javaProject.exists()) {
			name = javaProject.getElementName();
		}
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, name);
	}	
}

