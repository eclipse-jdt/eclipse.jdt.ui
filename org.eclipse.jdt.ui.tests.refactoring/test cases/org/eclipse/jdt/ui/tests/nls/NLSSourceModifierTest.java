/*****************************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.ui.tests.nls;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHolder;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSInfo;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSourceModifier;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.TextChange;

public class NLSSourceModifierTest extends TestCase {
    
    private IJavaProject javaProject;
    
    private IPackageFragmentRoot fSourceFolder;
    
    public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(NLSSourceModifierTest.class));
	}
	
	public static Test suite() {
		return allTests();		
	}

    protected void setUp() throws Exception {
        javaProject = ProjectTestSetup.getProject();
        fSourceFolder = JavaProjectHelper.addSourceContainer(javaProject, "src");                
    }

    protected void tearDown() throws Exception {        
        JavaProjectHelper.clear(javaProject, ProjectTestSetup.getDefaultClasspath());        
    }
       
    public NLSSourceModifierTest(String name) {
        super(name); 
    }
    
    public void testFromSkippedToTranslated() throws Exception {
        
        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\";\n" +
            "}\n"; 
        
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        
        NLSHolder nlsHolder = NLSHolder.create(cu, new NLSInfo(cu));
        NLSSubstitution[] nlsSubstitutions = nlsHolder.getSubstitutions();
        NLSSubstitution.setPrefix("key.");
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].generateKey(nlsSubstitutions);
        
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, "${key}", "${key}", false, pack, "Accessor");
        
        Document doc = new Document(klazz);
        change.getEdit().apply(doc);
        
        assertEquals(
                "public class Test {\n" +
                "	private String str=Accessor.getString(\"key.0\"); //$NON-NLS-1$\n" +
            	"}\n", 
            	doc.get());
    }
    
    public void testFromSkippedToNotTranslated() throws Exception {
        
        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\";\n" +
            "}\n"; 
        
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        
        NLSHolder nlsHolder = NLSHolder.create(cu, new NLSInfo(cu));
        NLSSubstitution[] nlsSubstitutions = nlsHolder.getSubstitutions();
        NLSSubstitution.setPrefix("key.");
        nlsSubstitutions[0].setState(NLSSubstitution.IGNORED);
        
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, "${key}", "${key}", false, pack, "Accessor");
        
        Document doc = new Document(klazz);
        change.getEdit().apply(doc);
        
        assertEquals(
                "public class Test {\n" +
                "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
                "}\n",  
            	doc.get());
    }
    
    /**
     * TODO: the key should be 0
     */
    public void testFromNotTranslatedToTranslated() throws Exception {
        
        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
            "}\n"; 
        
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        
        NLSHolder nlsHolder = NLSHolder.create(cu, new NLSInfo(cu));
        NLSSubstitution[] nlsSubstitutions = nlsHolder.getSubstitutions();
        NLSSubstitution.setPrefix("key.");
        nlsSubstitutions[0].setState(NLSSubstitution.EXTERNALIZED);
        nlsSubstitutions[0].generateKey(nlsSubstitutions);
        
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, "${key}", "${key}", false, pack, "Accessor");
        
        Document doc = new Document(klazz);
        change.getEdit().apply(doc);
        
        assertEquals(
                "public class Test {\n" +
                "	private String str=Accessor.getString(\"key.0\"); //$NON-NLS-1$\n" +
            	"}\n", 
            	doc.get());
    }
    
    public void testFromNotTranslatedToSkipped() throws Exception {
        
        String klazz =
            "public class Test {\n" +
            "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
            "}\n"; 
        
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        
        NLSHolder nlsHolder = NLSHolder.create(cu, new NLSInfo(cu));
        NLSSubstitution[] nlsSubstitutions = nlsHolder.getSubstitutions();
        NLSSubstitution.setPrefix("key.");
        nlsSubstitutions[0].setState(NLSSubstitution.INTERNALIZED);
        
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, "${key}", "${key}", false, pack, "Accessor");
        
        Document doc = new Document(klazz);
        change.getEdit().apply(doc);
        
        assertEquals(
                "public class Test {\n" +
                "	private String str=\"whatever\"; \n" +
                "}\n",  
            	doc.get());
    }
    
    public void testFromTranslatedToNotTranslated() throws Exception {
        
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
        
        NLSHolder nlsHolder = NLSHolder.create(cu, new NLSInfo(cu));
        NLSSubstitution[] nlsSubstitutions = nlsHolder.getSubstitutions();
        NLSSubstitution.setPrefix("key.");
        nlsSubstitutions[0].setValue("whatever");
        nlsSubstitutions[0].setState(NLSSubstitution.IGNORED);
        
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, "${key}", "${key}", false, pack, "Accessor");
        
        Document doc = new Document(klazz);
        change.getEdit().apply(doc);
        
        assertEquals(
                "package test;\n" +
                "public class Test {\n" +
                "	private String str=\"whatever\"; //$NON-NLS-1$\n" +
                "}\n",  
            	doc.get());
    }
    
    public void testFromTranslatedToSkipped() throws Exception {
        
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
        
        NLSHolder nlsHolder = NLSHolder.create(cu, new NLSInfo(cu));
        NLSSubstitution[] nlsSubstitutions = nlsHolder.getSubstitutions();
        NLSSubstitution.setPrefix("key.");
        nlsSubstitutions[0].setValue("whatever");
        nlsSubstitutions[0].setState(NLSSubstitution.INTERNALIZED);
        
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, "${key}", "${key}", false, pack, "Accessor");
        
        Document doc = new Document(klazz);
        change.getEdit().apply(doc);
        
        assertEquals(
                "package test;\n" +
                "public class Test {\n" +
                "	private String str=\"whatever\"; \n" +
                "}\n",  
            	doc.get());
    }
    
    public void testReplacementOfKey() throws Exception {        
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
        
        NLSHolder nlsHolder = NLSHolder.create(cu, new NLSInfo(cu));
        NLSSubstitution[] nlsSubstitutions = nlsHolder.getSubstitutions();
        nlsSubstitutions[0].setKey("nls.0");        
        
        TextChange change = (TextChange) NLSSourceModifier.create(cu, nlsSubstitutions, "${key}", "${key}", false, pack, "Accessor");
        
        Document doc = new Document(klazz);
        change.getEdit().apply(doc);
        
        assertEquals(
                "package test;\n" +
                "public class Test {\n" +
                "	private String str=Accessor.getString(\"nls.0\"); //$NON-NLS-1$\n" +
                "}\n",  
            	doc.get());
    }
}
