package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.IOException;
import java.io.Reader;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;


public class ProposalInfo {
	
	private static final int NUMBER_OF_JAVADOC_LINES= 12;
	
	private IJavaProject fJavaProject;
	private char[] fPackageName;
	private char[] fTypeName;
	private char[] fMemberName;
	private char[][] fParameterPackages;
	private char[][] fParameterTypes;
	private boolean fIsConstructor;

	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName, char[] methodName, char[][] paramPackages, char[][] paramTypes, boolean isConstructor) {
		fJavaProject= jproject;
		fPackageName= packName;
		fTypeName= typeQualifiedName;
		fMemberName= methodName;
		fParameterPackages= paramPackages;
		fParameterTypes= paramTypes;
		fIsConstructor= isConstructor;
	}
	
	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName) {
		this(jproject, packName, typeQualifiedName, null, null, null, false);
	}

	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName, char[] fieldName) {
		this(jproject, packName, typeQualifiedName, fieldName, null, null, false);
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
						member= JavaModelUtil.findMethod(name, paramTypes, fIsConstructor, type);
					} else {
						IField field= type.getField(name);
						if (field.exists()) {
							member= field;
						}
					}
				} else {
					member= type;
				}
				
				if (member != null) {
					Reader reader= JavaDocAccess.getJavaDoc(member);
					if (reader != null) {
						return  new JavaDoc2HTMLTextReader(reader).getString();
					}
				}
					
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		} catch (IOException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
}