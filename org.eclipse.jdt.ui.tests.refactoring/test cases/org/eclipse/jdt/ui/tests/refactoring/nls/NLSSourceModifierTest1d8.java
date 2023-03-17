/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertEquals;

import java.util.Hashtable;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Document;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.nls.AccessorClassCreator;
import org.eclipse.jdt.internal.corext.refactoring.nls.AccessorClassModifier;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSourceModifier;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class NLSSourceModifierTest1d8 {
	@Rule
	public ProjectTestSetup pts= new Java1d8ProjectTestSetup();

    private IJavaProject javaProject;

    private IPackageFragmentRoot fSourceFolder;

    @Before
	public void setUp() throws Exception {
    	Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		options.put(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		options.put(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		options.put(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

        javaProject = pts.getProject();
        fSourceFolder = JavaProjectHelper.addSourceContainer(javaProject, "src");
    }

    @After
	public void tearDown() throws Exception {
        JavaProjectHelper.clear(javaProject, pts.getDefaultClasspath());
    }

	@Test
	public void fromSkippedToTranslated() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\";\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("key.");
        nlsSubstitutions[0].generateKey(nlsSubstitutions, new Properties());

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", false);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=Accessor.getString(\"key.0\"); //$NON-NLS-1$\n" +
            	"}\n",
            	doc.get());
    }

	@Test
	public void fromSkippedToTranslatedEclipseNew() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\";\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("key_");
        nlsSubstitutions[0].generateKey(nlsSubstitutions, new Properties());

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=Accessor.key_0;\n" +
            	"}\n",
            	doc.get());

      CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, defaultSubst, null);
      String accessor= accessorChange.getPreview();
      StringBuilder buf= new StringBuilder();
      buf.append("package test;\n");
      buf.append("\n");
      buf.append("import org.eclipse.osgi.util.NLS;\n");
      buf.append("\n");
      buf.append("public class Accessor extends NLS {\n");
      buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
      buf.append("    public static String key_0;\n");
      buf.append("    static {\n");
      buf.append("        // initialize resource bundle\n");
      buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
      buf.append("    }\n");
      buf.append("    private Accessor() {\n");
      buf.append("    }\n");
      buf.append("}\n");
      String expected= buf.toString();
      StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	@Test
	public void fromSkippedToNotTranslated() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\";\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setPrefix("key.");
        nlsSubstitutions[0].setState(NLSSubstitution.IGNORED);

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", false);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
                "}\n",
            	doc.get());
    }

	@Test
	public void fromSkippedToNotTranslatedEclipse() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\";\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setPrefix("key_");
        nlsSubstitutions[0].setState(NLSSubstitution.IGNORED);

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
                "}\n",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, NLSRefactoring.DEFAULT_SUBST_PATTERN, null);
        String accessor= accessorChange.getPreview();
        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

    /*
     * TODO: the key should be 0
     */
	@Test
	public void fromNotTranslatedToTranslated() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("key.");
        nlsSubstitutions[0].generateKey(nlsSubstitutions, new Properties());


        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", false);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=Accessor.getString(\"key.0\"); //$NON-NLS-1$\n" +
            	"}\n",
            	doc.get());
    }

	@Test
	public void fromNotTranslatedToTranslatedEclipse() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("key_");
        nlsSubstitutions[0].generateKey(nlsSubstitutions, new Properties());


        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=Accessor.key_0; \n" +
            	"}\n",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, NLSRefactoring.DEFAULT_SUBST_PATTERN, null);

        String accessor= accessorChange.getPreview();
        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    public static String key_0;\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	@Test
	public void fromNotTranslatedToSkipped() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setPrefix("key.");
        nlsSubstitutions[0].setState(NLSSubstitution.INTERNALIZED);

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", false);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=\"whatever\"; \n" +
                "}\n",
            	doc.get());
    }

	@Test
	public void fromNotTranslatedToSkippedEclipse() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setPrefix("key_");
        nlsSubstitutions[0].setState(NLSSubstitution.INTERNALIZED);

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=\"whatever\"; \n" +
                "}\n",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, NLSRefactoring.DEFAULT_SUBST_PATTERN, null);
        String accessor= accessorChange.getPreview();
        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	private NLSSubstitution[] getSubstitutions(ICompilationUnit cu, CompilationUnit astRoot) {
		NLSHint hint= new NLSHint(cu, astRoot);
		return hint.getSubstitutions();
	}

	@Test
	public void fromTranslatedToNotTranslated() throws Exception {

        String klazz =
            "package test;\n" +
            "public class Test {\n" +
            "	private String str=Accessor.getString(\"key.0\"); //$NON-NLS-1$\n" +
            "}\n";

        String accessorKlazz =
            "package test;\n" +
    		"public class Accessor {\n" +
    		"	private static final String BUNDLE_NAME = \"test.test\";//$NON-NLS-1$\n" +
    		"	public static String getString(String s) {\n" +
    		"		return \"\";\n" +
    		"	}\n" +
    		"}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setValue("whatever");
        nlsSubstitutions[0].setPrefix("key.");
        nlsSubstitutions[0].setState(NLSSubstitution.IGNORED);

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", false);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "package test;\n" +
                "public class Test {\n" +
                "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
                "}\n",
            	doc.get());
    }

	@Test
	public void fromTranslatedToNotTranslatedEclipse() throws Exception {

        String klazz =
            "package test;\n" +
            "public class Test {\n" +
            "	private String str=Accessor.k_0;\n" +
            "}\n";

        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    public static String k_0;\n");
        buf.append("}\n");
        String accessorKlazz= buf.toString();

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit accessorCu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment nlspack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
        nlspack.createCompilationUnit("NLS.java", "public class NLS {}", false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setValue("whatever");
        nlsSubstitutions[0].setPrefix("k_");
        nlsSubstitutions[0].setState(NLSSubstitution.IGNORED);

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "package test;\n" +
                "public class Test {\n" +
                "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
                "}\n",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void fromTranslatedToSkipped() throws Exception {

        String klazz =
            "package test;\n" +
            "public class Test {\n" +
            "	private String str=Accessor.getString(\"key.0\"); //$NON-NLS-1$\n" +
            "}\n";

        String accessorKlazz =
            "package test;\n" +
    		"public class Accessor {\n" +
    		"	private static final String BUNDLE_NAME = \"test.test\";//$NON-NLS-1$\n" +
    		"	public static String getString(String s) {\n" +
    		"		return \"\";\n" +
    		"	}\n" +
    		"}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setValue("whatever");
        nlsSubstitutions[0].setState(NLSSubstitution.INTERNALIZED);
        nlsSubstitutions[0].setPrefix("key.");

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", false);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "package test;\n" +
                "public class Test {\n" +
                "	private String str=\"whatever\"; \n" +
                "}\n",
            	doc.get());
    }

	@Test
	public void fromTranslatedToSkippedEclipse() throws Exception {

        String klazz =
            "package test;\n" +
            "public class Test {\n" +
            "	private String str=Accessor.key_0;\n" +
            "}\n";

        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    public static String key_0;\n");
        buf.append("}\n");
        String accessorKlazz= buf.toString();

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit accessorCu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment nlspack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
        nlspack.createCompilationUnit("NLS.java", "public class NLS {}", false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setValue("whatever");
        nlsSubstitutions[0].setState(NLSSubstitution.INTERNALIZED);
        nlsSubstitutions[0].setPrefix("key_");

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "package test;\n" +
                "public class Test {\n" +
                "	private String str=\"whatever\";\n" +
                "}\n",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void replacementOfKey() throws Exception {
        String klazz =
            "package test;\n" +
            "public class Test {\n" +
            "	private String str=Accessor.getString(\"key.0\"); //$NON-NLS-1$\n" +
            "}\n";

        String accessorKlazz =
            "package test;\n" +
    		"public class Accessor {\n" +
    		"	private static final String BUNDLE_NAME = \"test.test\";//$NON-NLS-1$\n" +
    		"	public static String getString(String s) {\n" +
    		"		return \"\";\n" +
    		"	}\n" +
    		"}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setPrefix("key.");
        nlsSubstitutions[0].setKey("nls.0");

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", false);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "package test;\n" +
                "public class Test {\n" +
                "	private String str=Accessor.getString(\"nls.0\"); //$NON-NLS-1$\n" +
                "}\n",
            	doc.get());
    }

	@Test
	public void replacementOfKeyEclipse() throws Exception {
        String klazz =
            "package test;\n" +
            "public class Test {\n" +
            "	private String str=Accessor.key_0; \n" +
            "}\n";

        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    public static String key_0;\n");
        buf.append("}\n");
        String accessorKlazz= buf.toString();

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit accessorCu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment nlspack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
        nlspack.createCompilationUnit("NLS.java", "public class NLS {}", false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setInitialValue("whatever");
        nlsSubstitutions[0].setValue("whatever");
        nlsSubstitutions[0].setPrefix("nls_");
        nlsSubstitutions[0].setKey("nls_0");

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "package test;\n" +
                "public class Test {\n" +
                "	private String str=Accessor.nls_0; \n" +
                "}\n",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    public static String nls_0;\n");
        buf.append("}\n");
        String expected= buf.toString();

        assertEquals(expected, accessorDoc.get());
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=223865
	@Test
	public void replacementOfKeysBug223865() throws Exception {

		String klazz=
			"package test;\n" +
			"public class Test {\n" +
			"	private String str=Accessor.key_0;\n" +
			"	private String str=Accessor.key_1;\n" +
			"}\n";

		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
		buf.append("\n");
		buf.append("    private Accessor() {\n");
		buf.append("    }\n");
		buf.append("    static {\n");
		buf.append("        // initialize resource bundle\n");
		buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
		buf.append("    }\n");
		buf.append("    public static String key_0;\n");
		buf.append("    public static String key_1;\n");
		buf.append("}\n");
		String accessorKlazz= buf.toString();

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		ICompilationUnit accessorCu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

		IPackageFragment nlspack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
		nlspack.createCompilationUnit("NLS.java", "public class NLS {}", false, null);

		CompilationUnit astRoot= createAST(cu);
		NLSSubstitution[] nlsSubstitutions= getSubstitutions(cu, astRoot);
		nlsSubstitutions[0].setValue("whatever");
		nlsSubstitutions[0].setInitialValue("whatever");
		nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
		nlsSubstitutions[0].setPrefix("key_");
		nlsSubstitutions[0].setKey("key_0");
		nlsSubstitutions[1].setValue("whatever");
		nlsSubstitutions[1].setInitialValue("whatever");
		nlsSubstitutions[1].setState(NLSSubstitution.EXTERNALIZED);
		nlsSubstitutions[1].setPrefix("key_");
		nlsSubstitutions[1].setKey("key_0");

		String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
		TextChange change= (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

		Document doc= new Document(klazz);
		change.getEdit().apply(doc);

		assertEquals(
				"package test;\n" +
				"public class Test {\n" +
				"	private String str=Accessor.key_0;\n" +
				"	private String str=Accessor.key_0;\n" +
				"}\n", doc.get());

		TextChange accessorChange= (TextChange) AccessorClassModifier.create(accessorCu, nlsSubstitutions);
		Document accessorDoc= new Document(accessorKlazz);
		accessorChange.getEdit().apply(accessorDoc);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
		buf.append("\n");
		buf.append("    private Accessor() {\n");
		buf.append("    }\n");
		buf.append("    static {\n");
		buf.append("        // initialize resource bundle\n");
		buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
		buf.append("    }\n");
		buf.append("    public static String key_0;\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEquals(expected, accessorDoc.get());
	}

	@Test
	public void bug95708_1() throws Exception {
        String klazz =
            "public class Test {\n" +
            "	private String str1=\"whatever\";\n" +
            "	private String str2=\"whatever\";\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("key_");
        nlsSubstitutions[0].setKey("0");
        nlsSubstitutions[1].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[1].setPrefix("key_");
        nlsSubstitutions[1].setKey("0");

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str1=Accessor.key_0;\n" +
                "	private String str2=Accessor.key_0;\n" +
                "}\n",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, NLSRefactoring.DEFAULT_SUBST_PATTERN, null);
        String accessor= accessorChange.getPreview();
        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    public static String key_0;\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	@Test
	public void bug95708_2() throws Exception {
        String klazz =
            "public class Test {\n" +
            "	private String str1=Accessor.key_0;\n" +
            "	private String str2=Accessor.key_0;\n" +
            "}\n";

        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    public static String key_0;\n");
        buf.append("}\n");
        String accessorKlazz= buf.toString();

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit accessorCu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment nlspack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
        nlspack.createCompilationUnit("NLS.java", "public class NLS {}", false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setInitialValue("whatever");
        nlsSubstitutions[0].setValue("whatever");
        nlsSubstitutions[0].setPrefix("nls_");
        nlsSubstitutions[0].setKey("nls_0");

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str1=Accessor.nls_0;\n" +
                "	private String str2=Accessor.key_0;\n" +
                "}\n",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("    public static String key_0;\n");
        buf.append("    public static String nls_0;\n");
        buf.append("}\n");
        String expected= buf.toString();

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void insertionOrder1() throws Exception {
         String klazz =
             "public class Test {\n" +
             "	private String str1=Accessor.key_b;\n" +
             "	private String str2=Accessor.key_y;\n" +
             "	private String str3=\"h\";\n" +
             "	private String str4=\"a\";\n" +
             "	private String str5=\"z\";\n" +
             "}\n";

         StringBuilder buf= new StringBuilder();
         buf.append("package test;\n");
         buf.append("import org.eclipse.osgi.util.NLS;\n");
         buf.append("public class Accessor extends NLS {\n");
         buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
         buf.append("    private Accessor() {\n");
         buf.append("    }\n");
         buf.append("    public static String key_b;\n");
         buf.append("    public static String key_y;\n");
         buf.append("    static {\n");
         buf.append("        // initialize resource bundle\n");
         buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
         buf.append("    }\n");
         buf.append("}\n");
         String accessorKlazz= buf.toString();

         IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
         ICompilationUnit accessorCu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
         ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

         IPackageFragment nlspack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
         nlspack.createCompilationUnit("NLS.java", "public class NLS {}", false, null);

         CompilationUnit astRoot= createAST(cu);
         NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
         nlsSubstitutions[4].setInitialValue("b");
         nlsSubstitutions[3].setInitialValue("y");
         nlsSubstitutions[2].setState(NLSSubstitution.EXTERNALIZED);
         nlsSubstitutions[2].setPrefix("key_");
         nlsSubstitutions[2].setKey("z");
         nlsSubstitutions[1].setState(NLSSubstitution.EXTERNALIZED);
         nlsSubstitutions[1].setPrefix("key_");
         nlsSubstitutions[1].setKey("a");
         nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
         nlsSubstitutions[0].setPrefix("key_");
         nlsSubstitutions[0].setKey("h");

         String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
         TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

         Document doc = new Document(klazz);
         change.getEdit().apply(doc);

         assertEquals(
                 "public class Test {\n" +
                 "	private String str1=Accessor.key_b;\n" +
                 "	private String str2=Accessor.key_y;\n" +
                 "	private String str3=Accessor.key_h;\n" +
                 "	private String str4=Accessor.key_a;\n" +
                 "	private String str5=Accessor.key_z;\n" +
                 "}\n",
             	doc.get());

         TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
         Document accessorDoc= new Document(accessorKlazz);
         accessorChange.getEdit().apply(accessorDoc);

         buf= new StringBuilder();
         buf.append("package test;\n");
         buf.append("import org.eclipse.osgi.util.NLS;\n");
         buf.append("public class Accessor extends NLS {\n");
         buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
         buf.append("    private Accessor() {\n");
         buf.append("    }\n");
         buf.append("    public static String key_a;\n");
         buf.append("    public static String key_b;\n");
         buf.append("    public static String key_h;\n");
         buf.append("    public static String key_y;\n");
         buf.append("    public static String key_z;\n");
         buf.append("    static {\n");
         buf.append("        // initialize resource bundle\n");
         buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
         buf.append("    }\n");
         buf.append("}\n");
         String expected= buf.toString();

         assertEquals(expected, accessorDoc.get());
     }

	@Test
	public void insertionOrder2() throws Exception {
        String klazz =
            "public class Test {\n" +
            "	private String str1=Accessor.key_b;\n" +
            "	private String str2=Accessor.key_y;\n" +
            "	private String str3=\"h\";\n" +
            "	private String str4=\"a\";\n" +
            "	private String str5=\"z\";\n" +
            "}\n";

        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    public static String key_b;\n");
        buf.append("    public static String key_y;\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        String accessorKlazz= buf.toString();

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit accessorCu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment nlspack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
        nlspack.createCompilationUnit("NLS.java", "public class NLS {}", false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[4].setInitialValue("b");
        nlsSubstitutions[4].setKey("key_i");
        nlsSubstitutions[3].setInitialValue("y");
        nlsSubstitutions[3].setKey("key_g");
        nlsSubstitutions[2].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[2].setPrefix("key_");
        nlsSubstitutions[2].setKey("z");
        nlsSubstitutions[1].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[1].setPrefix("key_");
        nlsSubstitutions[1].setKey("a");
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("key_");
        nlsSubstitutions[0].setKey("h");

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str1=Accessor.key_g;\n" +
                "	private String str2=Accessor.key_i;\n" +
                "	private String str3=Accessor.key_h;\n" +
                "	private String str4=Accessor.key_a;\n" +
                "	private String str5=Accessor.key_z;\n" +
                "}\n",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    public static String key_a;\n");
        buf.append("    public static String key_g;\n");
        buf.append("    public static String key_h;\n");
        buf.append("    public static String key_i;\n");
        buf.append("    public static String key_z;\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void insertionOrder3() throws Exception {
        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("public class Test {\n");
        buf.append("    private String str1= Accessor.Test_A_1;\n");
        buf.append("    private String str2= Accessor.Test_B_1;\n");
        buf.append("    private String str3= \"str3\";\n");
        buf.append("}\n");
        String klazz= buf.toString();

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("\n");
        buf.append("    public static String Test_B_1;\n");
        buf.append("\n");
        buf.append("    public static String Test_A_1;\n");
        buf.append("\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        String accessorKlazz= buf.toString();

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit accessorCu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment nlspack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
        nlspack.createCompilationUnit("NLS.java", "public class NLS {}", false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[2].setInitialValue("str2");
        nlsSubstitutions[2].setKey("Test_B_1");
        nlsSubstitutions[1].setInitialValue("str1");
        nlsSubstitutions[1].setKey("Test_A_1");
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("Test_");
        nlsSubstitutions[0].setKey("B_2");

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("public class Test {\n");
        buf.append("    private String str1= Accessor.Test_A_1;\n");
        buf.append("    private String str2= Accessor.Test_B_1;\n");
        buf.append("    private String str3= Accessor.Test_B_2;\n");
        buf.append("}\n");
        String expectedKlazz= buf.toString();

        assertEquals(expectedKlazz, doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("\n");
        buf.append("    public static String Test_B_1;\n");
        buf.append("\n");
        buf.append("    public static String Test_B_2;\n");
        buf.append("\n");
        buf.append("    public static String Test_A_1;\n");
        buf.append("\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void insertionOrder4() throws Exception {
        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("public class Test {\n");
        buf.append("    private String str1= Accessor.Test_A_a;\n");
        buf.append("    private String str2= Accessor.Test_A_b;\n");
        buf.append("    private String str3= \"str3\";\n");
        buf.append("    private String str4= \"str4\";\n");
        buf.append("    private String str5= Accessor.Test_B_b;\n");
        buf.append("    private String str6= Accessor.Test_B_c;\n");
        buf.append("}\n");
        String klazz= buf.toString();

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("\n");
        buf.append("    public static String Test_A_a;\n");
        buf.append("    public static String Test_A_b;\n");
        buf.append("\n");
        buf.append("    public static String Test_B_b;\n");
        buf.append("    public static String Test_B_c;\n");
        buf.append("\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        String accessorKlazz= buf.toString();

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit accessorCu= pack.createCompilationUnit("Accessor.java", accessorKlazz, false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment nlspack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
        nlspack.createCompilationUnit("NLS.java", "public class NLS {}", false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("Test_");
        nlsSubstitutions[0].setKey("A_z");
        nlsSubstitutions[1].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[1].setPrefix("Test_");
        nlsSubstitutions[1].setKey("B_a");
        nlsSubstitutions[2].setInitialValue("str1");
        nlsSubstitutions[2].setKey("Test_A_a");
        nlsSubstitutions[3].setInitialValue("str2");
        nlsSubstitutions[3].setKey("Test_A_b");
        nlsSubstitutions[4].setInitialValue("str5");
        nlsSubstitutions[4].setKey("Test_B_b");
        nlsSubstitutions[5].setInitialValue("str6");
        nlsSubstitutions[5].setKey("Test_B_c");

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("public class Test {\n");
        buf.append("    private String str1= Accessor.Test_A_a;\n");
        buf.append("    private String str2= Accessor.Test_A_b;\n");
        buf.append("    private String str3= Accessor.Test_A_z;\n");
        buf.append("    private String str4= Accessor.Test_B_a;\n");
        buf.append("    private String str5= Accessor.Test_B_b;\n");
        buf.append("    private String str6= Accessor.Test_B_c;\n");
        buf.append("}\n");
        String expectedKlazz= buf.toString();

        assertEquals(expectedKlazz, doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("import org.eclipse.osgi.util.NLS;\n");
        buf.append("public class Accessor extends NLS {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("\n");
        buf.append("    public static String Test_A_a;\n");
        buf.append("    public static String Test_A_b;\n");
        buf.append("    public static String Test_A_z;\n");
        buf.append("\n");
        buf.append("    public static String Test_B_a;\n");
        buf.append("    public static String Test_B_b;\n");
        buf.append("    public static String Test_B_c;\n");
        buf.append("\n");
        buf.append("    static {\n");
        buf.append("        // initialize resource bundle\n");
        buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void bug131323() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\";\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("key.");
        nlsSubstitutions[0].generateKey(nlsSubstitutions, new Properties());

        String subpattern= "getFoo(${key})";
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, subpattern, pack, "Accessor", false);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=Accessor.getFoo(\"key.0\"); //$NON-NLS-1$\n" +
            	"}\n",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), false, nlsSubstitutions, subpattern, null);
        String accessor= accessorChange.getPreview();
        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("\n");
        buf.append("import java.util.MissingResourceException;\n");
        buf.append("import java.util.ResourceBundle;\n");
        buf.append("\n");
        buf.append("public class Accessor {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle\n");
        buf.append("            .getBundle(BUNDLE_NAME);\n");
        buf.append("\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    public static String getFoo(String key) {\n");
        buf.append("        try {\n");
        buf.append("            return RESOURCE_BUNDLE.getString(key);\n");
        buf.append("        } catch (MissingResourceException e) {\n");
        buf.append("            return '!' + key + '!';\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
        String expected= buf.toString();
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	@Test
	public void checkBundleNameWhenResourceAndAccessorAreInDifferentPackages() throws Exception {

		String klazz= "public class Test {\n" +
				"	private String str=\"whatever\";\n" +
				"}\n";

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

		CompilationUnit astRoot= createAST(cu);
		NLSSubstitution[] nlsSubstitutions= getSubstitutions(cu, astRoot);
		nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
		nlsSubstitutions[0].setPrefix("key.");
		nlsSubstitutions[0].generateKey(nlsSubstitutions, new Properties());

		String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
		TextChange change= (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", false);

		Document doc= new Document(klazz);
		change.getEdit().apply(doc);

		assertEquals(
				"public class Test {\n" +
						"	private String str=Accessor.getString(\"key.0\"); //$NON-NLS-1$\n" +
						"}\n",
				doc.get());
		IPackageFragment resourcePackage= fSourceFolder.createPackageFragment("test.messages", false, null);
		CreateTextFileChange accessorChange= (CreateTextFileChange) AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack,
				resourcePackage.getPath().append("test.properties"), false, nlsSubstitutions, defaultSubst, null);
		String accessor= accessorChange.getPreview();
        StringBuilder buf= new StringBuilder();
        buf.append("package test;\n");
        buf.append("\n");
        buf.append("import java.util.MissingResourceException;\n");
        buf.append("import java.util.ResourceBundle;\n");
        buf.append("\n");
        buf.append("public class Accessor {\n");
        buf.append("    private static final String BUNDLE_NAME = \"test.messages.test\"; //$NON-NLS-1$\n");
        buf.append("\n");
        buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle\n");
        buf.append("            .getBundle(BUNDLE_NAME);\n");
        buf.append("\n");
        buf.append("    private Accessor() {\n");
        buf.append("    }\n");
        buf.append("    public static String getString(String key) {\n");
        buf.append("        try {\n");
        buf.append("            return RESOURCE_BUNDLE.getString(key);\n");
        buf.append("        } catch (MissingResourceException e) {\n");
        buf.append("            return '!' + key + '!';\n");
        buf.append("        }\n");
        buf.append("    }\n");
        buf.append("}\n");
		String expected= buf.toString();
		StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
	}

	@Test
	public void checkBundleNameWhenResourceAndAccessorAreInDifferentPackagesEclipse() throws Exception {

        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\";\n" +
            "}\n";

        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        CompilationUnit astRoot= createAST(cu);
        NLSSubstitution[] nlsSubstitutions = getSubstitutions(cu, astRoot);
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].setPrefix("key_");
        nlsSubstitutions[0].generateKey(nlsSubstitutions, new Properties());

        String defaultSubst= NLSRefactoring.DEFAULT_SUBST_PATTERN;
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, defaultSubst, pack, "Accessor", true);

        Document doc = new Document(klazz);
        change.getEdit().apply(doc);

        assertEquals(
                "public class Test {\n" +
                "	private String str=Accessor.key_0;\n" +
            	"}\n",
            	doc.get());
      IPackageFragment resourcePackage = fSourceFolder.createPackageFragment("test.messages", false, null);
      CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, resourcePackage.getPath().append("test.properties"), true, nlsSubstitutions, defaultSubst, null);
      String accessor= accessorChange.getPreview();
      StringBuilder buf= new StringBuilder();
      buf.append("package test;\n");
      buf.append("\n");
      buf.append("import org.eclipse.osgi.util.NLS;\n");
      buf.append("\n");
      buf.append("public class Accessor extends NLS {\n");
      buf.append("    private static final String BUNDLE_NAME = \"test.messages.test\"; //$NON-NLS-1$\n");
      buf.append("    public static String key_0;\n");
      buf.append("    static {\n");
      buf.append("        // initialize resource bundle\n");
      buf.append("        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);\n");
      buf.append("    }\n");
      buf.append("    private Accessor() {\n");
      buf.append("    }\n");
      buf.append("}\n");
      String expected= buf.toString();
      StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	private CompilationUnit createAST(ICompilationUnit cu) {
		return ASTCreator.createAST(cu, null);
	}
}
