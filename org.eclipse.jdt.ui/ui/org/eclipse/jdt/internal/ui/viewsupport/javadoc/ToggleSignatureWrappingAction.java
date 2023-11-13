/*******************************************************************************
* Copyright (c) 2024 Jozef Tomek and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Jozef Tomek - initial API and implementation
*******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.browser.BrowserTextAccessor;

public class ToggleSignatureWrappingAction extends ToggleSignatureStylingMenuAction {

	public ToggleSignatureWrappingAction(BrowserTextAccessor browserAccessor, String preferenceKeyPrefix) {
		super(ToggleSignatureWrappingAction.class.getSimpleName(),
				JavadocStylingMessages.JavadocStyling_styling_wrapping,
				browserAccessor,
				JavaElementLinks.CHECKBOX_ID_WRAPPING,
				JavaElementLinks::getPreferenceForWrapping,
				JavaElementLinks::setPreferenceForWrapping,
				preferenceKeyPrefix,
				JavaPluginImages.DESC_DLCL_WRAP_ALL,
				JavaPluginImages.DESC_ELCL_WRAP_ALL,
				new JavadocEnrichmentImageDescriptor(JavaPluginImages.DESC_ELCL_WRAP_ALL));
	}
}
