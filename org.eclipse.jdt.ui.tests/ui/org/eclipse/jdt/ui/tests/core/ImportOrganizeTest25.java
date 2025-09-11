/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java25ProjectTestSetup;

public class ImportOrganizeTest25 extends CoreTests {
	@Rule
	public Java25ProjectTestSetup proj= new Java25ProjectTestSetup(true);

	private IJavaProject fJProject1;

	@Before
	public void before() throws Exception {
		fJProject1= proj.getProject();

//		fJProject1.setOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
//		fJProject1.setOption(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, JavaCore.IGNORE);

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_25);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_25);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_25);
		JavaCore.setOptions(options);
	}

	@After
	public void after() throws Exception {
		setOrganizeImportSettings(null, 99, 99, fJProject1);
		JavaProjectHelper.clear(fJProject1, proj.getDefaultClasspath());
	}

	protected IChooseImportQuery createQuery(final String name, final String[] choices, final int[] nEntries) {
		return (openChoices, ranges) -> {
			assertEquals(name + "-query-nchoices1", choices.length, openChoices.length);
			assertEquals(name + "-query-nchoices2", nEntries.length, openChoices.length);
			for (int i1= 0; i1 < nEntries.length; i1++) {
				assertEquals(name + "-query-cnt" + i1, openChoices[i1].length, nEntries[i1]);
			}
			TypeNameMatch[] res= new TypeNameMatch[openChoices.length];
			for (int i2= 0; i2 < openChoices.length; i2++) {
				TypeNameMatch[] selection= openChoices[i2];
				assertNotNull(name + "-query-setset" + i2, selection);
				assertTrue(name + "-query-setlen" + i2, selection.length > 0);
				TypeNameMatch found= null;
				for (TypeNameMatch s : selection) {
					if (s.getFullyQualifiedName().equals(choices[i2])) {
						found= s;
					}
				}
				assertNotNull(name + "-query-notfound" + i2, found);
				res[i2]= found;
			}
			return res;
		};
	}

	protected OrganizeImportsOperation createOperation(ICompilationUnit cu, String[] order, int threshold, boolean ignoreLowerCaseNames, boolean save, boolean allowSyntaxErrors, IChooseImportQuery chooseImportQuery) {
		setOrganizeImportSettings(order, threshold, threshold, cu.getJavaProject());
		return new OrganizeImportsOperation(cu, null, ignoreLowerCaseNames, save, allowSyntaxErrors, chooseImportQuery);
	}

	protected OrganizeImportsOperation createOperation(ICompilationUnit cu, String[] order, int threshold, boolean ignoreLowerCaseNames, boolean save, boolean allowSyntaxErrors, IChooseImportQuery chooseImportQuery, boolean restoreExistingImports) {
		setOrganizeImportSettings(order, threshold, threshold, cu.getJavaProject());
		return new OrganizeImportsOperation(cu, null, ignoreLowerCaseNames, save, allowSyntaxErrors, chooseImportQuery, restoreExistingImports);
	}

	protected void setOrganizeImportSettings(String[] order, int threshold, int staticThreshold, IJavaProject project) {
		IEclipsePreferences scope= new ProjectScope(project.getProject()).getNode(JavaUI.ID_PLUGIN);
		if (order == null) {
			scope.remove(PreferenceConstants.ORGIMPORTS_IMPORTORDER);
			scope.remove(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD);
		} else {
			StringBuilder buf= new StringBuilder();
			for (String o : order) {
				buf.append(o);
				buf.append(';');
			}
			scope.put(PreferenceConstants.ORGIMPORTS_IMPORTORDER, buf.toString());
			scope.put(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD, String.valueOf(threshold));
			scope.put(PreferenceConstants.ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD, String.valueOf(staticThreshold));
		}
	}

	protected void assertImports(ICompilationUnit cu, String[] imports) throws Exception {
		IImportDeclaration[] desc= cu.getImports();
		assertEquals(cu.getElementName() + "-count", imports.length, desc.length);
		for (int i= 0; i < imports.length; i++) {
			String elementName= desc[i].getElementName();
			if (elementName.endsWith(".*") && Flags.isModule(desc[i].getFlags())) {
				elementName= elementName.substring(0, elementName.length() - 2);
			}
			assertEquals(cu.getElementName() + "-cmpentries" + i, imports[i], elementName);
		}
	}

	@Test
	public void testSimpleBaseUse() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			import module java.base;
			class E {
				List<String> a;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		Map<String, String> options= new HashMap<>();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_25);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_25);
		cu.setOptions(options);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("E", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"java.base"
		});
	}

	@Test
	public void testUnneededImportFromBase() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			import java.util.List;
			import module java.base;
			class E {
				List<String> a;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		Map<String, String> options= new HashMap<>();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_25);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_25);
		cu.setOptions(options);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("E", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"java.base"
		});
	}

	@Test
	public void testNeededImportOutsideBase() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack2", false, null);
		String str2= """
			package pack2;
			public class List<T> {
			}
			""";
		pack2.createCompilationUnit("List.java", str2, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			import pack2.List;
			import module java.base;
			class E {
				List<String> a;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		Map<String, String> options= new HashMap<>();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_25);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_25);
		cu.setOptions(options);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("E", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"java.base",
			"pack2.List",
		});
	}

	@Test
	public void testImplicitBase() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			void main() {
				List<String> a;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		Map<String, String> options= new HashMap<>();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_25);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_25);
		cu.setOptions(options);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("E", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
		});
	}

}
