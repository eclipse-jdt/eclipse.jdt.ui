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

public class ToggleSignatureFormattingAction extends ToggleSignatureStylingMenuAction {

	public ToggleSignatureFormattingAction(BrowserTextAccessor browserAccessor, String preferenceKeyPrefix) {
	super(ToggleSignatureFormattingAction.class.getSimpleName(),
			JavadocStylingMessages.JavadocStyling_styling_formatting,
			browserAccessor,
			JavaElementLinks.CHECKBOX_ID_FORMATTIG,
			JavaElementLinks::getPreferenceForFormatting,
			JavaElementLinks::setPreferenceForFormatting,
			preferenceKeyPrefix,
			JavaPluginImages.DESC_DLCL_TEMPLATE,
			JavaPluginImages.DESC_ELCL_TEMPLATE,
			new JavadocEnrichmentImageDescriptor(JavaPluginImages.DESC_ELCL_TEMPLATE));
	}
}
