/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class LocalVariableIndex extends ASTVisitor {
	
	private int fTopIndex;
	
	public static int perform(MethodDeclaration method) {
		// we have to find the outermost method declaratio since a local or anonymous
		// type can reference final variables from the outer scope.
		MethodDeclaration target= method;
		while (ASTNodes.getParent(target, ASTNode.METHOD_DECLARATION) != null) {
			target= (MethodDeclaration)ASTNodes.getParent(target, ASTNode.METHOD_DECLARATION);
		}
		return doPerform(target);
	}

	public static int perform(Initializer initializer) {
		return doPerform(initializer);
	}

	private static int doPerform(BodyDeclaration node) {
		LocalVariableIndex counter= new LocalVariableIndex();
		node.accept(counter);
		return counter.fTopIndex;
	}

	public boolean visit(SingleVariableDeclaration node) {
		handleVariableBinding(node.resolveBinding());
		return true;
	}
	
	public boolean visit(VariableDeclarationFragment node) {
		handleVariableBinding(node.resolveBinding());
		return true;
	}
	
	private void handleVariableBinding(IVariableBinding binding) {
		if (binding == null)
			return;
		fTopIndex= Math.max(fTopIndex, binding.getVariableId());
	}
}
