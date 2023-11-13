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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.ActionContributionItem;

/**
 * Facility to make it possible for toolbar widget items to react to their mouse events.
 * Toolbar items become receivers of these events by means of being created from {@link ActionContributionItem}
 * that wraps and action implementing convenient callback interface {@link IMouseListeningToolbarItemAction}.
 */
public final class MouseListeningToolItemsConfigurer {

	private static final String WIDGET_DATA_KEY= MouseListeningToolItemsConfigurer.class.getSimpleName();

	final ToolBar toolbar;
	final Runnable postHandleEventAction;
	ToolItem lastEnteredItem= null;
	IMouseListeningToolbarItemAction lastEnteredItemAction= null;

	/**
	 * Does necessary setup of the toolbar widget to enable forwarding toolbar items mouse events to all of it's
	 * child menu items that are {@link IMouseListeningToolbarItemAction receivers} of these events.
	 * Context specific post-handle action(s) callback <code>postHandleEventAction</code> is executed after
	 * dispatching toolbar items mouse event to all receiver toolbar items if at least one of them signals that
	 * post-handle action(s) should be performed.
	 * <br><br>
	 * <b>Attention:</b> This method must be called when all toolbar item widgets were already created inside the toolbar.
	 * @param toolbar widget containing events receiving items
	 * @param postHandleEventAction context specific post-handle action(s) callback
	 */
	public static void registerForToolBarManager(ToolBar toolbar, Runnable postHandleEventAction) {
		if (toolbar.getData(WIDGET_DATA_KEY) == null) { // prevent re-registrations on same toolbar widget
			var instance= new MouseListeningToolItemsConfigurer(toolbar, postHandleEventAction);
			Listener listener= instance::handleEvent;
			toolbar.setData(WIDGET_DATA_KEY, listener);
			toolbar.addListener(SWT.MouseEnter, listener);
			toolbar.addListener(SWT.MouseExit, listener);
			toolbar.addListener(SWT.MouseHover, listener);
			toolbar.addListener(SWT.MouseMove, listener);
			toolbar.addListener(SWT.MouseDown, listener);
		}
	}

	private MouseListeningToolItemsConfigurer(ToolBar toolbar, Runnable postHandleEventAction) {
		this.toolbar= toolbar;
		this.postHandleEventAction= postHandleEventAction;
	}

	private void handleEvent(Event event) {
		boolean runPostAction= false;
		switch (event.type) {
			case SWT.MouseMove:
			case SWT.MouseEnter:
				var toolItem= toolbar.getItem(new Point(event.x, event.y));
				if (lastEnteredItem != toolItem) {
					if (lastEnteredItem != null) {
						runPostAction |= lastEnteredItemAction.mouseExit(event);
						lastEnteredItemAction= null;
						lastEnteredItem= null;
					}
					if (toolItem != null
							&& toolItem.getData() instanceof ActionContributionItem actionItem
							&& actionItem.getAction() instanceof IMouseListeningToolbarItemAction enteredAction) {
						lastEnteredItem= toolItem;
						lastEnteredItemAction= enteredAction;
						runPostAction |= enteredAction.mouseEnter(event);
					}
				} else if (lastEnteredItem != null) {
					runPostAction |= lastEnteredItemAction.mouseMove(event);
				}
				break;
			case SWT.MouseExit:
				if (lastEnteredItem != null) {
					runPostAction |= lastEnteredItemAction.mouseExit(event);
					lastEnteredItemAction= null;
					lastEnteredItem= null;
				}
				break;
			case SWT.MouseHover:
				if (lastEnteredItem != null) {
					runPostAction |= lastEnteredItemAction.mouseHover(event);
				}
				break;
			case SWT.MouseDown:
				if (lastEnteredItem != null && event.button == 1) {
					runPostAction |= lastEnteredItemAction.mouseClick(event);
				}
				break;
			default:
				break;
		}
		if (postHandleEventAction != null && runPostAction) {
			postHandleEventAction.run();
		}
	}

	/**
	 * Mouse listener tagging interface for toolbar item actions. If a toolbar item was created from {@link ActionContributionItem}
	 * and the action it is wrapping implements this interface, then action's methods defined in this interface are called
	 * on mouse events over corresponding toolbar item.
	 * <br><br>
	 * Methods return <code>boolean</code> to indicate whether some further post-handle action(s), that is specific to usage context,
	 * should be performed.
	 */
	public interface IMouseListeningToolbarItemAction  {

		/**
		 * Notification when mouse enters area of the widget of toolbar item wrapping an action that is target of this call.
		 * @param event event forwarded from menu's {@link Listener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean mouseEnter(Event event) {
			return false;
		}

		/**
		 * Notification when mouse exits area of the widget of toolbar item wrapping an action that is target of this call.
		 * @param event event forwarded from menu's {@link Listener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean mouseExit(Event event) {
			return false;
		}

		/**
		 * Notification when mouse hovers over area of the widget of toolbar item wrapping an action that is target of this call.
		 * @param event event forwarded from menu's {@link Listener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean mouseHover(Event event) {
			return false;
		}

		/**
		 * Notification when mouse moves inside area of the widget of toolbar item wrapping an action that is target of this call.
		 * @param event event forwarded from menu's {@link Listener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean mouseMove(Event event) {
			return false;
		}

		/**
		 * Notification when mouse is clicked inside area of the widget of toolbar item wrapping an action that is target of this call.
		 * @param event event forwarded from menu's {@link Listener}
		 * @return <code>true</code> if context specific post-handle action(s) should be performed, <code>false</code> otherwise (default)
		 */
		default boolean mouseClick(Event event) {
			return false;
		}
	}
}