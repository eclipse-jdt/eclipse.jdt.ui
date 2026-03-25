/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import org.eclipse.osgi.service.debug.DebugOptions;

import org.eclipse.jdt.internal.core.JavaModelManager;

public class TestUtils {


	public static void waitForIndexer() {
		JavaModelManager.getIndexManager().waitForIndex(false, null);
	}

	/**
	 * Enables or disables debug traces.
	 * @param classFromBundle A class from the bundle for which the debug tracing should be enabled or disabled.
	 * @param enable whether to enable or disable debug tracing
	 */
	public static void setDebugEnabled(Class<?> classFromBundle, boolean enable) {
		Bundle bundle= FrameworkUtil.getBundle(classFromBundle);
		BundleContext context= bundle.getBundleContext();
		ServiceReference<DebugOptions> reference= context.getServiceReference(DebugOptions.class);
		try {
			DebugOptions options= context.getService(reference);
			options.setDebugEnabled(enable);
			String key= bundle.getSymbolicName() + "/debug";
			options.setOption(key, enable ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		} finally {
			context.ungetService(reference);
		}
	}
}
