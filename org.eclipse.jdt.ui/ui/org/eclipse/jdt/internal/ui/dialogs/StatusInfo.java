package org.eclipse.jdt.internal.ui.dialogs;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;

/**
 * Used to set a status line:
 * Can be an error, warning or ok. For error and warning states,
 * a message describes the problem
 */
public class StatusInfo {
	
	private final int OK= 0;
	private final int WARNING= 1;
	private final int ERROR= 2;
	
	private String fStatusMessage;
	private int fType;
	
	public StatusInfo() {
		fStatusMessage= null;
		fType= OK;
	}
	
	public boolean isOK() {
		return fType == OK;
	}
	
	public boolean isWarning() {
		return fType == WARNING;
	}
	
	public boolean isError() {
		return fType == ERROR;
	}
	
	public String getMessage() {
		return fStatusMessage;
	}
	
	public void setError(String errorMessage) {
		fStatusMessage= errorMessage;
		fType= ERROR;
	}
	
	public void setWarning(String warningMessage) {
		fStatusMessage= warningMessage;
		fType= WARNING;
	}
	
	public void setOK() {
		fStatusMessage= null;
		fType= OK;
	}
	
	public void setStatus(IStatus status) {
		switch (status.getSeverity()) {
			case IStatus.OK:
				fType= OK; break;
			case IStatus.ERROR:
				fType= ERROR; break;
			default:
				fType= WARNING;
		}									
		fStatusMessage= status.getMessage();
	}

	public void setStatus(IStatus status, String templateString) {
		fStatusMessage= MessageFormat.format(templateString, new Object[] { status.getMessage() });
	}
	
	/**
	 * Compare two StatusInfos. The more severe is returned:
	 * An error is more severe than a warning, and a warning is more severe
	 * than ok.
	 */
	public StatusInfo getMoreSevere(StatusInfo other) {
		if (other != null && (other.fType > fType)) {
			return other;
		}
		return this;
	}
	
	

}