package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


public class ProposalInfo {
	
	private static final int NUMBER_OF_JAVADOC_LINES= 12;
	
	private IJavaProject fJavaProject;
	private char[] fPackageName;
	private char[] fTypeName;
	private char[] fMemberName;
	private char[][] fParameterPackages;
	private char[][] fParameterTypes;

	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName, char[] methodName, char[][] paramPackages, char[][] paramTypes) {
		fJavaProject= jproject;
		fPackageName= packName;
		fTypeName= typeQualifiedName;
		fMemberName= methodName;
		fParameterPackages= paramPackages;
		fParameterTypes= paramTypes;
	}
	
	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName) {
		this(jproject, packName, typeQualifiedName, null, null, null);
	}

	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName, char[] fieldName) {
		this(jproject, packName, typeQualifiedName, fieldName, null, null);
	}	
	
	private String getParameterSignature(int index) {
		StringBuffer buf= new StringBuffer();
		char[] pack= fParameterPackages[index];
		if (pack != null && pack.length > 0) {
			buf.append(pack);
			buf.append('.');
		}
		buf.append(fParameterTypes[index]);
		return Signature.createTypeSignature(buf.toString(), true);
	}
	
	/**
	 * Gets the text for this proposal info
	 */	
	public String getInfo() {
		try {
			IType type= JavaModelUtil.findType(fJavaProject, new String(fPackageName), new String(fTypeName));
			if (type != null) {
				IMember member= null;
				if (fMemberName != null) {
					String name= new String(fMemberName);
					if (fParameterTypes != null) {
						String[] paramTypes= new String[fParameterTypes.length];
						for (int i= 0; i < fParameterTypes.length; i++) {
							paramTypes[i]= getParameterSignature(i);
						}
						member= JavaModelUtil.findMethod(name, paramTypes, false, type);
					} else {
						IField field= type.getField(name);
						if (field.exists()) {
							member= field;
						}
					}
				} else {
					member= type;
				}
				
				if (member != null)
					return  JavaDocAccess.getJavaDocText(member);
					
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}
		return null;
	}
}