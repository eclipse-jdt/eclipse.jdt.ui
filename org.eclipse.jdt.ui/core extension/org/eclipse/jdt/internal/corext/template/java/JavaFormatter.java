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
package org.eclipse.jdt.internal.corext.template.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

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
				format(buffer, (JavaContext) context);
			else
				indentate(buffer);

			trimBegin(buffer);
		} catch (CoreException e) {
			// TODO: handle exception
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

	private void format(TemplateBuffer templateBuffer, JavaContext context) throws CoreException {
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
	
	private void plainFormat(TemplateBuffer templateBuffer, JavaContext context) {
		
		String string= templateBuffer.getString();
		IDocument doc= new Document(context.getDocument().get());
		
		int start= context.getStart();
		try {
			doc.replace(start, context.getEnd() - start, string);
		} catch (BadLocationException e) {
			return; // don't format if the document has changed
		}

		TemplateVariable[] variables= templateBuffer.getVariables();

		int[] offsets= variablesToOffsets(variables, start);
		
		Map options;
		if (context.getCompilationUnit() != null)
			options= context.getCompilationUnit().getJavaProject().getOptions(true); 
		else
			options= JavaCore.getOptions();
		
		string= CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, doc.get(), start, string.length(), 0, offsets, fLineDelimiter, options);
		
		offsetsToVariables(offsets, variables, start);

		templateBuffer.setContent(string, variables);	    
	}

	private void indentate(TemplateBuffer templateBuffer) throws CoreException {

		String string= templateBuffer.getString();
		TemplateVariable[] variables= templateBuffer.getVariables();

		List positions= variablesToPositions(variables);
		List edits= new ArrayList(5);
		
		TextBuffer textBuffer= TextBuffer.create(string);
	    int lineCount= textBuffer.getNumberOfLines();
	    for (int i= 0; i < lineCount; i++) {
	    	TextRegion region= textBuffer.getLineInformation(i);
			edits.add(new InsertEdit(region.getOffset(), CodeFormatterUtil.createIndentString(fInitialIndentLevel)));

			String lineDelimiter= textBuffer.getLineDelimiter(i);
			if (lineDelimiter != null)
				edits.add(new ReplaceEdit(region.getOffset() + region.getLength(), lineDelimiter.length(), fLineDelimiter));
	    }

		string= edit(string, positions, edits);
		positionsToVariables(positions, variables);
		
		templateBuffer.setContent(string, variables);
	}

	private static void trimBegin(TemplateBuffer templateBuffer) throws CoreException {
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
	
	private static String edit(String string, List positions, TextEdit edit) throws CoreException {
	    TextBuffer textBuffer= TextBuffer.create(string);
		TextBufferEditor editor= new TextBufferEditor(textBuffer);
		addEdits(editor, positions);
		editor.add(edit);
		editor.performEdits(null);
		
		return textBuffer.getContent();
	}

	private static String edit(String string, List positions, List edits) throws CoreException {
	    TextBuffer textBuffer= TextBuffer.create(string);
		TextBufferEditor editor= new TextBufferEditor(textBuffer);
		addEdits(editor, positions);
		addEdits(editor, edits);
		editor.performEdits(null);
		
		return textBuffer.getContent();
	}
		
	private static int[] variablesToOffsets(TemplateVariable[] variables, int start) {
		Vector vector= new Vector();
		for (int i= 0; i != variables.length; i++) {
		    int[] offsets= variables[i].getOffsets();
		    for (int j= 0; j != offsets.length; j++)
				vector.add(new Integer(offsets[j]));
		}
		
		int[] offsets= new int[vector.size()];
		for (int i= 0; i != offsets.length; i++)
			offsets[i]= ((Integer) vector.get(i)).intValue() + start;

		Arrays.sort(offsets);

		return offsets;	    
	}
	
	private static void offsetsToVariables(int[] allOffsets, TemplateVariable[] variables, int start) {
		int[] currentIndices= new int[variables.length];
		for (int i= 0; i != currentIndices.length; i++)
			currentIndices[i]= 0;

		int[][] offsets= new int[variables.length][];
		for (int i= 0; i != variables.length; i++)
			offsets[i]= variables[i].getOffsets();
		
		for (int i= 0; i != allOffsets.length; i++) {

			int min= Integer.MAX_VALUE;
			int minVariableIndex= -1;
			for (int j= 0; j != variables.length; j++) {
			    int currentIndex= currentIndices[j];
			    
			    // determine minimum
				if (currentIndex == offsets[j].length)
					continue;
					
				int offset= offsets[j][currentIndex];

				if (offset < min) {
				    min= offset;
					minVariableIndex= j;
				}		
			}

			offsets[minVariableIndex][currentIndices[minVariableIndex]]= allOffsets[i] - start;
			currentIndices[minVariableIndex]++;
		}

		for (int i= 0; i != variables.length; i++)
			variables[i].setOffsets(offsets[i]);	
	}

	private static List variablesToPositions(TemplateVariable[] variables) {
   		List positions= new ArrayList(5);
		for (int i= 0; i != variables.length; i++) {
		    int[] offsets= variables[i].getOffsets();
		    for (int j= 0; j != offsets.length; j++)
				positions.add(new RangeMarker(offsets[j], 0));
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

	private static void addEdits(TextBufferEditor editor, List edits) {
		for (Iterator iter= edits.iterator(); iter.hasNext();) {
			editor.add((TextEdit) iter.next());
		}
	}
}
