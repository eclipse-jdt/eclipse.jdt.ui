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

package org.eclipse.jdt.internal.corext.refactoring.genericize;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintCreator2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraintFactory2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.VariableVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class AugmentRawTypesConstraintCreator2 extends ConstraintCreator2 {

	/**
	 * Property in <code>ASTNode</code>s that holds the node's <code>ConstraintVariable</code>.
	 */
	private static final String CV_PROP= "org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CONSTRAINT_VARIABLE"; //$NON-NLS-1$
	
	private TypeConstraintFactory2 fTCFactory;

	public AugmentRawTypesConstraintCreator2(TypeConstraintFactory2 factory) {
		fTCFactory= factory;
	}
	
	public boolean visit(Javadoc node) {
		return false;
	}
	
	public boolean visit(Type node) {
		return false;
	}
	
	public void endVisit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		Expression rhs= node.getRightHandSide();
		
		ConstraintVariable2 left= getConstraintVariable(lhs);
		ConstraintVariable2 right= getConstraintVariable(rhs);
		
		Assignment.Operator op= node.getOperator();
		if ((op == Assignment.Operator.ASSIGN || op == Assignment.Operator.PLUS_ASSIGN) &&
				TypeRules.isJavaLangObject(lhs.resolveTypeBinding())) {
			//Special handling for automatic String conversion: do nothing; the RHS can be anything.
		} else {
			addConstraints(fTCFactory.createSubtypeConstraint(right, left)); // left= right;  -->  [right] <= [left]
		}
		//TODO: other implicit conversions: numeric promotion, autoboxing?
		
		setConstraintVariable(node, left); // type of assignement is type of 'left'
	}
	
	public boolean visit(CatchClause node) {
		SingleVariableDeclaration exception= node.getException();
		IVariableBinding variableBinding= exception.resolveBinding();
		Type exceptionType= exception.getType();
		VariableVariable2 cv= fTCFactory.makeVariableVariable(variableBinding, exceptionType);
		setConstraintVariable(exception, cv);
		return true;
	}
	
	public boolean visit(MethodDeclaration node) {
		IMethodBinding methodBinding= node.resolveBinding();

		if (methodBinding == null)
			return true; //TODO: emit error?

		for (int i= 0, n= node.parameters().size(); i < n; i++) {
			SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) node.parameters().get(i);
			//parameterTypeVariable currently not used, but need to register in order to store source range
			ConstraintVariable2 parameterTypeVariable= fTCFactory.makeParameterTypeVariable(methodBinding, i, paramDecl.getType());
			setConstraintVariable(node, parameterTypeVariable);
		}
		
		//TODO: Who needs declaring type constraints?
//		ITypeConstraint2[] declaring=
//			fTCFactory.createEqualsConstraint(
//				fTCFactory.makeDeclaringTypeVariable(methodBinding),
//				fTCFactory.makePlainTypeVariable(methodBinding.getDeclaringClass()));
//
//		addConstraints(declaring);

		if (! methodBinding.isConstructor()){
			ConstraintVariable2 returnTypeBindingVariable= fTCFactory.makeReturnTypeVariable(methodBinding, node.getReturnType());
//			ConstraintVariable2 returnTypeVariable= getConstraintVariable(node.getReturnType2());
//			returnTypeBindingVariable.setRepresentative(returnTypeVariable);
			//TODO: how to ensure that returnTypeBindingVariable is stored when it is not used in a TC?
//			ITypeConstraint2[] defines= fTCFactory.createEqualsConstraint(returnTypeBindingVariable, returnTypeVariable);
//			addConstraints(defines);
		}
		if (MethodChecks.isVirtual(methodBinding)){
			//TODO
//			Collection constraintsForOverriding = getConstraintsForOverriding(methodBinding);
//			result.addAll(constraintsForOverriding);
		}
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		//TODO
		
		return true;
	}
	
	
	public void endVisit(FieldDeclaration node) {
		// No need to tie the VariableDeclarationFragments together.
		// The FieldDeclaration can be split up when fragments get different types.
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.Type)
	 */
	public void endVisit(Type node) {
		//TODO: who needs this?
		TypeVariable2 typeVariable= fTCFactory.makeTypeVariable(node);
		setConstraintVariable(node, typeVariable);
	}
	
	public void endVisit(VariableDeclarationExpression node) {
		// Constrain the types of the child VariableDeclarationFragments to be equal to one
		// another, since the initializers in a 'for' statement can only have one type.
		// Pairwise constraints between adjacent variables is enough.
		Type type= node.getType();
		ConstraintVariable2 typeCv= getConstraintVariable(type);
		setConstraintVariable(node, typeCv);
		
		List fragments= node.fragments();
		int size= fragments.size();
		ConstraintVariable2[] equalVariables= new ConstraintVariable2[size];
		for (int i= 0; i < size; i++) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) fragments.get(i);
			equalVariables[i]= getConstraintVariable(fragment);
		}
//		addConstraints(fTCFactory.createEqualsConstraint(equalVariables)); //TODO
		
//		ConstraintVariable2 typeVariable= fTCFactory.makeTypeVariable(type);
//		List result= new ArrayList((size * (size - 1))/2);
//		for (int i= 0; i < size; i++) {
//			VariableDeclarationFragment fragment1= (VariableDeclarationFragment) fragments.get(i);
//			SimpleName fragment1Name= fragment1.getName();
//			ITypeConstraint2[] fragment1DefinesConstraints= fTCFactory.createEqualsConstraint(
//					fTCFactory.makeExpressionVariable(fragment1Name),
//					typeVariable);
//			result.addAll(Arrays.asList(fragment1DefinesConstraints));
//			for (int j= i + 1; j < size; j++) {
//				VariableDeclarationFragment fragment2= (VariableDeclarationFragment) fragments.get(j);
//				ITypeConstraint2[] fragment12equalsConstraints= fTCFactory.createEqualsConstraint(
//						fTCFactory.makeExpressionVariable(fragment1Name),
//						fTCFactory.makeExpressionVariable(fragment2.getName()));
//				result.addAll(Arrays.asList(fragment12equalsConstraints));
//			}
//		}
//		return (ITypeConstraint2[]) result.toArray(new ITypeConstraint2[result.size()]);
	}
	
	public void endVisit(VariableDeclarationStatement node) {
		// No need to tie the VariableDeclarationFragments together.
		// The VariableDeclarationExpression can be split up when fragments get different types.
	}

	public void endVisit(SingleVariableDeclaration node) {
		// used for formal method parameters and catch clauses
		//TODO: extra dimensions?
		
//		ConstraintVariable2 typeCv= getConstraintVariable(node.getType()); //TODO: who needs this?
		
//		ConstraintVariable2 nameCv;
//		switch (node.getParent().getNodeType()) {
//			case ASTNode.METHOD_DECLARATION :
//				MethodDeclaration parent= (MethodDeclaration) node.getParent();
//				int index= parent.parameters().indexOf(node);
//				nameCv= fTCFactory.makeParameterTypeVariable(parent.resolveBinding(), index, node.getType());
//				//store source range even if variable not used in constraint here. TODO: move to visit(MethodDeclaration)?
//				break;
//			case ASTNode.CATCH_CLAUSE :
//				nameCv= fTCFactory.makeVariableVariable(node.resolveBinding());
//			
//				break;
//			default:
//				unexpectedNode(node.getParent());
//		}
//		setConstraintVariable(node, nameCv);
		
		//TODO: Move this into visit(SimpleName) or leave it here?
//		ExpressionVariable2 name= fTCFactory.makeExpressionVariable(node.getName());
//		TypeVariable2 type= fTCFactory.makeTypeVariable(node.getType());
//		ITypeConstraint2[] nameEqualsType= fTCFactory.createEqualsConstraint(name, type);
//		addConstraints(nameEqualsType);
		
		//TODO: When can a SingleVariableDeclaration have an initializer? Never up to Java 1.5?
		Expression initializer= node.getInitializer();
		if (initializer == null)
			return;
		
		ConstraintVariable2 initializerCv= getConstraintVariable(initializer);
		ConstraintVariable2 nameCv= getConstraintVariable(node);
		//TODO: check: property has been set in visit(CatchClause), visit(MethodDeclaration), visit(EnhancedForStatament)
		ITypeConstraint2[] initializerTc= fTCFactory.createSubtypeConstraint(initializerCv, nameCv);
		addConstraints(initializerTc);
	}
	
	public void endVisit(VariableDeclarationFragment node) {
		ConstraintVariable2 nameCv= getConstraintVariable(node.getName());
		setConstraintVariable(node, nameCv);
		
		Expression initializer= node.getInitializer();
		if (initializer == null)
			return;
		
		ConstraintVariable2 initializerCv= getConstraintVariable(initializer);
		// name= initializer  -->  [initializer] <= [name]
		ITypeConstraint2[] tc= fTCFactory.createSubtypeConstraint(initializerCv, nameCv);
		addConstraints(tc);
	}
	
	//--------- private helpers ----------------//
	
//	private ITypeConstraint2[] getConstraintsFromFragmentList(List/*<VariableDeclarationFragment>*/ fragments, Type type) {
//		// Constrain the types of the declared variables to be equal to one
//		// another. Pairwise constraints between adjacent variables is enough.
//		int size= fragments.size();
//		ConstraintVariable2 typeVariable= fTCFactory.makeTypeVariable(type);
//		List result= new ArrayList((size * (size - 1))/2);
//		for (int i= 0; i < size; i++) {
//			VariableDeclarationFragment fragment1= (VariableDeclarationFragment) fragments.get(i);
//			SimpleName fragment1Name= fragment1.getName();
//			ITypeConstraint2[] fragment1DefinesConstraints= fTCFactory.createEqualsConstraint(
//					fTCFactory.makeExpressionVariable(fragment1Name),
//					typeVariable);
//			result.addAll(Arrays.asList(fragment1DefinesConstraints));
//			for (int j= i + 1; j < size; j++) {
//				VariableDeclarationFragment fragment2= (VariableDeclarationFragment) fragments.get(j);
//				ITypeConstraint2[] fragment12equalsConstraints= fTCFactory.createEqualsConstraint(
//						fTCFactory.makeExpressionVariable(fragment1Name),
//						fTCFactory.makeExpressionVariable(fragment2.getName()));
//				result.addAll(Arrays.asList(fragment12equalsConstraints));
//			}
//		}
//		return (ITypeConstraint2[]) result.toArray(new ITypeConstraint2[result.size()]);
//	}
	
	/**
	 * @param node the ASTNode
	 * @return the {@link ConstraintVariable2} associated with the node, or <code>null</code>
	 */
	protected ConstraintVariable2 getConstraintVariable(ASTNode node) {
		return (ConstraintVariable2) node.getProperty(CV_PROP);
	}
	
	/**
	 * @param node the ASTNode
	 * @param constraintVariable the {@link ConstraintVariable2} to be associated with node
	 */
	protected void setConstraintVariable(ASTNode node, ConstraintVariable2 constraintVariable) {
		node.setProperty(CV_PROP, constraintVariable);
	}
	
	private void unexpectedNode(ASTNode node) {
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(node);
		JavaPlugin.logErrorMessage((cu == null ? "" : cu.getElementName() + " - ")  //$NON-NLS-1$//$NON-NLS-2$
				+ node.getNodeType() + ": " + node.toString()); //$NON-NLS-1$
	}

}
