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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.ltk.core.refactoring.Change;

class AccessorClass {

	private final ICompilationUnit fCu;
	private final String fAccessorClassName;
	private final IPath fAccessorPath;
	private final IPath fResourceBundlePath;
	private final IPackageFragment fAccessorPackage;

	private static String lineDelim= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

	private AccessorClass(ICompilationUnit cu, String accessorClassname, IPath accessorPath, IPackageFragment accessorPackage, IPath resourceBundlePath) {
		fCu= cu;
		fAccessorClassName= accessorClassname;
		fAccessorPath= accessorPath;
		fAccessorPackage= accessorPackage;
		fResourceBundlePath= resourceBundlePath;
	}

	public static Change create(ICompilationUnit cu, String accessorClassname, IPath accessorPath, IPackageFragment accessorPackage, IPath resourceBundlePath, IProgressMonitor pm) throws CoreException {
		AccessorClass accessorClass= new AccessorClass(cu, accessorClassname, accessorPath, accessorPackage, resourceBundlePath);

		return new CreateTextFileChange(accessorPath, accessorClass.createAccessorCUSource(pm), "java"); //$NON-NLS-1$
	}

	private String createAccessorCUSource(IProgressMonitor pm) throws CoreException {
		return CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, getUnformattedSource(pm), 0, null, null, fCu.getJavaProject());
	}

	private String getUnformattedSource(IProgressMonitor pm) throws CoreException {
		ICompilationUnit newCu= null;
		try {
			newCu= WorkingCopyUtil.getNewWorkingCopy(fAccessorPackage, fAccessorPath.lastSegment());

			String comment= CodeGeneration.getTypeComment(newCu, fAccessorClassName, lineDelim);
			String classContent= createClass();
			String cuContent= CodeGeneration.getCompilationUnitContent(newCu, comment, classContent, lineDelim);
			if (cuContent == null) {
				StringBuffer buf= new StringBuffer();
				if (!fAccessorPackage.isDefaultPackage()) {
					buf.append("package ").append(fAccessorPackage.getElementName()).append(';'); //$NON-NLS-1$
				}
				buf.append(lineDelim).append(lineDelim);
				if (comment != null) {
					buf.append(comment).append(lineDelim);
				}
				buf.append(classContent);
				cuContent= buf.toString();
			}
			
			newCu.getBuffer().setContents(cuContent);
			addImportsToAccessorCu(newCu, pm);
			return newCu.getSource();
		} finally {
			if (newCu != null) {
				newCu.discardWorkingCopy();
			}
		}
	}

	private void addImportsToAccessorCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		String[] order= JavaPreferencesSettings.getImportOrderPreference(store);
		int importThreshold= JavaPreferencesSettings.getImportNumberThreshold(store);
		ImportsStructure is= new ImportsStructure(newCu, order, importThreshold, true);
		is.addImport("java.util.MissingResourceException"); //$NON-NLS-1$
		is.addImport("java.util.ResourceBundle"); //$NON-NLS-1$
		is.create(false, pm);
	}

	private String createClass() throws CoreException {
		String ld= lineDelim; //want shorter name
		return "public class " + fAccessorClassName + " {" //$NON-NLS-2$ //$NON-NLS-1$
				+ "private static final String " + NLSRefactoring.BUNDLE_NAME + " = \"" + getResourceBundleName() + "\";" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ NLSElement.createTagText(1) + ld + ld + "private static final ResourceBundle " + getResourceBundleConstantName() + "= ResourceBundle.getBundle(" //$NON-NLS-1$ //$NON-NLS-2$
				+ NLSRefactoring.BUNDLE_NAME + ");" + ld + ld //$NON-NLS-1$
				+ createConstructor() + ld + createGetStringMethod() + ld + "}" + ld; //$NON-NLS-1$
	}

	private String getResourceBundleConstantName() {
		return "RESOURCE_BUNDLE";//$NON-NLS-1$
	}

	private String createGetStringMethod() throws CoreException {
		String bodyStatement= new StringBuffer().append("try {").append(lineDelim) //$NON-NLS-1$
				.append("return ") //$NON-NLS-1$
				.append(getResourceBundleConstantName()).append(".getString(key);").append(lineDelim) //$NON-NLS-1$
				.append("} catch (MissingResourceException e) {").append(lineDelim) //$NON-NLS-1$
				.append("return '!' + key + '!';").append(lineDelim) //$NON-NLS-1$
				.append("}").toString(); //$NON-NLS-1$

		String methodBody= CodeGeneration.getMethodBodyContent(fCu, fAccessorClassName, "getString", false, bodyStatement, //$NON-NLS-1$
				lineDelim);
		if (methodBody == null) {
			methodBody= ""; //$NON-NLS-1$
		}
		return "public static String getString(String key) {" //$NON-NLS-1$
				+ lineDelim + methodBody + lineDelim + '}';
	}

	private String createConstructor() {
		return "private " + fAccessorClassName + "(){" + //$NON-NLS-2$//$NON-NLS-1$
				lineDelim + '}';
	}

	/* Currently not used.
	 private String createGetStringMethodComment() throws CoreException {
	 if (fCodeGenerationSettings.createComments) {
	 String comment= CodeGeneration.getMethodComment(fCu, fAccessorClassName, "getString", //$NON-NLS-1$
	 new String[]{"key"}, //$NON-NLS-1$
	 new String[0], "QString;", //$NON-NLS-1$
	 null, lineDelim);
	 if (comment == null) {
	 return "";//$NON-NLS-1$
	 }

	 return comment + lineDelim;
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
				IPackageFragment p= (IPackageFragment) el;
				return p.getElementName() + '.' + getPropertyFileNameWithoutExtension();
			} else
				if ((el instanceof IPackageFragmentRoot) || (el instanceof IJavaProject)) {
					return getPropertyFileNameWithoutExtension();
				}
		}
		throw new CoreException(new StatusInfo(IStatus.ERROR, "Resourcebundle not specified")); //$NON-NLS-1$
	}
}