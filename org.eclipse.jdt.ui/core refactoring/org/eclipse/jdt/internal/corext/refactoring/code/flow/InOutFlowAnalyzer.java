/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;

public class InOutFlowAnalyzer extends FlowAnalyzer {
	
	private Selection fSelection;
	
	public InOutFlowAnalyzer(FlowContext context, Selection selection) {
		super(context);
		fSelection= selection;
	}
	
	public FlowInfo analyse(List selectedNodes, BlockScope scope) {
		FlowContext context= getFlowContext();
		GenericSequentialFlowInfo result= createSequential();
		for (Iterator iter= selectedNodes.iterator(); iter.hasNext(); ) {
			AstNode node= (AstNode)iter.next();
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
}

