/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.util;

import java.util.List;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;
import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.core.refactoring.Assert;

public class GenericVisitor implements IAbstractSyntaxTreeVisitor, IParentTrackable {

	private IParentTracker fParentTracker;
	private CompilationUnitDeclaration fRoot;
	
	public void setParentTracker(IParentTracker tracker) {
		fParentTracker= tracker;
		Assert.isNotNull(fParentTracker);
	}

	public CompilationUnitDeclaration getRoot() {
		return fRoot;
	}

	protected AstNode internalGetParent() {
		if (fParentTracker == null)
			return null;
		return fParentTracker.getParent();
	}
	
	protected List internalGetParents() {
		if (fParentTracker == null)
			return null;
		return fParentTracker.getParents();
	}

	//---- Hooks for subclasses -------------------------------------------------

	protected boolean visitRange(int start, int end, AstNode node, Scope scope) {
		return true;
	}
	
	protected void endVisitRange(int start, int end, AstNode node, Scope scope) {
	}

	//---- Helpers ----------------------------------------------------------------
	
	private boolean visitNode(AstNode node, Scope scope) {
		return visitRange(node.sourceStart, node.sourceEnd, node, scope);
	}

	private void endVisitNode(AstNode node, Scope scope) {
		endVisitRange(node.sourceStart, node.sourceEnd, node, scope);
	}
	
	//---- Problem reporting ----------------------------------------------------
	
	public void acceptProblem(IProblem problem) {
	}
	
	//---- Base class visit methods ---------------------------------------------
	
	protected boolean visitTypeDeclaration(TypeDeclaration node, Scope scope) {
		return visitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);
	}
	
	protected void endVisitTypeDeclaration(TypeDeclaration node, Scope scope) {
		endVisitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);
	}
	
	protected boolean visitAbstractVariableDeclaration(AbstractVariableDeclaration node, Scope scope) {
		return visitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);
	}
	
	protected void endVisitAbstractVariableDeclaration(AbstractVariableDeclaration node, Scope scope) {
		endVisitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);
	}
	
	protected boolean visitAbstractMethodDeclaration(AbstractMethodDeclaration node, Scope scope) {
		return visitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);
	}
	
	protected void endVisitAbstractMethodDeclaration(AbstractMethodDeclaration node, Scope scope) {
		endVisitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);
	}
	
	//---- Compilation Unit -------------------------------------------------------
	
	public boolean visit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
		fRoot= node;
		return visitNode(node, scope);
	}
	
	public void endVisit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(ImportReference node, CompilationUnitScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(ImportReference node, CompilationUnitScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(TypeDeclaration node, CompilationUnitScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public void endVisit(TypeDeclaration node, CompilationUnitScope scope) {
		endVisitTypeDeclaration(node, scope);
	}
	
	//---- Type -------------------------------------------------------------------
	
	public boolean visit(Clinit node, ClassScope scope) {
		return visitAbstractMethodDeclaration(node, scope);
	}
	
	public void endVisit(Clinit node, ClassScope scope) {
		endVisitAbstractMethodDeclaration(node, scope);
	}
	
	public boolean visit(TypeDeclaration node, ClassScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public void endVisit(TypeDeclaration node, ClassScope scope) {
		endVisitTypeDeclaration(node, scope);
	}
	
	public boolean visit(MemberTypeDeclaration node, ClassScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public void endVisit(MemberTypeDeclaration node, ClassScope scope) {
		endVisitTypeDeclaration(node, scope);
	}
	
	public boolean visit(FieldDeclaration node, MethodScope scope) {
		return visitAbstractVariableDeclaration(node, scope);
	}
	
	public void endVisit(FieldDeclaration node, MethodScope scope) {
		endVisitAbstractVariableDeclaration(node, scope);
	}
	
	public boolean visit(Initializer node, MethodScope scope) {
		return visitAbstractVariableDeclaration(node, scope);
	}
	
	public void endVisit(Initializer node, MethodScope scope) {
		endVisitAbstractVariableDeclaration(node, scope);
	}
	
	public boolean visit(ConstructorDeclaration node, ClassScope scope) {
		return visitAbstractMethodDeclaration(node, scope);
	}
	
	public void endVisit(ConstructorDeclaration node, ClassScope scope) {
		endVisitAbstractMethodDeclaration(node, scope);
	}
	
	public boolean visit(MethodDeclaration node, ClassScope scope) {
		return visitAbstractMethodDeclaration(node, scope);
	}
	
	public void endVisit(MethodDeclaration node, ClassScope scope) {
		endVisitAbstractMethodDeclaration(node, scope);
	}
	
	public boolean visit(SingleTypeReference node, ClassScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(SingleTypeReference node, ClassScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(QualifiedTypeReference node, ClassScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(QualifiedTypeReference node, ClassScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(ArrayTypeReference node, ClassScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(ArrayTypeReference node, ClassScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(ArrayQualifiedTypeReference node, ClassScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(ArrayQualifiedTypeReference node, ClassScope scope) {
		endVisitNode(node, scope);
	}
	
	//---- Methods ----------------------------------------------------------------
	
	public boolean visit(LocalTypeDeclaration node, MethodScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public void endVisit(LocalTypeDeclaration node, MethodScope scope) {
		endVisitTypeDeclaration(node, scope);
	}
	
	public boolean visit(Argument node, BlockScope scope) {
		return visitAbstractVariableDeclaration(node, scope);
	}
	
	public void endVisit(Argument node, BlockScope scope) {
		endVisitAbstractVariableDeclaration(node, scope);
	}
	
	//---- Methods / Block --------------------------------------------------------
	
	public boolean visit(TypeDeclaration node, BlockScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public void endVisit(TypeDeclaration node, BlockScope scope) {
		endVisitTypeDeclaration(node, scope);
	}
	
	public boolean visit(AnonymousLocalTypeDeclaration node, BlockScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public void endVisit(AnonymousLocalTypeDeclaration node, BlockScope scope) {
		endVisitTypeDeclaration(node, scope);
	}
	
	public boolean visit(LocalDeclaration node, BlockScope scope) {
		return visitAbstractVariableDeclaration(node, scope);
	}

	public void endVisit(LocalDeclaration node, BlockScope scope) {
		endVisitAbstractVariableDeclaration(node, scope);
	}

	public boolean visit(FieldReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(FieldReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ArrayReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ArrayReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ArrayTypeReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ArrayTypeReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ArrayQualifiedTypeReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ArrayQualifiedTypeReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(SingleTypeReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(SingleTypeReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(QualifiedTypeReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(QualifiedTypeReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(QualifiedNameReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(QualifiedNameReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(QualifiedSuperReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(QualifiedSuperReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(QualifiedThisReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(QualifiedThisReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(SingleNameReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(SingleNameReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(SuperReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(SuperReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ThisReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ThisReference node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	//---- Statements -------------------------------------------------------------
	
	public boolean visit(AllocationExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(AllocationExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(AND_AND_Expression node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(AND_AND_Expression node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(ArrayAllocationExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(ArrayAllocationExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(ArrayInitializer node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(ArrayInitializer node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(AssertStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(AssertStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(Assignment node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(Assignment node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(BinaryExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(BinaryExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(Block node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(Block node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(Break node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(Break node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(Case node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(Case node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(CastExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(CastExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(CharLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(CharLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ClassLiteralAccess node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ClassLiteralAccess node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(CompoundAssignment node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(CompoundAssignment node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(ConditionalExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ConditionalExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(Continue node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(Continue node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(DefaultCase node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(DefaultCase node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(DoStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(DoStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(DoubleLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(DoubleLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(EmptyStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(EmptyStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(EqualExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(EqualExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ExplicitConstructorCall node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ExplicitConstructorCall node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ExtendedStringLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ExtendedStringLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(FalseLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(FalseLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(FloatLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(FloatLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ForStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(ForStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(IfStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}
		
	public void endVisit(IfStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}
		
	public boolean visit(InstanceOfExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(InstanceOfExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(IntLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(IntLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(LabeledStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(LabeledStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(LongLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(LongLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(MessageSend node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(MessageSend node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(NullLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(NullLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(OR_OR_Expression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(OR_OR_Expression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(PostfixExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(PostfixExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(PrefixExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(PrefixExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(QualifiedAllocationExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(QualifiedAllocationExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ReturnStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ReturnStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(StringLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(StringLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(SwitchStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(SwitchStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(SynchronizedStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(SynchronizedStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(ThrowStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(ThrowStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(TrueLiteral node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(TrueLiteral node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(TryStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(TryStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(UnaryExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(UnaryExpression node, BlockScope scope) {
		endVisitNode(node, scope);
	}

	public boolean visit(WhileStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(WhileStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}	
}

