/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.IRefactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.core.refactoring.NullChange;

/**
 * useful for tests
 */
public class NullRefactoring implements IRefactoring {
	
	public IChange createChange(IProgressMonitor pm){
		pm.beginTask("", 1);
		pm.worked(1);
		pm.done();
		return new NullChange("NullRefactoring");
	}

	public RefactoringStatus checkPreconditions(IProgressMonitor pm){
		pm.beginTask("", 1);
		pm.worked(1);
		pm.done();
		return new RefactoringStatus();
	}
	
	public String getName(){
		return "Null Refactoring";
	}
}