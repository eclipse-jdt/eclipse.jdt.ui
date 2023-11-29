/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CodeFormatFix;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.fix.IMultiLineCleanUp.MultiLineCleanUpContext;

public class CodeFormatCleanUp extends AbstractCleanUp {

	public CodeFormatCleanUp() {
		super();
	}

	public CodeFormatCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requiresChangedRegions= isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE) && isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		return new CleanUpRequirements(false, false, requiresChangedRegions, null);
	}

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		ICompilationUnit compilationUnit= context.getCompilationUnit();
		if (compilationUnit == null)
			return null;

		try {
			IRegion[] regions;
			if (context instanceof MultiLineCleanUpContext) {
				regions= ((MultiLineCleanUpContext)context).getRegions();
			} else {
				regions= null;
			}
			boolean removeWhitespaces= isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			return CodeFormatFix.createCleanUp(compilationUnit,
					regions,
					isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE),
					removeWhitespaces && isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL),
					removeWhitespaces && isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY),
					isEnabled(CleanUpConstants.FORMAT_CORRECT_INDENTATION));
		} catch (CoreException e) {
			throw new CoreException(Status.error("Error formating " + compilationUnit.getPath(), e)); //$NON-NLS-1$
		} catch (RuntimeException e) {
			throw new RuntimeException("Error formating " + compilationUnit.getPath(), e); //$NON-NLS-1$
		}
	}

	@Override
	public String[] getStepDescriptions() {
		ArrayList<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE))
			result.add(MultiFixMessages.CodeFormatCleanUp_description);

		if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES)) {
			if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL)) {
				result.add(MultiFixMessages.CodeFormatCleanUp_RemoveTrailingAll_description);
			} else if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY)) {
				result.add(MultiFixMessages.CodeFormatCleanUp_RemoveTrailingNoEmpty_description);
			}
		}

		if (isEnabled(CleanUpConstants.FORMAT_CORRECT_INDENTATION))
			result.add(MultiFixMessages.CodeFormatCleanUp_correctIndentation_description);

		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getPreview() {
		StringBuilder buf= new StringBuilder();
		buf.append("/**\n"); //$NON-NLS-1$
		buf.append(" *A Javadoc comment\n"); //$NON-NLS-1$
		buf.append("* @since 2007\n"); //$NON-NLS-1$
		buf.append(" */\n"); //$NON-NLS-1$
		buf.append("public class Engine {\n"); //$NON-NLS-1$
		buf.append("  public void start() {}\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES) && isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL)) {
			buf.append("\n"); //$NON-NLS-1$
		} else {
			buf.append("    \n"); //$NON-NLS-1$
		}
		if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES)) {
			buf.append("    public\n"); //$NON-NLS-1$
		} else {
			buf.append("    public \n"); //$NON-NLS-1$
		}
		buf.append("        void stop() {\n"); //$NON-NLS-1$
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$

		return buf.toString();
	}
}
