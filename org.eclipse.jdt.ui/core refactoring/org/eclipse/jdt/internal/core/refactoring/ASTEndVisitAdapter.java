/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;

public abstract class ASTEndVisitAdapter implements IAbstractSyntaxTreeVisitor {
	
	public void endVisit(AllocationExpression allocationExpression, BlockScope scope) {
	}

	public void endVisit(AND_AND_Expression and_and_Expression, BlockScope scope) {
	}

	public void endVisit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
	}

	public void endVisit(Argument argument, BlockScope scope) {
	}

	public void endVisit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
	}

	public void endVisit(ArrayInitializer arrayInitializer, BlockScope scope) {
	}

	public void endVisit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
	}

	public void endVisit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
	}

	public void endVisit(ArrayReference arrayReference, BlockScope scope) {
	}

	public void endVisit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
	}

	public void endVisit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
	}

	public void endVisit(Assignment assignment, BlockScope scope) {
	}

	public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
	}

	public void endVisit(Block block, BlockScope scope) {
	}

	public void endVisit(Break breakStatement, BlockScope scope) {
	}

	public void endVisit(Case caseStatement, BlockScope scope) {
	}

	public void endVisit(CastExpression castExpression, BlockScope scope) {
	}

	public void endVisit(CharLiteral charLiteral, BlockScope scope) {
	}

	public void endVisit(ClassLiteralAccess classLiteral, BlockScope scope) {
	}

	public void endVisit(Clinit clinit, ClassScope scope) {
	}

	public void endVisit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
	}

	public void endVisit(CompoundAssignment compoundAssignment, BlockScope scope) {
	}

	public void endVisit(ConditionalExpression conditionalExpression, BlockScope scope) {
	}

	public void endVisit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
	}

	public void endVisit(Continue continueStatement, BlockScope scope) {
	}

	public void endVisit(DefaultCase defaultCaseStatement, BlockScope scope) {
	}

	public void endVisit(DoStatement doStatement, BlockScope scope) {
	}

	public void endVisit(DoubleLiteral doubleLiteral, BlockScope scope) {
	}

	public void endVisit(EqualExpression equalExpression, BlockScope scope) {
	}

	public void endVisit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
	}

	public void endVisit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
	}

	public void endVisit(FalseLiteral falseLiteral, BlockScope scope) {
	}

	public void endVisit(FieldDeclaration fieldDeclaration, MethodScope scope) {
	}

	public void endVisit(FieldReference fieldReference, BlockScope scope) {
	}

	public void endVisit(FloatLiteral floatLiteral, BlockScope scope) {
	}

	public void endVisit(ForStatement forStatement, BlockScope scope) {
	}

	public void endVisit(IfStatement ifStatement, BlockScope scope) {
	}

	public void endVisit(ImportReference importRef, CompilationUnitScope scope) {
	}

	public void endVisit(Initializer initializer, MethodScope scope) {
	}

	public void endVisit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
	}

	public void endVisit(IntLiteral intLiteral, BlockScope scope) {
	}

	public void endVisit(LabeledStatement labeledStatement, BlockScope scope) {
	}

	public void endVisit(LocalDeclaration localDeclaration, BlockScope scope) {
	}

	public void endVisit(LocalTypeDeclaration localTypeDeclaration, MethodScope scope) {
	}

	public void endVisit(LongLiteral longLiteral, BlockScope scope) {
	}

	public void endVisit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
	}

	public void endVisit(MessageSend messageSend, BlockScope scope) {
	}

	public void endVisit(MethodDeclaration methodDeclaration, ClassScope scope) {
	}

	public void endVisit(NullLiteral nullLiteral, BlockScope scope) {
	}

	public void endVisit(OR_OR_Expression or_or_Expression, BlockScope scope) {
	}

	public void endVisit(PostfixExpression postfixExpression, BlockScope scope) {
	}

	public void endVisit(PrefixExpression prefixExpression, BlockScope scope) {
	}

	public void endVisit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
	}

	public void endVisit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
	}

	public void endVisit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
	}

	public void endVisit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
	}

	public void endVisit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
	}

	public void endVisit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
	}

	public void endVisit(ReturnStatement returnStatement, BlockScope scope) {
	}

	public void endVisit(SingleNameReference singleNameReference, BlockScope scope) {
	}

	public void endVisit(SingleTypeReference singleTypeReference, BlockScope scope) {
	}

	public void endVisit(SingleTypeReference singleTypeReference, ClassScope scope) {
	}

	public void endVisit(StringLiteral stringLiteral, BlockScope scope) {
	}

	public void endVisit(SuperReference superReference, BlockScope scope) {
	}

	public void endVisit(SwitchStatement switchStatement, BlockScope scope) {
	}

	public void endVisit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
	}

	public void endVisit(ThisReference thisReference, BlockScope scope) {
	}

	public void endVisit(ThrowStatement throwStatement, BlockScope scope) {
	}

	public void endVisit(TrueLiteral trueLiteral, BlockScope scope) {
	}

	public void endVisit(TryStatement tryStatement, BlockScope scope) {
	}

	public void endVisit(TypeDeclaration typeDeclaration, BlockScope scope) {
	}

	public void endVisit(TypeDeclaration typeDeclaration, ClassScope scope) {
	}

	public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
	}

	public void endVisit(UnaryExpression unaryExpression, BlockScope scope) {
	}

	public void endVisit(WhileStatement whileStatement, BlockScope scope) {
	}
}