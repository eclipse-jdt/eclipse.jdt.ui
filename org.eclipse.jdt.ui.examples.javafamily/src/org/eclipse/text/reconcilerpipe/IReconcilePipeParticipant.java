/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.text.reconcilerpipe;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;

/**
 * Reconcile pipe participant.
 * 
 * @since 3.0
 */
public interface IReconcilePipeParticipant {

	boolean isLastParticipant();
	boolean isFirstParticipant();

	/**
	 * Sets the participant which is in front of this participant in the pipe.
	 * <p>
	 * Note: This method must called at most once per participant.
	 * </p>
	 * 
	 * @param participant
	 * @exception org.eclipse.jface.text.Assert#AssertionFailedException if called more than once
	 */
	void setPreviousParticipant(IReconcilePipeParticipant participant);

	/**
	 * Activates incremental reconciling of the specified dirty region.
	 * As a dirty region might span multiple content types, the segment of the
	 * dirty region which should be investigated is also provided to this 
	 * reconciling strategy. The given regions refer to the document passed into
	 * the most recent call of <code>setDocument</code>.
	 *
	 * @param dirtyRegion the document region which has been changed
	 * @param subRegion the sub region in the dirty region which should be reconciled
	 * @return an array with reconcile results 
	 */
	IReconcileResult[] reconcile(DirtyRegion dirtyRegion, IRegion subRegion);

	/**
	 * Activates non-incremental reconciling. The reconciling strategy is just told
	 * that there are changes and that it should reconcile the given partition of the
	 * document most recently passed into <code>setDocument</code>.
	 *
	 * @param partition the document partition to be reconciled
	 * @return an array with reconcile results 
	 */
	IReconcileResult[] reconcile(IRegion partition);

	/**
	 * Sets the progress monitor to this participant.
	 * 
	 * @param monitor the progress monitor to be used
	 */
	void setProgressMonitor(IProgressMonitor monitor);

	/**
	 * Returns the progress monitor used to report progress.
	 *
	 * @return a progress monitor or null if no progress monitor is provided
	 */
	public IProgressMonitor getProgressMonitor();

	/**
	 * Tells this reconcile pipe participant on which model it will
	 * work. This method will be called before any other method 
	 * and can be called multiple times. The regions passed to the
	 * other methods always refer to the most recent document 
	 * passed into this method.
	 *
	 * @param inputModel the model on which this strategy will work
	 */
	void setInputModel(ITextModel inputModel);
}
