/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IResource;

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

import org.w3c.dom.Element;

/**
 * History for the open type dialog. Object and keys are both {@link TypeInfo}s.
 */
public class OpenTypeHistory extends History {
	
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
		public UpdateJob() {
			super(CorextMessages.TypeInfoHistory_consistency_check);
		}
		protected IStatus run(IProgressMonitor monitor) {
			OpenTypeHistory history= OpenTypeHistory.getInstance();
			history.internalCheckConsistency(monitor);
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
		public boolean belongsTo(Object family) {
			return FAMILY.equals(family);
		}
	}
	
	// Needs to be volatile since accesses aren't synchronized.
	private volatile boolean fNeedsConsistencyCheck;
	// Map of cached time stamps
	private Map fTimestampMapping;
	
	private final IElementChangedListener fDeltaListener;
	private final UpdateJob fUpdateJob;
	private final TypeInfoFactory fTypeInfoFactory;
	
	private static final String FILENAME= "OpenTypeHistory.xml"; //$NON-NLS-1$
	private static final String NODE_ROOT= "typeInfoHistroy"; //$NON-NLS-1$
	private static final String NODE_TYPE_INFO= "typeInfo"; //$NON-NLS-1$
	private static final String NODE_NAME= "name"; //$NON-NLS-1$
	private static final String NODE_PACKAGE= "package"; //$NON-NLS-1$
	private static final String NODE_ENCLOSING_NAMES= "enclosingTypes"; //$NON-NLS-1$
	private static final String NODE_PATH= "path"; //$NON-NLS-1$
	private static final String NODE_MODIFIERS= "modifiers";  //$NON-NLS-1$
	private static final String NODE_TIMESTAMP= "timestamp"; //$NON-NLS-1$
	private static final char[][] EMPTY_ENCLOSING_NAMES= new char[0][0];
	
	private static OpenTypeHistory fgInstance;
	
	public static synchronized OpenTypeHistory getInstance() {
		if (fgInstance == null)
			fgInstance= new OpenTypeHistory();
		return fgInstance;
	}
	
	public static synchronized void shutdown() {
		if (fgInstance == null)
			return;
		fgInstance.doShutdown();
	}
	
	private OpenTypeHistory() {
		super(FILENAME, NODE_ROOT, NODE_TYPE_INFO);
		fTypeInfoFactory= new TypeInfoFactory();
		fTimestampMapping= new HashMap();
		fNeedsConsistencyCheck= true;
		load();
		fDeltaListener= new TypeHistoryDeltaListener();
		JavaCore.addElementChangedListener(fDeltaListener);
		fUpdateJob= new UpdateJob();
		// It is not necessary anymore that the update job has a rule since
		// markAsInconsistent isn't synchronized anymore. See bugs
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=128399 and
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=135278
		// for details.
		fUpdateJob.setPriority(Job.SHORT);
	}
	
	public void markAsInconsistent() {
		fNeedsConsistencyCheck= true;
		// cancel the old job. If no job is running this is a NOOP.
		fUpdateJob.cancel();
		fUpdateJob.schedule();
	}
	
	public boolean needConsistencyCheck() {
		return fNeedsConsistencyCheck;
	}

	public void checkConsistency(IProgressMonitor monitor) throws OperationCanceledException {
		if (!fNeedsConsistencyCheck)
			return;
		if (fUpdateJob.getState() == Job.RUNNING) {
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
		if (!fNeedsConsistencyCheck)
			return;
		internalCheckConsistency(monitor);
	}
	
	public synchronized boolean contains(TypeInfo type) {
		return super.contains(type);
	}

	public synchronized void accessed(TypeInfo info) {
		fTimestampMapping.put(info, new Long(info.getContainerTimestamp()));
		super.accessed(info);
	}

	public synchronized TypeInfo remove(TypeInfo info) {
		fTimestampMapping.remove(info);
		return (TypeInfo)super.remove(info);
	}

	public synchronized TypeInfo[] getTypeInfos() {
		Collection values= getValues();
		int size= values.size();
		TypeInfo[] result= new TypeInfo[size];
		int i= size - 1;
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			result[i]= (TypeInfo)iter.next();
			i--;
		}
		return result;
	}

	public synchronized TypeInfo[] getFilteredTypeInfos(TypeInfoFilter filter) {
		Collection values= getValues();
		List result= new ArrayList();
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			TypeInfo type= (TypeInfo)iter.next();
			if ((filter == null || filter.matchesHistoryElement(type)) && !TypeFilter.isFiltered(type.getFullyQualifiedName()))
				result.add(type);
		}
		Collections.reverse(result);
		return (TypeInfo[])result.toArray(new TypeInfo[result.size()]);
		
	}
	
	protected Object getKey(Object object) {
		return object;
	}

	private synchronized void internalCheckConsistency(IProgressMonitor monitor) throws OperationCanceledException {
		// Setting fNeedsConsistencyCheck is necessary here since 
		// markAsInconsistent isn't synchronized.
		fNeedsConsistencyCheck= true;
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		List typesToCheck= new ArrayList(getKeys());
		monitor.beginTask(CorextMessages.TypeInfoHistory_consistency_check, typesToCheck.size());
		monitor.setTaskName(CorextMessages.TypeInfoHistory_consistency_check);
		for (Iterator iter= typesToCheck.iterator(); iter.hasNext();) {
			TypeInfo type= (TypeInfo)iter.next();
			long currentTimestamp= type.getContainerTimestamp();
			Long lastTested= (Long)fTimestampMapping.get(type);
			if (lastTested != null && currentTimestamp == lastTested.longValue() && !type.isContainerDirty())
				continue;
			try {
				IType jType= type.resolveType(scope);
				if (jType == null || !jType.exists()) {
					remove(type);
				} else {
					// copy over the modifiers since they may have changed
					type.setModifiers(jType.getFlags());
					fTimestampMapping.put(type, new Long(currentTimestamp));
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
	
	private void doShutdown() {
		JavaCore.removeElementChangedListener(fDeltaListener);
		save();
	}
	
	protected Object createFromElement(Element type) {
		String name= type.getAttribute(NODE_NAME);
		String pack= type.getAttribute(NODE_PACKAGE);
		char[][] enclosingNames= getEnclosingNames(type);
		String path= type.getAttribute(NODE_PATH);
		int modifiers= 0;
		try {
			modifiers= Integer.parseInt(type.getAttribute(NODE_MODIFIERS));
		} catch (NumberFormatException e) {
			// take zero
		}
		TypeInfo info= fTypeInfoFactory.create(
			pack.toCharArray(), name.toCharArray(), enclosingNames, modifiers, path);
		long timestamp= IResource.NULL_STAMP;
		String timestampValue= type.getAttribute(NODE_TIMESTAMP);
		if (timestampValue != null && timestampValue.length() > 0) {
			try {
				timestamp= Long.parseLong(timestampValue);
			} catch (NumberFormatException e) {
				// take null stamp
			}
		}
		long currentTimestamp= info.getContainerTimestamp();
		if (timestamp != IResource.NULL_STAMP && currentTimestamp != IResource.NULL_STAMP && timestamp == currentTimestamp) {
			fTimestampMapping.put(info, new Long(currentTimestamp));
		}
		return info;
	}

	protected void setAttributes(Object object, Element typeElement) {
		TypeInfo type= (TypeInfo)object;
		typeElement.setAttribute(NODE_NAME, type.getTypeName());
		typeElement.setAttribute(NODE_PACKAGE, type.getPackageName());
		typeElement.setAttribute(NODE_ENCLOSING_NAMES, type.getEnclosingName());
		typeElement.setAttribute(NODE_PATH, type.getPath());
		typeElement.setAttribute(NODE_MODIFIERS, Integer.toString(type.getModifiers()));
		typeElement.setAttribute(NODE_TIMESTAMP, Long.toString(type.getContainerTimestamp()));
	}

	private char[][] getEnclosingNames(Element type) {
		String enclosingNames= type.getAttribute(NODE_ENCLOSING_NAMES);
		if (enclosingNames.length() == 0)
			return EMPTY_ENCLOSING_NAMES;
		StringTokenizer tokenizer= new StringTokenizer(enclosingNames, "."); //$NON-NLS-1$
		List names= new ArrayList();
		while(tokenizer.hasMoreTokens()) {
			String name= tokenizer.nextToken();
			names.add(name.toCharArray());
		}
		return (char[][])names.toArray(new char[names.size()][]);
	}
}