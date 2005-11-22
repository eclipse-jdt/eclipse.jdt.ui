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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.Java50Fix;
import org.eclipse.jdt.internal.corext.fix.Java50Fix.AnnotationTuple;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Create fixes which can transform pre Java50 code to Java50 code
 * @see org.eclipse.jdt.internal.corext.fix.Java50Fix
 *
 */
public class Java50MultiFix extends AbstractMultiFix {

	private static final String ADD_DEPRICATED_ANNOTATION_SETTINGS_ID= "AddDepricatedAnnotation"; //$NON-NLS-1$
	private static final String ADD_OVERRIDE_ANNOTATION_SETTINGS_ID= "AddOverrideAnnotation"; //$NON-NLS-1$
	
	private boolean fAddOverrideAnnotation;
	private boolean fAddDepricatedAnnotation;

	public Java50MultiFix(boolean addOverrideAnnotation, boolean addDepricatedAnnotation) {
		init(	addOverrideAnnotation,
				addDepricatedAnnotation);
	}

	public Java50MultiFix(IDialogSettings settings) {
		if (settings.get(ADD_OVERRIDE_ANNOTATION_SETTINGS_ID) == null) {
			settings.put(ADD_OVERRIDE_ANNOTATION_SETTINGS_ID, true);
		}
		if (settings.get(ADD_DEPRICATED_ANNOTATION_SETTINGS_ID) == null) {
			settings.put(ADD_DEPRICATED_ANNOTATION_SETTINGS_ID, true);
		}
		init(	settings.getBoolean(ADD_OVERRIDE_ANNOTATION_SETTINGS_ID), 
				settings.getBoolean(ADD_DEPRICATED_ANNOTATION_SETTINGS_ID));
	}

	private void init(boolean addOverrideAnnotation, boolean addDepricatedAnnotation) {
		fAddOverrideAnnotation= addOverrideAnnotation;
		fAddDepricatedAnnotation= addDepricatedAnnotation;
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;
		
		if (!fAddOverrideAnnotation && !fAddDepricatedAnnotation)
			return null;
		
		List/*<AnnotationTuple>*/ annotationTuples= new ArrayList();
		IProblem[] problems= compilationUnit.getProblems();
		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= getProblemLocation(problems[i]);
			
			if (Java50Fix.isMissingDeprecated(problem) || Java50Fix.isMissingOverride(problem)) {				
				
				ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
				if (selectedNode != null) { 
				
					ASTNode declaringNode= Java50Fix.getDeclaringNode(selectedNode);
					if (declaringNode instanceof BodyDeclaration) {
					
						List/*<String>*/ annotations= new ArrayList();
						
						Java50Fix.addAnnotations(problem, fAddOverrideAnnotation, fAddDepricatedAnnotation, annotations);
						
						if (!annotations.isEmpty()) {
							BodyDeclaration declaration= (BodyDeclaration) declaringNode;
							AnnotationTuple tuple= new AnnotationTuple(declaration, (String[])annotations.toArray(new String[annotations.size()]));
							annotationTuples.add(tuple);
						}
						
					}
				}
			}
		}
		if (annotationTuples.isEmpty()) 
			return null;
		
		return new Java50Fix("", cu, (AnnotationTuple[])annotationTuples.toArray(new AnnotationTuple[annotationTuples.size()])); //$NON-NLS-1$
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (fAddOverrideAnnotation) {
			options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.WARNING);
		}
		if (fAddDepricatedAnnotation) {
			options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.WARNING);
		}
		return options;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		Button addOverrid= new Button(composite, SWT.CHECK);
		addOverrid.setText(MultiFixMessages.Java50MultiFix_AddMissingOverride_description);
		addOverrid.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		addOverrid.setSelection(fAddOverrideAnnotation);
		addOverrid.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAddOverrideAnnotation= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button addDepricated= new Button(composite, SWT.CHECK);
		addDepricated.setText(MultiFixMessages.Java50MultiFix_AddMissingDeprecated_description);
		addDepricated.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		addDepricated.setSelection(fAddDepricatedAnnotation);
		addDepricated.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAddDepricatedAnnotation= ((Button)e.getSource()).getSelection();
			}
		});
		
		return composite;
	}

	public void saveSettings(IDialogSettings settings) {
		settings.put(ADD_OVERRIDE_ANNOTATION_SETTINGS_ID, fAddOverrideAnnotation);
		settings.put(ADD_DEPRICATED_ANNOTATION_SETTINGS_ID, fAddDepricatedAnnotation);
	}

}
