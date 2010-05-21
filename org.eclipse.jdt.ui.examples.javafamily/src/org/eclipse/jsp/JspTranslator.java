/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		
	protected void startTag(boolean endTag, String name, int startName) {

		fCurrentTagHandler= fTagHandlerFactor.getHandler(name);
	}
	
	protected void tagAttribute(String attrName, String value, int startName, int startValue) {

		if (fCurrentTagHandler != null)
			fCurrentTagHandler.addAttribute(attrName, value, fLines);
	}
	
	protected void endTag(boolean end) {

		if (fCurrentTagHandler != null)
			try  {
				fCurrentTagHandler.processEndTag(fResultCollector, fLines);
			} catch (IOException ex)  {
				ex.printStackTrace();
			}
	}
	
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
	
	protected void text(String t, int line) {
		int i= 0;
		StringBuffer out= new StringBuffer();
		while (i < t.length()) {
			char c= t.charAt(i++);
			if (c == '\n') {
				fContent.append("    System.out.println(\"" + out.toString() + "\");  //$NON-NLS-1$\n");  //$NON-NLS-1$//$NON-NLS-2$
				fContentLines.add(new Integer(line++));
				out.setLength(0);
			} else {
				out.append(c);	
			}
		}
		if (out.length() > 0)  {
			fContent.append("    System.out.print(\"" + out.toString() + "\");  //$NON-NLS-1$\n"); //$NON-NLS-1$ //$NON-NLS-2$
			fContentLines.add(new Integer(line));
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

	public String translate(Reader reader, String name) throws IOException  {

		StringBuffer buffer= new StringBuffer();
		
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
		for (int i= 0; i < fDeclarationLines.size(); i++)  {
			fSmap[line++]= ((Integer)fDeclarationLines.get(i)).intValue();
			System.out.println("" + ((Integer)fDeclarationLines.get(i)).intValue()); //$NON-NLS-1$
		}
		fSmap[line]= fSmap[line - 1] + 1;
		line++;

		buffer.append("  public void out() {\n"); //$NON-NLS-1$
		fSmap[line]= fSmap[line - 1] + 1;
		line++;
		
		if (fLocalDeclarations.length() > 0)  {
			buffer.append(fLocalDeclarations.toString());
			System.out.println(fLocalDeclarations);
			for (int i= 0; i < fLocalDeclarationLines.size(); i++) {
				System.out.println("" + ((Integer)fLocalDeclarationLines.get(i)).intValue()); //$NON-NLS-1$
				fSmap[line++]= ((Integer)fLocalDeclarationLines.get(i)).intValue();
			}
		}
		
		buffer.append(fContent.toString());
		System.out.println(fContent);
		for (int i= 0; i < fContentLines.size(); i++)  {
			fSmap[line++]= ((Integer)fContentLines.get(i)).intValue();
			System.out.println("" + ((Integer)fContentLines.get(i)).intValue()); //$NON-NLS-1$
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
	
	public int[] getLineMapping()  {
		return fSmap;
	}

	/*
	 * @see org.eclipse.jface.text.source.ITranslator#setTagHandlerFactory(org.eclipse.jface.text.source.ITagHandlerFactory)
	 */
	public void setTagHandlerFactory(ITagHandlerFactory tagHandlerFactory) {
		fTagHandlerFactor= tagHandlerFactory;
	}

	/*
	 * @see ITranslator#backTranslateOffsetInLine(String, String, int)
	 */
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
