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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CodeStyleFix;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

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
				isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return CodeStyleFix.createCleanUp(compilationUnit, problems,
				isFlag(QUALIFY_FIELD_ACCESS), 
				isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC), 
				isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT));
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC))
			options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.WARNING);
		if (isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT))
			options.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.WARNING);
		return options;
	}

	public Control createConfigurationControl(Composite parent, IJavaProject project) {
		
		indent(addCheckBox(parent, QUALIFY_FIELD_ACCESS, MultiFixMessages.CodeStyleCleanUp_useThis_checkBoxLabel));
		

			Button button= new Button(parent, SWT.CHECK);
			button.setText(MultiFixMessages.CodeStyleCleanUp_useDeclaring_checkBoxLabel);
			button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		indent(button);
			
		Composite sub= new Composite(parent, SWT.NONE);
		sub.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout layout= new GridLayout(1, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		sub.setLayout(layout);
		indent(sub);
		
			final int[] flags= new int[] {CHANGE_NON_STATIC_ACCESS_TO_STATIC, CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT, QUALIFY_STATIC_FIELD_ACCESS};
			final int[] uiFlags= new int[] {1073741824, 536870912, 268435456};
			final String[] labels= new String[] {MultiFixMessages.CodeStyleCleanUp_changeNonStatic_checkBoxLabel, MultiFixMessages.CodeStyleCleanUp_changeIndirect_checkBoxLabel, MultiFixMessages.CodeStyleCleanUp_addStaticQualifier_checkBoxLabel};
		
			createSubGroup(sub, button, SWT.CHECK, flags, labels, uiFlags, true);
		
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
		if (isFlag(QUALIFY_FIELD_ACCESS))
			result.add(MultiFixMessages.CodeStyleMultiFix_AddThisQualifier_description);
		if (isFlag(QUALIFY_STATIC_FIELD_ACCESS))
			result.add(MultiFixMessages.CodeStyleMultiFix_QualifyAccessToStaticField);
		if (isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC))
			result.add(MultiFixMessages.CodeStyleMultiFix_ChangeNonStaticAccess_description);
		if (isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT))
			result.add(MultiFixMessages.CodeStyleMultiFix_ChangeIndirectAccessToStaticToDirect);
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isFlag(QUALIFY_FIELD_ACCESS)) {
			CodeStyleFix fix= CodeStyleFix.createAddFieldQualifierFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		if (isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT)) {
			CodeStyleFix fix= CodeStyleFix.createIndirectAccessToStaticFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		if (isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC)) {
			CodeStyleFix[] fixes= CodeStyleFix.createNonStaticAccessFixes(compilationUnit, problem);
			if (fixes != null && fixes.length > 0)
				return true;
		}
		return false;
	}
	
}
