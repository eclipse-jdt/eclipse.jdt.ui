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

	public static class Keyboard {

		private static final int[][] MAP= new int[][] { 
				{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {'\r'}, {}, {},
				{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
				{' '}, { SWT.SHIFT, '"' } /* ! */,
				{ SWT.SHIFT, '2' } /* " */,
				{ '#' } /* # by ALT GR */, 
				{ '$' }, { SWT.SHIFT, '5' } /* % */,
				{ SWT.SHIFT, '6' } /* & */,
				{ '\'' }, { SWT.SHIFT, '8' } /* ( */,
				{ SWT.SHIFT, '9' } /* ) */,
				{ SWT.SHIFT, '3' } /* * */,
				{ SWT.SHIFT, '1' } /* + */,
				{ ',' }, { '-' }, { '.' }, { SWT.SHIFT, '7' } /* / */,
				{ '0' }, { '1' }, { '2' }, { '3' }, { '4' }, { '5' }, { '6' }, { '7' }, { '8' }, { '9' },
				{ SWT.SHIFT, '.' } /* : */,
				{ SWT.SHIFT, ',' } /* ; */,
				{ '<' }, { SWT.SHIFT, '0' } /* = */,
				{ SWT.SHIFT, '<' } /* > */,
				{ SWT.SHIFT, '\'' } /* ? */,
				{ '@' }, { 'A' }, { 'B' }, { 'C' }, { 'D' }, { 'E' }, { 'F' }, { 'G' }, { 'H' }, { 'I' }, { 'J' },
				{ 'K' }, { 'L' }, { 'M' }, { 'N' }, { 'O' }, { 'P' }, { 'Q' }, { 'R' }, { 'S' }, { 'T' }, { 'U' },
				{ 'V' }, { 'W' }, { 'X' }, { 'Y' }, { 'Z' }, { '[' }, { '\\' }, { ']' }, { '^' }, { SWT.SHIFT, '-' } /* _ */,
				{ SWT.SHIFT, '^' } /* ` */,
				{ 'a' }, { 'b' }, { 'c' }, { 'd' }, { 'e' }, { 'f' }, { 'g' }, { 'h' }, { 'i' }, { 'j' }, { 'k' },
				{ 'l' }, { 'm' }, { 'n' }, { 'o' }, { 'p' }, { 'q' }, { 'r' }, { 's' }, { 't' }, { 'u' }, { 'v' },
				{ 'w' }, { 'x' }, { 'y' }, { 'z' }, { '{' }, { '|' }, { '}' }, { '~' }, {}, {}, {}, {}, {}, {}, {}, {},
				{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, };

		public Keyboard(String string) {}

		public int[] getKeyCodes(char ch) {
			if (Character.isLetter(ch))
				if (Character.isLowerCase(ch))
					return new int[] { ch };
				else
					return new int[] { SWT.SHIFT, ch };
			else if (ch < MAP.length)
				return MAP[ch];
			else return new int[] {};
		}
	}
	
	private static final Keyboard fgKeyboard= new Keyboard("de_sg");

	public static Display getActiveDisplay() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay();
	}

	public static void pressKeyCode(Display display, int keyCode) {
		keyCodeDown(display, keyCode, '\0');
		keyCodeUp(display, keyCode, '\0');
	}

	public static void pressKeyCodeCombination(Display display, int[] keyCodes) {
		for (int i= 0; i < keyCodes.length; i++)
			keyCodeDown(display, keyCodes[i], '\0');
		for (int i= keyCodes.length - 1; i >= 0; i--)
			keyCodeUp(display, keyCodes[i], '\0');
	}

	private static void keyCodeDown(Display display, int keyCode, char ch) {
		keyCodeEvent(display, SWT.KeyDown, keyCode, ch);
	}

	private static void keyCodeUp(Display display, int keyCode, char ch) {
		keyCodeEvent(display, SWT.KeyUp, keyCode, ch);
	}

	private static Event sfKeyCodeEvent= new Event();
	private static void keyCodeEvent(Display display, int type, int keyCode, char character) {
		sfKeyCodeEvent.type= type;
		sfKeyCodeEvent.keyCode= keyCode;
		sfKeyCodeEvent.character= character;
		
		display.post(sfKeyCodeEvent);
		EditorTestHelper.runEventQueue();
	}

	public static void pressCharacter(Display display, char character) {
		pressCharacterCombination(display, fgKeyboard.getKeyCodes(character));
	}
	
	public static void pressCharacterCombination(Display display, int[] keyCodes) {
		int len= keyCodes.length;
		for (int i= 0; i <= len - 2; i++)
			keyCodeDown(display, keyCodes[i], '\0');
		if (len > 0) {
			keyCodeDown(display, keyCodes[len - 1], (char)keyCodes[len - 1]);
			keyCodeUp(display, keyCodes[len - 1], (char)keyCodes[len - 1]);
		}
		for (int i= len - 2; i >= 0; i--)
			keyCodeUp(display, keyCodes[i], '\0');
	}


}
