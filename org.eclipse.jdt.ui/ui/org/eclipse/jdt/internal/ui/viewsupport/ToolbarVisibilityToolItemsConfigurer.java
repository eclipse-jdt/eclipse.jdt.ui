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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.ActionContributionItem;

/**
 * Facility to make it possible for toolbar widget items to react to their parent toolbar's visibility events.
 * Toolbar items become receivers of these events by means of being created from {@link ActionContributionItem}
 * that wraps and action implementing convenient callback interface {@link IToolbarVisibilityToolItemAction}.
 */
public final class ToolbarVisibilityToolItemsConfigurer {

	final ToolBar toolbar;
	final Runnable postHandleEventAction;

	/**
	 * Does necessary setup of the toolbar widget to enable forwarding toolbar's visibility events to all of it's
	 * child toolbar items that are {@link IToolbarVisibilityToolItemAction receivers} of these events.
	 * Context specific post-handle action(s) callback <code>postHandleEventAction</code> is executed after
	 * dispatching toolbar visibility event to all receiver toolbar items if at least one of them signals that
	 * post-handle action(s) should be performed.
	 * <br><br>
	 * <b>Attention:</b> This method must be called when all toolbar item widgets were already created inside the tololbar.
	 * @param toolbar widget containing events receiving items
	 * @param postHandleEventAction context specific post-handle action(s) callback
	 */
	public static void registerForToolBarManager(ToolBar toolbar, Runnable postHandleEventAction) {
		var instance= new ToolbarVisibilityToolItemsConfigurer(toolbar, postHandleEventAction);
		Listener listener= instance::handleEvent;
		toolbar.getShell().addListener(SWT.Show, listener);
		toolbar.getShell().addListener(SWT.Hide, listener);
	}

	private ToolbarVisibilityToolItemsConfigurer(ToolBar toolbar, Runnable postHandleEventAction) {
		this.toolbar= toolbar;
		this.postHandleEventAction= postHandleEventAction;
	}

	private void handleEvent(Event event) {
		boolean runPostAction= false;
		BiFunction<IToolbarVisibilityToolItemAction, Event, Boolean> callback =
			switch (event.type) {
				case SWT.Show -> IToolbarVisibilityToolItemAction::toolbarShown;
				case SWT.Hide -> IToolbarVisibilityToolItemAction::toolbarHidden;
				default -> throw new IllegalArgumentException(event.toString());
			};
		for (ToolItem toolItem : toolbar.getItems()) {
			if (toolItem != null
					&& toolItem.getData() instanceof ActionContributionItem
					&& ((ActionContributionItem) toolItem.getData()).getAction() instanceof IToolbarVisibilityToolItemAction listener) {
				runPostAction |= callback.apply(listener, event);
			}
		}
		if (postHandleEventAction != null && runPostAction) {
			postHandleEventAction.run();
		}
	}

	/**
	 * Toolbar visibility listener tagging interface for toolbar item actions. If a toolbar item was created from {@link ActionContributionItem}
	 * and the action it is wrapping implements this interface, then action's methods defined in this interface are called
	 * on change of toolbar visibility.
	 * <br><br>
	 * Methods return <code>boolean</code> to indicate whether some further post-handle action(s), that is specific to usage context,
	 * should be performed.
	 */
	public interface IToolbarVisibilityToolItemAction  {

		/**
		 * Notification when the toolbar, that contains toolbar item wrapping an action that is target of this call, was shown.
		 * @param event event forwarded from toolbar's {@link Listener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean toolbarShown(Event event) {
			return false;
		}

		/**
		 * Notification when the toolbar, that contains toolbar item wrapping an action that is target of this call, was hidden.
		 * @param event event forwarded from toolbar's {@link Listener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean toolbarHidden(Event event) {
			return false;
		}
	}
}