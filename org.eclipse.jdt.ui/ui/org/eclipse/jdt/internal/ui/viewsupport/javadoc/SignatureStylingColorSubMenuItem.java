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

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;

/**
 * Menu item action for building &amp; presenting color preferences sub-menu of javadoc styling menu.
 */
class SignatureStylingColorSubMenuItem extends Action implements IMenuCreator {
	private final Shell parentShell;
	private final Supplier<String> javadocContentSupplier;

	private Menu menu= null;

	public SignatureStylingColorSubMenuItem(Shell parent, Supplier<String> javadocContentSupplier) {
		super(JavadocStylingMessages.JavadocStyling_colorPreferences_menu, AS_DROP_DOWN_MENU);
		this.parentShell= Objects.requireNonNull(parent);
		this.javadocContentSupplier= Objects.requireNonNull(javadocContentSupplier);
		setMenuCreator(this);
	}

	@Override
	public Menu getMenu(Menu parent) {
		// we keep it simple here and just re-create new menu with correct items
		dispose();
		var content= javadocContentSupplier.get();
		if (content != null) {
			menu= new Menu(parent);

			new ActionContributionItem(new ResetSignatureStylingColorsPreferencesMenuItem()).fill(menu, -1);
			new Separator().fill(menu, -1);

			int typeParamsReferencesCount= JavaElementLinks.getNumberOfTypeParamsReferences(content);
			if (typeParamsReferencesCount == 0) {
				new ActionContributionItem(new NoSignatureStylingTypeParametersMenuItem()).fill(menu, -1);
			} else {
				for (int i= 1; i <= typeParamsReferencesCount; i++) {
					var item= new ActionContributionItem(new SignatureStylingColorPreferenceMenuItem(
							parentShell,
							JavadocStylingMessages.JavadocStyling_colorPreferences_typeParameter,
							i,
							JavaElementLinks::getColorPreferenceForTypeParamsReference,
							JavaElementLinks::setColorPreferenceForTypeParamsReference));
					item.fill(menu, -1);
				}
			}

			var typeParamsReferenceIndices= JavaElementLinks.getColorPreferencesIndicesForTypeParamsReference();

			if (typeParamsReferenceIndices[typeParamsReferenceIndices.length - 1] > typeParamsReferencesCount) {
				new Separator().fill(menu, -1);
			}

			for (int i= 0; i < typeParamsReferenceIndices.length; i++) {
				if (typeParamsReferenceIndices[i] > typeParamsReferencesCount) {
					new ActionContributionItem(new UnusedColorPreferenceMenuItem(
							JavadocStylingMessages.JavadocStyling_colorPreferences_typeParameter, typeParamsReferenceIndices[i]))
						.fill(menu, -1);
				}
			}
		}
		return menu;
	}

	@Override
	public Menu getMenu(Control parent) {
		return null;
	}

	@Override
	public void dispose() {
		if (menu != null) {
			menu.dispose();
			menu= null;
		}
	}

	private static final class ResetSignatureStylingColorsPreferencesMenuItem extends Action {
		public ResetSignatureStylingColorsPreferencesMenuItem() {
			super(JavadocStylingMessages.JavadocStyling_colorPreferences_resetAll);
			setId(ResetSignatureStylingColorsPreferencesMenuItem.class.getSimpleName());
		}

		@Override
		public void run() {
			JavaElementLinks.resetAllColorPreferencesToDefaults(); // triggers call to SignatureStylingMenuToolbarAction.parametersColorChanged()
		}
	}

	private static final class NoSignatureStylingTypeParametersMenuItem extends Action {
		public NoSignatureStylingTypeParametersMenuItem() {
			super(JavadocStylingMessages.JavadocStyling_colorPreferences_noTypeParameters);
			setId(NoSignatureStylingTypeParametersMenuItem.class.getSimpleName());
			setEnabled(false);
		}
	}

	private static final class UnusedColorPreferenceMenuItem extends Action {
		public UnusedColorPreferenceMenuItem(String textPrefix, int colorIdx) {
			super(Messages.format(textPrefix, colorIdx));
			setId(UnusedColorPreferenceMenuItem.class.getSimpleName() + "_" + colorIdx); //$NON-NLS-1$
			setToolTipText(JavadocStylingMessages.JavadocStyling_colorPreferences_unusedTypeParameter);
			setEnabled(false);
		}
	}
}