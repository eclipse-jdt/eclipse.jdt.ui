/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.osgi.util.NLS;

public final class CallHierarchyMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchyMessages";//$NON-NLS-1$

	private CallHierarchyMessages() {
		// Do not instantiate
	}

	public static String CallerMethodWrapper_taskname;
	public static String CalleeMethodWrapper_taskname;

	static {
		NLS.initializeMessages(BUNDLE_NAME, CallHierarchyMessages.class);
	}
}