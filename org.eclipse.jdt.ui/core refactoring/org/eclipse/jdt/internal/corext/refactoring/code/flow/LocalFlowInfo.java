/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.HashSet;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;

class LocalFlowInfo extends FlowInfo {

	public LocalFlowInfo(LocalVariableBinding binding, int localAccessMode, FlowContext context) {
		super(NO_RETURN);
		if (context.considerAccessMode()) {
			fAccessModes= new int[context.getArrayLength()];
			fAccessModes[binding.id - context.getStartingIndex()]= localAccessMode;
			context.manageLocal(binding);
		}
	}	
}

