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

package org.eclipse.jdt.internal.ui.text.spelling.newapi;

import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.ui.texteditor.spelling.ISpellingEngine;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingContext;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellCheckPreferenceKeys;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellEvent;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellEventListener;

/**
 * Abstract spelling engine
 * 
 * @since 3.1
 */
public abstract class SpellingEngine implements ISpellingEngine {

	private static class SpellingProblemAdapter extends SpellingProblem {
		
		private ISpellEvent fSpellEvent;
		
		public SpellingProblemAdapter(ISpellEvent spellEvent) {
			super();
			fSpellEvent= spellEvent;
		}
		
		/*
		 * @see org.eclipse.ui.texteditor.spelling.SpellingProblem#getOffset()
		 */
		public int getOffset() {
			return fSpellEvent.getBegin();
		}
		
		/*
		 * @see org.eclipse.ui.texteditor.spelling.SpellingProblem#getLength()
		 */
		public int getLength() {
			return fSpellEvent.getEnd() - fSpellEvent.getBegin() + 1;
		}
		
		/*
		 * @see org.eclipse.ui.texteditor.spelling.SpellingProblem#getMessage()
		 */
		public String getMessage() {
			return MessageFormat.format(JavaUIMessages.getString("Spelling.error.label"), new String[] { fSpellEvent.getWord() }); //$NON-NLS-1$
		}
		
		/*
		 * @see org.eclipse.ui.texteditor.spelling.SpellingProblem#getProposals()
		 */
		public ICompletionProposal[] getProposals() {
			// TODO: implement
			return new ICompletionProposal[0];
		}
	}
	
	protected static class SpellEventListener implements ISpellEventListener {
		
		private ISpellingProblemCollector fCollector;
		
		public SpellEventListener(ISpellingProblemCollector collector) {
			super();
			fCollector= collector;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellEventListener#handle(org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellEvent)
		 */
		public void handle(ISpellEvent event) {
			fCollector.accept(new SpellingProblemAdapter(event));
		}
	}

	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingEngine#check(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IRegion[], org.eclipse.ui.texteditor.spelling.SpellingContext, org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void check(IDocument document, IRegion[] regions, SpellingContext context, ISpellingProblemCollector collector, IProgressMonitor monitor) {
		IPreferenceStore preferences= PreferenceConstants.getPreferenceStore();
		if (collector != null) {
			Locale locale= getLocale(preferences);
			ISpellChecker checker= org.eclipse.jdt.internal.ui.text.spelling.SpellCheckEngine.getInstance().createSpellChecker(locale, preferences);
			if (checker != null)
				check(document, regions, checker, locale, collector, monitor);
		}
	}

	/**
	 * @param document
	 * @param regions
	 * @param checker
	 * @param locale
	 * @param collector
	 * @param monitor the progress monitor, can be <code>null</code>
	 */
	protected abstract void check(IDocument document, IRegion[] regions, ISpellChecker checker, Locale locale, ISpellingProblemCollector collector, IProgressMonitor monitor);
	
	/**
	 * Returns the current locale of the spelling preferences.
	 * 
	 * @return The current locale of the spelling preferences
	 */
	private Locale getLocale(IPreferenceStore preferences) {
		Locale defaultLocale= org.eclipse.jdt.internal.ui.text.spelling.SpellCheckEngine.getDefaultLocale();
		String locale= preferences.getString(ISpellCheckPreferenceKeys.SPELLING_LOCALE);
		if (locale.equals(defaultLocale.toString()))
			return defaultLocale;
	
		if (locale.length() >= 5)
			return new Locale(locale.substring(0, 2), locale.substring(3, 5));
	
		return defaultLocale;
	}
}
