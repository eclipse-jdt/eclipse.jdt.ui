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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

class AccessorClassCreator {

	private final ICompilationUnit fCu;
	private final String fAccessorClassName;
	private final IPath fAccessorPath;
	private final IPath fResourceBundlePath;
	private final IPackageFragment fAccessorPackage;
	private final boolean fIsEclipseNLS;
	private final NLSSubstitution[] fNLSSubstitutions;

	private AccessorClassCreator(ICompilationUnit cu, String accessorClassname, IPath accessorPath, IPackageFragment accessorPackage, IPath resourceBundlePath, boolean isEclipseNLS, NLSSubstitution[] nlsSubstitutions) {
		fCu= cu;
		fAccessorClassName= accessorClassname;
		fAccessorPath= accessorPath;
		fAccessorPackage= accessorPackage;
		fResourceBundlePath= resourceBundlePath;
		fIsEclipseNLS= isEclipseNLS;
		fNLSSubstitutions= nlsSubstitutions;
	}

	public static Change create(ICompilationUnit cu, String accessorClassname, IPath accessorPath, IPackageFragment accessorPackage, IPath resourceBundlePath, boolean isEclipseNLS, NLSSubstitution[] nlsSubstitutions, IProgressMonitor pm) throws CoreException {
		AccessorClassCreator accessorClass= new AccessorClassCreator(cu, accessorClassname, accessorPath, accessorPackage, resourceBundlePath, isEclipseNLS, nlsSubstitutions);

		return new CreateTextFileChange(accessorPath, accessorClass.createAccessorCUSource(pm), null, "java"); //$NON-NLS-1$
	}

	private String createAccessorCUSource(IProgressMonitor pm) throws CoreException {
		IProject project= getFileHandle(fAccessorPath).getProject();
		String lineDelimiter= StubUtility.getLineDelimiterPreference(project);
		return CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, getUnformattedSource(pm), 0, null, lineDelimiter, fCu.getJavaProject());
	}
	
	private static IFile getFileHandle(IPath filePath) {
		if (filePath == null)
			return null;
		return ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
	}

	private String getUnformattedSource(IProgressMonitor pm) throws CoreException {
		ICompilationUnit newCu= null;
		try {
			newCu= WorkingCopyUtil.getNewWorkingCopy(fAccessorPackage, fAccessorPath.lastSegment());

			String typeComment= null, fileComment= null;
			final IJavaProject project= newCu.getJavaProject();
			final String lineDelim= StubUtility.getLineDelimiterUsed(project);
			if (StubUtility.doAddComments(project)) {
				typeComment= CodeGeneration.getTypeComment(newCu, fAccessorClassName, lineDelim);
				fileComment= CodeGeneration.getFileComment(newCu, lineDelim);
			}
			String classContent= createClass(lineDelim);
			String cuContent= CodeGeneration.getCompilationUnitContent(newCu, fileComment, typeComment, classContent, lineDelim);
			if (cuContent == null) {
				StringBuffer buf= new StringBuffer();
				if (fileComment != null) {
					buf.append(fileComment).append(lineDelim);
				}
				if (!fAccessorPackage.isDefaultPackage()) {
					buf.append("package ").append(fAccessorPackage.getElementName()).append(';'); //$NON-NLS-1$
				}
				buf.append(lineDelim).append(lineDelim);
				if (typeComment != null) {
					buf.append(typeComment).append(lineDelim);
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
		String[] order= JavaPreferencesSettings.getImportOrderPreference(newCu.getJavaProject());
		int importThreshold= JavaPreferencesSettings.getImportNumberThreshold(newCu.getJavaProject());
		ImportsStructure is= new ImportsStructure(newCu, order, importThreshold, true);
		if (fIsEclipseNLS) {
			is.addImport("org.eclipse.osgi.util.NLS"); //$NON-NLS-1$
		} else {
			is.addImport("java.util.MissingResourceException"); //$NON-NLS-1$
			is.addImport("java.util.ResourceBundle"); //$NON-NLS-1$
		}
		is.create(false, pm);
	}

	private String createClass(String lineDelim) throws CoreException {
		if (fIsEclipseNLS) {
			return "public class " + fAccessorClassName + " extends NLS {" //$NON-NLS-2$ //$NON-NLS-1$
				+ "private static final String " + NLSRefactoring.BUNDLE_NAME + " = \"" + getResourceBundleName() + "\"; " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ NLSElement.createTagText(1) + lineDelim
				+ createConstructor(lineDelim) + lineDelim
				+ createStaticInitializer(lineDelim) + lineDelim
				+ createStaticFields(lineDelim) + lineDelim
				+ "}" + lineDelim; //$NON-NLS-1$
		} else {
			return "public class " + fAccessorClassName + " {" //$NON-NLS-2$ //$NON-NLS-1$
			+ "private static final String " + NLSRefactoring.BUNDLE_NAME + " = \"" + getResourceBundleName() + "\"; " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			+ NLSElement.createTagText(1)
			+ lineDelim
			+ lineDelim + "private static final ResourceBundle " + getResourceBundleConstantName() + "= ResourceBundle.getBundle(" + NLSRefactoring.BUNDLE_NAME + ");"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			+ lineDelim
			+ lineDelim	+ createConstructor(lineDelim)
			+ lineDelim + createGetStringMethod(lineDelim)
			+ lineDelim + "}" //$NON-NLS-1$
			+ lineDelim;
		}
	}

	private String getResourceBundleConstantName() {
		return "RESOURCE_BUNDLE";//$NON-NLS-1$
	}
	
	private String createStaticFields(String lineDelim) {
		StringBuffer buf= new StringBuffer();
		int count= 0;
		
		for (int i= 0; i < fNLSSubstitutions.length; i++) {
			NLSSubstitution substitution= fNLSSubstitutions[i];
			int newState= substitution.getState();
			if (substitution.hasStateChanged()
					&& newState == NLSSubstitution.EXTERNALIZED && substitution.getInitialState() == NLSSubstitution.INTERNALIZED) {
				if (count > 0)
					buf.append(lineDelim);
				appendStaticField(buf,substitution);
			}
		}
		
		return buf.toString();
	}

	private void appendStaticField(StringBuffer buf, NLSSubstitution substitution) {
		buf.append("public static String "); //$NON-NLS-1$
		buf.append(substitution.getKey());
		buf.append(';');
	}
	
	private String createGetStringMethod(String lineDelim) throws CoreException {
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
	
	private String createStaticInitializer(String lineDelim) throws CoreException {
		return "static {" //$NON-NLS-1$
		+ lineDelim
		+ "// initialize resource bundle" //$NON-NLS-1$
		+ lineDelim
		+ "NLS.initializeMessages(BUNDLE_NAME, " + fAccessorClassName + ".class);" //$NON-NLS-1$ //$NON-NLS-2$
		+ lineDelim
		+ "}"; //$NON-NLS-1$
	}

	private String createConstructor(String lineDelim) {
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
