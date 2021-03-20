/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.manipulation.CodeGeneration;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;

public class InfoFilesUtil {

	/**
	 * Creates a compilation unit in the given package fragment with the specified file name. The
	 * compilation unit is formatted and contains the given contents with file and type comments
	 * prepended to it.
	 *
	 * @param fileName the name of the compilation unit
	 * @param fileContent the contents of the compilation unit
	 * @param pack the package fragment to create the compilation unit in
	 * @param addComments whether to add comments
	 * @param monitor the progress monitor
	 * @throws CoreException when there is a problem while creating the compilation unit
	 */
	public static void createInfoJavaFile(String fileName, String fileContent, IPackageFragment pack, boolean addComments, IProgressMonitor monitor) throws CoreException {
		String lineDelimiter= StubUtility.getLineDelimiterUsed(pack.getJavaProject());
		StringBuilder content= new StringBuilder();
		String fileComment= addComments ? getFileComment(fileName, pack, lineDelimiter) : null;
		String typeComment= addComments ?
				(JavaModelUtil.MODULE_INFO_JAVA.equals(fileName) ? getModuleComment(fileName, pack, fileContent, lineDelimiter) : getTypeComment(fileName, pack, lineDelimiter))
				: null;
		if (fileComment != null) {
			content.append(fileComment);
			content.append(lineDelimiter);
		}

		if (typeComment != null) {
			content.append(typeComment);
			content.append(lineDelimiter);
		} else if (fileComment != null) {
			// insert an empty file comment to avoid that the file comment becomes the type comment
			content.append("/**"); //$NON-NLS-1$
			content.append(lineDelimiter);
			content.append(" *"); //$NON-NLS-1$
			content.append(lineDelimiter);
			content.append(" */"); //$NON-NLS-1$
			content.append(lineDelimiter);
		}

		content.append(fileContent);

		ICompilationUnit compilationUnit= pack.createCompilationUnit(fileName, content.toString(), true, monitor);

		JavaModelUtil.reconcile(compilationUnit);

		compilationUnit.becomeWorkingCopy(monitor);
		try {
			IBuffer buffer= compilationUnit.getBuffer();
			ISourceRange sourceRange= compilationUnit.getSourceRange();
			String originalContent= buffer.getText(sourceRange.getOffset(), sourceRange.getLength());

			int kind= CodeFormatter.K_COMPILATION_UNIT;
			if (JavaModelUtil.MODULE_INFO_JAVA.equals(fileName)) {
				kind= CodeFormatter.K_MODULE_INFO;
			}
			String formattedContent= CodeFormatterUtil.format(kind, originalContent, 0, lineDelimiter, FormatterProfileManager.getProjectSettings(pack.getJavaProject()));
			formattedContent= org.eclipse.jdt.internal.core.manipulation.util.Strings.trimLeadingTabsAndSpaces(formattedContent);
			buffer.replace(sourceRange.getOffset(), sourceRange.getLength(), formattedContent);
			compilationUnit.commitWorkingCopy(true, new SubProgressMonitor(monitor, 1));
		} finally {
			compilationUnit.discardWorkingCopy();
		}
	}

	public static String getFileComment(String fileName, IPackageFragment pack, String lineDelimiterUsed) throws CoreException {
		ICompilationUnit cu= pack.getCompilationUnit(fileName);
		return CodeGeneration.getFileComment(cu, lineDelimiterUsed);
	}

	public static String getTypeComment(String fileName, IPackageFragment pack, String lineDelimiterUsed) throws CoreException {
		ICompilationUnit cu= pack.getCompilationUnit(fileName);
		String typeName= fileName.substring(0, fileName.length() - JavaModelUtil.DEFAULT_CU_SUFFIX.length());
		return CodeGeneration.getTypeComment(cu, typeName, lineDelimiterUsed);
	}

	public static String getModuleComment(String fileName, IPackageFragment pack, String fileContent, String lineDelimiterUsed) throws CoreException {
		ICompilationUnit cu= pack.getCompilationUnit(fileName);
		Pattern p= Pattern.compile("\\s*module\\s*(\\w*).*"); //$NON-NLS-1$
		Matcher m= p.matcher(fileContent);
		String moduleName= ""; //$NON-NLS-1$
		if (m.matches()) {
			moduleName= m.group(1);
		}
		return CodeGeneration.getModuleComment(cu, moduleName, new String[0], new String[0], lineDelimiterUsed);
	}

	private InfoFilesUtil() {
	}

}
