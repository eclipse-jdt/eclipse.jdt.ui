/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;

/**
 * Updates the IBM copyright header to the current year.
 * <strong>For internal use only, use at our own risk!</strong>
 *
 * @since 3.4
 */
public class UpdateCopyrightFix extends AbstractFix {
	
	public static final String CURRENT_YEAR= new SimpleDateFormat("yyyy").format(new Date()); //$NON-NLS-1$

	private static final Pattern CURRENT_YEAR_PATTERN= Pattern.compile("Copyright \\(c\\) " + CURRENT_YEAR + " IBM Corporation and others."); //$NON-NLS-1$ //$NON-NLS-2$
	private static final Pattern BEFORE_YEAR_CURRENT_YEAR_PATTERN= Pattern.compile("Copyright \\(c\\) (\\d\\d\\d\\d), " + CURRENT_YEAR + " IBM Corporation and others."); //$NON-NLS-1$ //$NON-NLS-2$
	private static final Pattern BEFORE_YEAR_PATTERN= Pattern.compile("Copyright \\(c\\) (\\d\\d\\d\\d) IBM Corporation and others."); //$NON-NLS-1$
	private static final Pattern BEFORE_YEAR_BEFORE_YEAR_PATTERN= Pattern.compile("Copyright \\(c\\) (\\d\\d\\d\\d), (\\d\\d\\d\\d) IBM Corporation and others."); //$NON-NLS-1$
	
	public static IFix createCleanUp(ICompilationUnit compilationUnit, boolean updateIbmCopyright) throws CoreException {
		if (!updateIbmCopyright)
			return null;
		
		IBuffer buffer= compilationUnit.getBuffer();
		if (buffer.getLength() < 137)
			return null;
		
		String oneYearHeaderText= buffer.getText(85, 46);
		
		Matcher matcher= CURRENT_YEAR_PATTERN.matcher(oneYearHeaderText);
		if (matcher.matches())
			return null;
		
		String twoYearHeaderText= buffer.getText(85, 52);
		
		matcher= BEFORE_YEAR_CURRENT_YEAR_PATTERN.matcher(twoYearHeaderText);
		if (matcher.matches())
			return null;

		TextEdit edit= null;
		
		matcher= BEFORE_YEAR_BEFORE_YEAR_PATTERN.matcher(twoYearHeaderText);
		if (matcher.matches()) {
			String firstYear= matcher.group(1);
		
			edit= new ReplaceEdit(85, 52, getReplaceString(firstYear));
		} else {
			matcher= BEFORE_YEAR_PATTERN.matcher(oneYearHeaderText);
			if (matcher.matches()) {
				String firstYear= matcher.group(1);
				
				edit= new ReplaceEdit(85, 46, getReplaceString(firstYear));
			}
		}
		
		if (edit == null)
			return null;
		
		CompilationUnitChange change= new CompilationUnitChange("Update Copyright", compilationUnit); //$NON-NLS-1$
		change.setEdit(edit);
		
		return new UpdateCopyrightFix(change);
	}
	
	private static String getReplaceString(String firstYear) {
		return new StringBuffer().append("Copyright (c) ").append(firstYear).append(", ").append(CURRENT_YEAR).append(" IBM Corporation and others.").toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private final CompilationUnitChange fChange;

	protected UpdateCopyrightFix(CompilationUnitChange change) {
		super("Update IBM Copyright"); //$NON-NLS-1$
		fChange= change;
	}

	/**
	 * {@inheritDoc}
	 */
	public CompilationUnitChange createChange() throws CoreException {
		return fChange;
	}

}
