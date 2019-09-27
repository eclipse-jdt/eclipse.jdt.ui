/*******************************************************************************
 * Copyright (c) 2016 Björn Michael and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Björn Michael <b.michael@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;

/**
 * Filters out members of {@link java.lang.Object}.
 *
 * @author Björn Michael
 * @since 3.13
 */
public class JavaLangObjectMembersFilter extends ViewerFilter {

	private static final String JAVA_LANG_OBJECT_CLASS_NAME= Object.class.getName();

	@Override
	public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
		if (parentElement instanceof IType) {
			if (JAVA_LANG_OBJECT_CLASS_NAME.equals(((IType) parentElement).getFullyQualifiedName())) {
				return true; // java.lang.Object itself is shown; don't filter it out
			}
		}

		if (parentElement instanceof ITypeRoot) {
			IType primaryType= ((ITypeRoot) parentElement).findPrimaryType();
			if (primaryType != null && JAVA_LANG_OBJECT_CLASS_NAME.equals(primaryType.getFullyQualifiedName())) {
				return true; // top-level type is java.lang.Object itself; don't filter it out
			}
		}

		if (element instanceof IMember) {
			IType declaringType= ((IMember) element).getDeclaringType();
			if (declaringType != null) {
				String fullyQualifiedName= declaringType.getFullyQualifiedName();
				if (JAVA_LANG_OBJECT_CLASS_NAME.equals(fullyQualifiedName)) {
					return false;
				}
			}
		}

		return true;
	}

}
