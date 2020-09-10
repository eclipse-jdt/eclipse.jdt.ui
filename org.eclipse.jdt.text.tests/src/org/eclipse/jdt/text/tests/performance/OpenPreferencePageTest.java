/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.preference.PreferenceDialog;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * @since 3.1
 */
public class OpenPreferencePageTest extends TextPerformanceTestCase {

	private static final Class<OpenPreferencePageTest> THIS= OpenPreferencePageTest.class;

	private PerformanceMeter fMeter;

	public static Test suite() {
		return new TestSuite(THIS);
	}

	public void testOpenPreferencePage() {
		Display display= EditorTestHelper.getActiveDisplay();

		PreferenceDialog d= PreferencesUtil.createPreferenceDialogOn(null, null, null, null);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(d.getShell(), IWorkbenchHelpContextIds.PREFERENCE_DIALOG);
		// HACK to get control back instantly
		d.setBlockOnOpen(false);
		d.open();
		EditorTestHelper.runEventQueue();

		Tree tree= findTree(d.getShell());
		assertNotNull(tree);

		tree.forceFocus();

		TreeItem javaNode= findTreeItem(tree.getItems(), "Java"); //$NON-NLS-1$
		assertNotNull(javaNode);
		tree.setSelection(new TreeItem[] {javaNode});
		EditorTestHelper.runEventQueue();


		// setExpanded does not work - use keyboard events
		// item.setExpanded(true);
		SWTEventHelper.pressKeyCode(display, SWT.KEYPAD_ADD);
		long timeout= System.currentTimeMillis() + 5000;
		TreeItem editorNode= null;
		while (editorNode == null && System.currentTimeMillis() < timeout) {
			EditorTestHelper.runEventQueue();
			editorNode= findTreeItem(javaNode.getItems(), "Editor");
		}
		assertNotNull(editorNode);

		EditorTestHelper.runEventQueue();

		Rectangle bounds= editorNode.getBounds();
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

		assertPerformance(fMeter);
	}

	private Tree findTree(Composite composite) {
		for (Control child : composite.getChildren()) {
			if (child instanceof Tree) {
				return (Tree) child;
			} else if (child instanceof Composite) {
				Tree tree= findTree((Composite) child);
				if (tree != null)
					return tree;
			}
		}
		return null;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Performance performance= Performance.getDefault();
		fMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this, "cold"));
		EditorTestHelper.joinJobs(1000, 10000, 100);
	}

	@Override
	protected void tearDown() throws Exception {
		fMeter.dispose();
		super.tearDown();
	}

	private TreeItem findTreeItem(TreeItem[] items, String string) {
		// depth first
		for (TreeItem item : items) {
			if (string.equals(item.getText())) {
				return item;
			}
		}
		return null;
	}
}
