/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IInputSelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;

/**
 * Action group that adds the Java search actions to a context menu and
 * the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class JavaSearchActionGroup extends ActionGroup {

	private JavaSearchGroup fOldGroup;
	private GroupContext fOldContext;

	/**
	 * Creates a new <code>JavaSearchActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 * <p>
	 * Note: this constructor will go away. The final constructor will only take
	 *  an <code>IViewPart</code>
	 * </p>
	 */
	public JavaSearchActionGroup(IViewPart part, IInputSelectionProvider provider) {
		fOldGroup= new JavaSearchGroup();
		fOldContext= new GroupContext(provider);
	}
	
	/**
	 * Creates a new <code>JavaSearchActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 * <p>
	 * Note: this constructor will go away. The final constructor will only take
	 *  an <code>Page</code>
	 * </p>
	 */
	public JavaSearchActionGroup(Page page, IInputSelectionProvider provider) {
		fOldGroup= new JavaSearchGroup();
		fOldContext= new GroupContext(provider);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		fOldGroup.fill(menu, fOldContext);
	}	
}
