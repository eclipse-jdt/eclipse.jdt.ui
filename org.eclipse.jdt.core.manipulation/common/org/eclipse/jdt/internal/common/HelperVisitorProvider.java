/*******************************************************************************
 * Copyright (c) 2021 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer
 *******************************************************************************/
package org.eclipse.jdt.internal.common;

/**
 *
 * @author chammer
 * @param <V>
 * @param <T>
 * @param <E>
 * @since 1.17
 *
 */
public interface HelperVisitorProvider<V,T,E extends HelperVisitorProvider<V, T, E>> {
	/**
	 * @return
	 */
	HelperVisitor<E,V,T> getHelperVisitor();
	/**
	 * @param hv
	 */
	void setHelperVisitor(HelperVisitor<E,V,T> hv);
}
