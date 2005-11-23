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

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.Java50Fix;

/**
 * Create fixes which can transform pre Java50 code to Java50 code
 * @see org.eclipse.jdt.internal.corext.fix.Java50Fix
 *
 */
public class Java50CleanUp extends AbstractCleanUp {
	
	/**
	 * Add '@Deprecated' annotation in front of deprecated members.<p>
	 * i.e.:<pre><code>
	 *      &#x2f;**@deprecated*&#x2f;
	 *      int i;
	 *  ->
	 *      &#x2f;**@deprecated*&#x2f;
	 *      &#x40;Deprecated
	 *      int i;</pre></code>  
	 */
	public static final int ADD_DEPRECATED_ANNOTATION= 1;
	
	/**
	 * Add '@Override' annotation in front of overriding methods.<p>
	 * i.e.:<pre><code>
	 * class E1 {void foo();}
	 * class E2 extends E1 {
	 * 	 void foo(); -> &#x40;Override void foo();
	 * }</pre></code>  
	 */
	public static final int ADD_OVERRIDE_ANNOATION= 2;
	
	private static final int DEFAULT_FLAG= ADD_DEPRECATED_ANNOTATION | ADD_OVERRIDE_ANNOATION;
	private static final String SECTION_NAME= "CleanUp_Java50"; //$NON-NLS-1$

	public Java50CleanUp(int flag) {
		super(flag);
	}

	public Java50CleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return Java50Fix.createCleanUp(compilationUnit, 
				isFlag(ADD_OVERRIDE_ANNOATION), 
				isFlag(ADD_DEPRECATED_ANNOTATION));
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (isFlag(ADD_OVERRIDE_ANNOATION))
			options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.WARNING);
		
		if (isFlag(ADD_DEPRECATED_ANNOTATION))
			options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.WARNING);
		return options;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		addCheckBox(composite, ADD_OVERRIDE_ANNOATION, MultiFixMessages.Java50MultiFix_AddMissingOverride_description);
		addCheckBox(composite, ADD_DEPRECATED_ANNOTATION, MultiFixMessages.Java50MultiFix_AddMissingDeprecated_description);
		
		return composite;
	}
	
	public void saveSettings(IDialogSettings settings) {
		super.saveSettings(getSection(settings, SECTION_NAME));
	}

}
