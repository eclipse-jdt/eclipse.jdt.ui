/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.SortMembersFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class SortMembersCleanUp extends AbstractCleanUp {
	
	public SortMembersCleanUp() {
		super();
    }
	
	public SortMembersCleanUp(Map options) {
		super(options);
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		boolean sortMembers= isEnabled(CleanUpConstants.SORT_MEMBERS);
		return SortMembersFix.createCleanUp(compilationUnit, sortMembers, sortMembers && isEnabled(CleanUpConstants.SORT_MEMBERS_ALL));
	}

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

	public Map getRequiredOptions() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		if (isEnabled(CleanUpConstants.SORT_MEMBERS)) {
			if (isEnabled(CleanUpConstants.SORT_MEMBERS_ALL)) {
				return new String[] {MultiFixMessages.SortMembersCleanUp_AllMembers_description};
			} else {
				return new String[] {MultiFixMessages.SortMembersCleanUp_Excluding_description};
			}
		}		
		return null;
	}
	
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		buf.append("public class SortExample {\n"); //$NON-NLS-1$
		
		if ((isEnabled(CleanUpConstants.SORT_MEMBERS) && isEnabled(CleanUpConstants.SORT_MEMBERS_ALL))) {
			buf.append("  private String bar;\n"); //$NON-NLS-1$
			buf.append("  private String foo;\n"); //$NON-NLS-1$
		} else {
			buf.append("  private String foo;\n"); //$NON-NLS-1$
			buf.append("  private String bar;\n"); //$NON-NLS-1$
		}
		
		if (isEnabled(CleanUpConstants.SORT_MEMBERS)) {
			buf.append("  private void bar();\n"); //$NON-NLS-1$
			buf.append("  private void foo();\n"); //$NON-NLS-1$
		} else {
			buf.append("  private void foo();\n"); //$NON-NLS-1$
			buf.append("  private void bar();\n"); //$NON-NLS-1$
		}
		
		buf.append("}\n"); //$NON-NLS-1$
		
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
		return -1;
	}

    public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
	    return false;
    }
    
	public boolean requireAST(ICompilationUnit unit) throws CoreException {
		return isEnabled(CleanUpConstants.SORT_MEMBERS);
	}    
}
