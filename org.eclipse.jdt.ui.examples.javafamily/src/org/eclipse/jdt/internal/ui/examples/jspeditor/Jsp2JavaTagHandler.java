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

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import java.util.ArrayList;

import org.eclipse.jface.text.source.ITagHandler;

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
		fInUseBean= "jsp:useBean".equals(startTag);
		fInTagLib= "c:out".equals(startTag);
	}
	/*
	 * @see org.eclipse.jface.text.source.ITagHandler#addAttribute(java.lang.String, java.lang.String)
	 */
	public void addAttribute(String name, String value) {
		if (fInUseBean) {
			if ("id".equals(name))
				fId= value;
			else if ("class".equals(name))
				fClass= value;
		}
		if (fInTagLib) {
			fTagLibValue= value;
		}
	}

	/*
	 * @see org.eclipse.jface.text.source.ITagHandler#translate(java.lang.StringBuffer)
	 */
	public void translate(StringBuffer declBuffer, StringBuffer localDeclBuffer, StringBuffer contentBuffer, ArrayList dec, ArrayList localDec, ArrayList content, int line) {
		if (fInUseBean) {
			if (fId != null && fClass != null) {
				localDeclBuffer.append(fClass + " " + fId + "= new " + fClass + "();\n");
				localDec.add(new Integer(line));

				System.out.println("  jsp_typeRef/" + fClass);

				fId= fClass= null;
			}
			fInUseBean= false;
		}
		if (fInTagLib && fTagLibValue != null) {
			contentBuffer.append("System.out.println(" + fTagLibValue.substring(2, fTagLibValue.length() - 1) + ");\n");
			content.add(new Integer(line));

			fTagLibValue= null;
			fInTagLib= false;
		}
	}

	/*
	 * @see org.eclipse.jface.text.source.ITagHandler#getSmap()
	 */
	public int[] getSmap() {
		// XXX Auto-generated method stub
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.source.ITagHandler#translate(java.lang.StringBuffer[], java.util.ArrayList[], int, int)
	 */
	public void translate(StringBuffer[] targetBuffers, ArrayList[] lineMappingInfos, int startingLine, int endingLine) {
		if (fInUseBean) {
			if (fId != null && fClass != null) {
				targetBuffers[1].append(fClass + " " + fId + "= new " + fClass + "();\n");
				lineMappingInfos[1].add(new Integer(startingLine));

				System.out.println("  jsp_typeRef/" + fClass);

				fId= fClass= null;
			}
			fInUseBean= false;
		}
		if (fInTagLib && fTagLibValue != null) {
			targetBuffers[2].append("System.out.println(" + fTagLibValue.substring(2, fTagLibValue.length() - 1) + ");\n");
			lineMappingInfos[2].add(new Integer(startingLine));

			fTagLibValue= null;
			fInTagLib= false;
		}
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
		} else if (originalLine.indexOf("<c:out value=\"${") != -1)  {
			javaPartitionStart= handleTagLib(originalLine, offsetInTranslatedLine);
		}
		return javaPartitionStart;
	}

	private int handleJavaSection(String jspLineStr, int relativeLineOffsetInJava)  {
		return jspLineStr.indexOf("<%") + 3; //$NON-NLS-1$
	}

	private int handleTagLib(String jspLineStr, int relativeLineOffsetInJava)  {
		int javaFileOffset= "System.out.println(".length();
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
}