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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

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

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.MethodDeclaration)
	 */
	public final void endVisit(final MethodDeclaration node) {
		Assert.isNotNull(node);
		final IMethodBinding binding= node.resolveBinding();
		if (binding != null) {
			final int size= node.parameters().size();
			final ConstraintVariable2[] variables= new ConstraintVariable2[size];
			ConstraintVariable2 variable= null;
			SingleVariableDeclaration declaration= null;
			for (int index= 0; index < size; index++) {
				declaration= (SingleVariableDeclaration) node.parameters().get(index);
				variable= fModel.createMethodParameterVariable(binding, index);
			}
		}
//		
//		
//		IMethodBinding methodBinding= node.resolveBinding();
//		
//			if (methodBinding == null)
//				return; //TODO: emit error?
//			
//			int parameterCount= node.parameters().size();
//			TypeConstraintVariable2[] parameterTypeCvs= new TypeConstraintVariable2[parameterCount];
//			for (int i= 0; i < parameterCount; i++) {
//				SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) node.parameters().get(i);
//				//parameterTypeVariable currently not used, but need to register in order to store source range
//				TypeConstraintVariable2 parameterTypeCv= fModel.makeDeclaredParameterTypeVariable(methodBinding, i, fCU);
//				parameterTypeCvs[i]= parameterTypeCv;
//				if (parameterTypeCv == null)
//					continue;
//				
//				//creating equals constraint between parameterTypeVariable's elements and the Type's elements
//				ConstraintVariable2 typeCv= getConstraintVariable(paramDecl.getType());
//				createElementEqualsConstraints(parameterTypeCv, typeCv);
//				
//				//TODO: should avoid having a VariableVariable as well as a ParameterVariable for a parameter
//				ConstraintVariable2 nameCv= getConstraintVariable(paramDecl.getName());
//				createElementEqualsConstraints(parameterTypeCv, nameCv);
//			}
//			
//			TypeConstraintVariable2 returnTypeCv= null;
//			if (! methodBinding.isConstructor()) {
//				//TODO: should only create return type variable if type is generic?
//				TypeConstraintVariable2 returnTypeBindingCv= fModel.makeDeclaredReturnTypeVariable(methodBinding, fCU);
//				if (returnTypeBindingCv != null) {
//					returnTypeCv= (TypeConstraintVariable2) getConstraintVariable(node.getReturnType2());
//					createElementEqualsConstraints(returnTypeBindingCv, returnTypeCv);
//				}
//			}
//			if (MethodChecks.isVirtual(methodBinding)) {
//				//TODO: RippleMethod constraints for corner cases: see testCuRippleMethods3, bug 41989
//				addConstraintsForOverriding(methodBinding, returnTypeCv, parameterTypeCvs);
//			}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.MethodInvocation)
	 */
	public final void endVisit(final MethodInvocation node) {
		IMethodBinding methodBinding= node.resolveMethodBinding();
		if (methodBinding == null)
			return;
		
		Expression receiver= node.getExpression();
		//TODO: Expression can be null when visiting a non-special method in a subclass of a container type.
		
//		Map methodTypeVariables= createMethodTypeParameters(methodBinding);
//		doVisitMethodInvocationReturnType(node, methodBinding, receiver, methodTypeVariables);
//		List arguments= node.arguments();
//		doVisitMethodInvocationArguments(methodBinding, arguments, receiver, methodTypeVariables);
	}

	private static ConstraintVariable2 getConstraintVariable(ASTNode node) {
		return (ConstraintVariable2) node.getProperty(PROPERTY_CONSTRAINT_VARIABLE);
	}

	private static void setConstraintVariable(ASTNode node, ConstraintVariable2 constraintVariable) {
		node.setProperty(PROPERTY_CONSTRAINT_VARIABLE, constraintVariable);
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