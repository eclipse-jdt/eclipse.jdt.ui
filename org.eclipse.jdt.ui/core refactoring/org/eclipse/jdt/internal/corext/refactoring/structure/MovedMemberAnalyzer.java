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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.dom.Bindings;

/**
 * Updates references in moved static members.
 * Accepts <code>BodyDeclaration</code>s.
 */
/* package */ class MovedMemberAnalyzer extends MoveStaticMemberAnalyzer {
//TODO:
//	- Reference to type inside moved type:
//	  - if originally resolved by qualification -> no problem
//	  - if originally resolved by import -> must add import in target too (qualify if import ambiguous)
	
	public MovedMemberAnalyzer(MoveStaticMembersRefactoring.ASTData ast,
			IBinding[] members, ITypeBinding source, ITypeBinding target) {
		super(ast, members, source, target);
	}
	
	public boolean targetNeedsSourceImport() {
		return fNeedsImport;
	}
	
	//---- types and fields --------------------------
		
	public boolean visit(SimpleName node) {
		if (node.isDeclaration() || isProcessed(node))
			return super.visit(node);
		IBinding binding= node.resolveBinding();
		if (isMovedMember(binding))
			return super.visit(node);
			
		if (isSourceAccess(binding))
			rewrite(node, fSource);
		return super.visit(node);
	}
	
	public boolean visit(QualifiedName node) {
		IBinding binding= node.resolveBinding();
		if (isMovedMember(binding))
			return super.visit(node);
		
		if (isSourceAccess(binding)) {
			rewrite(node, fSource);
			return false;
		}
		if (isTargetAccess(binding)) {
			SimpleName replace= (SimpleName)fAst.rewriter.createCopy(node.getName());
			fAst.rewriter.markAsReplaced(node, replace, null);
		}
		return super.visit(node);
	}
	
	public boolean visit(FieldAccess node) {
		IBinding binding= node.resolveFieldBinding();
		if (isMovedMember(binding))
			return super.visit(node);
			
		if (isSourceAccess(binding))
			rewrite(node, fSource);
		if (isTargetAccess(binding)) {
			fAst.rewriter.markAsRemoved(node.getExpression(), null);
		}
		return super.visit(node);
	}
	
	//---- method invocations ----------------------------------
	
	public boolean visit(MethodInvocation node) {
		IBinding binding= node.resolveMethodBinding();
		if (isMovedMember(binding))
			return super.visit(node);
			
		if (isSourceAccess(binding))
			rewrite(node, fSource);
		if (isTargetAccess(binding)) {
			fAst.rewriter.markAsRemoved(node.getExpression(), null);
		}	
		return super.visit(node);
	}
	
	//---- helper methods --------------------------------------
	
	private boolean isSourceAccess(IBinding binding) {
		if (binding instanceof IMethodBinding) {
			IMethodBinding method= (IMethodBinding)binding;
			return Modifier.isStatic(method.getModifiers()) && Bindings.equals(fSource, method.getDeclaringClass());
		} else if (binding instanceof ITypeBinding) {
			ITypeBinding type= (ITypeBinding)binding;
			return Modifier.isStatic(type.getModifiers()) && Bindings.equals(fSource, type.getDeclaringClass());			
		} else if (binding instanceof IVariableBinding) {
			IVariableBinding field= (IVariableBinding)binding;
			return field.isField() && Modifier.isStatic(field.getModifiers()) && Bindings.equals(fSource, field.getDeclaringClass());
		}
		return false;
	}
	
	private boolean isTargetAccess(IBinding binding) {
		if (binding instanceof IMethodBinding) {
			IMethodBinding method= (IMethodBinding)binding;
			return Modifier.isStatic(method.getModifiers()) && Bindings.equals(fTarget, method.getDeclaringClass());
		} else if (binding instanceof ITypeBinding) {
			ITypeBinding type= (ITypeBinding)binding;
			return Modifier.isStatic(type.getModifiers()) && Bindings.equals(fTarget, type.getDeclaringClass());			
		} else if (binding instanceof IVariableBinding) {
			IVariableBinding field= (IVariableBinding)binding;
			return field.isField() && Modifier.isStatic(field.getModifiers()) && Bindings.equals(fTarget, field.getDeclaringClass());
		}
		return false;
	}
}
