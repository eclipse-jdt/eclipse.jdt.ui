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
package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.preference.PreferenceDialog;

import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;


/**
 * @since 3.1
 */
public class OpenPreferencePageTest extends TestCase {
	
	private static final Class THIS= OpenPreferencePageTest.class;
	
	private PerformanceMeter fMeter;
	
	public static Test suite() {
		return new TestSuite(THIS);
	}

	public void testOpenPreferencePage() {
		Display display= EditorTestHelper.getActiveDisplay();
		
		PreferenceDialog d= PreferencesUtil.createPreferenceDialogOn(null, null, null);
		WorkbenchHelp.setHelp(d.getShell(), IWorkbenchHelpContextIds.PREFERENCE_DIALOG);
		// HACK to get control back instantly
		d.setBlockOnOpen(false);
		d.open();
		EditorTestHelper.runEventQueue();
		
		Control control= display.getFocusControl();
		assertTrue(control instanceof Tree);
		Tree tree= (Tree) control;
		TreeItem item= findTreeItem(tree.getItems(), "Java"); //$NON-NLS-1$
		assertTrue(item != null);
		tree.setSelection(new TreeItem[] {item});
		EditorTestHelper.runEventQueue();
		
		// setExpanded does not work - use keyboard events
		// item.setExpanded(true);
		SWTEventHelper.pressKeyCode(display, SWT.KEYPAD_ADD);
		long timeout= System.currentTimeMillis() + 1000;
		TreeItem[] items= item.getItems();
		item= null;
		while (item == null && System.currentTimeMillis() < timeout) {
			EditorTestHelper.runEventQueue();
			item= findTreeItem(items, "Editor");
		}
		assertNotNull(item);
		
		EditorTestHelper.runEventQueue();
		
		Rectangle bounds= item.getBounds();
		Point p= new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		p= tree.toDisplay(p);
		Event event= new Event();
		event.type= SWT.MouseMove;
		event.x= p.x;
		event.y= p.y;
		display.post(event);
		EditorTestHelper.runEventQueue();
		event.type= SWT.MouseDown;
		event.button= 1;
		
		fMeter.start();
		display.post(event);
		event.type= SWT.MouseUp;
		display.post(event);
		EditorTestHelper.runEventQueue();
		fMeter.stop();
		fMeter.commit();
		
		d.close();
		
		Performance.getDefault().assertPerformance(fMeter);
	}

	protected void setUp() throws Exception {
		Performance performance= Performance.getDefault();
		fMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this, "cold"));
		EditorTestHelper.joinJobs(1000, 10000, 100);
	}
	
	protected void tearDown() throws Exception {
		fMeter.dispose();
	}

	private TreeItem findTreeItem(TreeItem[] items, String string) {
		// depth first
		for (int i= 0; i < items.length; i++) {
			if (string.equals(items[i].getText()))
				return items[i];
		}
		return null;
	}
}
