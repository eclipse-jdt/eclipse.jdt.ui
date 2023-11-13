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

import java.util.function.BiFunction;

import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.jface.action.ActionContributionItem;

/**
 * Facility to make it possible for menu widget items to react to their parent menu's visibility events.
 * Menu items become receivers of these events by means of being created from {@link ActionContributionItem}
 * that wraps and action implementing convenient callback interface {@link IMenuVisibilityMenuItemAction}.
 */
public final class MenuVisibilityMenuItemsConfigurer {

	/**
	 * Does necessary setup of the menu widget to enable forwarding menu's visibility events to all of it's
	 * child menu items that are {@link IMenuVisibilityMenuItemAction receivers} of these events.
	 * Context specific post-handle action(s) callback <code>postHandleEventAction</code> is executed after
	 * dispatching menu visibility event to all receiver menu items if at least one of them signals that
	 * post-handle action(s) should be performed.
	 * <br><br>
	 * <b>Attention:</b> This method must be called when all menu item widgets were already created inside the menu.
	 * @param menu widget containing events receiving items
	 * @param postHandleEventAction context specific post-handle action(s) callback
	 */
	public static void registerForMenu(Menu menu, Runnable postHandleEventAction) {
		menu.addMenuListener(new MenuListenerImpl(menu, postHandleEventAction));
	}

	private static class MenuListenerImpl implements MenuListener {
		final Menu menu;
		final Runnable postHandleEventAction;

		private MenuListenerImpl(Menu menu, Runnable postHandleEventAction) {
			this.menu= menu;
			this.postHandleEventAction= postHandleEventAction;
		}

		@Override
		public void menuShown(MenuEvent e) {
			handleEvent(e, IMenuVisibilityMenuItemAction::menuShown);
		}

		@Override
		public void menuHidden(MenuEvent e) {
			handleEvent(e, IMenuVisibilityMenuItemAction::menuHidden);
		}

		private void handleEvent(MenuEvent e, BiFunction<IMenuVisibilityMenuItemAction, MenuEvent, Boolean> callback) {
			boolean runPostAction= false;
			for (MenuItem item : menu.getItems()) {
				if (item.getData() instanceof ActionContributionItem actionItem
						&& actionItem.getAction() instanceof IMenuVisibilityMenuItemAction listener) {
					runPostAction |= callback.apply(listener, e);
				}
			}
			if (postHandleEventAction != null && runPostAction) {
				postHandleEventAction.run();
			}
		}
	}

	/**
	 * Menu visibility listener tagging interface for menu item actions. If a menu item was created from {@link ActionContributionItem}
	 * and the action it is wrapping implements this interface, then action's methods defined in this interface are called
	 * on change of menu visibility.
	 * <br><br>
	 * Methods return <code>boolean</code> to indicate whether some further post-handle action(s), that is specific to usage context,
	 * should be performed.
	 */
	public interface IMenuVisibilityMenuItemAction {

		/**
		 * Notification when the menu, that contains menu item wrapping an action that is target of this call, was shown.
		 * @param event event forwarded from menu's {@link MenuListener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean menuShown(MenuEvent event) {
			return false;
		}

		/**
		 * Notification when the menu, that contains menu item wrapping an action that is target of this call, was hidden.
		 * @param event event forwarded from menu's {@link MenuListener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean menuHidden(MenuEvent event) {
			return false;
		}
	}
}