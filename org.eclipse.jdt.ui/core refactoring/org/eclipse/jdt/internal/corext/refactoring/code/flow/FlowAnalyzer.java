/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;
import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;

/**
 * Special flow analyzer to determine the return value of the extracted method
 * and  the variables which have to be passed to the method.
 * 
 * Note: This analyzer doesn't do a full flow analysis. For example it doesn't
 * do dead code analysis or variable initialization analysis. It analyses the
 * the first access to a variable (read or write) and if all execution pathes
 * return a value.
 */
abstract class FlowAnalyzer implements IAbstractSyntaxTreeVisitor {

	private HashMap fData = new HashMap(100);
	/* package */ FlowContext fFlowContext= null;

	public FlowAnalyzer(FlowContext context) {
		fFlowContext= context;
	}

	protected abstract boolean createReturnFlowInfo(ReturnStatement node);

	protected abstract boolean traverseRange(int start, int end);
	
	protected boolean traverseNode(AstNode node) {
		return traverseRange(node.sourceStart, node.sourceEnd);
	}
	
	protected boolean skipNode(AstNode node) {
		return !traverseRange(node.sourceStart, node.sourceEnd);
	}
	
	protected boolean skipRange(int start, int end) {
		return !traverseRange(start, end);
	}

	//---- Hooks to create Flow info objects. User may introduce their own infos.
	
	protected ReturnFlowInfo createReturn(ReturnStatement statement) {
		return new ReturnFlowInfo(statement);
	}
	
	protected ThrowFlowInfo createThrow() {
		return new ThrowFlowInfo();
	}
	
	protected BranchFlowInfo createBranch(char[] label) {
		return new BranchFlowInfo(label, fFlowContext);
	}
	
	protected GenericSequentialFlowInfo createSequential() {
		return new GenericSequentialFlowInfo();
	}
	
	protected ConditionalFlowInfo createConditional() {
		return new ConditionalFlowInfo();
	}
	
	protected ForFlowInfo createFor() {
		return new ForFlowInfo();
	}
	
	protected TryFlowInfo createTry() {
		return new TryFlowInfo();
	}
	
	protected WhileFlowInfo createWhile() {
		return new WhileFlowInfo();
	}
	
	protected IfFlowInfo createIf() {
		return new IfFlowInfo();
	}
	
	protected DoWhileFlowInfo createDoWhile() {
		return new DoWhileFlowInfo();
	}
	
	protected SwitchFlowInfo createSwitch() {
		return new SwitchFlowInfo();
	}

	protected BlockFlowInfo createBlock() {
		return new BlockFlowInfo();
	}
	
	protected FlowContext getFlowContext() {
		return fFlowContext;
	}
	
	//---- Helpers to access flow analysis objects ----------------------------------------
	
	protected FlowInfo getFlowInfo(AstNode node) {
		return (FlowInfo)fData.remove(node);
	}
	
	protected void setFlowInfo(AstNode node, FlowInfo info) {
		fData.put(node, info);	
	}
	
	protected FlowInfo assignFlowInfo(AstNode target, AstNode source) {
		FlowInfo result= getFlowInfo(source);
		setFlowInfo(target, result);
		return result;
	}
	
	protected FlowInfo accessFlowInfo(AstNode node) {
		return (FlowInfo)fData.get(node);
	}
	
	//---- Helpers to process sequential flow infos -------------------------------------
	
	protected GenericSequentialFlowInfo processSequential(AstNode parent, AstNode[] nodes) {
		GenericSequentialFlowInfo result= createSequential(parent);
		process(result, nodes);
		return result;
	}
	
	protected GenericSequentialFlowInfo processSequential(AstNode parent, AstNode node1) {
		GenericSequentialFlowInfo result= createSequential(parent);
		if (node1 != null)
			result.merge(getFlowInfo(node1), fFlowContext);
		return result;
	}
	
	protected GenericSequentialFlowInfo processSequential(AstNode parent, AstNode node1, AstNode node2) {
		GenericSequentialFlowInfo result= createSequential(parent);
		if (node1 != null)
			result.merge(getFlowInfo(node1), fFlowContext);
		if (node2 != null)
			result.merge(getFlowInfo(node2), fFlowContext);
		return result;
	}
	
	protected GenericSequentialFlowInfo createSequential(AstNode parent) {
		GenericSequentialFlowInfo result= createSequential();
		setFlowInfo(parent, result);
		return result;
	}
	
	protected GenericSequentialFlowInfo createSequential(AstNode[] nodes) {
		GenericSequentialFlowInfo result= createSequential();
		process(result, nodes);
		return result;		
	}
	
	//---- Generic merge methods --------------------------------------------------------
	
	protected void process(GenericSequentialFlowInfo info, AstNode[] nodes) {
		if (nodes == null)
			return;
		for (int i= 0; i < nodes.length; i++) {
			info.merge(getFlowInfo(nodes[i]), fFlowContext);
		}
	}
	
	protected void process(GenericSequentialFlowInfo info, AstNode node) {
		if (node != null)
			info.merge(getFlowInfo(node), fFlowContext);
	}
	
	protected void process(GenericSequentialFlowInfo info, AstNode node1, AstNode node2) {
		if (node1 != null)
			info.merge(getFlowInfo(node1), fFlowContext);
		if (node2 != null)
			info.merge(getFlowInfo(node2), fFlowContext);
	}
	
	//---- Problem management -----------------------------------------------------------
	
	public void acceptProblem(IProblem problem) {
		// Should not be called.
	}
	
	//---- Reusable methods to process AST nodes ----------------------------------------
	
	private void process(TypeDeclaration declaration) {
		if (skipRange(declaration.declarationSourceStart, declaration.declarationSourceEnd))
			return;
		GenericSequentialFlowInfo info= processSequential(declaration, declaration.superclass);
		process(info, declaration.superInterfaces);
		process(info, declaration.memberTypes);
		process(info, declaration.fields);
		process(info, declaration.methods);
		info.setNoReturn();
	}
	
	//---- concret visit methods --------------------------------------------------------

	public void endVisit(AllocationExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.type);
		process(info, node.arguments);
	}

	public void endVisit(AND_AND_Expression node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.left, node.right);
	}

	public void endVisit(AnonymousLocalTypeDeclaration node, BlockScope scope) {
		if (skipRange(node.declarationSourceStart, node.declarationSourceEnd))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.memberTypes);
		process(info, node.fields);
		process(info, node.methods);
		info.setNoReturn();
	}

	public void endVisit(Argument node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.type, node.initialization);
	}

	public void endVisit(ArrayAllocationExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.type);
		process(info, node.dimensions);
		process(info, node.initializer);
	}

	public void endVisit(ArrayInitializer node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.expressions);
	}

	public void endVisit(ArrayQualifiedTypeReference node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(ArrayQualifiedTypeReference node, ClassScope scope) {
		// Leaf node.
	}

	public void endVisit(ArrayReference node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.receiver, node.position);
	}

	public void endVisit(ArrayTypeReference node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(ArrayTypeReference node, ClassScope scope) {
		// Leaf node.
	}

	public void endVisit(AssertStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.assertExpression, node.exceptionArgument);
	}
	
	public void endVisit(Assignment node, BlockScope scope) {
		if (skipNode(node))
			return;
		// first process right and side and then left hand side.
		processSequential(node, node.expression, node.lhs);
	}

	public void endVisit(BinaryExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.left, node.right);
	}

	public void endVisit(Block node, BlockScope scope) {
		if (skipNode(node))
			return;
		BlockFlowInfo info= createBlock();
		setFlowInfo(node, info);
		process(info, node.statements);
	}

	public void endVisit(Break node, BlockScope scope) {
		if (skipNode(node))
			return;
		setFlowInfo(node, createBranch(node.label));
	}

	public void endVisit(Case node, BlockScope scope) {
		if (skipNode(node))
			return;
		assignFlowInfo(node, node.constantExpression);
	}

	public void endVisit(CastExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.type, node.expression);
	}

	public void endVisit(CharLiteral node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(ClassLiteralAccess node, BlockScope scope) {
		if (skipNode(node))
			return;
		assignFlowInfo(node, node.type);
	}

	public void endVisit(Clinit node, ClassScope scope) {
		// Leaf node.
	}

	public void endVisit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
		if (skipNode(node))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.imports);
		process(info, node.types);
	}

	public void endVisit(CompoundAssignment node, BlockScope scope) {
		if (skipNode(node))
			return;
		endVisit((Assignment)node, scope);
	}

	public void endVisit(ConditionalExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		ConditionalFlowInfo info= createConditional();
		setFlowInfo(node, info);
		info.mergeCondition(getFlowInfo(node.condition), fFlowContext);
		info.merge(
			getFlowInfo(node.valueIfTrue), 
			getFlowInfo(node.valueIfFalse), 
			fFlowContext);
	}

	public void endVisit(ConstructorDeclaration node, ClassScope scope) {
		if (skipRange(node.declarationSourceStart, node.declarationSourceEnd))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.arguments);
		process(info, node.thrownExceptions);
		process(info, node.constructorCall);
		process(info, node.statements);
	}

	public void endVisit(Continue node, BlockScope scope) {
		if (skipNode(node))
			return;
		setFlowInfo(node, createBranch(node.label));
	}

	public void endVisit(DefaultCase node, BlockScope scope) {
		// Leaf Node
	}

	public void endVisit(DoStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		DoWhileFlowInfo info= createDoWhile();
		setFlowInfo(node, info);
		info.mergeAction(getFlowInfo(node.action), fFlowContext);
		info.mergeCondition(getFlowInfo(node.condition), fFlowContext);
		info.removeLabel(null);
	}

	public void endVisit(DoubleLiteral node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(EmptyStatement node, BlockScope scope) {
		// Leaf node.
	}
	
	public void endVisit(EqualExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.left, node.right);
	}

	public void endVisit(ExplicitConstructorCall node, BlockScope scope) {
		if (skipNode(node))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.qualification);
		process(info, node.arguments);
	}

	public void endVisit(ExtendedStringLiteral node, BlockScope scope) {
		// Leaf node
	}

	public void endVisit(FalseLiteral node, BlockScope scope) {
		// Leaf node
	}

	public void endVisit(FieldDeclaration node, MethodScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.type, node.initialization);
	}

	public void endVisit(FieldReference node, BlockScope scope) {
		if (skipNode(node))
			return;
		assignFlowInfo(node, node.receiver);
	}

	public void endVisit(FloatLiteral floatLiteral, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(ForStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		ForFlowInfo forInfo= createFor();
		setFlowInfo(node, forInfo);
		forInfo.mergeInitializer(createSequential(node.initializations), fFlowContext);
		forInfo.mergeCondition(getFlowInfo(node.condition), fFlowContext);
		forInfo.mergeAction(getFlowInfo(node.action), fFlowContext);
		// Increments are executed after the action.
		forInfo.mergeIncrement(createSequential(node.increments), fFlowContext);
		forInfo.removeLabel(null);
	}

	public void endVisit(IfStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		IfFlowInfo info= createIf();
		setFlowInfo(node, info);
		info.mergeCondition(getFlowInfo(node.condition), fFlowContext);
		info.merge(getFlowInfo(node.thenStatement), getFlowInfo(node.elseStatement), fFlowContext);
	}

	public void endVisit(ImportReference node, CompilationUnitScope scope) {
		// Leaf node.
	}

	public void endVisit(Initializer node, MethodScope scope) {
		if (skipNode(node))
			return;
		assignFlowInfo(node, node.block);
	}

	public void endVisit(InstanceOfExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.type, node.expression);
	}

	public void endVisit(IntLiteral intLiteral, BlockScope scope) {
		// Leaf Node.
	}

	public void endVisit(LabeledStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		FlowInfo info= assignFlowInfo(node, node.statement);
		if (info != null)
			info.removeLabel(node.label);
	}

	public void endVisit(LocalDeclaration node, BlockScope scope) {
		if (skipRange(node.declarationSourceStart, node.declarationSourceEnd))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.type, node.initialization);
		// ckeck if the local variable itself is selected. If so and we have an initialization
		// we have to track a write access.
		if (node.initialization != null && !skipRange(node.sourceStart, node.sourceEnd)) {
			info.merge(
				new LocalFlowInfo(node.binding, FlowInfo.WRITE, fFlowContext),
				fFlowContext);
		}
	}

	public void endVisit(LocalTypeDeclaration node, BlockScope scope) {
		process(node);
	}

	public void endVisit(LongLiteral node, BlockScope scope) {
		// Leaf Node.
	}

	public void endVisit(MemberTypeDeclaration node, ClassScope scope) {
		process(node);
	}

	public void endVisit(MessageSend node, BlockScope scope) {
		if (skipNode(node))
			return;
		// First evaluate arguments and then the receiver.
		GenericSequentialFlowInfo info= processSequential(node, node.arguments);
		process(info, node.receiver);
	}

	public void endVisit(MethodDeclaration node, ClassScope scope) {
		if (skipRange(node.declarationSourceStart, node.declarationSourceEnd))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.returnType);
		process(info, node.arguments);
		process(info, node.thrownExceptions);
		process(info, node.statements);
	}

	public void endVisit(NullLiteral node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(OR_OR_Expression node, BlockScope scope) {
		if (skipNode(node))
			return;
		processSequential(node, node.left, node.right);
	}

	public void endVisit(PostfixExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		assignFlowInfo(node, node.lhs);
	}

	public void endVisit(PrefixExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		assignFlowInfo(node, node.lhs);
	}

	public void endVisit(QualifiedAllocationExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.enclosingInstance);
		process(info, node.type);
		process(info, node.arguments);
		process(info, node.anonymousType);
	}

	public void endVisit(QualifiedNameReference node, BlockScope scope) {
		if (skipNode(node))
			return;
		if (!(node.binding instanceof LocalVariableBinding))
			return;
		setFlowInfo(node, new LocalFlowInfo(
			(LocalVariableBinding)node.binding,
			FlowInfo.READ,
			fFlowContext));
	}

	public void endVisit(QualifiedSuperReference node, BlockScope scope) {
		if (skipNode(node))
			return;
		assignFlowInfo(node, node.qualification);
	}

	public void endVisit(QualifiedThisReference node, BlockScope scope) {
		if (skipNode(node))
			return;
		assignFlowInfo(node, node.qualification);
	}

	public void endVisit(QualifiedTypeReference node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(QualifiedTypeReference node, ClassScope scope) {
		// Leaf node.
	}

	public void endVisit(ReturnStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
			
		if (createReturnFlowInfo(node)) {
			ReturnFlowInfo info= createReturn(node);
			setFlowInfo(node, info);
			info.merge(getFlowInfo(node.expression), fFlowContext);
		} else {
			assignFlowInfo(node, node.expression);
		}
	}
	
	public void endVisit(SingleNameReference node, BlockScope scope) {
		if (skipNode(node))
			return;
		if (!(node.binding instanceof LocalVariableBinding))
			return;
		setFlowInfo(node, new LocalFlowInfo(
			(LocalVariableBinding)node.binding,
			FlowInfo.READ,
			fFlowContext));
	}

	public void endVisit(SingleTypeReference node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(SingleTypeReference node, ClassScope scope) {
		// Leaf node.
	}

	public void endVisit(StringLiteral node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(SuperReference node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(SwitchStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		Statement[] statements= node.statements;
		SwitchFlowInfo switchFlowInfo= createSwitch();
		setFlowInfo(node, switchFlowInfo);
		switchFlowInfo.mergeTest(getFlowInfo(node.testExpression), fFlowContext);
		GenericSequentialFlowInfo info= null;
		boolean defaultCaseExists= false;
		
		for (int i= 0; i < statements.length; i++) {
			Statement statement= statements[i];
			if (statement instanceof DefaultCase) {
				defaultCaseExists= true;
			}
			if (statement instanceof Case || statement instanceof DefaultCase) {
				if (info == null) {
					info= createSequential();
				} else {
					if (info.isReturn() || info.isPartialReturn() || info.branches()) {
						switchFlowInfo.mergeCase(info, fFlowContext);
						info= createSequential();
					}
				}
			} else {
				info.merge(getFlowInfo(statement), fFlowContext);
			}
		}
		switchFlowInfo.mergeCase(info, fFlowContext);
		switchFlowInfo.mergeDefault(defaultCaseExists, fFlowContext);
			
		switchFlowInfo.removeLabel(null);
	}

	public void endVisit(SynchronizedStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		GenericSequentialFlowInfo info= processSequential(node, node.expression);
		process(info, node.block);
	}

	public void endVisit(ThisReference node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(ThrowStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		ThrowFlowInfo info= createThrow();
		setFlowInfo(node, info);
		info.merge(getFlowInfo(node.exception), fFlowContext);
	}

	public void endVisit(TrueLiteral node, BlockScope scope) {
		// Leaf node.
	}

	public void endVisit(TryStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		TryFlowInfo info= createTry();
		setFlowInfo(node, info);
		info.mergeTry(getFlowInfo(node.tryBlock), fFlowContext);
		Block[] blocks= node.catchBlocks;
		if (blocks != null) {
			for (int i= 0; i < blocks.length; i++) {
				info.mergeCase(getFlowInfo(blocks[i]), fFlowContext);
			}
		}
		info.mergeFinally(getFlowInfo(node.finallyBlock), fFlowContext);
	}

	public void endVisit(TypeDeclaration node, CompilationUnitScope scope) {
		process(node);
	}

	public void endVisit(UnaryExpression node, BlockScope scope) {
		if (skipNode(node))
			return;
		assignFlowInfo(node, node.expression);
	}

	public void endVisit(WhileStatement node, BlockScope scope) {
		if (skipNode(node))
			return;
		WhileFlowInfo info= createWhile();
		setFlowInfo(node, info);
		info.mergeCondition(getFlowInfo(node.condition), fFlowContext);
		info.mergeAction(getFlowInfo(node.action), fFlowContext);
		info.removeLabel(null);
	}
	
	//---- special visit methods -------------------------------------------------------
	
	private boolean visitAssignment(Assignment node, BlockScope scope, int accessMode) {
		LocalVariableBinding binding= getWrite(node.lhs);
		if (binding != null) {
			if (traverseNode(node.lhs)) {
				setFlowInfo(node.lhs, new LocalFlowInfo(
					binding,
					accessMode,
					fFlowContext));
			}
			node.expression.traverse(this, scope);
			return false;
		}
		return true;
	}		
	
	private static LocalVariableBinding getWrite(Reference reference) {
		if (reference == null)
			return null;
		if (!(reference instanceof SingleNameReference))
			return null;
		SingleNameReference snr= (SingleNameReference)reference;
		if (!(snr.binding instanceof LocalVariableBinding))
			return null;
		return (LocalVariableBinding)snr.binding;
	}

	public boolean visit(Assignment node, BlockScope scope) {
		if (skipNode(node))
			return false;
		return visitAssignment(node, scope, FlowInfo.WRITE);
	}
	
	public boolean visit(CompoundAssignment node, BlockScope scope) {
		int mode= FlowInfo.READ;
		if (fFlowContext.computeReturnValues())
			mode= FlowInfo.WRITE;
		return visitAssignment(node, scope, mode);
	}
	
	public boolean visit(PostfixExpression node, BlockScope scope) {
		return visit((CompoundAssignment)node, scope);
	}
	
	public boolean visit(PrefixExpression node, BlockScope scope) {
		return visit((CompoundAssignment)node, scope);
	}
	
	//---- Generic Visit method to determine if we have to traverse the node ----------------
	
	public boolean visit(AllocationExpression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(AND_AND_Expression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(AnonymousLocalTypeDeclaration node, BlockScope scope) {
		return traverseRange(node.declarationSourceStart, node.declarationSourceEnd);
	}

	public boolean visit(Argument node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ArrayAllocationExpression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ArrayInitializer node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ArrayQualifiedTypeReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ArrayQualifiedTypeReference node, ClassScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ArrayReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ArrayTypeReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ArrayTypeReference node, ClassScope scope) {
		return traverseNode(node);
	}
	
	public boolean visit(AssertStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(BinaryExpression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(Block node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(Break node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(Case node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(CastExpression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(CharLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ClassLiteralAccess node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(Clinit node, ClassScope scope) {
		return traverseRange(node.declarationSourceStart, node.declarationSourceEnd);
	}

	public boolean visit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ConditionalExpression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ConstructorDeclaration node, ClassScope scope) {
		return traverseRange(node.declarationSourceStart, node.declarationSourceEnd);
	}

	public boolean visit(Continue node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(DefaultCase node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(DoStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(DoubleLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(EmptyStatement node, BlockScope scope) {
		// Empty statements aren't of any interest.
		return false;
	}
	
	public boolean visit(EqualExpression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ExplicitConstructorCall node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ExtendedStringLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(FalseLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(FieldDeclaration node, MethodScope scope) {
		return traverseNode(node);
	}

	public boolean visit(FieldReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(FloatLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ForStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(IfStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ImportReference node, CompilationUnitScope scope) {
		return traverseNode(node);
	}

	public boolean visit(Initializer node, MethodScope scope) {
		return traverseNode(node);
	}

	public boolean visit(InstanceOfExpression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(IntLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(LabeledStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(LocalDeclaration node, BlockScope scope) {
		return traverseRange(node.declarationSourceStart, node.declarationSourceEnd);
	}

	public boolean visit(LocalTypeDeclaration node, BlockScope scope) {
		return traverseRange(node.declarationSourceStart, node.declarationSourceEnd);
	}

	public boolean visit(LongLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(MemberTypeDeclaration node, ClassScope scope) {
		return traverseRange(node.declarationSourceStart, node.declarationSourceEnd);
	}

	public boolean visit(MessageSend node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(MethodDeclaration node, ClassScope scope) {
		return traverseRange(node.declarationSourceStart, node.declarationSourceEnd);
	}

	public boolean visit(NullLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(OR_OR_Expression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(QualifiedAllocationExpression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(QualifiedNameReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(QualifiedSuperReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(QualifiedThisReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(QualifiedTypeReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(QualifiedTypeReference node, ClassScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ReturnStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(SingleNameReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(SingleTypeReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(SingleTypeReference node, ClassScope scope) {
		return traverseNode(node);
	}

	public boolean visit(StringLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(SuperReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(SwitchStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(SynchronizedStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ThisReference node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(ThrowStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(TrueLiteral node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(TryStatement node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(TypeDeclaration node, CompilationUnitScope scope) {
		return traverseRange(node.declarationSourceStart, node.declarationSourceEnd);
	}

	public boolean visit(UnaryExpression node, BlockScope scope) {
		return traverseNode(node);
	}

	public boolean visit(WhileStatement node, BlockScope scope) {
		return traverseNode(node);
	}
}