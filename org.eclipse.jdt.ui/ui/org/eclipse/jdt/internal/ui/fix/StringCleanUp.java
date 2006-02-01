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
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.StringFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Create fixes which can solve problems in connection with Strings
 * @see org.eclipse.jdt.internal.corext.fix.StringFix
 *
 */
public class StringCleanUp extends AbstractCleanUp {
	
	/**
	 * Add '$NON-NLS$' tags to non externalized strings.<p>
	 * i.e.:<pre><code>
	 * 	 String s= ""; -> String s= ""; //$NON-NLS-1$</code></pre>  
	 */
	public static final int ADD_MISSING_NLS_TAG= 1;
	
	/**
	 * Remove unnecessary '$NON-NLS$' tags.<p>
	 * i.e.:<pre><code>
	 *   String s; //$NON-NLS-1$ -> String s;</code></pre>
	 */
	public static final int REMOVE_UNNECESSARY_NLS_TAG= 2;
	
	private static final int DEFAULT_FLAG= REMOVE_UNNECESSARY_NLS_TAG;
	private static final String SECTION_NAME= "CleanUp_NLSTag"; //$NON-NLS-1$

	public StringCleanUp(int flag) {
		super(flag);
	}

	public StringCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;

		return StringFix.createCleanUp(compilationUnit, 
				isFlag(ADD_MISSING_NLS_TAG), 
				isFlag(REMOVE_UNNECESSARY_NLS_TAG));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return StringFix.createCleanUp(compilationUnit, problems,
				isFlag(ADD_MISSING_NLS_TAG), 
				isFlag(REMOVE_UNNECESSARY_NLS_TAG));
	}

	public Map getRequiredOptions() {
		Map result= new Hashtable();
		
		if (isFlag(ADD_MISSING_NLS_TAG) || isFlag(REMOVE_UNNECESSARY_NLS_TAG))
			result.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		
		return result;
	}

	public Control createConfigurationControl(Composite parent, IJavaProject project) {

		addCheckBox(parent, REMOVE_UNNECESSARY_NLS_TAG, MultiFixMessages.StringMultiFix_RemoveUnnecessaryNonNls_description);
		
		return parent;
	}
	
	public void saveSettings(IDialogSettings settings) {
		super.saveSettings(getSection(settings, SECTION_NAME));
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isFlag(ADD_MISSING_NLS_TAG))
			result.add(removeMemonic(MultiFixMessages.StringMultiFix_AddMissingNonNls_description));
		if (isFlag(REMOVE_UNNECESSARY_NLS_TAG))
			result.add(removeMemonic(MultiFixMessages.StringMultiFix_RemoveUnnecessaryNonNls_description));
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 * @throws CoreException 
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		return StringFix.createFix(compilationUnit, problem, isFlag(REMOVE_UNNECESSARY_NLS_TAG), isFlag(ADD_MISSING_NLS_TAG)) != null;
	}

}
