package junit.swingui;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.TreePath;
import junit.framework.*;

/**
 * A hierarchical view of a test run. The contents of a test suite is shown as a
 * tree.
 */
public class TestHierarchyRunView implements TestRunView {
	TestSuitePanel fTreeBrowser;
	TestRunContext fTestContext;

	public TestHierarchyRunView(TestRunContext context) {
		fTestContext = context;
		fTreeBrowser = new TestSuitePanel();
		fTreeBrowser.getTree().addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				testSelected();
			}
		});
	}

	public void addTab(JTabbedPane pane) {
		Icon treeIcon = TestRunner.getIconResource(getClass(), "icons/hierarchy.gif");
		pane.addTab("Test Hierarchy", treeIcon, fTreeBrowser, "The test hierarchy");
	}

	public Test getSelectedTest() {
		return fTreeBrowser.getSelectedTest();
	}

	public void activate() {
		testSelected();
	}

	public void revealFailure(Test failure) {
		JTree tree = fTreeBrowser.getTree();
		TestTreeModel model = (TestTreeModel) tree.getModel();
		Vector vpath = new Vector();
		int index = model.findTest(failure, (Test) model.getRoot(), vpath);
		if (index >= 0) {
			Object[] path = new Object[vpath.size() + 1];
			vpath.copyInto(path);
			Object last = path[vpath.size() - 1];
			path[vpath.size()] = model.getChild(last, index);
			TreePath selectionPath = new TreePath(path);
			tree.setSelectionPath(selectionPath);
			tree.makeVisible(selectionPath);
		}
	}

	public void aboutToStart(Test suite, TestResult result) {
		fTreeBrowser.showTestTree(suite);
		result.addListener(fTreeBrowser);
	}

	public void runFinished(Test suite, TestResult result) {
		result.removeListener(fTreeBrowser);
	}

	protected void testSelected() {
		fTestContext.handleTestSelected(getSelectedTest());
	}
}
