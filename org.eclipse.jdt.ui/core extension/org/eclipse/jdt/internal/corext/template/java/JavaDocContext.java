/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.DocumentTemplateContext;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.TemplateTranslator;

/**
 * A context for javadoc.
 */
public class JavaDocContext extends DocumentTemplateContext {

	// tags
	private static final char HTML_TAG_BEGIN= '<';
	private static final char HTML_TAG_END= '>';
	private static final char JAVADOC_TAG_BEGIN= '@';	
	
	/** The compilation unit of the javadoc, may be <code>null</code>. */
	private final ICompilationUnit fUnit;

	/**
	 * Creates a javadoc template context.
	 * 
	 * @param type   the context type.
	 * @param string the document string.
	 * @param completionPosition the completion position within the document.
	 * @param unit the compilation unit (may be <code>null</code>).
	 */
	public JavaDocContext(ContextType type, String string, int completionPosition, ICompilationUnit unit) {
		super(type, string, completionPosition);
		fUnit= unit;
	}
	
	/**
	 * Returns the compilation unit if applicable, <code>null</code> otherwise.
	 */
	public ICompilationUnit getUnit() {
		return fUnit;
	}

	/*
	 * @see DocumentTemplateContext#getStart()
	 */ 
	public int getStart() {
		String string= getString();
		int start= getCompletionPosition();

		if ((start != 0) && (string.charAt(start - 1) == HTML_TAG_END))
			start--;

		while ((start != 0) && Character.isUnicodeIdentifierPart(string.charAt(start - 1)))
			start--;
		
		if ((start != 0) && Character.isUnicodeIdentifierStart(string.charAt(start - 1)))
			start--;

		// include html and javadoc tags
		if ((start != 0) && (
			(string.charAt(start - 1) == HTML_TAG_BEGIN) ||
			(string.charAt(start - 1) == JAVADOC_TAG_BEGIN)))
		{
			start--;
		}	

		return start;
	}

	/*
	 * @see TemplateContext#canEvaluate(Template templates)
	 */
	public boolean canEvaluate(Template template) {
		return template.matches(getKey(), getContextType().getName());
	}

	/*
	 * @see TemplateContext#evaluate(Template)
	 */
	public TemplateBuffer evaluate(Template template) throws CoreException {
		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(template.getPattern());

		getContextType().edit(buffer, this);
			
		return buffer;
	}

}

