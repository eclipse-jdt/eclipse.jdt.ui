/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.internal.corext.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.CorextMessages;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * History for the open type dialog. Object and keys are both {@link TypeInfo}s.
 */
public class OpenTypeHistory extends TypeInfoHistory {
	
	private static class TypeHistoryDeltaListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {
			if (processDelta(event.getDelta())) {
				OpenTypeHistory.getInstance().markAsInconsistent();
			}
		}
		
		/**
		 * Computes whether the history needs a consistency check or not.
		 * 
		 * @param delta the Java element delta
		 * 
		 * @return <code>true</code> if consistency must be checked 
		 *  <code>false</code> otherwise.
		 */
		private boolean processDelta(IJavaElementDelta delta) {
			IJavaElement elem= delta.getElement();
			
			boolean isChanged= delta.getKind() == IJavaElementDelta.CHANGED;
			boolean isRemoved= delta.getKind() == IJavaElementDelta.REMOVED;
						
			switch (elem.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
					if (isRemoved || (isChanged && 
							(delta.getFlags() & IJavaElementDelta.F_CLOSED) != 0)) {
						return true;
					}
					return processChildrenDelta(delta);
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					if (isRemoved || (isChanged && (
							(delta.getFlags() & IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0 ||
							(delta.getFlags() & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0))) {
						return true;
					}
					return processChildrenDelta(delta);
				case IJavaElement.TYPE:
					if (isChanged && (delta.getFlags() & IJavaElementDelta.F_MODIFIERS) != 0) {
						return true;
					}
					// type children can be inner classes: fall through
				case IJavaElement.JAVA_MODEL:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.CLASS_FILE:
					if (isRemoved) {
						return true;
					}				
					return processChildrenDelta(delta);
				case IJavaElement.COMPILATION_UNIT:
					// Not the primary compilation unit. Ignore it 
					if (!JavaModelUtil.isPrimary((ICompilationUnit) elem)) {
						return false;
					}

					if (isRemoved || (isChanged && isUnknownStructuralChange(delta.getFlags()))) {
						return true;
					}
					return processChildrenDelta(delta);
				default:
					// fields, methods, imports ect
					return false;
			}	
		}
		
		private boolean isUnknownStructuralChange(int flags) {
			if ((flags & IJavaElementDelta.F_CONTENT) == 0)
				return false;
			return (flags & IJavaElementDelta.F_FINE_GRAINED) == 0; 
		}

		/*
		private boolean isPossibleStructuralChange(int flags) {
			return (flags & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_FINE_GRAINED)) == IJavaElementDelta.F_CONTENT;
		}
		*/		
		
		private boolean processChildrenDelta(IJavaElementDelta delta) {
			IJavaElementDelta[] children= delta.getAffectedChildren();
			for (int i= 0; i < children.length; i++) {
				if (processDelta(children[i])) {
					return true;
				}
			}
			return false;
		}
	}
	
	private static class UpdateJob extends Job {
		public static final String FAMILY= UpdateJob.class.getName();
		private UpdateJob fLastJob;
		public UpdateJob(UpdateJob lastJob) {
			super(CorextMessages.TypeInfoHistory_consistency_check);
			fLastJob= lastJob;
		}
		protected IStatus run(IProgressMonitor monitor) {
			OpenTypeHistory history= OpenTypeHistory.getInstance();
			try {
				if (fLastJob != null) {
					boolean joined= false;
					while (!joined) {
						try {
							fLastJob.join();
							joined= true;
						} catch (InterruptedException e) {
						}
					}
					fLastJob= null;
				}
				history.internalCheckConsistency(monitor);
			} finally {
				history.clearUpdateJob(this);
			}
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
		public boolean belongsTo(Object family) {
			return FAMILY.equals(family);
		}
	}
	
	private boolean fNeedsConsistencyCheck;
	private final IElementChangedListener fDeltaListener;
	private UpdateJob fUpdateJob;
	
	private static final String FILENAME= "OpenTypeHistory.xml"; //$NON-NLS-1$
	private static OpenTypeHistory fgInstance;
	
	public static synchronized OpenTypeHistory getInstance() {
		if (fgInstance == null)
			fgInstance= new OpenTypeHistory();
		return fgInstance;
	}
	
	public static void shutdown() {
		if (fgInstance == null)
			return;
		fgInstance.doShutdown();
	}
	
	private OpenTypeHistory() {
		super(FILENAME);
		fNeedsConsistencyCheck= true;
		fDeltaListener= new TypeHistoryDeltaListener();
		JavaCore.addElementChangedListener(fDeltaListener);
	}
	
	public synchronized void markAsInconsistent() {
		fNeedsConsistencyCheck= true;
		if (fUpdateJob != null) {
			fUpdateJob.cancel();
		}
		fUpdateJob= new UpdateJob(fUpdateJob);
		fUpdateJob.setPriority(Job.SHORT);
		fUpdateJob.schedule();
	}
	
	public synchronized boolean needConsistencyCheck() {
		return fNeedsConsistencyCheck;
	}

	public void checkConsistency(IProgressMonitor monitor) throws OperationCanceledException {
		synchronized (this) {
			if (!fNeedsConsistencyCheck)
				return;
		}
		// When joining the update job make sure that we don't hold looks
		// Otherwise the update Job can't continue normally. As a result
		// the update job could have already finished before we join it.
		// However this isn't a problem since the join will then be a NOP.
		if (hasUpdateJob()) {
			try {
				Platform.getJobManager().join(UpdateJob.FAMILY, monitor);
			} catch (OperationCanceledException e) {
				// Ignore and do the consistency check without
				// waiting for the update job.
			} catch (InterruptedException e) {
				// Ignore and do the consistency check without
				// waiting for the update job.
			}
		}
		// Since we gave up the lock when joining the update job
		// we have to re check the fNeedsConsistencyCheck flag
		synchronized(this) {
			if (!fNeedsConsistencyCheck)
				return;
			internalCheckConsistency(monitor);
		}
	}
	
	protected Object getKey(Object object) {
		return object;
	}

	private synchronized void internalCheckConsistency(IProgressMonitor monitor) throws OperationCanceledException {
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		List keys= new ArrayList(getKeys());
		monitor.beginTask(CorextMessages.TypeInfoHistory_consistency_check, keys.size());
		monitor.setTaskName(CorextMessages.TypeInfoHistory_consistency_check);
		for (Iterator iter= keys.iterator(); iter.hasNext();) {
			TypeInfo type= (TypeInfo)iter.next();
			try {
				IType jType= type.resolveType(scope);
				if (jType == null || !jType.exists()) {
					remove(type);
				} else {
					// copy over the modifiers since they may have changed
					type.setModifiers(jType.getFlags());
				}
			} catch (JavaModelException e) {
				remove(type);
			}
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			monitor.worked(1);
		}
		monitor.done();
		fNeedsConsistencyCheck= false;
	}
	
	private synchronized void clearUpdateJob(UpdateJob toClear) {
		if (fUpdateJob == toClear)
			fUpdateJob= null;
	}
	
	private synchronized boolean hasUpdateJob() {
		return fUpdateJob != null;
	}
	
	private void doShutdown() {
		JavaCore.removeElementChangedListener(fDeltaListener);
	}
}