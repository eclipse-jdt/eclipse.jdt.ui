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
package org.eclipse.jdt.internal.corext.template.java;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.jdt.internal.ui.text.JavaIndenter;

/**
 * A template editor using the Java formatter to format a template buffer.
 */
public class JavaFormatter {

	private static final String MARKER= "/*${" + GlobalTemplateVariables.Cursor.NAME + "}*/"; //$NON-NLS-1$ //$NON-NLS-2$

	/** The line delimiter to use if code formatter is not used. */
	private final String fLineDelimiter;
	/** The initial indent level */
	private final int fInitialIndentLevel;
	
	/** The java partitioner */
	private boolean fUseCodeFormatter;

	/**
	 * Creates a JavaFormatter with the target line delimiter.
	 * 
	 * @param lineDelimiter the line delimiter to use
	 * @param initialIndentLevel the initial indentation level
	 * @param useCodeFormatter <code>true</code> if the core code formatter should be used
	 */
	public JavaFormatter(String lineDelimiter, int initialIndentLevel, boolean useCodeFormatter) {
		fLineDelimiter= lineDelimiter;
		fUseCodeFormatter= useCodeFormatter;
		fInitialIndentLevel= initialIndentLevel;
	}

	/**
	 * Formats the template buffer.
	 * @param buffer
	 * @param context
	 * @throws BadLocationException
	 */
	public void format(TemplateBuffer buffer, TemplateContext context) throws BadLocationException {
		try {
			if (fUseCodeFormatter)
				// try to format and fall back to indenting
				try {
					format(buffer, (JavaContext) context);
				} catch (BadLocationException e) {
					indent(buffer);
				} catch (MalformedTreeException e) {
					indent(buffer);
				}
			else
				indent(buffer);

			// don't trim the buffer if the replacement area is empty
			// case: surrounding empty lines with block
			if (context instanceof DocumentTemplateContext) {
				DocumentTemplateContext dtc= (DocumentTemplateContext) context;
				if (dtc.getStart() == dtc.getCompletionOffset())
					if (dtc.getDocument().get(dtc.getStart(), dtc.getEnd() - dtc.getEnd()).trim().length() == 0)
						return;
			}
			
			trimBegin(buffer);
		} catch (MalformedTreeException e) {
			throw new BadLocationException();
		}
	}

	private static int getCaretOffset(TemplateVariable[] variables) {
	    for (int i= 0; i != variables.length; i++) {
	        TemplateVariable variable= variables[i];
	        
	        if (variable.getType().equals(GlobalTemplateVariables.Cursor.NAME))
	        	return variable.getOffsets()[0];
	    }
	    
	    return -1;
	}
	
	private boolean isInsideCommentOrString(String string, int offset) {

		IDocument document= new Document(string);
		JavaPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(document);

		try {		
			ITypedRegion partition= document.getPartition(offset);
			String partitionType= partition.getType();
		
			return partitionType != null && (
				partitionType.equals(IJavaPartitions.JAVA_MULTI_LINE_COMMENT) ||
				partitionType.equals(IJavaPartitions.JAVA_SINGLE_LINE_COMMENT) ||
				partitionType.equals(IJavaPartitions.JAVA_STRING) ||
				partitionType.equals(IJavaPartitions.JAVA_CHARACTER) ||
				partitionType.equals(IJavaPartitions.JAVA_DOC));

		} catch (BadLocationException e) {
			return false;	
		}
	}

	private void format(TemplateBuffer templateBuffer, JavaContext context) throws BadLocationException {
		// XXX 4360, 15247
		// workaround for code formatter limitations
		// handle a special case where cursor position is surrounded by whitespaces		

		String string= templateBuffer.getString();
		TemplateVariable[] variables= templateBuffer.getVariables();

		int caretOffset= getCaretOffset(variables);
		if ((caretOffset > 0) && Character.isWhitespace(string.charAt(caretOffset - 1)) &&
			(caretOffset < string.length()) && Character.isWhitespace(string.charAt(caretOffset)) &&
			! isInsideCommentOrString(string, caretOffset))
		{
			List positions= variablesToPositions(variables);

		    TextEdit insert= new InsertEdit(caretOffset, MARKER);
		    string= edit(string, positions, insert);
			positionsToVariables(positions, variables);
		    templateBuffer.setContent(string, variables);

			plainFormat(templateBuffer, context);			

			string= templateBuffer.getString();
			variables= templateBuffer.getVariables();
			caretOffset= getCaretOffset(variables);

			positions= variablesToPositions(variables);
			TextEdit delete= new DeleteEdit(caretOffset, MARKER.length());
		    string= edit(string, positions, delete);
			positionsToVariables(positions, variables);		    
		    templateBuffer.setContent(string, variables);
	
		} else {
			plainFormat(templateBuffer, context);			
		}	    
	}
	
	private void plainFormat(TemplateBuffer templateBuffer, JavaContext context) throws BadLocationException {
		
		IDocument doc= new Document(templateBuffer.getString());
		
		TemplateVariable[] variables= templateBuffer.getVariables();
		
		List offsets= variablesToPositions(variables);
		
		Map options;
		if (context.getCompilationUnit() != null)
			options= context.getCompilationUnit().getJavaProject().getOptions(true); 
		else
			options= JavaCore.getOptions();
		
		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_UNKNOWN, doc.get(), fInitialIndentLevel, fLineDelimiter, options);
		if (edit == null)
			throw new BadLocationException(); // fall back to indenting
		
		MultiTextEdit root;
		if (edit instanceof MultiTextEdit)
			root= (MultiTextEdit) edit;
		else {
			root= new MultiTextEdit(0, doc.getLength());
			root.addChild(edit);
		}
		for (Iterator it= offsets.iterator(); it.hasNext();) {
			TextEdit position= (TextEdit) it.next();
			try {
				root.addChild(position);
			} catch (MalformedTreeException e) {
				// position conflicts with formatter edit
				// ignore this position
			}
		}
		
		root.apply(doc, TextEdit.UPDATE_REGIONS);
		
		positionsToVariables(offsets, variables);
		
		templateBuffer.setContent(doc.get(), variables);	    
	}	

	private void indent(TemplateBuffer templateBuffer) throws BadLocationException, MalformedTreeException {

		TemplateVariable[] variables= templateBuffer.getVariables();
		List positions= variablesToPositions(variables);
		
		IDocument document= new Document(templateBuffer.getString());
		MultiTextEdit root= new MultiTextEdit(0, document.getLength());
		root.addChildren((TextEdit[]) positions.toArray(new TextEdit[positions.size()]));
		
		// first line
		int offset= document.getLineOffset(0);
		TextEdit edit= new InsertEdit(offset, CodeFormatterUtil.createIndentString(fInitialIndentLevel));
		root.addChild(edit);
		root.apply(document, TextEdit.UPDATE_REGIONS);
		root.removeChild(edit);
		
		formatDelimiter(document, root, 0);
		
		// following lines
	    int lineCount= document.getNumberOfLines();
	    JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
	    JavaIndenter indenter= new JavaIndenter(document, scanner);
	    
	    for (int line= 1; line < lineCount; line++) {
			IRegion region= document.getLineInformation(line);
			offset= region.getOffset();
	    	StringBuffer indent= indenter.computeIndentation(offset);
	    	if (indent == null)
	    		continue;
	    	int nonWS= scanner.findNonWhitespaceForwardInAnyPartition(offset, offset + region.getLength());
	    	if (nonWS == JavaHeuristicScanner.NOT_FOUND)
	    		continue;
	    		
	    	edit= new ReplaceEdit(offset, nonWS - offset, indent.toString());
			root.addChild(edit);
			root.apply(document, TextEdit.UPDATE_REGIONS);
			root.removeChild(edit);

			formatDelimiter(document, root, line);
	    }
	    
		positionsToVariables(positions, variables);
		templateBuffer.setContent(document.get(), variables);
	}

	/**
	 * Changes the delimiter to the configured line delimiter.
	 * 
	 * @param document the temporary document being edited
	 * @param root the root edit containing all positions that will be updated along the way
	 * @param line the line to format
	 * @throws BadLocationException if applying the changes fails
	 */
	private void formatDelimiter(IDocument document, MultiTextEdit root, int line) throws BadLocationException {
		IRegion region= document.getLineInformation(line);
		String lineDelimiter= document.getLineDelimiter(line);
		if (lineDelimiter != null) {
			TextEdit edit= new ReplaceEdit(region.getOffset() + region.getLength(), lineDelimiter.length(), fLineDelimiter);
			root.addChild(edit);
			root.apply(document, TextEdit.UPDATE_REGIONS);
			root.removeChild(edit);
		}
	}

	private static void trimBegin(TemplateBuffer templateBuffer) throws BadLocationException {
		String string= templateBuffer.getString();
		TemplateVariable[] variables= templateBuffer.getVariables();

		List positions= variablesToPositions(variables);

		int i= 0;
		while ((i != string.length()) && Character.isWhitespace(string.charAt(i)))
			i++;

		string= edit(string, positions, new DeleteEdit(0, i));
		positionsToVariables(positions, variables);

		templateBuffer.setContent(string, variables);
	}
	
	
	private static String edit(String string, List positions, TextEdit edit) throws BadLocationException {
		MultiTextEdit root= new MultiTextEdit(0, string.length());
		root.addChildren((TextEdit[]) positions.toArray(new TextEdit[positions.size()]));
		root.addChild(edit);
		IDocument document= new Document(string);
		root.apply(document);
		
		return document.get();
	}
	
	private static List variablesToPositions(TemplateVariable[] variables) {
   		List positions= new ArrayList(5);
		for (int i= 0; i != variables.length; i++) {
		    int[] offsets= variables[i].getOffsets();
		    
		    // trim positions off whitespace
		    String value= variables[i].getDefaultValue();
		    int wsStart= 0;
		    while (wsStart < value.length() && Character.isWhitespace(value.charAt(wsStart)) && !Strings.isLineDelimiterChar(value.charAt(wsStart)))
		    	wsStart++;
		    
		    variables[i].getValues()[0]= value.substring(wsStart);
		    
		    for (int j= 0; j != offsets.length; j++) {
		    	offsets[j] += wsStart;
				positions.add(new RangeMarker(offsets[j], 0));
		    }
		}
		return positions;	    
	}
	
	private static void positionsToVariables(List positions, TemplateVariable[] variables) {
		Iterator iterator= positions.iterator();
		
		for (int i= 0; i != variables.length; i++) {
		    TemplateVariable variable= variables[i];
		    
			int[] offsets= new int[variable.getOffsets().length];
			for (int j= 0; j != offsets.length; j++)
				offsets[j]= ((TextEdit) iterator.next()).getOffset();
			
		 	variable.setOffsets(offsets);   
		}
	}	
}
