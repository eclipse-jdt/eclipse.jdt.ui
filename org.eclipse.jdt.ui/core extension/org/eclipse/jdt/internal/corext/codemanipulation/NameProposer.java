/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

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
		boolean isBoolean=	field.getTypeSignature().equals(Signature.SIG_BOOLEAN);
		return proposeGetterName(field.getElementName(), isBoolean);
	}	
	
	public String proposeSetterName(IField field) throws JavaModelException {
		return proposeSetterName(field.getElementName());
	}
	
	/**
	 * @seprecated use proposeSetterName(IField)
	 */
	public String proposeSetterName(String fieldName) {
		return SETTER_NAME + proposeAccessorName(fieldName);
	}
	
	public String proposeAccessorName(String fieldName) {
		String name= removePrefixAndSuffix(fieldName);
		if (name.length() > 0 && Character.isLowerCase(name.charAt(0))) {
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

	public String proposeParameterName(String paramType) {
		String name= Signature.getSimpleName(paramType);
		int arrIndex= name.indexOf('[');
		if (arrIndex != -1) {
			name= name.substring(0, arrIndex);
		}

		char firstLetter= name.charAt(0);
		if (Character.isUpperCase(firstLetter)) {
			StringBuffer nameBuffer= new StringBuffer();
			nameBuffer.append(Character.toLowerCase(firstLetter));
			nameBuffer.append(name.substring(1));
			if (arrIndex != -1) {
				nameBuffer.append('s');
			}
			if (JavaConventions.validateFieldName(name).isOK()) {
				return nameBuffer.toString();
			}
		}
		return String.valueOf(Character.toLowerCase(firstLetter));
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
					if (bestLength < currLen && fieldname.length() != currLen) {
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
