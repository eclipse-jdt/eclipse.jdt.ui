/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.text.MessageFormat;import java.util.MissingResourceException;import java.util.ResourceBundle;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.wizard.WizardPage;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;

public abstract class NewElementWizardPage extends WizardPage {

	private IStatus fCurrStatus;
	
	// added for 1GEUK5C, 1GEUUN9, 1GEUNW2
	private boolean fPageVisible;
	
	/**
	 * @deprecated
	 */
	public NewElementWizardPage(String name, ResourceBundle bundle) {
		super(name);
		if (bundle != null) {
			setTitle(bundle.getString(name + ".title")); //$NON-NLS-1$
			setDescription(bundle.getString(name + ".description")); //$NON-NLS-1$
		}
		fPageVisible= false;	
	}
	
	public NewElementWizardPage(String name) {
		this(name, NewWizardMessages.getResourceBundle());
		fPageVisible= false;
	}
		
	// ---- String Resources ----------------

	/**
	 * Returns the resource bundel currently used
	 * Can be overwritten by clients to use a differnent resource bundle
	 * than the JavaUi resource bundle
	 * @deprecated
	 */		
	protected ResourceBundle getResourceBundle() {
		return NewWizardMessages.getResourceBundle();
	}
	
	/**
	 * Returns the resource string for the given key
	 * @deprecated
	 */
	protected String getResourceString(String key) {
		return NewWizardMessages.getString(key);
	}

	/**
	 * Returns a formatted resources string for the given key
	 * @deprecated
	 */	
	protected String getFormattedString(String key, String[] args) {
		return NewWizardMessages.getFormattedString(key, args);
	}

	/**
	 * Returns a formatted resources string for the given key
	 * @deprecated
	 */		
	protected String getFormattedString(String key, String arg) {
		return NewWizardMessages.getFormattedString(key, arg);
	}
		
	// ---- WizardPage ----------------
	
	/**
	 * @see WizardPage#becomesVisible
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		fPageVisible= visible;
		updateStatus(fCurrStatus);
	}			
	
	/**
	 * Create a runnable that creates the element specified by this wizard.
	 * Only called when status is ok.
	 * Can return null, if wizard does not create anything
	 */
	public abstract IRunnableWithProgress getRunnable();
	

	/**
	 * Updates the status line and the ok button depending on the status
	 */
	protected void updateStatus(IStatus status) {
		fCurrStatus= status;
		setPageComplete(!status.matches(IStatus.ERROR));
		if (fPageVisible) {
			StatusTool.applyToStatusLine(this, status);
		}
	}
			
}