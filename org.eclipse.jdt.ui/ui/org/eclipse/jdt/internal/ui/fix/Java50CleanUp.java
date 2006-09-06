/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.Java50Fix;

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
	
	public Java50CleanUp(Map options) {
		super(options);
	}
	
	public Java50CleanUp() {
		super();
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
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		buf.append("class E {\n"); //$NON-NLS-1$
		buf.append("    /**\n"); //$NON-NLS-1$
		buf.append("     * @deprecated\n"); //$NON-NLS-1$
		buf.append("     */\n"); //$NON-NLS-1$
		if (isFlag(ADD_DEPRECATED_ANNOTATION)) {
			buf.append("    @Deprecated\n"); //$NON-NLS-1$
		}
		buf.append("    public void foo() {}\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("class ESub extends E {\n"); //$NON-NLS-1$
		if (isFlag(ADD_OVERRIDE_ANNOATION)) {
			buf.append("    @Override\n"); //$NON-NLS-1$
		}
		buf.append("    public void foo() {}\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		
		return buf.toString();
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
	
	/**
	 * {@inheritDoc}
	 */
	public int getDefaultFlag() {
		return DEFAULT_FLAG;
	}

	protected int createFlag(Map options) {
    	int result= 0;
    	
    	if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.ADD_MISSING_ANNOTATIONS))) {
    		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE))) {
    			result|= ADD_OVERRIDE_ANNOATION;
    		}
    		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED))) {
    			result|= ADD_DEPRECATED_ANNOTATION;
    		}
    	}
    	
	    return result;
    }

	
}
