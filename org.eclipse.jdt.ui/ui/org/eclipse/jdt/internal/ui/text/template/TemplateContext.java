/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.core.Assert;

public class TemplateContext implements VariableEvaluator {
	
	public static final String JAVA= "java"; //$NON-NLS-1$
	public static final String JAVADOC= "javadoc"; //$NON-NLS-1$

	private static final String FILE= "file"; //$NON-NLS-1$
	private static final String LINE= "line"; //$NON-NLS-1$
	private static final String DATE= "date"; //$NON-NLS-1$

	private static final String INDEX= "index"; //$NON-NLS-1$
	private static final String ARRAY= "array"; //$NON-NLS-1$
	private static final String ITERATOR= "iterator"; //$NON-NLS-1$
	private static final String COLLECTION= "collection"; //$NON-NLS-1$
	private static final String VECTOR= "vector"; //$NON-NLS-1$
	private static final String ENUMERATION= "enumeration"; //$NON-NLS-1$
	private static final String TYPE= "type"; //$NON-NLS-1$
	private static final String ELEMENT_TYPE= "element_type"; //$NON-NLS-1$
	private static final String ELEMENT= "element"; //$NON-NLS-1$

	private ITextViewer fViewer;
	private int fStart;
	private int fEnd;
	private ICompilationUnit fUnit;

	/**
	 * compilation unit can be null.
	 */
	public TemplateContext(ITextViewer viewer, int start, int end, ICompilationUnit unit) {
		Assert.isNotNull(viewer);
		Assert.isTrue(start <= end);
		
		fViewer= viewer;
		fStart= start;
		fEnd= end;
		fUnit= unit;
	}

	public int getStart() {
		return fStart;
	}
	
	public int getEnd() {
		return fEnd;
	}
	
	public ITextViewer getViewer() {
		return fViewer;
	}

	/*
	 * @see VariableEvaluator#reset()
	 */
	public void reset() {
	}

	/*
	 * @see VariableEvaluator#acceptText(String, int)
	 */
	public void acceptText(String variable, int offset) {
	}
		 	 	
	/*
	 * @see VariableEvaluator#evaluateVariable(String, int)
	 */
	public String evaluateVariable(String variable, int offset) {
		if (variable.equals(FILE)) {
			if (fUnit == null)
				return null;
			else
				return fUnit.getElementName();

		// line number			
		} else if (variable.equals(LINE)) {
			try {
				int line= fViewer.getDocument().getLineOfOffset(offset) + 1;
				return Integer.toString(line);
			} catch (BadLocationException e) {} // ignore
			
			return null;
			
		} else if (variable.equals(DATE)) {
			return DateFormat.getDateInstance().format(new Date());
		
		} else {
			return null;
		}
	}

}

