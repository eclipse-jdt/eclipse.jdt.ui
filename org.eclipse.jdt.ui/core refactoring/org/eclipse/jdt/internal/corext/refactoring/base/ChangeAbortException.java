/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.base;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;


/**
 * This exception is thrown if an unexpected errors occurs during execution
 * of a change object.
 */
public class ChangeAbortException extends RuntimeException {

	private Throwable fThrowable;


	/**
	 * Creates a new <code>ChangeAbortException</code> for the given throwable.
	 * 
	 * @param t the unexpected throwable caught while performing the change
	 * @param context the change context used to process the change
	 */
	public ChangeAbortException(Throwable t) {
		fThrowable= t;
		Assert.isNotNull(fThrowable);
	}
	
	/**
	 * Returns the <code>Throwable</code> that has caused the change to fail.
	 * 
	 * @return the throwable that has caused the change to fail
	 */
	public Throwable getThrowable() {
		return fThrowable;
	}
	
	/**
	 * Prints a stack trace out for the exception, and
	 * any nested exception that it may have embedded in
	 * its Status object.
	 */
	public void printStackTrace(PrintStream output) {
		synchronized (output) {
			output.print("ChangeAbortException: "); //$NON-NLS-1$
			super.printStackTrace(output);
			
			if (fThrowable != null) {
				output.print(RefactoringCoreMessages.getFormattedString("ChangeAbortException.wrapped", "ChangeAbortException: ")); //$NON-NLS-2$ //$NON-NLS-1$
				fThrowable.printStackTrace(output);
			}
		}
	}
	/**
	 * Prints a stack trace out for the exception, and
	 * any nested exception that it may have embedded in
	 * its Status object.
	 */
	public void printStackTrace(PrintWriter output) {
		synchronized (output) {
			output.print("ChangeAbortException: "); //$NON-NLS-1$
			super.printStackTrace(output);
			
			if (fThrowable != null) {
				output.print(RefactoringCoreMessages.getFormattedString("ChangeAbortException.wrapped", "ChangeAbortException: ")); //$NON-NLS-2$ //$NON-NLS-1$
				fThrowable.printStackTrace(output);
			}
		}
	}		
}
