/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.methods;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;

public abstract class MethodRefactoring extends Refactoring{
	
	private IMethod fMethod;
	
	protected MethodRefactoring(IMethod method) {
		Assert.isNotNull(method);
		Assert.isTrue(method.exists(), RefactoringCoreMessages.getString("MethodRefactoring.assert.must_exist")); //$NON-NLS-1$
		fMethod= method;
	}
	
	public final IMethod getMethod(){
		return fMethod;
	}
}
