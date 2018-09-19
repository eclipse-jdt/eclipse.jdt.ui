/***************************************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 **************************************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.jdt.internal.ui.filters.EmptyPackageFilter;




/**
 * Filters out all empty package fragments unless the mode of the viewer is set to hierarchical
 * layout.
 *
 * This filter is only applicable to instances of the Common Navigator.
 */
public class NonEssentialEmptyPackageFilter extends NonEssentialElementsFilter {

	public NonEssentialEmptyPackageFilter() {
		super(new EmptyPackageFilter());
	}
}
