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
package org.eclipse.jdt.internal.ui.text.java;

 
import java.io.IOException;
import java.io.Reader;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavadocContentAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;


public class ProposalInfo {
		
	private IJavaProject fJavaProject;
	private char[] fPackageName;
	private char[] fTypeName;
	private char[] fMemberName;
	private char[][] fParameterPackages;
	private char[][] fParameterTypes;
	private boolean fIsConstructor;
	
	private IMember fMember;

	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName, char[] methodName, char[][] paramPackages, char[][] paramTypes, boolean isConstructor) {
		fJavaProject= jproject;
		fPackageName= packName;
		fTypeName= typeQualifiedName;
		fMemberName= methodName;
		fParameterPackages= paramPackages;
		fParameterTypes= paramTypes;
		fIsConstructor= isConstructor;
	}
	
	public ProposalInfo(IMember member) {
		fMember= member;
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
	
	private IMember getMember() throws JavaModelException {
		if (fMember == null) {
			IType type= fJavaProject.findType(new String(fPackageName), new String(fTypeName));
			if (type != null) {
				if (fMemberName != null) {
					String name= new String(fMemberName);
					if (fParameterTypes != null) {
						String[] paramTypes= new String[fParameterTypes.length];
						for (int i= 0; i < fParameterTypes.length; i++) {
							paramTypes[i]= getParameterSignature(i);
						}
						fMember= JavaModelUtil.findMethod(name, paramTypes, fIsConstructor, type);
					} else {
						IField field= type.getField(name);
						if (field.exists()) {
							fMember= field;
						}
					}
				} else {
					fMember= type;
				}
			}
		}
		return fMember;
	}
	
	/**
	 * Gets the text for this proposal info
	 */	
	public String getInfo() {
		try {
			IMember member= getMember();
			if (member != null) {
				Reader reader= JavadocContentAccess.getContentReader(member, true);
				if (reader != null) {
					return new JavaDoc2HTMLTextReader(reader).getString();
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
