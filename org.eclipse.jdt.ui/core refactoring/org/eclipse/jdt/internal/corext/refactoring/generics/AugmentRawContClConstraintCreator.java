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

package org.eclipse.jdt.internal.corext.refactoring.generics;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.AugmentRawContainerClientsTCModel;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.PlainTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.VariableVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class AugmentRawContClConstraintCreator extends HierarchicalASTVisitor {

	/**
	 * Property in <code>ASTNode</code>s that holds the node's <code>ConstraintVariable</code>.
	 */
	private static final String CV_PROP= "org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CONSTRAINT_VARIABLE"; //$NON-NLS-1$
	
	private AugmentRawContainerClientsTCModel fTCFactory;
	private ContainerMethods fContainerMethods;
	private ICompilationUnit fCU;
	

	public AugmentRawContClConstraintCreator(AugmentRawContainerClientsTCModel factory) {
		fTCFactory= factory;
		fContainerMethods= new ContainerMethods(fTCFactory);
	}
	
	public boolean visit(CompilationUnit node) {
		fTCFactory.newCu(); //TODO: make sure that accumulators are reset after last CU!
		fCU= RefactoringASTParser.getCompilationUnit(node);
		return super.visit(node);
	}
	
	public boolean visit(Javadoc node) {
		return false;
	}
	
	public boolean visit(Type node) {
		return false; //TODO
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.Type)
	 */
	public void endVisit(Type node) {
		//TODO: who needs this?
		TypeVariable2 typeVariable= fTCFactory.makeTypeVariable(node);
		if (typeVariable == null)
			return;
		
		setConstraintVariable(node, typeVariable);
		if (fTCFactory.isACollectionType(typeVariable.getTypeBinding()))
			fTCFactory.makeElementVariable(typeVariable);
	}
	
	public boolean visit(SimpleName node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IVariableBinding) {
			VariableVariable2 cv= fTCFactory.makeDeclaredVariableVariable((IVariableBinding) binding, fCU);
			setConstraintVariable(node, cv);
		}
		// TODO else?
		return super.visit(node);
	}
	
	public void endVisit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		Expression rhs= node.getRightHandSide();
		
		ConstraintVariable2 left= getConstraintVariable(lhs);
		ConstraintVariable2 right= getConstraintVariable(rhs);
		
		Assignment.Operator op= node.getOperator();
		if (op == Assignment.Operator.PLUS_ASSIGN &&
				lhs.resolveTypeBinding().getQualifiedName().equals("java.lang.String")) { //TODO: use util
			//Special handling for automatic String conversion: do nothing; the RHS can be anything.
		} else {
			CollectionElementVariable2 leftElement= fTCFactory.getElementVariable(left);
			CollectionElementVariable2 rightElement= fTCFactory.getElementVariable(right);
			
			fTCFactory.createEqualsConstraint(leftElement, rightElement);
			
			//TODO: filter?
//			fTCFactory.createSubtypeConstraint(right, left); // left= right;  -->  [right] <= [left]
		}
		//TODO: other implicit conversions: numeric promotion, autoboxing?
		
		setConstraintVariable(node, left); // type of assignement is type of 'left'
	}
	
	public void endVisit(CastExpression node) {
		Expression expression= node.getExpression();
		ConstraintVariable2 expressionCv= getConstraintVariable(expression);
		if (! (expressionCv instanceof CollectionElementVariable2))
			return;
		
		fTCFactory.makeCastVariable(node, (CollectionElementVariable2) expressionCv);
		
		Type type= node.getType();
		ConstraintVariable2 typeCv= getConstraintVariable(type);
		
		//TODO: can this be loosened when we remove casts?
		setConstraintVariable(node, typeCv);
		
		boolean eitherIsIntf= type.resolveBinding().isInterface() || expression.resolveTypeBinding().isInterface();
		if (eitherIsIntf)
			return;
		
		//TODO: preserve up- and down-castedness!
		
	}
	
	public boolean visit(CatchClause node) {
		SingleVariableDeclaration exception= node.getException();
		IVariableBinding variableBinding= exception.resolveBinding();
		VariableVariable2 cv= fTCFactory.makeDeclaredVariableVariable(variableBinding, fCU);
		setConstraintVariable(exception, cv);
		return true;
	}
	
	public void endVisit(ClassInstanceCreation node) {
		//TODO: arguments, class body
		TypeVariable2 typeCv= (TypeVariable2) getConstraintVariable(node.getType());
		setConstraintVariable(node, typeCv);
		
//		PlainTypeVariable2 cv= fTCFactory.makePlainTypeVariable(node.resolveTypeBinding());
//		setConstraintVariable(node, cv);
		
		
//		TypeHandle cicTypeHandle= fTCFactory.getTypeHandleFactory().getTypeHandle(node.resolveTypeBinding());
//		TypeHandle collectionTypeHandle= fTCFactory.getCollectionTypeHandle();
//		if (cicTypeHandle.canAssignTo(collectionTypeHandle)) {
//			fTCFactory.makeElementVariable();
//		}
//		
//		return true;
	}
	
	public void endVisit(StringLiteral node) {
		ITypeBinding typeBinding= node.resolveTypeBinding();
		PlainTypeVariable2 cv= fTCFactory.makePlainTypeVariable(typeBinding);
		setConstraintVariable(node, cv);
	}
	
	public boolean visit(MethodDeclaration node) {
		IMethodBinding methodBinding= node.resolveBinding();

		if (methodBinding == null)
			return true; //TODO: emit error?

		for (int i= 0, n= node.parameters().size(); i < n; i++) {
			SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) node.parameters().get(i);
			//parameterTypeVariable currently not used, but need to register in order to store source range
			ConstraintVariable2 parameterTypeVariable= fTCFactory.makeDeclaredParameterTypeVariable(methodBinding, i, fCU);
			setConstraintVariable(paramDecl, parameterTypeVariable);
		}
		
		//TODO: Who needs declaring type constraints?
//		ITypeConstraint2[] declaring=
//			fTCFactory.createEqualsConstraint(
//				fTCFactory.makeDeclaringTypeVariable(methodBinding),
//				fTCFactory.makePlainTypeVariable(methodBinding.getDeclaringClass()));
//
//		addConstraints(declaring);

		if (! methodBinding.isConstructor()){
			ConstraintVariable2 returnTypeBindingVariable= fTCFactory.makeDeclaredReturnTypeVariable(methodBinding, fCU);
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
	
	public void endVisit(MethodInvocation node) {
		IMethodBinding methodBinding= node.resolveMethodBinding();
		
		SpecialMethod specialMethod= fContainerMethods.getSpecialMethodFor(methodBinding);
		if (specialMethod != null) {
			visitContainerMethodInvocation(node, specialMethod);
			return;
		}
		//TODO: normal method invocation
		
		return;
	}
	
	private void visitContainerMethodInvocation(MethodInvocation node, SpecialMethod specialMethod) {
		Expression expression= node.getExpression();
		specialMethod.generateConstraintsFor(node, this);
		// TODO Auto-generated method stub
		
	}

	public void endVisit(FieldDeclaration node) {
		// No need to tie the VariableDeclarationFragments together.
		// The FieldDeclaration can be split up when fragments get different types.
	}
	
	
	public void endVisit(VariableDeclarationExpression node) {
		// Constrain the types of the child VariableDeclarationFragments to be equal to one
		// another, since the initializers in a 'for' statement can only have one type.
		// Pairwise constraints between adjacent variables is enough.
		Type type= node.getType();
		ConstraintVariable2 typeCv= getConstraintVariable(type);
		CollectionElementVariable2 typeElement= fTCFactory.getElementVariable(typeCv);
		if (typeElement == null)
			return;
		
		setConstraintVariable(node, typeCv);
		
		List fragments= node.fragments();
		for (Iterator iter= fragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			ConstraintVariable2 fragmentCv= getConstraintVariable(fragment);
			CollectionElementVariable2 fragmentElement= fTCFactory.getElementVariable(fragmentCv);
			fTCFactory.createEqualsConstraint(typeElement, fragmentElement); //TODO: batch
		}
		
	}
	
	public void endVisit(VariableDeclarationStatement node) {
		// TODO: in principle, no need to tie the VariableDeclarationFragments together.
		// The VariableDeclarationExpression can be split up when fragments get different types.
		// Warning: still need to connect fragments with type!
		ConstraintVariable2 typeCv= getConstraintVariable(node.getType());
		if (typeCv == null)
			return;
		CollectionElementVariable2 typeElement= fTCFactory.getElementVariable(typeCv);
		if (typeElement == null)
			return;
		
		List fragments= node.fragments();
		for (Iterator iter= fragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			ConstraintVariable2 fragmentCv= getConstraintVariable(fragment);
			CollectionElementVariable2 fragmentElement= fTCFactory.getElementVariable(fragmentCv);
			fTCFactory.createEqualsConstraint(typeElement, fragmentElement); //TODO: batch
		}
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
		//fTCFactory.createSubtypeConstraint(initializerCv, nameCv); //TODO: not for augment raw container clients
	}
	
	public void endVisit(VariableDeclarationFragment node) {
		VariableVariable2 cv= fTCFactory.makeDeclaredVariableVariable(node.resolveBinding(), fCU);
		setConstraintVariable(node, cv);
		
		//TODO: prune unused CV for local variables (but not fields)
		
		Expression initializer= node.getInitializer();
		if (initializer == null)
			return;
		
		ConstraintVariable2 initializerCv= getConstraintVariable(initializer);
		if (initializerCv == null)
			return;
		
		CollectionElementVariable2 leftElement= fTCFactory.getElementVariable(cv);
		CollectionElementVariable2 rightElement= fTCFactory.getElementVariable(initializerCv);
		fTCFactory.createEqualsConstraint(leftElement, rightElement);
		
		// name= initializer  -->  [initializer] <= [name]
		//fTCFactory.createSubtypeConstraint(initializerCv, cv); //TODO: not for augment raw container clients
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
	
	
	public AugmentRawContainerClientsTCModel getTCFactory() {
		return fTCFactory;
	}
	
	/**
	 * @param node the ASTNode
	 * @return the {@link ConstraintVariable2} associated with the node, or <code>null</code>
	 */
	protected ConstraintVariable2 getConstraintVariable(ASTNode node) {
		//TODO: make static?
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
