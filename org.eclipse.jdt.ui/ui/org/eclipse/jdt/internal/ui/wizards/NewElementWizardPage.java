/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;

public abstract class NewElementWizardPage extends WizardPage {

	private IStatus fCurrStatus;
	
	// added for 1GEUK5C, 1GEUUN9, 1GEUNW2
	private boolean fPageVisible;
	
	public NewElementWizardPage(String name) {
		super(name);
		fPageVisible= false;	
	}
		
	// ---- WizardPage ----------------
	
	/**
	 * @see WizardPage#becomesVisible
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		fPageVisible= visible;
		// policy: wizards are not allowed to come up with an error message
		if (visible && fCurrStatus.matches(IStatus.ERROR)) {
			StatusInfo status= new StatusInfo();
			status.setError("");  //$NON-NLS-1$
			fCurrStatus= status;
		} 
		updateStatus(fCurrStatus);
	}	

	/**
	 * Updates the status line and the ok button depending on the status
	 */
	protected void updateStatus(IStatus status) {
		fCurrStatus= status;
		setPageComplete(!status.matches(IStatus.ERROR));
		if (fPageVisible) {
			StatusUtil.applyToStatusLine(this, status);
		}
	}
			
}