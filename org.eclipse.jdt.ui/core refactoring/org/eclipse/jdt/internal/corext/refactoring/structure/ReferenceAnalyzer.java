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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;


/* package */ class ReferenceAnalyzer extends MoveStaticMemberAnalyzer {
	
	private ITypeBinding fTarget;
	
	public ReferenceAnalyzer(ITypeBinding source, ITypeBinding target, IBinding[] members, MoveStaticMembersRefactoring.ASTData ast) {
		super(ast, members);
		fTarget= target;
	}
	
	public boolean needsTargetImport() {
		return fNeedsImport;
	}
	
	//---- Moved members are handled by the MovedMemberAnalyzer --------------

	public boolean visit(TypeDeclaration node) {
		if (isMovedMember(node.resolveBinding()))
			return false;
		return super.visit(node);
	}
	
	public boolean visit(VariableDeclarationFragment node) {
		if (isMovedMember(node.resolveBinding()))
			return false;
		return super.visit(node);
	}
	
	public boolean visit(MethodDeclaration node) {
		if (isMovedMember(node.resolveBinding()))
			return false;
		return super.visit(node);
	}
	
	
	//---- types and fields --------------------------
		
	public boolean visit(SimpleName node) {
		if (node.isDeclaration() || !isMovedMember(node.resolveBinding()) || isProcessed(node))
			return false;
		rewrite(node, fTarget);
		return super.visit(node);
	}
	
	public boolean visit(QualifiedName node) {
		if (!isMovedMember(node.resolveBinding()))
			return super.visit(node);
		rewrite(node, fTarget);
		return false;
	}
	
	public boolean visit(FieldAccess node) {
		if (!isMovedMember(node.resolveFieldBinding()))
			return super.visit(node);
		rewrite(node, fTarget);
		return super.visit(node);
	}
	
	//---- method invocations ----------------------------------
	
	public boolean visit(MethodInvocation node) {
		if (!isMovedMember(node.resolveMethodBinding()))
			return super.visit(node);
		rewrite(node, fTarget);
		return super.visit(node);
	}	
}
