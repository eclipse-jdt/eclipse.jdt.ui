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

public class ToggleSignatureTypeParametersColoringAction extends ToggleSignatureStylingMenuAction {

	public ToggleSignatureTypeParametersColoringAction(BrowserTextAccessor browserAccessor, String preferenceKeyPrefix) {
		super(ToggleSignatureTypeParametersColoringAction.class.getSimpleName(),
				JavadocStylingMessages.JavadocStyling_styling_typeParamsReferencesColoring,
				browserAccessor,
				JavaElementLinks.CHECKBOX_ID_TYPE_PARAMETERS_REFERENCES_COLORING,
				JavaElementLinks::getPreferenceForTypeParamsReferencesColoring,
				JavaElementLinks::setPreferenceForTypeParamsReferencesColoring,
				preferenceKeyPrefix,
				JavaPluginImages.DESC_DVIEW_MEMBERS,
				JavaPluginImages.DESC_VIEW_MEMBERS,
				new JavadocEnrichmentImageDescriptor(JavaPluginImages.DESC_VIEW_MEMBERS));
	}

}
