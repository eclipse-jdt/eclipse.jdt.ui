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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ExcludeOperation;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.IPackageExplorerActionListener;
import org.eclipse.jdt.internal.corext.buildpath.PackageExplorerActionEvent;

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
    protected IPackageExplorerActionListener fListener;
    
    private final int PROJ= 0x00;
    private final int SRC= 0x01;
    private final int NF= 0x02;
    private final int PACK= 0x03;
    private final int CU= 0x04;
    private final int EXCLUDED_FILE= 0x05;
    private final int FILE= 0x06;
    private final int EXCLUDED_PACK= 0x07;
    private final int DEFAULT_PACK= 0x08;
    
    /* ### Project Structure:
     * - DummyProject
     *        |- src
     *            |- default package
     *            |- pack1
     *                 |- A.java
     *                 |- B.java (excluded)
     *                 |- NormalFile
     *                 |- pack2 (excluded)
     *        |- NormalFolder
     */
    
    public NewProjectWizardOperationTest() {
        super(THIS.getName());
    }
    
    protected void setUp() throws Exception {
        fProject= createProject();
        fSelection= new ArrayList();
        fActionGroup= new DialogPackageExplorerActionGroup(this, null);
        assertFalse(fProject.isOnClasspath(fProject.getUnderlyingResource()));
    }

    protected void tearDown() throws Exception {
        fActionGroup.dispose();
        fSelection.clear();
        fProject.getProject().delete(true, true, null);
    }
    
    public void testProjectWithOthers() throws JavaModelException {
        addToSelection(new int[] {PROJ});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, SRC});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, NF});
        context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK});
        context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, CU});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, SRC, NF});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF});
        context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, CU});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, SRC, PACK, NF, CU});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, EXCLUDED_FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, EXCLUDED_FILE, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, EXCLUDED_FILE, FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testSrcWithOthers() throws JavaModelException {
        addToSelection(new int[] {SRC});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {REMOVE_FROM_BP, EDIT, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, NF});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, CU});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testNormalFolderWithOthers() throws JavaModelException {
        addToSelection(new int[] {NF});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK});
        context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, CU});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, CU});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, CU, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK, EXCLUDED_FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testPackageWithOthers() throws JavaModelException {
        addToSelection(new int[] {PACK});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {ADD_TO_BP, EXCLUDE, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, CU});
        context= createContext();
        addListener(new int[] {EXCLUDE, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {ADD_TO_BP, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testCUWithOthers() throws JavaModelException {
        addToSelection(new int[] {CU});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {EXCLUDE, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, EXCLUDED_PACK, PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testExcludedFileWithOthers() throws JavaModelException {
        addToSelection(new int[] {EXCLUDED_FILE});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {UNEXCLUDE, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, FILE});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {UNEXCLUDE, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testFileWithOthers() throws JavaModelException {
        addToSelection(new int[] {FILE});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {FILE, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testExcludedPackWithOthers() throws JavaModelException {
        addToSelection(new int[] {EXCLUDED_PACK});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {ADD_TO_BP, UNEXCLUDE, CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testDefaultPackWithOthers() throws JavaModelException {
        addToSelection(new int[] {DEFAULT_PACK});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
    }
    
    private IJavaProject createProject() throws CoreException, InvocationTargetException {
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
        
        final IPath projectPath= fProject.getProject().getFullPath();

        // configure the classpath entries, including the default jre library.
        List cpEntries= new ArrayList();
        cpEntries.add(JavaCore.newSourceEntry(projectPath.append(srcPath)));
        cpEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));
        IClasspathEntry[] entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
        fProject.setRawClasspath(entries, null);
        
        // one package in src folder
        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(fProject.getPath().append(srcPath));
        IPackageFragment pack1= root.createPackageFragment("pack1", true, null);
        final IPackageFragment pack2= root.createPackageFragment("pack1.pack2", true, null);
        IPackageFragment defaultPack= root.getPackageFragment("");
        
        // two compilation units A and B in 'package'
        ICompilationUnit cuA= createICompilationUnit("A", pack1);
        final IResource excludedElements[]= {null, null}; 
        final ICompilationUnit cuB= createICompilationUnit("B", pack1);
        ExcludeOperation op= new ExcludeOperation(null, new IClasspathInformationProvider() {

            public void handleResult(List resultElements, CoreException exception, int operationType) {
                excludedElements[0]= (IFile)resultElements.get(0);
                excludedElements[1]= (IFolder)resultElements.get(1);
            }

            public List getSelection() {
                List list= new ArrayList();
                list.add(cuB);
                list.add(pack2);
                return list;
            }

            public IJavaProject getJavaProject() {
                return fProject;
            }

            public IOutputFolderQuery getOutputFolderQuery() throws JavaModelException {
                return null;
            }

            public IInclusionExclusionQuery getInclusionExclusionQuery() throws JavaModelException {
                return null;
            }

            public IOutputLocationQuery getOutputLocationQuery() throws JavaModelException {
                return null;
            }

            public IFolderCreationQuery getFolderCreationQuery() throws JavaModelException {
                return null;
            }

            public ILinkToQuery getLinkFolderQuery() throws JavaModelException {
                return null;
            }

            public void deleteCreatedResources() {
            }
            
        });
        op.run(null);
        IFile file= fProject.getProject().getFile(filePath);
        file.create(null, false, null);
        
        fItems= new Object[9];
        fItems[PROJ]= fProject;
        fItems[SRC]= root;
        fItems[NF]= folder2;
        fItems[PACK]= pack1;
        fItems[CU]= cuA;
        fItems[EXCLUDED_FILE]= excludedElements[0];
        fItems[FILE]= file;
        fItems[EXCLUDED_PACK]= excludedElements[1];
        fItems[DEFAULT_PACK]= defaultPack;
        
        return fProject;
    }
    
    protected int getID(ClasspathModifierAction action) {
        return Integer.parseInt(action.getId());
    }
    
    protected void addToSelection(int[] indices) {
        for (int i= 0; i < indices.length; i++) {
            fSelection.add(fItems[indices[i]]);
        }
    }
    
    protected void addToSelection(Object obj) {
        fSelection.add(obj);
    }
    
    protected DialogExplorerActionContext createContext() {
        return new DialogExplorerActionContext(fSelection, fProject);
    }
    
    protected void addListener(final int[] expectedValues) {
        fListener= new IPackageExplorerActionListener() {
            public void handlePackageExplorerActionEvent(PackageExplorerActionEvent event) {
                ClasspathModifierAction[] actions= event.getEnabledActions();
                assertTrue(actions.length == expectedValues.length);
                for(int i= 0; i < actions.length; i++) {
                    assertTrue(getID(actions[i]) == expectedValues[i]);
                }
            }
        };
        fActionGroup.addListener(fListener);
    }
    
    protected void reset() {
        fSelection.clear();
        fActionGroup.removeListener(fListener);
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