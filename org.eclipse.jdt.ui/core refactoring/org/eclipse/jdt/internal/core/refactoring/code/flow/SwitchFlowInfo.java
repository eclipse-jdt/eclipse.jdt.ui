/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code.flow;

class SwitchFlowInfo extends FlowInfo {
	private GenericConditionalFlowInfo fCases;
	
	public SwitchFlowInfo() {
		fCases= new GenericConditionalFlowInfo();
	}
	
	public void mergeTest(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeSequential(info, context);
	}
	
	public void mergeCase(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		fCases.mergeConditional(info, context);
	}
	
	public void mergeDefault(boolean defaultCaseExists, FlowContext context) {
		if (!defaultCaseExists)
			fCases.mergeEmptyCondition(context);
		mergeSequential(fCases, context);
	}
}

