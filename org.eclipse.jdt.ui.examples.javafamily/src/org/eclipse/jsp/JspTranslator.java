/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
package org.eclipse.jsp;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.eclipse.jface.text.source.translation.ITagHandler;
import org.eclipse.jface.text.source.translation.ITagHandlerFactory;
import org.eclipse.jface.text.source.translation.ITranslator;

import org.eclipse.jdt.internal.ui.examples.jspeditor.JspTranslatorResultCollector;


public class JspTranslator extends AbstractJspParser implements ITranslator {

	private StringBuffer fDeclarations= new StringBuffer();
	private StringBuffer fContent= new StringBuffer();
	private StringBuffer fLocalDeclarations= new StringBuffer();

	private ArrayList fContentLines= new ArrayList();
	private ArrayList fDeclarationLines= new ArrayList();
	private ArrayList fLocalDeclarationLines= new ArrayList();
	private int[] fSmap;

	private ITagHandlerFactory fTagHandlerFactor;
	private ITagHandler fCurrentTagHandler;

	private JspTranslatorResultCollector fResultCollector;


	public JspTranslator() {

		// Links for passing parameters to the tag handlers
		fResultCollector= new JspTranslatorResultCollector(fDeclarations, fLocalDeclarations, fContent, fDeclarationLines, fLocalDeclarationLines, fContentLines);
	}

	@Override
	protected void startTag(boolean endTag, String name, int startName) {

		fCurrentTagHandler= fTagHandlerFactor.getHandler(name);
	}

	@Override
	protected void tagAttribute(String attrName, String value, int startName, int startValue) {

		if (fCurrentTagHandler != null)
			fCurrentTagHandler.addAttribute(attrName, value, fLines);
	}

	@Override
	protected void endTag(boolean end) {

		if (fCurrentTagHandler != null)
			try  {
				fCurrentTagHandler.processEndTag(fResultCollector, fLines);
			} catch (IOException ex)  {
				ex.printStackTrace();
			}
	}

	@Override
	protected void java(char ch, String java, int line) {

		if (ch == '!')
			fCurrentTagHandler= fTagHandlerFactor.getHandler("<%!"); //$NON-NLS-1$
		else
			fCurrentTagHandler= fTagHandlerFactor.getHandler("<%"); //$NON-NLS-1$

		/*
		 * XXX: This is needed because the used parser does not treat
		 *      "<%" like every other tag.
		 */
		fCurrentTagHandler.addAttribute("source", java, line); //$NON-NLS-1$

		try {
			fCurrentTagHandler.processEndTag(fResultCollector, line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void text(String t, int line) {
		int i= 0;
		StringBuilder out= new StringBuilder();
		while (i < t.length()) {
			char c= t.charAt(i++);
			if (c == '\n') {
				fContent.append("    System.out.println(\"" + out.toString() + "\");  //$NON-NLS-1$\n");  //$NON-NLS-1$//$NON-NLS-2$
				fContentLines.add(Integer.valueOf(line++));
				out.setLength(0);
			} else {
				out.append(c);
			}
		}
		if (out.length() > 0)  {
			fContent.append("    System.out.print(\"" + out.toString() + "\");  //$NON-NLS-1$\n"); //$NON-NLS-1$ //$NON-NLS-2$
			fContentLines.add(Integer.valueOf(line));
		}
	}

	private void resetTranslator() {
		fDeclarations.setLength(0);
		fContent.setLength(0);
		fLocalDeclarations.setLength(0);

		fLocalDeclarationLines.clear();
		fContentLines.clear();
		fDeclarationLines.clear();

	}

	@Override
	public String translate(Reader reader, String name) throws IOException  {

		StringBuilder buffer= new StringBuilder();

		resetTranslator();
		parse(reader);

		int lineCount= 2 + fDeclarationLines.size() + 1 + 1 + fLocalDeclarationLines.size() + fContentLines.size() + 3;
		fSmap= new int[lineCount];
		int line= 0;
		fSmap[line++]= 1;

		buffer.append("public class " + name + " {\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
		fSmap[line++]= 1;
		fSmap[line++]= 1;

		buffer.append(fDeclarations.toString() + "\n"); //$NON-NLS-1$
		System.out.println(fDeclarations.toString());
		for (Object fDeclarationLine : fDeclarationLines) {
			fSmap[line++]= ((Integer)fDeclarationLine).intValue();
			System.out.println("" + ((Integer)fDeclarationLine).intValue()); //$NON-NLS-1$
		}
		fSmap[line]= fSmap[line - 1] + 1;
		line++;

		buffer.append("  public void out() {\n"); //$NON-NLS-1$
		fSmap[line]= fSmap[line - 1] + 1;
		line++;

		if (fLocalDeclarations.length() > 0)  {
			buffer.append(fLocalDeclarations.toString());
			System.out.println(fLocalDeclarations);
			for (Object fLocalDeclarationLine : fLocalDeclarationLines) {
				System.out.println("" + ((Integer)fLocalDeclarationLine).intValue()); //$NON-NLS-1$
				fSmap[line++]= ((Integer)fLocalDeclarationLine).intValue();
			}
		}

		buffer.append(fContent.toString());
		System.out.println(fContent);
		for (Object fContentLine : fContentLines) {
			fSmap[line++]= ((Integer)fContentLine).intValue();
			System.out.println("" + ((Integer)fContentLine).intValue()); //$NON-NLS-1$
		}

		buffer.append("  }\n"); //$NON-NLS-1$
		fSmap[line]= fSmap[line - 1];

		line++;

		buffer.append("}\n"); //$NON-NLS-1$
		fSmap[line]= fSmap[line - 2];

		for (int i= 0; i < fSmap.length; i++)
			System.out.println("" + i + " -> " + fSmap[i]); //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(buffer.toString());

		return buffer.toString();
	}

	@Override
	public int[] getLineMapping()  {
		return fSmap;
	}

	/*
	 * @see org.eclipse.jface.text.source.ITranslator#setTagHandlerFactory(org.eclipse.jface.text.source.ITagHandlerFactory)
	 */
	@Override
	public void setTagHandlerFactory(ITagHandlerFactory tagHandlerFactory) {
		fTagHandlerFactor= tagHandlerFactory;
	}

	/*
	 * @see ITranslator#backTranslateOffsetInLine(String, String, int)
	 */
	@Override
	public int backTranslateOffsetInLine(String originalLine, String translatedLine, int offsetInTranslatedLine, String tag)  {

		ITagHandler handler;
		if (tag != null)
			handler= fTagHandlerFactor.getHandler(tag);
		else
			handler= fTagHandlerFactor.findHandler(originalLine);

		if (handler != null)
			return handler.backTranslateOffsetInLine(originalLine, translatedLine, offsetInTranslatedLine);

		return -1;
	}
}
