/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.core.Assert;

public class TemplateContext {
	
	public static final String JAVA= "java"; //$NON-NLS-1$
	public static final String JAVADOC= "javadoc"; //$NON-NLS-1$
	
	private static final char HTML_TAG_BEGIN= '<';
	private static final char HTML_TAG_END= '>';
	private static final char JAVADOC_TAG_BEGIN= '@';	

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
	
	public ICompilationUnit getUnit() {
		return fUnit;
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

