/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;import org.eclipse.jdt.internal.compiler.IProblem;import org.eclipse.jdt.internal.compiler.ast.*;import org.eclipse.jdt.internal.compiler.lookup.BlockScope;import org.eclipse.jdt.internal.compiler.lookup.ClassScope;import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;import org.eclipse.jdt.internal.compiler.lookup.MethodScope;

public class ASTParentTrackingAdapter implements IAbstractSyntaxTreeVisitor, IParentTracker {

	private List fParentStack= new ArrayList(10);
	private IAbstractSyntaxTreeVisitor fVisitor;

	public ASTParentTrackingAdapter(IAbstractSyntaxTreeVisitor visitor) {
		fVisitor= visitor;
		Assert.isNotNull(fVisitor);
	}
	
	public AstNode getParent() {
		return (AstNode)fParentStack.get(fParentStack.size() - 1);
	}
	
	public List getParents() {
		return fParentStack;
	}
	
	private void pushParent(AstNode node) {
		fParentStack.add(node);
	}
	
	private void popParent() {
		fParentStack.remove(fParentStack.size() - 1);
	}
	
	public void acceptProblem(IProblem problem) {
		fVisitor.acceptProblem(problem);
	}

	public void endVisit(AllocationExpression allocationExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(allocationExpression, scope);	
	}

	public void endVisit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(and_and_Expression, scope);	
	}

	public void endVisit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
		popParent();
		fVisitor.endVisit(anonymousTypeDeclaration, scope);	
	}

	public void endVisit(Argument argument, BlockScope scope) {
		popParent();
		fVisitor.endVisit(argument, scope);	
	}

	public void endVisit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(arrayAllocationExpression, scope);	
	}

	public void endVisit(ArrayInitializer arrayInitializer, BlockScope scope) {
		popParent();
		fVisitor.endVisit(arrayInitializer, scope);	
	}

	public void endVisit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(arrayQualifiedTypeReference, scope);	
	}

	public void endVisit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
		popParent();
		fVisitor.endVisit(arrayQualifiedTypeReference, scope);	
	}

	public void endVisit(ArrayReference arrayReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(arrayReference, scope);	
	}

	public void endVisit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(arrayTypeReference, scope);	
	}

	public void endVisit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		popParent();
		fVisitor.endVisit(arrayTypeReference, scope);	
	}

	public void endVisit(Assignment assignment, BlockScope scope) {
		popParent();
		fVisitor.endVisit(assignment, scope);	
	}

	public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(binaryExpression, scope);	
	}

	public void endVisit(Block block, BlockScope scope) {
		popParent();
		fVisitor.endVisit(block, scope);	
	}

	public void endVisit(Break breakStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(breakStatement, scope);	
	}

	public void endVisit(Case caseStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(caseStatement, scope);	
	}

	public void endVisit(CastExpression castExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(castExpression, scope);	
	}

	public void endVisit(CharLiteral charLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(charLiteral, scope);	
	}

	public void endVisit(ClassLiteralAccess classLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(classLiteral, scope);	
	}

	public void endVisit(Clinit clinit, ClassScope scope) {
		popParent();
		fVisitor.endVisit(clinit, scope);	
	}

	public void endVisit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		popParent();
		fVisitor.endVisit(compilationUnitDeclaration, scope);	
	}

	public void endVisit(CompoundAssignment compoundAssignment, BlockScope scope) {
		popParent();
		fVisitor.endVisit(compoundAssignment, scope);	
	}

	public void endVisit(ConditionalExpression conditionalExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(conditionalExpression, scope);	
	}

	public void endVisit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		popParent();
		fVisitor.endVisit(constructorDeclaration, scope);	
	}

	public void endVisit(Continue continueStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(continueStatement, scope);	
	}

	public void endVisit(DefaultCase defaultCaseStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(defaultCaseStatement, scope);	
	}

	public void endVisit(DoStatement doStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(doStatement, scope);	
	}

	public void endVisit(DoubleLiteral doubleLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(doubleLiteral, scope);	
	}

	public void endVisit(EmptyStatement node, BlockScope scope) {
		popParent();
		fVisitor.endVisit(node, scope);
	}
	
	public void endVisit(EqualExpression equalExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(equalExpression, scope);	
	}

	public void endVisit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		popParent();
		fVisitor.endVisit(explicitConstructor, scope);	
	}

	public void endVisit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(extendedStringLiteral, scope);	
	}

	public void endVisit(FalseLiteral falseLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(falseLiteral, scope);	
	}

	public void endVisit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		popParent();
		fVisitor.endVisit(fieldDeclaration, scope);	
	}

	public void endVisit(FieldReference fieldReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(fieldReference, scope);	
	}

	public void endVisit(FloatLiteral floatLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(floatLiteral, scope);	
	}

	public void endVisit(ForStatement forStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(forStatement, scope);	
	}

	public void endVisit(IfStatement ifStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(ifStatement, scope);	
	}

	public void endVisit(ImportReference importRef, CompilationUnitScope scope) {
		popParent();
		fVisitor.endVisit(importRef, scope);	
	}

	public void endVisit(Initializer initializer, MethodScope scope) {
		popParent();
		fVisitor.endVisit(initializer, scope);	
	}

	public void endVisit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(instanceOfExpression, scope);	
	}

	public void endVisit(IntLiteral intLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(intLiteral, scope);	
	}

	public void endVisit(LabeledStatement labeledStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(labeledStatement, scope);	
	}

	public void endVisit(LocalDeclaration localDeclaration, BlockScope scope) {
		popParent();
		fVisitor.endVisit(localDeclaration, scope);	
	}

	public void endVisit(LocalTypeDeclaration localTypeDeclaration, MethodScope scope) {
		popParent();
		fVisitor.endVisit(localTypeDeclaration, scope);	
	}

	public void endVisit(LongLiteral longLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(longLiteral, scope);	
	}

	public void endVisit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
		popParent();
		fVisitor.endVisit(memberTypeDeclaration, scope);	
	}

	public void endVisit(MessageSend messageSend, BlockScope scope) {
		popParent();
		fVisitor.endVisit(messageSend, scope);	
	}

	public void endVisit(MethodDeclaration methodDeclaration, ClassScope scope) {
		popParent();
		fVisitor.endVisit(methodDeclaration, scope);	
	}

	public void endVisit(NullLiteral nullLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(nullLiteral, scope);	
	}

	public void endVisit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(or_or_Expression, scope);	
	}

	public void endVisit(PostfixExpression postfixExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(postfixExpression, scope);	
	}

	public void endVisit(PrefixExpression prefixExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(prefixExpression, scope);	
	}

	public void endVisit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(qualifiedAllocationExpression, scope);	
	}

	public void endVisit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(qualifiedNameReference, scope);	
	}

	public void endVisit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(qualifiedSuperReference, scope);	
	}

	public void endVisit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(qualifiedThisReference, scope);	
	}

	public void endVisit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(qualifiedTypeReference, scope);	
	}

	public void endVisit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		popParent();
		fVisitor.endVisit(qualifiedTypeReference, scope);	
	}

	public void endVisit(ReturnStatement returnStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(returnStatement, scope);	
	}

	public void endVisit(SingleNameReference singleNameReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(singleNameReference, scope);	
	}

	public void endVisit(SingleTypeReference singleTypeReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(singleTypeReference, scope);	
	}

	public void endVisit(SingleTypeReference singleTypeReference, ClassScope scope) {
		popParent();
		fVisitor.endVisit(singleTypeReference, scope);	
	}

	public void endVisit(StringLiteral stringLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(stringLiteral, scope);	
	}

	public void endVisit(SuperReference superReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(superReference, scope);	
	}

	public void endVisit(SwitchStatement switchStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(switchStatement, scope);	
	}

	public void endVisit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(synchronizedStatement, scope);	
	}

	public void endVisit(ThisReference thisReference, BlockScope scope) {
		popParent();
		fVisitor.endVisit(thisReference, scope);	
	}

	public void endVisit(ThrowStatement throwStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(throwStatement, scope);	
	}

	public void endVisit(TrueLiteral trueLiteral, BlockScope scope) {
		popParent();
		fVisitor.endVisit(trueLiteral, scope);	
	}

	public void endVisit(TryStatement tryStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(tryStatement, scope);	
	}

	public void endVisit(TypeDeclaration typeDeclaration, BlockScope scope) {
		popParent();
		fVisitor.endVisit(typeDeclaration, scope);	
	}

	public void endVisit(TypeDeclaration typeDeclaration, ClassScope scope) {
		popParent();
		fVisitor.endVisit(typeDeclaration, scope);	
	}

	public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		popParent();
		fVisitor.endVisit(typeDeclaration, scope);	
	}

	public void endVisit(UnaryExpression unaryExpression, BlockScope scope) {
		popParent();
		fVisitor.endVisit(unaryExpression, scope);	
	}

	public void endVisit(WhileStatement whileStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(whileStatement, scope);	
	}

	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		boolean result= fVisitor.visit(allocationExpression, scope);
		pushParent(allocationExpression);
		return result;
	}

	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		boolean result= fVisitor.visit(and_and_Expression, scope);
		pushParent(and_and_Expression);
		return result;
	}

	public boolean visit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
		boolean result= fVisitor.visit(anonymousTypeDeclaration, scope);
		pushParent(anonymousTypeDeclaration);
		return result;
	}

	public boolean visit(Argument argument, BlockScope scope) {
		boolean result= fVisitor.visit(argument, scope);
		pushParent(argument);
		return result;
	}

	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		boolean result= fVisitor.visit(arrayAllocationExpression, scope);
		pushParent(arrayAllocationExpression);
		return result;
	}

	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		boolean result= fVisitor.visit(arrayInitializer, scope);
		pushParent(arrayInitializer);
		return result;
	}

	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		boolean result= fVisitor.visit(arrayQualifiedTypeReference, scope);
		pushParent(arrayQualifiedTypeReference);
		return result;
	}

	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
		boolean result= fVisitor.visit(arrayQualifiedTypeReference, scope);
		pushParent(arrayQualifiedTypeReference);
		return result;
	}

	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		boolean result= fVisitor.visit(arrayReference, scope);
		pushParent(arrayReference);
		return result;
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		boolean result= fVisitor.visit(arrayTypeReference, scope);
		pushParent(arrayTypeReference);
		return result;
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		boolean result= fVisitor.visit(arrayTypeReference, scope);
		pushParent(arrayTypeReference);
		return result;
	}

	public boolean visit(Assignment assignment, BlockScope scope) {
		boolean result= fVisitor.visit(assignment, scope);
		pushParent(assignment);
		return result;
	}

	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		boolean result= fVisitor.visit(binaryExpression, scope);
		pushParent(binaryExpression);
		return result;
	}

	public boolean visit(Block block, BlockScope scope) {
		boolean result= fVisitor.visit(block, scope);
		pushParent(block);
		return result;
	}

	public boolean visit(Break breakStatement, BlockScope scope) {
		boolean result= fVisitor.visit(breakStatement, scope);
		pushParent(breakStatement);
		return result;
	}

	public boolean visit(Case caseStatement, BlockScope scope) {
		boolean result= fVisitor.visit(caseStatement, scope);
		pushParent(caseStatement);
		return result;
	}

	public boolean visit(CastExpression castExpression, BlockScope scope) {
		boolean result= fVisitor.visit(castExpression, scope);
		pushParent(castExpression);
		return result;
	}

	public boolean visit(CharLiteral charLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(charLiteral, scope);
		pushParent(charLiteral);
		return result;
	}

	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(classLiteral, scope);
		pushParent(classLiteral);
		return result;
	}

	public boolean visit(Clinit clinit, ClassScope scope) {
		boolean result= fVisitor.visit(clinit, scope);
		pushParent(clinit);
		return result;
	}

	public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		boolean result= fVisitor.visit(compilationUnitDeclaration, scope);
		pushParent(compilationUnitDeclaration);
		return result;
	}

	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		boolean result= fVisitor.visit(compoundAssignment, scope);
		pushParent(compoundAssignment);
		return result;
	}

	public boolean visit(ConditionalExpression conditionalExpression, BlockScope scope) {
		boolean result= fVisitor.visit(conditionalExpression, scope);
		pushParent(conditionalExpression);
		return result;
	}

	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		boolean result= fVisitor.visit(constructorDeclaration, scope);
		pushParent(constructorDeclaration);
		return result;
	}

	public boolean visit(Continue continueStatement, BlockScope scope) {
		boolean result= fVisitor.visit(continueStatement, scope);
		pushParent(continueStatement);
		return result;
	}

	public boolean visit(DefaultCase defaultCaseStatement, BlockScope scope) {
		boolean result= fVisitor.visit(defaultCaseStatement, scope);
		pushParent(defaultCaseStatement);
		return result;
	}

	public boolean visit(DoStatement doStatement, BlockScope scope) {
		boolean result= fVisitor.visit(doStatement, scope);
		pushParent(doStatement);
		return result;
	}

	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(doubleLiteral, scope);
		pushParent(doubleLiteral);
		return result;
	}

	public boolean visit(EmptyStatement node, BlockScope scope) {
		boolean result= fVisitor.visit(node, scope);
		pushParent(node);
		return result;
	}
	
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		boolean result= fVisitor.visit(equalExpression, scope);
		pushParent(equalExpression);
		return result;
	}

	public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		boolean result= fVisitor.visit(explicitConstructor, scope);
		pushParent(explicitConstructor);
		return result;
	}

	public boolean visit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(extendedStringLiteral, scope);
		pushParent(extendedStringLiteral);
		return result;
	}

	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(falseLiteral, scope);
		pushParent(falseLiteral);
		return result;
	}

	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		boolean result= fVisitor.visit(fieldDeclaration, scope);
		pushParent(fieldDeclaration);
		return result;
	}

	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		boolean result= fVisitor.visit(fieldReference, scope);
		pushParent(fieldReference);
		return result;
	}

	public boolean visit(FloatLiteral floatLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(floatLiteral, scope);
		pushParent(floatLiteral);
		return result;
	}

	public boolean visit(ForStatement forStatement, BlockScope scope) {
		boolean result= fVisitor.visit(forStatement, scope);
		pushParent(forStatement);
		return result;
	}

	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		boolean result= fVisitor.visit(ifStatement, scope);
		pushParent(ifStatement);
		return result;
	}

	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		boolean result= fVisitor.visit(importRef, scope);
		pushParent(importRef);
		return result;
	}

	public boolean visit(Initializer initializer, MethodScope scope) {
		boolean result= fVisitor.visit(initializer, scope);
		pushParent(initializer);
		return result;
	}

	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		boolean result= fVisitor.visit(instanceOfExpression, scope);
		pushParent(instanceOfExpression);
		return result;
	}

	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(intLiteral, scope);
		pushParent(intLiteral);
		return result;
	}

	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		boolean result= fVisitor.visit(labeledStatement, scope);
		pushParent(labeledStatement);
		return result;
	}

	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		boolean result= fVisitor.visit(localDeclaration, scope);
		pushParent(localDeclaration);
		return result;
	}

	public boolean visit(LocalTypeDeclaration localTypeDeclaration, MethodScope scope) {
		boolean result= fVisitor.visit(localTypeDeclaration, scope);
		pushParent(localTypeDeclaration);
		return result;
	}

	public boolean visit(LongLiteral longLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(longLiteral, scope);
		pushParent(longLiteral);
		return result;
	}

	public boolean visit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
		boolean result= fVisitor.visit(memberTypeDeclaration, scope);
		pushParent(memberTypeDeclaration);
		return result;
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		boolean result= fVisitor.visit(messageSend, scope);
		pushParent(messageSend);
		return result;
	}

	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		boolean result= fVisitor.visit(methodDeclaration, scope);
		pushParent(methodDeclaration);
		return result;
	}

	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(nullLiteral, scope);
		pushParent(nullLiteral);
		return result;
	}

	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		boolean result= fVisitor.visit(or_or_Expression, scope);
		pushParent(or_or_Expression);
		return result;
	}

	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		boolean result= fVisitor.visit(postfixExpression, scope);
		pushParent(postfixExpression);
		return result;
	}

	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		boolean result= fVisitor.visit(prefixExpression, scope);
		pushParent(prefixExpression);
		return result;
	}

	public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		boolean result= fVisitor.visit(qualifiedAllocationExpression, scope);
		pushParent(qualifiedAllocationExpression);
		return result;
	}

	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		boolean result= fVisitor.visit(qualifiedNameReference, scope);
		pushParent(qualifiedNameReference);
		return result;
	}

	public boolean visit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
		boolean result= fVisitor.visit(qualifiedSuperReference, scope);
		pushParent(qualifiedSuperReference);
		return result;
	}

	public boolean visit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
		boolean result= fVisitor.visit(qualifiedThisReference, scope);
		pushParent(qualifiedThisReference);
		return result;
	}

	public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		boolean result= fVisitor.visit(qualifiedTypeReference, scope);
		pushParent(qualifiedTypeReference);
		return result;
	}

	public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		boolean result= fVisitor.visit(qualifiedTypeReference, scope);
		pushParent(qualifiedTypeReference);
		return result;
	}

	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		boolean result= fVisitor.visit(returnStatement, scope);
		pushParent(returnStatement);
		return result;
	}

	public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
		boolean result= fVisitor.visit(singleNameReference, scope);
		pushParent(singleNameReference);
		return result;
	}

	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		boolean result= fVisitor.visit(singleTypeReference, scope);
		pushParent(singleTypeReference);
		return result;
	}

	public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
		boolean result= fVisitor.visit(singleTypeReference, scope);
		pushParent(singleTypeReference);
		return result;
	}

	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(stringLiteral, scope);
		pushParent(stringLiteral);
		return result;
	}

	public boolean visit(SuperReference superReference, BlockScope scope) {
		boolean result= fVisitor.visit(superReference, scope);
		pushParent(superReference);
		return result;
	}

	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		boolean result= fVisitor.visit(switchStatement, scope);
		pushParent(switchStatement);
		return result;
	}

	public boolean visit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
		boolean result= fVisitor.visit(synchronizedStatement, scope);
		pushParent(synchronizedStatement);
		return result;
	}

	public boolean visit(ThisReference thisReference, BlockScope scope) {
		boolean result= fVisitor.visit(thisReference, scope);
		pushParent(thisReference);
		return result;
	}

	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		boolean result= fVisitor.visit(throwStatement, scope);
		pushParent(throwStatement);
		return result;
	}

	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		boolean result= fVisitor.visit(trueLiteral, scope);
		pushParent(trueLiteral);
		return result;
	}

	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		boolean result= fVisitor.visit(tryStatement, scope);
		pushParent(tryStatement);
		return result;
	}

	public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
		boolean result= fVisitor.visit(typeDeclaration, scope);
		pushParent(typeDeclaration);
		return result;
	}

	public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
		boolean result= fVisitor.visit(typeDeclaration, scope);
		pushParent(typeDeclaration);
		return result;
	}

	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		boolean result= fVisitor.visit(typeDeclaration, scope);
		pushParent(typeDeclaration);
		return result;
	}

	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		boolean result= fVisitor.visit(unaryExpression, scope);
		pushParent(unaryExpression);
		return result;
	}

	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		boolean result= fVisitor.visit(whileStatement, scope);
		pushParent(whileStatement);
		return result;
	}
	
    /**
     * @see IAbstractSyntaxTreeVisitor#endVisit(AssertStatement, BlockScope)
     */
    public void endVisit(AssertStatement assertStatement, BlockScope scope) {
		popParent();
		fVisitor.endVisit(assertStatement, scope);	
    }

    /**
     * @see IAbstractSyntaxTreeVisitor#visit(AssertStatement, BlockScope)
     */
    public boolean visit(AssertStatement assertStatement, BlockScope scope) {
		boolean result= fVisitor.visit(assertStatement, scope);
		pushParent(assertStatement);
		return result;
    }

}
