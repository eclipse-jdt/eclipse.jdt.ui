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
 * visit(..) or visitXX(..) method.  If you wanted
 * to add a method implementation to abstract class Foo,
 * an ASTNode descendant, put your implementation
 * in visitFoo(..).  This class will provide appropriate
 * dispatch, just as if the method implementations
 * had been added to the ASTNode heirarchy.
 * 
 * Details:
 * 
 * This class has a visit(XX node) or 
 * visitXX(X node) method for every for every class
 * (concrete or abstract) XX in the ASTNode heirarchy. 
 * In this class' default implementations of these methods,
 * the method corresponding to a given ASTNode descendant
 * class will call (and return the return value of) the
 * visitXX(..) method for it's superclass, with the exception
 * of the visitASTNode(..) method which simply returns true,
 * since ASTNode doesn't have a superclass that is within the
 * ASTNode heirarchy.
 * 
 * Because of this organization, when visit(..) or 
 * visitXX(..) methods are overridden in a subclass,
 * and the visitor is applied to a node, only the
 * most specialized overridden method implementation for
 * the node's type will be called, unless this most specialized
 * method calls other visit methods (this is discouraged)
 * or, (preferably) calls super.visitXX(node), which will invoke
 * this class' implementation of the method, which will,
 * in turn, invoke the visitXX(..) method corresponding
 * to the superclass.
 * 
 * Thus, the dispatching behaviour acheived when 
 * HeirarchicalASTVisitors' visit(..) or visitXX(..) methods,
 * corresponding to a particular concrete or abstract
 * ASTNode descendant class, are overridden is exactly
 * analogous to the dispatching behaviour obtained when
 * method implementations are added to the same ASTNode
 * descendant classes.
 * 
 * 
 * 
 *    */
/*
 * IMPORTANT NOTE:
 * 
 * The structure and behaviour of this class is
 * verified reflectively by 
 * org.eclipse.jdt.ui.tests.core.HeirarchicalASTVisitorTest
 *  */
public abstract class HierarchicalASTVisitor extends ASTVisitor {

/***Begin ASTNode Heirarchy*****************************************/
	public boolean visitASTNode(ASTNode node) {
		return true;
	}
	

/***Begin Expression Heirarchy**************************************/
	public boolean visitExpression(Expression node) {
		return visitASTNode(node);	
	}
	
	public boolean visit(ArrayAccess node) {
		return visitExpression(node);
	}
	public boolean visit(ArrayCreation node) {
		return visitExpression(node);
	}
	public boolean visit(ArrayInitializer node) {
		return visitExpression(node);
	}
	public boolean visit(Assignment node) {
		return visitExpression(node);
	}	
	public boolean visit(BooleanLiteral node) {
		return visitExpression(node);
	}
	public boolean visit(CastExpression node) {
		return visitExpression(node);
	}
	public boolean visit(CharacterLiteral node) {
		return visitExpression(node);
	}
	public boolean visit(ClassInstanceCreation node) {
		return visitExpression(node);
	}
	public boolean visit(ConditionalExpression node) {
		return visitExpression(node);
	}
	public boolean visit(FieldAccess node) {
		return visitExpression(node);
	}
	public boolean visit(InfixExpression node) {
		return visitExpression(node);
	}
	public boolean visit(InstanceofExpression node) {
		return visitExpression(node);
	}
	public boolean visit(MethodInvocation node) {
		return visitExpression(node);
	}
	public boolean visit(NullLiteral node) {
		return visitExpression(node);
	}
	public boolean visit(NumberLiteral node) {
		return visitExpression(node);
	}
	public boolean visit(ParenthesizedExpression node) {
		return visitExpression(node);
	}
	public boolean visit(PostfixExpression node) {
		return visitExpression(node);
	}
	public boolean visit(PrefixExpression node) {
		return visitExpression(node);
	}
/***Begin Name Heirarchy********************************************/
	public boolean visitName(Name node) {
		return visitExpression(node);	
	}
	
	public boolean visit(QualifiedName node) {
		return visitName(node);
	}
	public boolean visit(SimpleName node) {
		return visitName(node);
	}
/***End Name Heirarchy********************************************/
/***Begin Expression Heirarchy**************************************/
	public boolean visit(StringLiteral node) {
		return visitExpression(node);
	}
	public boolean visit(SuperFieldAccess node) {
		return visitExpression(node);
	}
	public boolean visit(SuperMethodInvocation node) {
		return visitExpression(node);
	}
	public boolean visit(ThisExpression node) {
		return visitExpression(node);
	}
	public boolean visit(TypeLiteral node) {
		return visitExpression(node);
	}
	public boolean visit(VariableDeclarationExpression node) {
		return visitExpression(node);
	}
/***End Expression Heirarchy**************************************/

	
/***Begin BodyDeclaration Heirarchy*******************************/
	public boolean visitBodyDeclaration(BodyDeclaration node) {
		return visitASTNode(node);	
	}

	public boolean visit(FieldDeclaration node) {
		return visitBodyDeclaration(node);
	}
	public boolean visit(Initializer node) {
		return visitBodyDeclaration(node);
	}	
	public boolean visit(MethodDeclaration node) {
		return visitBodyDeclaration(node);
	}
	public boolean visit(TypeDeclaration node) {
		return visitBodyDeclaration(node);
	}
/***End BodyDeclaration Heirarchy*********************************/
	
	
	public boolean visit(AnonymousClassDeclaration node) {
		return visitASTNode(node);
	}
	

/***Begin Type Heirarchy******************************************/	
	public boolean visitType(Type node) {
		return visitASTNode(node);
	}
	
	public boolean visit(ArrayType node) {
		return visitType(node);
	}
	public boolean visit(PrimitiveType node) {
		return visitType(node);
	}
	public boolean visit(SimpleType node) {
		return visitType(node);
	}	
/***End Type Heirarchy********************************************/
	
	
/***Begin Statement Heirarchy*************************************/	
	public boolean visitStatement(Statement node) {
		return visitASTNode(node);
	}
	
	public boolean visit(AssertStatement node) {
		return visitStatement(node);
	}
	public boolean visit(Block node) {
		return visitStatement(node);
	}
	public boolean visit(BreakStatement node) {
		return visitStatement(node);
	}
	public boolean visit(ConstructorInvocation node) {
		return visitStatement(node);
	}
	public boolean visit(ContinueStatement node) {
		return visitStatement(node);
	}
	public boolean visit(DoStatement node) {
		return visitStatement(node);
	}
	public boolean visit(EmptyStatement node) {
		return visitStatement(node);
	}
	public boolean visit(ExpressionStatement node) {
		return visitStatement(node);
	}	
	public boolean visit(ForStatement node) {
		return visitStatement(node);
	}
	public boolean visit(IfStatement node) {
		return visitStatement(node);
	}
	public boolean visit(LabeledStatement node) {
		return visitStatement(node);
	}
	public boolean visit(ReturnStatement node) {
		return visitStatement(node);
	}
	public boolean visit(SuperConstructorInvocation node) {
		return visitStatement(node);
	}
	public boolean visit(SwitchCase node) {
		return visitStatement(node);
	}
	public boolean visit(SwitchStatement node) {
		return visitStatement(node);
	}
	public boolean visit(SynchronizedStatement node) {
		return visitStatement(node);
	}
	public boolean visit(ThrowStatement node) {
		return visitStatement(node);
	}
	public boolean visit(TryStatement node) {
		return visitStatement(node);
	}
	public boolean visit(TypeDeclarationStatement node) {
		return visitStatement(node);
	}
	public boolean visit(VariableDeclarationStatement node) {
		return visitStatement(node);
	}
	public boolean visit(WhileStatement node) {
		return visitStatement(node);
	}
/***End Statement Heirarchy*************************************/		
	
	
	public boolean visit(CatchClause node) {
		return visitASTNode(node);
	}
	
	
	public boolean visit(CompilationUnit node) {
		return visitASTNode(node);
	}


	public boolean visit(ImportDeclaration node) {
		return visitASTNode(node);
	}
	
	
	public boolean visit(Javadoc node) {
		return visitASTNode(node);
	}
	

	public boolean visit(PackageDeclaration node) {
		return visitASTNode(node);
	}
	
	
/***Begin VariableDeclaration Heirarchy***************************/		
	public boolean visitVariableDeclaration(VariableDeclaration node) {
		return visitASTNode(node);	
	}
	
	public boolean visit(SingleVariableDeclaration node) {
		return visitVariableDeclaration(node);
	}
	public boolean visit(VariableDeclarationFragment node) {
		return visitVariableDeclaration(node);
	}
/***End VariableDeclaration Heirarchy*****************************/		
/***End ASTNode Heirarchy*****************************************/
}
