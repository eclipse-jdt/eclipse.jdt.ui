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

import org.eclipse.jdt.internal.corext.fix.CodeStyleFix;
import org.eclipse.jdt.internal.corext.fix.IFix;

/**
 * Creates fixes which can resolve code style issues 
 * @see org.eclipse.jdt.internal.corext.fix.CodeStyleFix
 */
public class CodeStyleCleanUp extends AbstractCleanUp {
	
	/**
	 * Adds a 'this' qualifier to field accesses.<p>
	 * i.e.:<pre><code>
	 *   int fField;
	 *   void foo() {fField= 10;} -> void foo() {this.fField= 10;}</pre></code>
	 */
	public static final int QUALIFY_FIELD_ACCESS= 1;
	
	/**
	 * Changes non static accesses to static members to static accesses.<p>
	 * i.e.:<pre><code>
	 * class E {
	 *   public static int i;
	 *   void foo() {(new E()).i= 10;} -> void foo() {E.i= 10;}
	 * }</code></pre>
	 */
	public static final int CHANGE_NON_STATIC_ACCESS_TO_STATIC= 2;
	
	/**
	 * Qualifies static field accesses with declaring type.<p>
	 * i.e.:<pre><code>
	 * class E {
	 *   public static int i;
	 *   void foo() {i= 10;} -> void foo() {E.i= 10;}
	 * }</code></pre>
	 */
	public static final int QUALIFY_STATIC_FIELD_ACCESS= 4;
	
	/**
	 * Changes indirect accesses to static members to direct ones.<p>
	 * i.e.:<pre><code>
	 * class E {public static int i;}
	 * class ESub extends E {
	 *   void foo() {ESub.i= 10;} -> void foo() {E.i= 10;}
	 * }</code></pre>
	 */
	public static final int CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT= 8;
	
	/**
	 * Adds block to control statement body if the body is not a block.<p>
	 * i.e.:<pre><code>
	 * 	 if (b) foo(); -> if (b) {foo();}</code></pre>
	 */
	public static final int ADD_BLOCK_TO_CONTROL_STATEMENTS= 16;

	private static final int DEFAULT_FLAG= CHANGE_NON_STATIC_ACCESS_TO_STATIC | CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT;
	private static final String SECTION_NAME= "CleanUp_CodeStyle"; //$NON-NLS-1$

	public CodeStyleCleanUp(int flag) {
		super(flag);
	}

	public CodeStyleCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return CodeStyleFix.createCleanUp(compilationUnit, 
				isFlag(QUALIFY_FIELD_ACCESS), 
				isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC), 
				isFlag(QUALIFY_STATIC_FIELD_ACCESS), 
				isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT),
				isFlag(ADD_BLOCK_TO_CONTROL_STATEMENTS));
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC))
			options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.WARNING);
		if (isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT))
			options.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.WARNING);
		return options;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		addCheckBox(composite, QUALIFY_FIELD_ACCESS, MultiFixMessages.CodeStyleMultiFix_AddThisQualifier_description);
		addCheckBox(composite, QUALIFY_STATIC_FIELD_ACCESS, MultiFixMessages.CodeStyleMultiFix_QualifyAccessToStaticField);
		addCheckBox(composite, CHANGE_NON_STATIC_ACCESS_TO_STATIC, MultiFixMessages.CodeStyleMultiFix_ChangeNonStaticAccess_description);
		addCheckBox(composite, CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT, MultiFixMessages.CodeStyleMultiFix_ChangeIndirectAccessToStaticToDirect);
		addCheckBox(composite, ADD_BLOCK_TO_CONTROL_STATEMENTS, MultiFixMessages.CodeStyleMultiFix_ConvertSingleStatementInControlBodeyToBlock_description);
		
		return composite;
	}

	public void saveSettings(IDialogSettings settings) {
		super.saveSettings(getSection(settings, SECTION_NAME));
	}
	
}
