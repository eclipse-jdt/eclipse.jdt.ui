/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.methods;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public abstract class MethodRefactoring extends Refactoring{
	
	private IMethod fMethod;
	
	public MethodRefactoring(IJavaSearchScope scope, IMethod method) {
		super(scope);
		Assert.isNotNull(method);
		Assert.isTrue(method.exists(), RefactoringCoreMessages.getString("MethodRefactoring.assert.must_exist")); //$NON-NLS-1$
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
