package org.eclipse.jdt.internal.ui.text.java;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Extension to <code>IProblemRequestor</code>.
 */
public interface IProblemRequestorExtension {
	
	/**
	 * Sets the progress monitor to this problem requestor.
	 * 
	 * @param monitor the progress monitor to be used
	 */
	void setProgressMonitor(IProgressMonitor monitor);
	
	/**
	 * Sets the active state of this problem requestor.
	 * 
	 * @param isActive the state of this problem requestor
	 */
	void setIsActive(boolean isActive);
}
