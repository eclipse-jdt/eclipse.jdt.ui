/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;

public class InOutFlowAnalyzer extends FlowAnalyzer {
	
	private Selection fSelection;
	
	public InOutFlowAnalyzer(FlowContext context, Selection selection) {
		super(context);
		fSelection= selection;
	}
	
	public FlowInfo analyse(AstNode[] selectedNodes, BlockScope scope) {
		FlowContext context= getFlowContext();
		GenericSequentialFlowInfo result= createSequential();
		for (int i= 0; i < selectedNodes.length; i++) {
			AstNode node= selectedNodes[i];
			if (scope instanceof MethodScope)
				node.traverse(this, (MethodScope)scope);
			else
				node.traverse(this, scope);
			result.merge(getFlowInfo(node), context);
		}
		return result;
	}
	
	protected boolean traverseRange(int start, int end) {
		// we are only traversing the selected nodes.
		return true;
	}
	
	protected boolean createReturnFlowInfo(ReturnStatement node) {
		// we are only traversing selected nodes.
		return true;
	}
	
	public void endVisit(Block node, BlockScope scope) {
		super.endVisit(node, scope);
		clearAccessMode(accessFlowInfo(node), node.statements);
	}
	
	public void endVisit(ForStatement node, BlockScope scope) {
		super.endVisit(node, scope);
		clearAccessMode(accessFlowInfo(node), node.initializations);
	}
	
	public void endVisit(ConstructorDeclaration node, ClassScope scope) {
		super.endVisit(node, scope);
		FlowInfo info= accessFlowInfo(node);
		clearAccessMode(info, node.statements);
		clearAccessMode(info, node.arguments);
	}

	public void endVisit(MethodDeclaration node, ClassScope scope) {
		super.endVisit(node, scope);
		FlowInfo info= accessFlowInfo(node);
		clearAccessMode(info, node.statements);
		clearAccessMode(info, node.arguments);
	}
	
	private void clearAccessMode(FlowInfo info, Statement[] statements) {
		if (statements == null || info == null)
			return;
		for (int i= 0; i < statements.length; i++) {
			if (statements[i] instanceof LocalDeclaration) {
				LocalDeclaration declaration= (LocalDeclaration)statements[i];
				info.clearAccessMode(declaration.binding, fFlowContext);
			}
		}
	}				
}

