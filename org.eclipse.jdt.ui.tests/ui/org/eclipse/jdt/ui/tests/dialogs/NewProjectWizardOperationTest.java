/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IFolderCreationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IInclusionExclusionQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.ILinkToQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup.DialogExplorerActionContext;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

/**
 */
public class NewProjectWizardOperationTest extends TestCase implements IClasspathInformationProvider{
    
    private static final Class THIS= NewProjectWizardOperationTest.class;
    protected IJavaProject fProject;
    protected DialogPackageExplorerActionGroup fActionGroup;
    protected List fSelection;
    protected Object[] fItems;
    
    private final int PROJ= 0x00;
    private final int SRC= 0x01;
    private final int NF= 0x02;
    private final int PACK= 0x03;
    private final int CUA= 0x04;
    private final int CUB= 0x05;
    private final int FILE= 0x06;
    
    /* ### Project Structure:
     * - DummyProject
     *        |- src
     *            |- pack1
     *                 |- A.java
     *                 |- B.java
     *                 |- NormalFile
     *        |- NormalFolder
     */
    
    public NewProjectWizardOperationTest() {
        super(THIS.getName());
    }
    
    protected void setUp() throws Exception {
        /*fProject= createProject();
        fSelection= new ArrayList();
        fActionGroup= new DialogPackageExplorerActionGroup(this, null);
        assertFalse(fProject.isOnClasspath(fProject.getUnderlyingResource()));*/
    }

    protected void tearDown() throws Exception {
        /*fActionGroup.dispose();
        fSelection.clear();
        fProject.getProject().delete(true, true, null);*/
    }
    
    public void testAddtoBuildpathOperation() throws JavaModelException {
        /*addToSelection(PROJ);
        DialogExplorerActionContext context= createContext();
        
        fActionGroup.addListener(new IPackageExplorerActionListener() {
            public void handlePackageExplorerActionEvent(PackageExplorerActionEvent event) {
                ClasspathModifierAction[] actions= event.getEnabledActions();
                assertTrue(actions.length == 2);
                assertTrue(getID(actions[0]) == IClasspathInformationProvider.ADD_TO_BP);
                assertTrue(getID(actions[1]) == IClasspathInformationProvider.CREATE_LINK);
            }
        });
        fActionGroup.setContext(context);*/
    }
    
    public void testRemoveFromBuildpathOperation() {
        
    }
    
    public void testExcludeOperation() {
        
    }
    
    public void testUnexcludeOperation() {
        
    }
    
    public void testEditOperation() {
        
    }
    
    public void testResetAllOperation() {
        
    }
    
    public void testLinkFolderOperation() {
        
    }
    
    private IJavaProject createProject() throws CoreException {
        fProject= JavaProjectHelper.createJavaProject("Dummy project", PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
        IPath srcPath= new Path("src");
        IPath normalFolderPath= new Path("NormalFolder");
        IPath packagePath= srcPath.append("pack1");
        IPath filePath= packagePath.append("NormalFile");
        
        // src folder
        IFolder folder= fProject.getProject().getFolder(srcPath);
        CoreUtility.createFolder(folder, true, true, null);
        
        // one normal folder
        IFolder folder2= fProject.getProject().getFolder(normalFolderPath);
        CoreUtility.createFolder(folder, true, true, null);
        
        // one package in src folder
        fProject.getProject().getFolder(packagePath);
        CoreUtility.createFolder(folder, true, true, null);
        
        final IPath projectPath= fProject.getProject().getFullPath();

        // configure the classpath entries, including the default jre library.
        List cpEntries= new ArrayList();
        cpEntries.add(JavaCore.newSourceEntry(projectPath.append(srcPath)));
        cpEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));
        IClasspathEntry[] entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
        
        fProject.setRawClasspath(entries, null);
        
        // two compilation units A and B in 'package'
        IPackageFragment pack1= fProject.findPackageFragment(fProject.getPath().append(packagePath));
        ICompilationUnit cuA= createICompilationUnit("A", pack1);
        ICompilationUnit cuB= createICompilationUnit("B", pack1);
        IFile file= fProject.getProject().getFile(filePath);
        file.create(null, false, null);
        
        fItems= new Object[7];
        fItems[PROJ]= fProject;
        fItems[SRC]= fProject.findPackageFragmentRoot(fProject.getPath().append(srcPath));
        fItems[NF]= folder2;
        fItems[PACK]= pack1;
        fItems[CUA]= cuA;
        fItems[CUB]= cuB;
        fItems[FILE]= file;
        
        return fProject;
    }
    
    protected int getID(ClasspathModifierAction action) {
        return Integer.parseInt(action.getId());
    }
    
    protected void addToSelection(int i) {
        fSelection.add(fItems[i]);
    }
    
    protected void addToSelection(Object obj) {
        fSelection.add(obj);
    }
    
    protected DialogExplorerActionContext createContext() {
        return new DialogExplorerActionContext(fSelection, fProject);
    }
    
    protected ICompilationUnit createICompilationUnit(String className, IPackageFragment fragment) throws JavaModelException {
        String packString= fragment.getElementName().equals("") ? fragment.getElementName() : "package " + fragment.getElementName() +";\n";
        StringBuffer content= getFileContent(className, packString);
        return fragment.createCompilationUnit(className+".java", content.toString(), false, null);
    }
    
    protected StringBuffer getFileContent(String className, String packageHeader) {
        StringBuffer buf= new StringBuffer();
        buf.append(packageHeader);
        buf.append("\n");   
        buf.append("public class "+className+ " {\n");
        buf.append("    public void foo() {\n");
        buf.append("    }\n");      
        buf.append("}\n");
        return buf;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#handleResult(java.util.List, org.eclipse.core.runtime.CoreException, int)
     */
    public void handleResult(List resultElements, CoreException exception, int operationType) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#getSelection()
     */
    public List getSelection() {
        return fSelection;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#getJavaProject()
     */
    public IJavaProject getJavaProject() {
        return fProject;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#getOutputFolderQuery()
     */
    public IOutputFolderQuery getOutputFolderQuery() throws JavaModelException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#getInclusionExclusionQuery()
     */
    public IInclusionExclusionQuery getInclusionExclusionQuery() throws JavaModelException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#getOutputLocationQuery()
     */
    public IOutputLocationQuery getOutputLocationQuery() throws JavaModelException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#getFolderCreationQuery()
     */
    public IFolderCreationQuery getFolderCreationQuery() throws JavaModelException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#getLinkFolderQuery()
     */
    public ILinkToQuery getLinkFolderQuery() throws JavaModelException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#deleteCreatedResources()
     */
    public void deleteCreatedResources() {
    }
}
