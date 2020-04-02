/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - moved template related code to jdt.core.manipulation - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * A context for javadoc.
 */
public class JavaDocContext extends CompilationUnitContext {

	// tags
	private static final char HTML_TAG_BEGIN= '<';
	private static final char HTML_TAG_END= '>';
	private static final char JAVADOC_TAG_BEGIN= '@';

	/**
	 * Creates a javadoc template context.
	 *
	 * @param type the context type.
	 * @param document the document.
	 * @param completionOffset the completion offset within the document.
	 * @param completionLength the completion length within the document.
	 * @param compilationUnit the compilation unit (may be <code>null</code>).
	 */
	public JavaDocContext(TemplateContextType type, IDocument document, int completionOffset, int completionLength, ICompilationUnit compilationUnit) {
		super(type, document, completionOffset, completionLength, compilationUnit);
	}

	/**
	 * Creates a javadoc template context.
	 *
	 * @param type the context type.
	 * @param document the document.
	 * @param completionPosition the position defining the completion offset and length
	 * @param compilationUnit the compilation unit (may be <code>null</code>).
	 * @since 3.2
	 */
	public JavaDocContext(TemplateContextType type, IDocument document, Position completionPosition, ICompilationUnit compilationUnit) {
		super(type, document, completionPosition, compilationUnit);
	}

	/*
	 * @see TemplateContext#canEvaluate(Template templates)
	 */
	@Override
	public boolean canEvaluate(Template template) {
		if (!template.getContextTypeId().equals(getContextType().getId()))
			return false;

		if (this.isForceEvaluation())
			return true;

		String key= getKey().toLowerCase();
		if (template.matches(key, getContextType().getId()) && key.length() != 0) {
			String templateName= template.getName().toLowerCase();
			return JavaManipulationPlugin.CODEASSIST_SUBSTRING_MATCH_ENABLED
					? templateName.contains(key)
					: templateName.startsWith(key);
		}

		return false;
	}

	/*
	 * @see DocumentTemplateContext#getStart()
	 */
	@Override
	public int getStart() {
		if (this.isManaged() && getCompletionLength() > 0)
			return super.getStart();

		try {
			IDocument document= getDocument();

			if (getCompletionLength() == 0) {
				int start= getCompletionOffset();

				if ((start != 0) && (document.getChar(start - 1) == HTML_TAG_END))
					start--;

				while ((start != 0) && Character.isUnicodeIdentifierPart(document.getChar(start - 1)))
					start--;

				if ((start != 0) && Character.isUnicodeIdentifierStart(document.getChar(start - 1)))
					start--;

				// include html and javadoc tags
				if ((start != 0) && (
					(document.getChar(start - 1) == HTML_TAG_BEGIN) ||
					(document.getChar(start - 1) == JAVADOC_TAG_BEGIN)))
				{
					start--;
				}

				return start;

			}

			int start= getCompletionOffset();
			int end= getCompletionOffset() + getCompletionLength();

			while (start != 0 && Character.isUnicodeIdentifierPart(document.getChar(start - 1)))
				start--;

			while (start != end && Character.isWhitespace(document.getChar(start)))
				start++;

			if (start == end)
				start= getCompletionOffset();

			return start;


		} catch (BadLocationException e) {
			return getCompletionOffset();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.DocumentTemplateContext#getEnd()
	 */
	@Override
	public int getEnd() {

		if (this.isManaged() || getCompletionLength() == 0)
			return super.getEnd();

		try {
			IDocument document= getDocument();

			int start= getCompletionOffset();
			int end= getCompletionOffset() + getCompletionLength();

			while (start != end && Character.isWhitespace(document.getChar(end - 1)))
				end--;

			return end;

		} catch (BadLocationException e) {
			return super.getEnd();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.DocumentTemplateContext#getKey()
	 */
	@Override
	public String getKey() {

		if (getCompletionLength() == 0)
			return super.getKey();

		try {
			IDocument document= getDocument();

			int start= getStart();
			int end= getCompletionOffset();
			return start <= end
				? document.get(start, end - start)
				: ""; //$NON-NLS-1$

		} catch (BadLocationException e) {
			return super.getKey();
		}
	}

	/*
	 * @see TemplateContext#evaluate(Template)
	 */
	@Override
	public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(template);

		getContextType().resolve(buffer, this);

		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		boolean useCodeFormatter= prefs.getBoolean(PreferenceConstants.TEMPLATES_USE_CODEFORMATTER);

		IJavaProject project= getJavaProject();
		JavaFormatter formatter= new JavaFormatter(TextUtilities.getDefaultLineDelimiter(getDocument()), getIndentation(), useCodeFormatter, project);
		formatter.format(buffer, this);

		return buffer;
	}

	/**
	 * Returns the indentation level at the position of code completion.
	 *
	 * @return the indentation level at the position of the code completion
	 */
	private int getIndentation() {
		int start= getStart();
		IDocument document= getDocument();
		try {
			IRegion region= document.getLineInformationOfOffset(start);
			String lineContent= document.get(region.getOffset(), region.getLength());
			IJavaProject project= getJavaProject();
			return Strings.computeIndentUnits(lineContent, project);
		} catch (BadLocationException e) {
			return 0;
		}
	}
}

