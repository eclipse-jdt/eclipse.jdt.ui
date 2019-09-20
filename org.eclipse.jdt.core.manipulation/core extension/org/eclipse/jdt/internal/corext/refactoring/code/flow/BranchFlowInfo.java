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

import java.util.HashSet;

import org.eclipse.jdt.core.dom.SimpleName;

class BranchFlowInfo extends FlowInfo {

	public BranchFlowInfo(SimpleName label, FlowContext context) {
		super(NO_RETURN);
		fBranches= new HashSet<>(2);
		fBranches.add(makeString(label));
	}
}


