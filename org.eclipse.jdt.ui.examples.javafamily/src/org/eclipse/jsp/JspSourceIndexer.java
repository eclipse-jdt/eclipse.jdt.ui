/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.jdt.internal.core.index.IDocument;
import org.eclipse.jdt.internal.core.index.IIndexer;
import org.eclipse.jdt.internal.core.index.IIndexerOutput;


public class JspSourceIndexer implements IIndexer {
	
	private static final String[] JSP_FILE_TYPES= new String[] { JspUIPlugin.JSP_TYPE }; //$NON-NLS-1$

	private IIndexerOutput fOutput;
			
	public String[] getFileTypes() {
		return JSP_FILE_TYPES;
	}

	public void setFileTypes(String[] fileTypes) {
		// empty implementation
	}

	protected void indexFile(IDocument document) {
		
		// Add the name of the file to the index
		fOutput.addDocument(document);
				
		try {
			Reader reader= new StringReader(document.getStringContent());
			JspIndexParser tr= new JspIndexParser(fOutput);
			tr.parse(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void index(IDocument document, IIndexerOutput indexerOutput) throws IOException {
		fOutput= indexerOutput;
		if (shouldIndex(document))
			indexFile(document);
	}
	
	public boolean shouldIndex(IDocument document) {
		String type= document.getType();
		String[] supportedTypes= getFileTypes();
		for (int i= 0; i < supportedTypes.length; ++i)
			if (supportedTypes[i].equals(type))
				return true;
		return false;
	}
}
