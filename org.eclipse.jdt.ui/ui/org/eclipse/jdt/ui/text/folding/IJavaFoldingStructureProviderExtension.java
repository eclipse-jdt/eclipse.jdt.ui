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
package org.eclipse.jdt.ui.text.folding;



/**
 * Extends {@link IJavaFoldingStructureProvider} with the following
 * functions:
 * <ul>
 * <li>collapsing of comments and members</li>
 * </ul>
 * 
 * @since 3.2
 */
public interface IJavaFoldingStructureProviderExtension {
	/**
	 * Collapses all members except for top level types.
	 */
	void collapseMembers();
	/**
	 * Collapses all comments.
	 */
	void collapseComments();
}
