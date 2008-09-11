/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

/**
 * A fix which when executed can set up a linked mode model
 * and put an editor into linked mode.
 *
 * @since 3.4
 */
public interface ILinkedFix extends IFix {

	/**
	 * @return the linked proposal model to use to set up linked positions or <b>null</b>
	 */
	public LinkedProposalModel getLinkedPositions();
}
