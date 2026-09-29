/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.INavigatorContentExtension;

/**
 * Provides JDT labels for resources contributed by the generic resource
 * content extension.
 */
public class JavaResourceNavigatorLabelProvider extends JavaNavigatorLabelProvider {

	@Override
	INavigatorContentExtension getContentExtension(ICommonContentExtensionSite commonContentExtensionSite) {
		return commonContentExtensionSite.getService().getContentExtensionById(JavaNavigatorContentProvider.JDT_EXTENSION_ID);
	}
}
