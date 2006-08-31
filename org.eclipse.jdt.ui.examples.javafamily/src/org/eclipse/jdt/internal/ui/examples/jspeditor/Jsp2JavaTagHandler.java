/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import java.io.IOException;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.source.translation.ITagHandler;
import org.eclipse.jface.text.source.translation.ITranslatorResultCollector;

/**
 * 
 * @since 3.0
 */
public class Jsp2JavaTagHandler implements ITagHandler {

	private boolean fInUseBean;
	private boolean fInTagLib;
	private String fTagLibValue;
	private String fClass;
	private String fId;
	private String fSource;
	private boolean fInDeclaration;
	private boolean fInJavaSection;
	

	/*
	 * @see org.eclipse.jface.text.source.ITagHandler#canHandleTag(java.lang.String)
	 */
	public boolean canHandleTag(String tag) {
		return true;
	}

	/*
	 * @see org.eclipse.jface.text.source.ITagHandler#canHandleText(java.lang.String)
	 */
	public boolean canHandleText(String text) {
		return true;
	}

	public void reset(String startTag)  {
		fInUseBean= "jsp:useBean".equals(startTag); //$NON-NLS-1$
		fInTagLib= "c:out".equals(startTag); //$NON-NLS-1$
		fInJavaSection= "<%".equals(startTag); //$NON-NLS-1$
		fInDeclaration= "<%!".equals(startTag); //$NON-NLS-1$
	}
	/*
	 * @see org.eclipse.jface.text.source.ITagHandler#addAttribute(java.lang.String, java.lang.String)
	 */
	public void addAttribute(String name, String value, int sourceLineNumber) {
		if (fInUseBean) {
			if ("id".equals(name)) //$NON-NLS-1$
				fId= value;
			else if ("class".equals(name)) //$NON-NLS-1$
				fClass= value;
		}
		if (fInTagLib) {
			fTagLibValue= value;
		}
		if ("source".equals(name)) //$NON-NLS-1$
			fSource= value;
	}

	/*
	 * @see org.eclipse.jface.text.source.ITagHandler#backTranslateOffsetInLine(java.lang.String, int)
	 */
	public int backTranslateOffsetInLine(String originalLine, String translatedLine, int offsetInTranslatedLine) {
		int javaPartitionStart= 0;
		if (originalLine.indexOf("<%") != -1) //$NON-NLS-1$
			javaPartitionStart= handleJavaSection(originalLine, offsetInTranslatedLine);
		else if (originalLine.indexOf("<jsp:useBean id=\"") != -1)  { //$NON-NLS-1$
			javaPartitionStart= handleUseBeanTag(originalLine, offsetInTranslatedLine);
		} else if (originalLine.indexOf("<c:out value=\"${") != -1)  { //$NON-NLS-1$
			javaPartitionStart= handleTagLib(originalLine, offsetInTranslatedLine);
		}
		return javaPartitionStart;
	}

	private int handleJavaSection(String jspLineStr, int relativeLineOffsetInJava)  {
		return jspLineStr.indexOf("<%") + 3; //$NON-NLS-1$
	}

	private int handleTagLib(String jspLineStr, int relativeLineOffsetInJava)  {
		int javaFileOffset= "System.out.println(".length(); //$NON-NLS-1$
		return jspLineStr.indexOf("<c:out value=\"${") + 16 - javaFileOffset; //$NON-NLS-1$
	}
	
	/*
	 * This is a good example where the relative line offset in the Java
	 * document cannot be directly mapped back to Jsp document.
	 */
	private int handleUseBeanTag(String jspLineStr, int relativeLineOffsetInJava)  {

		int javaPartitionStart;

		int variableNameStart= jspLineStr.indexOf("<jsp:useBean id=\"") + 17; //$NON-NLS-1$
		int variableNameLength= Math.max(0, jspLineStr.indexOf('"', variableNameStart) - variableNameStart);

		int typeStart= jspLineStr.indexOf("class=\"") + 7; //$NON-NLS-1$
		int typeLength= Math.max(0, jspLineStr.indexOf('"', typeStart) - typeStart);
					
		if (relativeLineOffsetInJava < typeLength)  {
			javaPartitionStart= typeStart;
		} else if (relativeLineOffsetInJava < typeLength + variableNameLength)
			javaPartitionStart= variableNameStart;
		else
			javaPartitionStart= typeStart;

		// start relative to Jsp line start
		return javaPartitionStart - relativeLineOffsetInJava;
	}

	/*
	 * @see org.eclipse.jface.text.source.ITagHandler#processEndTag(ITranslatorResultCollector, int)
	 */
	public void processEndTag(ITranslatorResultCollector resultCollector, int sourceLineNumber) throws IOException {
		Assert.isTrue(resultCollector instanceof JspTranslatorResultCollector);

		JspTranslatorResultCollector jspResultCollector= (JspTranslatorResultCollector)resultCollector;
		
		if (fInUseBean) {
			if (fId != null && fClass != null) {
				jspResultCollector.appendLocalDeclaration(fClass + " " + fId + "= new " + fClass + "();\n", sourceLineNumber); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				fId= fClass= null;
			}
			fInUseBean= false;
		}
		if (fInTagLib && fTagLibValue != null) {
			jspResultCollector.appendContent("System.out.println(" + fTagLibValue.substring(2, fTagLibValue.length() - 1) + ");\n", sourceLineNumber);   //$NON-NLS-1$ //$NON-NLS-2$
			fTagLibValue= null;
			fInTagLib= false;
		}
		if (fInJavaSection)  {
			int i= 0;
			StringBuffer out= new StringBuffer();
			while (i < fSource.length()) {
				char c= fSource.charAt(i++);
				if (c == '\n') {
					jspResultCollector.appendContent(out.toString() + "\n", sourceLineNumber++); //$NON-NLS-1$
					out.setLength(0);
				} else {
					out.append(c);	
				}
			}
			if (out.length() > 0)  {
				jspResultCollector.appendContent(out.toString() + "\n", sourceLineNumber); //$NON-NLS-1$
			}
		}
		if (fInDeclaration)  {
			int i= 0;
			StringBuffer out= new StringBuffer();
			while (i < fSource.length()) {
				char c= fSource.charAt(i++);
				if (c == '\n') {
					jspResultCollector.appendDeclaration(out.toString() + "\n", sourceLineNumber++); //$NON-NLS-1$
					out.setLength(0);
				} else {
					out.append(c);	
				}
			}
			if (out.length() > 0)  {
				jspResultCollector.appendDeclaration(out.toString() + "\n", sourceLineNumber); //$NON-NLS-1$
			}
		}
	}
}
