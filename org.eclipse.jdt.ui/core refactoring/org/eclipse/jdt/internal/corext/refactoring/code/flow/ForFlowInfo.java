/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

class ForFlowInfo extends FlowInfo {

	public void mergeInitializer(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeAccessModeSequential(info, context);
	}
	
	public void mergeCondition(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeAccessModeSequential(info, context);
	}
	
	public void mergeIncrement(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
		info.mergeEmptyCondition(context);
		mergeAccessModeSequential(info, context);
	}
	
	public void mergeAction(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		
		if (!context.isLoopReentranceMode())
			info.mergeEmptyCondition(context);
		
		mergeSequential(info, context);		
	}
}

