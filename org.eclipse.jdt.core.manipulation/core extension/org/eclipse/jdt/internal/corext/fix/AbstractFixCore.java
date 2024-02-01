/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - moved to jdt.core.manipulation and modified
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.IStatus;


public abstract class AbstractFixCore implements IProposableFix, ILinkedFixCore {

	private final String fDisplayString;

	protected AbstractFixCore(String displayString) {
		fDisplayString= displayString;
	}

	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return fDisplayString;
	}

	@Override
	public LinkedProposalModelCore getLinkedPositionsCore() {
		return null;
	}

	@Override
	public IStatus getStatus() {
		return null;
	}
}
