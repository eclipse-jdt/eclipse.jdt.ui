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
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;

public class OccurrencesFinder extends ASTVisitor {
	
	private IBinding fTarget;
	private List fUsages= new ArrayList();
	private List fWriteUsages= new ArrayList();

	public OccurrencesFinder(IBinding target) {
		super();
		fTarget= target;
	}

	public List getUsages() {
		return fUsages;
	}
	
	public List getWriteUsages() {
		return fWriteUsages;
	}
	
	public boolean visit(QualifiedName node) {
		match(node, fUsages, node.resolveBinding());
		return super.visit(node);
	}

	public boolean visit(SimpleName node) {
		match(node, fUsages, node.resolveBinding());
		return super.visit(node);
	}

	/*
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.ConstructorInvocation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		match(node.getName(), fUsages, node.resolveConstructorBinding());
		Expression expression = node.getExpression();
		if (expression != null)
			expression.accept(this);
		
		List list = node.arguments();
		for (Iterator iter= list.iterator(); iter.hasNext();)
			((ASTNode)iter.next()).accept(this);

		return false;
	}
	public boolean visit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		Name name= getName(lhs);
		if (name != null) 
			match(name, fWriteUsages, name.resolveBinding());	
		lhs.accept(this);
		node.getRightHandSide().accept(this);
		return false;
	}
	
	public boolean visit(SingleVariableDeclaration node) {
		if (node.getInitializer() != null)
			match(node.getName(), fWriteUsages, node.resolveBinding());
		return super.visit(node);
	}

	public boolean visit(VariableDeclarationFragment node) {
		if (node.getInitializer() != null)
			match(node.getName(), fWriteUsages, node.resolveBinding());
		return super.visit(node);
	}

	public boolean visit(PrefixExpression node) {
		PrefixExpression.Operator operator= node.getOperator();	
		if (operator == Operator.INCREMENT || operator == Operator.DECREMENT) {
			Expression operand= node.getOperand();
			Name name= getName(operand);
			if (name != null) 
				match(name, fWriteUsages, name.resolveBinding());				
		}
		return super.visit(node);
	}

	public boolean visit(PostfixExpression node) {
		Expression operand= node.getOperand();
		Name name= getName(operand);
		if (name != null) 
			match(name, fWriteUsages, name.resolveBinding());
		return super.visit(node);
	}

	private void match(Name node, List result, IBinding binding) {
		
		if (binding == null)
			return;
			
		if (binding.equals(fTarget)) {
			result.add(node);
			return;
		}
		
		String otherKey= binding.getKey();
		String targetKey= fTarget.getKey();
					
		if (targetKey != null && otherKey != null) {
			if (targetKey.equals(otherKey))
				result.add(node);
		}
	}

	private Name getName(Expression expression) {
		if (expression instanceof SimpleName)
			return ((SimpleName)expression);
		else if (expression instanceof QualifiedName)
			return ((QualifiedName)expression);
		else if (expression instanceof FieldAccess)
			return ((FieldAccess)expression).getName();
		return null;
	}	
}
