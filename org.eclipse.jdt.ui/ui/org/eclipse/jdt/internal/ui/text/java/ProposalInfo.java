/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;


import java.io.IOException;
import java.io.Reader;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavadocContentAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class ProposalInfo {

	private IJavaProject fJavaProject;
	private String fFullTypeName;
	private IMember fMember;

	public ProposalInfo(IJavaProject jproject, String fullyQualifiedTypeName) {
		fJavaProject= jproject;
		fFullTypeName= fullyQualifiedTypeName;
	}

	public ProposalInfo(IMember member) {
		fMember= member;
	}

	private IMember getMember() throws JavaModelException {
		if (fMember == null) {
			IType type= fJavaProject.findType(fFullTypeName);
			if (type != null) {
				fMember= type;
			}
		}
		return fMember;
	}

	/**
	 * Gets the text for this proposal info
	 */
	public String getInfo() {
			IMember member;
			try {
				member= getMember();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return null;
			}
			if (member != null) {
				try {
					Reader reader= JavadocContentAccess.getHTMLContentReader(member, true, true);
					if (reader != null)
						return getString(reader);
				} catch (JavaModelException e) {
					return null;
				}
			}
		return null;
	}
	
	/**
	 * Gets the reader content as a String
	 */
	private static String getString(Reader reader) {
		StringBuffer buf= new StringBuffer();
		char[] buffer= new char[1024];
		int count;
		try {
			while ((count= reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}
	
}
