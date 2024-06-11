/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lukas Hanke <hanke@yatta.de> - [templates][content assist] Content assist for 'for' loop should suggest member variables - https://bugs.eclipse.org/117215
 *     Microsoft Corporation - [templates][content assist] - Extract the UI related code - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.template.java.TemplateUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * This class provides general functions to test the for loop template based completion.
 */
public abstract class AbstractForLoopJavaContextTest {

	private static final String PROJECT= "NewForLoopJavaContextTest";

	private static final String SRC= "src";

	private static final String CU_NAME= "A.java";

	private static final String CU_PREFIX= """
		package test;
		
		import java.io.Serializable;
		import java.util.Collection;
		import java.util.List;
		
		public class A<E extends Number> {
		""";

	private static final String CU_POSTFIX= """
		 {
		\t
		}
		}
		""";

	private IJavaProject fProject;

	private ICompilationUnit fCU;

	@Before
	public void setUp() throws Exception {
		if (JavaCore.getPlugin() != null) {
			Hashtable<String, String> options= JavaCore.getDefaultOptions();
			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
			//			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_LENGTH, "4");
			//			options.put(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "4");
			JavaCore.setOptions(options);
		}
		setUpProject(JavaCore.VERSION_1_5);
	}

	private void setUpProject(String sourceLevel) throws CoreException, JavaModelException {
		fProject= JavaProjectHelper.createJavaProject(PROJECT, "bin");
		JavaProjectHelper.addRTJar(fProject);
		fProject.setOption(JavaCore.COMPILER_SOURCE, sourceLevel);
		IPackageFragmentRoot fragmentRoot= JavaProjectHelper.addSourceContainer(fProject, SRC);
		IPackageFragment fragment= fragmentRoot.createPackageFragment("test", true, new NullProgressMonitor());
		fCU= fragment.createCompilationUnit(CU_NAME, "", true, new NullProgressMonitor());
		fCU.becomeWorkingCopy(null);
	}

	@After
	public void tearDown() throws Exception {
		fCU.discardWorkingCopy();
		JavaProjectHelper.delete(fProject);
		if (JavaCore.getPlugin() != null) {
			JavaCore.setOptions(JavaCore.getDefaultOptions());
		}
	}

	protected Template getTemplate(String id) {
		TemplateStore store= JavaPlugin.getDefault().getTemplateStore();
		return store.getTemplateData(id).getTemplate();
	}

	/**
	 * Load the template using {@link AbstractForLoopJavaContextTest#getTemplate(String)} with the
	 * template's id.
	 *
	 * @return the loaded {@link Template} instance.
	 */
	protected abstract Template getForLoop();

	protected String evaluateTemplateInMethod(String signature) throws BadLocationException, TemplateException, CoreException {
		String prefix= CU_PREFIX + getInnerClasses();
		fCU.getBuffer().setContents(prefix + signature + CU_POSTFIX);
		int offset= prefix.length() + signature.length() + 3;
		fCU.reconcile(ICompilationUnit.NO_AST, false, null, null);
		return TemplateUtils.evaluateTemplate(getForLoop(), fCU, offset);
	}

	/**
	 * Helper which should return all inner class declarations which are used in the tests as
	 * String.
	 *
	 * @return all inner class declarations
	 */
	protected abstract String getInnerClasses();

	protected String evaluateTemplateInMethodWithField(String signature, String fieldDeclaration) throws BadLocationException, TemplateException, CoreException {
		StringBuilder buf= new StringBuilder();
		buf.append(fieldDeclaration);
		buf.append(";\n");
		buf.append(signature);
		return evaluateTemplateInMethod(buf.toString());
	}

}
