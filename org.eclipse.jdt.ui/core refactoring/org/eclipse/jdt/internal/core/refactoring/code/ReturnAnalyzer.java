/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BranchStatement;
import org.eclipse.jdt.internal.compiler.ast.Break;
import org.eclipse.jdt.internal.compiler.ast.Case;
import org.eclipse.jdt.internal.compiler.ast.DefaultCase;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

/**
 * Analyses return statements.
 */
/* package */ class ReturnAnalyzer {

	static class FlowInfo {
		public static final int NO_RETURN= 		0;
		public static final int SOME_RETURN=	1;
		public static final int ALL_RETURN=		2;
		public static final String UNLABELED=	"@unlabeled"; //$NON-NLS-1$
		public int mode;
		private HashMap fBranches;
		public FlowInfo(int m) {
			mode= m;
		}
		public FlowInfo(char[] label) {
			fBranches= new HashMap(2);
			String s= makeString(label);
			fBranches.put(s, s);
		}
		public boolean branches() {
			return fBranches != null ? !fBranches.isEmpty() : false;
		}
		public void removeLabel(String label) {
			if (fBranches != null)
				fBranches.remove(label);	
		}
		public void adjust() {
			if (mode != NO_RETURN)
				mode= SOME_RETURN;
		}
		public void merge(FlowInfo info) {
			if (mode != info.mode)
				mode= SOME_RETURN;
				
			if (info.fBranches != null) {
				if (fBranches == null) {
					fBranches= info.fBranches;
				} else {
					Iterator iter= info.fBranches.keySet().iterator();
					while (iter.hasNext()) {
						Object elem= iter.next();
						fBranches.put(elem, elem);
					}
				}
			}
		}
		public boolean isExtractable() {
			return mode != SOME_RETURN && !branches();
		}
		private static String makeString(char[] label) {
			if (label == null)
				return UNLABELED;
			else
				return new String(label);
		}
	}
	
	static abstract class Node {
		public abstract FlowInfo flowAnalysis();
		public void manage(Node node) {
		}
	}

	// Return statement	
	static class ReturnNode extends Node {
		public FlowInfo flowAnalysis() {
			return new FlowInfo(FlowInfo.ALL_RETURN);
		}
	}
	
	// Break and Continue statement.
	static class BranchNode extends Node {
		private FlowInfo fFlowInfo;
		public BranchNode(BranchStatement statement) {
			fFlowInfo= new FlowInfo(statement.label);
		}
		public FlowInfo flowAnalysis() {
			return fFlowInfo;
		}
	}
	
	// Case statement
	static class CaseNode extends Node {
		public FlowInfo flowAnalysis() {
			return null;	// A case node isn't executed. So we don't need flow info.
		}
	}
	
	// Default Case statement
	static class DefaultCaseNode extends CaseNode {
	}
	
	// For and While loop.
	static class LoopNode extends Node {
		protected Node fAction;
		protected String fLabel;
		public LoopNode(String label) {
			fLabel= label;
		}
		public FlowInfo flowAnalysis() {
			if (fAction == null)
				return new FlowInfo(FlowInfo.NO_RETURN);
			
			FlowInfo result= fAction.flowAnalysis();
			result.removeLabel(fLabel);
			result.adjust();
			
			return result;	
		}
		public void manage(Node node) {
			fAction= node;
		}
	}
	
	// Do While loop.
	static class DoWhileNode extends LoopNode {
		public DoWhileNode(String label) {
			super(label);
		}
		public FlowInfo flowAnalysis() {
			if (fAction == null)
				return new FlowInfo(FlowInfo.NO_RETURN);

			FlowInfo result= fAction.flowAnalysis();
			if (result.branches()) {
				result.removeLabel(fLabel);
				result.adjust();
			}
			return result;
		}
	}

	static class IfNode extends Node {
		protected Node fThenNode;
		protected Node fElseNode;
		public IfNode() {
		}
		public void manage(Node node) {
			if (fThenNode == null)
				fThenNode= node;
			else
				fElseNode= node;
		}
		public FlowInfo flowAnalysis() {
			FlowInfo result= null;
			if (fThenNode != null) {
				result= fThenNode.flowAnalysis();
				if (fElseNode != null) {
					result.merge(fElseNode.flowAnalysis());
				} else {
					result.adjust();
				}
			} else {
				result= new FlowInfo(FlowInfo.NO_RETURN);
			}
			return result;
		}
	}
	
	static abstract class CompositeNode extends Node {
		protected List fSubNodes;	
		public CompositeNode() {
			this(5);
		}
		public CompositeNode(int size) {
			fSubNodes= new ArrayList(size);
		}
		public void manage(Node node) {
			fSubNodes.add(node);
		}
		public FlowInfo flowAnalysis() {
			int size= fSubNodes.size();
			if (size == 0)
				return new FlowInfo(FlowInfo.NO_RETURN);
			Node last= (Node)fSubNodes.get(0);
			FlowInfo result= last.flowAnalysis();
			for (int i= 1; i < size; i++) {
				Node node= (Node)fSubNodes.get(i);
				result.merge(node.flowAnalysis());
				last= node;
			}
			if (last != null && last instanceof ReturnNode && !result.branches()) {
				result.mode= FlowInfo.ALL_RETURN;
			}
			return result;
		}
	}
	
	// A Block	
	static class BlockNode extends CompositeNode {
		protected String fLabel;
		BlockNode(String label) {
			fLabel= label;
		}
		public FlowInfo flowAnalysis() {
			FlowInfo result= super.flowAnalysis();
			if (fLabel != null)
				result.removeLabel(fLabel);
			return result;
		}
	}
	
	static class RootNode extends CompositeNode {
	}
			
	static class TryNode extends CompositeNode {
		private Node fFinallyBlock;
		private int fNumberOfBlocks;
		public TryNode(TryStatement statement) {
			this(statement.catchBlocks.length + 1);	// +1 for the try block
		}
		protected TryNode(int blocks) {
			super(blocks);
			fNumberOfBlocks= blocks;
		}
		public void manage(Node node) {
			if (fNumberOfBlocks == fSubNodes.size())
				fFinallyBlock= node;
			else
				super.manage(node);	
		}
		public FlowInfo flowAnalysis() {
			if (fFinallyBlock != null) {
				FlowInfo result= fFinallyBlock.flowAnalysis();
				if (result.mode == FlowInfo.ALL_RETURN)
					return result;
			}
			return super.flowAnalysis();	
		}
	}
	
	static class SwitchNode extends CompositeNode {
		protected String fLabel;
		protected BlockNode fCurrentBlock;
		protected BlockNode fDefaultCaseBlock;
		public SwitchNode(String label) {
			fLabel= label;
		}
		public void manage(Node node) {
			if (node instanceof CaseNode) {
				if (fCurrentBlock == null) {
					fCurrentBlock= new BlockNode(null);
					if (node instanceof DefaultCaseNode)
						fDefaultCaseBlock= fCurrentBlock;
					else
						super.manage(fCurrentBlock);
				}
			} else if (node instanceof BranchNode || node instanceof ReturnNode) {
				fCurrentBlock.manage(node);
				fCurrentBlock= null;
			} else {
				fCurrentBlock.manage(node);
			}
		}
		public FlowInfo flowAnalysis() {
			FlowInfo result= super.flowAnalysis();
			if (result.mode == FlowInfo.ALL_RETURN && !result.branches() && fDefaultCaseBlock == null) {
				result.mode= FlowInfo.SOME_RETURN;
				return result;
			}
			if (fDefaultCaseBlock != null)
				result.merge(fDefaultCaseBlock.flowAnalysis());
			return result;	
		}
	}
		
	public static final int NO=			0;
	public static final int VOID=		1;
	public static final int TYPE=		2;
	
	private StatementAnalyzer fStatementAnalyzer;
	private ReturnStatement fReturnStatement;
	private HashMap fLabels;
	private RootNode fRootNode;
	private Node fCurrentNode;
	private Stack fStack;

	public ReturnAnalyzer(StatementAnalyzer analyzer) {
		fStatementAnalyzer= analyzer;
		fLabels= new HashMap(10);
	}
	
	public void checkActivation(RefactoringStatus status, List topNodes) {
		if (fRootNode == null || getReturnStatementKind() != TYPE)
			return;
			
		FlowInfo info= fRootNode.flowAnalysis();
			
		if (!info.isExtractable()) {
			status.addFatalError(RefactoringCoreMessages.getString("ReturnAnalyzer.execution_flow")); //$NON-NLS-1$
		}	
	}
	
	public int getReturnStatementKind() {
		if (fReturnStatement == null)
			return NO;
		else if (fReturnStatement.expression == null)
			return VOID;
		else
			return TYPE;	
	}
	
	public boolean hasReturnStatement() {
		return getReturnStatementKind() != NO;
	}
	
	public String getReturnType() {
		AbstractMethodDeclaration enclosingMethod= fStatementAnalyzer.getEnclosingMethod();
		// Copy the return type from the enclosing method if possible.
		if (enclosingMethod instanceof MethodDeclaration) {
			TypeReference type= ((MethodDeclaration)enclosingMethod).returnType;
			if (type == null)
				return ""; //$NON-NLS-1$
			else
				return type.toStringExpression(0);
		} else {
			// We have a constructor. So no return type
			return ""; //$NON-NLS-1$
		}
	}
	
	public void startFound() {
		fCurrentNode= fRootNode= new RootNode();
		fStack= new Stack();
	}
	
	public void visit(ReturnStatement statement, BlockScope scope, int mode) {
		if (mode != StatementAnalyzer.SELECTED)
			return;
			
		// If the return statement belongs to a "inner" method (e.g. anonymous type)
		// do nothing.
		if (fStatementAnalyzer.getEnclosingMethod() != getMethod(scope))
			return;
			
		fReturnStatement= statement;

		fCurrentNode.manage(new ReturnNode());			
	}
	
	public void visit(LabeledStatement statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		fLabels.put(statement.statement, new String(statement.label));
	}
	
	public void visit(BranchStatement statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		fCurrentNode.manage(new BranchNode(statement));
	}
	
	public void visit(Case statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		fCurrentNode.manage(new CaseNode());	
	}
	 
	public void visit(DefaultCase statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		fCurrentNode.manage(new DefaultCaseNode());	
	}
	 
	public void visit(Block statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		manageNode(new BlockNode(getBlockLabel(statement)));
	}
	 
	public void endVisit(Block statement, BlockScope scope, int mode) {
		endVisit(mode);
	}
	
	public void visit(IfStatement statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		manageNode(new IfNode());
	}

	public void endVisit(IfStatement statement, BlockScope scope, int mode) {
		endVisit(mode);
	}
	
	public void visit(ForStatement statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		manageNode(new LoopNode(getLoopLabel(statement)));
	}
	
	public void endVisit(ForStatement statement, BlockScope scope, int mode) {
		endVisit(mode);
	}
	
	public void visit(WhileStatement statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		manageNode(new LoopNode(getLoopLabel(statement)));
	}
	
	public void endVisit(WhileStatement statement, BlockScope scope, int mode) {
		endVisit(mode);
	}
	
	public void visit(DoStatement statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		manageNode(new DoWhileNode(getLoopLabel(statement)));
	}
	
	public void endVisit(DoStatement statement, BlockScope scope, int mode) {
		endVisit(mode);
	}
	
	public void visit(TryStatement statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		manageNode(new TryNode(statement));	
	}

	public void endVisit(TryStatement statement, BlockScope scope, int mode) {
		endVisit(mode);
	}
	
	public void visit(SwitchStatement statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		manageNode(new SwitchNode(getLoopLabel(statement)));
	}
	
	public void endVisit(SwitchStatement statement, BlockScope scope, int mode) {
		endVisit(mode);
	}
	
	private String getLoopLabel(Statement statement) {
		String result= (String)fLabels.get(statement);
		if (result == null)
			result= FlowInfo.UNLABELED;
		return result;
	}
	
	private String getBlockLabel(Statement statement) {
		return (String)fLabels.get(statement);
	}
	
	private boolean skipNode(int mode) {
		return mode != StatementAnalyzer.SELECTED;
	}
	
	private void endVisit(int mode) {
		if (skipNode(mode))
			return;
		unmanageNode();
	}
	
	private AbstractMethodDeclaration getMethod(Scope scope) {
		while (!(scope instanceof MethodScope) && scope != null) {
			scope= scope.parent;
		}
		if (scope == null)
			return null;
		MethodScope methodScope= (MethodScope)scope;
		if (methodScope.referenceContext instanceof AbstractMethodDeclaration)
			return (AbstractMethodDeclaration)methodScope.referenceContext;
			
		return null;	
	}
	
	private void manageNode(Node node) {
		fStack.push(fCurrentNode);
		fCurrentNode.manage(node);
		fCurrentNode= node;
	}
	
	private void unmanageNode() {
		if (fCurrentNode == fRootNode)
			return;
		fCurrentNode= (Node)fStack.pop();
	}
}

