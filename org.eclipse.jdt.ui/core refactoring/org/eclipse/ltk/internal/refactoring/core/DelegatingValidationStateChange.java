/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.internal.refactoring.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.ltk.refactoring.core.IDynamicValidationStateChange;
import org.eclipse.ltk.refactoring.core.IValidationStateListener;


public class DelegatingValidationStateChange extends CompositeChange implements IDynamicValidationStateChange {

	private IDynamicValidationStateChange[] fDynamicChanges;
		
	public DelegatingValidationStateChange(IChange[] changes) {
		List dynamic= new ArrayList();
		addAll(changes);
		for (int i= 0; i < changes.length; i++) {
			if (changes[i] instanceof IDynamicValidationStateChange) {
				dynamic.add(changes[i]);
			}
		}
		fDynamicChanges= (IDynamicValidationStateChange[]) dynamic.toArray(new IDynamicValidationStateChange[dynamic.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected IChange createUndoChange(IChange[] childUndos) throws CoreException {
		return new DelegatingValidationStateChange(childUndos);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void addValidationStateListener(final IValidationStateListener listener) {
		for (int i= 0; i < fDynamicChanges.length; i++) {
			final int index= i;
			final IDynamicValidationStateChange dc= fDynamicChanges[i];
			Platform.run(new ISafeRunnable() {
				public void run() throws Exception {
					if (dc != null)
						dc.addValidationStateListener(listener);
				}
				public void handleException(Throwable e) {
					fDynamicChanges[index]= null;
					RefactoringCorePlugin.logRemovedListener(e);
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeValidationStateListener(final IValidationStateListener listener) {
		for (int i= 0; i < fDynamicChanges.length; i++) {
			final int index= i;
			final IDynamicValidationStateChange dc= fDynamicChanges[i];
			Platform.run(new ISafeRunnable() {
				public void run() throws Exception {
					if (dc != null)
						dc.removeValidationStateListener(listener);
				}
				public void handleException(Throwable e) {
					fDynamicChanges[index]= null;
					RefactoringCorePlugin.logRemovedListener(e);
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void aboutToPerformChange(final IChange change) {
		for (int i= 0; i < fDynamicChanges.length; i++) {
			final int index= i;
			final IDynamicValidationStateChange dc= fDynamicChanges[i];
			Platform.run(new ISafeRunnable() {
				public void run() throws Exception {
					if (dc != null)
						dc.aboutToPerformChange(change);
				}
				public void handleException(Throwable e) {
					fDynamicChanges[index]= null;
					RefactoringCorePlugin.logRemovedListener(e);
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void changePerformed(final IChange change, final Exception e) {
		for (int i= 0; i < fDynamicChanges.length; i++) {
			final int index= i;
			final IDynamicValidationStateChange dc= fDynamicChanges[i];
			Platform.run(new ISafeRunnable() {
				public void run() throws Exception {
					if (dc != null)
						dc.changePerformed(change, e);
				}
				public void handleException(Throwable e) {
					fDynamicChanges[index]= null;
					RefactoringCorePlugin.logRemovedListener(e);
				}
			});
		}
	}
}
