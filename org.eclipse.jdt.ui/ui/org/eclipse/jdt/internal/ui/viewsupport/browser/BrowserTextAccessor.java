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
 * Provides access to the text of the {@link Browser} internally created &amp; used by {@link BrowserInformationControl}.
 */
public class BrowserTextAccessor {

	private final ListenerList<IBrowserContentChangeListener> listeners= new ListenerList<>();
	private Browser browser;

	public BrowserTextAccessor(BrowserInformationControl iControl) {
		// only way so far to get hold of reference to browser is to get it through LocationListener
		iControl.addLocationListener(new BrowserTextAccessorLocationListener());
	}

	public BrowserTextAccessor(Browser browser) {
		this.browser= browser;
		browser.addLocationListener(new BrowserTextAccessorLocationListener());
	}

	public String getText() {
		return browser.getText();
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