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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.InvalidInputException;


/**
 * This class is responsible for creating and storing <code>NLSSubstitution</code> and 
 * <code>NLSLine</code> elements for a given <code>ICompilationUnit</code>.  
 */
public class NLSHolder {	
	private NLSSubstitution[] fSubstitutions;
	private NLSLine[] fLines;
	
	// Property Files are cached here.
	private static Map propertyMap = new HashMap();
	
	//clients create instances by using the factory method
	private NLSHolder(NLSSubstitution[] substitutions, NLSLine[] lines, ICompilationUnit cu) {
		fSubstitutions= substitutions;
		fLines= lines;
		propertyMap.clear();
	}

	public static NLSHolder create(ICompilationUnit cu){
		NLSLine[] nlsLines= createRawLines(cu);
		NLSSubstitution[] subs = createNLSSubstitutions(nlsLines, cu);
		return new NLSHolder(subs, nlsLines, cu);
	}

	public NLSSubstitution[] getSubstitutions(){
		return fSubstitutions;
	}
	
	public NLSLine[] getLines(){
		return fLines;
	}
	
	private static NLSLine[] createRawLines(ICompilationUnit cu){
		try {
			return NLSScanner.scan(cu);
		} catch (JavaModelException x) {
			return new NLSLine[0];
		} catch (InvalidInputException x) {
			return new NLSLine[0];
		}		
	}	
	
	private static NLSSubstitution[] createNLSSubstitutions(NLSLine[] lines, ICompilationUnit cu) {
	    NLSInfo nlsInfo = new NLSInfo(cu);
		List result= new ArrayList();		
		for (int i = 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for(int j = 0; j < elements.length; j++){
				NLSElement nlsElement= elements[j];
				if (nlsElement.hasTag()) {
				    AccessorClassInfo accessorClassInfo = nlsInfo.getAccessorClassInfo(nlsElement);
				    if (accessorClassInfo == null) {
				        // no accessorclass => not translated				        
				        result.add(new NLSSubstitution(
				                NLSSubstitution.IGNORED, 
				                stripQuotes(nlsElement.getValue()), 
				                nlsElement));
				    } else {				        
				        String key = stripQuotes(nlsElement.getValue());
						Properties props = (Properties) propertyMap.get(accessorClassInfo);
						if (props == null) {
						    IFile propertyFile = nlsInfo.getResourceBundleFile(nlsElement);
						    if (propertyFile != null) {
						        try {
						            props = getProperties(propertyFile);
						            propertyMap.put(accessorClassInfo, props);
						        } catch (Exception e) {
						            // do nothing
						        }
						    }
						}
						String value = "";
						if (props != null) {
						    value = props.getProperty(key);
						}
				        result.add(new NLSSubstitution(
				        		NLSSubstitution.EXTERNALIZED, 
								key, 
								value, 
								nlsElement, 
								accessorClassInfo));
				    }				    
				} else {
					result.add(new NLSSubstitution(
					        NLSSubstitution.INTERNALIZED, 
					        stripQuotes(nlsElement.getValue()), 
							nlsElement));
				}				
			}
		} 		
		return (NLSSubstitution[]) result.toArray(new NLSSubstitution[result.size()]);
	}	
    
    private static String stripQuotes(String str) {
		return str.substring(1, str.length() - 1);
	}
	
    private static Properties getProperties(IFile propertyFile) throws Exception {
        Properties props = new Properties();
        props.load(propertyFile.getContents());        
        return props;        
    }	
}
