/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.jface.viewers.ISelection;

/**
 * A selection converter converts a given selection into
 * another selection.
 * 
 * @since 3.0
 */
public interface ISelectionConverter {
	/**
	 * Converts the given selection into another selection.
	 * 
	 * @param selection the original selection
	 * 
	 * @return the converted selection
	 */
	public ISelection convert(ISelection selection);
}
