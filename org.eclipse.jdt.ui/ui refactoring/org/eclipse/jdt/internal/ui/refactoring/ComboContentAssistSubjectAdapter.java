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

package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEventConsumer;
import org.eclipse.jface.text.contentassist.IContentAssistSubject;

import org.eclipse.jdt.internal.corext.Assert;

//###################################################################################
//#################### COPY FROM org.eclipse.ui.texteditor !!! ######################
//###################################################################################
// Changes: - getLocationAtOffset();
//          - added VerifyKeyListener support;
//###################################################################################


//###################################################################################
//#################### COPY FROM org.eclipse.ui.texteditor !!! ######################
//###################################################################################
// Changes: - getLocationAtOffset();
//          - added VerifyKeyListener support;
//###################################################################################


/**
 * Adapts a <code>Combo</code> to an <code>IContentAssistSubject</code>.
 * 
 * @see org.eclipse.swt.widgets.Combo
 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject
 * @since 3.0
 */
final class ComboContentAssistSubjectAdapter implements IContentAssistSubject {

	private class InternalDocument extends Document {
		
		private ModifyListener fModifyListener;
		
		private InternalDocument() {
			super(fCombo.getText());
			fModifyListener= new ModifyListener() {
				/*
				 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
				 */
				public void modifyText(ModifyEvent e) {
					set(fCombo.getText());
				}
			};
			fCombo.addModifyListener(fModifyListener);
		}
		
		/*
		 * @see org.eclipse.jface.text.AbstractDocument#replace(int, int, java.lang.String)
		 */
		public void replace(int pos, int length, String text) throws BadLocationException {
			super.replace(pos, length, text);
			fCombo.removeModifyListener(fModifyListener);
			fCombo.setText(get());
			fCombo.addModifyListener(fModifyListener);
		}
	}
	
	/**
	 * The combo.
	 */
	private Combo fCombo;
	private HashMap fModifyListeners;
	private List fVerifyKeyListeners;
	private Listener fKeyDownListener;

	/**
	 * Creates a content assist subject adapter for the given combo.
	 * 
	 * @param combo the combo to adapt
	 */
	public ComboContentAssistSubjectAdapter(Combo combo) {
		Assert.isNotNull(combo);
		fCombo= combo;
		fModifyListeners= new HashMap();
		fVerifyKeyListeners= new ArrayList(1);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#getControl()
	 */
	public Control getControl() {
		return fCombo;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#getLineHeight()
	 */
	public int getLineHeight() {
		return fCombo.getTextHeight();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#getCaretOffset()
	 */
	public int getCaretOffset() {
		return fCombo.getSelection().y;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#getLocationAtOffset(int)
	 */
	public Point getLocationAtOffset(int offset) {
		String comboString= fCombo.getText();
		GC gc = new GC(fCombo);
		gc.setFont(fCombo.getFont());
		Point extent= gc.textExtent(comboString.substring(0, Math.min(offset, comboString.length())));
		int spaceWidth= gc.textExtent(" ").x; //$NON-NLS-1$
		gc.dispose();
		/*
		 * FIXME: the two space widths below is a workaround for bug 44072
		 */
		int x= 2 * spaceWidth + fCombo.getClientArea().x + fCombo.getBorderWidth() + extent.x;
		return new Point(x, 0);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#getLineDelimiter()
	 */
	public String getLineDelimiter() {
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#addKeyListener(org.eclipse.swt.events.KeyListener)
	 */
	public void addKeyListener(KeyListener keyListener) {
		fCombo.addKeyListener(keyListener);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#getSelectionRange()
	 */
	public Point getWidgetSelectionRange() {
		return new Point(fCombo.getSelection().x, Math.abs(fCombo.getSelection().y - fCombo.getSelection().x));
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#getSelectedRange()
	 */
	public Point getSelectedRange() {
		return new Point(fCombo.getSelection().x, Math.abs(fCombo.getSelection().y - fCombo.getSelection().x));
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#getDocument()
	 */
	public IDocument getDocument() {
		IDocument document= (IDocument)fCombo.getData("document"); //$NON-NLS-1$
		if (document == null) {
			document= new InternalDocument() ;
			fCombo.setData("document", document); //$NON-NLS-1$
		}
		return document;
	}
	
	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#canAddVerifyKeyListener()
	 */
	public boolean supportsVerifyKeyListener() {
		return true;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#appendVerifyKeyListener(org.eclipse.swt.custom.VerifyKeyListener)
	 */
	public boolean appendVerifyKeyListener(final VerifyKeyListener verifyKeyListener) {
		installKeyDownListener();
		fVerifyKeyListeners.add(verifyKeyListener);
		return true;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#prependVerifyKeyListener(org.eclipse.swt.custom.VerifyKeyListener)
	 */
	public boolean prependVerifyKeyListener(final VerifyKeyListener verifyKeyListener) {
		installKeyDownListener();
		fVerifyKeyListeners.add(0, verifyKeyListener);
		return true;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#removeVerifyKeyListener(org.eclipse.swt.custom.VerifyKeyListener)
	 */
	public void removeVerifyKeyListener(VerifyKeyListener verifyKeyListener) {
		fVerifyKeyListeners.remove(verifyKeyListener);
		if (fVerifyKeyListeners.size() == 0 && fKeyDownListener != null) {
			fCombo.removeListener(SWT.KeyDown, fKeyDownListener);
			fKeyDownListener= null;
		}
	}
	
	private void installKeyDownListener() {
		if (fKeyDownListener != null)
			return;
		fKeyDownListener= new Listener() {
			public void handleEvent(Event e) {
				VerifyEvent verifyEvent= new VerifyEvent(e);
				for (Iterator iter= fVerifyKeyListeners.iterator(); iter.hasNext() && verifyEvent.doit; ) {
					VerifyKeyListener vkListener= (VerifyKeyListener) iter.next();
					vkListener.verifyKey(verifyEvent);
				}
				e.doit= verifyEvent.doit;
			}
		};
		fCombo.addListener(SWT.KeyDown, fKeyDownListener);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#setEventConsumer(org.eclipse.jface.text.IEventConsumer)
	 */
	public void setEventConsumer(final IEventConsumer eventConsumer) {
		// this is not supported
		//TODO: must be supported to handle Tab (to put focus into proposalsList)
		//p.s.: event is handled too late if done in verifyKeyListener (-> fCombo already lost focus)
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#setSelectedRange(int, int)
	 */
	public void setSelectedRange(int i, int j) {
		fCombo.setSelection(new Point(i, i+j));
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#revealRange(int, int)
	 */
	public void revealRange(int i, int j) {
		// XXX: this should be improved
		fCombo.setSelection(new Point(i, i+j));
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#removeKeyListener(org.eclipse.swt.events.KeyListener)
	 */
	public void removeKeyListener(KeyListener keyListener) {
		fCombo.removeKeyListener(keyListener);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#removeSelectionListener(org.eclipse.swt.events.SelectionListener)
	 */
	public void removeSelectionListener(SelectionListener selectionListener) {
		fCombo.removeSelectionListener(selectionListener);
		Object listener= fModifyListeners.get(selectionListener);
		if (listener instanceof Listener)
			fCombo.removeListener(SWT.Modify, (Listener)listener);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistSubject#addSelectionListener(org.eclipse.swt.events.SelectionListener)
	 */
	public boolean addSelectionListener(final SelectionListener selectionListener) {
		fCombo.addSelectionListener(selectionListener);
		Listener listener= new Listener() {
			/*
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			public void handleEvent(Event e) {
				selectionListener.widgetSelected(new SelectionEvent(e));
	
			}
		};
		fCombo.addListener(SWT.Modify, listener); 
		fModifyListeners.put(selectionListener, listener);
		return true;
	}
}
