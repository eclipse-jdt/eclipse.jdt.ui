/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import java.util.ArrayList;import java.util.Collections;import java.util.Iterator;import java.util.List;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.Change;import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.core.refactoring.base.ICompositeChange;

/**
 * Represents a composite change.
 */
public class CompositeChange extends Change implements ICompositeChange {

	private List fChanges;
	private IChange fUndoChange;
	private String fName;
	
	public CompositeChange(){
		this("CompositeChange>", new ArrayList(5));
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
	public void aboutToPerform() {
		for (Iterator iter= fChanges.iterator(); iter.hasNext(); ) {
			((IChange)iter.next()).aboutToPerform();
		}
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
			pm.beginTask("", fChanges.size());
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
			buff.append("\t").append(iter.next().toString()).append("\n");
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