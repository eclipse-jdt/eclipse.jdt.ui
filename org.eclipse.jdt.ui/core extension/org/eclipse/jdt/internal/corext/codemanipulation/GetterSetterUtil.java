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
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

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
		String getterName= getGetterName(field, EMPTY, true);
		IMethod primaryCandidate= JavaModelUtil.findMethod(getterName, new String[0], false, field.getDeclaringType());
		if (! JavaModelUtil.isBoolean(field) || (primaryCandidate != null && primaryCandidate.exists()))
			return primaryCandidate;
		//bug 30906 describes why we need to look for other alternatives here (try with get... for booleans)
		String secondCandidateName= getGetterName(field, EMPTY, false);
		return JavaModelUtil.findMethod(secondCandidateName, new String[0], false, field.getDeclaringType());
	}
	
	public static IMethod getSetter(IField field) throws JavaModelException{
		String[] args= new String[] { field.getTypeSignature() };	
		return JavaModelUtil.findMethod(getSetterName(field, EMPTY), args, false, field.getDeclaringType());
	}
	
	/**
	 * Create a stub for a getter of the given field using getter/setter templates. The resulting code
	 * has to be formatted and indented.
	 * @param field The field to create a getter for
	 * @param setterName The chosen name for the setter
	 * @param addComments If <code>true</code>, comments will be added.
	 * @param flags The flags signaling visibility, if static, synchronized or final
	 * @return Returns the generated stub.
	 * @throws CoreException
	 */
	public static String getSetterStub(IField field, String setterName, boolean addComments, int flags) throws CoreException {
		
		String fieldName= field.getElementName();
		IType parentType= field.getDeclaringType();
		
		String returnSig= field.getTypeSignature();
		String typeName= Signature.toString(returnSig);
		
		IJavaProject project= field.getJavaProject();

		String accessorName = NamingConventions.removePrefixAndSuffixForFieldName(project, fieldName, field.getFlags());
		String argname= StubUtility.suggestArgumentName(project, accessorName, EMPTY);

		boolean isStatic= Flags.isStatic(flags);
		boolean isSync= Flags.isSynchronized(flags);
		boolean isFinal= Flags.isFinal(flags);
		
		// create the setter stub
		StringBuffer buf= new StringBuffer();
		if (addComments) {
			String comment= CodeGeneration.getSetterComment(field.getCompilationUnit(), parentType.getTypeQualifiedName('.'), setterName, field.getElementName(), typeName, argname, accessorName, String.valueOf('\n'));
			if (comment != null) {
				buf.append(comment);
				buf.append('\n');
			}
		}
		buf.append(JdtFlags.getVisibilityString(flags));
		buf.append(' ');	
		if (isStatic)
			buf.append("static "); //$NON-NLS-1$
		if (isSync)
			buf.append("synchronized "); //$NON-NLS-1$
		if (isFinal)
			buf.append("final "); //$NON-NLS-1$				
			
		buf.append("void "); //$NON-NLS-1$
		buf.append(setterName);
		buf.append('('); 
		buf.append(typeName); 
		buf.append(' '); 
		buf.append(argname); 
		buf.append(") {\n"); //$NON-NLS-1$
		
		boolean useThis= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEGEN_KEYWORD_THIS);
		if (argname.equals(fieldName) || (useThis && !isStatic)) {
			if (isStatic)
				fieldName= parentType.getElementName() + '.' + fieldName;
			else
				fieldName= "this." + fieldName; //$NON-NLS-1$
		}
		String body= CodeGeneration.getSetterMethodBodyContent(field.getCompilationUnit(), parentType.getTypeQualifiedName('.'), setterName, fieldName, argname, String.valueOf('\n'));
		if (body != null) {
			buf.append(body);
		}
		buf.append("}\n"); //$NON-NLS-1$		
		return buf.toString();
	}
	
	/**
	 * Create a stub for a getter of the given field using getter/setter templates. The resulting code
	 * has to be formatted and indented.
	 * @param field The field to create a getter for
	 * @param getterName The chosen name for the getter
	 * @param addComments If <code>true</code>, comments will be added.
	 * @param flags The flags signaling visibility, if static, synchronized or final
	 * @return Returns the generated stub.
	 * @throws CoreException
	 */
	public static String getGetterStub(IField field, String getterName, boolean addComments, int flags) throws CoreException {
		String fieldName= field.getElementName();
		IType parentType= field.getDeclaringType();
		
		boolean isStatic= Flags.isStatic(flags);
		boolean isSync= Flags.isSynchronized(flags);
		boolean isFinal= Flags.isFinal(flags);
		
		String typeName= Signature.toString(field.getTypeSignature());
		String accessorName = NamingConventions.removePrefixAndSuffixForFieldName(field.getJavaProject(), fieldName, field.getFlags());

		// create the getter stub
		StringBuffer buf= new StringBuffer();
		if (addComments) {
			String comment= CodeGeneration.getGetterComment(field.getCompilationUnit(), parentType.getTypeQualifiedName('.'), getterName, field.getElementName(), typeName, accessorName, String.valueOf('\n'));
			if (comment != null) {
				buf.append(comment);
				buf.append('\n');
			}					
		}
		
		buf.append(JdtFlags.getVisibilityString(flags));
		buf.append(' ');			
		if (isStatic)
			buf.append("static "); //$NON-NLS-1$
		if (isSync)
			buf.append("synchronized "); //$NON-NLS-1$
		if (isFinal)
			buf.append("final "); //$NON-NLS-1$
			
		buf.append(typeName);
		buf.append(' ');
		buf.append(getterName);
		buf.append("() {\n"); //$NON-NLS-1$
		
		boolean useThis= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEGEN_KEYWORD_THIS);
		if (useThis && !isStatic) {
			fieldName= "this." + fieldName; //$NON-NLS-1$
		}
		
		String body= CodeGeneration.getGetterMethodBodyContent(field.getCompilationUnit(), parentType.getTypeQualifiedName('.'), getterName, fieldName, String.valueOf('\n'));
		if (body != null) {
			buf.append(body);
		}
		buf.append("}\n"); //$NON-NLS-1$
		return buf.toString(); 
	}

}
