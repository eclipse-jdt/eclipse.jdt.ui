package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class GetterSetterUtil {
	
	private static final String GETTER_NAME= "get";
	private static final String SETTER_NAME= "set";
	
	private GetterSetterUtil(){
	}
	
	public static String getGetterName(String fieldName, CodeGenerationSettings settings, String[] namePrefixes, String[] nameSuffixes){
		return GETTER_NAME + evalAccessorName(fieldName, namePrefixes, nameSuffixes);
	}
	
	public static String getSetterName(String fieldName, CodeGenerationSettings settings, String[] namePrefixes, String[] nameSuffixes){
		return SETTER_NAME + evalAccessorName(fieldName, namePrefixes, nameSuffixes);
	}

	public static IMethod getGetter(IField field, CodeGenerationSettings settings, String[] namePrefixes, String[] nameSuffixes) throws JavaModelException{
		return getAccessor(field, new String[0], namePrefixes, nameSuffixes, GETTER_NAME);
	}
	
	public static IMethod getSetter(IField field, CodeGenerationSettings settings, String[] namePrefixes, String[] nameSuffixes) throws JavaModelException{
		String[] args= new String[] { field.getTypeSignature() };		
		return getAccessor(field, args, namePrefixes, nameSuffixes, SETTER_NAME);
	}

	private static IMethod getAccessor(IField field, String[] args, String[] namePrefixes, String[] nameSuffixes, String prefix) throws JavaModelException {
		String accessorName= prefix + evalAccessorName(field.getElementName(), namePrefixes, nameSuffixes);
		return JavaModelUtil.findMethod(accessorName, args, false, field.getDeclaringType());
	}

	
	/**
	 * The policy to evaluate the base name (setBasename / getBasename)
	 */
	private static String evalAccessorName(String fieldname, String[] namePrefixes, String[] nameSuffixes) {
		String name= fieldname;
		int bestLength= 0;
		if (namePrefixes != null) {
			for (int i= 0; i < namePrefixes.length; i++) {
				String curr= namePrefixes[i];
				if (fieldname.startsWith(curr) && isBetter(fieldname.length(), curr.length(), bestLength)){
					name= fieldname.substring(curr.length());
					bestLength= curr.length();
				}
			}
		}
		if (nameSuffixes != null) {
			for (int i= 0; i < nameSuffixes.length; i++) {
				String curr= nameSuffixes[i];
				if (fieldname.endsWith(curr) && isBetter(fieldname.length(), curr.length(), bestLength)) {
					name= fieldname.substring(0, fieldname.length() - curr.length());
					bestLength= curr.length();
				}	
			}
		}
		if (name.length() > 0 && Character.isLowerCase(name.charAt(0))) {
			name= String.valueOf(Character.toUpperCase(name.charAt(0))) + name.substring(1);
		}
		return name;
	}
	
	private static boolean isBetter(int fieldnameLength, int currLength, int bestLength){
		if (bestLength >= currLength)
			return false;
		if (fieldnameLength == currLength)
			return false;
		return true;	
	}	
}
