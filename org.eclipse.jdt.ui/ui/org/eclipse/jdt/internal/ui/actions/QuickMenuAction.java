/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;


public abstract class QuickMenuAction extends Action { 

	private static final int CHAR_INDENT= 3;
	
	public QuickMenuAction(String commandId) {
		setActionDefinitionId(commandId);
	}

	public void run() {
		Display display= Display.getCurrent();
		if (display == null)
			return;
		Control focus= display.getFocusControl();
		if (focus == null || focus.isDisposed())
			return;
		
		Point location= getMenuLocation(focus);
		if (location == null)
			return;
		MenuManager menu= new MenuManager();
		fillMenu(menu);
		final Menu widget= menu.createContextMenu(focus.getShell());
		widget.setLocation(location);
		widget.setVisible(true);
	}
	
	protected abstract void fillMenu(IMenuManager menu);
	
	public String getShortCutString() {
		final ICommandManager commandManager = PlatformUI.getWorkbench().getCommandSupport().getCommandManager();
		final ICommand command = commandManager.getCommand(getActionDefinitionId());
		if (command.isDefined()) {
			List l= command.getKeySequenceBindings();
			if (!l.isEmpty()) {
				IKeySequenceBinding binding= (IKeySequenceBinding)l.get(0);
				return binding.getKeySequence().format();
			}
		}
		return null; //$NON-NLS-1$
	}
	
	private Point getMenuLocation(Control focus) {
		if (focus instanceof StyledText) {
			return getMenuLocation((StyledText)focus);
		} else if (focus instanceof Tree) {
			return getMenuLocation((Tree)focus);
		} else if (focus instanceof Table) {
			return getMenuLocation((Table)focus);
		}
		return null;
	}
	
	protected Point getMenuLocation(StyledText text) {
		Point result= text.getLocationAtOffset(text.getCaretOffset());
		result.y+= text.getLineHeight();
		return result;
	}
	
	protected Point getMenuLocation(Tree tree) {
		TreeItem[] items= tree.getSelection();
		switch (items.length) {
			case 0:
				return null;
			case 1:
				Rectangle bounds= items[0].getBounds();
				return tree.toDisplay(
					Math.max(0, bounds.x + getAvarageCharWith(tree) * CHAR_INDENT), 
					bounds.y + bounds.height);
			default:
				Rectangle[] rectangles= new Rectangle[items.length];
				for (int i= 0; i < rectangles.length; i++) {
					rectangles[i]= items[i].getBounds();
				}
				Point cursorLocation= tree.getDisplay().getCursorLocation();
				Point p= findBestLocation(getIncludedPositions(rectangles, tree.getClientArea()), 
					tree.toControl(cursorLocation));
				if (p == null)
					return cursorLocation;
				return tree.toDisplay(p.x + getAvarageCharWith(tree) * CHAR_INDENT, p.y);
		}
	}
	
	protected Point getMenuLocation(Table table) {
		TableItem[] items= table.getSelection();
		switch (items.length) {
			case 0: {
				return null;
			} case 1: {
				Rectangle bounds= items[0].getBounds(0);
				Rectangle iBounds= items[0].getImageBounds(0);
				return table.toDisplay(
					Math.max(0, bounds.x + iBounds.width + getAvarageCharWith(table) * CHAR_INDENT), 
					bounds.y + bounds.height);
			} default: {
				Rectangle[] rectangles= new Rectangle[items.length];
				for (int i= 0; i < rectangles.length; i++) {
					rectangles[i]= items[i].getBounds(0);
				}
				Rectangle iBounds= items[0].getImageBounds(0);
				Point cursorLocation= table.getDisplay().getCursorLocation();
				Point p= findBestLocation(getIncludedPositions(rectangles, table.getClientArea()), 
					table.toControl(cursorLocation));
				if (p == null)
					return cursorLocation;
				return table.toDisplay(
					p.x + iBounds.width + getAvarageCharWith(table) * CHAR_INDENT, 
					p.y);
			}
		}
	}
	
	private Point[] getIncludedPositions(Rectangle[] rectangles, Rectangle widgetBounds) {
		List result= new ArrayList();
		for (int i= 0; i < rectangles.length; i++) {
			Rectangle rectangle= rectangles[i];
			Rectangle intersect= widgetBounds.intersection(rectangle);
			if (intersect != null && intersect.height == rectangle.height) {
				result.add(new Point(intersect.x, intersect.y + intersect.height));
			}
		}
		return (Point[]) result.toArray(new Point[result.size()]);
	}
	
	private Point findBestLocation(Point[] points, Point relativeCursor) {
		Point result= null;
		double bestDist= Double.MAX_VALUE;
		for (int i= 0; i < points.length; i++) {
			Point point= points[i];
			int a= 0;
			int b= 0;
			if (point.x > relativeCursor.x) {
				a= point.x - relativeCursor.x;
			} else {
				a= relativeCursor.x - point.x;
			}
			if (point.y > relativeCursor.y) {
				b= point.y - relativeCursor.y;	
			} else {
				b= relativeCursor.y - point.y;
			}
			double dist= Math.sqrt(a * a + b * b);
			if (dist < bestDist) {
				result= point;
				bestDist= dist;
			}
		}
		return result;
	}
	
	private int getAvarageCharWith(Control control) {
		GC gc= null;
		try {
			gc= new GC(control);
			return gc.getFontMetrics().getAverageCharWidth();
		} finally {
			if (gc != null)
				gc.dispose();
		}
	}
}
