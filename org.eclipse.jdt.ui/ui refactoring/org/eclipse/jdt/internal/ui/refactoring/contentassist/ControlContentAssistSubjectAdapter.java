/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.eclipse.jface.text.IEventConsumer;
import org.eclipse.jface.text.contentassist.IContentAssistSubject;
import org.eclipse.jface.util.Assert;



public abstract class ControlContentAssistSubjectAdapter implements IContentAssistSubject {

	protected static final boolean DEBUG= false;
	
	private List fVerifyKeyListeners;
	private Set fKeyListeners;
	private Listener fControlListener;

	public ControlContentAssistSubjectAdapter() {
		fVerifyKeyListeners= new ArrayList(1);
		fKeyListeners= new HashSet(1);
	}

	public abstract Control getControl();

	public void addKeyListener(KeyListener keyListener) {
		fKeyListeners.add(keyListener);
		if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#addKeyListener()");
		installControlListener();
	}

	public void removeKeyListener(KeyListener keyListener) {
		boolean deleted= fKeyListeners.remove(keyListener);
		if (DEBUG && !deleted)
			System.err.println("removeKeyListener -> wasn't here");
		if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#removeKeyListener() -> " + fKeyListeners.size());
		uninstallControlListener();
	}

	public boolean supportsVerifyKeyListener() {
		return true;
	}

	public boolean appendVerifyKeyListener(final VerifyKeyListener verifyKeyListener) {
		fVerifyKeyListeners.add(verifyKeyListener);
		if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#appendVerifyKeyListener() -> " + fVerifyKeyListeners.size());
		installControlListener();
		return true;
	}

	public boolean prependVerifyKeyListener(final VerifyKeyListener verifyKeyListener) {
		fVerifyKeyListeners.add(0, verifyKeyListener);
		if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#prependVerifyKeyListener() -> " + fVerifyKeyListeners.size());
		installControlListener();
		return true;
	}

	public void removeVerifyKeyListener(VerifyKeyListener verifyKeyListener) {
		fVerifyKeyListeners.remove(verifyKeyListener);
		if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#removeVerifyKeyListener() -> " + fVerifyKeyListeners.size());
		uninstallControlListener();
	}

	public void setEventConsumer(IEventConsumer eventConsumer) {
		// this is not supported
		if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#setEventConsumer()");
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#getLineDelimiter()
	 */
	public String getLineDelimiter() {
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	private void installControlListener() {
		if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#installControlListener() -> k: " + fKeyListeners.size() + ", v: " + fVerifyKeyListeners.size());
		if (fControlListener != null)
			return;
	
		fControlListener= new Listener() {
			public void handleEvent(Event e) {
				VerifyEvent verifyEvent= new VerifyEvent(e);
				KeyEvent keyEvent= new KeyEvent(e);
				switch (e.type) {
					case SWT.Traverse :
						if (DEBUG) dump("before traverse", e, verifyEvent);
						verifyEvent.doit= true;
						for (Iterator iter= fVerifyKeyListeners.iterator(); iter.hasNext(); ) {
							((VerifyKeyListener) iter.next()).verifyKey(verifyEvent);
							if (! verifyEvent.doit) {
								e.detail= SWT.TRAVERSE_NONE;
								e.doit= true;
								if (DEBUG) dump("traverse eaten by verify", e, verifyEvent);
								return;
							}
							if (DEBUG) dump("traverse ok", e, verifyEvent);
						}
						break;
					
					case SWT.KeyDown:
						for (Iterator iter= fVerifyKeyListeners.iterator(); iter.hasNext(); ) {
							((VerifyKeyListener) iter.next()).verifyKey(verifyEvent);
							if (! verifyEvent.doit) {
								e.doit= verifyEvent.doit;
								if (DEBUG) dump("keyDown eaten by verify", e, verifyEvent);
								return;
							}
						}
						if (DEBUG) dump("keyDown ok", e, verifyEvent);
						for (Iterator iter= fKeyListeners.iterator(); iter.hasNext();) {
							((KeyListener) iter.next()).keyPressed(keyEvent);
						}
						break;
	
					default :
						Assert.isTrue(false);
				}
			}
			private void dump(String who, Event e, VerifyEvent ve) {
				StringBuffer sb= new StringBuffer("---\n");
				sb.append(who);
				sb.append(" - e: keyCode="+e.keyCode+hex(e.keyCode));
				sb.append("; character="+e.character+hex(e.character));
				sb.append("; stateMask="+e.stateMask+hex(e.stateMask));
				sb.append("; doit="+e.doit);
				sb.append("; detail="+e.detail+hex(e.detail));
				sb.append("\n");
				sb.append("  verifyEvent keyCode="+e.keyCode+hex(e.keyCode));
				sb.append("; character="+e.character+hex(e.character));
				sb.append("; stateMask="+e.stateMask+hex(e.stateMask));
				sb.append("; doit="+ve.doit);
				System.out.println(sb);
			}
			private String hex(int i) {
				return "[0x" + Integer.toHexString(i) + ']';
			}
		};
		getControl().addListener(SWT.Traverse, fControlListener);
		getControl().addListener(SWT.KeyDown, fControlListener);
		if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#installControlListener() - installed");
	}

	private void uninstallControlListener() {
		if (fControlListener == null || fKeyListeners.size() + fVerifyKeyListeners.size() != 0) {
			if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#uninstallControlListener() -> k: " + fKeyListeners.size() + ", v: " + fVerifyKeyListeners.size());
			return;
		}
		getControl().removeListener(SWT.Traverse, fControlListener);
		getControl().removeListener(SWT.KeyDown, fControlListener);
		fControlListener= null;
		if (DEBUG) System.err.println("ControlContentAssistSubjectAdapter#uninstallControlListener() - done");
	}

}
