/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;

public class HistoryDropDownAction extends Action implements IMenuCreator {


	public static final int RESULTS_IN_DROP_DOWN= 10;

	private TypeHierarchyViewPart fHierarchyView;
	
	public HistoryDropDownAction(TypeHierarchyViewPart view) {
		fHierarchyView= view;
		setToolTipText(TypeHierarchyMessages.getString("HistoryDropDownAction.label")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(this, "backward_nav.gif"); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.TYPEHIERARCHY_BACKWARD_ACTION });
		setMenuCreator(this);
	}

	public void dispose() {
		fHierarchyView= null;
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	public Menu getMenu(Control parent) {
		Menu menu= new Menu(parent);
		for (int i= 0; i < RESULTS_IN_DROP_DOWN; i++) {
			IType type= fHierarchyView.getHistoryEntry(i);
			if (type == null) {
				break;
			}
			HistoryAction action= new HistoryAction(fHierarchyView, i, type);
			action.setChecked(i == fHierarchyView.getCurrentHistoryIndex());
			addActionToMenu(menu, action);
		}
		return menu;
	}

	protected void addActionToMenu(Menu parent, Action action) {
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	public void run() {
	}
}
