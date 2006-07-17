/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import org.eclipse.jface.action.IAction;

public interface IClasspathModifierAction extends IAction {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run();

	/**
	 * Get the description suitable to the provided type
	 * 
	 * @param type the type of the selected element(s), must be a constant of 
	 * <code>DialogPackageActionGroup</code>.
	 * @return a short description of the operation.
	 * 
	 * @see DialogPackageExplorerActionGroup
	 */
	public String getDescription(int type);

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#getId()
	 */
	public String getId();

	public int getTypeId();

	/**
	 * Get the action's name.
	 * 
	 * @return a human readable name for the operation/action executed
	 */
	public String getName();

}