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
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.*;

/**
 * This class provides a convenient behaviour-only 
 * extension mechanism for the ASTNode hierarchy.
 * If you feel like you would like to add a method to
 * the ASTNode heirarchy (or a subtree of the heirarchy),
 * and you want to have different implementations
 * of it at different points in the heirarchy,
 * simply create a HierarchicalASTVisitor representing
 * the new method and all its implementations,
 * locating each implementation within the right
 * visit(XX) method.  If you wanted to add a method implementation to abstract
 * class Foo, an ASTNode descendant, put your implementation in visit(Foo). 
 * This class will provide appropriate dispatch, just as if the method
 * implementations had been added to the ASTNode heirarchy.
 * 
 * Details:
 * 
 * This class has a visit(XX node) method for every for every 
 * class (concrete or abstract) XX in the ASTNode heirarchy. In this class'
 * default implementations of these methods, the method corresponding to a given
 * ASTNode descendant class will call (and return the return value of) the
 * visit(YY) method for it's superclass YY, with the exception of the
 * visit(ASTNode) method which simply returns true, since ASTNode doesn't have a
 * superclass that is within the ASTNode heirarchy.
 * 
 * Because of this organization, when visit(XX) methods  are overridden in a
 * subclass, and the visitor is applied to a node, only the most specialized
 * overridden method implementation for the node's type will be called, unless
 * this most specialized method calls other visit methods (this is discouraged)
 * or, (preferably) calls super.visit(XX node), (the reference type of the
 * parameter must be XX) which will invoke this class' implementation of the
 * method, which will, in turn, invoke the visit(YY) method corresponding to the
 * superclass, YY.
 * 
 * Thus, the dispatching behaviour acheived when 
 * HierarchicalASTVisitors' visit(XX) methods, corresponding to a particular
 * concrete or abstract ASTNode descendant class, are overridden is exactly
 * analogous to the dispatching behaviour obtained when method implementations
 * are added to the same ASTNode descendant classes.
 * 
 * 
 * 
 *   
 */
/*
 * IMPORTANT NOTE:
 * 
 * The structure and behaviour of this class is
 * verified reflectively by 
 * org.eclipse.jdt.ui.tests.core.HeirarchicalASTVisitorTest
 * 
 */
public abstract class HierarchicalASTVisitor extends ASTVisitor {

	//---- Begin ASTNode Heirarchy -------------------------------------
	public boolean visit(ASTNode node) {
		return true;
	}

	//---- Begin Expression Heirarchy ----------------------------------
	public boolean visit(Expression node) {
		return visit((ASTNode) node);
	}

	public boolean visit(ArrayAccess node) {
		return visit((Expression) node);
	}
	public boolean visit(ArrayCreation node) {
		return visit((Expression) node);
	}
	public boolean visit(ArrayInitializer node) {
		return visit((Expression) node);
	}
	public boolean visit(Assignment node) {
		return visit((Expression) node);
	}
	public boolean visit(BooleanLiteral node) {
		return visit((Expression) node);
	}
	public boolean visit(CastExpression node) {
		return visit((Expression) node);
	}
	public boolean visit(CharacterLiteral node) {
		return visit((Expression) node);
	}
	public boolean visit(ClassInstanceCreation node) {
		return visit((Expression) node);
	}
	public boolean visit(ConditionalExpression node) {
		return visit((Expression) node);
	}
	public boolean visit(FieldAccess node) {
		return visit((Expression) node);
	}
	public boolean visit(InfixExpression node) {
		return visit((Expression) node);
	}
	public boolean visit(InstanceofExpression node) {
		return visit((Expression) node);
	}
	public boolean visit(MethodInvocation node) {
		return visit((Expression) node);
	}
	public boolean visit(NullLiteral node) {
		return visit((Expression) node);
	}
	public boolean visit(NumberLiteral node) {
		return visit((Expression) node);
	}
	public boolean visit(ParenthesizedExpression node) {
		return visit((Expression) node);
	}
	public boolean visit(PostfixExpression node) {
		return visit((Expression) node);
	}
	public boolean visit(PrefixExpression node) {
		return visit((Expression) node);
	}
	//---- Begin Name Heirarchy ----------------------------------
	public boolean visit(Name node) {
		return visit((Expression) node);
	}

	public boolean visit(QualifiedName node) {
		return visit((Name) node);
	}
	public boolean visit(SimpleName node) {
		return visit((Name) node);
	}
	//---- End Name Heirarchy ------------------------------------
	public boolean visit(StringLiteral node) {
		return visit((Expression) node);
	}
	public boolean visit(SuperFieldAccess node) {
		return visit((Expression) node);
	}
	public boolean visit(SuperMethodInvocation node) {
		return visit((Expression) node);
	}
	public boolean visit(ThisExpression node) {
		return visit((Expression) node);
	}
	public boolean visit(TypeLiteral node) {
		return visit((Expression) node);
	}
	public boolean visit(VariableDeclarationExpression node) {
		return visit((Expression) node);
	}
	//---- End Expression Heirarchy ----------------------------------

	//---- Begin BodyDeclaration Heirarchy ---------------------------
	public boolean visit(BodyDeclaration node) {
		return visit((ASTNode) node);
	}

	public boolean visit(FieldDeclaration node) {
		return visit((BodyDeclaration) node);
	}
	public boolean visit(Initializer node) {
		return visit((BodyDeclaration) node);
	}
	public boolean visit(MethodDeclaration node) {
		return visit((BodyDeclaration) node);
	}
	public boolean visit(TypeDeclaration node) {
		return visit((BodyDeclaration) node);
	}
	//---- End BodyDeclaration Heirarchy -----------------------------

	public boolean visit(AnonymousClassDeclaration node) {
		return visit((ASTNode) node);
	}

	//---- Begin Type Heirarchy -------------------------------------- 
	public boolean visit(Type node) {
		return visit((ASTNode) node);
	}

	public boolean visit(ArrayType node) {
		return visit((Type) node);
	}
	public boolean visit(PrimitiveType node) {
		return visit((Type) node);
	}
	public boolean visit(SimpleType node) {
		return visit((Type) node);
	}
	//---- End Type Heirarchy ----------------------------------------

	//---- Begin Statement Heirarchy --------------------------------- 
	public boolean visit(Statement node) {
		return visit((ASTNode) node);
	}

	public boolean visit(AssertStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(Block node) {
		return visit((Statement) node);
	}
	public boolean visit(BreakStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(ConstructorInvocation node) {
		return visit((Statement) node);
	}
	public boolean visit(ContinueStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(DoStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(EmptyStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(ExpressionStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(ForStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(IfStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(LabeledStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(ReturnStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(SuperConstructorInvocation node) {
		return visit((Statement) node);
	}
	public boolean visit(SwitchCase node) {
		return visit((Statement) node);
	}
	public boolean visit(SwitchStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(SynchronizedStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(ThrowStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(TryStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(TypeDeclarationStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(VariableDeclarationStatement node) {
		return visit((Statement) node);
	}
	public boolean visit(WhileStatement node) {
		return visit((Statement) node);
	}
	//---- End Statement Heirarchy ----------------------------------  

	public boolean visit(CatchClause node) {
		return visit((ASTNode) node);
	}

	public boolean visit(CompilationUnit node) {
		return visit((ASTNode) node);
	}

	public boolean visit(ImportDeclaration node) {
		return visit((ASTNode) node);
	}

	public boolean visit(Javadoc node) {
		return visit((ASTNode) node);
	}

	public boolean visit(PackageDeclaration node) {
		return visit((ASTNode) node);
	}

	//---- Begin VariableDeclaration Heirarchy ---------------------------  
	public boolean visit(VariableDeclaration node) {
		return visit((ASTNode) node);
	}

	public boolean visit(SingleVariableDeclaration node) {
		return visit((VariableDeclaration) node);
	}
	public boolean visit(VariableDeclarationFragment node) {
		return visit((VariableDeclaration) node);
	}
	//---- End VariableDeclaration Heirarchy ----------------------------- 
	//---- End ASTNode Heirarchy -----------------------------------------
}
