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
import java.util.stream.Stream;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.internal.text.html.BrowserInformationControlInput;

import org.eclipse.jface.text.IInputChangedListener;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks.IStylingConfigurationListener;

/**
 * Toolbar item action for building &amp; presenting javadoc styling menu.
 */
public class SignatureStylingMenuToolbarAction extends Action implements IMenuCreator, IInputChangedListener, IStylingConfigurationListener {
	private final Action[] noStylingActions= { new NoStylingEnhancementsAction() };
	private final Action[] enabledActions;
	private final Shell parent;
	private final Runnable enhancementsReconfiguredTask;

	private Action[] actions;
	protected Menu menu= null;
	private boolean enhancementsEnabled= JavaElementLinks.getStylingEnabledPreference();
	private String javadocContent;

	public SignatureStylingMenuToolbarAction(Shell parent, JavadocContentInputAccessor contentInputAccessor, Runnable enhancementsReconfiguredTask) {
		super(JavadocStylingMessages.JavadocStyling_enabledTooltip, IAction.AS_DROP_DOWN_MENU);
		Objects.requireNonNull(parent);
		Objects.requireNonNull(contentInputAccessor);
		setImageDescriptor(JavaPluginImages.DESC_ETOOL_JDOC_HOVER_EDIT);
		// SignatureStylingColorSubMenuItem requires top level shell to display native color picker
		// JavadocView passes top level shell but JavadocHover passes hover's shell
		// (Display.getActiveShell() would not work since JavadocView is created when active shell is startup splash screen shell)
		Shell topLevelShell = (parent.getParent() instanceof Shell parentShell) ? parentShell : parent;
		enabledActions= new Action[] {
				new ToggleSignatureTypeParametersColoringAction(),
				new SignatureStylingColorSubMenuItem(topLevelShell, () -> this.javadocContent)};
		actions= noStylingActions;
		setMenuCreator(this);
		this.parent= parent;
		this.enhancementsReconfiguredTask= enhancementsReconfiguredTask;
		presentEnhancementsState();
		setHoverImageDescriptor(null);
		setId(SignatureStylingMenuToolbarAction.class.getSimpleName());
		contentInputAccessor.addInputChangedListener(this);
		JavaElementLinks.addStylingConfigurationListener(this);
	}

	@Override
	public void inputChanged(Object newInput) {
		javadocContent = null;
		if (!enhancementsEnabled) {
			return;
		}
		if (newInput instanceof String str) {
			javadocContent = str;
		} else if (newInput instanceof BrowserInformationControlInput bicInput) {
			javadocContent = bicInput.getHtml();
		}
		if (javadocContent != null && !javadocContent.isBlank() && JavaElementLinks.isStylingPresent(javadocContent)) {
			actions= enabledActions;
		} else {
			actions= noStylingActions;
		}
	}

	@Override
	public Menu getMenu(Control p) {
		// we keep it simple here and just re-create new menu with correct items
		dispose();
		menu= new Menu(parent);
		Stream.of(actions).forEach(action -> new ActionContributionItem(action).fill(menu, -1));
		return menu;
	}

	@Override
	public Menu getMenu(Menu p) {
		return null;
	}

	@Override
	public void dispose() {
		if (menu != null) {
			menu.dispose();
			menu= null;
		}
	}

	@Override
	public void runWithEvent(Event event) {
		enhancementsEnabled = !enhancementsEnabled;
		JavaElementLinks.setStylingEnabledPreference(enhancementsEnabled); // triggers call to stylingStateChanged()
	}

	private void runEnhancementsReconfiguredTask() {
		if (enhancementsReconfiguredTask != null) {
			parent.getDisplay().execute(enhancementsReconfiguredTask);
		}
	}

	private void presentEnhancementsState() {
		setImageDescriptor(enhancementsEnabled ? JavaPluginImages.DESC_ETOOL_JDOC_HOVER_EDIT : JavaPluginImages.DESC_DTOOL_JDOC_HOVER_EDIT);
		setToolTipText(enhancementsEnabled ? JavadocStylingMessages.JavadocStyling_enabledTooltip : JavadocStylingMessages.JavadocStyling_disabledTooltip);
		noStylingActions[0].setText(enhancementsEnabled ? JavadocStylingMessages.JavadocStyling_noEnhancements : JavadocStylingMessages.JavadocStyling_enhancementsDisabled);
	}

	@Override
	public void stylingStateChanged(boolean isEnabled) {
		parent.getDisplay().execute(() -> {
			enhancementsEnabled= isEnabled;
			presentEnhancementsState();
			// even if enhancements switched from off to on, only inputChanged() sets enabledActions
			actions= noStylingActions;
			runEnhancementsReconfiguredTask();
		});
	}

	@Override
	public void parametersColoringStateChanged(boolean isEnabled) {
		parent.getDisplay().execute(() -> {
			enabledActions[0].setChecked(isEnabled);
			runEnhancementsReconfiguredTask();
		});
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
			setEnabled(false);
		}
	}

	private static class ToggleSignatureTypeParametersColoringAction extends Action {

		public ToggleSignatureTypeParametersColoringAction() {
			super(JavadocStylingMessages.JavadocStyling_typeParamsColoring, IAction.AS_CHECK_BOX);
			setId(ToggleSignatureTypeParametersColoringAction.class.getSimpleName());
			setChecked(JavaElementLinks.getPreferenceForTypeParamsColoring());
		}

		@Override
		public void run() {
			super.run();
			JavaElementLinks.setPreferenceForTypeParamsColoring(isChecked()); // triggers call to parametersColoringStateChanged()
		}

	}

	public interface JavadocContentInputAccessor {
		void addInputChangedListener(IInputChangedListener changeListener);
	}
}