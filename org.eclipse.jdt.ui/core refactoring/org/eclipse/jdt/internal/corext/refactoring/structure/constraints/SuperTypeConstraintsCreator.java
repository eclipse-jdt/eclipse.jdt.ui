/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure.constraints;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;

/**
 * Type constraints creator to determine the necessary constraints to replace type occurrences by a given super type.
 * 
 * @since 3.1
 */
public final class SuperTypeConstraintsCreator extends HierarchicalASTVisitor {

	/** The constraint variable property */
	private static final String PROPERTY_CONSTRAINT_VARIABLE= "cv"; //$NON-NLS-1$

	/** The current method declarations being processed */
	private final Stack fCurrentMethods= new Stack();

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ArrayAccess)
	 */
	public final void endVisit(final ArrayAccess node) {

	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ArrayCreation)
	 */
	public final void endVisit(final ArrayCreation node) {

	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ArrayType)
	 */
	public final void endVisit(final ArrayType node) {

	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.SuperConstructorInvocation)
	 */
	public void endVisit(SuperConstructorInvocation node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.SuperFieldAccess)
	 */
	public void endVisit(SuperFieldAccess node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.SuperMethodInvocation)
	 */
	public void endVisit(SuperMethodInvocation node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.QualifiedName)
	 */
	public void endVisit(QualifiedName node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.FieldAccess)
	 */
	public void endVisit(FieldAccess node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.FieldDeclaration)
	 */
	public void endVisit(FieldDeclaration node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.InstanceofExpression)
	 */
	public final void endVisit(final InstanceofExpression node) {
		final Expression expression= node.getLeftOperand();
		final Type type= node.getRightOperand();
		final ConstraintVariable2 leftVariable= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ConstraintVariable2 rightVariable= (ConstraintVariable2) type.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (leftVariable != null && rightVariable != null) {
			final ITypeBinding leftBinding= expression.resolveTypeBinding();
			final ITypeBinding rightBinding= type.resolveBinding();
			if (leftBinding != null && leftBinding.isClass() && rightBinding != null && rightBinding.isClass()) {

				// return createOrOrSubtypeConstraint(leftVariable, rightVariable);
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ClassInstanceCreation)
	 */
	public void endVisit(ClassInstanceCreation node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ConstructorInvocation)
	 */
	public void endVisit(ConstructorInvocation node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.MethodDeclaration)
	 */
	public final void endVisit(final MethodDeclaration node) {
		Assert.isNotNull(node);
		fCurrentMethods.pop();
		final IMethodBinding binding= node.resolveBinding();
		if (binding != null) {
			// TODO implement
		}
	}

	//	
	// private ITypeConstraint[] getReturnTypeConstraint(Expression invocation, IMethodBinding methodBinding){
	// if (methodBinding == null || methodBinding.isConstructor() || methodBinding.getReturnType().isPrimitive())
	// return new ITypeConstraint[0];
	// ConstraintVariable returnTypeVariable= fConstraintVariableFactory.makeReturnTypeVariable(methodBinding);
	// ConstraintVariable invocationVariable= fConstraintVariableFactory.makeExpressionOrTypeVariable(invocation, getContext());
	// return fTypeConstraintFactory.createDefinesConstraint(invocationVariable, returnTypeVariable);
	// }
	//	
	// private ITypeConstraint[] getArgumentConstraints(List arguments, IMethodBinding methodBinding){
	// List result= new ArrayList(arguments.size());
	// for (int i= 0, n= arguments.size(); i < n; i++) {
	// Expression argument= (Expression) arguments.get(i);
	// ConstraintVariable expressionVariable= fConstraintVariableFactory.makeExpressionOrTypeVariable(argument, getContext());
	// ConstraintVariable parameterTypeVariable= fConstraintVariableFactory.makeParameterTypeVariable(methodBinding, i);
	// ITypeConstraint[] argConstraint= fTypeConstraintFactory.createSubtypeConstraint(expressionVariable, parameterTypeVariable);
	// result.addAll(Arrays.asList(argConstraint));
	// }
	// return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	// }
	//	

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.MethodInvocation)
	 */
	public final void endVisit(final MethodInvocation node) {
		final IMethodBinding binding= node.resolveMethodBinding();
		if (binding != null) {
			// TODO implement
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
	 */
	public final boolean visit(final MethodDeclaration node) {
		fCurrentMethods.push(node);
		return super.visit(node);
	}

	/** The type constraint model to solve */
	private final SuperTypeConstraintsModel fModel;

	/**
	 * Creates a new super type constraints creator.
	 * 
	 * @param model the model to create the type constraints for
	 */
	public SuperTypeConstraintsCreator(final SuperTypeConstraintsModel model) {
		Assert.isNotNull(model);

		fModel= model;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ReturnStatement)
	 */
	public final void endVisit(final ReturnStatement node) {
		final Expression expression= node.getExpression();
		if (expression != null) {
			final ConstraintVariable2 descendant= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
			if (descendant != null) {
				final MethodDeclaration declaration= (MethodDeclaration) fCurrentMethods.peek();
				if (declaration != null) {
					final IMethodBinding binding= declaration.resolveBinding();
					if (binding != null) {
						final ConstraintVariable2 ancestor= fModel.createReturnTypeVariable(binding);
						if (ancestor != null)
							node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
						fModel.createSubtypeConstraint(descendant, ancestor);
					}
				}
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ConditionalExpression)
	 */
	public final void endVisit(final ConditionalExpression node) {
		ConstraintVariable2 thenVariable= null;
		ConstraintVariable2 elseVariable= null;
		final Expression thenExpression= node.getThenExpression();
		if (thenExpression != null)
			thenVariable= (ConstraintVariable2) thenExpression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final Expression elseExpression= node.getElseExpression();
		if (elseExpression != null)
			elseVariable= (ConstraintVariable2) elseExpression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ITypeBinding binding= node.resolveTypeBinding();
		if (binding != null) {
			final ConstraintVariable2 ancestor= fModel.createIndependantTypeVariable(binding);
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
			if (thenVariable != null)
				fModel.createSubtypeConstraint(thenVariable, ancestor);
			if (elseVariable != null)
				fModel.createSubtypeConstraint(elseVariable, ancestor);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ThisExpression)
	 */
	public final void endVisit(final ThisExpression node) {
		final ITypeBinding binding= node.resolveTypeBinding();
		if (binding != null)
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, fModel.createPlainTypeVariable(binding));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.SingleVariableDeclaration)
	 */
	public final void endVisit(final SingleVariableDeclaration node) {
		final ConstraintVariable2 ancestor= (ConstraintVariable2) node.getType().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (ancestor != null) {
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
			final Expression expression= node.getInitializer();
			if (expression != null) {
				final ConstraintVariable2 descendant= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
				if (descendant != null)
					fModel.createSubtypeConstraint(descendant, ancestor);
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ArrayInitializer)
	 */
	public final void endVisit(final ArrayInitializer node) {
		final ITypeBinding binding= node.resolveTypeBinding();
		if (binding != null) {
			final ConstraintVariable2 ancestor= fModel.createIndependantTypeVariable(binding);
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
			Expression expression= null;
			ConstraintVariable2 descendant= null;
			for (final Iterator iterator= node.expressions().iterator(); iterator.hasNext();) {
				expression= (Expression) iterator.next();
				descendant= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
				if (descendant != null)
					fModel.createSubtypeConstraint(descendant, ancestor);
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.jdt.core.dom.ThisExpression)
	 */
	public final boolean visit(final ThisExpression node) {
		return false;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.Assignment)
	 */
	public final void endVisit(final Assignment node) {
		final ConstraintVariable2 ancestor= (ConstraintVariable2) node.getLeftHandSide().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ConstraintVariable2 descendant= (ConstraintVariable2) node.getRightHandSide().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (ancestor != null && descendant != null) {
			fModel.createSubtypeConstraint(descendant, ancestor);
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.CastExpression)
	 */
	public final void endVisit(final CastExpression node) {
		final ConstraintVariable2 descendant= (ConstraintVariable2) node.getType().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ConstraintVariable2 ancestor= (ConstraintVariable2) node.getExpression().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, descendant);
		if (descendant != null)
			fModel.createSubtypeConstraint(descendant, ancestor);
		fModel.createCastVariable(node, ancestor);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.VariableDeclarationStatement)
	 */
	public final void endVisit(final VariableDeclarationStatement node) {
		endVisit(node.fragments(), node.getType());
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.VariableDeclarationExpression)
	 */
	public final void endVisit(final VariableDeclarationExpression node) {
		endVisit(node.fragments(), node.getType());
	}

	/**
	 * End of visit the variable declaration fragment list.
	 * 
	 * @param fragments the fragments (element type: <code>VariableDeclarationFragment</code>)
	 * @param type the type of the fragments
	 */
	protected final void endVisit(final List fragments, final Type type) {
		final ConstraintVariable2 ancestor= (ConstraintVariable2) type.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (ancestor != null) {
			ConstraintVariable2 descendant= null;
			VariableDeclarationFragment fragment= null;
			for (int index= 0; index < fragments.size(); index++) {
				fragment= (VariableDeclarationFragment) fragments.get(index);
				descendant= (ConstraintVariable2) fragment.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
				if (descendant != null)
					fModel.createSubtypeConstraint(descendant, ancestor);
			}
			type.getParent().setProperty(PROPERTY_CONSTRAINT_VARIABLE, ancestor);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.VariableDeclarationFragment)
	 */
	public final void endVisit(final VariableDeclarationFragment node) {
		final Expression initializer= node.getInitializer();
		if (initializer != null)
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, initializer.getProperty(PROPERTY_CONSTRAINT_VARIABLE));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ParenthesizedExpression)
	 */
	public final void endVisit(final ParenthesizedExpression node) {
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, node.getExpression().getProperty(PROPERTY_CONSTRAINT_VARIABLE));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.Type)
	 */
	public final void endVisit(final Type node) {
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, fModel.createTypeVariable(node));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.jdt.core.dom.AnnotationTypeDeclaration)
	 */
	public final boolean visit(final AnnotationTypeDeclaration node) {
		return false;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.jdt.core.dom.Comment)
	 */
	public final boolean visit(final Comment node) {
		return false;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.jdt.core.dom.Type)
	 */
	public final boolean visit(final Type node) {
		return false;
	}
}