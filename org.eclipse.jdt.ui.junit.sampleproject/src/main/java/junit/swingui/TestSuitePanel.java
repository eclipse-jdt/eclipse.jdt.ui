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

import java.awt.*;
import java.util.Vector;

import javax.swing.*;
import javax.swing.tree.*;
import junit.framework.*;

/**
 * A Panel showing a test suite as a tree.
 */
class TestSuitePanel extends JPanel implements TestListener {
	private final JTree fTree;
	private final JScrollPane fScrollTree;
	private TestTreeModel fModel;

	static class TestTreeCellRenderer extends DefaultTreeCellRenderer {
		private Icon fErrorIcon;
		private Icon fOkIcon;
		private Icon fFailureIcon;

		TestTreeCellRenderer() {
			super();
			loadIcons();
		}

		void loadIcons() {
			fErrorIcon = TestRunner.getIconResource(getClass(), "icons/error.gif");
			fOkIcon = TestRunner.getIconResource(getClass(), "icons/ok.gif");
			fFailureIcon = TestRunner.getIconResource(getClass(), "icons/failure.gif");
		}

		String stripParenthesis(Object o) {
			String text = o.toString();
			int pos = text.indexOf('(');
			if (pos < 1)
				return text;
			return text.substring(0, pos);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {

			Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			TreeModel model = tree.getModel();
			if (model instanceof TestTreeModel) {
				TestTreeModel testModel = (TestTreeModel) model;
				Test t = (Test) value;
				String s = "";
				if (testModel.isFailure(t)) {
					if (fFailureIcon != null)
						setIcon(fFailureIcon);
					s = " - Failed";
				} else if (testModel.isError(t)) {
					if (fErrorIcon != null)
						setIcon(fErrorIcon);
					s = " - Error";
				} else if (testModel.wasRun(t)) {
					if (fOkIcon != null)
						setIcon(fOkIcon);
					s = " - Passed";
				}
				if (c instanceof JComponent)
					((JComponent) c).setToolTipText(getText() + s);
			}
			setText(stripParenthesis(value));
			return c;
		}
	}

	public TestSuitePanel() {
		super(new BorderLayout());
		setPreferredSize(new Dimension(300, 100));
		fTree = new JTree();
		fTree.setModel(null);
		fTree.setRowHeight(20);
		ToolTipManager.sharedInstance().registerComponent(fTree);
		fTree.putClientProperty("JTree.lineStyle", "Angled");
		fScrollTree = new JScrollPane(fTree);
		add(fScrollTree, BorderLayout.CENTER);
	}

	public void addError(final Test test, final Throwable t) {
		fModel.addError(test);
		fireTestChanged(test, true);
	}

	public void addFailure(final Test test, final AssertionFailedError t) {
		fModel.addFailure(test);
		fireTestChanged(test, true);
	}

	/**
	 * A test ended.
	 */
	public void endTest(Test test) {
		fModel.addRunTest(test);
		fireTestChanged(test, false);
	}

	/**
	 * A test started.
	 */
	public void startTest(Test test) {
	}

	/**
	 * Returns the selected test or null if multiple or none is selected
	 */
	public Test getSelectedTest() {
		TreePath[] paths = fTree.getSelectionPaths();
		if (paths != null && paths.length == 1)
			return (Test) paths[0].getLastPathComponent();
		return null;
	}

	/**
	 * Returns the Tree
	 */
	public JTree getTree() {
		return fTree;
	}

	/**
	 * Shows the test hierarchy starting at the given test
	 */
	public void showTestTree(Test root) {
		fModel = new TestTreeModel(root);
		fTree.setModel(fModel);
		fTree.setCellRenderer(new TestTreeCellRenderer());
	}

	private void fireTestChanged(final Test test, final boolean expand) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Vector vpath = new Vector();
				int index = fModel.findTest(test, (Test) fModel.getRoot(), vpath);
				if (index >= 0) {
					Object[] path = new Object[vpath.size()];
					vpath.copyInto(path);
					TreePath treePath = new TreePath(path);
					fModel.fireNodeChanged(treePath, index);
					if (expand) {
						Object[] fullPath = new Object[vpath.size() + 1];
						vpath.copyInto(fullPath);
						fullPath[vpath.size()] = fModel.getChild(treePath.getLastPathComponent(), index);
						TreePath fullTreePath = new TreePath(fullPath);
						fTree.scrollPathToVisible(fullTreePath);
					}
				}
			}
		});
	}
}
