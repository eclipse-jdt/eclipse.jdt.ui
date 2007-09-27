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
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.UpdateCopyrightFix;

/**
 * This clean up is for internal use only. You can enabled it by adding
 * <code>-Dorg.eclipse.jdt.ui/UpdateCopyrightOnSave=true</code>
 * to your VM options.
 * 
 * @since 3.4
 */
public class CopyrightUpdaterCleanUp extends AbstractCleanUp {

	public static final String UPDATE_IBM_COPYRIGHT_TO_CURRENT_YEAR= "cleanup.update_ibm_copyright_to_current_year"; //$NON-NLS-1$

	public CopyrightUpdaterCleanUp(Map values) {
		super(values);
	}

	public CopyrightUpdaterCleanUp() {
		super();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public CleanUpOptions getDefaultOptions(int kind) {
		CleanUpOptions result= new CleanUpOptions();
		
		result.setOption(UPDATE_IBM_COPYRIGHT_TO_CURRENT_YEAR, CleanUpOptions.FALSE);
		
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CleanUpContext context) throws CoreException {
		if (!isEnabled(UPDATE_IBM_COPYRIGHT_TO_CURRENT_YEAR))
			return null;
		
		return UpdateCopyrightFix.createCleanUp(context.getCompilationUnit(), true);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		ArrayList result= new ArrayList();
		
		if (isEnabled(UPDATE_IBM_COPYRIGHT_TO_CURRENT_YEAR))
			result.add("Update IBM Copyright to current year"); //$NON-NLS-1$
		
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		buf.append("/*******************************************************************************\n"); //$NON-NLS-1$
		if (isEnabled(UPDATE_IBM_COPYRIGHT_TO_CURRENT_YEAR)) {
			buf.append(" * Copyright (c) 2005, ").append(UpdateCopyrightFix.CURRENT_YEAR).append(" IBM Corporation and others.\n"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			buf.append(" * Copyright (c) 2005 IBM Corporation and others.\n");			 //$NON-NLS-1$
		}
		buf.append(" * All rights reserved. This  program and the accompanying materials\n"); //$NON-NLS-1$
		buf.append(" * are made available under the terms of the Eclipse Public License v1.0\n"); //$NON-NLS-1$
		buf.append(" * which accompanies this distribution, and is available at\n"); //$NON-NLS-1$
		buf.append(" * http://www.eclipse.org/legal/epl-v10.html\n"); //$NON-NLS-1$
		buf.append(" *\n"); //$NON-NLS-1$
		buf.append(" * Contributors:\n"); //$NON-NLS-1$
		buf.append(" *     IBM Corporation - initial API and implementation\n"); //$NON-NLS-1$
		buf.append(" *******************************************************************************/\n"); //$NON-NLS-1$
		
		return buf.toString();
	}
}
