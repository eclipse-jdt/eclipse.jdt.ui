/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * @author weinand
 */
public class JspTranslator extends AbstractJspParser {
	
	boolean DEBUG= false;
	boolean fIgnoreHTML= true;

	boolean fInUseBean;
	
	StringBuffer fDeclarations= new StringBuffer();
	StringBuffer fContent= new StringBuffer();
	StringBuffer fLocalDeclarations= new StringBuffer();
	String fId;
	String fClass;
	
	private ArrayList fContentLines;
	private ArrayList fDeclarationLines;
	private ArrayList fLocalDeclarationLines;
	private int[] fSmap;
	
	
	public JspTranslator() {
	}
		
	protected void startTag(boolean endTag, String name, int startName) {
		
		fInUseBean= "jsp:useBean".equals(name);
		
		if (DEBUG) {
			if (endTag)
				System.out.println("   </" + name + ">");
			else
				System.out.println("   <" + name);
		}
	}
	
	protected void tagAttribute(String attrName, String value, int startName, int startValue) {
		if (fInUseBean) {
			if ("id".equals(attrName))
				fId= value;
			else if ("class".equals(attrName))
				fClass= value;
		}
		if (DEBUG)
			System.out.println("     " + attrName + "=\"" + value + "\"");
	}
	
	protected void endTag(boolean end) {
		if (fInUseBean) {
			if (fId != null && fClass != null) {
				fLocalDeclarations.append(fClass + " " + fId + "= new " + fClass + "();\n");
				fLocalDeclarationLines.add(new Integer(fLines));

				System.out.println("  jsp_typeRef/" + fClass);

				fId= fClass= null;
			}
			fInUseBean= false;
		}
		if (DEBUG) {
			if (end)
				System.out.println("   />");
			else
				System.out.println("   >");
		}
	}
	
	protected void java(char ch, String java, int line) {
		int i= 0;
		StringBuffer out= new StringBuffer();
		while (i < java.length()) {
			char c= java.charAt(i++);
			if (c == '\n') {
				if (ch == '!')  {
					fDeclarations.append(out.toString() + "\n");
					fDeclarationLines.add(new Integer(line++));
				} else  {
					fContent.append(out.toString() + "\n");
					fContentLines.add(new Integer(line++));
				}
				out.setLength(0);
			} else {
				out.append(c);	
			}
		}
		if (out.length() > 0)  {
			if (ch == '!')  {
				fDeclarations.append(out.toString() + "\n");
				fDeclarationLines.add(new Integer(line));
			} else  {
				fContent.append(out.toString() + "\n");
				fContentLines.add(new Integer(line));
			}
		}
	}
	
	protected void text(String t, int line) {
		int i= 0;
		StringBuffer out= new StringBuffer();
		while (i < t.length()) {
			char c= t.charAt(i++);
			if (c == '\n') {
				fContent.append("    System.out.println(\"" + out.toString() + "\");  //$NON-NLS-1$\n");
				fContentLines.add(new Integer(line++));
				out.setLength(0);
			} else {
				out.append(c);	
			}
		}
		if (out.length() > 0)  {
			fContent.append("    System.out.print(\"" + out.toString() + "\");  //$NON-NLS-1$\n");
			fContentLines.add(new Integer(line));
		}
	}
	
	private void resetTranslator() {
		fDeclarations.setLength(0);
		fContent.setLength(0);
		fLocalDeclarations.setLength(0);
		
		fLocalDeclarationLines= new ArrayList();
		fContentLines= new ArrayList();
		fDeclarationLines= new ArrayList();
	}

	public String createJava(Reader reader, String name) throws IOException {

		StringBuffer buffer= new StringBuffer();
		
		resetTranslator();
		parse(reader);

		int lineCount= 2 + fDeclarationLines.size() + 1 + 1 + fLocalDeclarationLines.size() + fContentLines.size() + 3;
		fSmap= new int[lineCount];
		int line= 0;
		fSmap[line++]= 1;

		buffer.append("public class " + name + " {\n\n");
		fSmap[line++]= 1;
		fSmap[line++]= 1;

		buffer.append(fDeclarations.toString() + "\n");
		System.out.println(fDeclarations.toString());
		for (int i= 0; i < fDeclarationLines.size(); i++)  {
			fSmap[line++]= ((Integer)fDeclarationLines.get(i)).intValue();
			System.out.println("" + ((Integer)fDeclarationLines.get(i)).intValue());
		}
		fSmap[line]= fSmap[line - 1] + 1;
		line++;

		buffer.append("  public void out() {\n");
		fSmap[line]= fSmap[line - 1] + 1;
		line++;
		
		if (fLocalDeclarations.length() > 0)  {
			buffer.append(fLocalDeclarations.toString());
			System.out.println(fLocalDeclarations);
			for (int i= 0; i < fLocalDeclarationLines.size(); i++) {
				System.out.println("" + ((Integer)fLocalDeclarationLines.get(i)).intValue());
				fSmap[line++]= ((Integer)fLocalDeclarationLines.get(i)).intValue();
			}
		}
		
		buffer.append(fContent.toString());
		System.out.println(fContent);
		for (int i= 0; i < fContentLines.size(); i++)  {
			fSmap[line++]= ((Integer)fContentLines.get(i)).intValue();
			System.out.println("" + ((Integer)fContentLines.get(i)).intValue());
		}

		buffer.append("  }\n");
		fSmap[line]= fSmap[line - 1];

		line++;
		
		buffer.append("}\n");
		fSmap[line]= fSmap[line - 2];
		
		for (int i= 0; i < fSmap.length; i++)
			System.out.println("" + i + " -> " + fSmap[i]);
		
		System.out.println(buffer.toString());
		
		return buffer.toString();
	}
	
	public int[] getSmap()  {
		return fSmap;
	}
}
