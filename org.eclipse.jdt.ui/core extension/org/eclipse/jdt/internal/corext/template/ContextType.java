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
package org.eclipse.jdt.internal.corext.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.textmanipulation.RangeMarker;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

/**
 * A context type is a context factory.
 */
public abstract class ContextType implements ITemplateEditor {

	/** name of the context type */
	private final String fName;

	/** variables used by this content type */
	private final Map fVariables= new HashMap();

	/**
	 * Creates a context type with a name.
	 * 
	 * @param name the name of the context. It has to be unique wrt to other context names.
	 */
	public ContextType(String name) {
		fName= name;   
	}

	/**
	 * Returns the name of the context type.
	 */
	public String getName() {
	    return fName;
	}
	
	/**
	 * Adds a template variable to the context type.
	 */
	public void addVariable(TemplateVariable variable) {
		fVariables.put(variable.getName(), variable);   
	}
	
	/**
	 * Removes a template variable from the context type.
	 */
	public void removeVariable(TemplateVariable variable) {
		fVariables.remove(variable.getName());
	}

	/**
	 * Removes all template variables from the context type.
	 */
	public void removeAllVariables() {
		fVariables.clear();
	}

	/**
	 * Returns an iterator for the variables known to the context type.
	 */
	public Iterator variableIterator() {
	 	return fVariables.values().iterator();   
	}
	
	/**
	 * Returns the variable with the given name
	 */
	protected TemplateVariable getVariable(String name) {
		return (TemplateVariable) fVariables.get(name);
	}	

	/**
	 * Validates a pattern and returnes <code>null</code> if the validation was
	 * a success or an error message if not.
	 */
	public String validate(String pattern) throws CoreException {
		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(pattern);
		if (buffer != null) {
			return validateVariables(buffer.getVariables());
		}
		return translator.getErrorMessage();
	}
	
	protected String validateVariables(TemplatePosition[] variables) {
		return null;
	}

    /*
     * @see ITemplateEditor#edit(TemplateBuffer)
     */
    public void edit(TemplateBuffer templateBuffer, TemplateContext context) throws CoreException {
		TextBuffer textBuffer= TextBuffer.create(templateBuffer.getString());
		TemplatePosition[] variables= templateBuffer.getVariables();

		List positions= variablesToPositions(variables);
		List edits= new ArrayList(5);

        // iterate over all variables and try to resolve them
        for (int i= 0; i != variables.length; i++) {
            TemplatePosition variable= variables[i];

			if (variable.isResolved())
				continue;			

			String name= variable.getName();
			int[] offsets= variable.getOffsets();
			int length= variable.getLength();
			
			TemplateVariable evaluator= (TemplateVariable) fVariables.get(name);
			String value= (evaluator == null)
				? null
				: evaluator.evaluate(context);
			
			if (value == null)
				continue;

			variable.setLength(value.length());
			variable.setResolved(evaluator.isResolved(context));

        	for (int k= 0; k != offsets.length; k++)
				edits.add(SimpleTextEdit.createReplace(offsets[k], length, value));
        }

		TextBufferEditor editor= new TextBufferEditor(textBuffer);
		addEdits(editor, positions);
		addEdits(editor, edits);
        editor.performEdits(null);

		positionsToVariables(positions, variables);
        
        templateBuffer.setContent(textBuffer.getContent(), variables);
    }

	private static void addEdits(TextBufferEditor editor, List edits) throws CoreException {
		for (Iterator iter= edits.iterator(); iter.hasNext();) {
			editor.add((TextEdit) iter.next());
		}
	}

	private static List variablesToPositions(TemplatePosition[] variables) {
   		List positions= new ArrayList(5);
		for (int i= 0; i != variables.length; i++) {
		    int[] offsets= variables[i].getOffsets();
		    for (int j= 0; j != offsets.length; j++)
				positions.add(new RangeMarker(offsets[j], 0));
		}
		
		return positions;
	}
	
	private static void positionsToVariables(List positions, TemplatePosition[] variables) {
		Iterator iterator= positions.iterator();
		
		for (int i= 0; i != variables.length; i++) {
		    TemplatePosition variable= variables[i];
		    
			int[] offsets= new int[variable.getOffsets().length];
			for (int j= 0; j != offsets.length; j++)
				offsets[j]= ((TextEdit) iterator.next()).getTextRange().getOffset();
			
		 	variable.setOffsets(offsets);   
		}
	}

}
