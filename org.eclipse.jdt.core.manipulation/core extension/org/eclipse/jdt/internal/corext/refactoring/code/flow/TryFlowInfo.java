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

class TryFlowInfo extends FlowInfo {

	public TryFlowInfo() {
		super();
	}

	public void mergeResources(FlowInfo info, FlowContext context) {
		if (info == null)
			return;

		mergeSequential(info, context);
	}

	public void mergeTry(FlowInfo info, FlowContext context) {
		if (info == null)
			return;

		mergeSequential(info, context);
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

