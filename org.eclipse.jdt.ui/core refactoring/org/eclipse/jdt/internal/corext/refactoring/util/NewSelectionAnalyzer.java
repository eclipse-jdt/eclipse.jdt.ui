/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class NewSelectionAnalyzer extends GenericVisitor {

	// The buffer containing the source code.
	protected ExtendedBuffer fBuffer;
	
	// Selection state.
	protected Selection fSelection;
	private boolean fTraverseSelectedNodes;

	// Error handling	
	protected RefactoringStatus fStatus= new RefactoringStatus();

	// The Scope that encloses the selection.	
	protected Scope fEnclosingScope;
	
	// Selected nodes
	protected List fSelectedNodes;
	protected List fParentsOfFirstSelectedNode;
	protected AstNode fParentOfFirstSelectedNode;
	protected AstNode fFirstSelectedNode;
	protected AstNode fLastSelectedNode;
	
	// Type binding of the selected expression
	private TypeBinding fExpressionTypeBinding;
	private boolean fExpressionIsPartOfOperator;
	private boolean fExpressionTypeIsVisible;
	
	private static final int BREAK_LENGTH= "break".length(); //$NON-NLS-1$
	private static final int CONTINUE_LENGTH= "continue".length(); //$NON-NLS-1$
		 
	public NewSelectionAnalyzer(ExtendedBuffer buffer, Selection selection) {
		Assert.isNotNull(buffer);
		Assert.isNotNull(selection);
		fBuffer= buffer;
		fSelection= selection;
	}

	/**
	 * Returns the refactoring status object containing information about correctness
	 * of the selection.
	 * 
	 * @return the status containing information about the correctness of the selection.
	 */
	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	/**
	 * Returns the selected nodes. The list only contains the top nodes. Any
	 * child notes most be accessed by traversing the returned top nodes.
	 * 
	 * @return an array containing the selected top nodes. Returns <code>null</code>
	 *  if there aren't any selected nodes.
	 */
	public AstNode[] getSelectedNodes() {
		if (fSelectedNodes == null)
			return null;
		return (AstNode[])fSelectedNodes.toArray(new AstNode[fSelectedNodes.size()]);
	}
		
	public boolean hasSelectedNodes() {
		return fSelectedNodes != null && fSelectedNodes.size() > 0;
	}
		
	/**
	 * Returns the parents of the first selected node. The parents are order from the top
	 * most parent down to the direct parent of the first selected node. That means <code>
	 * result[0]</code> is a <code>CompilationUnitDeclaration</code> and <code>
	 * result[result.length - 1]</code> the direct parent of the first selected node.
	 * 
	 * @returns the parents of the first selected node or <code>null</code> if no node
	 *  is selected. Returns <code>null</code> if there aren't any selected nodes.
	 */
	public AstNode[] getParents() {
		if (fParentsOfFirstSelectedNode == null)
			return null;
		return (AstNode[])fParentsOfFirstSelectedNode.toArray(
			new AstNode[fParentsOfFirstSelectedNode.size()]);	
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
	 * Returns the first selected node.
	 * 
	 * @return the first selected node
	 */
	public AstNode getFirstSelectedNode() {
		if (fSelectedNodes == null || fSelectedNodes.size() == 0)
			return null;
		return (AstNode)fSelectedNodes.get(0);
	}
	
	/**
	 * Returns the start position of the first selected node. Returns -1 if there
	 * isn't any selected node.
	 * 
	 * @return the start position of the first selected node or -1
	 */
	public int getStartOfFirstSelectedNode() {
		AstNode node= getFirstSelectedNode();
		if (node == null)
			return -1;
		return ASTUtil.getSourceStart(node);
	}
	
	/**
	 * Returns the last selected node.
	 */
	public AstNode getLastSelectedNode() {
		if (fSelectedNodes == null || fSelectedNodes.size() == 0)
			return null;
			
		return (AstNode)fSelectedNodes.get(fSelectedNodes.size() - 1);
	}

	/**
	 * Returns the end position of the last selected node. Returns -1 if there
	 * isn't any selected node.
	 * 
	 * @return the end position of the last selected node or -1
	 */
	public int getEndOfLastSelectedNode() {
		AstNode node= getLastSelectedNode();
		if (node == null)
			return -1;
		return ASTUtil.getSourceEnd(node);
	}
	
	/**
	 * Returns <code>true</code> if the selection covers a single expression. Otherwise
	 * <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if selection covers expression; otherwise <code>false</code>.
	 */
	public boolean isExpressionSelected() {
		if (fSelectedNodes == null || fSelectedNodes.size() != 1)
			return false;
		return (fSelectedNodes.get(0) instanceof Expression);
	}
	
	/**
	 * Returns the buffer this selection analyzer is working on.
	 * 
	 * @return the buffer containing the compilation unit's content
	 */
	protected ExtendedBuffer getBuffer() {
		return fBuffer;
	}

	/**
	 * Returns the analyzer's selection.
	 * 
	 * @return the analyzer's selection
	 */
	public Selection getSelection() {
		return fSelection;
	}
	
	/* XXX: check if we really need these methods */
		
	public boolean returnsExpressionValue() {
		return (fExpressionTypeBinding != null && fExpressionTypeBinding != BaseTypes.VoidBinding);
	}
	
	public TypeBinding getExpressionTypeBinding() {
		return fExpressionTypeBinding;
	}
	
	public boolean isExpressionTypeVisible() {
		return fExpressionTypeIsVisible;
	}
	
	protected boolean isExpressionPartOfOperator() {
		return fExpressionIsPartOfOperator;
	}
	
	//--- general helpers ---------------------------------------------------------
	
	protected boolean contains(Statement[] statements, AstNode node) {
		if (statements == null)
			return false;
		for (int i= 0; i < statements.length; i++) {
			if (statements[i] == node)
				return true;
		}
		return false;
	}
	
	protected boolean isSelected(AstNode node) {
		if (fLastSelectedNode == node)
			return true;
		return fSelection.covers(ASTUtil.getSourceStart(node), ASTUtil.getSourceEnd(node));
	}
	
	//--- node management ---------------------------------------------------------
	
	protected boolean visitRange(int start, int end, AstNode node, Scope scope) {
		// The selection lies behind the node.
		if (end < fSelection.start || fSelection.end < start) {
			return false;
		} else if (fSelection.covers(start, end)) {
			if (isFirstNode()) {
				handleFirstSelectedNode(node, scope);
			} else {
				handleNextSelectedNode(node);
			}
			return fTraverseSelectedNodes;
		} else if (fSelection.coveredBy(start, end)) {
			return true;
		} else if (fSelection.endsIn(start, end)) {
			handleSelectionEndsIn(node);
			return false;
		}
		// There is a possibility that the user has selected trailing semicolons that don't belong
		// to the statement. So dive into it to check if sub nodes are fully covered.
		return true;
	}
	
	private boolean isFirstNode() {
		return fSelectedNodes == null;
	}
	
	protected void reset() {
		fSelectedNodes= null;
		fParentsOfFirstSelectedNode= null;
		fParentOfFirstSelectedNode= null;
		fFirstSelectedNode= null;
		fLastSelectedNode= null;
	}
	
	protected void invalidSelection(String message) {
		reset();
		fStatus.addFatalError(message);
	}
	
	protected void handleFirstSelectedNode(AstNode node, Scope scope) {
		fEnclosingScope= scope;
		fParentOfFirstSelectedNode= internalGetParent();
		fParentsOfFirstSelectedNode= new ArrayList(internalGetParents());
		fFirstSelectedNode= node;
		fLastSelectedNode= node;

		fSelectedNodes= new ArrayList(5);
		fSelectedNodes.add(node);
	}
	
	protected void handleNextSelectedNode(AstNode node) {
		if (fParentOfFirstSelectedNode == internalGetParent()) {
			fSelectedNodes.add(node);
			fLastSelectedNode= node;
		}
	}

	protected void handleSelectionEndsIn(AstNode node) {
	}
		
	//---- General visit* methods --------------------------------------------------
	
	protected boolean visitAssignment(Assignment assignment, BlockScope scope) {
		boolean result= visitNode(assignment, scope);
		trackExpressionTypeBinding(assignment, null, scope);
		return result;
	}
	
	//--- Expression / Condition handling -----------------------------------------
	
	private boolean visitLiteral(Literal literal, BlockScope scope) {
		boolean result= visitNode(literal, scope);
		trackExpressionTypeBinding(literal, literal.literalType(scope), scope);
		return result;
	}
	
	private boolean visitBinaryExpression(BinaryExpression node, BlockScope scope) {
		boolean result= visitNode(node, scope);
		trackExpressionTypeBinding(node, getTypeBinding(node, scope), scope);
		return result;	
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
			AstNode parent= internalGetParent();
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
	
	//---- Methods / Block --------------------------------------------------------
		
	public boolean visit(LocalDeclaration node, BlockScope scope) {
		// XXX: 1GF089K: ITPJCORE:WIN2000 - AbstractLocalDeclaration.declarationSourceStart includes preceeding comment
		AstNode lastNode= getLastSelectedNode();
		int start= node.declarationSourceStart;
		if (lastNode != null && lastNode.sourceEnd < node.declarationSourceEnd)
			start= Math.max(start, lastNode.sourceEnd + 1);
		int pos= fBuffer.indexOfStatementCharacter(start);		
		node.declarationSourceStart= pos;
		// XXX: end
		
		return super.visit(node, scope);
	}

	//---- Statements -------------------------------------------------------------
	
	public boolean visit(AllocationExpression node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		trackExpressionTypeBinding(node, node.type.binding, scope);
		return result;
	}
	
	public boolean visit(AND_AND_Expression node, BlockScope scope) {
		return visitBinaryExpression(node, scope);
	}
	
	public boolean visit(ArrayAllocationExpression node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		trackExpressionTypeBinding(node, 
			scope.createArray(node.type.binding, node.dimensions.length),
			scope);
		return result;
	}
	
	public boolean visit(Assignment node, BlockScope scope) {
		return visitAssignment(node, scope);
	}

	public boolean visit(BinaryExpression node, BlockScope scope) {
		return visitBinaryExpression(node, scope);
	}
	
	public boolean visit(Break node, BlockScope scope) {
		// XXX
		if (node.label == null) {
			node.sourceEnd= node.sourceStart + BREAK_LENGTH - 1;
		}
		return visitNode(node, scope);
	}

	public boolean visit(CastExpression node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		trackExpressionTypeBinding(node, node.castTb, scope);
		return result;
	}

	public boolean visit(CharLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}
	
	public boolean visit(CompoundAssignment node, BlockScope scope) {
		return visitAssignment(node, scope);
	}
					
	public boolean visit(Continue node, BlockScope scope) {
		// XXX
		if (node.label == null) {
			node.sourceEnd= node.sourceStart + CONTINUE_LENGTH - 1;
		}
		return visitNode(node, scope);
	}

	public boolean visit(DoubleLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(EqualExpression node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		trackExpressionTypeBinding(node, BaseTypes.BooleanBinding, scope);
		return result;	
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

	public boolean visit(InstanceOfExpression node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		trackExpressionTypeBinding(node, BaseTypes.BooleanBinding, scope);
		return result;
	}

	public boolean visit(IntLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(LongLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(MessageSend node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		if (node.binding != null && node.binding.returnType != BaseTypeBinding.VoidBinding)
			trackExpressionTypeBinding(node, node.binding.returnType, scope);
		return result;
	}

	public boolean visit(NullLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(OR_OR_Expression node, BlockScope scope) {
		return visitBinaryExpression(node, scope);
	}

	public boolean visit(PostfixExpression node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		trackExpressionTypeBinding(node, null, scope);
		return result;
	}

	public boolean visit(PrefixExpression node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		trackExpressionTypeBinding(node, null, scope);
		return result;
	}

	public boolean visit(QualifiedAllocationExpression node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		trackExpressionTypeBinding(node, node.type.binding, scope);
		return true;
	}

	public boolean visit(TrueLiteral node, BlockScope scope) {
		return visitLiteral(node, scope);
	}

	public boolean visit(UnaryExpression node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		trackExpressionTypeBinding(node, getTypeBinding(node, scope), scope);
		return result;
	}
}
