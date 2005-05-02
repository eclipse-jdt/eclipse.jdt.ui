/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import junit.framework.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;



/**
 * @since 3.1
 */
public final class KeyboardProbe {
	
	private static final boolean TRACE= false;
	private static final char FAKE_CHAR= '$';
	
	public static void main(String[] args) throws FileNotFoundException {
		
//		final PrintStream stream= new PrintStream(new BufferedOutputStream(new FileOutputStream(new File("/tmp/keyboardprobe.log"))));
		final PrintStream stream= System.out;
		char[][] keycodes= new KeyboardProbe().getKeycodes();
		for (int i= 0; i < keycodes.length; i++) {
			if (keycodes[i][NONE] != '\0')
				stream.println(makePrintable(keycodes[i][NONE]) + "\t" + i);
			if (keycodes[i][SHIFT] != '\0')
				stream.println(makePrintable(keycodes[i][SHIFT]) + "\tSHIFT+" + i);
			if (keycodes[i][ALT_CTRL] != '\0')
				stream.println(makePrintable(keycodes[i][ALT_CTRL]) + "\tALT+CTRL+" + i);
		}
		System.exit(0);
	}
	
	private static String makePrintable(char c) {
		if (Character.isISOControl(c))
			return "  (" + (int) c + ")";
		
		return "" + c + " (" + (int) c + ")";
	}

	private Display fDisplay;
	private char[][] fCodes= null;
	private static final int NONE= 0;
	private static final int SHIFT= 1;
	private static final int ALT_CTRL= 2;
	private boolean fKeyContinue;
	private boolean fTextContinue;
	private boolean fDisposeDisplay= false;
	private Shell fShell;
	
	/**
	 * Returns the characters being input into a text component by injecting key
	 * events into the system event queue. For each character, the array holds
	 * the resulting character when pressed alone and when pressed together with
	 * shift.
	 * 
	 * @return the characters input by pressing the keys. The key code is the
	 *         index into the array.
	 */
	public synchronized char[][] getKeycodes() {
		initialize();
		return fCodes;
	}
	
	/**
	 * Initializes this keyboard probe.
	 */
	public synchronized void initialize() {
		if (fCodes == null) {
			try {
				fCodes= probe();
			} finally {
				if (fDisposeDisplay && fDisplay != null) {
					fDisplay.dispose();
				}
				fDisplay= null;
				
				if (fShell != null && !fShell.isDisposed()) {
					fShell.dispose();
				}
				fShell= null;
			}
		}
	}

	/**
	 * Returns the character resulting from pressing the character 'key' with
	 * or without shift being pressed.
	 * 
	 * @param key the key to press
	 * @param pressShift whether pressed together with shift
	 * @return the char resulting from the given key combo, or 0 if there is no equivalent
	 */
	public char getCharForKeybinding(char key, boolean pressShift) {
		char[][] keycodes= getKeycodes();
		if (key < keycodes.length)
			return keycodes[key][pressShift ? SHIFT : NONE];
		return 0;
	}
	
	/**
	 * Returns a key binding combo that will produce the wanted character. Note
	 * that there may be more than one combo that can produce the wanted
	 * binding, any one is returned. The last integer in the returned array is
	 * a character that must be set to the Event.character field, any integers 
	 * before are SWT constants describing modifier keys that must be pressed
	 * to get the desired result.
	 * 
	 * @param expected the char to be input
	 * @return the corresponding key combo, or an empty array if there is no combo
	 */
	public int[] getKeybindingForChar(char expected) {
		char[][] keycodes= getKeycodes();
		for (int i= 0; i < keycodes.length; i++) {
			if (keycodes[i][NONE] == expected)
				return new int[] {i};
			else if (keycodes[i][SHIFT] == expected)
				return new int[] { SWT.SHIFT, i };
		}
		return new int[0];
	}
	
	/**
	 * Presses a key combination such that the expected character is input by
	 * simulating key events on the given display. Returns <code>true</code>
	 * if a key combo could be found, <code>false</code> if not.
	 * 
	 * @param expected the expected character
	 * @param display the display to simulate the events on
	 * @return <code>true</code> if there was a key combo to press,
	 *         <code>false</code> if <code>expected</code> has no combo on
	 *         the current keyboard layout.
	 */
	public boolean pressChar(char expected, Display display) {
		return pressChar(expected, new int[0], display);
	}
	
	/**
	 * Presses a key combination such that the expected character is input by
	 * simulating key events on the given display. Returns <code>true</code>
	 * if a key combo could be found, <code>false</code> if not.
	 * 
	 * @param expected the expected character
	 * @param modifiers additional modifiers to press
	 * @param display the display to simulate the events on
	 * @return <code>true</code> if there was a key combo to press,
	 *         <code>false</code> if <code>expected</code> has no combo on
	 *         the current keyboard layout.
	 */
	public boolean pressChar(char expected, int[] modifiers, Display display) {
		int[] charkeys= getKeybindingForChar(expected);
		int[] combo= new int[charkeys.length + modifiers.length];
		System.arraycopy(modifiers, 0, combo, 0, modifiers.length);
		System.arraycopy(charkeys, 0, combo, modifiers.length, charkeys.length);
		
		
		for (int i= 0; i <= combo.length - 2; i++) {
			SWTEventHelper.keyCodeDown(display, combo[i], false);
		}
		
		if (combo.length > 0) {
			SWTEventHelper.pressKeyChar(display, (char) combo[combo.length - 1], false);
		}
		
		for (int i= combo.length - 2; i >= 0; i--) {
			SWTEventHelper.keyCodeUp(display, combo[i], false);
		}
		
		return combo.length > 0;
	}
	
	char[][] probe() {
		char[][] codes= new char[256][];
		
		fDisplay= getOrCreateDisplay();
		Text text= createControl(fDisplay);
		for (int keyCode= 0; keyCode < codes.length; keyCode++) {
			codes[keyCode]= new char[3];
			if (skipCode(keyCode))
				continue;
			
			postNaturalKeyPress(keyCode);
			char c= getContent(text);
			
			if (TRACE) System.out.println("" + keyCode + "content[NONE]: " + c);
			
			codes[keyCode][NONE]= c;
			
			clearText(text);
			
			postShiftKeyPress(keyCode);
			c= getContent(text);
			
			if (TRACE) System.out.println("" + keyCode + "content[SHIFT]: " + c);
			codes[keyCode][SHIFT]= c;
			
			clearText(text);

			if (isAltGRCandidate(keyCode)) {
				postAltCtrlKeyPress(keyCode);
				c= getContent(text);
				
				if (TRACE) System.out.println("" + keyCode + "content[ALT_CTRL]: " + c);
				codes[keyCode][ALT_CTRL]= c;
				
				clearText(text);
			}
		}
		
		return codes;
	}

	private boolean skipCode(int keyCode) {
		return keyCode == 0 || keyCode > 200;
	}
	
	private boolean isAltGRCandidate(int keyCode) {
		return keyCode >= 48 && keyCode <= 57
				|| keyCode == 91
				|| keyCode == 92
				|| keyCode == 123
				|| keyCode == 36
				|| keyCode == 33;
	}

	private char getContent(Text text) {
		String content= text.getText();
		char c;
		if (content.length() == 2) {
			c= content.charAt(0);
			if (TRACE && content.charAt(1) != FAKE_CHAR)
				System.out.println("second char was '" + content.charAt(1) + "'");
		} else if (content.length() > 2) {
			c= content.charAt(0);
			if (TRACE) System.out.println("rest content was '" + content.substring(1) + "'");			
		} else if (content.length() == 1) {
			c= '\0';
			if (TRACE && content.charAt(0) != FAKE_CHAR)
				System.out.println("second char was '" + content.charAt(0) + "'");
		} else {
			c= '\0';
			if (TRACE) System.out.println("no content");
		}
		return c;
	}

	private void clearText(Text text) {
		fTextContinue= false;
		text.setText("");

		DisplayHelper helper= new DisplayHelper() {
			protected boolean condition() {
				return fTextContinue;
			}
		};
		
		Assert.assertTrue("unable to clear text", helper.waitForCondition(fDisplay, 300));
	}

	private Display getOrCreateDisplay() {
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
			fDisposeDisplay= true;
		}
		return display;
	}
	
	private void addListeners(Text control) {
		control.addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
				onKeyDown(event);
			}
		});
		control.addListener(SWT.KeyUp, new Listener() {
			public void handleEvent(Event event) {
				onKeyUp(event);
			}
		});
		control.addListener(SWT.Modify, new Listener() {
			
			public void handleEvent(Event event) {
				onModify(event);
			}
		});
	}
	
	private Text createControl(Display display) {
		fShell= new Shell(display);
		fShell.setSize(300, 200);
		fShell.setText("Keyboard Probe"); //$NON-NLS-1$
		fShell.setLayout(new FillLayout());
		Text text= new Text(fShell, SWT.MULTI | SWT.LEFT | SWT.WRAP);
		text.setSize(300, 200);
		fShell.setVisible(true);
		fShell.forceActive();
		fShell.forceFocus();
		addListeners(text);
		
		text.setFocus();
		return text;
	}
	
	private void postNaturalKeyPress(int i) {
		
		fKeyContinue= false;
		
		Event event= new Event();
		event.type= SWT.KeyDown;
		event.keyCode= i;
		event.character= (char) i;
		fDisplay.post(event);
		
		event.type= SWT.KeyUp;
		fDisplay.post(event);
		
		Event event2= new Event();
		event2.type= SWT.KeyDown;
		event2.character= FAKE_CHAR;
		fDisplay.post(event2);
		
		event2.type= SWT.KeyUp;
		fDisplay.post(event2);
		
		DisplayHelper helper= new DisplayHelper() {
			protected boolean condition() {
				return fKeyContinue;
			}
		};
		Assert.assertTrue("unable to press character for keycode: " + i, helper.waitForCondition(fDisplay, 300));

	}
	
	private void postShiftKeyPress(int i) {
		fKeyContinue= false;
		
		Event shift= new Event();
		shift.type= SWT.KeyDown;
		shift.keyCode= SWT.SHIFT;
		fDisplay.post(shift);
		
		Event event= new Event();
		event.type= SWT.KeyDown;
		event.character= (char) i;
		fDisplay.post(event);
		
		event.type= SWT.KeyUp;
		fDisplay.post(event);
		
		shift.type= SWT.KeyUp;
		fDisplay.post(shift);
		
		Event event2= new Event();
		event2.type= SWT.KeyDown;
		event2.character= FAKE_CHAR;
		fDisplay.post(event2);
		
		event2.type= SWT.KeyUp;
		fDisplay.post(event2);
		
		DisplayHelper helper= new DisplayHelper() {
			protected boolean condition() {
				return fKeyContinue;
			}
		};
		Assert.assertTrue("unable to press SHIFT character for keycode: " + i, helper.waitForCondition(fDisplay, 300));
	}
	
	private void postAltCtrlKeyPress(int i) {
		fKeyContinue= false;
		
		Event ctrl= new Event();
		ctrl.type= SWT.KeyDown;
		ctrl.keyCode= SWT.CTRL;
		fDisplay.post(ctrl);
		
		Event alt= new Event();
		alt.type= SWT.KeyDown;
		alt.keyCode= SWT.ALT;
		fDisplay.post(alt);
		
		Event event= new Event();
		event.type= SWT.KeyDown;
		event.character= (char) i;
		fDisplay.post(event);
		
		event.type= SWT.KeyUp;
		fDisplay.post(event);
		
		alt.type= SWT.KeyUp;
		fDisplay.post(alt);
		
		ctrl.type= SWT.KeyUp;
		fDisplay.post(ctrl);
		
		Event event2= new Event();
		event2.type= SWT.KeyDown;
		event2.character= FAKE_CHAR;
		fDisplay.post(event2);
		
		event2.type= SWT.KeyUp;
		fDisplay.post(event2);
		
		DisplayHelper helper= new DisplayHelper() {
			protected boolean condition() {
				return fKeyContinue;
			}
		};
		Assert.assertTrue("unable to press SHIFT character for keycode: " + i, helper.waitForCondition(fDisplay, 300));
	}
	
	private int fKeyMask= 0;
	
	private void onKeyDown(Event event) {
		fKeyMask |= event.stateMask;
	}
	
	private void onKeyUp(Event event) {
		fKeyMask &= ~event.stateMask;
		if (fKeyMask == 0)
			fKeyContinue= true;
	}
	
	private void onModify(Event event) {
		fTextContinue= true;
	}
}
