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

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.index.IDocument;
import org.eclipse.jdt.internal.core.index.IIndexerOutput;
import org.eclipse.jdt.internal.core.jdom.CompilationUnit;
import org.eclipse.jsp.copied_from_jdtcore.SourceIndexer;
import org.eclipse.jsp.copied_from_jdtcore.SourceIndexerRequestor;


public class JspJavaSourceIndexer extends SourceIndexer {
	
	private static final String[] JSP_FILE_TYPES= new String[] { JspUIPlugin.JSP_TYPE }; //$NON-NLS-1$
	
	JspJavaSourceIndexer(IFile resourceFile) {
		super(resourceFile);
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.core.index.IIndexer#getFileTypes()
	 */
	public String[] getFileTypes() {
		return JSP_FILE_TYPES;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.core.index.IIndexer#setFileTypes(java.lang.String[])
	 */
	public void setFileTypes(String[] fileTypes) {
		// empty implementation
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.core.index.IIndexer#shouldIndex(org.eclipse.jdt.internal.core.index.IDocument)
	 */
	public boolean shouldIndex(IDocument document) {
		return true;
	}
	
	protected void indexFile(IDocument document) {
		
		IIndexerOutput output= getOutput();
		
		// Add the name of the file to the index
		output.addDocument(document);
		
		// preprocess JSP
		String n= document.getName();
		int pos= n.lastIndexOf('/');
		if (pos > 0)
			n= n.substring(pos+1);
		n= n.replace('.', '_');
		
		JspTranslator jspParser= null;
		String content;
		String java= null;
		
		try {
			content= document.getStringContent();
			Reader reader= new StringReader(content);
			jspParser= new JspTranslator();
			java= jspParser.translate(reader, n);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (java == null)
			return;
		
		// Java
		if (false) {
			
			if (true) {
				System.out.println("============"); //$NON-NLS-1$
				System.out.println(java);
				System.out.println("------------"); //$NON-NLS-1$
			}

			char[] source= Util.getChars(java);
			char[] name= Util.getChars(n + ".java");	 //$NON-NLS-1$
			
			// Create a new Parser
			SourceIndexerRequestor requestor = new SourceIndexerRequestor(this, document);
			SourceElementParser parser = new SourceElementParser(
				requestor, 
				problemFactory, 
				new CompilerOptions(JavaCore.create(getResourceFile().getProject()).getOptions(true)), 
				true); // index local declarations
	
			// Launch the parser
			if (source == null || name == null) return; // could not retrieve document info (e.g. resource was discarded)
			CompilationUnit compilationUnit = new CompilationUnit(source, name);
			try {
				parser.parseCompilationUnit(compilationUnit, true/*full parse*/);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
