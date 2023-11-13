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

import java.util.function.Supplier;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;

import org.eclipse.core.runtime.ListenerList;

import org.eclipse.jface.internal.text.html.BrowserInformationControl;

/**
 * Cache over text content of a {@link Browser} widget for situations when multiple accesses & changes to it's content need
 * to be done in rapid succession on SWT display thread before browser widget backend gets a chance to update it's model.
 * <br><br>
 * This facility addresses the fact that at least for some browser backends (e.g. <code>org.eclipse.swt.browser.IE</code>)
 * setting content to browser is delayed - setting content via call to {@link Browser#setText(String)} is not immediate
 * as it requires some future work on SWT display thread, and therefore is not right away reflected in return value of
 * to {@link Browser#getText()}.
 * <br><br>
 * <b>Attention:</b><br>
 * This class in not thread-safe as it is assumed it's used always only from SWT display thread.
 */
public class BrowserTextAccessor {

	private final ListenerList<IBrowserContentChangeListener> listeners= new ListenerList<>();
	private Browser browser;
	private String textCache;

	public BrowserTextAccessor(BrowserInformationControl iControl) {
		// only way so far to get hold of reference to browser is to get it through LocationListener
		iControl.addLocationListener(new BrowserTextAccessorLocationListener());
	}

	public BrowserTextAccessor(Browser browser) {
		this.browser= browser;
		browser.addLocationListener(new BrowserTextAccessorLocationListener());
	}

	protected boolean isInitlaized() {
		return browser != null;
	}

	/**
	 * Gets the working content of the browser, i.e. content set via last call to {@link #setText(String)}, if one was made,
	 * otherwise content fetched from browser if no {@link #setText(String)} call was made since last time actual content inside
	 * browser was changed (e.g. by {@link #applyChanges()}).
	 * @return working content of the browser
	 */
	protected String getText() {
		if (textCache == null) {
			textCache= browser.getText();
		}
		return textCache;
	}

	/**
	 * Sets working content of the browser, i.e. content to be set inside the browser on next call to {@link #applyChanges()}.
	 * @param text new content
	 */
	protected void setText(String text) {
		textCache= text;
	}

	/**
	 * Commits working content to the browser if any was set since last time actual content inside browser was changed
	 * (e.g. by {@link #applyChanges()}).
	 */
	public void applyChanges() {
		if (textCache != null) {
			browser.setText(textCache);
			textCache= null;
		}
	}

	public void addContentChangedListener(IBrowserContentChangeListener listener) {
		listeners.add(listener);
	}

	public void removeContentChangedListener(IBrowserContentChangeListener listener) {
		listeners.remove(listener);
	}

	// to avoid BrowserTextAccessor itself implementing LocationListener
	private class BrowserTextAccessorLocationListener implements LocationListener {

		@Override
		public void changing(LocationEvent event) {
			if (browser == null && event.widget instanceof Browser) {
				browser= (Browser) event.widget;
			}
		}

		@Override
		public void changed(LocationEvent event) {
			// expected to already be NULL (since call to applyChanges()), otherwise we (may have) lost some uncommitted changes
			textCache= null;
			listeners.forEach(listener -> listener.browserContentChanged(BrowserTextAccessor.this::getText));
		}
	}

	/**
	 * Listener for browser content changes.
	 */
	public interface IBrowserContentChangeListener {

		/**
		 * Notification when content inside the browser accessed through {@link BrowserTextAccessor} was changed.
		 * @param contentAccessor supplier of the new browser content
		 */
		void browserContentChanged(Supplier<String> contentAccessor);
	}

}