/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring.infra;import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.IChangeExceptionHandler;

public class TestExceptionHandler implements IChangeExceptionHandler {

	public void handle(ChangeContext context, IChange change, Exception e) {
		if (e instanceof RuntimeException)
			throw (RuntimeException)e;
			
		throw new ChangeAbortException(e);	
	}
}
