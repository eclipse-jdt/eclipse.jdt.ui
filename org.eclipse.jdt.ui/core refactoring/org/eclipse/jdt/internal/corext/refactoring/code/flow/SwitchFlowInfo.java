/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

class SwitchFlowInfo extends FlowInfo {
	private GenericConditionalFlowInfo fCases;
	private boolean fHasNullCaseInfo;
	
	public SwitchFlowInfo() {
		fCases= new GenericConditionalFlowInfo();
	}
	
	public void mergeTest(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeSequential(info, context);
	}
	
	public void mergeCase(FlowInfo info, FlowContext context) {
		if (info == null) {
			fHasNullCaseInfo= true;
			return;
		}
		fCases.mergeConditional(info, context);
	}
	
	public void mergeDefault(boolean defaultCaseExists, FlowContext context) {
		if (!defaultCaseExists || fHasNullCaseInfo)
			fCases.mergeEmptyCondition(context);
		mergeSequential(fCases, context);
	}
}

