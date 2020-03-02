/*******************************************************************************
 * Copyright (c) 2020 Andrey Loskutov <loskutov@gmx.de>.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.actions;

import java.util.function.Supplier;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.ui.search.ElementQuerySpecification;

class LazyScopeQuerySpecification extends ElementQuerySpecification {

	private Supplier<IJavaSearchScope> scopeSupplier;
	private IJavaSearchScope scope;

	public LazyScopeQuerySpecification(IJavaElement javaElement, int limitTo, Supplier<IJavaSearchScope> supplier, String scopeDescription) {
		super(javaElement, limitTo, null, scopeDescription);
		scopeSupplier= supplier;
	}

	@Override
	public IJavaSearchScope getScope() {
		if(scope == null) {
			scope = scopeSupplier.get();
		}
		return scope;
	}
}