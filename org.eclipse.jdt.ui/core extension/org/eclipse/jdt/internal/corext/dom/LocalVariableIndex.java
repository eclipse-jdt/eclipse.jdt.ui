/*
 * (c) Copyright IBM Corp. 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class LocalVariableIndex extends ASTVisitor {
	
	private int fTopIndex;
	
	public static int perform(MethodDeclaration method) {
		LocalVariableIndex counter= new LocalVariableIndex();
		method.accept(counter);
		return counter.fTopIndex;
	}

	public boolean visit(SingleVariableDeclaration node) {
		fTopIndex= Math.max(fTopIndex, node.resolveBinding().getVariableId());
		return true;
	}
	
	public boolean visit(VariableDeclarationFragment node) {
		fTopIndex= Math.max(fTopIndex, node.resolveBinding().getVariableId());
		return true;
	}
}
