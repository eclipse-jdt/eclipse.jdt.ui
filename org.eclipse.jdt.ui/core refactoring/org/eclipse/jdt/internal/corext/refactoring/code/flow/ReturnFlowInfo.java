/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.HashSet;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypes;

class ReturnFlowInfo extends FlowInfo {
	
	public ReturnFlowInfo(ReturnStatement statement) {
		super(
			statement.expressionType == null || statement.expressionType == BaseTypes.VoidBinding
				? VOID_RETURN
				: VALUE_RETURN
		);
	}
	
	public void merge(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
		assignAccessMode(info);
	}	
}


