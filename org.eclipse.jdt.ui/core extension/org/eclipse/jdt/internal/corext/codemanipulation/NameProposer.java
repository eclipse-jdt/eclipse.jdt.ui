/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

/**
 * @deprecated
 */
public class NameProposer {
	
	private String[] fNamePrefixes;
	private String[] fNameSuffixes;
	
	public static final String GETTER_NAME= CodeGenerationMessages.getString("NameProposer.getterPrefix"); //$NON-NLS-1$
	public static final String GETTER_BOOL_NAME= CodeGenerationMessages.getString("NameProposer.queryPrefix"); //$NON-NLS-1$
	public static final String SETTER_NAME= CodeGenerationMessages.getString("NameProposer.setterPrefix"); //$NON-NLS-1$

	public NameProposer(String[] prefixes, String[] suffixes) {
		fNamePrefixes= prefixes;
		fNameSuffixes= suffixes;
	}
	
	public NameProposer(CodeGenerationSettings settings) {
		this(settings.fieldPrefixes, settings.fieldSuffixes);
	}	
	
	public NameProposer() {
		this(new String[0], new String[0]);
	}
	
	public String proposeGetterName(String fieldName, boolean isBoolean) {
		if (isBoolean) {
			String name= removePrefixAndSuffix(fieldName);
			int prefixLen=  GETTER_BOOL_NAME.length();
			if (name.startsWith(GETTER_BOOL_NAME) 
				&& name.length() > prefixLen && Character.isUpperCase(name.charAt(prefixLen))) {
				return name;
			} else {
				return GETTER_BOOL_NAME + proposeAccessorName(fieldName);
			}
		} else {
			return GETTER_NAME + proposeAccessorName(fieldName);
		}
	}
		
	public String proposeGetterName(IField field) throws JavaModelException {
		return proposeGetterName(field.getElementName(), JavaModelUtil.isBoolean(field));
	}	
	
	public String proposeSetterName(IField field) throws JavaModelException {
		return proposeSetterName(field.getElementName(), JavaModelUtil.isBoolean(field));
	}
	
	/**
	 * @seprecated use proposeSetterName(IField)
	 */
	public String proposeSetterName(String fieldName, boolean isBoolean) {
		if (isBoolean) {
			String name= removePrefixAndSuffix(fieldName);
			int prefixLen=  GETTER_BOOL_NAME.length();
			if (name.startsWith(GETTER_BOOL_NAME)
				&& name.length() > prefixLen && Character.isUpperCase(name.charAt(prefixLen))) {
				return SETTER_NAME + name.substring(prefixLen);
			}
		}
		return SETTER_NAME + proposeAccessorName(fieldName);
	}
	
	public String proposeAccessorName(String fieldName) {
		String name= removePrefixAndSuffix(fieldName);
		if (name.length() > 0 && Strings.isLowerCase(name.charAt(0))) {
			name= String.valueOf(Character.toUpperCase(name.charAt(0))) + name.substring(1);
		}
		return name;
	}

	public String proposeAccessorName(IField field) {
		return proposeAccessorName(field.getElementName());
	}
		
	public String proposeArgName(IField field) {
		String name= removePrefixAndSuffix(field.getElementName());
		if (name.length() > 0) {
			char firstLetter= name.charAt(0);
			if (Character.isUpperCase(firstLetter)) {
				name= String.valueOf(Character.toLowerCase(firstLetter)) + name.substring(1);
			}
			if (!JavaConventions.validateFieldName(name).isOK()) {
				name= String.valueOf(name.charAt(0));
			}
		}
		return name;
	}
	
	public String[] proposeLocalVariableName(String variableType) {
		String name= Signature.getSimpleName(variableType);
		int arrIndex= name.indexOf('[');
		if (arrIndex != -1)
			name= name.substring(0, arrIndex);

		List names= new ArrayList();
		for (int i= name.length() - 1; i >= 0; i--) {
			if (Character.isUpperCase(name.charAt(i))) {
				String variableName= getVariableName(name.substring(i), arrIndex != -1);
				if (variableName != null) {
					names.add(variableName);
				}
			}
		}
		
		if (names.isEmpty())
			names.add(String.valueOf(Character.toLowerCase(name.charAt(0))));
		
		return (String[]) names.toArray(new String[names.size()]);
	}

	public String proposeParameterName(String paramType) {
		String name= Signature.getSimpleName(paramType);
		int arrIndex= name.indexOf('[');
		if (arrIndex != -1) {
			name= name.substring(0, arrIndex);
		}

		char firstLetter= name.charAt(0);
		if (Character.isUpperCase(firstLetter)) {
			String variableName= getVariableName(name, arrIndex != -1);
			if (variableName != null) {
				return variableName;
			}
		}
		return String.valueOf(Character.toLowerCase(firstLetter));
	}
	
	public String proposeFieldName(String fieldType) {
		String name= Signature.getSimpleName(fieldType);
		String arraySuffix= ""; //$NON-NLS-1$
		int arrIndex= name.indexOf('[');
		if (arrIndex != -1) {
			name= name.substring(0, arrIndex);
			arraySuffix= "s"; //$NON-NLS-1$
		}
		
		for (int i= 0; i < fNamePrefixes.length; i++) {
			String fieldName= fNamePrefixes[i] + name + arraySuffix;
			if (JavaConventions.validateFieldName(fieldName).isOK()) {
				return fieldName;
			}
		}
		for (int i= 0; i < fNameSuffixes.length; i++) {
			String fieldName= Character.toLowerCase(name.charAt(0)) + name.substring(1) 
										+ arraySuffix + fNameSuffixes[i];
			if (JavaConventions.validateFieldName(fieldName).isOK()) {
				return fieldName;
			}
		}
		String variableName= getVariableName(name, arraySuffix.length() > 0);
		if (variableName != null) {
			return variableName;
		}		
		return String.valueOf(Character.toLowerCase(name.charAt(0)));
	}	
	
	
	private String getVariableName(String name, boolean isArray) {
		StringBuffer nameBuffer= new StringBuffer();
		nameBuffer.append(Character.toLowerCase(name.charAt(0)));
		nameBuffer.append(name.substring(1));
		if (isArray) {
			nameBuffer.append('s');
		}
		if (JavaConventions.validateFieldName(nameBuffer.toString()).isOK()) {
			return nameBuffer.toString();
		}
		return null;
	}
	
	public String[] proposeParameterNames(String[] paramTypes) {
		String[] res= new String[paramTypes.length];
		for (int i= 0; i < paramTypes.length; i++) {
			String name= proposeParameterName(paramTypes[i]);
			for (int k= 0; k < i; k++) {
				if (res[k].equals(name)) {
					res[k]= res[k] + k;
					name= name + i;
				}
			}
			res[i]= name;
		}
		return res;
	}
	
	
	private String removePrefixAndSuffix(String fieldname) {
		String name= fieldname;
		int bestLength= 0;
		if (fNamePrefixes != null) {
			for (int i= 0; i < fNamePrefixes.length; i++) {
				String curr= fNamePrefixes[i];
				if (fieldname.startsWith(curr)) {
					int currLen= curr.length();
					boolean capitalNotForced= !Character.isLetter(curr.charAt(currLen - 1));					
					if (bestLength < currLen && fieldname.length() != currLen && (capitalNotForced || Character.isUpperCase(fieldname.charAt(currLen)))) {
						name= fieldname.substring(currLen);
						bestLength= currLen;
					}
				}
			}
		}
		if (fNameSuffixes != null) {
			for (int i= 0; i < fNameSuffixes.length; i++) {
				String curr= fNameSuffixes[i];
				if (fieldname.endsWith(curr)) {
					int currLen= curr.length();
					if (bestLength < currLen && fieldname.length() != currLen) {
						name= fieldname.substring(0, fieldname.length() - currLen);
						bestLength= currLen;
					}
				}
			}
		}
		return name;
	}
}
