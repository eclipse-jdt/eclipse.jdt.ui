/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.jdt.core.JavaModelException;

/**
 * An <code>ChangeExceptionHandler</code> is informed about any exception that occurrs during
 * performing a change. Implementors of this interface can control if the change is supposed to
 * be continued or if it is to be aborted.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */ 
public interface IChangeExceptionHandler {


	/**
	 * Handles the given exception.
	 * 
	 * @param context the change context passed to <code>IChange.perform</code>
	 * @param change the change that caused the exception
	 * @param exception the exception cought during executing the change
	 * @exception ChangeAbortException if the change is to be aborted
	 */
	public void handle(ChangeContext context, IChange change, Exception exception) throws ChangeAbortException;	
}
