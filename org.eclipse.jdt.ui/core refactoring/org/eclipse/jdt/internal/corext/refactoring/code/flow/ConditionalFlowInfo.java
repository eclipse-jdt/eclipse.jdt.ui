/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;



class ConditionalFlowInfo extends FlowInfo {
	
	public ConditionalFlowInfo() {
		super(NO_RETURN);
	}
	
	public void mergeCondition(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeAccessModeSequential(info, context);
	}
	
	public void merge(FlowInfo truePart, FlowInfo falsePart, FlowContext context) {
		if (truePart == null && falsePart == null)
			return;
		
		GenericConditionalFlowInfo cond= new GenericConditionalFlowInfo();
		if (truePart != null)
			cond.mergeAccessMode(truePart, context);
			
		if (falsePart != null)
			cond.mergeAccessMode(falsePart, context);
			
		if (truePart == null || falsePart == null)
			cond.mergeEmptyCondition(context);
			
		mergeAccessModeSequential(cond, context);
	}
}


