/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

class WhileFlowInfo extends FlowInfo {

	public void mergeCondition(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
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

