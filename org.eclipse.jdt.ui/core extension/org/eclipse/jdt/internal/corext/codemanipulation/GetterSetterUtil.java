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
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class GetterSetterUtil {
	
	private static final String[] EMPTY= new String[0];
	
	//no instances
	private GetterSetterUtil(){
	}
	
	public static String getGetterName(IField field, String[] excludedNames) throws JavaModelException {
		boolean useIs= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEGEN_IS_FOR_GETTERS);
		return getGetterName(field, excludedNames, useIs);
	}
	
	private static String getGetterName(IField field, String[] excludedNames, boolean useIsForBoolGetters) throws JavaModelException {
		if (excludedNames == null) {
			excludedNames= EMPTY;
		}
		return getGetterName(field.getJavaProject(), field.getElementName(), field.getFlags(), useIsForBoolGetters && JavaModelUtil.isBoolean(field), excludedNames);
	}	
	
	public static String getGetterName(IJavaProject project, String fieldName, int flags, boolean isBoolean, String[] excludedNames){
		return NamingConventions.suggestGetterName(project, fieldName, flags, isBoolean, excludedNames);	
	}

	public static String getSetterName(IJavaProject project, String fieldName, int flags, boolean isBoolean, String[] excludedNames){
		return NamingConventions.suggestSetterName(project, fieldName, flags, isBoolean, excludedNames);	
	}

	public static String getSetterName(IField field, String[] excludedNames) throws JavaModelException {
		if (excludedNames == null) {
			excludedNames= EMPTY;
		}		
		return NamingConventions.suggestSetterName(field.getJavaProject(), field.getElementName(), field.getFlags(), JavaModelUtil.isBoolean(field), excludedNames);
	}	

	public static IMethod getGetter(IField field) throws JavaModelException{
		IMethod primaryCandidate= JavaModelUtil.findMethod(getGetterName(field, EMPTY, true), new String[0], false, field.getDeclaringType());
		if (! JavaModelUtil.isBoolean(field) || (primaryCandidate != null && primaryCandidate.exists()))
			return primaryCandidate;
		//bug 30906 describes why we need to look for other alternatives here
		String secondCandidateName= getGetterName(field, EMPTY, false);
		return JavaModelUtil.findMethod(secondCandidateName, new String[0], false, field.getDeclaringType());
	}
	
	public static IMethod getSetter(IField field) throws JavaModelException{
		String[] args= new String[] { field.getTypeSignature() };	
		return JavaModelUtil.findMethod(getSetterName(field, EMPTY), args, false, field.getDeclaringType());
	}	
}
