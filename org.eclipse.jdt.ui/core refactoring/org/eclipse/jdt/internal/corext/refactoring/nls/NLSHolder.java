/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.InvalidInputException;


/**
 * This class is responsible for creating <code>NLSSubstitution</code>
 */
public class NLSHolder {

	// TODO: in substitutions
	public static NLSSubstitution[] create(ICompilationUnit cu, NLSInfo nlsInfo) {
		NLSLine[] lines= createRawLines(cu);
		List result= new ArrayList();
		Properties props= null;
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				NLSElement nlsElement= elements[j];
				if (nlsElement.hasTag()) {
					AccessorClassInfo accessorClassInfo= nlsInfo.getAccessorClassInfo(nlsElement);
					if (accessorClassInfo == null) {
						// no accessorclass => not translated				        
						result.add(new NLSSubstitution(NLSSubstitution.IGNORED, stripQuotes(nlsElement.getValue()), nlsElement));
					} else {
						String key= stripQuotes(nlsElement.getValue());
						if (props == null) {
							props= getProperties(nlsInfo.getResourceBundleFile(nlsElement));
						}
						String value= props.getProperty(key);
						result.add(new NLSSubstitution(NLSSubstitution.EXTERNALIZED, key, value, nlsElement, accessorClassInfo));
					}
				} else {
					result.add(new NLSSubstitution(NLSSubstitution.INTERNALIZED, stripQuotes(nlsElement.getValue()), nlsElement));
				}
			}
		}
		return (NLSSubstitution[]) result.toArray(new NLSSubstitution[result.size()]);
	}

	private static String stripQuotes(String str) {
		return str.substring(1, str.length() - 1);
	}

	private static Properties getProperties(IFile propertyFile) {
		Properties props= new Properties();
		try {
			if (propertyFile != null) {
				InputStream is= propertyFile.getContents();
				props.load(is);
				is.close();
			}
		} catch (Exception e) {
			// sorry no property         
		}
		return props;
	}

	private static NLSLine[] createRawLines(ICompilationUnit cu) {
		try {
			return NLSScanner.scan(cu);
		} catch (JavaModelException x) {
			return new NLSLine[0];
		} catch (InvalidInputException x) {
			return new NLSLine[0];
		}
	}
}