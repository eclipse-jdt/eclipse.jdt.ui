/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;

/*package*/ class HistoryDropDownAction extends Action {

	private class HistoryAction extends Action {
		private final Object fElement;

		public HistoryAction(Object element, int accelerator) {
	        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			Assert.isNotNull(element);
			fElement= element;

			String label= fHistory.getText(element);
		    if (accelerator < 10) {
		    		//add the numerical accelerator
			    label= new StringBuffer().append('&').append(accelerator).append(' ').append(label).toString();
			}

			setText(label);
			setImageDescriptor(fHistory.getImageDescriptor(element));
		}

		public void run() {
			fHistory.setActiveEntry(fElement);
		}
	}

	private class HistoryMenuCreator implements IMenuCreator {

		public Menu getMenu(Menu parent) {
			return null;
		}

		public Menu getMenu(Control parent) {
			if (fMenu != null) {
				fMenu.dispose();
			}
			fMenu= new Menu(parent);
			
			List entries= fHistory.getHistoryEntries();
			boolean checkOthers= addEntryMenuItems(entries);
			
			if (entries.size() > 0) {
				new MenuItem(fMenu, SWT.SEPARATOR);
			}
			
			Action others= new HistoryListAction(fHistory);
			others.setChecked(checkOthers);
			addActionToMenu(fMenu, others);
			
			Action clearAction= fHistory.getClearAction();
			if (clearAction != null) {
				addActionToMenu(fMenu, clearAction);
			}
		
			return fMenu;
		}

		private boolean addEntryMenuItems(List entries) {
			if (entries.isEmpty()) {
				return false;
			}
			
			boolean checkOthers= true;
			int min= Math.min(entries.size(), RESULTS_IN_DROP_DOWN);
			for (int i= 0; i < min; i++) {
				Object entry= entries.get(i);
				HistoryAction action= new HistoryAction(entry, i + 1);
				boolean check= entry.equals(fHistory.getCurrentEntry());
				action.setChecked(check);
				if (check)
					checkOthers= false;
				addActionToMenu(fMenu, action);
			}
			return checkOthers;
		}

		private void addActionToMenu(Menu parent, Action action) {
			ActionContributionItem item= new ActionContributionItem(action);
			item.fill(parent, -1);
		}

		public void dispose() {
			fHistory= null;
		
			if (fMenu != null) {
				fMenu.dispose();
				fMenu= null;
			}
		}
	}

	public static final int RESULTS_IN_DROP_DOWN= 10;

	private ViewHistory fHistory;
	private Menu fMenu;

	public HistoryDropDownAction(ViewHistory history) {
		fHistory= history;
		fMenu= null;
		setMenuCreator(new HistoryMenuCreator());
		fHistory.configureHistoryDropDownAction(this);
	}

	public void run() {
		new HistoryListAction(fHistory).run();
	}
}
