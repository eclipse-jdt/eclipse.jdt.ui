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
package org.eclipse.jdt.internal.ui.search;

import java.util.List;

import org.eclipse.jface.text.IDocument;


import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.CompilationUnit;

public interface IOccurrencesFinder {
	
	public String initialize(CompilationUnit root, int offset, int length);
	
	public List perform();
	
	public String getJobLabel();

	public String getPluralLabelPattern(String documentName);
	
	public String getSingularLabel(String documentName);
	
	public Match[] getOccurrenceMatches(IJavaElement element, IDocument document);
}
