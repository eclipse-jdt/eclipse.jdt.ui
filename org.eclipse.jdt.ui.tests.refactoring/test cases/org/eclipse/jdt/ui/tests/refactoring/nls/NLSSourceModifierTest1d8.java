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
            """
			public class Test {
				private String str="whatever";
			}
			""";

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
                """
					public class Test {
						private String str=Accessor.getString("key.0"); //$NON-NLS-1$
					}
					""",
            	doc.get());
    }

	@Test
	public void fromSkippedToTranslatedEclipseNew() throws Exception {

        String klazz =
            """
			public class Test {
				private String str="whatever";
			}
			""";

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
                """
					public class Test {
						private String str=Accessor.key_0;
					}
					""",
            	doc.get());

      CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, defaultSubst, null);
      String accessor= accessorChange.getPreview();
      String expected= """
		package test;
		
		import org.eclipse.osgi.util.NLS;
		
		public class Accessor extends NLS {
		    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
		    public static String key_0;
		    static {
		        // initialize resource bundle
		        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
		    }
		    private Accessor() {
		    }
		}
		""";
      StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	@Test
	public void fromSkippedToNotTranslated() throws Exception {

        String klazz =
            """
			public class Test {
				private String str="whatever";
			}
			""";

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
                """
					public class Test {
						private String str="whatever"; //$NON-NLS-1$
					}
					""",
            	doc.get());
    }

	@Test
	public void fromSkippedToNotTranslatedEclipse() throws Exception {

        String klazz =
            """
			public class Test {
				private String str="whatever";
			}
			""";

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
                """
					public class Test {
						private String str="whatever"; //$NON-NLS-1$
					}
					""",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, NLSRefactoring.DEFAULT_SUBST_PATTERN, null);
        String accessor= accessorChange.getPreview();
        String expected= """
			package test;
			
			import org.eclipse.osgi.util.NLS;
			
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			}
			""";
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

    /*
     * TODO: the key should be 0
     */
	@Test
	public void fromNotTranslatedToTranslated() throws Exception {

        String klazz =
            """
			public class Test {
				private String str="whatever"; //$NON-NLS-1$
			}
			""";

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
                """
					public class Test {
						private String str=Accessor.getString("key.0"); //$NON-NLS-1$
					}
					""",
            	doc.get());
    }

	@Test
	public void fromNotTranslatedToTranslatedEclipse() throws Exception {

        String klazz =
            """
			public class Test {
				private String str="whatever"; //$NON-NLS-1$
			}
			""";

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
                """
					public class Test {
						private String str=Accessor.key_0;\s
					}
					""",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, NLSRefactoring.DEFAULT_SUBST_PATTERN, null);

        String accessor= accessorChange.getPreview();
        String expected= """
			package test;
			
			import org.eclipse.osgi.util.NLS;
			
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    public static String key_0;
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			}
			""";
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	@Test
	public void fromNotTranslatedToSkipped() throws Exception {

        String klazz =
            """
			public class Test {
				private String str="whatever"; //$NON-NLS-1$
			}
			""";

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
                """
					public class Test {
						private String str="whatever";\s
					}
					""",
            	doc.get());
    }

	@Test
	public void fromNotTranslatedToSkippedEclipse() throws Exception {

        String klazz =
            """
			public class Test {
				private String str="whatever"; //$NON-NLS-1$
			}
			""";

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
                """
					public class Test {
						private String str="whatever";\s
					}
					""",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, NLSRefactoring.DEFAULT_SUBST_PATTERN, null);
        String accessor= accessorChange.getPreview();
        String expected= """
			package test;
			
			import org.eclipse.osgi.util.NLS;
			
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			}
			""";
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	private NLSSubstitution[] getSubstitutions(ICompilationUnit cu, CompilationUnit astRoot) {
		NLSHint hint= new NLSHint(cu, astRoot);
		return hint.getSubstitutions();
	}

	@Test
	public void fromTranslatedToNotTranslated() throws Exception {

        String klazz =
            """
			package test;
			public class Test {
				private String str=Accessor.getString("key.0"); //$NON-NLS-1$
			}
			""";

        String accessorKlazz =
            """
			package test;
			public class Accessor {
				private static final String BUNDLE_NAME = "test.test";//$NON-NLS-1$
				public static String getString(String s) {
					return "";
				}
			}
			""";

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
                """
					package test;
					public class Test {
						private String str="whatever"; //$NON-NLS-1$
					}
					""",
            	doc.get());
    }

	@Test
	public void fromTranslatedToNotTranslatedEclipse() throws Exception {

        String klazz =
            """
			package test;
			public class Test {
				private String str=Accessor.k_0;
			}
			""";

        String accessorKlazz= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    public static String k_0;
			}
			""";

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
                """
					package test;
					public class Test {
						private String str="whatever"; //$NON-NLS-1$
					}
					""",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        String expected= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void fromTranslatedToSkipped() throws Exception {

        String klazz =
            """
			package test;
			public class Test {
				private String str=Accessor.getString("key.0"); //$NON-NLS-1$
			}
			""";

        String accessorKlazz =
            """
			package test;
			public class Accessor {
				private static final String BUNDLE_NAME = "test.test";//$NON-NLS-1$
				public static String getString(String s) {
					return "";
				}
			}
			""";

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
                """
					package test;
					public class Test {
						private String str="whatever";\s
					}
					""",
            	doc.get());
    }

	@Test
	public void fromTranslatedToSkippedEclipse() throws Exception {

        String klazz =
            """
			package test;
			public class Test {
				private String str=Accessor.key_0;
			}
			""";

        String accessorKlazz= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    public static String key_0;
			}
			""";

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
                """
					package test;
					public class Test {
						private String str="whatever";
					}
					""",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        String expected= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void replacementOfKey() throws Exception {
        String klazz =
            """
			package test;
			public class Test {
				private String str=Accessor.getString("key.0"); //$NON-NLS-1$
			}
			""";

        String accessorKlazz =
            """
			package test;
			public class Accessor {
				private static final String BUNDLE_NAME = "test.test";//$NON-NLS-1$
				public static String getString(String s) {
					return "";
				}
			}
			""";

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
                """
					package test;
					public class Test {
						private String str=Accessor.getString("nls.0"); //$NON-NLS-1$
					}
					""",
            	doc.get());
    }

	@Test
	public void replacementOfKeyEclipse() throws Exception {
        String klazz =
            """
			package test;
			public class Test {
				private String str=Accessor.key_0;\s
			}
			""";

        String accessorKlazz= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    public static String key_0;
			}
			""";

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
                """
					package test;
					public class Test {
						private String str=Accessor.nls_0;\s
					}
					""",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        String expected= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    public static String nls_0;
			}
			""";

        assertEquals(expected, accessorDoc.get());
    }

    //https://bugs.eclipse.org/bugs/show_bug.cgi?id=223865
	@Test
	public void replacementOfKeysBug223865() throws Exception {

		String klazz=
			"""
			package test;
			public class Test {
				private String str=Accessor.key_0;
				private String str=Accessor.key_1;
			}
			""";

		String accessorKlazz= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    public static String key_0;
			    public static String key_1;
			}
			""";

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
				"""
					package test;
					public class Test {
						private String str=Accessor.key_0;
						private String str=Accessor.key_0;
					}
					""", doc.get());

		TextChange accessorChange= (TextChange) AccessorClassModifier.create(accessorCu, nlsSubstitutions);
		Document accessorDoc= new Document(accessorKlazz);
		accessorChange.getEdit().apply(accessorDoc);

		String expected= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    public static String key_0;
			}
			""";

		assertEquals(expected, accessorDoc.get());
	}

	@Test
	public void bug95708_1() throws Exception {
        String klazz =
            """
			public class Test {
				private String str1="whatever";
				private String str2="whatever";
			}
			""";

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
                """
					public class Test {
						private String str1=Accessor.key_0;
						private String str2=Accessor.key_0;
					}
					""",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), true, nlsSubstitutions, NLSRefactoring.DEFAULT_SUBST_PATTERN, null);
        String accessor= accessorChange.getPreview();
        String expected= """
			package test;
			
			import org.eclipse.osgi.util.NLS;
			
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    public static String key_0;
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			}
			""";
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	@Test
	public void bug95708_2() throws Exception {
        String klazz =
            """
			public class Test {
				private String str1=Accessor.key_0;
				private String str2=Accessor.key_0;
			}
			""";

        String accessorKlazz= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    public static String key_0;
			}
			""";

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
                """
					public class Test {
						private String str1=Accessor.nls_0;
						private String str2=Accessor.key_0;
					}
					""",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        String expected= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    public static String key_0;
			    public static String nls_0;
			}
			""";

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void insertionOrder1() throws Exception {
         String klazz =
             """
			public class Test {
				private String str1=Accessor.key_b;
				private String str2=Accessor.key_y;
				private String str3="h";
				private String str4="a";
				private String str5="z";
			}
			""";

         String accessorKlazz= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			    public static String key_b;
			    public static String key_y;
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

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
                 """
					public class Test {
						private String str1=Accessor.key_b;
						private String str2=Accessor.key_y;
						private String str3=Accessor.key_h;
						private String str4=Accessor.key_a;
						private String str5=Accessor.key_z;
					}
					""",
             	doc.get());

         TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
         Document accessorDoc= new Document(accessorKlazz);
         accessorChange.getEdit().apply(accessorDoc);

         String expected= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			    public static String key_a;
			    public static String key_b;
			    public static String key_h;
			    public static String key_y;
			    public static String key_z;
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

         assertEquals(expected, accessorDoc.get());
     }

	@Test
	public void insertionOrder2() throws Exception {
        String klazz =
            """
			public class Test {
				private String str1=Accessor.key_b;
				private String str2=Accessor.key_y;
				private String str3="h";
				private String str4="a";
				private String str5="z";
			}
			""";

        String accessorKlazz= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			    public static String key_b;
			    public static String key_y;
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

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
                """
					public class Test {
						private String str1=Accessor.key_g;
						private String str2=Accessor.key_i;
						private String str3=Accessor.key_h;
						private String str4=Accessor.key_a;
						private String str5=Accessor.key_z;
					}
					""",
            	doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        String expected= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			    public static String key_a;
			    public static String key_g;
			    public static String key_h;
			    public static String key_i;
			    public static String key_z;
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void insertionOrder3() throws Exception {
        String klazz= """
			package test;
			public class Test {
			    private String str1= Accessor.Test_A_1;
			    private String str2= Accessor.Test_B_1;
			    private String str3= "str3";
			}
			""";

        String accessorKlazz= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			
			    public static String Test_B_1;
			
			    public static String Test_A_1;
			
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

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

        String expectedKlazz= """
			package test;
			public class Test {
			    private String str1= Accessor.Test_A_1;
			    private String str2= Accessor.Test_B_1;
			    private String str3= Accessor.Test_B_2;
			}
			""";

        assertEquals(expectedKlazz, doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        String expected= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			
			    public static String Test_B_1;
			
			    public static String Test_B_2;
			
			    public static String Test_A_1;
			
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void insertionOrder4() throws Exception {
        String klazz= """
			package test;
			public class Test {
			    private String str1= Accessor.Test_A_a;
			    private String str2= Accessor.Test_A_b;
			    private String str3= "str3";
			    private String str4= "str4";
			    private String str5= Accessor.Test_B_b;
			    private String str6= Accessor.Test_B_c;
			}
			""";

        String accessorKlazz= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			
			    public static String Test_A_a;
			    public static String Test_A_b;
			
			    public static String Test_B_b;
			    public static String Test_B_c;
			
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

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

        String expectedKlazz= """
			package test;
			public class Test {
			    private String str1= Accessor.Test_A_a;
			    private String str2= Accessor.Test_A_b;
			    private String str3= Accessor.Test_A_z;
			    private String str4= Accessor.Test_B_a;
			    private String str5= Accessor.Test_B_b;
			    private String str6= Accessor.Test_B_c;
			}
			""";

        assertEquals(expectedKlazz, doc.get());

        TextChange accessorChange= (TextChange)AccessorClassModifier.create(accessorCu, nlsSubstitutions);
        Document accessorDoc= new Document(accessorKlazz);
        accessorChange.getEdit().apply(accessorDoc);

        String expected= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			    private Accessor() {
			    }
			
			    public static String Test_A_a;
			    public static String Test_A_b;
			    public static String Test_A_z;
			
			    public static String Test_B_a;
			    public static String Test_B_b;
			    public static String Test_B_c;
			
			    static {
			        // initialize resource bundle
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			}
			""";

        assertEquals(expected, accessorDoc.get());
    }

	@Test
	public void bug131323() throws Exception {

        String klazz =
            """
			public class Test {
				private String str="whatever";
			}
			""";

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
                """
					public class Test {
						private String str=Accessor.getFoo("key.0"); //$NON-NLS-1$
					}
					""",
            	doc.get());

        CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, pack.getPath().append("test.properties"), false, nlsSubstitutions, subpattern, null);
        String accessor= accessorChange.getPreview();
        String expected= """
			package test;
			
			import java.util.MissingResourceException;
			import java.util.ResourceBundle;
			
			public class Accessor {
			    private static final String BUNDLE_NAME = "test.test"; //$NON-NLS-1$
			
			    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			            .getBundle(BUNDLE_NAME);
			
			    private Accessor() {
			    }
			    public static String getFoo(String key) {
			        try {
			            return RESOURCE_BUNDLE.getString(key);
			        } catch (MissingResourceException e) {
			            return '!' + key + '!';
			        }
			    }
			}
			""";
        StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	@Test
	public void checkBundleNameWhenResourceAndAccessorAreInDifferentPackages() throws Exception {

		String klazz= """
			public class Test {
				private String str="whatever";
			}
			""";

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
				"""
					public class Test {
						private String str=Accessor.getString("key.0"); //$NON-NLS-1$
					}
					""",
				doc.get());
		IPackageFragment resourcePackage= fSourceFolder.createPackageFragment("test.messages", false, null);
		CreateTextFileChange accessorChange= (CreateTextFileChange) AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack,
				resourcePackage.getPath().append("test.properties"), false, nlsSubstitutions, defaultSubst, null);
		String accessor= accessorChange.getPreview();
        String expected= """
			package test;
			
			import java.util.MissingResourceException;
			import java.util.ResourceBundle;
			
			public class Accessor {
			    private static final String BUNDLE_NAME = "test.messages.test"; //$NON-NLS-1$
			
			    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			            .getBundle(BUNDLE_NAME);
			
			    private Accessor() {
			    }
			    public static String getString(String key) {
			        try {
			            return RESOURCE_BUNDLE.getString(key);
			        } catch (MissingResourceException e) {
			            return '!' + key + '!';
			        }
			    }
			}
			""";
		StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
	}

	@Test
	public void checkBundleNameWhenResourceAndAccessorAreInDifferentPackagesEclipse() throws Exception {

        String klazz =
            """
			public class Test {
				private String str="whatever";
			}
			""";

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
                """
					public class Test {
						private String str=Accessor.key_0;
					}
					""",
            	doc.get());
      IPackageFragment resourcePackage = fSourceFolder.createPackageFragment("test.messages", false, null);
      CreateTextFileChange accessorChange= (CreateTextFileChange)AccessorClassCreator.create(cu, "Accessor", pack.getPath().append("Accessor.java"), pack, resourcePackage.getPath().append("test.properties"), true, nlsSubstitutions, defaultSubst, null);
      String accessor= accessorChange.getPreview();
      String expected= """
		package test;
		
		import org.eclipse.osgi.util.NLS;
		
		public class Accessor extends NLS {
		    private static final String BUNDLE_NAME = "test.messages.test"; //$NON-NLS-1$
		    public static String key_0;
		    static {
		        // initialize resource bundle
		        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
		    }
		    private Accessor() {
		    }
		}
		""";
      StringAsserts.assertEqualStringIgnoreDelim(accessor, expected);
    }

	private CompilationUnit createAST(ICompilationUnit cu) {
		return ASTCreator.createAST(cu, null);
	}
}
