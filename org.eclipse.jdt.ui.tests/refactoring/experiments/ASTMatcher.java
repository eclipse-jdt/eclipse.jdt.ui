package experiments;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.util.AST;
import org.eclipse.jdt.internal.core.refactoring.util.SelectionAnalyzer;
import org.eclipse.jface.text.ITextSelection;

class ASTMatcher extends AbstractSyntaxTreeVisitorAdapter{
	
	private AstNode fNode;
	private Set fMatchingNodes;
	
	private ASTMatcher(AstNode node){
		fNode= node;
		fMatchingNodes= new HashSet();
	}
	
	static void  run(ICompilationUnit cu, ITextSelection selection){
		try{
			AST ast= new AST(cu);
			SelectionAnalyzer analyzer= new SelectionAnalyzer(cu.getBuffer(), selection.getOffset(), selection.getLength());
			ast.accept(analyzer.getParentTracker());
			AstNode[] selected= analyzer.getSelectedNodes();
			//DebugUtils.dumpArray("selected nodes::", selected);
			if (selected == null || selected.length != 1)
				return;
			DebugUtils.dump("selected node.getClass() == " + selected[0].getClass());	
			AstNode[] matchingNodes= findMatchingNodes(ast, selected[0]);
			printASTNodes(matchingNodes);
		} catch(JavaModelException e){
			DebugUtils.dump("Exception::" + e);
		}	
	}
	
	private static void printASTNodes(AstNode[] nodes){
		for (int i = 0; i < nodes.length; i++) {
			DebugUtils.dump("node::" + nodes[i] + " offset::" + nodes[i].sourceStart() + " end::" + nodes[i].sourceEnd());
		}
	}
	
	private static AstNode[] findMatchingNodes(AST ast, AstNode node){
		ASTMatcher matcher= new ASTMatcher(node);
		ast.accept(matcher);
		return matcher.getMatchingNodes();
	}
	
	AstNode[] getMatchingNodes() {
		return (AstNode[]) fMatchingNodes.toArray(new AstNode[fMatchingNodes.size()]);
	}
	
	//--- visit
		
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		return true;
	}
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return true;
	}
	public boolean visit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
		return true;
	}
	public boolean visit(Argument argument, BlockScope scope) {
		return true;
	}
	public boolean visit(	ArrayAllocationExpression arrayAllocationExpression,	BlockScope scope) {
		return true;
	}
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		return true;
	}
	public boolean visit(	ArrayQualifiedTypeReference arrayQualifiedTypeReference,	BlockScope scope) {
		return true;
	}
	public boolean visit(	ArrayQualifiedTypeReference arrayQualifiedTypeReference,	ClassScope scope) {
		return true;
	}
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		return true;
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		return true;
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		return true;
	}
	public boolean visit(Assignment assignment, BlockScope scope) {
		return true;
	}
	public boolean visit(AssertStatement assertStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		if (equals(binaryExpression, fNode))
			fMatchingNodes.add(binaryExpression);
		return true;
	}
	public boolean visit(Block block, BlockScope scope) {
		return true;
	}
	public boolean visit(Break breakStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(Case caseStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(CastExpression castExpression, BlockScope scope) {
		if (equals(castExpression, fNode))
			fMatchingNodes.add(castExpression);
		return true;
	}
	public boolean visit(CharLiteral charLiteral, BlockScope scope) {
		if (equals(charLiteral, fNode))
			fMatchingNodes.add(charLiteral);
		return true;
	}
	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		if (equals(classLiteral, fNode))
			fMatchingNodes.add(classLiteral);
		return true;
	}
	public boolean visit(Clinit clinit, ClassScope scope) {
		return true;
	}
	public boolean visit(	CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		return true;
	}
	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		return true;
	}
	public boolean visit(	ConditionalExpression conditionalExpression,	BlockScope scope) {
		return true;
	}
	public boolean visit(	ConstructorDeclaration constructorDeclaration,ClassScope scope) {
		return true;
	}
	public boolean visit(Continue continueStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(DefaultCase defaultCaseStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(DoStatement doStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		return true;
	}
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		return true;
	}
	public boolean visit(EmptyStatement emptyStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(	ExplicitConstructorCall explicitConstructor,	BlockScope scope) {
		return true;
	}
	public boolean visit(	ExtendedStringLiteral extendedStringLiteral,	BlockScope scope) {
		return true;
	}
	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		return true;
	}
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		return true;
	}
	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		return true;
	}
	public boolean visit(FloatLiteral floatLiteral, BlockScope scope) {
		return true;
	}
	public boolean visit(ForStatement forStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		return true;
	}
	public boolean visit(Initializer initializer, MethodScope scope) {
		return true;
	}
	public boolean visit(	InstanceOfExpression instanceOfExpression, 	BlockScope scope) {
		return true;
	}

	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		if (equals(intLiteral, fNode))
			fMatchingNodes.add(intLiteral);
		return true;
	}

	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		return true;
	}
	public boolean visit(	LocalTypeDeclaration localTypeDeclaration, 	MethodScope scope) {
		return true;
	}
	public boolean visit(LongLiteral longLiteral, BlockScope scope) {
		return true;
	}
	public boolean visit(	MemberTypeDeclaration memberTypeDeclaration,	ClassScope scope) {
		return true;
	}
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		if (equals(messageSend, fNode))
			fMatchingNodes.add(messageSend);
		return true;
	}
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		return true;
	}
	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		return true;
	}
	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return true;
	}
	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		return true;
	}
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		return true;
	}
	public boolean visit(	QualifiedAllocationExpression qualifiedAllocationExpression, 	BlockScope scope) {
		return true;
	}
	public boolean visit(QualifiedNameReference qualifiedNameReference,	BlockScope scope) {
		return true;
	}
	public boolean visit(	QualifiedSuperReference qualifiedSuperReference,	BlockScope scope) {
		return true;
	}
	public boolean visit(	QualifiedThisReference qualifiedThisReference, BlockScope scope) {
		return true;
	}
	public boolean visit(	QualifiedTypeReference qualifiedTypeReference, 	BlockScope scope) {
		return true;
	}
	public boolean visit(	QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		return true;
	}
	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(	SingleNameReference singleNameReference,	BlockScope scope) {
		return true;
	}
	public boolean visit(	SingleTypeReference singleTypeReference,	BlockScope scope) {
		return true;
	}
	public boolean visit(	SingleTypeReference singleTypeReference,	ClassScope scope) {
		return true;
	}

	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		if (equals(stringLiteral, fNode))
			fMatchingNodes.add(stringLiteral);
		return true;
	}

	public boolean visit(SuperReference superReference, BlockScope scope) {
		return true;
	}
	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(	SynchronizedStatement synchronizedStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(ThisReference thisReference, BlockScope scope) {
		return true;
	}
	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		return true;
	}
	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		return true;
	}
	public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
		return true;
	}
	public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
		return true;
	}
	public boolean visit(	TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		return true;
	}
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		if (equals(unaryExpression, fNode))
			fMatchingNodes.add(unaryExpression);
		return true;
	}
	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		return true;
	}
	
	//-- equals on leaf nodes
	private static boolean equals(BinaryExpression binaryExpression, AstNode node){
		if (!(node instanceof BinaryExpression))
			return false;
		BinaryExpression be= (BinaryExpression)node;
		if (!binaryExpression.operatorToString().equals(be.operatorToString()))
			return false;
		if (! equals(binaryExpression.left, be.left))
			return false;
		if (! equals(binaryExpression.right, be.right))
			return false;
		return true;	
	}
	
	private static boolean equals(CastExpression castExpression, AstNode node){
		if (!(node instanceof CastExpression))
			return false;
		CastExpression ce= 	(CastExpression)node;
		if (! equals(castExpression.expression, ce.expression))	
			return false;
		if (!equals(castExpression.type, ce.type))	
			return false;
		return true;		 			
	}
	
	private static boolean equals(ClassLiteralAccess classLiteralAccess, AstNode node){
		if (!(node instanceof ClassLiteralAccess))
			return false;
		ClassLiteralAccess cla= (ClassLiteralAccess)node;	
		if (! equals(classLiteralAccess.type, cla.type))
			return false;	
		return true;	
	}
	
	private static boolean equals(ConditionalExpression conditionalExpression, AstNode node){
		if (!(node instanceof ConditionalExpression))
			return false;
		ConditionalExpression ce= (ConditionalExpression)node;
		if (! equals(conditionalExpression.condition, ce.condition))
			return false;
		if (! equals(conditionalExpression.valueIfTrue, ce.valueIfTrue))
			return false;
		if (! equals(conditionalExpression.valueIfFalse, ce.valueIfFalse))
			return false;	
		return true;	
	}
	
	private static boolean equals(InstanceOfExpression instanceOfExpression, AstNode node){
		if (!(node instanceof InstanceOfExpression))
			return false;
		InstanceOfExpression ioe= (InstanceOfExpression)node;
		if (! equals(instanceOfExpression.expression, ioe.expression))
			return false;
		if (! equals(instanceOfExpression.type, ioe.type))
			return false;	
		return true;	
	}
	
	private static boolean equals(MessageSend messageSend, AstNode node){
		if (!(node instanceof MessageSend))
			return false;	
		MessageSend ms= (MessageSend)node;
		if (! CharOperation.equals(messageSend.selector, ms.selector))	
			return false;	
		//XX need to check binding?
		if (! equals(messageSend.receiver, ms.receiver))	
			return false;	
		if ((messageSend.arguments == null) != (ms.arguments == null))	
			return false;	
		if (messageSend.arguments == null)
			return true;	//both no- arguments
		if (messageSend.arguments.length != ms.arguments.length)
			return false;	
		Expression[] arguments= messageSend.arguments;
		for (int i = 0; i < arguments.length; i++) {
			if (! equals(arguments[i], ms.arguments[i]))
				return false;
		}	
		return true;				
	}
	
	private static boolean equals(UnaryExpression unaryExpression, AstNode node){
		if (!(node instanceof UnaryExpression))
			return false;
		UnaryExpression ue= (UnaryExpression)node;
		if (! ue.operatorToString().equals(unaryExpression.operatorToString()))
			return false;
		return equals(unaryExpression.expression, ue.expression);
	}
	
	//-- equals on non-leaf nodes
	
	private static boolean equals(Expression expression, AstNode node){
		//XX missing
		if (expression instanceof CastExpression)
			return equals((CastExpression)expression, node);
		if (expression instanceof ClassLiteralAccess)
			return equals((ClassLiteralAccess)expression, node);
		if (expression instanceof OperatorExpression)
			return equals((OperatorExpression)expression, node);
		if (expression instanceof Literal)	
			return equals((Literal)expression, node);
		if (expression instanceof MessageSend)
			return equals((MessageSend)expression, node);
		if (expression instanceof OperatorExpression)
			return equals((OperatorExpression)expression, node);
		return expression.equals(node);	
	}

	private static boolean equals(Literal literal, AstNode node){
		if (!(node instanceof Literal))
			return false;
		return CharOperation.equals(((Literal)node).source(),  literal.source());	
	}

	private static boolean equals(OperatorExpression operatorExpression, AstNode node){
		if (operatorExpression instanceof BinaryExpression)
			return equals((BinaryExpression)operatorExpression, node);	
		if (operatorExpression instanceof ConditionalExpression)
			return equals((ConditionalExpression)operatorExpression, node);	
		if (operatorExpression instanceof InstanceOfExpression)
			return equals((InstanceOfExpression)operatorExpression, node);	
		if (operatorExpression instanceof UnaryExpression)
			return equals((UnaryExpression)operatorExpression, node);
		return operatorExpression.equals(node);		
	}	
}

