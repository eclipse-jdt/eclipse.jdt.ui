package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.IProblemRequestor;
 
/**
 * Extends the problem requestor with live cycle methods.
 */ 
public interface IProblemRequestorExtension extends IProblemRequestor {
	
	/**
	 * Marks the beginning of problem reporting.
	 */
	void beginReporting();
	
	/**
	 * Marks the end of problem reporting.
	 */
	void endReporting();
}
