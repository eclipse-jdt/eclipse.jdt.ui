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
package org.eclipse.jdt.internal.ui.text;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.WordRule;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;


/**
 * AbstractJavaCommentScanner.java
 */
public class JavaCommentScanner extends AbstractJavaScanner{

	private static class TaskTagDetector implements IWordDetector {

		public boolean isWordStart(char c) {
			return c == '@' || Character.isJavaIdentifierStart(c);
		}

		public boolean isWordPart(char c) {
			return Character.isJavaIdentifierPart(c);
		}
	}

	/**
	 * Character scanner returning uppercased characters of the wrapped scanner.
	 * 
	 * @since 3.0
	 */
	private class UppercaseScanner implements ICharacterScanner {
		
		/**
		 * The wrapped scanner.
		 */
		private ICharacterScanner fScanner;
		
		/**
		 * Set the scanner
		 * 
		 * @param scanner the scanner
		 */
		public void setScanner(ICharacterScanner scanner) {
			fScanner= scanner;
		}
		
		/*
		 * @see org.eclipse.jface.text.rules.ICharacterScanner#getLegalLineDelimiters()
		 */
		public char[][] getLegalLineDelimiters() {
			return fScanner.getLegalLineDelimiters();
		}

		/*
		 * @see org.eclipse.jface.text.rules.ICharacterScanner#getColumn()
		 */
		public int getColumn() {
			return fScanner.getColumn();
		}

		/*
		 * @see org.eclipse.jface.text.rules.ICharacterScanner#read()
		 */
		public int read() {
			int ch= fScanner.read();
			return ch != EOF ? Character.toUpperCase((char) ch) : EOF;
		}

		/*
		 * @see org.eclipse.jface.text.rules.ICharacterScanner#unread()
		 */
		public void unread() {
			fScanner.unread();
		}
	}
	
	private class TaskTagRule extends WordRule {

		private IToken fToken;
		/**
		 * Uppercase words
		 * @since 3.0
		 */
		private Map fUppercaseWords= new HashMap();
		/**
		 * Original words
		 * @since 3.0
		 */
		private Map fOriginalWords= fWords;
		/**
		 * Uppercase scanner
		 * @since 3.0
		 */
		private UppercaseScanner fUppercaseScanner= new UppercaseScanner();
		/**
		 * <code>true</code> if task tag detection is case-sensitive.
		 * @since 3.0
		 */
		private boolean fCaseSensitive= true;
		
		public TaskTagRule(IToken token, IToken defaultToken) {
			super(new TaskTagDetector(), defaultToken);
			fToken= token;
		}
	
		public void clearTaskTags() {
			fOriginalWords.clear();
			fUppercaseWords.clear();
		}
	
		public void addTaskTags(String value) {
			String[] tasks= split(value, ","); //$NON-NLS-1$
			for (int i= 0; i < tasks.length; i++) {
				if (tasks[i].length() > 0) {
					addWord(tasks[i], fToken);
				}
			}
		}
		
		private String[] split(String value, String delimiters) {
			StringTokenizer tokenizer= new StringTokenizer(value, delimiters);
			int size= tokenizer.countTokens();
			String[] tokens= new String[size];
			int i= 0;
			while (i < size)
				tokens[i++]= tokenizer.nextToken();
			return tokens;
		}
		
		/*
		 * @see org.eclipse.jface.text.rules.WordRule#addWord(java.lang.String, org.eclipse.jface.text.rules.IToken)
		 * @since 3.0
		 */
		public void addWord(String word, IToken token) {
			Assert.isNotNull(word);
			Assert.isNotNull(token);		
		
			fOriginalWords.put(word, token);
			fUppercaseWords.put(word.toUpperCase(), token);
		}
		
		/*
		 * @see IRule#evaluate(ICharacterScanner)
		 * @since 3.0
		 */
		public IToken evaluate(final ICharacterScanner scanner) {
			if (fCaseSensitive) {
				fWords= fOriginalWords;
				return super.evaluate(scanner);
			}
			
			fWords= fUppercaseWords;
			fUppercaseScanner.setScanner(scanner);
			return super.evaluate(fUppercaseScanner);
		}
		
		/**
		 * Is task tag detection case-senstive?
		 * 
		 * @return <code>true</code> iff task tag detection is case-sensitive
		 * @since 3.0
		 */
		public boolean isCaseSensitive() {
			return fCaseSensitive;
		}
		
		/**
		 * Enables/disables the case-sensitivity of the task tag detection.
		 * 
		 * @param caseSensitive <code>true</code> iff case-sensitivity should be enabled
		 * @since 3.0
		 */
		public void setCaseSensitive(boolean caseSensitive) {
			fCaseSensitive= caseSensitive;
		}
	}
	
	private static final String COMPILER_TASK_TAGS= JavaCore.COMPILER_TASK_TAGS;	
	protected static final String TASK_TAG= IJavaColorConstants.TASK_TAG;
	/**
	 * Preference key of a string preference, specifying if task tag detection is case-sensitive.
	 * @since 3.0
	 */
	private static final String COMPILER_TASK_CASE_SENSITIVE= JavaCore.COMPILER_TASK_CASE_SENSITIVE;
	/**
	 * Preference value of enabled preferences.
	 * @since 3.0
	 */
	private static final String ENABLED= JavaCore.ENABLED;

	private TaskTagRule fTaskTagRule;
	private Preferences fCorePreferenceStore;
	private String fDefaultTokenProperty;
	private String[] fTokenProperties;

	public JavaCommentScanner(IColorManager manager, IPreferenceStore store, Preferences coreStore, String defaultTokenProperty) {
		this(manager, store, coreStore, defaultTokenProperty, new String[] { defaultTokenProperty, TASK_TAG });
	}
	
	public JavaCommentScanner(IColorManager manager, IPreferenceStore store, Preferences coreStore, String defaultTokenProperty, String[] tokenProperties) {
		super(manager, store);
		
		fCorePreferenceStore= coreStore;
		fDefaultTokenProperty= defaultTokenProperty;
		fTokenProperties= tokenProperties;

		initialize();
	}

	/**
	 * Initialize with the given arguments.
	 * 
	 * @param manager Color manager
	 * @param store Preference store
	 * @param defaultTokenProperty Default token property
	 * 
	 * @since 3.0	
	 */
	public JavaCommentScanner(IColorManager manager, IPreferenceStore store, String defaultTokenProperty) {
		this(manager, store, null, defaultTokenProperty, new String[] { defaultTokenProperty, TASK_TAG });
	}
	
	/**
	 * Initialize with the given arguments.
	 * 
	 * @param manager Color manager
	 * @param store Preference store
	 * @param defaultTokenProperty Default token property
	 * @param tokenProperties Token properties
	 * 
	 * @since 3.0
	 */
	public JavaCommentScanner(IColorManager manager, IPreferenceStore store, String defaultTokenProperty, String[] tokenProperties) {
		this(manager, store, null, defaultTokenProperty, tokenProperties);
	}

	/*
	 * @see AbstractJavaScanner#createRules()
	 */
	protected List createRules() {
		List list= new ArrayList();
		
		// Add rule for Task Tags.
		boolean isCaseSensitive= true;
		String tasks= null;
		if (getPreferenceStore().contains(COMPILER_TASK_TAGS)) {
			tasks= getPreferenceStore().getString(COMPILER_TASK_TAGS);
			isCaseSensitive= ENABLED.equals(getPreferenceStore().getString(COMPILER_TASK_CASE_SENSITIVE));
		} else if (fCorePreferenceStore != null) {
			tasks= fCorePreferenceStore.getString(COMPILER_TASK_TAGS);
			isCaseSensitive= ENABLED.equals(fCorePreferenceStore.getString(COMPILER_TASK_CASE_SENSITIVE));
		}
		if (tasks != null) {
			fTaskTagRule= new TaskTagRule(getToken(TASK_TAG), getToken(fDefaultTokenProperty));
			fTaskTagRule.addTaskTags(tasks);
			fTaskTagRule.setCaseSensitive(isCaseSensitive);
			list.add(fTaskTagRule);
		}

		setDefaultReturnToken(getToken(fDefaultTokenProperty));

		return list;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.AbstractJavaScanner#affectsBehavior(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public boolean affectsBehavior(PropertyChangeEvent event) {
		return event.getProperty().equals(COMPILER_TASK_TAGS) || event.getProperty().equals(COMPILER_TASK_CASE_SENSITIVE) || super.affectsBehavior(event);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.AbstractJavaScanner#adaptToPreferenceChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (fTaskTagRule != null && event.getProperty().equals(COMPILER_TASK_TAGS)) {
			Object value= event.getNewValue();
			if (value instanceof String) {
				fTaskTagRule.clearTaskTags();
				fTaskTagRule.addTaskTags((String) value);
			}
		} else if (fTaskTagRule != null && event.getProperty().equals(COMPILER_TASK_CASE_SENSITIVE)) {
			Object value= event.getNewValue();
			if (value instanceof String)
				fTaskTagRule.setCaseSensitive(ENABLED.equals(value));
		} else if (super.affectsBehavior(event))
			super.adaptToPreferenceChange(event);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.AbstractJavaScanner#getTokenProperties()
	 */
	protected String[] getTokenProperties() {
		return fTokenProperties;
	}

}

