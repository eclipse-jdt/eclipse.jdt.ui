/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.DialogPage;

/**
 * A settable IStatus
 * Can be an error, warning, info or ok. For error, info and warning states,
 * a message describes the problem
 */
public class StatusInfo implements IStatus {
	
	private String fStatusMessage;
	private int fSeverity;
	
	public StatusInfo() {
		fStatusMessage= null;
		fSeverity= OK;
	}
	
	public boolean isOK() {
		return fSeverity == IStatus.OK;
	}
	
	public boolean isWarning() {
		return fSeverity == IStatus.WARNING;
	}
	
	public boolean isInfo() {
		return fSeverity == IStatus.INFO;
	}	
	
	public boolean isError() {
		return fSeverity == IStatus.ERROR;
	}
	
	/**
	 * @see IStatus#getMessage
	 */
	public String getMessage() {
		return fStatusMessage;
	}
	
	public void setError(String errorMessage) {
		fStatusMessage= errorMessage;
		fSeverity= IStatus.ERROR;
	}
	
	public void setWarning(String warningMessage) {
		fStatusMessage= warningMessage;
		fSeverity= IStatus.WARNING;
	}
	
	public void setInfo(String infoMessage) {
		fStatusMessage= infoMessage;
		fSeverity= IStatus.INFO;
	}	
	
	public void setOK() {
		fStatusMessage= null;
		fSeverity= IStatus.OK;
	}
	
	/**
	 * @see IStatus#matches(int)
	 */
	public boolean matches(int severityMask) {
		return (fSeverity & severityMask) != 0;
	}

	/**
	 * @see IStatus#isMultiStatus()
	 */
	public boolean isMultiStatus() {
		return false;
	}

	/**
	 * @see IStatus#getSeverity()
	 */
	public int getSeverity() {
		return fSeverity;
	}

	/**
	 * @see IStatus#getPlugin()
	 */
	public String getPlugin() {
		return JavaUI.ID_PLUGIN;
	}

	/**
	 * @see IStatus#getException()
	 */
	public Throwable getException() {
		return null;
	}

	/**
	 * @see IStatus#getCode()
	 */
	public int getCode() {
		return fSeverity;
	}

	/**
	 * @see IStatus#getChildren()
	 */
	public IStatus[] getChildren() {
		return new IStatus[0];
	}	

}