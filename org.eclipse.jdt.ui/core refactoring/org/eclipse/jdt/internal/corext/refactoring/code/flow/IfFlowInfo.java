/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

class IfFlowInfo extends FlowInfo {

	public void mergeCondition(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeAccessModeSequential(info, context);
	}
	
	public void merge(FlowInfo thenPart, FlowInfo elsePart, FlowContext context) {
		if (thenPart == null && elsePart == null)
			return;
		
		GenericConditionalFlowInfo cond= new GenericConditionalFlowInfo();
		if (thenPart != null)
			cond.merge(thenPart, context);
			
		if (elsePart != null)
			cond.merge(elsePart, context);
			
		if (thenPart == null || elsePart == null)
			cond.mergeEmptyCondition(context);
			
		mergeSequential(cond, context);
	}
}

