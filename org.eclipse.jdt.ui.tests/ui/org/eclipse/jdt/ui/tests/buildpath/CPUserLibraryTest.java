/*******************************************************************************
 * Copyright (c) 2018-2020 GK Software SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.buildpath;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementSorter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListLabelProvider;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPUserLibraryElement;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

public class CPUserLibraryTest {

	/** Make {@link #getSortedChildren(Object)} accessible. */
	static class MyTreeViewer extends TreeViewer {
		public MyTreeViewer(Tree tree) {
			super(tree);
		}
		@Override
		public Object[] getSortedChildren(Object parentElementOrTreePath) {
			return super.getSortedChildren(parentElementOrTreePath);
		}
	}
	/** Install our own {@link MyTreeViewer}.
	 * @param <E> the type of the root elements.
	 */
	static class MyTreeListDialogField<E> extends TreeListDialogField<E> {
		public MyTreeListDialogField(ITreeListAdapter<E> adapter, String[] buttonLabels, ILabelProvider lprovider) {
			super(adapter, buttonLabels, lprovider);
		}
		@Override
		protected MyTreeViewer createTreeViewer(Composite parent) {
			Tree tree= new Tree(parent, getTreeStyle());
			tree.setFont(parent.getFont());
			return new MyTreeViewer(tree);
		}
		@Override
		public MyTreeViewer getTreeViewer() {
			return (MyTreeViewer) super.getTreeViewer();
		}
	}

	@Test
	public void testUserLibrarySorting() {
		Shell parent= new Shell();
		try {
			String[] buttonLabels = { "OK", "CANCEL" };
			MyTreeListDialogField<CPUserLibraryElement> listDialogField= new MyTreeListDialogField<>(null, buttonLabels, new CPListLabelProvider());
			listDialogField.setElements(getLibraryElements("BLIB", "CLIB", "ALIB"));
			listDialogField.setViewerComparator(new CPListElementSorter());
			listDialogField.getTreeControl(parent);
			MyTreeViewer treeViewer= listDialogField.getTreeViewer();
			Object[] sortedChildren= treeViewer.getSortedChildren(listDialogField);
			String result = Arrays.stream(sortedChildren)
					.map(e -> ((CPUserLibraryElement)e).getName())
					.collect(Collectors.joining(":"));
			assertEquals("Unexpected element", "ALIB:BLIB:CLIB", result);
		} finally {
			parent.dispose();
		}
	}

	private List<CPUserLibraryElement> getLibraryElements(String... libNames) {
		return Arrays.stream(libNames)
				.map(n -> new CPUserLibraryElement(n, false, null))
				.collect(Collectors.toList());
	}
}
