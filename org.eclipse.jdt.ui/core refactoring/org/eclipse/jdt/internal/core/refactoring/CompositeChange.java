/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import java.util.ArrayList;import java.util.Collections;import java.util.Iterator;import java.util.List;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.Change;import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.ICompositeChange;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

/**
 * Represents a composite change.
 */
public class CompositeChange extends Change implements ICompositeChange {

	private List fChanges;
	private IChange fUndoChange;
	private String fName;
	
	public CompositeChange(){
		this(RefactoringCoreMessages.getString("CompositeChange.CompositeChange"), new ArrayList(5)); //$NON-NLS-1$
	}
		
	public CompositeChange(String name){
		this(name, new ArrayList(5));
	}
	
	public CompositeChange(String name, int initialCapacity){
		this(name, new ArrayList(initialCapacity));
	}
		
	private CompositeChange(String name, List changes){
		fChanges= changes;
		fName= name;
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public RefactoringStatus aboutToPerform(ChangeContext context, IProgressMonitor pm) {
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
	public void performed() {
		for (Iterator iter= fChanges.iterator(); iter.hasNext(); ) {
			((IChange)iter.next()).performed();
		}
	} 
	
	/**
	 * @see IChange#getUndoChange
	 */
	public final IChange getUndoChange(){
		return fUndoChange;
	}
	
	protected void setUndoChange(IChange change){
	   /*
	 	* subclasses may want to perform the undo 
	 	* smarter than the default implementation
	 	*/ 
		fUndoChange= change;
	}
	
	public void addChange(IChange change){
		fChanges.add(change);
	}
	
	public IChange[] getChildren() {
		if (fChanges == null)
			return null;
		return (IChange[])fChanges.toArray(new IChange[fChanges.size()]);
	}
	
	protected final List getChanges() {
		return fChanges;
	}
	
	/**
	 * to reverse a composite means reversing all changes in reverse order
	 */ 
	private List createUndoList(ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		List undoList= null;
		try {
			undoList= new ArrayList(fChanges.size());
			Iterator iter= fChanges.iterator();
			pm.beginTask("", fChanges.size()); //$NON-NLS-1$
			while (iter.hasNext()){
				try {
					IChange each= (IChange)iter.next();
					each.perform(context, new SubProgressMonitor(pm, 1));
					undoList.add(each.getUndoChange());
					context.addPerformedChange(each);
				} catch (Exception e) {
					handleException(context, e);
				}
			};
			pm.done();
			Collections.reverse(undoList);
			return undoList;
		} catch (Exception e) {
			handleException(context, e);
		}
		if (undoList == null)
			undoList= new ArrayList(0);
		return undoList;	
	}

	/**
	 * @see IChange#perform
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		if (!isActive()){
			fUndoChange= new NullChange();
		} else{
			fUndoChange= new CompositeChange(fName, createUndoList(context, pm));
		}	
		pm.done();
	}
	
	public String toString(){
		StringBuffer buff= new StringBuffer();
		Iterator iter= fChanges.iterator();
		while (iter.hasNext()){
			buff.append("\t").append(iter.next().toString()).append("\n"); //$NON-NLS-2$ //$NON-NLS-1$
		};	
		return buff.toString();
	}
	
	
	public String getName(){
		return fName;
	}
	
	public IJavaElement getCorrespondingJavaElement(){
		return null;
	}
	
	/**
	 * @see IChange#setActive
	 * Apart setting the active/non-active status on itself 
	 * this method also activates/disactivates all subchanges of this change.
	 */
	public void setActive(boolean active){
		super.setActive(active);
		Iterator iter= fChanges.iterator();
		while (iter.hasNext()){
			((IChange)iter.next()).setActive(active);
		}
	}	
}