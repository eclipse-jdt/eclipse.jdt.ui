/*****************************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.ltk.core.refactoring.Change;

class AccessorClass {

	private final ICompilationUnit fCu;
	private final String fAccessorClassName;
	private final CodeGenerationSettings fCodeGenerationSettings;
	private final IPath fAccessorPath;
	private final IPath fResourceBundlePath;
	private final IPackageFragment fAccessorPackage;

	private AccessorClass(ICompilationUnit cu, String accessorClassname, CodeGenerationSettings codeGenerationSettings,
		IPath accessorPath, IPackageFragment accessorPackage, IPath resourceBundlePath) {
		fCu= cu;
		fAccessorClassName= accessorClassname;
		fCodeGenerationSettings= codeGenerationSettings;
		fAccessorPath= accessorPath;
		fAccessorPackage= accessorPackage;
		fResourceBundlePath= resourceBundlePath;
	}

	public static Change create(ICompilationUnit cu, String accessorClassname, CodeGenerationSettings codeGenerationSettings,
		IPath accessorPath, IPackageFragment accessorPackage, IPath resourceBundlePath, IProgressMonitor pm)
		throws CoreException {
		AccessorClass accessorClass= new AccessorClass(cu, accessorClassname, codeGenerationSettings, accessorPath,
			accessorPackage, resourceBundlePath);

		return new CreateTextFileChange(accessorPath, accessorClass.createAccessorCUSource(pm), "java"); //$NON-NLS-1$
	}

	private String createAccessorCUSource(IProgressMonitor pm) throws CoreException {
		return CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, getUnformattedSource(pm), 0, null, null, fCu
			.getJavaProject());
	}

	private String getUnformattedSource(IProgressMonitor pm) throws CoreException {
		ICompilationUnit newCu= null;
		try {
			newCu= WorkingCopyUtil.getNewWorkingCopy(fAccessorPackage, fAccessorPath.lastSegment());

			String comment= CodeGeneration.getTypeComment(newCu, fAccessorClassName, NLSRefactoring.fgLineDelimiter);
			newCu.getBuffer().setContents(
				CodeGeneration.getCompilationUnitContent(newCu, comment, createClass(), NLSRefactoring.fgLineDelimiter));
			addImportsToAccessorCu(newCu, pm);
			return newCu.getSource();
		} finally {
			if (newCu != null) {
				newCu.discardWorkingCopy();
			}
		}
	}

	private void addImportsToAccessorCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		ImportsStructure is= new ImportsStructure(newCu, fCodeGenerationSettings.importOrder,
			fCodeGenerationSettings.importThreshold, true);
		is.addImport("java.util.MissingResourceException"); //$NON-NLS-1$
		is.addImport("java.util.ResourceBundle"); //$NON-NLS-1$
		is.create(false, pm);
	}

	private String createClass() throws CoreException {
		String ld= NLSRefactoring.fgLineDelimiter; //want shorter name
		return "public class " + fAccessorClassName + " {" //$NON-NLS-2$ //$NON-NLS-1$
			+ "private static final String " + NLSRefactoring.BUNDLE_NAME + " = \"" + getResourceBundleName() + "\";" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			+ NLSElement.createTagText(1) + ld + ld 
			+ "private static final ResourceBundle " + getResourceBundleConstantName() + "= ResourceBundle.getBundle(" //$NON-NLS-1$ //$NON-NLS-2$
			+ NLSRefactoring.BUNDLE_NAME + ");" + ld + ld //$NON-NLS-1$
			+ createConstructor() + ld + createGetStringMethod() + ld + "}" + ld; //$NON-NLS-1$
	}

	private String getResourceBundleConstantName() {
		return "RESOURCE_BUNDLE";//$NON-NLS-1$
	}

	private String createGetStringMethod() throws CoreException {
		String bodyStatement= new StringBuffer().append("try {").append(NLSRefactoring.fgLineDelimiter) //$NON-NLS-1$
			.append("return ") //$NON-NLS-1$
			.append(getResourceBundleConstantName()).append(".getString(key);").append(NLSRefactoring.fgLineDelimiter) //$NON-NLS-1$
			.append("} catch (MissingResourceException e) {").append(NLSRefactoring.fgLineDelimiter) //$NON-NLS-1$
			.append("return '!' + key + '!';").append(NLSRefactoring.fgLineDelimiter) //$NON-NLS-1$
			.append("}").toString(); //$NON-NLS-1$

		String methodBody= CodeGeneration.getMethodBodyContent(fCu, fAccessorClassName, 
			"getString", false, bodyStatement, //$NON-NLS-1$
			NLSRefactoring.fgLineDelimiter); 
		if (methodBody == null) {
			methodBody= ""; //$NON-NLS-1$
		}
		return "public static String getString(String key) {" //$NON-NLS-1$
			+ NLSRefactoring.fgLineDelimiter + methodBody + NLSRefactoring.fgLineDelimiter + '}';
	}

	private String createConstructor() throws CoreException {
		return "private " + fAccessorClassName + "(){" + //$NON-NLS-2$//$NON-NLS-1$
				NLSRefactoring.fgLineDelimiter + '}';
	}

	/* Currently not used.
	private String createGetStringMethodComment() throws CoreException {
		if (fCodeGenerationSettings.createComments) {
			String comment= CodeGeneration.getMethodComment(fCu, fAccessorClassName, "getString", //$NON-NLS-1$
				new String[]{"key"}, //$NON-NLS-1$
				new String[0], "QString;", //$NON-NLS-1$
				null, NLSRefactoring.fgLineDelimiter);
			if (comment == null) {
				return "";//$NON-NLS-1$
			}

			return comment + NLSRefactoring.fgLineDelimiter;
		} else {
			return "";//$NON-NLS-1$
		}
	}
	*/

	private String getPropertyFileName() {
		return fResourceBundlePath.lastSegment();
	}

	private String getPropertyFileNameWithoutExtension() {
		String fileName= getPropertyFileName();
		return fileName.substring(0, fileName.indexOf(NLSRefactoring.PROPERTY_FILE_EXT));
	}

	private String getResourceBundleName() throws CoreException {
		IResource res= ResourcesPlugin.getWorkspace().getRoot().findMember(fResourceBundlePath.removeLastSegments(1));
		if (res != null && res.exists()) {
			IJavaElement el= JavaCore.create(res);
			if (el instanceof IPackageFragment) {
				IPackageFragment p= (IPackageFragment)el;
				return p.getElementName() + '.' + getPropertyFileNameWithoutExtension();
			} else if (el instanceof IPackageFragmentRoot) {
				return getPropertyFileNameWithoutExtension();
			}
		}
		throw new CoreException(new StatusInfo(IStatus.ERROR, NLSMessages.getString("Resourcebundle not specified"))); //$NON-NLS-1$
	}
}