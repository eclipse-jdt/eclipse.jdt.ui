/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.core.refactoring;import java.util.ArrayList;import java.util.List;import org.eclipse.jdt.core.refactoring.text.AbstractTextBufferChange;import org.eclipse.jdt.internal.core.refactoring.Assert;

/**
 * A change context is used to given an <code>IChange</code> object access to several workspace
 * resource independend from whether the change is executed head less or not. 
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */ 
public class ChangeContext {

	private IChangeExceptionHandler fExceptionHandler;
	private IChange fFailedChange;
	private boolean fTryToUndo;
	private List fPerformedChanges= new ArrayList();

	/**
	 * Creates a new change context with the given exception handler.
	 * 
	 * @param handler object to handle exceptions caught during performing
	 *  a change. Must not be <code>null</code>
	 */
	public ChangeContext(IChangeExceptionHandler handler) {
		fExceptionHandler= handler;
		Assert.isNotNull(fExceptionHandler);
	}
		
	/**
	 * Returns the exception handler used to report exception back to the client.
	 * 
	 * @return the exception handler to report exceptions
	 */
	public IChangeExceptionHandler getExceptionHandler() {
		return fExceptionHandler;
	}
	
	/**
	 * Sets the change that caused an exception to the given value.
	 * 
	 * @param change the change that caused an exception
	 */
	public void setFailedChange(IChange change) {
		fFailedChange= change;
	}
	
	/**
	 * Returns the change that caused an exception.
	 * 
	 * @return the change that caused an exception
	 */
	public IChange getFailedChange() {
		return fFailedChange;
	}
	
	/**
	 * An unexpected error has occurred during execution of a change. Communicate
	 * to the outer operation that the successfully performed changes collected by 
	 * this change context are supposed to be undone.
	 * 
	 * @see ChangeContext#addPerformedChange(IChange)
	 */
	public void setTryToUndo() {
		fTryToUndo= true;
	}
	
	/**
	 * Returns <code>true</code> if an exception has been caught during execution of
	 * the change and the outer operation should try to undo all successfully performed
	 * changes. Otherwise <code>false</code> is returned.
	 * 
	 * @return if the outer operation should try to undo all successfully performed
	 *  changes
	 */
	public boolean getTryToUndo() {
		return fTryToUndo;
	}
	
	/**
	 * Adds the given change to the list of successfully performed changes.
	 * 
	 * @param the change executed successfully.
	 */
	public void addPerformedChange(IChange change) {
		if (change instanceof ICompositeChange && !(change instanceof AbstractTextBufferChange))
			return;
			
		fPerformedChanges.add(change);
	}
	
	/**
	 * Returns all changes that have been performed successfully
	 * 
	 * @return the successfully performed changes.
	 */
	public IChange[] getPerformedChanges() {
		return (IChange[])fPerformedChanges.toArray(new IChange[fPerformedChanges.size()]);
	}
	
	/**
	 * Removes all performed changes from this context.
	 */
	public void clearPerformedChanges() {
		fPerformedChanges= new ArrayList(1);
	}
}
