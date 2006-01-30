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
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.Java50Fix;
import org.eclipse.jdt.internal.corext.fix.Java50Fix.ISerialVersionFixContext;
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
	 * Convert for loops to enhanced for loops.<p>
	 * i.e.:<pre><code>
	 * for (int i = 0; i < array.length; i++) {} -> for (int element : array) {}</code></pre>
	 */
	public static final int CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP= 4;
	
	/**
	 * Adds type parameters to raw type references.<p>
	 * i.e.:<pre><code>
	 * List l; -> List<Object> l;
	 */
	public static final int ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE= 8;
	
	/**
	 * Adds a generated serial version id to subtypes of
	 * java.io.Serializable and java.io.Externalizable
	 * 
	 * public class E implements Serializable {}
	 * ->
	 * public class E implements Serializable {
	 * 		private static final long serialVersionUID = 4381024239L;
	 * }
	 */
	public static final int ADD_CALCULATED_SERIAL_VERSION_ID= 16;
	
	/**
	 * Adds a default serial version it to subtypes of
	 * java.io.Serializable and java.io.Externalizable
	 * 
	 * public class E implements Serializable {}
	 * ->
	 * public class E implements Serializable {
	 * 		private static final long serialVersionUID = 1L;
	 * }
	 */
	public static final int ADD_DEFAULT_SERIAL_VERSION_ID= 32;
	
	private static final int DEFAULT_FLAG= ADD_DEPRECATED_ANNOTATION | ADD_OVERRIDE_ANNOATION;
	private static final String SECTION_NAME= "CleanUp_Java50"; //$NON-NLS-1$

	private ISerialVersionFixContext fContext;

	public Java50CleanUp(int flag) {
		super(flag);
	}

	public Java50CleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		Assert.isTrue(!(isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) && isFlag(ADD_DEFAULT_SERIAL_VERSION_ID)));
		
		return Java50Fix.createCleanUp(compilationUnit, 
				isFlag(ADD_OVERRIDE_ANNOATION), 
				isFlag(ADD_DEPRECATED_ANNOTATION),
				isFlag(CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP), 
				isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE),
				isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || isFlag(ADD_DEFAULT_SERIAL_VERSION_ID), getContext());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;

		Assert.isTrue(!(isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) && isFlag(ADD_DEFAULT_SERIAL_VERSION_ID)));
		
		return Java50Fix.createCleanUp(compilationUnit, problems,
				isFlag(ADD_OVERRIDE_ANNOATION), 
				isFlag(ADD_DEPRECATED_ANNOTATION),
				isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE),
				isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || isFlag(ADD_DEFAULT_SERIAL_VERSION_ID), getContext());
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (isFlag(ADD_OVERRIDE_ANNOATION))
			options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.WARNING);
		
		if (isFlag(ADD_DEPRECATED_ANNOTATION))
			options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.WARNING);
		
		if (isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE))
			options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || isFlag(ADD_DEFAULT_SERIAL_VERSION_ID))
			options.put(JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION, JavaCore.WARNING);
		
		return options;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		addCheckBox(composite, ADD_OVERRIDE_ANNOATION, MultiFixMessages.Java50MultiFix_AddMissingOverride_description);
		addCheckBox(composite, ADD_DEPRECATED_ANNOTATION, MultiFixMessages.Java50MultiFix_AddMissingDeprecated_description);
		addCheckBox(composite, CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP, MultiFixMessages.Java50CleanUp_ConvertToEnhancedForLoop_description);
		addCheckBox(composite, ADD_CALCULATED_SERIAL_VERSION_ID, MultiFixMessages.SerialVersionCleanUp_Generated_description);
		
		return composite;
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
			result.add(removeMemonic(MultiFixMessages.Java50MultiFix_AddMissingOverride_description));
		if (isFlag(ADD_DEPRECATED_ANNOTATION))
			result.add(removeMemonic(MultiFixMessages.Java50MultiFix_AddMissingDeprecated_description));
		if (isFlag(CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP))
			result.add(removeMemonic(MultiFixMessages.Java50CleanUp_ConvertToEnhancedForLoop_description));
		if (isFlag(ADD_TYPE_PARAMETERS_TO_RAW_TYPE_REFERENCE))
			result.add(removeMemonic(MultiFixMessages.Java50CleanUp_AddTypeParameters_description));
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID))
			result.add(removeMemonic(MultiFixMessages.SerialVersionCleanUp_Generated_description));
		if (isFlag(ADD_DEFAULT_SERIAL_VERSION_ID))
			result.add("Add default serial version ID");
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canCleanUp(IJavaProject project) {
		return JavaModelUtil.is50OrHigher(project);
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
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || isFlag(ADD_DEFAULT_SERIAL_VERSION_ID)) {
			Java50Fix[] fix= Java50Fix.createMissingSerialVersionFixes(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void beginCleanUp(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		super.beginCleanUp(project, compilationUnits, monitor);
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID))
			fContext= Java50Fix.createSerialVersionHashContext(project, compilationUnits, monitor);
	}

	/**
	 * {@inheritDoc}
	 */
	public void endCleanUp() throws CoreException {
		super.endCleanUp();
		fContext= null;
	}
	
	private ISerialVersionFixContext getContext() {
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID)) {
			return fContext;
		} else if (isFlag(ADD_DEFAULT_SERIAL_VERSION_ID)){
			return new ISerialVersionFixContext() {
				public long getSerialVersionId(String qualifiedName) throws CoreException {
					return 1;
				}
			};
		}
		return null;
	}

}
