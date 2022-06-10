/*******************************************************************************
 * Copyright (c) 2021, 2022 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.common;

/**
 *
 * @author chammer
 * @param <V> - type of key in HelperVisitor mapping
 * @param <T> - type of value in HelperVisitor mapping
 * @param <E> - type that extends HelperVisitorProvider providing HelperVisitor mapping V -> T
 *
 */
public interface HelperVisitorProvider<V, T, E extends HelperVisitorProvider<V, T, E>> {
	/**
	 * @return - HelperVisitor
	 */
	HelperVisitor<E, V, T> getHelperVisitor();

	/**
	 * @param hv - HelperVisitor
	 */
	void setHelperVisitor(HelperVisitor<E, V, T> hv);
}
