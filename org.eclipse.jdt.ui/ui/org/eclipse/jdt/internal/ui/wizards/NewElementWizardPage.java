/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.text.MessageFormat;import java.util.MissingResourceException;import java.util.ResourceBundle;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.wizard.WizardPage;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;

public abstract class NewElementWizardPage extends WizardPage {

	private IStatus fCurrStatus;
	private ResourceBundle fResourceBundle;
	
	public NewElementWizardPage(String name, ResourceBundle bundle) {
		super(name);
		
		fResourceBundle= bundle;
		if (bundle != null) {
			setTitle(bundle.getString(name + ".title"));
			setDescription(bundle.getString(name + ".description"));
		}		
	}
	
	public NewElementWizardPage(String name) {
		this(name, JavaPlugin.getResourceBundle());
	}
		
	// ---- String Resources ----------------

	/**
	 * Returns the resource bundel currently used
	 * Can be overwritten by clients to use a differnent resource bundle
	 * than the JavaUi resource bundle
	 */		
	protected ResourceBundle getResourceBundle() {
		return fResourceBundle;
	}
	
	/**
	 * Returns the resource string for the given key
	 */
	protected String getResourceString(String key) {
		try {
			return fResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";
		} catch (NullPointerException e) {
			return "!" + key + "!";
		}
	}

	/**
	 * Returns a formatted resources string for the given key
	 */	
	protected String getFormattedString(String key, String[] args) {
		String str= getResourceString(key);
		return MessageFormat.format(str, args);
	}

	/**
	 * Returns a formatted resources string for the given key
	 */		
	protected String getFormattedString(String key, String arg) {
		String str= getResourceString(key);
		return MessageFormat.format(str, new String[] { arg });
	}
		
	// ---- WizardPage ----------------
	
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
	public abstract IRunnableWithProgress getRunnable();
	

	/**
	 * Updates the status line and the ok button depending on the status
	 */
	protected void updateStatus(IStatus status) {
		fCurrStatus= status;
		if (isCurrentPage()) {
			setPageComplete(!status.matches(IStatus.ERROR));
			StatusTool.applyToStatusLine(this, status);
		}
	}
			
}