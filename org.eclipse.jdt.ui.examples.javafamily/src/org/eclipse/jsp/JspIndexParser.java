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

import org.eclipse.jdt.internal.core.index.IIndexerOutput;

/**
 * @author weinand
 */
public class JspIndexParser extends AbstractJspParser {
	
	public static final String JSP_TYPE_REF= "jsp_typeRef";	//$NON-NLS-1$
	
	boolean fInUseBean;
	String fId;
	String fClass;
	IIndexerOutput fOutput;
	
	
	JspIndexParser(IIndexerOutput output) {
		fOutput= output;
	}
		
	protected void startTag(boolean endTag, String name, int startName) {
		fInUseBean= "jsp:useBean".equals(name); //$NON-NLS-1$
	}
	
	protected void tagAttribute(String attrName, String value, int startName, int startValue) {
		if (fInUseBean) {
			if ("id".equals(attrName)) //$NON-NLS-1$
				fId= value;
			else if ("class".equals(attrName)) //$NON-NLS-1$
				fClass= value;
		}
	}
	
	protected void endTag(boolean end) {
		if (fInUseBean) {
			if (fId != null && fClass != null) {

				String s= JSP_TYPE_REF + "/" + fClass; //$NON-NLS-1$
				System.out.println("  " + s); //$NON-NLS-1$
				fOutput.addRef(s);				

				fId= fClass= null;
			}
			fInUseBean= false;
		}
	}
}
