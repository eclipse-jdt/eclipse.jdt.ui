package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.AnonymousLocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.AssertStatement;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.Break;
import org.eclipse.jdt.internal.compiler.ast.Case;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CharLiteral;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Continue;
import org.eclipse.jdt.internal.compiler.ast.DefaultCase;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.MemberTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.jdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.jdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedSuperReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.GenericVisitor;

class AstNodeMatcher extends AbstractSyntaxTreeVisitorAdapter{
	
	private boolean fMatches;
	private Iterator fIterator;
	
	private AstNodeMatcher(AstNode[] flattenedNode2){
		fMatches= true;
		fIterator= Arrays.asList(flattenedNode2).iterator();
	}
	
	/**
	 * works for Expression nodes for now.
	 */
	public static boolean nodeMatches(AstNode node1, AstNode[] flattenedNode2){
		Assert.isTrue(flattenedNode2[0] instanceof Expression);
		if (! (node1 instanceof Expression))
			return false;
		
		AstNodeMatcher instance= new AstNodeMatcher(flattenedNode2);
		node1.traverse(instance, (BlockScope)null);
		return instance.fMatches;
	}
	
	private AstNode getNextNode(){
		return (AstNode)fIterator.next();
	}
		
	///--- visit 
	
	//shortcut;
	private boolean no(){
		fMatches= false;
		return false;
	}	
	
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == allocationExpression)
			return false;

		if (! nextNode.getClass().equals(allocationExpression.getClass()))	
			return no();	

		AllocationExpression other= (AllocationExpression)nextNode;
		if (other.binding != allocationExpression.binding)
			return no();

		return true;
	}
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == and_and_Expression)
			return false;

		if (! nextNode.getClass().equals(and_and_Expression.getClass()))	
			return no();	
		
		//subnodes decide
		return true;
	}
	public boolean visit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == anonymousTypeDeclaration)
			return false;

		if (true)
			return no();
		
		return true;
	}
	public boolean visit(Argument argument, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == argument)
			return false;

		if (true)
			return no();
		
		return true;
	}
	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == arrayAllocationExpression)
			return false;

		if (! nextNode.getClass().equals(arrayAllocationExpression.getClass()))	
			return no();	

		ArrayAllocationExpression other= (ArrayAllocationExpression)nextNode;
		
		if (other.arrayTb != arrayAllocationExpression.arrayTb)
			return no();
		
		return true;
	}
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == arrayInitializer)
			return false;

		if (! nextNode.getClass().equals(arrayInitializer.getClass()))	
			return no();	

		ArrayInitializer other= (ArrayInitializer)nextNode;
		
		if (other.binding != arrayInitializer.binding)
			return no();
		
		return true;
	}
	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == arrayQualifiedTypeReference)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == arrayQualifiedTypeReference)
			return false;

		if (true)
			return no();	
		return true;
	}
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == arrayReference)
			return false;

		if (! nextNode.getClass().equals(arrayReference.getClass()))	
			return no();	

		ArrayReference other= (ArrayReference)nextNode;
		
		if (other.arrayElementBinding != arrayReference.arrayElementBinding)	
			return no();

		return true;
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == arrayTypeReference)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == arrayTypeReference)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(Assignment assignment, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == assignment)
			return false;

		if (! nextNode.getClass().equals(assignment.getClass()))	
			return no();	
		
		//subnodes decide
		return true;
	}
	public boolean visit(AssertStatement assertStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == assertStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == binaryExpression)
			return false;

		if (! nextNode.getClass().equals(binaryExpression.getClass()))	
			return no();	
		
		BinaryExpression other= (BinaryExpression)nextNode;

		if (! binaryExpression.operatorToString().equals(other.operatorToString()))
			return no();		
				
		return true;
	}
	public boolean visit(Block block, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == block)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(Break breakStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == breakStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(Case caseStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == caseStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(CastExpression castExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == castExpression)
			return false;

		if (! nextNode.getClass().equals(castExpression.getClass()))
			return no();	
			
		CastExpression other= (CastExpression)nextNode;
	
		if(other.castTb != castExpression.castTb)
			return no();
		return true;
	}
	public boolean visit(CharLiteral charLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == charLiteral)
			return false;

		if (! nextNode.getClass().equals(charLiteral.getClass()))
			return no();	
			
		CharLiteral other= (CharLiteral)nextNode;
		
		if (! CharOperation.equals(other.source(), charLiteral.source()))
			return no();	

		return true;
	}
	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == classLiteral)
			return false;

		if (! nextNode.getClass().equals(classLiteral.getClass()))
			return no();	
			
		ClassLiteralAccess other= (ClassLiteralAccess)nextNode;
		
		if (other.targetType != classLiteral.targetType)
			return no();
		return true;
	}
	public boolean visit(Clinit clinit, ClassScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == clinit)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == compilationUnitDeclaration)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == compoundAssignment)
			return false;

		if (! nextNode.getClass().equals(compoundAssignment.getClass()))	
			return no();	
		
		CompoundAssignment other= (CompoundAssignment)nextNode;
		if (other.operator != compoundAssignment.operator)
			return no();
		return true;
	}
	public boolean visit(ConditionalExpression conditionalExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == conditionalExpression)
			return false;

		if (! nextNode.getClass().equals(conditionalExpression.getClass()))	
			return no();			

		//subnodes decide
		return true;
	}
	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == constructorDeclaration)
			return false;

		if (true)
			return no();	
		return true;
	}
	public boolean visit(Continue continueStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == continueStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(DefaultCase defaultCaseStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == defaultCaseStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(DoStatement doStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == doStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == doubleLiteral)
			return false;

		if (! nextNode.getClass().equals(doubleLiteral.getClass()))	
			return no();	
			
		DoubleLiteral other= (DoubleLiteral)nextNode;
		if (! CharOperation.equals(other.source(), doubleLiteral.source()))
			return no();	

		return true;
	}
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == equalExpression)
			return false;

		if (! nextNode.getClass().equals(equalExpression.getClass()))	
			return no();	
		
		//subnodes decide
		return true;
	}
	public boolean visit(EmptyStatement emptyStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == emptyStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == explicitConstructor)
			return false;

		if (true)
			return no();	
		return true;
	}
	public boolean visit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == extendedStringLiteral)
			return false;

		if (! nextNode.getClass().equals(extendedStringLiteral.getClass()))	
			return no();	
			
		ExtendedStringLiteral other= (ExtendedStringLiteral)nextNode;
		if (! CharOperation.equals(other.source(), extendedStringLiteral.source()))
			return no();	

		return true;
	}
	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == falseLiteral)
			return false;

		if (! nextNode.getClass().equals(falseLiteral.getClass()))	
			return no();	
			
		return true;
	}
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == fieldDeclaration)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == fieldReference)
			return false;

		if (! nextNode.getClass().equals(fieldReference.getClass()))	
			return no();	
		
		FieldReference other= (FieldReference)nextNode;
		if (other.binding != fieldReference.binding)
			return no();
		
		return true;
	}
	public boolean visit(FloatLiteral floatLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == floatLiteral)
			return false;

		if (! nextNode.getClass().equals(floatLiteral.getClass()))	
			return no();	
			
		FloatLiteral other= (FloatLiteral)nextNode;
		if (! CharOperation.equals(other.source(), floatLiteral.source()))
			return no();	

		return true;	
	}
	public boolean visit(ForStatement forStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == forStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == ifStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == importRef)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(Initializer initializer, MethodScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == initializer)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == instanceOfExpression)
			return false;

		if (! nextNode.getClass().equals(instanceOfExpression.getClass()))	
			return no();	
		
		//subnodes decide		
		return true;
	}
	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == intLiteral)
			return false;

		if (! nextNode.getClass().equals(intLiteral.getClass()))	
			return no();	
		
		IntLiteral other= (IntLiteral)nextNode;
		if (intLiteral.value != other.value)
			return no();	
				
		return true;
	}
	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == labeledStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == localDeclaration)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(LongLiteral longLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == longLiteral)
			return false;

		if (! nextNode.getClass().equals(longLiteral.getClass()))	
			return no();	
			
		LongLiteral other= (LongLiteral)nextNode;
		if (! CharOperation.equals(other.source(), longLiteral.source()))
			return no();	

		return true;	
	}
	public boolean visit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == memberTypeDeclaration)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == messageSend)
			return false;

		if (! nextNode.getClass().equals(messageSend.getClass()))	
			return no();	
			
		MessageSend other= (MessageSend)nextNode;
		if (other.binding != messageSend.binding)
			return no();	
			
		return true;
	}
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == methodDeclaration)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == nullLiteral)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == or_or_Expression)
			return false;

		if (! nextNode.getClass().equals(or_or_Expression.getClass()))	
			return no();	
		
		//subnodes decide
		return true;
	}
	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == postfixExpression)
			return false;

		if (! nextNode.getClass().equals(postfixExpression.getClass()))	
			return no();	

		PostfixExpression other= (PostfixExpression)nextNode;
		if (other.operator != postfixExpression.operator)
			return no();

		//subnodes decide
		return true;
	}
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == prefixExpression)
			return false;

		if (! nextNode.getClass().equals(prefixExpression.getClass()))	
			return no();	

		PrefixExpression other= (PrefixExpression)nextNode;
		if (other.operator != prefixExpression.operator)
			return no();

		//subnodes decide
		return true;
	}
	public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == qualifiedAllocationExpression)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == qualifiedNameReference)
			return false;

		if (! nextNode.getClass().equals(qualifiedNameReference.getClass()))	
			return no();	
		
		QualifiedNameReference other= (QualifiedNameReference)nextNode;
		if (other.binding != qualifiedNameReference.binding)
			return no();	
		
		return true;
	}
	public boolean visit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == qualifiedSuperReference)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(QualifiedThisReference qualifiedThisReference,	BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == qualifiedThisReference)
			return false;

		if (true)
			return no();	
		return true;
	}
	public boolean visit(QualifiedTypeReference qualifiedTypeReference,	BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == qualifiedTypeReference)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(QualifiedTypeReference qualifiedTypeReference,	ClassScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == qualifiedTypeReference)
			return false;

		if (true)
			return no();	
		return true;
	}
	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == returnStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(SingleNameReference singleNameReference,	BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == singleNameReference)
			return false;

		if (! nextNode.getClass().equals(singleNameReference.getClass()))	
			return no();	
		
		SingleNameReference other= (SingleNameReference)nextNode;
		if (other.binding != singleNameReference.binding)
			return no();	
		
		return true;
	}
	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == singleTypeReference)
			return false;

		if (! nextNode.getClass().equals(singleTypeReference.getClass()))	
			return no();	
		
		SingleTypeReference other= (SingleTypeReference)nextNode;
		if (other.binding != singleTypeReference.binding)
			return no();	
		
		return true;
	}
	public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == singleTypeReference)
			return false;

		if (! nextNode.getClass().equals(singleTypeReference.getClass()))	
			return no();	
		
		SingleTypeReference other= (SingleTypeReference)nextNode;
		if (other.binding != singleTypeReference.binding)
			return no();	
		
		return true;
	}
	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == stringLiteral)
			return false;

		if (! nextNode.getClass().equals(stringLiteral.getClass()))	
			return no();	
		
		StringLiteral other= (StringLiteral)nextNode;
		if (! CharOperation.equals(other.source(), stringLiteral.source()))
			return no();	
		return true;
	}
	public boolean visit(SuperReference superReference, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == superReference)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == switchStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == synchronizedStatement)
			return false;

		if (true)
			return no();	
		return true;
	}
	public boolean visit(ThisReference thisReference, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == thisReference)
			return false;

		//XXX ?
		if (! nextNode.getClass().equals(thisReference.getClass()))	
			return no();	
			
		return true;
	}
	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == throwStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == trueLiteral)
			return false;

		if (! nextNode.getClass().equals(trueLiteral.getClass()))	
			return no();	
		return true;
	}
	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == tryStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == typeDeclaration)
			return false;

		if (true)
			return no();	
		return true;
	}
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == unaryExpression)
			return false;

		if (! nextNode.getClass().equals(unaryExpression.getClass()))	
			return no();	
		
		UnaryExpression other= (UnaryExpression)nextNode;
		if (((other.bits & AstNode.OperatorMASK) >> AstNode.OperatorSHIFT) != 
			((unaryExpression.bits & AstNode.OperatorMASK) >> AstNode.OperatorSHIFT))
			return no();	
		
		return true;
	}
	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == whileStatement)
			return false;

		if (true)
			return no();
		return true;
	}
	public boolean visit(LocalTypeDeclaration localTypeDeclaration, BlockScope scope) {
		AstNode nextNode= getNextNode();
		if (nextNode == localTypeDeclaration)
			return false;

		if (true)
			return no();
		return true;
	}
}
