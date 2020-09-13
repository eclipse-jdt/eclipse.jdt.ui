/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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

import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

/**
 * A <code>ICleanUpFix</code> which can be used in a
 * correction proposal environment. A proposal
 * will be shown to the user and if chosen the
 * fix is executed.
 *
 * @since 1.10
 */
public interface IProposableFix extends ICleanUpFixCore {

	/**
	 * Returns the string to be displayed in the list of completion proposals.
	 *
	 * @return the string to be displayed
	 */
	String getDisplayString();

	/**
	 * Returns optional additional information about the proposal. The additional information will
	 * be presented to assist the user in deciding if the selected proposal is the desired choice.
	 * <p>
	 * Returns <b>null</b> if the default proposal info should be used.
	 * </p>
	 *
	 * @return the additional information or <code>null</code>
	 */
	String getAdditionalProposalInfo();

	/**
	 * A status informing about issues with this fix
	 * or <b>null</b> if no issues.
	 *
	 * @return status to inform the user
	 */
	IStatus getStatus();
}
