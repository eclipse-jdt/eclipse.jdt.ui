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
import java.util.Random;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.PotentialProgrammingProblemsFix;
import org.eclipse.jdt.internal.corext.fix.PotentialProgrammingProblemsFix.ISerialVersionFixContext;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class PotentialProgrammingProblemsCleanUp extends AbstractCleanUp {
	
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
	public static final int ADD_CALCULATED_SERIAL_VERSION_ID= 1;
	
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
	public static final int ADD_DEFAULT_SERIAL_VERSION_ID= 2;
	
	/**
	 * Adds a default serial version it to subtypes of
	 * java.io.Serializable and java.io.Externalizable
	 * 
	 * public class E implements Serializable {}
	 * ->
	 * public class E implements Serializable {
	 * 		private static final long serialVersionUID = 84504L;
	 * }
	 */
	public static final int ADD_RANDOM_SERIAL_VERSION_ID= 4;

	private static final int DEFAULT_FLAG= 0;
	private static final String SECTION_NAME= "CleanUp_PotentialProgrammingProblems"; //$NON-NLS-1$
	private static final Random RANDOM_NUMBER_GENERATOR= new Random();

	private ISerialVersionFixContext fContext;

	public PotentialProgrammingProblemsCleanUp(int flag) {
		super(flag);
	}

	public PotentialProgrammingProblemsCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return PotentialProgrammingProblemsFix.createCleanUp(compilationUnit,
				isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || 
				isFlag(ADD_DEFAULT_SERIAL_VERSION_ID) ||
				isFlag(ADD_RANDOM_SERIAL_VERSION_ID), getContext());
	}

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return PotentialProgrammingProblemsFix.createCleanUp(compilationUnit, problems, 
				isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || 
				isFlag(ADD_DEFAULT_SERIAL_VERSION_ID) ||
				isFlag(ADD_RANDOM_SERIAL_VERSION_ID), getContext());
	}

	/**
	 * {@inheritDoc}
	 */
	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || 
				isFlag(ADD_DEFAULT_SERIAL_VERSION_ID)  ||
				isFlag(ADD_RANDOM_SERIAL_VERSION_ID))
			options.put(JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION, JavaCore.WARNING);
		return options;
	}

	/**
	 * {@inheritDoc}
	 */
	public Control createConfigurationControl(Composite parent, IJavaProject project) {

		Button button= new Button(parent, SWT.CHECK);
		button.setText(MultiFixMessages.PotentialProgrammingProblemsCleanUp_AddSerialId_section_name);
		button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		Composite sub= new Composite(parent, SWT.NONE);
		sub.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		sub.setLayout(new GridLayout(2, false));
		
		final Button button1= addRadioButton(sub, ADD_CALCULATED_SERIAL_VERSION_ID, MultiFixMessages.PotentialProgrammingProblemsCleanUp_Generated_radioButton_name); 
		final Button button2= addRadioButton(sub, ADD_RANDOM_SERIAL_VERSION_ID, MultiFixMessages.PotentialProgrammingProblemsCleanUp_Random_radioButton_name);
		
		if (!isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) && !isFlag(ADD_RANDOM_SERIAL_VERSION_ID)) {
			button.setSelection(false);
			button1.setSelection(true);
			button1.setEnabled(false);
			button2.setEnabled(false);
		} else {
			button.setSelection(true);
		}
		
		button.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				boolean isSelected= ((Button)e.getSource()).getSelection();
				button1.setEnabled(isSelected);
				button2.setEnabled(isSelected);
				if (!isSelected) {
					setFlag(ADD_CALCULATED_SERIAL_VERSION_ID, false);
					setFlag(ADD_RANDOM_SERIAL_VERSION_ID, false);
				} else {
					setFlag(ADD_CALCULATED_SERIAL_VERSION_ID, button1.getSelection());
					setFlag(ADD_RANDOM_SERIAL_VERSION_ID, button2.getSelection());
				}
			}
			
		});
		
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
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID))
			result.add(MultiFixMessages.SerialVersionCleanUp_Generated_description);
		if (isFlag(ADD_DEFAULT_SERIAL_VERSION_ID))
			result.add(MultiFixMessages.CodeStyleCleanUp_addDefaultSerialVersionId_description);
		if (isFlag(ADD_RANDOM_SERIAL_VERSION_ID))
			result.add(MultiFixMessages.PotentialProgrammingProblemsCleanUp_RandomSerialId_description);
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || isFlag(ADD_DEFAULT_SERIAL_VERSION_ID)) {
			IFix[] fix= PotentialProgrammingProblemsFix.createMissingSerialVersionFixes(compilationUnit, problem);
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
			fContext= PotentialProgrammingProblemsFix.createSerialVersionHashContext(project, compilationUnits, monitor);
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
		} else if (isFlag(ADD_RANDOM_SERIAL_VERSION_ID)) {
			return new ISerialVersionFixContext() {
				public long getSerialVersionId(String qualifiedName) throws CoreException {
					return RANDOM_NUMBER_GENERATOR.nextLong();
				}
				
			};
		}
		return null;
	}

}
