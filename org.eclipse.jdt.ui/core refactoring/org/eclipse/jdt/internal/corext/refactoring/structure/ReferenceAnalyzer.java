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
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Updates references to moved static members.
 * Accepts <code>CompilationUnit</code>s.
 */
/* package */ class ReferenceAnalyzer extends MoveStaticMemberAnalyzer {
	
	public ReferenceAnalyzer(CompilationUnitRewrite cuRewrite, IBinding[] members, ITypeBinding target, ITypeBinding source) {
		super(cuRewrite, members, source, target);
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
	
	public boolean visit(FieldDeclaration node) {
		//see bug 42383: multiple VariableDeclarationFragments not supported:
		VariableDeclarationFragment singleFragment= (VariableDeclarationFragment) node.fragments().get(0);
		if (isMovedMember(singleFragment.resolveBinding()))
			return false; // don't update javadoc of moved field here
		return super.visit(node);
	}
	
	public boolean visit(MethodDeclaration node) {
		if (isMovedMember(node.resolveBinding()))
			return false;
		return super.visit(node);
	}
	
	
	//---- types and fields --------------------------
		
	public boolean visit(SimpleName node) {
		if (! node.isDeclaration() && isMovedMember(node.resolveBinding()) && ! isProcessed(node))
			rewrite(node, fTarget);
		return false;
	}
	
	public boolean visit(QualifiedName node) {
		if (isMovedMember(node.resolveBinding())) {
			if (node.getParent() instanceof ImportDeclaration) {
				fCuRewrite.getImportRewrite().removeImport(node.resolveTypeBinding());
				fCuRewrite.getImportRewrite().addImport(fTarget.getQualifiedName() + '.' + node.getName().getIdentifier());
			} else {
				rewrite(node, fTarget);
			}
			return false;
		} else {
			return super.visit(node);
		}
	}
	
	public boolean visit(FieldAccess node) {
		if (isMovedMember(node.resolveFieldBinding()))
			rewrite(node, fTarget);
		return super.visit(node);
	}
	
	//---- method invocations ----------------------------------
	
	public boolean visit(MethodInvocation node) {
		if (isMovedMember(node.resolveMethodBinding()))
			rewrite(node, fTarget);
		return super.visit(node);
	}
	
	//---- javadoc references ----------------------------------
	
	public boolean visit(MemberRef node) {
		if (isMovedMember(node.resolveBinding()))
			rewrite(node, fTarget);
		return false;
	}
	
	public boolean visit(MethodRef node) {
		if (isMovedMember(node.resolveBinding()))
			rewrite(node, fTarget);
		return false;
	}
}
