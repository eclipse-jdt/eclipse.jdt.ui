/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code.flow;

import java.util.HashSet;

class BranchFlowInfo extends FlowInfo {
	
	public BranchFlowInfo(char[] label, FlowContext context) {
		super(NO_RETURN);
		if (context.considerExecutionFlow()) {
			fBranches= new HashSet(2);
			fBranches.add(makeString(label));
		}
	}
}

