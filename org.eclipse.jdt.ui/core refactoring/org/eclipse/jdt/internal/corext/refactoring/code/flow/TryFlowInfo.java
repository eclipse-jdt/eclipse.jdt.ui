/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

class TryFlowInfo extends FlowInfo {
	
	public TryFlowInfo() {
		super();
	}
	
	public void mergeTry(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
		assign(info);
	}
	
	public void mergeCatch(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
		mergeConditional(info, context);
	}
	
	public void mergeFinally(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
		mergeSequential(info, context);
	}
}

