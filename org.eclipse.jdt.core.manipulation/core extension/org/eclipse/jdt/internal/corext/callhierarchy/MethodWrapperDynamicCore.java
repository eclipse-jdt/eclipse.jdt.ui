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

import org.eclipse.jdt.core.IJavaElement;

public class MethodWrapperDynamicCore implements IMethodWrapperDynamic {

	@Override
	public boolean equals(MethodWrapper o1, Object o2) {
		if (o1 == o2) {
			return true;
		}

		if (o2 == null) {
			return false;
		}

		if (o2.getClass() != o1.getClass()) {
			return false;
		}

		MethodWrapper other= (MethodWrapper) o2;

		if (o1.getParent() == null) {
			if (other.getParent() != null) {
				return false;
			}
		} else {
			if (!o1.getParent().equals(other.getParent())) {
				return false;
			}
		}

		if (o1.getMethodCall() == null) {
			if (other.getMethodCall() != null) {
				return false;
			}
		} else {
			if (!o1.getMethodCall().equals(other.getMethodCall())) {
				return false;
			}
		}

		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(MethodWrapper wrapper, Class<T> adapter) {
		if (adapter == IJavaElement.class) {
	        return (T) wrapper.getMember();
	    } else {
	        return null;
	    }
	}

}
