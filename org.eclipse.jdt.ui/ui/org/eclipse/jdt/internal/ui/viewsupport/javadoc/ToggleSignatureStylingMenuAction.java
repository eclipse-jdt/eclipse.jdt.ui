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

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.MenuEvent;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.viewsupport.browser.BrowserTextAccessor;
import org.eclipse.jdt.internal.ui.viewsupport.browser.HoverStylingInBrowserMenuAction;

/**
 * Base class for signature styling menu item actions that toggle specific styling inside browser HTML content controlled by specified checkbox
 * and support mouse hovering and {@link org.eclipse.jdt.internal.ui.viewsupport.browser.HoverPreferenceStylingInBrowserAction.StylingPreference} states
 * persisted in preference store and presented via different icons.
 */
public class ToggleSignatureStylingMenuAction extends HoverStylingInBrowserMenuAction {
	final Function<String, StylingPreference> stylingPreferenceGetter;
	final BiConsumer<String, StylingPreference> stylingPreferenceSaver;
	final String preferenceKeyPrefix;
	final ImageDescriptor preferenceOffImage;
	final ImageDescriptor preferenceAlwaysImage;
	final ImageDescriptor preferenceHoverImage;

	public ToggleSignatureStylingMenuAction(String id, String text, BrowserTextAccessor browserAccessor, String checkboxId,
			Function<String, StylingPreference> preferenceGetter, BiConsumer<String, StylingPreference> preferenceSaver, String preferenceKeyPrefix,
			ImageDescriptor toggleOffImage, ImageDescriptor toggleOnImage, ImageDescriptor preferenceHoverImage) {
		super(text, browserAccessor, checkboxId);
		this.stylingPreferenceGetter= preferenceGetter;
		this.stylingPreferenceSaver= preferenceSaver;
		this.preferenceKeyPrefix= preferenceKeyPrefix;
		this.preferenceOffImage= toggleOffImage;
		this.preferenceAlwaysImage= toggleOnImage;
		this.preferenceHoverImage= preferenceHoverImage;
		loadCurentPreference();
		setId(id);
	}

	@Override
	protected void presentCurrentPreference() {
		setImageDescriptor(
				switch (currentPreference) {
					case OFF -> preferenceOffImage;
					case HOVER ->  preferenceHoverImage;
					case ALWAYS -> preferenceAlwaysImage;
				});
		String preference= switch (currentPreference) {
			case OFF -> JavadocStylingMessages.JavadocStyling_stylingTooltip_preference_off;
			case HOVER -> JavadocStylingMessages.JavadocStyling_stylingTooltip_preference_hover;
			case ALWAYS -> JavadocStylingMessages.JavadocStyling_stylingTooltip_preference_always;
		};
		setToolTipText(Messages.format(JavadocStylingMessages.JavadocStyling_stylingTooltip_prefix, preference));
	}

	@Override
	protected StylingPreference getPreferenceFromStore() {
		return stylingPreferenceGetter.apply(preferenceKeyPrefix);
	}

	@Override
	protected void putPreferenceToStore(StylingPreference preference) {
		stylingPreferenceSaver.accept(preferenceKeyPrefix, preference);
	}

	@Override
	public boolean itemUnarmed(ArmEvent event) {
		if (event != null) {
			return super.itemUnarmed(event);
		} else { // menu is just being closed
			return false; // do not trigger browser text changes commit, done in SignatureStylingMenuToolbarAction.menuHidden()
		}
	}

	@Override
	public boolean menuHidden(MenuEvent e) {
		super.menuHidden(e); // ignore return value
		return false; // do not trigger browser text changes commit, done in SignatureStylingMenuToolbarAction.menuHidden()
	}
}