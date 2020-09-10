/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

package org.eclipse.jdt.ui.tests.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.tests.core.rules.JUnitSourceSetup;

import org.eclipse.jdt.internal.ui.search.JavaSearchResult;
import org.eclipse.jdt.internal.ui.search.JavaSearchResultPage;
import org.eclipse.jdt.internal.ui.search.LevelTreeContentProvider;

/**
 */
public class TreeContentProviderTest {

	@Rule
	public JUnitSourceSetup projectSetup = new JUnitSourceSetup();

	private LevelTreeContentProvider fProvider;
	private JavaSearchResult fResult;

	static class MockTreeViewer extends AbstractTreeViewer {

		@Override
		protected void addTreeListener(Control control, TreeListener listener) {
			// ignore
		}

		@Override
		protected void doUpdateItem(Item item, Object element) {
			// ignore
		}

		@Override
		protected Item[] getChildren(Widget widget) {
			return new Item[0];
		}

		@Override
		protected boolean getExpanded(Item item) {
			return false;
		}

		@Override
		protected int getItemCount(Control control) {
			return 0;
		}

		@Override
		protected int getItemCount(Item item) {
			return 0;
		}

		@Override
		protected Item[] getItems(Item item) {
			return new Item[0];
		}

		@Override
		protected Item getParentItem(Item item) {
			return null;
		}

		@Override
		protected Item[] getSelection(Control control) {
			return new Item[0];
		}

		@Override
		protected Item newItem(Widget parent, int style, int index) {
			return null;
		}

		@Override
		protected void removeAll(Control control) {
			// ignore
		}

		@Override
		protected void setExpanded(Item item, boolean expand) {
			// ignore
		}

		@Override
		protected void setSelection(List items) {
			// ignore
		}

		@Override
		protected void showItem(Item item) {
			// ignore
		}

		@Override
		public Control getControl() {
			return null;
		}
	}

	@Before
	public void setUp() throws Exception {
		fProvider= new LevelTreeContentProvider(new JavaSearchResultPage() {
			StructuredViewer fViewer= new MockTreeViewer();
			@Override
			protected StructuredViewer getViewer() {
				return fViewer;
			}

			@Override
			public AbstractTextSearchResult getInput() {
				return fResult;
			}
		}, LevelTreeContentProvider.LEVEL_PACKAGE);
		fResult= new JavaSearchResult(null);
		fProvider.inputChanged(null, null, fResult);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSimpleAdd() throws Exception {
		IMethod method= SearchTestHelper.getMethod("junit.framework.TestCase", "getName", new String[0]);
		addMatch(new Match(method, 0, 1));
		IType type= SearchTestHelper.getType("junit.framework.TestCase");
		IPackageFragment pkg= type.getPackageFragment();
		IJavaProject project= pkg.getJavaProject();
		IPackageFragmentRoot root= (IPackageFragmentRoot) pkg.getParent();

		fProvider.setLevel(LevelTreeContentProvider.LEVEL_TYPE);
		assertEquals(type, fProvider.getParent(method));
		assertNull(fProvider.getParent(type));

		fProvider.setLevel(LevelTreeContentProvider.LEVEL_PACKAGE);
		assertEquals(type, fProvider.getParent(method));
		assertEquals(fProvider.getParent(type), pkg);
		assertNull(fProvider.getParent(pkg));

		fProvider.setLevel(LevelTreeContentProvider.LEVEL_PROJECT);
		assertEquals(type, fProvider.getParent(method));
		assertEquals(fProvider.getParent(type), pkg);
		assertEquals(fProvider.getParent(pkg), root);
		assertEquals(fProvider.getParent(root), project);
		assertNull(fProvider.getParent(project));
	}

	@Test
	public void testRemove() throws Exception {
		IMethod method= SearchTestHelper.getMethod("junit.framework.TestCase", "getName", new String[0]);
		IType type= method.getDeclaringType();
		IPackageFragment pkg= type.getPackageFragment();
		Match match= new Match(method, 0, 1);
		addMatch(match);
		Match match2= new Match(method, 0, 1);
		addMatch(match2);
		removeMatch(match);
		assertEquals(1, fProvider.getChildren(type).length);
		assertEquals(1, fProvider.getChildren(pkg).length);
		assertEquals(1, fProvider.getElements(fResult).length);

		removeMatch(match2);

		assertEquals(0, fProvider.getChildren(type).length);
		assertEquals(0, fProvider.getChildren(pkg).length);
		assertEquals(0, fProvider.getElements(fResult).length);
	}

	@Test
	public void testRemoveParentFirst() throws Exception {
		IMethod method= SearchTestHelper.getMethod("junit.framework.TestCase", "getName", new String[0]);
		IType type= method.getDeclaringType();
		IPackageFragment pkg= type.getPackageFragment();

		Match match1= new Match(method, 0, 1);
		addMatch(match1);

		Match match2= new Match(type, 0, 1);
		addMatch(match2);

		removeMatch(match2);

		assertEquals(1, fProvider.getChildren(type).length);
		assertEquals(1, fProvider.getChildren(pkg).length);
		assertEquals(1, fProvider.getElements(fResult).length);

		removeMatch(match1);
		assertEquals(0, fProvider.getChildren(type).length);
		assertEquals(0, fProvider.getChildren(pkg).length);
		assertEquals(0, fProvider.getElements(fResult).length);
	}

	@Test
	public void testRemoveParentLast() throws Exception {
		IMethod method= SearchTestHelper.getMethod("junit.framework.TestCase", "getName", new String[0]);
		IType type= method.getDeclaringType();
		IPackageFragment pkg= type.getPackageFragment();

		Match match1= new Match(method, 0, 1);
		addMatch(match1);

		Match match2= new Match(type, 0, 1);
		addMatch(match2);

		removeMatch(match1);

		assertEquals(0, fProvider.getChildren(type).length);
		assertEquals(1, fProvider.getChildren(pkg).length);
		assertEquals(1, fProvider.getElements(fResult).length);

		removeMatch(match2);

		assertEquals(0, fProvider.getChildren(type).length);
		assertEquals(0, fProvider.getChildren(pkg).length);
		assertEquals(0, fProvider.getElements(fResult).length);
	}

	private void removeMatch(Match match) {
		fResult.removeMatch(match);
		fProvider.elementsChanged(new Object[] { match.getElement() });
	}

	private void addMatch(Match match) {
		fResult.addMatch(match);
		fProvider.elementsChanged(new Object[] { match.getElement() });
	}

}
