/*******************************************************************************
 * Copyright (c) 2017 Björn Michael and others.
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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Filters out deprecated fields and methods.
 *
 * @author Björn Michael
 * @since 3.13
 */
public class DeprecatedFieldsAndMethodsFilter extends ViewerFilter {

	@Override
	public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
		if (element instanceof IMember) {
			IMember member= (IMember) element;
			if (member instanceof IField || member instanceof IMethod) {
				try {
					return !Flags.isDeprecated(member.getFlags());
				} catch (final JavaModelException e) {
					// flags aren't determinable so let element through this filter
				}
			}
		}
		return true;
	}

}
