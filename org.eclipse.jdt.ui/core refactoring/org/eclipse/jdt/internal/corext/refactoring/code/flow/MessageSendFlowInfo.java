/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

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

	public void mergeExceptions(IMethodBinding binding, FlowContext context) {
		if (binding == null)
			return;
		ITypeBinding[] exceptions= binding.getExceptionTypes();
		if (exceptions == null)
			return;
		for (int i= 0; i < exceptions.length; i++) {
			ITypeBinding exception= exceptions[i];
			if (context.isExceptionCaught(exception))
				addException(exception);
		}
	}
}
