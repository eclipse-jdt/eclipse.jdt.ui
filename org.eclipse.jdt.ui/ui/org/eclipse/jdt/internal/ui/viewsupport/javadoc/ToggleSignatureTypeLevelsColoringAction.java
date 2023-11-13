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

public class ToggleSignatureTypeLevelsColoringAction extends ToggleSignatureStylingMenuAction {

	public ToggleSignatureTypeLevelsColoringAction(BrowserTextAccessor browserAccessor, String preferenceKeyPrefix) {
		super(ToggleSignatureTypeLevelsColoringAction.class.getSimpleName(),
				JavadocStylingMessages.JavadocStyling_styling_typeParamsLevelsColoring,
				browserAccessor,
				JavaElementLinks.CHECKBOX_ID_TYPE_PARAMETERS_LEVELS_COLORING,
				JavaElementLinks::getPreferenceForTypeParamsLevelsColoring,
				JavaElementLinks::setPreferenceForTypeParamsLevelsColoring,
				preferenceKeyPrefix,
				JavaPluginImages.DESC_DVIEW_TYPES,
				JavaPluginImages.DESC_VIEW_TYPES,
				new JavadocEnrichmentImageDescriptor(JavaPluginImages.DESC_VIEW_TYPES));
	}
}
