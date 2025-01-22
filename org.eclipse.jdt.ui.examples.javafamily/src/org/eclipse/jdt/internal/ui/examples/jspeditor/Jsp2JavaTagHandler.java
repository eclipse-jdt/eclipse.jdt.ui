/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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


	@Override
	public boolean canHandleTag(String tag) {
		return true;
	}

	@Override
	public boolean canHandleText(String text) {
		return true;
	}

	@Override
	public void reset(String startTag)  {
		fInUseBean= "jsp:useBean".equals(startTag); //$NON-NLS-1$
		fInTagLib= "c:out".equals(startTag); //$NON-NLS-1$
		fInJavaSection= "<%".equals(startTag); //$NON-NLS-1$
		fInDeclaration= "<%!".equals(startTag); //$NON-NLS-1$
	}
	@Override
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

	@Override
	public int backTranslateOffsetInLine(String originalLine, String translatedLine, int offsetInTranslatedLine) {
		int javaPartitionStart= 0;
		if (originalLine.contains("<%")) //$NON-NLS-1$
			javaPartitionStart= handleJavaSection(originalLine, offsetInTranslatedLine);
		else if (originalLine.contains("<jsp:useBean id=\""))  { //$NON-NLS-1$
			javaPartitionStart= handleUseBeanTag(originalLine, offsetInTranslatedLine);
		} else if (originalLine.contains("<c:out value=\"${"))  { //$NON-NLS-1$
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

		if ((typeLength <= relativeLineOffsetInJava)
				&& (relativeLineOffsetInJava < typeLength + variableNameLength)) {
			javaPartitionStart= variableNameStart;
		} else {
			javaPartitionStart= typeStart;
		}

		// start relative to Jsp line start
		return javaPartitionStart - relativeLineOffsetInJava;
	}

	@Override
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
			StringBuilder out= new StringBuilder();
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
			StringBuilder out= new StringBuilder();
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
