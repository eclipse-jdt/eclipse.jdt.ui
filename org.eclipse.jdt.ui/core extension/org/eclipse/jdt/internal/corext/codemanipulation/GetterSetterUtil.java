package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class GetterSetterUtil {
	
	private static final String[] empty= new String[0];
	
	//no instances
	private GetterSetterUtil(){
	}
	
	public static String getGetterName(IField field, String[] excludedNames) throws JavaModelException {
		if (excludedNames == null) {
			excludedNames= empty;
		}
		return NamingConventions.suggestGetterName(field.getJavaProject(), field.getElementName(), field.getFlags(), isBoolean(field), excludedNames);
	}

	public static String getSetterName(IField field, String[] excludedNames) throws JavaModelException {
		if (excludedNames == null) {
			excludedNames= empty;
		}		
		return NamingConventions.suggestSetterName(field.getJavaProject(), field.getElementName(), field.getFlags(), isBoolean(field), excludedNames);
	}	

	private static boolean isBoolean(IField field) throws JavaModelException {
		return field.getTypeSignature().equals(Signature.SIG_BOOLEAN);
	}
		
	public static IMethod getGetter(IField field) throws JavaModelException{
		return JavaModelUtil.findMethod(getGetterName(field, empty), new String[0], false, field.getDeclaringType());
	}
	
	public static IMethod getSetter(IField field) throws JavaModelException{
		String[] args= new String[] { field.getTypeSignature() };	
		return JavaModelUtil.findMethod(getSetterName(field, empty), args, false, field.getDeclaringType());
	}	
}
