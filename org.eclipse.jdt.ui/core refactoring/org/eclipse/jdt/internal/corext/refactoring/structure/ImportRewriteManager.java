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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

class ImportRewriteManager {

	private final Map fImportRewrites; //ICompilationUnit -> ImportEdit
	private final CodeGenerationSettings fPreferenceSettings;

	public ImportRewriteManager(CodeGenerationSettings preferenceSettings) {
		Assert.isNotNull(preferenceSettings);
		fPreferenceSettings= preferenceSettings;
		fImportRewrites= new HashMap();
	}

	public boolean hasImportEditFor(ICompilationUnit cu) throws JavaModelException {
		return fImportRewrites.containsKey(cu);
	}

	public ImportRewrite getImportRewrite(ICompilationUnit cu) throws CoreException {
		if (hasImportEditFor(cu))
			return (ImportRewrite)fImportRewrites.get(cu);

		ImportRewrite edit= new ImportRewrite(cu);
		fImportRewrites.put(cu, edit);
		return edit;
	}

	public void addImportTo(String fullyQualifiedName, ICompilationUnit cu) throws CoreException {
		getImportRewrite(cu).addImport(fullyQualifiedName);
	}

	public void addImportTo(IType type, ICompilationUnit cu) throws CoreException {
		addImportTo(JavaModelUtil.getFullyQualifiedName(type), cu);
	}

	public void removeImportTo(IType type, ICompilationUnit cu) throws CoreException {
		removeImportTo(JavaModelUtil.getFullyQualifiedName(type), cu);
	}

	public void removeImportTo(String fullyQualifiedName, ICompilationUnit cu) throws CoreException {
		getImportRewrite(cu).removeImport(fullyQualifiedName);
	}

	public void clear() {
		fImportRewrites.clear();
	}
}
