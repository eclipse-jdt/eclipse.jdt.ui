/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.spelling;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker;


/**
 * Java spelling engine
 *
 * @since 3.1
 */
public class JavaSpellingEngine extends SpellingEngine {


	/*
	 * @see org.eclipse.jdt.internal.ui.text.spelling.SpellingEngine#check(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IRegion[], org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker, org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void check(IDocument document, IRegion[] regions, ISpellChecker checker, ISpellingProblemCollector collector, IProgressMonitor monitor) {
		SpellEventListener listener= new SpellEventListener(collector, document);
		boolean isIgnoringJavaStrings= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SPELLING_IGNORE_JAVA_STRINGS);
		try {
			for (IRegion region : regions) {
				for (ITypedRegion partition : TextUtilities.computePartitioning(document, IJavaPartitions.JAVA_PARTITIONING, region.getOffset(), region.getLength(), false)) {
					if (monitor != null && monitor.isCanceled())
						return;
					if (listener.isProblemsThresholdReached())
						return;
					final String type= partition.getType();
					if (isIgnoringJavaStrings && IJavaPartitions.JAVA_STRING.equals(type))
						continue;
					if (!IDocument.DEFAULT_CONTENT_TYPE.equals(type) && !IJavaPartitions.JAVA_CHARACTER.equals(type))
						checker.execute(listener, new SpellCheckIterator(document, partition, checker.getLocale(), monitor));
				}
			}
		} catch (BadLocationException | AssertionFailedException x) {
			// ignore: the document has been changed in another thread and will be checked again
		}
	}
}
