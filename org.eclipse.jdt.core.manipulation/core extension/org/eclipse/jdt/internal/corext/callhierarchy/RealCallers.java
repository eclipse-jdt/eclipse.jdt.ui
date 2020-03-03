/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.callhierarchy;

/**
 * Class for the real callers.
 *
 * @since 3.5
 */
	public class RealCallers extends CallerMethodWrapper {

		/**
		 * Sets the parent method wrapper.
		 *
		 * @param methodWrapper the method wrapper
		 * @param methodCall the method call
		 */
		public RealCallers(MethodWrapper methodWrapper, MethodCall methodCall) {
			super(methodWrapper, methodCall);
	}

		@Override
		public boolean canHaveChildren() {
			return true;
	}

		@Override
		public boolean isRecursive() {
			return false;
		}
	}
