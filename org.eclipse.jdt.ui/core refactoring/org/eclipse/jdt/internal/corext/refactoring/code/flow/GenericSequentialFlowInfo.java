/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;



class GenericSequentialFlowInfo extends FlowInfo {
	
	public GenericSequentialFlowInfo() {
		super(NO_RETURN);
	}
	
	public void merge(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeSequential(info, context);
	}
	
	public void mergeAccessMode(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeAccessModeSequential(info, context);
	}
	
}


