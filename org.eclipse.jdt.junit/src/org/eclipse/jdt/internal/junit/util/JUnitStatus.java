/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jface.util.Assert;

/**
 * An implemention of IStatus. 
 * TO DO: Why is it duplicated, it should leverage the Status base class???
 */
public class JUnitStatus implements IStatus {
	private String fStatusMessage;
	private int fSeverity;
	
	/**
	 * Creates a status set to OK (no message)
	 */
	public JUnitStatus() {
		this(OK, null);
	}

	/**
	 * Creates a status .
	 * @param severity The status severity: ERROR, WARNING, INFO and OK.
	 * @param message The message of the status. Applies only for ERROR,
	 * WARNING and INFO.
	 */	
	public JUnitStatus(int severity, String message) {
		fStatusMessage= message;
		fSeverity= severity;
	}		
	
	/**
	 *  Returns if the status' severity is OK.
	 */
	public boolean isOK() {
		return fSeverity == IStatus.OK;
	}

	/**
	 *  Returns if the status' severity is WARNING.
	 */	
	public boolean isWarning() {
		return fSeverity == IStatus.WARNING;
	}

	/**
	 *  Returns if the status' severity is INFO.
	 */	
	public boolean isInfo() {
		return fSeverity == IStatus.INFO;
	}	

	/**
	 *  Returns if the status' severity is ERROR.
	 */	
	public boolean isError() {
		return fSeverity == IStatus.ERROR;
	}
	
	/**
	 * @see IStatus#getMessage
	 */
	public String getMessage() {
		return fStatusMessage;
	}
	
	/**
	 * Sets the status to ERROR.
	 * @param The error message (can be empty, but not null)
	 */	
	public void setError(String errorMessage) {
		Assert.isNotNull(errorMessage);
		fStatusMessage= errorMessage;
		fSeverity= IStatus.ERROR;
	}

	/**
	 * Sets the status to WARNING.
	 * @param The warning message (can be empty, but not null)
	 */		
	public void setWarning(String warningMessage) {
		Assert.isNotNull(warningMessage);
		fStatusMessage= warningMessage;
		fSeverity= IStatus.WARNING;
	}

	/**
	 * Sets the status to INFO.
	 * @param The info message (can be empty, but not null)
	 */		
	public void setInfo(String infoMessage) {
		Assert.isNotNull(infoMessage);
		fStatusMessage= infoMessage;
		fSeverity= IStatus.INFO;
	}	

	/**
	 * Sets the status to OK.
	 */		
	public void setOK() {
		fStatusMessage= null;
		fSeverity= IStatus.OK;
	}
	
	/*
	 * @see IStatus#matches(int)
	 */
	public boolean matches(int severityMask) {
		return (fSeverity & severityMask) != 0;
	}

	/**
	 * Returns always <code>false</code>.
	 * @see IStatus#isMultiStatus()
	 */
	public boolean isMultiStatus() {
		return false;
	}

	/*
	 * @see IStatus#getSeverity()
	 */
	public int getSeverity() {
		return fSeverity;
	}

	/*
	 * @see IStatus#getPlugin()
	 */
	public String getPlugin() {
		return JUnitPlugin.PLUGIN_ID;
	}

	/**
	 * Returns always <code>null</code>.
	 * @see IStatus#getException()
	 */
	public Throwable getException() {
		return null;
	}

	/**
	 * Returns always the error severity.
	 * @see IStatus#getCode()
	 */
	public int getCode() {
		return fSeverity;
	}

	/**
	 * Returns always <code>null</code>.
	 * @see IStatus#getChildren()
	 */
	public IStatus[] getChildren() {
		return new IStatus[0];
	}	

}