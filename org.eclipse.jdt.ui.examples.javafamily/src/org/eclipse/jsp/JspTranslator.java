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

/**
 * @author weinand
 */
public class JspTranslator extends AbstractJspParser {
	
	boolean DEBUG= false;
	boolean fIgnoreHTML= true;
	
	StringBuffer fDeclarations= new StringBuffer();
	StringBuffer fContent= new StringBuffer();
	StringBuffer fLocalDeclarations= new StringBuffer();
	boolean fInUseBean;
	String fId;
	String fClass;
	
	
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
	
	protected void java(char c, String java) {
		switch (c) {
		case '!':
			fDeclarations.append(java + "\n");
			break;
		default:
			fContent.append(java + "\n");
			break;
		}
	}
	
	protected void text(String t) {
		int i= 0;
		StringBuffer out= new StringBuffer();
		while (i < t.length()) {
			char c= t.charAt(i++);
			if (c == '\n') {
				fContent.append("    System.out.println(\"" + out.toString() + "\");\n");
				out.setLength(0);
			} else {
				out.append(c);	
			}
		}
		if (out.length() > 0)
			fContent.append("    System.out.print(\"" + out.toString() + "\");\n");
	}
	
	private void resetTranslator() {
		fDeclarations.setLength(0);
		fContent.setLength(0);
		fLocalDeclarations.setLength(0);
	}

	public String createJava(Reader reader, String name) throws IOException {

		StringBuffer buffer= new StringBuffer();
		
		resetTranslator();
		parse(reader);

		buffer.append("public class " + name + " {\n\n");

		buffer.append(fDeclarations.toString() + "\n");

		buffer.append("  public void out() {\n");
		
		if (fLocalDeclarations.length() > 0)
			buffer.append(fLocalDeclarations.toString());
		
		buffer.append(fContent.toString());

		buffer.append("  }\n");
		buffer.append("}\n");
		
		return buffer.toString();
	}
}
