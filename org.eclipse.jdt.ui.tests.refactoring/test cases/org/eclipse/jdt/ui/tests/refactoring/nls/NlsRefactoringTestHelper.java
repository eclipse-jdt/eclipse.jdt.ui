/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.nls;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

/**
 * Helper class for the nls-tests.
 *
 * often used functionality is located here to get around extending testcases
 * from testcases (just for code reuse).
 */
public class NlsRefactoringTestHelper {

    NullProgressMonitor fNpm;

    private HashMap fWorkSpaceElements = new HashMap();
    private IJavaProject fJavaProject;

    public NlsRefactoringTestHelper(IJavaProject javaProject) throws Exception {
        fJavaProject = javaProject;
        fNpm = new NullProgressMonitor();
        fWorkSpaceElements = new HashMap();
        setupTestSpace();
    }

    private InputStream getInputStream(String input) {
        return new ByteArrayInputStream(input.getBytes());
    }

    private void setupTestSpace() throws Exception {
        fWorkSpaceElements.put(fJavaProject.getPath().toString(), fJavaProject);

        createPackageFragmentRoot(fJavaProject, "src1");
        createPackageFragmentRoot(fJavaProject, "src2");

        createPackageFragment("p", "/TestSetupProject/src1");
        createPackageFragment("p", "/TestSetupProject/src2");

        createFile("/TestSetupProject/src2/p", "test.properties", "");
        createCu("/TestSetupProject/src1/p", "WithStrings.java", "package p;class WithStrings {String s1=\"test1\";String s2=\"test2\";}");
        createCu("/TestSetupProject/src1/p", "WithoutStrings.java", "package p;class WithoutStrings {}");

		String newFileTemplate= "${package_declaration}\n\n${type_declaration}";
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, newFileTemplate, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "", null);

    }

    private void createFile(String packageFragmentName, String fileName, String content) throws Exception {
        IPackageFragment fragment = (IPackageFragment) fWorkSpaceElements.get(packageFragmentName);
        IPath p = fragment.getPath().append(fileName);
        IFile file = createFile(p, content);
        fWorkSpaceElements.put(file.getFullPath().toString(), file);
    }

    private IFile createFile(IPath path, String content) throws Exception {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        InputStream iS = getInputStream(content);
        file.create(iS, true, fNpm);
        iS.close();
        return file;
    }

    public void createPackageFragment(String packageName, String fragmentRootName) throws Exception {
        IPackageFragmentRoot fragmentRoot = (IPackageFragmentRoot) fWorkSpaceElements.get(fragmentRootName);
        IPackageFragment newPackageFragment = fragmentRoot.createPackageFragment(packageName, false, fNpm);
        fWorkSpaceElements.put(newPackageFragment.getPath().toString(), newPackageFragment);
    }

    public void createPackageFragmentRoot(IJavaProject project, String string) throws CoreException {
        IPackageFragmentRoot srcRoot1 = JavaProjectHelper.addSourceContainer(project, string);
        fWorkSpaceElements.put(srcRoot1.getPath().toString(), srcRoot1);
    }

    public IPackageFragment getPackageFragment(String path) {
        return (IPackageFragment) fWorkSpaceElements.get(path);
    }

    public IFile getFile(String string) {
        return (IFile) fWorkSpaceElements.get(string);
    }

    public ICompilationUnit createCu(String path, String name, String content) throws Exception {
        IPackageFragment f = (IPackageFragment) fWorkSpaceElements.get(path);
        ICompilationUnit res = f.createCompilationUnit(name, content, false, fNpm);
        fWorkSpaceElements.put(res.getPath().toString(), res);
        return res;
    }

    public ICompilationUnit getCu(String string) {
        return (ICompilationUnit) fWorkSpaceElements.get(string);
    }
}
