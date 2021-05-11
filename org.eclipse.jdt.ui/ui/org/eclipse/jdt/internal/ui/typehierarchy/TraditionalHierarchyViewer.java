/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

/**
 * A TypeHierarchyViewer that looks like the type hierarchy view of VA/Java:
 * Starting form Object down to the element in focus, then all subclasses from
 * this element.
 * Used by the TypeHierarchyViewPart which has to provide a TypeHierarchyLifeCycle
 * on construction (shared type hierarchy)
 */
public class TraditionalHierarchyViewer extends TypeHierarchyViewer {

	public TraditionalHierarchyViewer(Composite parent, TypeHierarchyLifeCycle lifeCycle) {
		super(parent, new TraditionalHierarchyContentProvider(lifeCycle), lifeCycle);
	}

	/*
	 * @see TypeHierarchyViewer#updateContent
	 */
	@Override
	public void updateContent(boolean expand) {
		getTree().setRedraw(false);
		refresh();

		if (expand) {
			TraditionalHierarchyContentProvider contentProvider= (TraditionalHierarchyContentProvider) getContentProvider();
			int expandLevel= contentProvider.getExpandLevel();
			if (isMethodFiltering()) {
				expandLevel++;
			}
			expandToLevel(expandLevel);
		}
		getTree().setRedraw(true);
	}

	/**
	 * Content provider for the 'traditional' type hierarchy.
	 */
	public static class TraditionalHierarchyContentProvider extends TypeHierarchyContentProvider {


		public TraditionalHierarchyContentProvider(TypeHierarchyLifeCycle provider) {
			super(provider);
		}

		public int getExpandLevel() {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				IType input= hierarchy.getType();
				if (input != null) {
					return getDepth(hierarchy, input) + 2;
				} else {
					return 5;
				}
			}
			return 2;
		}

		private int getDepth(ITypeHierarchy hierarchy, IType input) {
			int count= 0;
			if (Flags.isInterface(hierarchy.getCachedFlags(input))) {
				IType[] superInterfaces= hierarchy.getSuperInterfaces(input);
				while (superInterfaces != null && superInterfaces.length > 0) {
					count++;
					IType superInterface= superInterfaces[0];
					superInterfaces= hierarchy.getSuperInterfaces(superInterface);
				}

			} else {
				IType superType= hierarchy.getSuperclass(input);
				while (superType != null) {
					count++;
					superType= hierarchy.getSuperclass(superType);
				}
			}
			return count;
		}

		@Override
		protected final void getRootTypes(List<IType> res) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				IType input= hierarchy.getType();
				if (input == null) {
					IType[] classes= hierarchy.getRootClasses();
					res.addAll(Arrays.asList(classes));
					IType[] interfaces= hierarchy.getRootInterfaces();
					res.addAll(Arrays.asList(interfaces));
				} else {
					if (Flags.isInterface(hierarchy.getCachedFlags(input))) {
						IType[] roots= hierarchy.getRootInterfaces();
						res.addAll(Arrays.asList(roots));
					} else if (isAnonymousFromInterface(input)) {
						res.add(hierarchy.getSuperInterfaces(input)[0]);
					} else {
						IType[] roots= hierarchy.getRootClasses();
						for (IType t : roots) {
							if (isObject(t)) {
								res.add(t);
								return;
							}
						}
						res.addAll(Arrays.asList(roots)); // something wrong with the hierarchy
					}
				}
			}
		}

		/*
		 * @see TypeHierarchyContentProvider.getTypesInHierarchy
		 */
		@Override
		protected final void getTypesInHierarchy(IType type, List<IType> res) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				IType[] types= hierarchy.getSubtypes(type);
				if (isObject(type)) {
					for (IType t : types) {
						if (!isAnonymousFromInterface(t)) { // no anonymous classes on 'Object' -> will be children of interface
							res.add(t);
						}
					}
				} else {
					boolean isHierarchyOnType= (hierarchy.getType() != null);
					boolean isClass= !Flags.isInterface(hierarchy.getCachedFlags(type));
					if (isClass || isHierarchyOnType) {
						res.addAll(Arrays.asList(types));
					} else {
						for (IType t : types) {
							// no classes implementing interfaces, only if anonymous
							if (Flags.isInterface(hierarchy.getCachedFlags(t)) || isAnonymous(t)) {
								res.add(t);
							}
						}
					}
				}
			}
		}

		@Override
		protected IType getParentType(IType type) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				if (Flags.isInterface(hierarchy.getCachedFlags(type))) {
					IType[] superInterfaces= hierarchy.getSuperInterfaces(type);
					if (superInterfaces != null && superInterfaces.length > 0) {
						return hierarchy.getSuperInterfaces(type)[0];
					}
					return null;
				}
				return hierarchy.getSuperclass(type);
				// don't handle interfaces
			}
			return null;
		}

	}
}
