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
import java.io.Reader;

/**
 * A translator translates a given file into another language.
 * The translator is also responsible to provide line mapping
 * information for the translation and to compute which offset
 * in an original line corresponds to a given offset in a
 * target line.
 *
 * @since 3.0
 */
public interface ITranslator {

	/**
	 * Reads the source from the given reader and creates
	 * translates it into another language. The translated
	 * source might be given the optional name.
	 *
	 * @param reader the reader to access the source
	 * @param name the name of the translated source or <code>null</code> if none
	 */
	String translate(Reader reader, String name) throws IOException;

	/**
	 * Returns the line mapping information.
	 *
	 * @return an int array where the index corresponds to line
	 * 			numbers in the translation and the value is a
	 * 			source line number
	 */
	int[] getLineMapping();

	/**
	 * Assigns an optional tag handler factory to this translator.
	 * <p>
	 * A translator can delegate the handling of individual tags
	 * to tag handlers. The factory is responsible to provide
	 * the correct tag handlers.</p>
	 *
	 * @param tagHandlerFactory	a tag handler factory or <code>null</code>
	 * 								if this translator does all work itself
	 */
	void setTagHandlerFactory(ITagHandlerFactory tagHandlerFactory);

	/**
	 * Computes the offset in the source line that corresponds
	 * to the given offset in the translated line.
	 *
	 * @param sourceLine				the source line
	 * @param translatedLine			the translated line
	 * @param offsetInTranslatedLine	the offset in the translated line
	 * @param tag						the tag to which the source line belongs or
	 *						 			  <code>null</code> if the tag is not known
	 * @return the offset in the source line or <code>-1</code> if
	 * 			it was not possible to compute the offset
	 */
	int backTranslateOffsetInLine(String sourceLine, String translatedLine, int offsetInTranslatedLine, String tag);
}
