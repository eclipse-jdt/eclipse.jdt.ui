/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.text.correction.SimilarElementsRequestor;

public class TypeInfoUtil {

	public static TypeInfo searchTypeInfo(IJavaProject javaProject, SimpleName accessNode, String qualifiedTypeName) throws JavaModelException {
		boolean is50OrHigher= JavaModelUtil.is50OrHigher(javaProject);
		
		int typeKinds= SimilarElementsRequestor.ALL_TYPES;
		if (accessNode != null) {
			typeKinds= ASTResolving.getPossibleTypeKinds(accessNode, is50OrHigher);
		}
		
		IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IJavaElement[] {javaProject});
		
		String typeName= Signature.getSimpleName(qualifiedTypeName);
	
		ArrayList typeInfos= new ArrayList();
		TypeInfoRequestor requestor= new TypeInfoRequestor(typeInfos);
		new SearchEngine().searchAllTypeNames(null, typeName.toCharArray(), SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, getSearchForConstant(typeKinds), searchScope, requestor, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, new NullProgressMonitor());
		
		for (Iterator iter= typeInfos.iterator(); iter.hasNext();) {
			TypeInfo info= (TypeInfo)iter.next();
			
			if (info.getFullyQualifiedName().equals(qualifiedTypeName)) {
				return info;
			}
		}
		return null;
	}
	
	private static int getSearchForConstant(int typeKinds) {
		final int CLASSES= SimilarElementsRequestor.CLASSES;
		final int INTERFACES= SimilarElementsRequestor.INTERFACES;
		final int ENUMS= SimilarElementsRequestor.ENUMS;
		final int ANNOTATIONS= SimilarElementsRequestor.ANNOTATIONS;
	
		switch (typeKinds & (CLASSES | INTERFACES | ENUMS | ANNOTATIONS)) {
			case CLASSES: return IJavaSearchConstants.CLASS;
			case INTERFACES: return IJavaSearchConstants.INTERFACE;
			case ENUMS: return IJavaSearchConstants.ENUM;
			case ANNOTATIONS: return IJavaSearchConstants.ANNOTATION_TYPE;
			case CLASSES | INTERFACES: return IJavaSearchConstants.CLASS_AND_INTERFACE;
			case CLASSES | ENUMS: return IJavaSearchConstants.CLASS_AND_ENUM;
			default: return IJavaSearchConstants.TYPE;
		}
	}
	

}
