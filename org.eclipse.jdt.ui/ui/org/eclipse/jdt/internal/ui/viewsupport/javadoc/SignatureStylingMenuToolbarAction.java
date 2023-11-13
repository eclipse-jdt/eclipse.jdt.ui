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

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.MenuVisibilityMenuItemsConfigurer;
import org.eclipse.jdt.internal.ui.viewsupport.ArmListeningMenuItemsConfigurer;
import org.eclipse.jdt.internal.ui.viewsupport.MouseListeningToolItemsConfigurer.IMouseListeningToolbarItemAction;
import org.eclipse.jdt.internal.ui.viewsupport.ReappearingMenuToolbarAction;
import org.eclipse.jdt.internal.ui.viewsupport.browser.BrowserTextAccessor;
import org.eclipse.jdt.internal.ui.viewsupport.browser.BrowserTextAccessor.IBrowserContentChangeListener;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserToggler;
import org.eclipse.jdt.internal.ui.viewsupport.browser.HoverStylingInBrowserMenuAction;

/**
 * Toolbar item action for building & presenting javadoc styling menu.
 */
public class SignatureStylingMenuToolbarAction extends ReappearingMenuToolbarAction implements IMouseListeningToolbarItemAction, MenuListener, IBrowserContentChangeListener {
	private final CheckboxInBrowserToggler previewCheckboxToggler;
	private boolean mouseExitCalled= false;

	public SignatureStylingMenuToolbarAction(Shell parent, BrowserTextAccessor browserAccessor, String preferenceKeyPrefix, Supplier<String> javadocContentSupplier) {
		super(JavadocStylingMessages.JavadocStyling_stylingMenu, JavaPluginImages.DESC_ETOOL_JDOC_HOVER_EDIT,
				new ToggleSignatureTypeParametersColoringAction(browserAccessor, preferenceKeyPrefix),
				new ToggleSignatureTypeLevelsColoringAction(browserAccessor, preferenceKeyPrefix),
				new ToggleSignatureFormattingAction(browserAccessor, preferenceKeyPrefix),
				new ToggleSignatureWrappingAction(browserAccessor, preferenceKeyPrefix),
				// widget for following action is being removed and re-added repeatedly, see SignatureStylingColorSubMenuItem.menuShown()
				new SignatureStylingColorSubMenuItem(parent, javadocContentSupplier));
		previewCheckboxToggler= new CheckboxInBrowserToggler(browserAccessor, JavaElementLinks.CHECKBOX_ID_PREVIEW);
		setId(SignatureStylingMenuToolbarAction.class.getSimpleName());
		// disable until content is loaded in browser
		setEnabled(false);
		setToolTipText(JavadocStylingMessages.JavadocStyling_noEnhancementsTooltip);
		browserAccessor.addContentChangedListener(this);

		// make sure actions have loaded preferences for hover to work
		Stream.of(actions)
			.filter(HoverStylingInBrowserMenuAction.class::isInstance)
			.forEach(a -> ((HoverStylingInBrowserMenuAction) a).loadCurentPreference());
	}

	@Override
	public void browserContentChanged(Supplier<String> contentAccessor) {
		var content= contentAccessor.get();
		if (content != null && !content.isBlank() // fail-fast
				&& previewCheckboxToggler.isCheckboxPresentInBrowser()) {
			setEnabled(true);
			setToolTipText(null);
		} else {
			setEnabled(false);
			setToolTipText(JavadocStylingMessages.JavadocStyling_noEnhancementsTooltip);
		}
	}

	@Override
	public Menu getMenu(Control p) {
		if (!menuCreated()) {
			Menu retVal= super.getMenu(p);
			Runnable browserContentSetter= previewCheckboxToggler.getBrowserTextAccessor()::applyChanges;
			ArmListeningMenuItemsConfigurer.registerForMenu(retVal, browserContentSetter);
			MenuVisibilityMenuItemsConfigurer.registerForMenu(retVal, browserContentSetter);
			retVal.addMenuListener(this); // must be last listener, since it commits browser text changes
			return retVal;
		} else {
			return super.getMenu(p);
		}
	}

	@Override
	public boolean mouseEnter(Event event) {
		if (!isEnabled()) {
			return false;
		}
		boolean retVal= false;
		for (Action action : actions) {
			if (action instanceof HoverStylingInBrowserMenuAction menuAction) {
				retVal |= menuAction.menuButtonMouseEnter(event);
			}
		}
		mouseExitCalled= false;
		return retVal;
	}

	@Override
	public boolean mouseExit(Event event) {
		if (!isEnabled()) {
			return false;
		}
		for (Action action : actions) {
			if (action instanceof HoverStylingInBrowserMenuAction menuAction) {
				menuAction.menuButtonMouseExit(event);
			}
		}
		return mouseExitCalled= true;
	}

	@Override
	public void runWithEvent(Event event) {
		// simulate opening menu with arrow
		Rectangle bounds= ((ToolItem) event.widget).getBounds();
		event.x= bounds.x;
		event.y= bounds.y + bounds.height;
		event.detail= SWT.ARROW;
		((ToolItem) event.widget).notifyListeners(SWT.Selection, event);
	}

	@Override
	public void menuShown(MenuEvent e) {
		toggleBrowserPreviewCheckbox(true);
		previewCheckboxToggler.getBrowserTextAccessor().applyChanges();
	}

	@Override
	public void menuHidden(MenuEvent e) {
		toggleBrowserPreviewCheckbox(false);
		if (mouseExitCalled) {
			// mouseExit() is not called after this when menu is being hidden after re-appearing, so trigger applyChanges() here
			previewCheckboxToggler.getBrowserTextAccessor().applyChanges();
		} // else applyChanges() will be triggered from mouseExit() that will be executed after this
	}

	private void toggleBrowserPreviewCheckbox(boolean enabled) {
		previewCheckboxToggler.toggleCheckboxInBrowser(enabled);
	}
}