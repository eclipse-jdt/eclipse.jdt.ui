/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc., and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import org.eclipse.swt.widgets.Display;

/**
 * This thread polls the display every 50milliseconds with a null UI operation
 * request and computes how long it take for display to process the request.
 * The time to handle request is time when display is busy doing other work, so
 * it's actually a UI Freeze.
 *
 * This could be moved to SWT or other common place where we need to check
 * UI Freezes and reused form there,
 */
public class CheckUIThreadReactivityThread extends Thread {

	long pauseBetweenEachPing = 50;
	final private Display fDisplay;

	private long maxDuration;

	public CheckUIThreadReactivityThread(Display display) {
		fDisplay= display;
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			long duration = System.currentTimeMillis();
			fDisplay.syncExec(() -> {}); // do nothing, but in UI Thread
			duration = System.currentTimeMillis() - duration;
			maxDuration = Math.max(duration, maxDuration);
			try {
				sleep(pauseBetweenEachPing);
			} catch (InterruptedException e) {
				// nothing
			}
		}
	}

	public long getMaxDuration() {
		return maxDuration;
	}
}
