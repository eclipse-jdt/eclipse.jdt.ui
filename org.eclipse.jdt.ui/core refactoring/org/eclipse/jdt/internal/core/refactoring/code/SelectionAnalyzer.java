/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypes;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.refactoring.ASTEndVisitAdapter;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.AstNodeData;
import org.eclipse.jdt.internal.core.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.core.refactoring.IParentTracker;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.util.ASTs;
import org.eclipse.jdt.internal.core.refactoring.util.Selection;

/**
 * Analyzer to check if a selection covers a valid set of nodes of an abstract syntax
 * tree. The selection is valid iff
 * <ul>
 * 	<li>it does not start or end in the middle of a comment.</li>
 * 	<li>it fully covers a set of nodes or a single node.</li>
 * 	<li>no extract characters except the empty statement ";" is included in the selection.</li>
 * </ul>
 * Examples of valid selections. The selection is denoted by []:
 * <ul>
 * 	<li><code>[foo()]</code></li>
 * 	<li><code>[foo();]</code></li>
 * 	<li><code>if ([i == 10])</code></li>
 * </ul>
 */
public class SelectionAnalyzer extends AbstractSyntaxTreeVisitorAdapter {
	
	/** Flags to indicate that the automaton is in an undefined state. */
	public static final int UNDEFINED=   0;
	public static final Integer UNDEFINDED_OBJECT= new Integer(UNDEFINED);
	
	/** Flags to indicating that the automaton traverses nodes before the selection. */
	public static final int BEFORE=	1;
	public static final Integer BEFORE_OBJECT= new Integer(BEFORE);
	
	/** Flags indicating that the automaton traverses nodes covered by the selection. */
	public static final int SELECTED=	2;
	public static final Integer SELECTED_OBJECT= new Integer(SELECTED);
	
	/** Flags indicating that the automaton traverses nodes after the selection. */
	public static final int AFTER=	      3;
	public static final Integer AFTER_OBJECT= new Integer(AFTER);
	
	// Parent tracking interface
	private IParentTracker fParentTracker;
	
	// The buffer containing the source code.
	private ExtendedBuffer fBuffer;
	
	// Selection state.
	protected Selection fSelection;
	private boolean fTraverseSelectedNodes;

	// Error handling	
	private boolean fCompileErrorFound;
	private RefactoringStatus fStatus= new RefactoringStatus();
	private int[] fLineSeparatorPositions;	

	// The method that encloses the selection.	
	private Scope fEnclosingScope;
	private AbstractMethodDeclaration fEnclosingMethod;
	private Stack fMethodStack= new Stack();
	
	// Selected nodes
	protected List fTopNodes;
	private List fParentsOfFirstSelectedNode;
	private AstNode fParentOfFirstSelectedNode;
	private AstNode fFirstSelectedNode;
	private boolean fNeedsSemicolon;
	
	// Type binding of the selected expression
	protected TypeBinding fExpressionTypeBinding;
	protected boolean fExpressionIsPartOfOperator;
	protected boolean fExpressionTypeIsVisible;
	
	private static final int BREAK_LENGTH= "break".length(); //$NON-NLS-1$
	private static final int CONTINUE_LENGTH= "continue".length(); //$NON-NLS-1$
	 
	public SelectionAnalyzer(ExtendedBuffer buffer, int start, int length) {
		fBuffer= buffer;
		Assert.isTrue(fBuffer != null);
		fSelection= Selection.createFromStartLength(start, length);
	}

	//---- Parent tracking ----------------------------------------------------------
	
	/**
	 * Sets the parent tracker to access the parent of a active
	 * AST node.
	 */
	public void setParentTracker(IParentTracker tracker) {
		fParentTracker= tracker;
	}
	
	private AstNode getParent() {
		return fParentTracker.getParent();
	}
	
	//---- Precondition checking ----------------------------------------------------
	
	/**
	 * Returns the refactoring status object containing information about correctness
	 * of the selection.
	 * 
	 * @return the status containing information about the correctness of the selection.
	 */
	public RefactoringStatus getStatus() {
		checkSelection();
		return fStatus;
	}
	
	/**
	 * Returns the parents of the first selected node.
	 * 
	 * @returns the parents of the first selected node or <code>null</code> if no node
	 *  is selected
	 */
	public AstNode[] getParents() {
		return null;
	}
	
	/**
	 * Returns the method that encloses the text selection. Returns <code>null</code>
	 * is the text selection isn't enclosed by a method or is the text selection doesn't
	 * mark a valid set of statements.
	 * @return the method that encloses the text selection.
	 */
	public AbstractMethodDeclaration getEnclosingMethod() {
		return fEnclosingMethod;
	}
	
	/**
	 * Returns the scope containing the selected top level nodes.
	 * 
	 * @return the scope covering the selected top level nodes.
	 */
	public Scope getEnclosingScope() {
		return fEnclosingScope;
	}
	
	/**
	 * Checks, if the current method visited by this visitor is the method enclosing the
	 * selection. If this is the case, the method returns <code>true</code>, otherwise 
	 * <code>false</code> is returned.
	 * 
	 * @return <code>true<code> if <code>fMethodStack.peek() == getEnclosingMethod</code> 
	 *  otherwise <code>false</code>.
	 */
	public boolean processesEnclosingMethod() {
		if (fMethodStack.isEmpty())
			return false;
		return getEnclosingMethod() == fMethodStack.peek();
	}
	
	/**
	 * Returns <code>true</code> if the given AST node is selected in the text editor.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether or not the given AST node is selected in the editor.
	 */
	public boolean isSelected(AstNode node) {
		return fSelection.covers(node);
	}
	
	/**
	 * Returns the last selected node.
	 */
	public AstNode getLastSelectedNode() {
		if (fTopNodes == null || fTopNodes.size() == 0)
			return null;
			
		return (AstNode)fTopNodes.get(fTopNodes.size() - 1);
	}
	
	/**
	 * Returns the end position of the last selected node. Returns -1 if there
	 * is any selected node.
	 * 
	 * @return the end position of the last selected node or -1.
	 */
	public int getEndOfLastSelectedNode() {
		AstNode node= getLastSelectedNode();
		if (node == null)
			return -1;
		return node.sourceEnd;
	}
	
	/**
	 * Returns <code>true</code> if the selection covers a single expression. Otherwise
	 * <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if selection covers expression; otherwise <code>false</code>.
	 */
	public boolean isExpressionSelected() {
		if (fTopNodes == null || fTopNodes.size() != 1)
			return false;
		return (fTopNodes.get(0) instanceof Expression);
	}
	
	/**
	 * Returns true if the last selected statement needs a semicolon to have correct
	 * syntax. For example a block or a try / catch / finnally doesn't need a semicolon
	 * at the end.
	 */
	public boolean getNeedsSemicolon() {
		return fNeedsSemicolon;
	}
	
	public boolean returnsExpressionValue() {
		return (fExpressionTypeBinding != null && fExpressionTypeBinding != BaseTypes.VoidBinding);
	}
	
	public TypeBinding getExpressionTypeBinding() {
		return fExpressionTypeBinding;
	}
	
	public boolean isExpressionTypeVisible() {
		return fExpressionTypeIsVisible;
	}
	
	//---- Problem management -----------------------------------------------------
	
	public void acceptProblem(IProblem problem) {
		if (problem.isWarning())
			return;
			
		reset();
		fStatus.addFatalError(RefactoringCoreMessages.getFormattedString("StatementAnalyzer.compilation_error",  //$NON-NLS-1$
								new Object[]{new Integer(problem.getSourceLineNumber()), problem.getMessage()}));
		fCompileErrorFound= true;
	}
	
	private int getLineNumber(AstNode node){
		Assert.isNotNull(fLineSeparatorPositions);
		return ProblemHandler.searchLineNumber(fLineSeparatorPositions, node.sourceStart);
	}
	
	//--- node management ---------------------------------------------------------
	
	private void reset() {
		fTopNodes= null;
		fParentsOfFirstSelectedNode= null;
		fParentOfFirstSelectedNode= null;
		fFirstSelectedNode= null;
		fEnclosingMethod= null;
		fNeedsSemicolon= true;
	}
	
	private void invalidSelection(String message) {
		reset();
		fStatus.addFatalError(message);
	}
	
	private boolean visitNode(AstNode node, Scope scope) {
		return visitRange(node.sourceStart, node.sourceEnd, node, scope);
	}
	
	private boolean visitRange(int start, int end, AstNode node, Scope scope) {
		// The selection lies behind the node.
		if (end < fSelection.start || fSelection.end < start) {
			return false;
		} else if (fSelection.endsIn(start, end)) {
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.ends_middle_of_statement")); //$NON-NLS-1$
			return false;
		} else if (fSelection.covers(start, end)) {
			if (firstCoveredNode()) {
				startFound(node, scope);
			} else {
				trackLastSelectedNode(node);
			}
			return fTraverseSelectedNodes;
		} else if (fSelection.coveredBy(start, end)) {
			return true;
		}
		return false;
	}
	
	private boolean firstCoveredNode() {
		return fTopNodes == null;
	}
	
	private void startFound(AstNode node, Scope scope) {
		fEnclosingScope= scope;
		fParentOfFirstSelectedNode= getParent();
		fParentsOfFirstSelectedNode= new ArrayList(fParentTracker.getParents());
		fFirstSelectedNode= node;

		fTopNodes= new ArrayList(5);
		fTopNodes.add(node);
		
		fNeedsSemicolon= true;
	}
	
	private void trackLastSelectedNode(AstNode node) {
		fNeedsSemicolon= true;
		checkParent();
		if (fParentOfFirstSelectedNode == getParent()) {
			fTopNodes.add(node);
		}
	}
	
	private void checkParent() {
		List parents= fParentTracker.getParents();
		for (int i= parents.size() - 1; i >= 0; i--) {
			AstNode node= (AstNode)parents.get(i);
			if (node == fParentOfFirstSelectedNode)
				return;
		}
		invalidSelection("Not all selected top level statements belong to the same parent.");
	}
	
	//---- General visit* methods --------------------------------------------------
	
	private boolean visitAssignment(Assignment assignment, BlockScope scope) {
		if (!visitNode(assignment, scope))
			return false;
		trackExpressionTypeBinding(assignment, null, scope);
		return true;
	}
	
	private boolean visitAbstractMethodDeclaration(AbstractMethodDeclaration node, Scope scope) {
		return visitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);		
	}
	
	private boolean visitTypeDeclaration(TypeDeclaration node, Scope scope) {
		return visitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);
	}
	
	//--- Expression / Condition handling -----------------------------------------
	
	private boolean visitLiteral(Literal literal, BlockScope scope) {
		if (!visitNode(literal, scope))
			return false;
		trackExpressionTypeBinding(literal, literal.literalType(scope), scope);
		return true;
	}
	
	private boolean visitBinaryExpression(BinaryExpression node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		trackExpressionTypeBinding(node, getTypeBinding(node, scope), scope);
		return true;	
	}
	
	private TypeBinding getTypeBinding(Statement statement, BlockScope scope) {
		int returnType= statement.bits & statement.ReturnTypeIDMASK;
		switch (returnType) {
			case TypeIds.T_boolean :
				return BaseTypes.BooleanBinding;
			case TypeIds.T_byte :
				return BaseTypes.ByteBinding;
			case TypeIds.T_char :
				return BaseTypes.CharBinding;
			case TypeIds.T_double :
				return BaseTypes.DoubleBinding;
			case TypeIds.T_float :
				return BaseTypes.FloatBinding;
			case TypeIds.T_int :
				return BaseTypes.IntBinding;
			case TypeIds.T_long :
				return BaseTypes.LongBinding;
			case TypeIds.T_String :
				return scope.getJavaLangString();
			default:
				return null;
		}
	}
	
	private void trackExpressionTypeBinding(Expression expression, TypeBinding binding, Scope scope) {
		if (expression == fFirstSelectedNode) {
			fExpressionTypeBinding= binding;
			AstNode parent= getParent();
			fExpressionIsPartOfOperator= expression instanceof Assignment && parent instanceof Expression;
			// Check if the type is visible in the current scope.
			if (binding != null) {
				binding= binding instanceof ArrayBinding ? ((ArrayBinding)binding).leafComponentType : binding;
				TypeBinding checkBinding= scope.getType(binding.sourceName());
				fExpressionTypeIsVisible= checkBinding == binding;
			} else {
				fExpressionTypeIsVisible= false;
			}
		}
	}

	//---- Compilation Unit -------------------------------------------------------
	
	public boolean visit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
		fLineSeparatorPositions= node.compilationResult.lineSeparatorPositions;
		return visitNode(node, scope);
	}
	
	public boolean visit(ImportReference node, CompilationUnitScope scope) {
		return visitNode(node, scope);
	}
	
	public boolean visit(TypeDeclaration node, CompilationUnitScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	//---- Type -------------------------------------------------------------------
	
	public boolean visit(Clinit clinit, ClassScope scope) {
		return visitAbstractMethodDeclaration(clinit, scope);
	}
	
	public boolean visit(TypeDeclaration node, ClassScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public boolean visit(MemberTypeDeclaration node, ClassScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public boolean visit(FieldDeclaration node, MethodScope scope) {
		return visitNode(node, scope);
	}
	
	public boolean visit(Initializer node, MethodScope scope) {
		return visitNode(node, scope);
	}
	
	public boolean visit(ConstructorDeclaration node, ClassScope scope) {
		return visitAbstractMethodDeclaration(node, scope);
	}
	
	public boolean visit(MethodDeclaration node, ClassScope scope) {
		return visitAbstractMethodDeclaration(node, scope);
	}
	
	public boolean visit(SingleTypeReference node, ClassScope scope) {
		return visitNode(node, scope);
	}
	
	public boolean visit(QualifiedTypeReference node, ClassScope scope) {
		return visitNode(node, scope);
	}
	
	public boolean visit(ArrayTypeReference node, ClassScope scope) {
		return visitNode(node, scope);
	}
	
	public boolean visit(ArrayQualifiedTypeReference node, ClassScope scope) {
		return visitNode(node, scope);
	}
	
	//---- Methods ----------------------------------------------------------------
	
	public boolean visit(LocalTypeDeclaration node, MethodScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public boolean visit(Argument node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	//---- Methods / Block --------------------------------------------------------
	
	public boolean visit(TypeDeclaration node, BlockScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public boolean visit(AnonymousLocalTypeDeclaration node, BlockScope scope) {
		return visitTypeDeclaration(node, scope);
	}
	
	public boolean visit(LocalDeclaration node, BlockScope scope) {
		// XXX: 1GF089K: ITPJCORE:WIN2000 - AbstractLocalDeclaration.declarationSourceStart includes preceeding comment
		AstNode lastNode= getLastSelectedNode();
		int start= node.declarationSourceStart;
		if (node != null)
			start= Math.max(start, lastNode.sourceEnd + 1);
		int pos= fBuffer.indexOfStatementCharacter(start);		
		node.declarationSourceStart= pos;
		// XXX: end
		
		return visitRange(node.declarationSourceStart, node.declarationSourceEnd, 
			node, scope);
	}

	public boolean visit(FieldReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(ArrayReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(ArrayTypeReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(ArrayQualifiedTypeReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(SingleTypeReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(QualifiedTypeReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(QualifiedNameReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(QualifiedSuperReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(QualifiedThisReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(SingleNameReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(SuperReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(ThisReference node, BlockScope scope) {
		return visitNode(node, scope);
	}

	//---- Statements -------------------------------------------------------------
	
	public boolean visit(AllocationExpression node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		trackExpressionTypeBinding(node, node.type.binding, scope);
		return true;
	}
	
	public boolean visit(AND_AND_Expression node, BlockScope scope) {
		return visitBinaryExpression(node, scope);
	}
	
	public boolean visit(ArrayAllocationExpression node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		trackExpressionTypeBinding(node, 
			scope.createArray(node.type.binding, node.dimensions.length),
			scope);
		return true;
	}
	
	public boolean visit(ArrayInitializer node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public boolean visit(AssertStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(Assignment node, BlockScope scope) {
		return visitAssignment(node, scope);
	}

	public boolean visit(BinaryExpression node, BlockScope scope) {
		return visitBinaryExpression(node, scope);
	}

	public boolean visit(Block block, BlockScope scope) {
		boolean result= visitNode(block, scope);
		
		if (fSelection.intersects(block))
			reset();
		
		return result;
	}

	public void endVisit(Block block, BlockScope scope) {
		if (fSelection.covers(block))
			fNeedsSemicolon= false;
	}

	public boolean visit(Break node, BlockScope scope) {
		if (node.label == null) {
			node.sourceEnd= node.sourceStart + BREAK_LENGTH - 1;
		}
		return visitNode(node, scope);
	}

	public boolean visit(Case node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(CastExpression node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		trackExpressionTypeBinding(node, node.castTb, scope);
		return true;
	}

	public boolean visit(CharLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(ClassLiteralAccess node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(CompoundAssignment node, BlockScope scope) {
		return visitAssignment(node, scope);
	}
	
	public boolean visit(ConditionalExpression node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(Continue node, BlockScope scope) {
		if (node.label == null) {
			node.sourceEnd= node.sourceStart + CONTINUE_LENGTH - 1;
		}
		return visitNode(node, scope);
	}

	public boolean visit(DefaultCase node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(DoStatement node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		
		int actionStart= fBuffer.indexAfter(Scanner.TokenNamedo, node.sourceStart);
		if (fSelection.start == actionStart) {
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.after_do_keyword")); //$NON-NLS-1$
			return false;
		}
		
		return true;
	}
	
	public boolean visit(DoubleLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(EqualExpression node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		trackExpressionTypeBinding(node, BaseTypes.BooleanBinding, scope);
		return true;	
	}

	public boolean visit(ExplicitConstructorCall node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(ExtendedStringLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(FalseLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(FloatLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(ForStatement node, BlockScope scope) {
		// XXX: 1GIT8SA: ITPJCORE:WIN2000 - AST: wrong sourceEnd if action is Block
		if (node.action instanceof Block)
			node.sourceEnd= node.action.sourceEnd;

		return visitNode(node, scope);			
	}
	
	public boolean visit(IfStatement node, BlockScope scope) {
		// XXX: 1GIT8SA: ITPJCORE:WIN2000 - AST: wrong sourceEnd if action is Block
		if (node.elseStatement != null) {
			if (node.elseStatement instanceof Block)
				node.sourceEnd= node.elseStatement.sourceEnd;
		} else {
			if (node.thenStatement instanceof Block)
				node.sourceEnd= node.thenStatement.sourceEnd;
		}
			
		return visitNode(node, scope);
	}
		
	public boolean visit(InstanceOfExpression node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		trackExpressionTypeBinding(node, BaseTypes.BooleanBinding, scope);
		return true;
	}

	public boolean visit(IntLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(LabeledStatement node, BlockScope scope) {
		// XXX: 1GIT8SA: ITPJCORE:WIN2000 - AST: wrong sourceEnd if action is Block
		if (node.statement instanceof Block)
			node.sourceEnd= node.statement.sourceEnd;
			
		return visitNode(node, scope);
	}

	public boolean visit(LongLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(MessageSend node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		if (node.binding.returnType != BaseTypeBinding.VoidBinding)
			trackExpressionTypeBinding(node, node.binding.returnType, scope);
		return true;
	}

	public boolean visit(NullLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(OR_OR_Expression node, BlockScope scope) {
		return visitBinaryExpression(node, scope);
	}

	public boolean visit(PostfixExpression node, BlockScope scope) {
		boolean result= visitNode(node, scope);
		trackExpressionTypeBinding(node, null, scope);
		return result;
	}

	public boolean visit(PrefixExpression node, BlockScope scope) {
		boolean result= visitNode(node, scope);
		trackExpressionTypeBinding(node, null, scope);
		return result;
	}

	public boolean visit(QualifiedAllocationExpression node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		trackExpressionTypeBinding(node, node.type.binding, scope);
		return true;
	}

	public boolean visit(ReturnStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(StringLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(SwitchStatement node, BlockScope scope) {
		// Include "}" into switch statement
		node.sourceEnd++;
		return visitNode(node, scope);
	}

	public void endVisit(SwitchStatement node, BlockScope scope) {
		if (fSelection.covers(node)) {
			fNeedsSemicolon= false;
		} else if (fSelection.end < node.sourceStart) {
			for (Iterator iter= fTopNodes.iterator(); iter.hasNext(); ) {
				AstNode topNode= (AstNode)iter.next();
				if (topNode == node.defaultCase || contains(node.cases, topNode)) {
					invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.switch_statement")); //$NON-NLS-1$
					break;
				}
			}
		}
	}

	public boolean visit(SynchronizedStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(SynchronizedStatement node, BlockScope scope) {
		if (fSelection.covers(node)) {
			fNeedsSemicolon= false;
			if (fFirstSelectedNode == node.block) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.synchronized_statement")); //$NON-NLS-1$
			}
		}
	}

	public boolean visit(ThrowStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public boolean visit(TrueLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(TryStatement node, BlockScope scope) {
		// Include "}" into sourceEnd;
		node.sourceEnd++;
		
		return visitNode(node, scope);
	}
	public void endVisit(TryStatement node, BlockScope scope) {
		if (fSelection.covers(node)) {
			fNeedsSemicolon= false;
		} else if (fSelection.end < node.sourceStart) {
			if (fFirstSelectedNode == node.tryBlock || fFirstSelectedNode == node.finallyBlock ||
					contains(node.catchBlocks, fFirstSelectedNode))
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.try_statement"));
		}
	}
		
	public boolean visit(UnaryExpression node, BlockScope scope) {
		if (!visitNode(node, scope))
			return false;
		trackExpressionTypeBinding(node, getTypeBinding(node, scope), scope);
		return true;
	}

	public boolean visit(WhileStatement node, BlockScope scope) {
		// XXX: 1GIT8SA: ITPJCORE:WIN2000 - AST: wrong sourceEnd if action is Block
		if (node.action instanceof Block)
			node.sourceEnd= node.action.sourceEnd;
			
		return visitNode(node, scope);
	}

	//--- general helpers ---------------------------------------------------------
	
	private boolean contains(Statement[] statements, AstNode node) {
		if (statements == null)
			return false;
		for (int i= 0; i < statements.length; i++) {
			if (statements[i] == node)
				return true;
		}
		return false;
	}
	
	private void checkSelection() {
		// We don't have a selected node;
		if (fTopNodes == null || fTopNodes.isEmpty())
			return;
		int pos= fBuffer.indexOfStatementCharacter(fSelection.start);
		AstNode node= (AstNode)fTopNodes.get(0);
		if (ASTs.getSourceStart(node) != pos) {
			invalidSelection("Beginning of selection contains characters that do not belong to a statement.");
			return;
		}	
		
		for (int i= 0; i < fTopNodes.size() - 1; i++) {
			AstNode first= (AstNode)fTopNodes.get(i);
			AstNode second= (AstNode)fTopNodes.get(i + 1);
			pos= fBuffer.indexOfStatementCharacter(ASTs.getSourceEnd(first) + 1);
			if (pos != ASTs.getSourceStart(second)) {
				invalidSelection("Selected statements do not belong to the same category. For example, a while statement's expression and action are selected.");
				return;
			}
		}
		node= getLastSelectedNode();	
		pos= fBuffer.indexOfStatementCharacter(ASTs.getSourceEnd(node) + 1);
		if (pos != -1 && pos <= fSelection.end)
			invalidSelection("End of selection contains characters that do not belong to a statement.");
	}		
}