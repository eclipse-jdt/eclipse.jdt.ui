/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.util;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.core.refactoring.Assert;

public class ParentProvider extends AbstractSyntaxTreeVisitorAdapter implements IParentProvider {

	private IParentTracker fTracker;

	public void setParentTracker(IParentTracker tracker) {
		fTracker= tracker;
		Assert.isNotNull(fTracker);	
	}
	
	public AstNode getParent() {
		if (fTracker == null)
			return null;
		return fTracker.getParent();
	}
}

