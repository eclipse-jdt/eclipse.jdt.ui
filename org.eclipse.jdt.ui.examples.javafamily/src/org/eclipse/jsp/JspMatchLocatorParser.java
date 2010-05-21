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
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.indexsearch.*;


public class JspMatchLocatorParser extends AbstractJspParser {

	IFile fResource;
	String fMatchString;
	ISearchResultCollector fCollector;

	boolean fInUseBean;
	String fId;
	String fClass;

	public JspMatchLocatorParser() {
		super();
	}

	protected void startTag(boolean endTag, String name, int startName) {
		fInUseBean= "jsp:useBean".equals(name); //$NON-NLS-1$
	}

	protected void tagAttribute(String attrName, String value, int startName, int startValue) {
		if (fInUseBean && "class".equals(attrName) && fMatchString.equals(value)) { //$NON-NLS-1$
			try {
				fCollector.accept(fResource, startValue, value.length());
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	public void match(IFile resource, String matchString, ISearchResultCollector collector) {
		
		fResource= resource;
		fMatchString= matchString;
		fCollector= collector;
		Reader reader= null;
		
		try {
			reader= new InputStreamReader(fResource.getContents());
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
				
		try {
			parse(reader);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
}
