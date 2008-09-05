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
package org.eclipse.jdt.ui.tests.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;


public class NewProjectTestSetup extends TestSetup {

    public static final String WORKSPACE_PROJECT= "WorkspaceProject";
    public static final String WORKSPACE_PROJECT_SRC= "WorkspaceProjectWithSourceAndInFolder";
    
    public static IJavaProject getProject(String projectName, String binFolderName) throws CoreException {
        return JavaProjectHelper.createJavaProject(projectName, binFolderName);
    }
    
    public static IClasspathEntry[] getDefaultClasspath() {
        return PreferenceConstants.getDefaultJRELibrary();
    }
    
    private IJavaProject fWorkspaceProject;
    private IJavaProject fWorkspaceProjectWithSrc;
    private IJavaProject fExternalProject;

    private boolean fAutobuilding;
    
    public NewProjectTestSetup(Test test) {
        super(test);
        try {
            fAutobuilding= JavaProjectHelper.setAutoBuilding(false);
        } catch (CoreException e) {
            JavaPlugin.log(e);
        }
    }
    
    public IJavaProject getProject(IJavaProject currentProject) throws CoreException {
        String name= currentProject.getElementName();
        currentProject.getProject().delete(true, null);
        if (name.equals(WORKSPACE_PROJECT))
            return getWorkspaceProject();
        if (name.equals(WORKSPACE_PROJECT_SRC))
            return getWorkspaceProjectWithSrc();
        return null;
    }
    
    public IJavaProject getWorkspaceProject() {
        try {
            fWorkspaceProject= getProject(WORKSPACE_PROJECT, "");
            List cpEntries= new ArrayList();
            IPath projectPath= fWorkspaceProject.getProject().getFullPath();
            cpEntries.add(JavaCore.newSourceEntry(projectPath));
            cpEntries.addAll(Arrays.asList(getDefaultClasspath()));
            IClasspathEntry[] entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
            fWorkspaceProject.setRawClasspath(entries, projectPath, new NullProgressMonitor());
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        } catch (CoreException e) {
            JavaPlugin.log(e);
        }
        return fWorkspaceProject;
    }
    
    public IJavaProject getWorkspaceProjectWithSrc() {
        try {
            fWorkspaceProjectWithSrc= getProject(WORKSPACE_PROJECT_SRC, PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
            createWithSrcAndBinFolder(fWorkspaceProjectWithSrc);
        } catch (CoreException e) {
            JavaPlugin.log(e);
        }
        return fWorkspaceProjectWithSrc;
    }
    
    public IJavaProject getExternalProject() {
        return fExternalProject;
    }
    /* (non-Javadoc)
     * @see junit.extensions.TestSetup#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        JavaCore.setOptions(TestOptions.getDefaultOptions());
        TestOptions.initializeCodeGenerationOptions();
        JavaPlugin.getDefault().getCodeTemplateStore().load(); 
    }

    protected void tearDown() throws Exception {
        if (fWorkspaceProject != null && fWorkspaceProject.exists())
            JavaProjectHelper.delete(fWorkspaceProject);
        if (fWorkspaceProjectWithSrc != null && fWorkspaceProjectWithSrc.exists())
            JavaProjectHelper.delete(fWorkspaceProjectWithSrc);
        if (fExternalProject != null && fExternalProject.exists())
            JavaProjectHelper.delete(fExternalProject);
        JavaProjectHelper.setAutoBuilding(fAutobuilding);
    }
    
    private void createWithSrcAndBinFolder(IJavaProject project) {
        IPath srcPath= new Path("src");
        try {
            if (srcPath.segmentCount() > 0) {
                IFolder folder= project.getProject().getFolder(srcPath);
                CoreUtility.createFolder(folder, true, true, null);
            }
            
            final IPath projectPath= project.getProject().getFullPath();
    
            // configure the classpath entries, including the default jre library.
            List cpEntries= new ArrayList();
            cpEntries.add(JavaCore.newSourceEntry(projectPath.append(srcPath)));
            cpEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));
            IClasspathEntry[] entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
            
            project.setRawClasspath(entries, null);
        } catch (CoreException e) {
            JavaPlugin.log(e);
        }
    }

}
