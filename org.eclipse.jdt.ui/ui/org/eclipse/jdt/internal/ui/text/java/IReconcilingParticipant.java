package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */



/**
 * Interface of an object participating in reconciling.
 */
public interface IReconcilingParticipant {
	
	/**
	 * Called after reconciling has been finished.
	 */
	void reconciled();
}
