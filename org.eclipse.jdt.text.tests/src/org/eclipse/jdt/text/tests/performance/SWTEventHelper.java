/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import org.eclipse.ui.PlatformUI;


/**
 * @since 3.1
 */
public class SWTEventHelper {
	
	public static Display getActiveDisplay() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay();
	}

	public static void pressKeyCode(Display display, int keyCode) {
		pressKeyCode(display, keyCode, true);
	}
	
	public static void pressKeyCode(Display display, int keyCode, boolean runEventQueue) {
		keyCodeDown(display, keyCode, runEventQueue);
		keyCodeUp(display, keyCode, runEventQueue);
	}

	public static void pressKeyCodeCombination(Display display, int[] keyCodes) {
		pressKeyCodeCombination(display, keyCodes, true);
	}
	
	public static void pressKeyCodeCombination(Display display, int[] keyCodes, boolean runEventQueue) {
		for (int i= 0; i < keyCodes.length; i++)
			keyCodeDown(display, keyCodes[i], runEventQueue);
		for (int i= keyCodes.length - 1; i >= 0; i--)
			keyCodeUp(display, keyCodes[i], runEventQueue);
	}

	private static void keyCodeDown(Display display, int keyCode, boolean runEventQueue) {
		keyCodeEvent(display, SWT.KeyDown, keyCode, runEventQueue);
	}

	private static void keyCodeUp(Display display, int keyCode, boolean runEventQueue) {
		keyCodeEvent(display, SWT.KeyUp, keyCode, runEventQueue);
	}

	private static Event sfKeyCodeEvent= new Event();
	private static void keyCodeEvent(Display display, int type, int keyCode, boolean runEventQueue) {
		sfKeyCodeEvent.type= type;
		sfKeyCodeEvent.keyCode= keyCode;
		
		display.post(sfKeyCodeEvent);
		if (runEventQueue)
			EditorTestHelper.runEventQueue();
	}
}
