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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.Java50Fix;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

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
	
	/**
	 * Adds type parameters to raw type references.<p>
	 * i.e.:<pre><code>
	 * List l; -> List<Object> l;
	 */
	public static final int ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE= 8;
	
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
				isFlag(ADD_DEPRECATED_ANNOTATION), 
				isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return Java50Fix.createCleanUp(compilationUnit, problems,
				isFlag(ADD_OVERRIDE_ANNOATION), 
				isFlag(ADD_DEPRECATED_ANNOTATION),
				isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE));
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (isFlag(ADD_OVERRIDE_ANNOATION))
			options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.WARNING);
		
		if (isFlag(ADD_DEPRECATED_ANNOTATION))
			options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.WARNING);
		
		if (isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE))
			options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
				
		return options;
	}

	public Control createConfigurationControl(Composite parent, IJavaProject project) {
		
		Button button= new Button(parent, SWT.CHECK);
		button.setText(MultiFixMessages.Java50CleanUp_addMissingAnnotations_checkBoxLabel);
		button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		int[] flags= new int[] {ADD_OVERRIDE_ANNOATION, ADD_DEPRECATED_ANNOTATION};
		int[] uiFlags= new int[] {1073741824, 536870912};
		String[] labels= new String[] {MultiFixMessages.Java50CleanUp_override_checkBoxLabel, MultiFixMessages.Java50CleanUp_deprecated_checkBoxLabel};
		
		Button[] boxes= createSubGroup(parent, button, SWT.CHECK, flags, labels, uiFlags, true);
		
		if (project != null && !JavaModelUtil.is50OrHigher(project)) {
			boxes[0].setEnabled(false);
			boxes[1].setEnabled(false);
			button.setEnabled(false);
		}
		
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
		if (isFlag(ADD_OVERRIDE_ANNOATION))
			result.add(MultiFixMessages.Java50MultiFix_AddMissingOverride_description);
		if (isFlag(ADD_DEPRECATED_ANNOTATION))
			result.add(MultiFixMessages.Java50MultiFix_AddMissingDeprecated_description);
		if (isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE))
			result.add(MultiFixMessages.Java50CleanUp_AddTypeParameters_description);
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isFlag(ADD_OVERRIDE_ANNOATION)) {
			Java50Fix fix= Java50Fix.createAddOverrideAnnotationFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		if (isFlag(ADD_DEPRECATED_ANNOTATION)) {
			Java50Fix fix= Java50Fix.createAddDeprectatedAnnotation(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		if (isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE)) {
			Java50Fix fix= Java50Fix.createRawTypeReferenceFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isFlag(ADD_OVERRIDE_ANNOATION)) {
			result+= getNumberOfProblems(problems, IProblem.MissingOverrideAnnotation);
		}
		if (isFlag(ADD_DEPRECATED_ANNOTATION)) {
			for (int i=0;i<problems.length;i++) {
				int id= problems[i].getID();
				if (id == IProblem.FieldMissingDeprecatedAnnotation || id == IProblem.MethodMissingDeprecatedAnnotation || id == IProblem.TypeMissingDeprecatedAnnotation)
					result++;
			}
		}
		if (isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE)) {
			for (int i=0;i<problems.length;i++) {
				int id= problems[i].getID();
				if (id == IProblem.UnsafeTypeConversion || id == IProblem.RawTypeReference || id == IProblem.UnsafeRawMethodInvocation)
					result++;
			}
		}
		return result;
	}
	
}
