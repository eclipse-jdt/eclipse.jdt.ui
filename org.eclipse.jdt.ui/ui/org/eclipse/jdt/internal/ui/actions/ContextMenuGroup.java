/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
// AW
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IInputSelectionProvider;

/**
 * A context menu contributor adds a set of actions to a given context menu.
 * Since actions managed by a context menu contributor form a logical set
 * the contributor can also be used to decide whether the managed actions
 * should be added at all.
 * 
 * @deprecated Use ActionGroup instead
 */
public abstract class ContextMenuGroup {

	/**
	 * Adds the given groups to the supplied context menu manager.
	 */
	public static void add(IMenuManager manager, ContextMenuGroup[] groups, IInputSelectionProvider provider) {
		GroupContext context= new GroupContext(provider);
		for (int i= 0; i < groups.length; i++)
			groups[i].fill(manager, context);
	}

	/**
	 * Fills the actions managed by this group to the given context menu.
	 * Implementors of this method can assume that the context manager already
	 * contains a group marker with this group's group name. So subclasses
	 * can add actions by simply calling <code>appendToGroup('groupName', ...);
	 * </code>.
	 * 
	 * @param manager the menu manager that represents the context menu.
	 * @param context the group context to access the current selection, the
	 *  current input element and the common super type for the selection.
	 */
	public abstract void fill(IMenuManager manager, GroupContext context);	
}