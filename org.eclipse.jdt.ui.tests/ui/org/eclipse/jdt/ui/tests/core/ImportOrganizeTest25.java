/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.Document;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.RequiresDirective;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.tests.core.rules.Java25ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.ImportsCleanUp;

public class ImportOrganizeTest25 extends CoreTests {
	@Rule
	public Java25ProjectTestSetup proj= new Java25ProjectTestSetup();
	@Rule
	public Java25ProjectTestSetup proj2= new Java25ProjectTestSetup("TestSetupProject25_2");
	@Rule
	public Java25ProjectTestSetup proj3= new Java25ProjectTestSetup("TestSetupProject25_3");

	private IJavaProject fJProject1;
	private IJavaProject fJProject2;
	private IJavaProject fJProject3;

	@Before
	public void before() throws Exception {
		fJProject1= proj.getProject();
		fJProject2= proj2.getProject();
		fJProject3= proj3.getProject();

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
		setOrganizeImportSettings(null, 99, 99, fJProject2);
		setOrganizeImportSettings(null, 99, 99, fJProject3);

		JavaProjectHelper.clear(fJProject1, proj.getDefaultClasspath());
		JavaProjectHelper.clear(fJProject2, proj2.getDefaultClasspath());
		JavaProjectHelper.clear(fJProject3, proj3.getDefaultClasspath());
	}

	protected IChooseImportQuery createQuery(final String name, final String[] choices, final int[] nEntries) {
		return (openChoices, _) -> {
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

	@Test
	public void testAmbigousClassNeeded() throws Exception {
		IPackageFragmentRoot sourceFolder2= JavaProjectHelper.addSourceContainer(fJProject2, "src");

		IPackageFragment defaultPkg2= sourceFolder2.createPackageFragment("", false, null);
		String mod2= """
			module mod2 {
				exports pack2;
			}
			""";
		defaultPkg2.createCompilationUnit("module-info.java", mod2, false, null);

		IPackageFragment pack2= sourceFolder2.createPackageFragment("pack2", false, null);
		String code= """
			package pack2;
			public class List<T> {}
			""";
		pack2.createCompilationUnit("List.java", code, false, null);

		String code2= """
			package pack2;
			public class NewList<T> {}
			""";
		pack2.createCompilationUnit("NewList.java", code2, false, null);

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject3, "src");
		IPackageFragment defaultPkg= sourceFolder.createPackageFragment("", false, null);
		String mod= """
			module mod {
			   requires java.base;
		       requires mod2;
		       exports pack1;
		    }
			""";
		defaultPkg.createCompilationUnit("module-info.java", mod, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			import module mod2;
			import module java.base;
			import java.util.List;
			import pack2.NewList;
			public class X {
				List<String> a;
				NewList<String> b;
				ArrayList<String> c;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str, false, null);
		Map<String, String> options= new HashMap<>();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_25);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_25);
		cu.setOptions(options);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("E", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
				"mod2",
				"java.base",
				"java.util.List",
		});
	}

	@Test
	public void testImplicitClass() throws Exception {
		IPackageFragmentRoot sourceFolder2= JavaProjectHelper.addSourceContainer(fJProject2, "src");

		IPackageFragment defaultPkg= sourceFolder2.createPackageFragment("", false, null);
		String code= """
			import java.security.cert.Certificate;
			import java.util.List;

			Certificate certificate;

			public static void main(String[] args) {
				Certificate certificate;
				List<String> list;
			}
			""";
		ICompilationUnit cu= defaultPkg.createCompilationUnit("MyClass.java", code, false, null);

		Map<String, String> options= new HashMap<>();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_25);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_25);
		cu.setOptions(options);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("E", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
				"java.security.cert.Certificate"
		});
	}

	@Test
	public void testCreateTextEditsAddsRequires() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		String originalSource= test.source().getSource();
		String originalModule= test.module().getSource();
		Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.source(), 99, null).createTextEdits(null);
		assertEquals(Set.of(test.source(), test.module()), edits.keySet());
		assertEquals(originalSource, test.source().getSource());
		assertEquals(originalModule, test.module().getSource());
		applyEdits(edits);
		assertImports(test.source(), new String[] { "dep.One" });
		assertModuleRequires(test.module(), "dependency");
		assertFalse(createModuleImportOperation(test.source(), 99, null).createTextEdits(null).containsKey(test.module()));
	}

	@Test
	public void testCreateTextEditCollectsDependenciesWithoutDescriptorEdits() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		String module= test.module().getSource();
		Map<ICompilationUnit, Set<String>> requiredModules= new HashMap<>();
		TextEdit edit= createModuleImportOperation(test.source(), 99, null).createTextEdit(null, requiredModules);
		assertEquals(Map.of(test.module(), Set.of("dependency")), requiredModules);
		JavaModelUtil.applyEdit(test.source(), edit, true, null);
		assertImports(test.source(), new String[] { "dep.One" });
		assertEquals(module, test.module().getSource());
	}

	@Test
	public void testCreateTextEditsForExistingImport() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "import dep.One;", "One one;");
		applyEdits(createModuleImportOperation(test.source(), 99, null).createTextEdits(null));
		assertImports(test.source(), new String[] { "dep.One" });
		assertModuleRequires(test.module(), "dependency");
	}

	@Test
	public void testCreateTextEditsForStaticImport() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "import static dep.One.VALUE;", "int value = VALUE;");
		applyEdits(createModuleImportOperation(test.source(), 99, null).createTextEdits(null));
		assertImports(test.source(), new String[] { "dep.One.VALUE" });
		assertModuleRequires(test.module(), "dependency");
	}

	@Test
	public void testCreateTextEditsForOnDemandImports() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one; Other other;");
		IPackageFragmentRoot dependencySource= fJProject2.getPackageFragmentRoot(fJProject2.getProject().getFolder("src"));
		dependencySource.getPackageFragment("dep").createCompilationUnit("Other.java", "package dep; public class Other {}", false, null);
		applyEdits(createModuleImportOperation(test.source(), 1, null).createTextEdits(null));
		assertImports(test.source(), new String[] { "dep.*" });
		assertModuleRequires(test.module(), "dependency");
	}

	@Test
	public void testCreateTextEditsPreservesExistingRequires() throws Exception {
		String moduleSource= "module client {\n\trequires static dependency;\n}\n";
		ModuleImportTest test= createModuleImportTest(moduleSource, "", "One one;");
		Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.source(), 99, null).createTextEdits(null);
		assertFalse(edits.containsKey(test.module()));
		applyEdits(edits);
		assertImports(test.source(), new String[] { "dep.One" });
		assertEquals(moduleSource, test.module().getSource());
	}

	@Test
	public void testCreateTextEditsRespectsTransitiveRequires() throws Exception {
		String moduleSource= "module client {\n\trequires bridge;\n}\n";
		ModuleImportTest test= createModuleImportTest(moduleSource, "", "One one;");
		IPackageFragmentRoot bridgeSource= JavaProjectHelper.addSourceContainer(fJProject3, "src");
		bridgeSource.createPackageFragment("", false, null).createCompilationUnit("module-info.java",
				"module bridge { requires transitive dependency; }", false, null);
		JavaProjectHelper.addRequiredModularProject(fJProject3, fJProject2);
		JavaProjectHelper.addRequiredModularProject(fJProject1, fJProject3);
		Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.source(), 99, null).createTextEdits(null);
		assertFalse(edits.containsKey(test.module()));
		applyEdits(edits);
		assertImports(test.source(), new String[] { "dep.One" });
		assertEquals(moduleSource, test.module().getSource());
	}

	@Test
	public void testCreateTextEditsWithoutModuleDescriptor() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		test.module().delete(false, null);
		Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.source(), 99, null).createTextEdits(null);
		assertEquals(Set.of(test.source()), edits.keySet());
		applyEdits(edits);
		assertImports(test.source(), new String[] { "dep.One" });
		assertFalse(test.module().exists());
	}

	@Test
	public void testCreateTextEditsUsesChosenImport() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		addAlternativeModule();
		IChooseImportQuery query= createQuery("One", new String[] { "alternative.One" }, new int[] { 2 });
		applyEdits(createModuleImportOperation(test.source(), 99, query).createTextEdits(null));
		assertImports(test.source(), new String[] { "alternative.One" });
		assertModuleRequires(test.module(), "alternative");
	}

	@Test
	public void testCreateTextEditsSkipsUnchosenImport() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		addAlternativeModule();
		IChooseImportQuery query= (choices, _) -> new TypeNameMatch[choices.length];
		Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.source(), 99, query).createTextEdits(null);
		assertFalse(edits.containsKey(test.module()));
		applyEdits(edits);
		assertImports(test.source(), new String[0]);
		assertModuleRequires(test.module());
	}

	@Test
	public void testCreateTextEditsIgnoresJavaBaseAndCurrentModule() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "String value; Local local;");
		IPackageFragmentRoot source= fJProject1.getPackageFragmentRoot(fJProject1.getProject().getFolder("src"));
		source.createPackageFragment("local", false, null).createCompilationUnit("Local.java", "package local; public class Local {}", false, null);
		Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.source(), 99, null).createTextEdits(null);
		assertFalse(edits.containsKey(test.module()));
		applyEdits(edits);
		assertImports(test.source(), new String[] { "local.Local" });
		assertModuleRequires(test.module());
	}

	@Test
	public void testCreateTextEditsForRestoredOnDemandImport() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "import dep.*;", "One one;");
		OrganizeImportsOperation operation= createOperation(test.source(), new String[0], 99, false, true, true, null, true);
		applyEdits(operation.createTextEdits(null));
		assertModuleRequires(test.module(), "dependency");
	}

	@Test
	public void testCreateTextEditsForStaticOnDemandImport() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "import static dep.One.*;", "int value = VALUE;");
		applyEdits(createModuleImportOperation(test.source(), 99, null).createTextEdits(null));
		assertModuleRequires(test.module(), "dependency");
	}

	@Test
	public void testCreateTextEditsAddsSeveralModules() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n\texports client;\n}\n", "import dep.One;", "One one; Other other;");
		addAlternativeModule();
		fJProject3.getPackageFragmentRoot(fJProject3.getProject().getFolder("src")).getPackageFragment("alternative")
				.createCompilationUnit("Other.java", "package alternative; public class Other {}", false, null);
		applyEdits(createModuleImportOperation(test.source(), 99, null).createTextEdits(null));
		assertImports(test.source(), new String[] { "alternative.Other", "dep.One" });
		assertModuleRequires(test.module(), "alternative", "dependency");
		assertTrue(test.module().getSource().contains("exports client;"));
	}

	@Test
	public void testCreateTextEditsForModuleDescriptor() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n\tuses One;\n}\n", "", "");
		Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.module(), 99, null).createTextEdits(null);
		assertEquals(Set.of(test.module()), edits.keySet());
		applyEdits(edits);
		assertImports(test.module(), new String[] { "dep.One" });
		assertModuleRequires(test.module(), "dependency");
		assertTrue(test.module().getSource().contains("uses One;"));
	}

	@Test
	public void testCreateTextEditsDoesNotRequireClasspathDependency() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		JavaProjectHelper.removeFromClasspath(fJProject1, fJProject2.getPath());
		JavaProjectHelper.addRequiredProject(fJProject1, fJProject2);
		Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.source(), 99, null).createTextEdits(null);
		assertFalse(edits.containsKey(test.module()));
	}

	@Test
	public void testCreateTextEditRemainsSourceOnly() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		String moduleSource= test.module().getSource();
		TextEdit edit= createModuleImportOperation(test.source(), 99, null).createTextEdit(null);
		JavaModelUtil.applyEdit(test.source(), edit, true, null);
		assertImports(test.source(), new String[] { "dep.One" });
		assertEquals(moduleSource, test.module().getSource());
	}

	@Test
	public void testCreateTextEditsUsesUnsavedModuleDescriptor() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		test.module().becomeWorkingCopy(null);
		try {
			String unsavedModule= "module client {\n\texports client;\n}\n";
			test.module().getBuffer().setContents(unsavedModule);
			Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.source(), 99, null).createTextEdits(null);
			TextEdit moduleEdit= edits.get(test.module());
			assertNotNull(moduleEdit);
			Document document= new Document(unsavedModule);
			moduleEdit.apply(document);
			assertTrue(document.get().contains("requires dependency;"));
			assertTrue(document.get().contains("exports client;"));
			assertEquals(unsavedModule, test.module().getSource());
			assertTrue(test.module().hasUnsavedChanges());
		} finally {
			test.module().discardWorkingCopy();
		}
	}

	@Test
	public void testCreateTextEditsDoesNotRequireUnexportedPackage() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		ICompilationUnit dependencyModule= fJProject2.getModuleDescription().getCompilationUnit();
		dependencyModule.getBuffer().setContents("module dependency {}\n");
		dependencyModule.save(null, true);
		Map<ICompilationUnit, TextEdit> edits= createModuleImportOperation(test.source(), 99, null).createTextEdits(null);
		assertFalse(edits.containsKey(test.module()));
	}

	@Test
	public void testCreateTextEditsCanceledChoice() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		addAlternativeModule();
		String originalSource= test.source().getSource();
		String originalModule= test.module().getSource();
		OrganizeImportsOperation operation= createModuleImportOperation(test.source(), 99, (_, _) -> null);
		assertThrows(OperationCanceledException.class, () -> operation.createTextEdits(null));
		assertEquals(originalSource, test.source().getSource());
		assertEquals(originalModule, test.module().getSource());
	}

	@Test
	public void testCreateTextEditsForJava9() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		JavaProjectHelper.set9CompilerOptions(fJProject1);
		JavaProjectHelper.set9CompilerOptions(fJProject2);
		applyEdits(createModuleImportOperation(test.source(), 99, null).createTextEdits(null));
		assertImports(test.source(), new String[] { "dep.One" });
		assertModuleRequires(test.module(), "dependency");
	}

	private record ModuleImportTest(ICompilationUnit source, ICompilationUnit module) { }

	@Test
	public void testRunAddsRequiresAndUndoesBothFiles() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		String source= test.source().getSource();
		String module= test.module().getSource();
		var undoManager= RefactoringCore.getUndoManager();
		undoManager.flush();
		try {
			OrganizeImportsOperation operation= createModuleImportOperation(test.source(), 99, null);
			assertEquals(test.source().getJavaProject().getProject(), operation.getScheduleRule());
			assertTrue(operation.getScheduleRule().contains(test.module().getResource()));
			ResourcesPlugin.getWorkspace().run(operation, operation.getScheduleRule(), IWorkspace.AVOID_UPDATE, null);
			assertImports(test.source(), new String[] { "dep.One" });
			assertModuleRequires(test.module(), "dependency");
			assertTrue(undoManager.anythingToUndo());
			undoManager.performUndo(null, new NullProgressMonitor());
			assertEquals(source, test.source().getSource());
			assertEquals(module, test.module().getSource());
		} finally {
			undoManager.flush();
		}
	}

	@Test
	public void testRunPreservesDirtyModuleDescriptor() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "", "One one;");
		test.source().becomeWorkingCopy(null);
		test.module().becomeWorkingCopy(null);
		try {
			test.module().getBuffer().setContents("module client {\n\texports client;\n}\n");
			createOperation(test.source(), new String[0], 99, false, false, true, null).run(null);
			assertImports(test.source(), new String[] { "dep.One" });
			assertModuleRequires(test.module(), "dependency");
			assertTrue(test.module().getSource().contains("exports client;"));
			assertTrue(test.module().hasUnsavedChanges());
			assertTrue(test.source().hasUnsavedChanges());
		} finally {
			RefactoringCore.getUndoManager().flush();
			test.module().discardWorkingCopy();
			test.source().discardWorkingCopy();
		}
	}

	@Test
	public void testOrganizeSelectionAggregatesRequiresAndUndoesAllFiles() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n\tuses One;\n}\n", "", "One one;");
		IPackageFragment pack= (IPackageFragment) test.source().getParent();
		ICompilationUnit second= pack.createCompilationUnit("Second.java", "package client;\nimport dep.One;\npublic class Second { One one; }\n", false, null);
		String source= test.source().getSource();
		String module= test.module().getSource();
		String secondSource= second.getSource();
		Change undo= organizeSelection(test.source(), second, test.module());
		assertImports(test.source(), new String[] { "dep.One" });
		assertImports(second, new String[] { "dep.One" });
		assertImports(test.module(), new String[] { "dep.One" });
		assertModuleRequires(test.module(), "dependency");
		assertTrue(test.module().getSource().contains("uses One;"));
		assertNotNull(undo);
		undo.perform(new NullProgressMonitor());
		assertEquals(source, test.source().getSource());
		assertEquals(module, test.module().getSource());
		assertEquals(secondSource, second.getSource());
	}

	@Test
	public void testOrganizeSelectionAddsMultipleRequires() throws Exception {
		ModuleImportTest test= createModuleImportTest("module client {\n}\n", "import dep.One;", "One one;");
		addAlternativeModule();
		IPackageFragment pack= (IPackageFragment) test.source().getParent();
		ICompilationUnit second= pack.createCompilationUnit("Second.java", "package client;\nimport alternative.One;\npublic class Second { One one; }\n", false, null);
		organizeSelection(test.source(), second);
		assertImports(test.source(), new String[] { "dep.One" });
		assertImports(second, new String[] { "alternative.One" });
		assertModuleRequires(test.module(), "alternative", "dependency");
	}

	private static Change organizeSelection(ICompilationUnit... units) throws Exception {
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		for (ICompilationUnit unit : units) {
			refactoring.addCompilationUnit(unit);
		}
		refactoring.addCleanUp(new ImportsCleanUp(Map.of(CleanUpConstants.ORGANIZE_IMPORTS, CleanUpOptions.TRUE)));
		CreateChangeOperation create= new CreateChangeOperation(new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS), RefactoringStatus.FATAL);
		PerformChangeOperation perform= new PerformChangeOperation(create);
		perform.run(new NullProgressMonitor());
		assertFalse(create.getConditionCheckingStatus().toString(), create.getConditionCheckingStatus().hasFatalError());
		assertTrue(perform.changeExecuted());
		return perform.getUndoChange();
	}

	private ModuleImportTest createModuleImportTest(String moduleSource, String imports, String body) throws Exception {
		IPackageFragmentRoot dependencySource= JavaProjectHelper.addSourceContainer(fJProject2, "src");
		dependencySource.createPackageFragment("", false, null).createCompilationUnit("module-info.java",
				"module dependency { exports dep; }", false, null);
		dependencySource.createPackageFragment("dep", false, null).createCompilationUnit("One.java",
				"package dep; public class One { public static final int VALUE = 1; }", false, null);
		JavaProjectHelper.addRequiredModularProject(fJProject1, fJProject2);
		IPackageFragmentRoot source= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		ICompilationUnit module= source.createPackageFragment("", false, null).createCompilationUnit("module-info.java", moduleSource, false, null);
		ICompilationUnit cu= source.createPackageFragment("client", false, null).createCompilationUnit("Client.java",
				"package client;\n" + imports + "\npublic class Client { " + body + " }\n", false, null);
		return new ModuleImportTest(cu, module);
	}

	private void addAlternativeModule() throws Exception {
		IPackageFragmentRoot source= JavaProjectHelper.addSourceContainer(fJProject3, "src");
		source.createPackageFragment("", false, null).createCompilationUnit("module-info.java",
				"module alternative { exports alternative; }", false, null);
		source.createPackageFragment("alternative", false, null).createCompilationUnit("One.java",
				"package alternative; public class One {}", false, null);
		JavaProjectHelper.addRequiredModularProject(fJProject1, fJProject3);
	}

	private OrganizeImportsOperation createModuleImportOperation(ICompilationUnit source, int threshold, IChooseImportQuery query) {
		return createOperation(source, new String[0], threshold, false, true, true, query);
	}

	private static void applyEdits(Map<ICompilationUnit, TextEdit> edits) throws Exception {
		for (Map.Entry<ICompilationUnit, TextEdit> edit : edits.entrySet()) {
			JavaModelUtil.applyEdit(edit.getKey(), edit.getValue(), true, null);
		}
	}

	private static void assertModuleRequires(ICompilationUnit module, String... expected) throws Exception {
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(module);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
		List<ModuleDirective> directives= astRoot.getModule().moduleStatements();
		String[] requires= directives.stream().filter(RequiresDirective.class::isInstance).map(RequiresDirective.class::cast)
				.map(directive -> directive.getName().getFullyQualifiedName()).toArray(String[]::new);
		assertArrayEquals(expected, requires);
	}

}
