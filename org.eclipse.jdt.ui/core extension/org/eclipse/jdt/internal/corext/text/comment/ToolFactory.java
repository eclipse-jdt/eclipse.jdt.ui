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
package org.eclipse.jdt.internal.corext.text.comment;

import java.util.Map;

import org.eclipse.jdt.core.formatter.CodeFormatter;

/**
 * TODO merge with JDT/Core ToolFactory
 * @since 3.1
 */
public class ToolFactory {

	/**
	 * Create a comment formatter.
	 * 
	 * @param textMeasurement the text measurement to use for formatting
	 * @param options the options map to use for formatting
	 * @return a comment formatter
	 * @since 3.1
	 */
	public static CodeFormatter createCommentFormatter(ITextMeasurement textMeasurement, Map options){
		return new CommentFormatter(textMeasurement, options);
	}
}
