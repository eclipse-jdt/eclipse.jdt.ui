/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - modified and renamed to ILinkedFixCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

/**
 * A fix which when executed can set up a linked mode model
 * and put an editor into linked mode.
 *
 * @since 1.10
 */
public interface ILinkedFixCore extends ICleanUpFixCore {

	/**
	 * @return the linked proposal model to use to set up linked positions or <b>null</b>
	 */
	LinkedProposalModelCore getLinkedPositionsCore();
}
