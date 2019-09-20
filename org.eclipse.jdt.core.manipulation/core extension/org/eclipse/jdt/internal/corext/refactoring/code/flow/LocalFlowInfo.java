/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
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

	public LocalFlowInfo(LocalFlowInfo info, int localAccessMode, FlowContext context) {
		super(NO_RETURN);
		fVariableId= info.fVariableId;
		if (context.considerAccessMode()) {
			createAccessModeArray(context);
			fAccessModes[fVariableId - context.getStartingIndex()]= localAccessMode;
		}
	}

	public void setWriteAccess(FlowContext context) {
		if (context.considerAccessMode()) {
			fAccessModes[fVariableId - context.getStartingIndex()]= FlowInfo.WRITE;
		}
	}
}

