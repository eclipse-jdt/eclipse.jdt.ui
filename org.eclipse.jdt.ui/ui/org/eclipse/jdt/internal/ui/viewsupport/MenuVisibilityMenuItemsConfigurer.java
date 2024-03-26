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

import java.util.function.BiConsumer;

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
	 * <br><br>
	 * <b>Attention:</b> This method must be called when all menu item widgets were already created inside the menu.
	 * @param menu widget containing events receiving items
	 */
	public static void registerForMenu(Menu menu) {
		menu.addMenuListener(new MenuListenerImpl(menu));
	}

	private static class MenuListenerImpl implements MenuListener {
		final Menu menu;

		private MenuListenerImpl(Menu menu) {
			this.menu= menu;
		}

		@Override
		public void menuShown(MenuEvent e) {
			handleEvent(e, IMenuVisibilityMenuItemAction::menuShown);
		}

		@Override
		public void menuHidden(MenuEvent e) {
			handleEvent(e, IMenuVisibilityMenuItemAction::menuHidden);
		}

		private void handleEvent(MenuEvent e, BiConsumer<IMenuVisibilityMenuItemAction, MenuEvent> callback) {
			for (MenuItem item : menu.getItems()) {
				if (item.getData() instanceof ActionContributionItem actionItem
						&& actionItem.getAction() instanceof IMenuVisibilityMenuItemAction listener) {
					callback.accept(listener, e);
				}
			}
		}
	}

	/**
	 * Menu visibility listener tagging interface for menu item actions. If a menu item was created from {@link ActionContributionItem}
	 * and the action it is wrapping implements this interface, then action's methods defined in this interface are called
	 * on change of menu visibility.
	 */
	public interface IMenuVisibilityMenuItemAction {

		/**
		 * Notification when the menu, that contains menu item wrapping an action that is target of this call, was shown.
		 * @param event event forwarded from menu's {@link MenuListener}
		 */
		default void menuShown(MenuEvent event) {}

		/**
		 * Notification when the menu, that contains menu item wrapping an action that is target of this call, was hidden.
		 * @param event event forwarded from menu's {@link MenuListener}
		 */
		default void menuHidden(MenuEvent event) {}
	}
}