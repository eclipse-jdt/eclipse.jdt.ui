/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

/**
 * A viewer including the content provider for the subtype hierarchy.
 * Used by the TypeHierarchyViewPart which has to provide a TypeHierarchyLifeCycle
 * on construction (shared type hierarchy)
 */
public class SubTypeHierarchyViewer extends TypeHierarchyViewer {

	public SubTypeHierarchyViewer(Composite parent, TypeHierarchyLifeCycle lifeCycle) {
		super(parent, new SubTypeHierarchyContentProvider(lifeCycle), lifeCycle);
	}

	@Override
	protected void hookUpdateContent(boolean expand) {
		getTree().setRedraw(false);
		refresh();

		if (expand) {
			int expandLevel= 2;
			if (isMethodFiltering()) {
				expandLevel++;
			}
			expandToLevel(expandLevel);
		}
		getTree().setRedraw(true);
	}

	/**
	 * Content provider for the subtype hierarchy
	 */
	public static class SubTypeHierarchyContentProvider extends TypeHierarchyContentProvider {
		public SubTypeHierarchyContentProvider(TypeHierarchyLifeCycle lifeCycle) {
			super(lifeCycle);
		}

		@Override
		protected final void getTypesInHierarchy(IType type, List<IType> res) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				IType[] types= hierarchy.getSubtypes(type);
				if (isObject(type)) {
					for (IType t : types) {
						if (!isAnonymousFromInterface(t)) {
							res.add(t);
						}
					}
				} else {
					res.addAll(Arrays.asList(types));
				}
			}

		}

		@Override
		protected IType getParentType(IType type) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				return hierarchy.getSuperclass(type);
				// dont handle interfaces
			}
			return null;
		}

}



}
