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

import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Base class for an {@link Action} that provides menu to be presented as an item in a toolbar which should
 * automatically re-appear again after menu was hidden as a result of selecting any of it's menu items.
 */
public class ReappearingMenuToolbarAction extends Action implements IMenuCreator {
	protected Action[] actions;
	protected Menu menu= null;
	private Point menuLocation;

	public ReappearingMenuToolbarAction(String text, ImageDescriptor image, Action... actions) {
		super(text, IAction.AS_DROP_DOWN_MENU);
		setImageDescriptor(image);
		setHoverImageDescriptor(image);
		this.actions= actions;
		setId(ReappearingMenuToolbarAction.class.getSimpleName());
		setMenuCreator(this);
	}

	/**
	 * Finds an {@link ActionContributionItem} wrapping <code>this</code> action amongst item widgets in a passed
	 * toolbar and does necessary setup to enable menu re-appearing behavior for menu widget supplied by this action.
	 * <br><br>
	 * <b>Attention:</b> This method must be called when all toolbar item widgets were already created inside the toolbar.
	 * @param toolbar widget where menu supplied by this action is parented
	 */
	public void setupMenuReopen(ToolBar toolbar) {
		for (var item : toolbar.getItems()) {
			if (item.getData() instanceof ActionContributionItem actionItem && actionItem.getAction() == this) {
				if (actionItem.getWidget() != null) {
					actionItem.getWidget().addListener(SWT.Selection, this::menuButtonSelected);
				} else {
					JavaPlugin.logErrorMessage("Unable to setup menu reopening since it's toolbar item widget is null"); //$NON-NLS-1$
				}
				return;
			}
		}
	}

	private void menuButtonSelected(Event e) {
		if (e.detail == SWT.ARROW) {
			// menu is being shown, save location used to position the menu so that we can later show it there
			menuLocation= ((ToolItem) e.widget).getParent().toDisplay(new Point(e.x, e.y));
		}
	}

	@Override
	public Menu getMenu(Control parent) {
		if (menu == null) {
			menu= new Menu(parent);
			addMenuItems();
		}
		return menu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	protected void addMenuItems() {
		Stream.of(actions).forEach(this::addMenuItem);
	}

	protected void addMenuItem(Action action) {
		var item= new ActionContributionItem(action);
		item.fill(menu, -1);
		if (action instanceof IReappearingMenuItem reappearingMenuItem) {
			((MenuItem) item.getWidget()).addSelectionListener(
					SelectionListener.widgetSelectedAdapter(e -> itemSelected(reappearingMenuItem)));
		}
	}

	private void itemSelected(IReappearingMenuItem action) {
		if (action.reopenMenu()) {
			if (menuLocation != null) {
				menu.setLocation(menuLocation);
				menu.setVisible(true); // display again after item selection
			} else {
				JavaPlugin.logErrorMessage(
						"Unable to display " //$NON-NLS-1$
						+ getClass().getName()
						+ " again since no previous display location was set"); //$NON-NLS-1$
			}
		}
	}

	@Override
	public void dispose() {
		if (menu != null) {
			menu.dispose();
		}
	}

	/**
	 * Reappearing menu item tagging interface for menu item actions. If a menu item was created for the action that implements
	 * this interface, then selecting this menu item (resulting in menu being closed) may, by means of {@link #reopenMenu()}
	 * return value, trigger menu to be displayed again right away.
	 */
	public interface IReappearingMenuItem {

		/**
		 * Whether the menu, that was closed after selecting menu item corresponding to this action, should be displayed again
		 * right away.
		 * @return <code>true</code> if menu should be displayed again after selecting this action, <code>false</code> otherwise
		 */
		default boolean reopenMenu() {
			return true;
		}
	}

}