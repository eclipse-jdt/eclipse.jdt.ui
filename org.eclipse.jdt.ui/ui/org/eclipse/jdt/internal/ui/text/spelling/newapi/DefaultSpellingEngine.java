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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ui.texteditor.spelling.ISpellingEngine;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingContext;

/**
 * Default spelling engine.
 * 
 * @since 3.1
 */
public class DefaultSpellingEngine implements ISpellingEngine {
	
	private static final String JAVA_CONTENT_TYPE_ID= "org.eclipse.jdt.core.javaSource"; //$NON-NLS-1$
	
//	private static final String PROPERTIES_CONTENT_TYPE_ID= "org.eclipse.jdt.core.javaProperties"; //$NON-NLS-1$
	
	private Map fEngines= new HashMap();
	
	private ISpellingEngine fDefaultEngine;

	public DefaultSpellingEngine() {
		fEngines.put(JAVA_CONTENT_TYPE_ID, new JavaSpellingEngine());
//		fEngines.put(PROPERTIES_CONTENT_TYPE_ID, new PropertiesFileSpellingEngine());
		fDefaultEngine= new TextSpellingEngine();
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.spelling.ISpellingEngine#check(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IRegion[], org.eclipse.ui.texteditor.spelling.SpellingContext, org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void check(IDocument document, IRegion[] regions, SpellingContext context, ISpellingProblemCollector collector, IProgressMonitor monitor) {
		String contentTypeId= context.getContentType().getId();
		if (fEngines.containsKey(contentTypeId))
			((ISpellingEngine) fEngines.get(contentTypeId)).check(document, regions, context, collector, monitor);
		else
			fDefaultEngine.check(document, regions, context, collector, monitor);
	}
}
