/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

class DoWhileFlowInfo extends FlowInfo {

	private boolean fActionBranches;

	public void mergeAction(FlowInfo info, FlowContext context) {
		if (info == null)
			return;

		fActionBranches= info.branches();

		assign(info);
				
		if (fActionBranches && fReturnKind == VALUE_RETURN) {
			fReturnKind= PARTIAL_RETURN;
		}
		
	}
	
	public void mergeCondition(FlowInfo info, FlowContext context) {
		if (fActionBranches || fReturnKind == VALUE_RETURN || fReturnKind == VOID_RETURN || info == null)
			return;
		mergeAccessModeSequential(info, context);
	}
}

