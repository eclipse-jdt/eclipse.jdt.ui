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
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraintVariable2;

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
		final ConstraintVariable2 first= (ConstraintVariable2) node.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ConstraintVariable2 second= (ConstraintVariable2) node.getArray().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (first instanceof TypeConstraintVariable2 && second instanceof TypeConstraintVariable2)
			fModel.createEqualsConstraint((TypeConstraintVariable2) first, (TypeConstraintVariable2) second);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ArrayCreation)
	 */
	public final void endVisit(final ArrayCreation node) {
		final ConstraintVariable2 first= (ConstraintVariable2) node.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ConstraintVariable2 second= (ConstraintVariable2) node.getType().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (first instanceof TypeConstraintVariable2 && second instanceof TypeConstraintVariable2)
			fModel.createEqualsConstraint((TypeConstraintVariable2) first, (TypeConstraintVariable2) second);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ArrayType)
	 */
	public final void endVisit(final ArrayType node) {
		final ConstraintVariable2 first= (ConstraintVariable2) node.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ConstraintVariable2 second= (ConstraintVariable2) node.getComponentType().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (first instanceof TypeConstraintVariable2 && second instanceof TypeConstraintVariable2)
			fModel.createEqualsConstraint((TypeConstraintVariable2) first, (TypeConstraintVariable2) second);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.SingleVariableDeclaration)
	 */
	public void endVisit(SingleVariableDeclaration node) {
		super.endVisit(node);
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
	public void endVisit(InstanceofExpression node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ClassInstanceCreation)
	 */
	public void endVisit(ClassInstanceCreation node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ConditionalExpression)
	 */
	public void endVisit(ConditionalExpression node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ConstructorInvocation)
	 */
	public void endVisit(ConstructorInvocation node) {
		super.endVisit(node);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.ArrayInitializer)
	 */
	public final void endVisit(final ArrayInitializer node) {
		final ITypeBinding binding= node.resolveTypeBinding();
		if (binding != null) {
			final ConstraintVariable2 constraint= (ConstraintVariable2) node.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
			if (constraint != null) {
				Expression expression= null;
				ConstraintVariable2 variable= null;
				for (final Iterator iterator= node.expressions().iterator(); iterator.hasNext();) {
					expression= (Expression) iterator.next();
					variable= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
					if (variable != null)
						fModel.createSubtypeConstraint(variable, constraint);
				}
			}
		}
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
			final ConstraintVariable2 variable= (ConstraintVariable2) expression.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
			if (variable != null) {
				final MethodDeclaration declaration= (MethodDeclaration) fCurrentMethods.peek();
				if (declaration != null) {
					final IMethodBinding binding= declaration.resolveBinding();
					if (binding != null) {
						final ConstraintVariable2 result= fModel.createReturnTypeVariable(binding);
						if (result != null)
							fModel.createSubtypeConstraint(variable, result);
					}
				}
			}
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
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#visit(org.eclipse.jdt.core.dom.ThisExpression)
	 */
	public final boolean visit(final ThisExpression node) {
		return false;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.Assignment)
	 */
	public final void endVisit(final Assignment node) {
		final ConstraintVariable2 left= (ConstraintVariable2) node.getLeftHandSide().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ConstraintVariable2 right= (ConstraintVariable2) node.getRightHandSide().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (left != null && right != null) {
			fModel.createSubtypeConstraint(left, right);
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, left);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.CastExpression)
	 */
	public final void endVisit(final CastExpression node) {
		final ConstraintVariable2 left= (ConstraintVariable2) node.getType().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		final ConstraintVariable2 right= (ConstraintVariable2) node.getExpression().getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, left);
		if (right instanceof TypeConstraintVariable2) {
			if (left != null)
				fModel.createSubtypeConstraint(left, right);
			fModel.createCastVariable(node, (TypeConstraintVariable2) right);
		}
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
	 * @param fragments the fragments (element type: <code>VariableDeclarationFragment</code>)
	 * @param type the type of the fragments
	 */
	protected final void endVisit(final List fragments, final Type type) {
		final ConstraintVariable2 variable= (ConstraintVariable2) type.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
		if (variable != null) {
			ConstraintVariable2 constraint= null;
			VariableDeclarationFragment fragment= null;
			for (int index= 0; index < fragments.size(); index++) {
				fragment= (VariableDeclarationFragment) fragments.get(index);
				constraint= (ConstraintVariable2) fragment.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
				if (constraint != null)
					fModel.createSubtypeConstraint(constraint, variable);
			}
			type.getParent().setProperty(PROPERTY_CONSTRAINT_VARIABLE, variable);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.VariableDeclarationFragment)
	 */
	public final void endVisit(final VariableDeclarationFragment node) {
		final Expression initializer= node.getInitializer();
		if (initializer != null)
			node.setProperty(PROPERTY_CONSTRAINT_VARIABLE,  initializer.getProperty(PROPERTY_CONSTRAINT_VARIABLE));
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