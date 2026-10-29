/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc. - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.tm4e;

import org.osgi.framework.BundleContext;

import org.eclipse.core.runtime.Plugin;

import org.eclipse.jdt.core.manipulation.internal.javadoc.CoreMarkdownAccessImpl;

/**
 * Activator for the tm4e integration bundle. Registers the
 * {@link TM4EFencedCodeTokenizer} when the bundle starts and unregisters it
 * when the bundle stops.
 */
public class TM4EPlugin extends Plugin {

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		CoreMarkdownAccessImpl.setFencedCodeTokenizer(new TM4EFencedCodeTokenizer());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		CoreMarkdownAccessImpl.setFencedCodeTokenizer(null);
		super.stop(context);
	}
}
