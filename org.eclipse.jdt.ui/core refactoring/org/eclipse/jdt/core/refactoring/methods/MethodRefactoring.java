/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.methods;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.core.refactoring.Assert;

/*
 * non java-doc
 * not API
 */
abstract class MethodRefactoring extends Refactoring{
	
	private IMethod fMethod;
	
	public MethodRefactoring(IJavaSearchScope scope, IMethod method) {
		super(scope);
		Assert.isNotNull(method);
		Assert.isTrue(method.exists(), "method must exist");
		fMethod= method;
	}

	public MethodRefactoring(IMethod method) {
		super();
		fMethod= method;
	}
	
	public final IMethod getMethod(){
		return fMethod;
	}
}
