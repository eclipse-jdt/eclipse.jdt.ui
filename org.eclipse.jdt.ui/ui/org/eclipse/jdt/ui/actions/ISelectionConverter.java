/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.viewers.ISelection;

/**
 * A selection converter converts a given selection into
 * another selection.
 * 
 * @since 3.2
 */
public interface ISelectionConverter {

	/**
	 * Converts the given original viewer selection into a new
	 * selection.
	 * 
	 * @param viewerSelection the original viewer selection
	 * 
	 * @return the new selection to be used
	 */
	public ISelection convertFrom(ISelection viewerSelection);

	/**
	 * Converts a selection to a viewer selection.
	 * 
	 * @param selection the selection to convert
	 * 
	 * @return a viewer selection
	 */
	public ISelection convertTo(ISelection selection);
}
