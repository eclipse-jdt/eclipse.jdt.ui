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
package org.eclipse.jdt.internal.ui.viewsupport.browser;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CheckboxInBrowserToggler {
	static final Pattern CHECKED_PATERN= Pattern.compile(" *checked(=['\"]\\S*?['\"])? *"); //$NON-NLS-1$

	private final BrowserTextAccessor fBrowserTextAccessor;
	private final String checkboxId;
	private final Pattern checkboxHtmlFragment;

	/**
	 * Creates new instance for specific checkbox with id equal to <code>checkboxId</code> within the content of the browser
	 * accessed through <code>browserAccessor</code>.
	 * @param browserTextAccessor browser accessor accessing target browser content
	 * @param checkboxId id attribute of the checkbox <code>&#60;input&#62;</code> element
	 */
	public CheckboxInBrowserToggler(BrowserTextAccessor browserTextAccessor, String checkboxId) {
		fBrowserTextAccessor= browserTextAccessor;
		this.checkboxId= checkboxId;
		this.checkboxHtmlFragment= Pattern.compile("<input [^>]*?id=['\"]" + checkboxId + "['\"][^>]*?>"); //$NON-NLS-1$ //$NON-NLS-2$;
	}

	@Override
	public String toString() {
		return "CheckboxInBrowserLocatorImpl[checkboxId: " + checkboxId + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Matcher createCheckboxHtmlFragmentMatcher() {
		return checkboxHtmlFragment.matcher(fBrowserTextAccessor.getText());
	}

	/**
	 * Returns whether the target checkbox is present inside the browser's content.
	 * @return <code>true</code> if the target checkbox can be found inside the browser's content, <code>false</code> otherwise
	 */
	public boolean isCheckboxPresentInBrowser() {
		return Boolean.TRUE == actOnBrowser(
				// fail-fast check first
				() -> fBrowserTextAccessor.getText().contains(checkboxId) && createCheckboxHtmlFragmentMatcher().find(),
				() -> "unable to check presence of checkbox #" + checkboxId); //$NON-NLS-1$
	}

	/**
	 * Set <code>checked</code> attribute of the <code>&#60;input&#62;</code> element of checkbox inside the browser's content,
	 * i.e. if the checkbox should be "pre-selected". It does not simulate user toggling checkbox checked state (e.g. by mouse).
	 * @param enabled <code>true</code> if <code>checked</code> attribute should be present inside the checkbox
	 * <code>&#60;input&#62;</code element, <code>false</code> otherwise
	 * @return previous presence state of the checkbox <code>checked</code> attribute, or {@link java.util.Optional#empty() empty()}
	 * if checkbox could not be found inside the browser's content
	 */
	public Optional<Boolean> toggleCheckboxInBrowser(boolean enabled) {
		return actOnBrowser(
				enabled // avoid creating new lambda instance on every call to this method
					? this::toggleCheckboxInBrowserOn
					: this::toggleCheckboxInBrowserOff,
				() -> "unable to toggle checkbox #" + checkboxId); //$NON-NLS-1$
	}

	private Optional<Boolean> toggleCheckboxInBrowserOn() {
		return toggleCheckboxInBrowserImpl(true);
	}

	private Optional<Boolean> toggleCheckboxInBrowserOff() {
		return toggleCheckboxInBrowserImpl(false);
	}

	private Optional<Boolean> toggleCheckboxInBrowserImpl(boolean enabled) {
		String html= fBrowserTextAccessor.getText();
		if (!html.contains(checkboxId)) { // fail-fast check
			// browser does not currently display content with target checkbox
			return Optional.empty();
		}
		var matcher= createCheckboxHtmlFragmentMatcher();
		if (matcher.find()) {
			matcher.region(matcher.start(), matcher.end()).usePattern(CHECKED_PATERN);
			if (enabled) {
				if (!matcher.find()) {
					int inputFragmentEnd= matcher.regionEnd() - 1;
					StringBuilder sb= new StringBuilder(html.length() + 8);
					sb.append(html, 0, inputFragmentEnd);
					sb.append(" checked"); //$NON-NLS-1$
					sb.append(html, inputFragmentEnd, html.length());
					fBrowserTextAccessor.setText(sb.toString());
					return Optional.of(Boolean.FALSE);
				} else {
					// checkbox was already checked in HTML
					return Optional.of(Boolean.TRUE);
				}
			} else if (matcher.find()) {
				StringBuilder sb= new StringBuilder(html.length() - (matcher.end() - matcher.start()));
				sb.append(html, 0, matcher.start());
				sb.append(html, matcher.end(), html.length());
				fBrowserTextAccessor.setText(sb.toString());
				return Optional.of(Boolean.TRUE);
			} else {
				// checkbox was already not checked in HTML
				return Optional.of(Boolean.FALSE);
			}
		} else {
			// browser does not currently display content with target checkbox
			return Optional.empty();
		}
	}

	private <R> R actOnBrowser(Supplier<R> action, Supplier<String> notInitializedMessageSuffixSupplier) {
		if (fBrowserTextAccessor.isInitlaized()) {
			return action.get();
		} else {
			// not really expected to happen, but if it does, let the user know, so that they can provide this info in case they decide to report
			JavaPlugin.logErrorMessage("Browser widget not yet initialized: " //$NON-NLS-1$
					+ notInitializedMessageSuffixSupplier.get());
			return null;
		}
	}

	/**
	 * Returns the {@link BrowserTextAccessor} this instance uses to access browser's content.
	 * @return browser text accessor used by this instance
	 */
	public BrowserTextAccessor getBrowserTextAccessor() {
		return fBrowserTextAccessor;
	}
}