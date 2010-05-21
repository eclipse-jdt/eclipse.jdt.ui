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

import java.io.*;
import java.io.Reader;

import org.eclipse.core.indexsearch.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;

/**
 * @author weinand
 */
public class JspIndexParser extends AbstractJspParser implements IIndexer {
	
	public static final String JSP_TYPE_REF= "jsp_typeRef";	//$NON-NLS-1$
	
	IFile fFile;
	String fFilePath;
	boolean fInUseBean;
	String fId;
	String fClass;
	IIndex fOutput;
	

	JspIndexParser(IFile resource) {
		fFile= resource;
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
				fOutput.addRef(s, fFilePath);				

				fId= fClass= null;
			}
			fInUseBean= false;
		}
	}
	
	public void index(IIndex indexerOutput) throws IOException {
		
		String type= fFile.getFileExtension();
		if (type != null && JspUIPlugin.JSP_TYPE.equalsIgnoreCase(type)) {
			
			// Add the name of the file to the index
			String path= fFile.getFullPath().toString();
			
			String encoding= null;
			try {
				encoding= fFile.getCharset();
			} catch (CoreException e1) {
			}
			if (encoding == null)
				encoding= ResourcesPlugin.getEncoding();
			
			String s= null;
			IPath location= fFile.getLocation();
			if (location == null)
				s= ""; //$NON-NLS-1$
			else
				s= new String(Util.getFileCharContent(location.toFile(), encoding));
			
			try {
				Reader reader= new StringReader(s);
				fOutput= indexerOutput;
				fFilePath= path;			
				parse(reader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
