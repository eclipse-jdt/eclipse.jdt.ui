/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;



class GenericConditionalFlowInfo extends FlowInfo {
	
	public GenericConditionalFlowInfo() {
		super(UNDEFINED);
	}
	
	public void merge(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
		mergeConditional(info, context);
	}
	
	public void mergeAccessMode(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
		mergeAccessModeConditional(info, context);
	}
}


