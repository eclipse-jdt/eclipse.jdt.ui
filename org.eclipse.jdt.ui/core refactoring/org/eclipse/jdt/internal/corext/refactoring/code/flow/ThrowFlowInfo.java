/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ITypeBinding;

class ThrowFlowInfo extends FlowInfo {
	
	public ThrowFlowInfo() {
		super(THROW);
	}
	
	public void merge(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
		assignAccessMode(info);
	}
	
	public void mergeException(ITypeBinding exception, FlowContext context) {
		if (exception != null && context.isExceptionCaught(exception))
			addException(exception);
	}
}


