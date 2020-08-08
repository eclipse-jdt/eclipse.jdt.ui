/*******************************************************************************
 * Copyright (c) 2020 GK Software SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.hover;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;

import org.eclipse.jdt.ui.tests.core.CoreTests;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;

public class JavadocHoverTests extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();
		JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	protected ICompilationUnit getWorkingCopy(String path, String source, WorkingCopyOwner owner) throws JavaModelException {
		ICompilationUnit workingCopy= (ICompilationUnit) JavaCore.create(getFile(path));
		if (owner != null)
			workingCopy= workingCopy.getWorkingCopy(owner, null/*no progress monitor*/);
		else
			workingCopy.becomeWorkingCopy(null/*no progress monitor*/);
		workingCopy.getBuffer().setContents(source);
		workingCopy.makeConsistent(null/*no progress monitor*/);
		return workingCopy;
	}

	protected IFile getFile(String path) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		return root.getFile(new Path(path));
	}
	@Test
	public void testValueTag() throws Exception {
		String source=
				"package p;\n" +
				"public class TestClass {\n" +
				"  /**\n" +
				"   * The value of this constant is {@value}.\n" +
				"   */\n" +
				"  public static final String SCRIPT_START = \"<script>\";\n" +
				"  /**\n" +
				"   * Evaluates the script starting with {@value TestClass#SCRIPT_START}.\n" +
				"   */\n" +
				"  public void test1() {\n" +
				"  }\n" +
				"  /**\n" +
				"   * Evaluates the script starting with {@value #SCRIPT_START}.\n" +
				"   */\n" +
				"  public void test2() {\n" +
				"  }\n" +
				"}\n";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/TestClass.java", source, null);
		assertNotNull("TestClass.java", cu);

		IType type= cu.getType("TestClass");
		// check javadoc on each member:
		for (IJavaElement member : type.getChildren()) {
			IJavaElement[] elements= { member };
			ISourceRange range= ((ISourceReference) member).getNameRange();
			JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(elements, cu, new Region(range.getOffset(), range.getLength()), null);
			String actualHtmlContent= hoverInfo.getHtml();

			// value should be expanded:
			assertTrue(actualHtmlContent, actualHtmlContent.contains("&lt;script&gt;"));
		}
	}
}
