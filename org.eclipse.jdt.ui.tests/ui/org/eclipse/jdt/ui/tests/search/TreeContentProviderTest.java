/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.search;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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

import org.eclipse.jdt.internal.ui.search.JavaSearchResult;
import org.eclipse.jdt.internal.ui.search.JavaSearchResultPage;
import org.eclipse.jdt.internal.ui.search.LevelTreeContentProvider;

/**
 */
public class TreeContentProviderTest extends TestCase {
	private LevelTreeContentProvider fProvider;
	private JavaSearchResult fResult;

	public static Test allTests() {
		return new JUnitSourceSetup(new TestSuite(TreeContentProviderTest.class));
	}

	public static Test suite() {
		return allTests();
	}

	static class MockTreeViewer extends AbstractTreeViewer {

		protected void addTreeListener(Control control, TreeListener listener) {
			// ignore
		}

		protected void doUpdateItem(Item item, Object element) {
			// ignore
		}

		protected Item[] getChildren(Widget widget) {
			return new Item[0];
		}

		protected boolean getExpanded(Item item) {
			return false;
		}

		protected int getItemCount(Control control) {
			return 0;
		}

		protected int getItemCount(Item item) {
			return 0;
		}

		protected Item[] getItems(Item item) {
			return new Item[0];
		}

		protected Item getParentItem(Item item) {
			return null;
		}

		protected Item[] getSelection(Control control) {
			return new Item[0];
		}

		protected Item newItem(Widget parent, int style, int index) {
			return null;
		}

		protected void removeAll(Control control) {
			// ignore
		}

		protected void setExpanded(Item item, boolean expand) {
			// ignore
		}

		protected void setSelection(List items) {
			// ignore
		}

		protected void showItem(Item item) {
			// ignore
		}

		public Control getControl() {
			return null;
		}
	}


	public TreeContentProviderTest(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	protected void setUp() throws Exception {
		super.setUp();
		fProvider= new LevelTreeContentProvider(new JavaSearchResultPage() {
			StructuredViewer fViewer= new MockTreeViewer();
			protected StructuredViewer getViewer() {
				return fViewer;
			}

			public AbstractTextSearchResult getInput() {
				return fResult;
			}
		}, LevelTreeContentProvider.LEVEL_PACKAGE);
		fResult= new JavaSearchResult(null);
		fProvider.inputChanged(null, null, fResult);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSimpleAdd() throws Exception {
		IMethod method= SearchTestHelper.getMethod("junit.framework.TestCase", "getName", new String[0]);
		addMatch(new Match(method, 0, 1));
		IType type= SearchTestHelper.getType("junit.framework.TestCase");
		IPackageFragment pkg= type.getPackageFragment();
		IJavaProject project= pkg.getJavaProject();
		IPackageFragmentRoot root= (IPackageFragmentRoot) pkg.getParent();

		fProvider.setLevel(LevelTreeContentProvider.LEVEL_TYPE);
		assertEquals(type, fProvider.getParent(method));
		assertEquals(fProvider.getParent(type), null);

		fProvider.setLevel(LevelTreeContentProvider.LEVEL_PACKAGE);
		assertEquals(type, fProvider.getParent(method));
		assertEquals(fProvider.getParent(type), pkg);
		assertEquals(fProvider.getParent(pkg), null);

		fProvider.setLevel(LevelTreeContentProvider.LEVEL_PROJECT);
		assertEquals(type, fProvider.getParent(method));
		assertEquals(fProvider.getParent(type), pkg);
		assertEquals(fProvider.getParent(pkg), root);
		assertEquals(fProvider.getParent(root), project);
		assertEquals(fProvider.getParent(project), null);
	}

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
