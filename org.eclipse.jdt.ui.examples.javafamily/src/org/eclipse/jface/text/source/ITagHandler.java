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

package org.eclipse.jface.text.source;

import java.util.ArrayList;


/**
 * A tag handler is responsible to
 * - handle the attributes for that tag
 * - translate the tag sequence including attributes to another language
 * - back-translate relative line offsets 
 * 
 * @since 3.0
 */
public interface ITagHandler {
	
	boolean canHandleTag(String tag);
	
	boolean canHandleText(String text);
	
	void addAttribute(String name, String value);
	
	void reset(String tag);
	
	void translate(StringBuffer declBuffer, StringBuffer localDeclBuffer, StringBuffer contentBuffer, ArrayList dec, ArrayList localDec, ArrayList content, int line);
	
	void translate(StringBuffer[] targetBuffers, ArrayList[] lineMappingInfos, int startingLine, int endingLine);	
	
	int backTranslateOffsetInLine(String originalLine, String translatedLine, int offsetInTranslatedLine);
}
