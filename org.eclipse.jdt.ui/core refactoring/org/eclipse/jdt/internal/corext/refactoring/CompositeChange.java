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
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

/**
 * Represents a composite change.
 */
public class CompositeChange extends Change implements ICompositeChange {

	private List fChanges;
	private IChange fUndoChange;
	private String fName;
	private boolean fIsSynthetic;
	
	public CompositeChange() {
		this(RefactoringCoreMessages.getString("CompositeChange.CompositeChange")); //$NON-NLS-1$
	}

	public CompositeChange(String name, IChange[] changes) {
		this(name, new ArrayList(changes.length));
		addAll(changes);
	}
			
	public CompositeChange(String name) {
		this(name, new ArrayList(5));
	}
	
	public CompositeChange(String name, int initialCapacity) {
		this(name, new ArrayList(initialCapacity));
	}
		
	private CompositeChange(String name, List changes) {
		Assert.isNotNull(changes);
		fChanges= changes;
		fName= name;
	}
	
	protected boolean isSynthetic() {
		return fIsSynthetic;
	}
	
	protected void setSynthetic(boolean synthetic) {
		fIsSynthetic= synthetic;
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public final RefactoringStatus aboutToPerform(ChangeContext context, IProgressMonitor pm) {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", fChanges.size() + 1); //$NON-NLS-1$
		result.merge(super.aboutToPerform(context, new SubProgressMonitor(pm,1)));
		for (Iterator iter= fChanges.iterator(); iter.hasNext(); ) {
			result.merge(((IChange)iter.next()).aboutToPerform(context, new SubProgressMonitor(pm,1)));
		}
		return result;
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public final void performed() {
		for (Iterator iter= fChanges.iterator(); iter.hasNext(); ) {
			((IChange)iter.next()).performed();
		}
	} 
	
	/* non java-doc
	 * @see IChange#getUndoChange
	 */
	public final IChange getUndoChange() {
		return fUndoChange;
	}

	public void addAll(IChange[] changes) {
		for (int i= 0; i < changes.length; i++) {
			add(changes[i]);
		}
	}
	
	public void add(IChange change) {
		if (change != null) {
			Assert.isTrue(change.getParent() == null);
			fChanges.add(change);
			change.internalSetParent(this);
		}
	}
	
	public void merge(CompositeChange change) {
		IChange[] others= change.getChildren();
		for (int i= 0; i < others.length; i++) {
			IChange other= others[i];
			change.remove(other);
			add(other);
		}
	}
	
	public boolean remove(IChange change) {
		Assert.isNotNull(change);
		boolean result= fChanges.remove(change);
		if (result) {
			change.internalSetParent(null);
		}
		return result;
		
	}
		
	public IChange[] getChildren() {
		if (fChanges == null)
			return null;
		return (IChange[])fChanges.toArray(new IChange[fChanges.size()]);
	}
	
	final List getChanges() {
		return fChanges;
	}
	
	/**
	 * to reverse a composite means reversing all changes in reverse order
	 */ 
	private IChange[] createUndoList(ChangeContext context, IProgressMonitor pm) throws CoreException {
		IChange[] undoList= null;
		try {
			undoList= new IChange[fChanges.size()];
			pm.beginTask("", fChanges.size()); //$NON-NLS-1$
			int size= fChanges.size();
			int last= size - 1;
			for (int i= 0; i < size; i++) {
				try {
					IChange each= (IChange)fChanges.get(i);
					each.perform(context, new SubProgressMonitor(pm, 1));
					undoList[last - i]= each.getUndoChange();
					context.addPerformedChange(each);
				} catch (Exception e) {
					handleException(context, e);
				}
			}
			pm.done();
			return undoList;
		} catch (Exception e) {
			handleException(context, e);
		}
		if (undoList == null)
			undoList= new IChange[0];
		return undoList;	
	}

	/* non java-doc
	 * @see IChange#perform
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.getString("CompositeChange.performingChangesTask.name")); //$NON-NLS-1$
		if (!isActive()) {
			fUndoChange= new NullChange();
		} else {
			fUndoChange= createUndoChange(createUndoList(context, new SubProgressMonitor(pm, 1)));
		}	
		pm.done();
	}
	
	/**
	 * Hook to create an undo change.
	 * 
	 * @return the undo change
	 * 
	 * @throws CoreException if an undo change can't be created
	 */
	protected IChange createUndoChange(IChange[] childUndos) throws CoreException {
		return new CompositeChange(fName, childUndos);
	}
	
	
	/* non java-doc
	 * for debugging only
	 */	
	public String toString() {
		StringBuffer buff= new StringBuffer();
		buff.append("CompositeChange\n"); //$NON-NLS-1$
		for (Iterator iter= fChanges.iterator(); iter.hasNext();) {
			buff.append("<").append(iter.next().toString()).append("/>\n"); //$NON-NLS-2$ //$NON-NLS-1$
		}
		return buff.toString();
	}
	
	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return fName;
	}

	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */	
	public Object getModifiedLanguageElement() {
		return null;
	}

	/* non java-doc
	 * @see IChange#setActive
	 * This method activates/disactivates all subchanges of this change. The
	 * change itself is always active to ensure that sub changes are always
	 * considered if they are active.
	 */
	public void setActive(boolean active) {
		for (Iterator iter= fChanges.iterator(); iter.hasNext(); ) {
			((IChange)iter.next()).setActive(active);
		}
	}	
	
	/*non java-doc
	 * @see IChange#isUndoable()
	 * Composite can be undone iff all its sub-changes can be undone.
	 */
	public boolean isUndoable() {
		for (Iterator iter= fChanges.iterator(); iter.hasNext(); ) {
			IChange each= (IChange)iter.next();
			if (! each.isUndoable())
				return false;
		}
		return true;
	}
}
