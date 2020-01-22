/*******************************************************************************
 * Copyright (c) 2019 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.Collection;

import org.eclipse.jdt.core.search.TypeNameMatch;

/**
 * Note: this is required for (abandoned) Scala-IDE
 *
 * @deprecated Use {@link org.eclipse.jdt.core.manipulation.TypeNameMatchCollector} instead.
 */
@Deprecated
public class TypeNameMatchCollector extends org.eclipse.jdt.core.manipulation.TypeNameMatchCollector {
	public TypeNameMatchCollector(Collection<TypeNameMatch> collection) {
		super(collection);
	}
}
