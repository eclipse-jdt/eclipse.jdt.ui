/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

/**
 * A viewer including the content provider for the supertype hierarchy.
 * Used by the TypeHierarchyViewPart which has to provide a TypeHierarchyLifeCycle
 * on construction (shared type hierarchy)
 */
public class SuperTypeHierarchyViewer extends TypeHierarchyViewer {

	public SuperTypeHierarchyViewer(Composite parent, TypeHierarchyLifeCycle lifeCycle) {
		super(parent, new SuperTypeHierarchyContentProvider(lifeCycle), lifeCycle);
	}

	/*
	 * @see TypeHierarchyViewer#updateContent
	 */
	@Override
	public void updateContent(boolean expand) {
		getTree().setRedraw(false);
		refresh();
		if (expand) {
			HashSet<Object> visited= new HashSet<>();
			TreeItem[] rootNodes= getTree().getItems();
			for (TreeItem rootNode : rootNodes) {
				expandNode(rootNode, visited);
			}
		}
		getTree().setRedraw(true);
	}

	private void expandNode(TreeItem treeItem, HashSet<Object> visited) {
		internalExpandToLevel(treeItem, 1);
		visited.add(treeItem.getData());
		for (TreeItem child : treeItem.getItems()) {
			if (!visited.contains(child.getData())) {
				expandNode(child, visited);
			}
		}
	}

	/*
	 * Content provider for the supertype hierarchy
	 */
	public static class SuperTypeHierarchyContentProvider extends TypeHierarchyContentProvider {
		public SuperTypeHierarchyContentProvider(TypeHierarchyLifeCycle lifeCycle) {
			super(lifeCycle);
		}

		@Override
		protected final void getTypesInHierarchy(IType type, List<IType> res) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				IType[] types= hierarchy.getSupertypes(type);
				res.addAll(Arrays.asList(types));
			}
		}

		@Override
		protected IType getParentType(IType type) {
			// cant handle
			return null;
		}

	}

}
