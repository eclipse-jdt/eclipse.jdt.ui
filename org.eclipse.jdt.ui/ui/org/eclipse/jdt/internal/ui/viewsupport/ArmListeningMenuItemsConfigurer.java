/*******************************************************************************
* Copyright (c) 2024 Jozef Tomek and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Jozef Tomek - initial API and implementation
*******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.ArmListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.jface.action.ActionContributionItem;

/**
 * Facility to make it possible for menu widget items to react to their arm events, i.e. when menu items
 * are chosen (before confirming their selection) by mouse hover or keyboard navigation.
 * Menu items become receivers of these events by means of being created from {@link ActionContributionItem}
 * that wraps and action implementing convenient callback interface {@link IArmListeningMenuItemAction}.
 */
public final class ArmListeningMenuItemsConfigurer {

	final Runnable postHandleEventAction;
	MenuItem lastEnteredItem= null;
	IArmListeningMenuItemAction lastEnteredItemAction= null;

	/**
	 * Does necessary setup of the menu widget to enable forwarding menu items armed events to all of it's
	 * child menu items that are {@link IArmListeningMenuItemAction receivers} of these events.
	 * Context specific post-handle action(s) callback <code>postHandleEventAction</code> is executed after
	 * dispatching menu items armed event to all receiver menu items if at least one of them signals that
	 * post-handle action(s) should be performed.
	 * <br><br>
	 * <b>Attention:</b> This method must be called when all menu item widgets were already created inside the menu.
	 * @param menu widget containing events receiving items
	 * @param postHandleEventAction context specific post-handle action(s) callback
	 */
	public static void registerForMenu(Menu menu, Runnable postHandleEventAction) {
		var instance= new ArmListeningMenuItemsConfigurer(postHandleEventAction);
		ArmListener listener= instance::handleEvent;
		for (MenuItem item : menu.getItems()) {
			item.addArmListener(listener);
		}
		menu.addListener(SWT.Hide, instance::menuHidden);
	}

	private ArmListeningMenuItemsConfigurer(Runnable postHandleEventAction) {
		this.postHandleEventAction= postHandleEventAction;
	}

	private void handleEvent(ArmEvent event) {
		var menuItem= (MenuItem) event.widget;
		if (lastEnteredItem == menuItem) {
			return;
		}
		boolean runPostAction= false;
		if (lastEnteredItemAction != null) {
			runPostAction |= lastEnteredItemAction.itemUnarmed(event);
			lastEnteredItemAction= null;
		}
		lastEnteredItem= menuItem;
		if (menuItem.getData() instanceof ActionContributionItem actionItem
				&& actionItem.getAction() instanceof IArmListeningMenuItemAction armedItem) {
			lastEnteredItemAction= armedItem;
			runPostAction |= armedItem.itemArmed(event);
		}
		if (postHandleEventAction != null && runPostAction) {
			postHandleEventAction.run();
		}
	}

	@SuppressWarnings("unused")
	private void menuHidden(Event e) {
		boolean runPostAction= false;
		if (lastEnteredItemAction != null) {
			runPostAction= lastEnteredItemAction.itemUnarmed(null);
			lastEnteredItemAction= null;
		}
		if (postHandleEventAction != null && runPostAction) {
			postHandleEventAction.run();
		}
	}

	/**
	 * Arm listener tagging interface for menu item actions. If a menu item was created from {@link ActionContributionItem}
	 * and the action it is wrapping implements this interface, then action's methods defined in this interface are called
	 * on armed events of the corresponding menu item.
	 * <br><br>
	 * Methods return <code>boolean</code> to indicate whether some further post-handle action(s), that is specific to usage context,
	 * should be performed.
	 */
	public interface IArmListeningMenuItemAction  {

		/**
		 * Notification when menu item wrapping an action that is target of this call was armed (by mouse hover or keyboard navigation).
		 * @param event event forwarded from menu's {@link ArmListener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean itemArmed(ArmEvent event) {
			return false;
		}

		/**
		 * Notification when menu item wrapping an action that is target of this call was armed (by mouse hover or keyboard navigation)
		 * previously but is not anymore.
		 * @param event event forwarded from menu's {@link ArmListener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean itemUnarmed(ArmEvent event) {
			return false;
		}
	}
}