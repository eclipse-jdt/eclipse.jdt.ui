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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;

/**
 * Abstract implementation of reconcile pipe participant.
 * 
 * @since 3.0
 */
public abstract class AbstractReconcilePipeParticipant implements IReconcilePipeParticipant {

	private IReconcilePipeParticipant fNextParticipant;
	private IReconcilePipeParticipant fPreviousParticipant;
	private IProgressMonitor fProgressMonitor;
	protected ITextModel fInputModel;

	/**
	 * Creates an intermediate reconcile participant which adds
	 * the given participant to the pipe.
	 */
	public AbstractReconcilePipeParticipant(IReconcilePipeParticipant participant) {
		Assert.isNotNull(participant);
		fNextParticipant= participant;
		fNextParticipant.setPreviousParticipant(this);
	}

	/**
	 * Creates the last reconcile participant of the pipe.
	 */
	public AbstractReconcilePipeParticipant() {
	}

	public boolean isLastParticipant() {
		return fNextParticipant == null;
	}

	public boolean isFirstParticipant() {
		return fPreviousParticipant == null;
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.IReconcilerResultCollector#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		fProgressMonitor= monitor;

		if (!isLastParticipant())
			fNextParticipant.setProgressMonitor(monitor);
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.IReconcilerResultCollector#getProgressMonitor()
	 */
	public IProgressMonitor getProgressMonitor() {
		return fProgressMonitor;
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.IReconcilePipeParticipant#reconcile(org.eclipse.jface.text.IRegion)
	 */
	public final IReconcileResult[] reconcile(IRegion partition) {
		IReconcileResult[] result= reconcileModel(null, partition);
		if (!isLastParticipant()) {
			fNextParticipant.setInputModel(getModel());
			IReconcileResult[] nextResult= fNextParticipant.reconcile(partition);
			return merge(result, convertToInputModel(nextResult));
		} else
			return result;
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.IReconcilePipeParticipant#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	public final IReconcileResult[] reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		IReconcileResult[] result= reconcileModel(dirtyRegion, subRegion);
		if (!isLastParticipant()) {
			fNextParticipant.setInputModel(getModel());
			IReconcileResult[] nextResult= fNextParticipant.reconcile(dirtyRegion, subRegion);
			return merge(result, convertToInputModel(nextResult));
		} else
			return result;
	}

	
	/**
	 * Reconciles the model of this reconcile pipe participant. The
	 * result is based on the input model.
	 * 
	 * @param dirtyRegion the document region which has been changed
	 * @param subRegion the sub region in the dirty region which should be reconciled
	 * @return an array with reconcile results 
	 */
	abstract protected IReconcileResult[] reconcileModel(DirtyRegion dirtyRegion, IRegion subRegion);

	protected IReconcileResult[] convertToInputModel(IReconcileResult[] inputResults) {
		return inputResults;
	}
	
	private IReconcileResult[] merge(IReconcileResult[] results1, IReconcileResult[] results2) {
		if (results1 == null)
			return results2;

		if (results2 == null)
			return results1;
		
		// XXX: not yet performance optimized 
		Collection collection= new ArrayList(Arrays.asList(results1));
		collection.addAll(Arrays.asList(results2));
		return (IReconcileResult[])collection.toArray(new IReconcileResult[collection.size()]); 
	}

	/*
	 * @see IProgressMonitor#isCanceled() 
	 */
	protected final boolean isCanceled() {
		return fProgressMonitor != null && fProgressMonitor.isCanceled();
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.IReconcilePipeParticipant#setPreviousParticipant(org.eclipse.text.reconcilerpipe.IReconcilePipeParticipant)
	 */
	public void setPreviousParticipant(IReconcilePipeParticipant participant) {
		Assert.isNotNull(participant);
		Assert.isTrue(fPreviousParticipant == null);
		fPreviousParticipant= participant;
	}

	/*
	 * @see IReconcilePipeParticipant#setInputModel(Object)
	 */
	public void setInputModel(ITextModel inputModel) {
		fInputModel= inputModel;
		
		if (!isLastParticipant())
			fNextParticipant.setInputModel(getModel());
	}

	public ITextModel getInputModel() {
		return fInputModel;
	}
	
	abstract public ITextModel getModel();

	/*
	 * @see IReconcilePipeParticipant#initialReconcile()
	 */
	public void initialReconcile() {
	}
}
