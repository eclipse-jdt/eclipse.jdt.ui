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
import java.util.stream.Stream;

import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks.IStylingConfigurationListener;
import org.eclipse.jdt.internal.ui.viewsupport.MenuVisibilityMenuItemsConfigurer;
import org.eclipse.jdt.internal.ui.viewsupport.MenuVisibilityMenuItemsConfigurer.IMenuVisibilityMenuItemAction;
import org.eclipse.jdt.internal.ui.viewsupport.browser.BrowserTextAccessor;
import org.eclipse.jdt.internal.ui.viewsupport.browser.BrowserTextAccessor.IBrowserContentChangeListener;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserToggler;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxToggleInBrowserAction;

/**
 * Toolbar item action for building & presenting javadoc styling menu.
 */
public class SignatureStylingMenuToolbarAction extends Action implements IMenuCreator, IBrowserContentChangeListener, IStylingConfigurationListener {
	private final Action[] noStylingActions= { new NoStylingEnhancementsAction() };
	private final Action[] enabledActions;
	private final CheckboxInBrowserToggler formattingCheckboxToggler;
	private final Shell parent;
	private final Runnable enhancementsReconfiguredTask;

	private Action[] actions;
	protected Menu menu= null;
	private boolean isEnabledOverride= false;
	private boolean enhancementsEnabled= JavaElementLinks.getStylingEnabledPreference();
	private boolean ignoreRunTask;

	public SignatureStylingMenuToolbarAction(Shell parent, BrowserTextAccessor browserAccessor, Supplier<String> javadocContentSupplier,  Runnable enhancementsReconfiguredTask) {
		super(JavadocStylingMessages.JavadocStyling_enabledTooltip, IAction.AS_DROP_DOWN_MENU);
		Objects.requireNonNull(parent);
		setImageDescriptor(JavaPluginImages.DESC_ETOOL_JDOC_HOVER_EDIT);
		enabledActions= new Action[] {
				new ToggleSignatureTypeParametersColoringAction(browserAccessor),
				// widget for following action is being removed and re-added repeatedly, see SignatureStylingColorSubMenuItem.menuShown()
				new SignatureStylingColorSubMenuItem(parent, javadocContentSupplier, enhancementsReconfiguredTask)};
		actions= enabledActions;
		setMenuCreator(this);
		this.parent= parent;
		this.enhancementsReconfiguredTask= enhancementsReconfiguredTask;
		formattingCheckboxToggler= new CheckboxInBrowserToggler(browserAccessor, JavaElementLinks.CHECKBOX_ID_FORMATTIG);
		presentEnhancementsState();
		setHoverImageDescriptor(null);
		setId(SignatureStylingMenuToolbarAction.class.getSimpleName());
		browserAccessor.addContentChangedListener(this); // remove not necessary since lifecycle of this action is the same as that of the browser widget
		JavaElementLinks.addStylingConfigurationListener(this);
	}

	@Override
	public void browserContentChanged(Supplier<String> contentAccessor) {
		if (!enhancementsEnabled) {
			return;
		}
		var content= contentAccessor.get();
		if (content != null && !content.isBlank() // fail-fast
				&& formattingCheckboxToggler.isCheckboxPresentInBrowser()) {
			reAddActionItems(enabledActions);
		} else {
			reAddActionItems(noStylingActions);
		}
	}

	private void reAddActionItems(Action[] newActions) {
		if (actions != newActions) {
			actions= newActions;
			if (menu != null) {
				Stream.of(menu.getItems()).forEach(MenuItem::dispose);
				addMenuItems();
			}
		}
	}

	@Override
	public boolean isEnabled() {
		if (isEnabledOverride) {
			// method called from org.eclipse.jface.action.ActionContributionItem.handleWidgetSelection() after getMenu() returned NULL
			isEnabledOverride= false;
			return false; // make ActionContributionItem.handleWidgetSelection() to do nothing as reaction to clicking on arrow
		} else {
			return super.isEnabled();
		}
	}

	@Override
	public Menu getMenu(Control p) {
		if (!enhancementsEnabled) {
			isEnabledOverride= true;
			return null;
		}
		if (menu == null) {
			menu= new Menu(parent);
			addMenuItems();
			MenuVisibilityMenuItemsConfigurer.registerForMenu(menu);
			return menu;
		} else {
			return menu;
		}
	}

	@Override
	public Menu getMenu(Menu p) {
		return null;
	}

	private void addMenuItems() {
		Stream.of(actions).forEach(action -> new ActionContributionItem(action).fill(menu, -1));
	}

	@Override
	public void dispose() {
		if (menu != null) {
			menu.dispose();
		}
	}

	@Override
	public void runWithEvent(Event event) {
		enhancementsEnabled = !enhancementsEnabled;
		JavaElementLinks.setStylingEnabledPreference(enhancementsEnabled);
		presentEnhancementsState();
		runEnhancementsReconfiguredTask();
	}

	private void runEnhancementsReconfiguredTask() {
		if (!ignoreRunTask && enhancementsReconfiguredTask != null) {
			enhancementsReconfiguredTask.run();
		}
	}

	private void presentEnhancementsState() {
		setImageDescriptor(enhancementsEnabled ? JavaPluginImages.DESC_ETOOL_JDOC_HOVER_EDIT : JavaPluginImages.DESC_DTOOL_JDOC_HOVER_EDIT);
		setToolTipText(enhancementsEnabled ? JavadocStylingMessages.JavadocStyling_enabledTooltip : JavadocStylingMessages.JavadocStyling_disabledTooltip);
	}

	@Override
	public void stylingStateChanged(boolean isEnabled) {
		parent.getDisplay().execute(() -> {
			if (enhancementsEnabled != isEnabled) {
				enhancementsEnabled= isEnabled;
				presentEnhancementsState();
				runEnhancementsReconfiguredTask();
			}
		});
	}

	@Override
	public void parametersColoringStateChanged(boolean isEnabled) {
		runEnhancementsReconfiguredTask();
	}

	@Override
	public void parametersColorChanged() {
		runEnhancementsReconfiguredTask();
	}

	public void setup(ToolBar toolbar) {
		toolbar.addDisposeListener(e -> JavaElementLinks.removeStylingConfigurationListener(this));
	}

	private class NoStylingEnhancementsAction extends Action {
		public NoStylingEnhancementsAction() {
			super(JavadocStylingMessages.JavadocStyling_noEnhancements);
			setEnabled(false);
		}
	}

	private class ToggleSignatureTypeParametersColoringAction extends CheckboxToggleInBrowserAction implements IMenuVisibilityMenuItemAction {

		public ToggleSignatureTypeParametersColoringAction(BrowserTextAccessor browserAccessor) {
			super(JavadocStylingMessages.JavadocStyling_typeParamsColoring, IAction.AS_CHECK_BOX, browserAccessor, JavaElementLinks.CHECKBOX_ID_TYPE_PARAMETERS_REFERENCES_COLORING);
			setId(ToggleSignatureTypeParametersColoringAction.class.getSimpleName());
			showCurentPreference();
		}

		private void showCurentPreference() {
			setChecked(JavaElementLinks.getPreferenceForTypeParamsColoring());
		}

		@Override
		public void menuShown(MenuEvent e) {
			showCurentPreference();
		}

		@Override
		public void run() {
			super.run();
			ignoreRunTask= true;
			JavaElementLinks.setPreferenceForTypeParamsColoring(isChecked());
			ignoreRunTask= false;
			toggleBrowserCheckbox(isChecked());
			checkboxToggler.getBrowserTextAccessor().applyChanges();
		}

	}
}