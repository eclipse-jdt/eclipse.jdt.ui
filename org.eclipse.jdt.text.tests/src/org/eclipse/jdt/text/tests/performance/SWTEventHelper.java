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



/**
 * @since 3.1
 */
public class SWTEventHelper {
	
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

	public static void keyCodeDown(Display display, int keyCode, boolean runEventQueue) {
		keyCodeEvent(display, SWT.KeyDown, keyCode, runEventQueue);
	}

	public static void keyCodeUp(Display display, int keyCode, boolean runEventQueue) {
		keyCodeEvent(display, SWT.KeyUp, keyCode, runEventQueue);
	}

	private static Event sfKeyCodeEvent= new Event();
	public static void keyCodeEvent(Display display, int type, int keyCode, boolean runEventQueue) {
		sfKeyCodeEvent.type= type;
		sfKeyCodeEvent.keyCode= keyCode;
		
		display.post(sfKeyCodeEvent);
		if (runEventQueue)
			EditorTestHelper.runEventQueue();
	}
	
	public static void pressKeyChar(Display display, char keyChar) {
		pressKeyChar(display, keyChar, true);
	}
	
	public static void pressKeyChar(Display display, char keyChar, boolean runEventQueue) {
		keyCharDown(display, keyChar, runEventQueue);
		keyCharUp(display, keyChar, runEventQueue);
	}

	public static void pressKeyCharCombination(Display display, char[] keyChars) {
		pressKeyCharCombination(display, keyChars, true);
	}
	
	public static void pressKeyCharCombination(Display display, char[] keyChars, boolean runEventQueue) {
		for (int i= 0; i < keyChars.length; i++)
			keyCharDown(display, keyChars[i], runEventQueue);
		for (int i= keyChars.length - 1; i >= 0; i--)
			keyCharUp(display, keyChars[i], runEventQueue);
	}

	public static void keyCharDown(Display display, char keyChar, boolean runEventQueue) {
		keyCharEvent(display, SWT.KeyDown, keyChar, runEventQueue);
	}

	public static void keyCharUp(Display display, char keyChar, boolean runEventQueue) {
		keyCharEvent(display, SWT.KeyUp, keyChar, runEventQueue);
	}

	private static Event sfKeyCharEvent= new Event();
	public static void keyCharEvent(Display display, int type, char keyChar, boolean runEventQueue) {
		sfKeyCharEvent.type= type;
		sfKeyCharEvent.character= keyChar;
		
		display.post(sfKeyCharEvent);
		if (runEventQueue)
			EditorTestHelper.runEventQueue();
	}
	
	private static Event sfMouseMoveEvent= new Event();
	public static void mouseMoveEvent(Display display, int x, int y, boolean runEventQueue) {
		sfMouseMoveEvent.type= SWT.MouseMove;
		sfMouseMoveEvent.x= x;
		sfMouseMoveEvent.y= y;
		
		display.post(sfMouseMoveEvent);
		if (runEventQueue)
			EditorTestHelper.runEventQueue();
	}
	
	public static void mouseDownEvent(Display display, int button, boolean runEventQueue) {
		mouseButtonEvent(display, SWT.MouseDown, button, runEventQueue);
	}

	public static void mouseUpEvent(Display display, int button, boolean runEventQueue) {
		mouseButtonEvent(display, SWT.MouseUp, button, runEventQueue);
	}

	private static Event sfMouseButtonEvent= new Event();
	public static void mouseButtonEvent(Display display, int type, int button, boolean runEventQueue) {
		sfMouseButtonEvent.type= type;
		sfMouseButtonEvent.button= button;
		
		display.post(sfMouseButtonEvent);
		if (runEventQueue)
			EditorTestHelper.runEventQueue();
	}
}
