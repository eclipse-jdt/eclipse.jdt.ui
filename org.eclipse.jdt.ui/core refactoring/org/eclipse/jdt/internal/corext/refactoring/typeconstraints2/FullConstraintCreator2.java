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

package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;

public class FullConstraintCreator2 extends ConstraintCreator2 {

	private TypeConstraintFactory2 fTCFactory;

	public FullConstraintCreator2(TypeConstraintFactory2 factory) {
		fTCFactory= factory;
	}
	
	public boolean visit(Javadoc node) {
		return false;
	}
	
	public boolean visit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		ExpressionVariable2 left= fTCFactory.makeExpressionVariable(lhs);
		ExpressionVariable2 right= fTCFactory.makeExpressionVariable(node.getRightHandSide());
		ExpressionVariable2 whole= fTCFactory.makeExpressionVariable(node);
		
		Assignment.Operator op= node.getOperator();
		if ((op == Assignment.Operator.ASSIGN || op == Assignment.Operator.PLUS_ASSIGN) &&
				lhs.resolveTypeBinding().getQualifiedName().equals("java.lang.String")) { //$NON-NLS-1$
			//Special handling for automatic String conversion: do nothing; the RHS can be anything.
		} else {
			addConstraints(fTCFactory.createSubtypeConstraint(right, left)); // left= right;  -->  [right] <= [left]
		}
		//TODO: other implicit conversions: numeric promotion, autoboxing?
		
		addConstraints(fTCFactory.createEqualsConstraint(whole, left)); // type of 'whole' is type of 'left'
		return true;
	}
	
	public boolean visit(MethodDeclaration node) {
		IMethodBinding methodBinding= node.resolveBinding();

		if (methodBinding == null)
			return true; //TODO: emit error?

		ITypeConstraint2[] declaring=
			fTCFactory.createEqualsConstraint(
				fTCFactory.makeDeclaringTypeVariable(methodBinding),
				fTCFactory.makePlainTypeVariable(methodBinding.getDeclaringClass()));

		addConstraints(declaring);

		if (! methodBinding.isConstructor() && ! methodBinding.getReturnType().isPrimitive()){
			ConstraintVariable2 returnTypeBindingVariable= fTCFactory.makeReturnTypeVariable(methodBinding);
			ConstraintVariable2 returnTypeVariable= fTCFactory.makeTypeVariable(node.getReturnType2());
			ITypeConstraint2[] defines= fTCFactory.createEqualsConstraint(returnTypeBindingVariable, returnTypeVariable);
			addConstraints(defines);
		}
		for (int i= 0, n= node.parameters().size(); i < n; i++) {
			SingleVariableDeclaration paramDecl= (SingleVariableDeclaration)node.parameters().get(i);
			ConstraintVariable2 parameterTypeVariable= fTCFactory.makeParameterTypeVariable(methodBinding, i);
			ConstraintVariable2 parameterNameVariable= fTCFactory.makeExpressionVariable(paramDecl.getName());
			ITypeConstraint2[] constraint= fTCFactory.createEqualsConstraint(parameterTypeVariable, parameterNameVariable);
			addConstraints(constraint);
		}
		if (MethodChecks.isVirtual(methodBinding)){
			//TODO
//			Collection constraintsForOverriding = getConstraintsForOverriding(methodBinding);
//			result.addAll(constraintsForOverriding);
		}
		return true;
	}
	
	
	public boolean visit(FieldDeclaration node) {
		for(Iterator iter= node.fragments().iterator(); iter.hasNext(); ) {
			VariableDeclarationFragment	f= (VariableDeclarationFragment) iter.next();

			if (f.getInitializer() != null) {
//				addConstraint(getConstraintFactory().createSubtypeConstraint(
//					getConstraintVariableFactory().makeExpressionOrTypeVariable(f.getInitializer(), getContext()),
//					getConstraintVariableFactory().makeExpressionOrTypeVariable(f.getName(), getContext())),
//					result);
			}
		}
		return true;
	}
	
	//TODO: extra dimensions?
	public boolean visit(SingleVariableDeclaration node) {
		ExpressionVariable2 name= fTCFactory.makeExpressionVariable(node.getName());
		TypeVariable2 type= fTCFactory.makeTypeVariable(node.getType());
		ITypeConstraint2[] nameEqualsType= fTCFactory.createEqualsConstraint(name, type);
		addConstraints(nameEqualsType);
		
		if (node.getInitializer() == null)
			return true;
		
		ITypeConstraint2[] initializer= fTCFactory.createSubtypeConstraint(
				fTCFactory.makeExpressionVariable(node.getInitializer()),
				name);
		addConstraints(initializer);
		
		return true;
	}
	
	
	
	public boolean visit(VariableDeclarationFragment node) {
		if (node.getInitializer() == null)
			return true;
		
		ITypeConstraint2[] initializer= fTCFactory.createSubtypeConstraint(
				fTCFactory.makeExpressionVariable(node.getInitializer()),
				fTCFactory.makeExpressionVariable(node.getName()));
		addConstraints(initializer);
		return true;
	}

	public ITypeConstraint2[] create(VariableDeclarationExpression vde){
		return getConstraintsFromFragmentList(vde.fragments(), vde.getType());
	}
	
	public ITypeConstraint2[] create(VariableDeclarationStatement node) {
		return getConstraintsFromFragmentList(node.fragments(), node.getType());
	}
	
	//--------- private helpers ----------------//
	
	private ITypeConstraint2[] getConstraintsFromFragmentList(List/*<VariableDeclarationFragment>*/ fragments, Type type) {
		// Constrain the types of the declared variables to be equal to one
		// another. Pairwise constraints between adjacent variables is enough.
		int size= fragments.size();
		ConstraintVariable2 typeVariable= fTCFactory.makeTypeVariable(type);
		List result= new ArrayList((size * (size - 1))/2);
		for (int i= 0; i < size; i++) {
			VariableDeclarationFragment fragment1= (VariableDeclarationFragment) fragments.get(i);
			SimpleName fragment1Name= fragment1.getName();
			ITypeConstraint2[] fragment1DefinesConstraints= fTCFactory.createEqualsConstraint(
					fTCFactory.makeExpressionVariable(fragment1Name),
					typeVariable);
			result.addAll(Arrays.asList(fragment1DefinesConstraints));
			for (int j= i + 1; j < size; j++) {
				VariableDeclarationFragment fragment2= (VariableDeclarationFragment) fragments.get(j);
				ITypeConstraint2[] fragment12equalsConstraints= fTCFactory.createEqualsConstraint(
						fTCFactory.makeExpressionVariable(fragment1Name),
						fTCFactory.makeExpressionVariable(fragment2.getName()));
				result.addAll(Arrays.asList(fragment12equalsConstraints));
			}
		}
		return (ITypeConstraint2[]) result.toArray(new ITypeConstraint2[result.size()]);
	}
	
}
