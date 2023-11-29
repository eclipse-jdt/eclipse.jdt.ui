/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

package org.eclipse.jface.text.source.translation;

import java.io.IOException;


/**
 * A tag handler is responsible to
 * - handle the attributes for the tags it supports
 * - translate the tag sequence including attributes to another language
 * - back-translate relative line offsets.
 * <p>
 * Tag handlers are used by translators via tag handler factories. The factory
 * can either return a new tag or one that already has some attributes. </p>
 *
 * @see org.eclipse.jface.text.source.translation.ITranslator
 * @see org.eclipse.jface.text.source.translation.ITagHandlerFactory
 * @since 3.0
 */
public interface ITagHandler {

	/**
	 * Tells whether this handler can handle the given tag.
	 *
	 * @param tag the tag to check
	 * @return <code>true</code> if this handler handles the given tag
	 */
	boolean canHandleTag(String tag);

	/**
	 * Tells whether this handler can handle the given text. Most
	 * likely the handler will check if the text contains a tag
	 * that he can handle.
	 *
	 * @param text the text to check
	 * @return <code>true</code> if this handler handles the given text
	 */
	boolean canHandleText(String text);

	/**
	 * Adds an attribute to this tag handler.
	 *
	 * @param name				the name of the attribute
	 * @param value			the attribute value
	 * @param sourceLineNumber the line number of the attribute in the source or <code>-1</code> if unknown
	 */
	void addAttribute(String name, String value, int sourceLineNumber);

	/**
	 * Resets this handler and sets the current tag to the given tag.
	 * A handler can handle more than one tag but only one tag at a time.
	 * <p>
	 * Resetting the handler clears the attributes.</p>
	 *
	 * @param tag the tag to check
	 */
	void reset(String tag);

	/**
	 * Writes the tag and line mapping information to the
	 * given translator result collector.
	 *
	 * @param resultCollector the translator's result collector
	 * @param sourceLineNumber the line number of the attribute in the source or <code>-1</code> if unknown
	 */
	void processEndTag(ITranslatorResultCollector resultCollector, int sourceLineNumber) throws IOException;

	/**
	 * Computes the offset in the source line that corresponds
	 * to the given offset in the translated line.
	 *
	 * @param sourceLine				the source line
	 * @param translatedLine			the translated line
	 * @param offsetInTranslatedLine	the offset in the translated line
	 * @return the offset in the source line or <code>-1</code> if
	 * 			it was not possible to compute the offset
	 */
	int backTranslateOffsetInLine(String sourceLine, String translatedLine, int offsetInTranslatedLine);
}
