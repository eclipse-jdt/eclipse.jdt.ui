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
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;


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
