/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

class MessageSendFlowInfo extends FlowInfo {

	public MessageSendFlowInfo() {
		super(NO_RETURN);
	}
	
	public void mergeArgument(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeSequential(info, context);
	}
	
	public void mergeReceiver(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
		mergeSequential(info, context);
	}
	
	public void mergeExceptions(MethodBinding binding, FlowContext context) {
		if (binding == null)
			return;
		TypeBinding[] exceptions= binding.thrownExceptions;
		if (exceptions == null)
			return;
		for (int i= 0; i < exceptions.length; i++) {
			TypeBinding exception= exceptions[i];
			if (context.isExceptionCaught(exception))
				addException(exception);
		}
	}
}
