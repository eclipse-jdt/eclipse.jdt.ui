/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import org.eclipse.jdt.core.dom.IVariableBinding;

class LocalFlowInfo extends FlowInfo {

	private int fVariableId;

	public LocalFlowInfo(IVariableBinding binding, int localAccessMode, FlowContext context) {
		super(NO_RETURN);
		fVariableId= binding.getVariableId();
		if (context.considerAccessMode()) {
			createAccessModeArray(context);
			fAccessModes[fVariableId - context.getStartingIndex()]= localAccessMode;
			context.manageLocal(binding);
		}
	}
	
	public void setWriteAccess(FlowContext context) {
		if (context.considerAccessMode()) {
			fAccessModes[fVariableId - context.getStartingIndex()]= FlowInfo.WRITE;
		}
	}
}

