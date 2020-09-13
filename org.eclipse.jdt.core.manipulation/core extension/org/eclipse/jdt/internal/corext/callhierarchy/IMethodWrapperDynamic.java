/*******************************************************************************
 * Copyright (c) 2019, Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

/**
 * Interface to allow supplying various Object methods dynamically.
 *
 * @since 1.11
 */
public interface IMethodWrapperDynamic {

	/**
	 * Dynamic equals method for MethodWrapper
	 *
	 * @param o1 - MethodWrapper
	 * @param o2 - Object to compare to
	 * @return true if equal, false otherwise
	 */
	boolean equals (MethodWrapper o1, Object o2);

	/**
	 * Dynamic adaptor method for MethodWrapper
	 * @param o1 - MethodWrapper
	 * @param adapter - class to adapt
	 * @return adapted class
	 */
	<T> T getAdapter(MethodWrapper o1, Class<T> adapter);

}
