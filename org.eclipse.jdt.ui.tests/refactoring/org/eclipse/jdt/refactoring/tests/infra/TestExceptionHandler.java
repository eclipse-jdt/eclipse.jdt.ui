/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests.infra;import org.eclipse.jdt.core.refactoring.ChangeAbortException;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.IChangeExceptionHandler;

public class TestExceptionHandler implements IChangeExceptionHandler {

	public void handle(ChangeContext context, IChange change, Exception e) {
		if (e instanceof RuntimeException)
			throw (RuntimeException)e;
			
		throw new ChangeAbortException(e);	
	}
}
