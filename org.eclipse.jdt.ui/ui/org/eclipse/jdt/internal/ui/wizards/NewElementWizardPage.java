/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class NewElementWizardPage extends WizardPage {

	private StatusInfo fCurrStatus;
	private ResourceBundle fResourceBundle;
	
	public NewElementWizardPage(String name, ResourceBundle bundle) {
		super(name);
		
		fResourceBundle= bundle;
		if (bundle != null) {
			setTitle(bundle.getString(name + ".title"));
			setDescription(bundle.getString(name + ".description"));
		}		
	}
		
	// ---- String Resources ----------------
	
	protected ResourceBundle getResourceBundle() {
		return fResourceBundle;
	}
	
	protected String getResourceString(String key) {
		try {
			return fResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";
		} catch (NullPointerException e) {
			return "!" + key + "!";
		}
	}
	
	protected String getFormattedString(String key, String[] args) {
		String str= getResourceString(key);
		return MessageFormat.format(str, args);
	}
	
	protected String getFormattedString(String key, String arg) {
		String str= getResourceString(key);
		return MessageFormat.format(str, new String[] { arg });
	}
		
	// ---- WizardPage ----------------
	
	/**
	 * Called by the wizard to create the new element
	 */		
	public boolean finishPage() {
		IRunnableWithProgress runnable= getRunnable();
		if (runnable != null) {
			return invokeRunnable(runnable);
		}
		return true;
	}
		
	
	/**
	 * Utility method: call a runnable in a WorkbenchModifyDelegatingOperation
	 */
	protected boolean invokeRunnable(IRunnableWithProgress runnable) {
		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(runnable);
		try {
			getWizard().getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			Shell shell= getShell();
			if (!ExceptionHandler.handle(e.getTargetException(), shell, fResourceBundle, "NewElementWizardPage.op_error.")) {
				MessageDialog.openError(shell, "Error", e.getTargetException().getMessage());
			}
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * @see WizardPage#becomesVisible
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			updateStatus(fCurrStatus);
		}
		super.setVisible(visible);
	}			
	
	/**
	 * Create a runnable that creates the element specified by this wizard.
	 * Only called when status is ok.
	 * Can return null, if wizard does not create anything
	 */
	public IRunnableWithProgress getRunnable() {
		return null;
	}
	
	/**
	 * Updates the status line and the ok button debending on the status
	 */
	protected void updateStatus(StatusInfo status) {
		fCurrStatus= status;
		if (isCurrentPage()) {
			setPageComplete(!fCurrStatus.isError());
			String message= fCurrStatus.getMessage();
			if (fCurrStatus.isError() && !"".equals(message)) {
				setErrorMessage(message);
				setMessage(null);
			} else if (fCurrStatus.isWarning()) {
				setErrorMessage(null);
				setMessage(message);
			} else {
				setErrorMessage(null);
				setMessage(null);
			}
		}
	}
			
}