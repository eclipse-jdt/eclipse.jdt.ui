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
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TemplateContext implements VariableEvaluator {
	
	public static final String JAVA= "java"; //$NON-NLS-1$
	public static final String JAVADOC= "javadoc"; //$NON-NLS-1$
	
	private static final char HTML_TAG_BEGIN= '<';
	private static final char HTML_TAG_END= '>';
	private static final char JAVADOC_TAG_BEGIN= '@';	

	private static final String FILE= "file"; //$NON-NLS-1$
	private static final String LINE= "line"; //$NON-NLS-1$
	private static final String DATE= "date"; //$NON-NLS-1$
	private static final String TIME= "time"; //$NON-NLS-1$

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
	private String fKey;
	private String fType;
	private ICompilationUnit fUnit;

	/**
	 * compilation unit can be null.
	 */
	public TemplateContext(ITextViewer viewer, int completionPosition, ICompilationUnit unit,
	    String contextType)
	{
		Assert.isNotNull(viewer);
		Assert.isTrue(completionPosition >= 0);
		
		fViewer= viewer;
		fEnd= completionPosition;
		fUnit= unit;
		fType= contextType;
		
		String source= fViewer.getDocument().get();
		fStart= guessStart(source, fEnd, contextType);						
		fKey= source.substring(fStart, fEnd);
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
	
	public String getKey() {
		return fKey;
	}
	
	public String getType() {
		return fType;
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
			if (fUnit != null)
				return fUnit.getElementName();

		} else if (variable.equals(LINE)) {
			try {
				int line= fViewer.getDocument().getLineOfOffset(offset) + 1;
				return Integer.toString(line);
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			}
			
		} else if (variable.equals(DATE)) {
			return DateFormat.getDateInstance().format(new Date());

		} else if (variable.equals(TIME)) {
			return DateFormat.getTimeInstance().format(new Date());
		
		}
		
		return null;
	}

	private static int guessStart(String source, int end, String contextType) {
		int start= end;

		if (contextType.equals(JAVA)) {				
			while ((start != 0) && Character.isUnicodeIdentifierPart(source.charAt(start - 1)))
				start--;
			
			if ((start != 0) && Character.isUnicodeIdentifierStart(source.charAt(start - 1)))
				start--;
				
		} else if (contextType.equals(JAVADOC)) {
			if ((start != 0) && (source.charAt(start - 1) == HTML_TAG_END))
				start--;

			while ((start != 0) && Character.isUnicodeIdentifierPart(source.charAt(start - 1)))
				start--;
			
			if ((start != 0) && Character.isUnicodeIdentifierStart(source.charAt(start - 1)))
				start--;

			// include html and javadoc tags
			if ((start != 0) && (
				(source.charAt(start - 1) == HTML_TAG_BEGIN) ||
				(source.charAt(start - 1) == JAVADOC_TAG_BEGIN)))
			{
				start--;
			}	
		}

		return start;		
	}	

}

