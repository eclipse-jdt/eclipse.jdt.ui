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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.actions.ActionContext;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.BuildpathModifierAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddArchivesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddLibrariesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.ICreateFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IInclusionExclusionQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.ILinkToQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IRemoveLinkedFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.OutputFolderQuery;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

/**
 */
public class NewProjectWizardOperationTest extends TestCase implements IClasspathInformationProvider {
	
	public static class DialogExplorerActionContext extends ActionContext {
		private IJavaProject fJavaProject;
		private List fSelectedElements;

		/**
		 * Constructor to create an action context for the dialog package explorer.
		 * 
		 * For reasons of completeness, the selection of the super class 
		 * <code>ActionContext</code> is also set, but is not intendet to be used.
		 * 
		 * @param selection the current selection
		 * @param jProject the element's Java project
		 */
		public DialogExplorerActionContext(ISelection selection, IJavaProject jProject) {
			super(null);
			fJavaProject= jProject;
			fSelectedElements= ((IStructuredSelection)selection).toList();
			IStructuredSelection structuredSelection= new StructuredSelection(new Object[] {fSelectedElements, jProject});
			super.setSelection(structuredSelection);
		}

		/**
		 * Constructor to create an action context for the dialog package explorer.
		 * 
		 * For reasons of completeness, the selection of the super class 
		 * <code>ActionContext</code> is also set, but is not intendet to be used.
		 * 
		 * @param selectedElements a list of currently selected elements
		 * @param jProject the element's Java project
		 */
		public DialogExplorerActionContext(List selectedElements, IJavaProject jProject) {
			super(null);
			fJavaProject= jProject;
			fSelectedElements= selectedElements;
			IStructuredSelection structuredSelection= new StructuredSelection(new Object[] {fSelectedElements, jProject});
			super.setSelection(structuredSelection);
		}

		public IJavaProject getJavaProject() {
			return fJavaProject;
		}

		public List getSelectedElements() {
			return fSelectedElements;
		}
	}

    
    public static final Class THIS= NewProjectWizardOperationTest.class;
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
    private final int JAR= 0x09; // on buildpath
    private final int ZIP= 0xA; // not on buildpath
    
    /* ### Project Structure:
     * - DummyProject
     *        |- src
     *            |- default package
     *            |- pack1
     *                 |- A.java
     *                 |- B.java (excluded)
     *                 |- NormalFile
     *                 |- pack2 (excluded)
     *            |- archive.jar (on buildpath)
     *            |- archive.zip (excluded)
     *        |- NormalFolder
     */
    
    public NewProjectWizardOperationTest() {
        super(THIS.getName());
    }
    
    protected void setUp() throws Exception {
        fProject= createProject();
        fSelection= new ArrayList();
        fActionGroup= new DialogPackageExplorerActionGroup(null, null, null, null);
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
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP, BuildpathModifierAction.CREATE_LINK});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, SRC});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, NF});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, CU});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, SRC, NF});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, SRC, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, NF, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, NF, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, CU});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, EXCLUDED_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, EXCLUDED_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, SRC, PACK, NF, CU});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, EXCLUDED_FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, EXCLUDED_FILE, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, EXCLUDED_FILE, FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PROJ, PACK, NF, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE, DEFAULT_PACK, JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testSrcWithOthers() throws JavaModelException {
        addToSelection(new int[] {SRC});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {BuildpathModifierAction.REMOVE_FROM_BP, BuildpathModifierAction.EDIT_FILTERS});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, NF});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, CU});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, JAR});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.REMOVE_FROM_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, JAR, NF});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, JAR, PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, JAR, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {SRC, NF, PACK, CU, EXCLUDED_FILE, FILE, EXCLUDED_PACK, DEFAULT_PACK, JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testNormalFolderWithOthers() throws JavaModelException {
        addToSelection(new int[] {NF});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, CU});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, CU});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, CU, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_FILE, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_FILE, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {NF, PACK, EXCLUDED_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testPackageWithOthers() throws JavaModelException {
        addToSelection(new int[] {PACK});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP, BuildpathModifierAction.EXCLUDE});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, CU});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.EXCLUDE});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, CU, FILE, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, CU, FILE, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE, DEFAULT_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE, DEFAULT_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {PACK, EXCLUDED_PACK, CU, FILE, EXCLUDED_FILE, DEFAULT_PACK, JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testCUWithOthers() throws JavaModelException {
        addToSelection(new int[] {CU});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {BuildpathModifierAction.EXCLUDE});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.EXCLUDE});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, EXCLUDED_FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, EXCLUDED_PACK, PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, EXCLUDED_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, EXCLUDED_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU, PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {CU,EXCLUDED_FILE, FILE, EXCLUDED_PACK, DEFAULT_PACK, JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testExcludedFileWithOthers() throws JavaModelException {
        addToSelection(new int[] {EXCLUDED_FILE});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {BuildpathModifierAction.UNEXCLUDE});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.UNEXCLUDE});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, FILE});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.UNEXCLUDE});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, EXCLUDED_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_FILE, EXCLUDED_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testFileWithOthers() throws JavaModelException {
        addToSelection(new int[] {FILE});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {FILE, EXCLUDED_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {FILE, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {FILE, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {FILE, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {FILE, EXCLUDED_PACK, JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testExcludedPackWithOthers() throws JavaModelException {
        addToSelection(new int[] {EXCLUDED_PACK});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {BuildpathModifierAction.ADD_SEL_SF_TO_BP, BuildpathModifierAction.UNEXCLUDE});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_PACK, DEFAULT_PACK});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_PACK, DEFAULT_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_PACK, DEFAULT_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_PACK, JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {EXCLUDED_PACK, DEFAULT_PACK, JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testDefaultPackWithOthers() throws JavaModelException {
        addToSelection(new int[] {DEFAULT_PACK});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {DEFAULT_PACK, JAR});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {DEFAULT_PACK, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {DEFAULT_PACK, JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testDefaultJARWithOthers() throws JavaModelException {
        addToSelection(new int[] {JAR});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {BuildpathModifierAction.REMOVE_FROM_BP});
        fActionGroup.setContext(context);
        reset();
        
        addToSelection(new int[] {JAR, ZIP});
        context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
    }
    
    public void testDefaultZipWithOthers() throws JavaModelException, InvocationTargetException {
        addToSelection(new int[] {ZIP});
        DialogExplorerActionContext context= createContext();
        addListener(new int[] {});
        fActionGroup.setContext(context);
        reset();
        
        // if the zip file is added to the buildpath, then both, the zip and the jar file 
        // should have the option to be removed and the reset all operation additionally becomes 
        // available as we changed the project.
        final IPackageFragmentRoot[] addedZipArchive= {null};
//        AddSelectedLibraryOperation operation= new AddSelectedLibraryOperation(null, new IClasspathInformationProvider() {
//
//            public void handleResult(List resultElements, CoreException exception, int operationType) {
//                addedZipArchive[0]= (IPackageFragmentRoot)resultElements.get(0);
//            }
//
//            public IStructuredSelection getSelection() {
//                List list= new ArrayList();
//                list.add(fItems[ZIP]); 
//                return new StructuredSelection(list);
//            }
//
//            public IJavaProject getJavaProject() {
//                return fProject;
//            }
//
//            public OutputFolderQuery getOutputFolderQuery() throws JavaModelException {
//                return null;
//            }
//
//            public IInclusionExclusionQuery getInclusionExclusionQuery() throws JavaModelException {
//                return null;
//            }
//
//            public IOutputLocationQuery getOutputLocationQuery() throws JavaModelException {
//                return null;
//            }
//
//            public ILinkToQuery getLinkFolderQuery() throws JavaModelException {
//                return null;
//            }
//            
//            public IAddArchivesQuery getExternalArchivesQuery() throws JavaModelException {
//                return null;
//            }
//
//            public IAddLibrariesQuery getLibrariesQuery() throws JavaModelException {
//                return null;
//            }
//
//            public void deleteCreatedResources() {
//            }
//
//			public IRemoveLinkedFolderQuery getRemoveLinkedFolderQuery() throws JavaModelException {
//				return null;
//			}
//
//			public ICreateFolderQuery getCreateFolderQuery() throws JavaModelException {
//				return null;
//			}
//        });
//        operation.run(null);
        
        fSelection.add(addedZipArchive[0]);
        fSelection.add(fItems[JAR]);
        context= createContext();
        addListener(new int[] {BuildpathModifierAction.REMOVE_FROM_BP, BuildpathModifierAction.RESET_ALL});
        fActionGroup.setContext(context);
        reset();
    }
    
    private IJavaProject createProject() throws CoreException, InvocationTargetException {
        fProject= JavaProjectHelper.createJavaProject("Dummy project", "bin");
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
        IPackageFragment defaultPack= root.getPackageFragment("");
        
        IPath libraryPath= root.getPath().append("archive.jar");
        IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(jarRoot.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        
        libraryPath= root.getPath().append("archive.zip");
        final IPackageFragmentRoot zipRoot= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(zipRoot.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        
        // two compilation units A and B in 'package'
        ICompilationUnit cuA= createICompilationUnit("A", pack1);
        final IResource excludedElements[]= {null, null}; 
//        final IPackageFragment pack2= root.createPackageFragment("pack1.pack2", true, null);
//        final ICompilationUnit cuB= createICompilationUnit("B", pack1);
//        ExcludeOperation op= new ExcludeOperation(null, new IClasspathInformationProvider() {
//
//            public void handleResult(List resultElements, CoreException exception, int operationType) {
//                excludedElements[0]= (IFile)resultElements.get(0);
//                excludedElements[1]= (IFolder)resultElements.get(1);
//            }
//
//            public IStructuredSelection getSelection() {
//                List list= new ArrayList();
//                list.add(cuB); // exclude compilation unit B
//                list.add(pack2); // exclude pack2
//                return new StructuredSelection(list);
//            }
//
//            public IJavaProject getJavaProject() {
//                return fProject;
//            }
//
//            public OutputFolderQuery getOutputFolderQuery() throws JavaModelException {
//                return null;
//            }
//
//            public IInclusionExclusionQuery getInclusionExclusionQuery() throws JavaModelException {
//                return null;
//            }
//
//            public IOutputLocationQuery getOutputLocationQuery() throws JavaModelException {
//                return null;
//            }
//
//            public ILinkToQuery getLinkFolderQuery() throws JavaModelException {
//                return null;
//            }
//            
//            public IAddArchivesQuery getExternalArchivesQuery() throws JavaModelException {
//                return null;
//            }
//
//            public IAddLibrariesQuery getLibrariesQuery() throws JavaModelException {
//                return null;
//            }
//
//            public void deleteCreatedResources() {
//            }
//
//			public IRemoveLinkedFolderQuery getRemoveLinkedFolderQuery() throws JavaModelException {
//				return null;
//			}
//
//			public ICreateFolderQuery getCreateFolderQuery() throws JavaModelException {
//				return null;
//			}
//            
//        });
//        op.run(null);
        IFile file= fProject.getProject().getFile(filePath);
        file.create(null, false, null);
        
        final IFile[] removedZipFile= {null};
//        RemoveFromClasspathOperation operation= new RemoveFromClasspathOperation(null, new IClasspathInformationProvider() {
//
//            public void handleResult(List resultElements, CoreException exception, int operationType) {
//                removedZipFile[0]= (IFile)resultElements.get(0);
//            }
//
//            public IStructuredSelection getSelection() {
//                List list= new ArrayList();
//                list.add(zipRoot); 
//                return new StructuredSelection(list);
//            }
//
//            public IJavaProject getJavaProject() {
//                return fProject;
//            }
//
//            public OutputFolderQuery getOutputFolderQuery() throws JavaModelException {
//                return null;
//            }
//
//            public IInclusionExclusionQuery getInclusionExclusionQuery() throws JavaModelException {
//                return null;
//            }
//
//            public IOutputLocationQuery getOutputLocationQuery() throws JavaModelException {
//                return null;
//            }
//
//            public ILinkToQuery getLinkFolderQuery() throws JavaModelException {
//                return null;
//            }
//            
//            public IAddArchivesQuery getExternalArchivesQuery() throws JavaModelException {
//                return null;
//            }
//
//            public IAddLibrariesQuery getLibrariesQuery() throws JavaModelException {
//                return null;
//            }
//
//            public void deleteCreatedResources() {
//            }
//
//			public IRemoveLinkedFolderQuery getRemoveLinkedFolderQuery() throws JavaModelException {
//				return null;
//			}
//
//			public ICreateFolderQuery getCreateFolderQuery() throws JavaModelException {
//				return null;
//			}
//            
//        });
//        operation.run(null);
        removedZipFile[0].create(null, false, null); // create the zip file
        
        fItems= new Object[11];
        fItems[PROJ]= fProject;
        fItems[SRC]= root;
        fItems[NF]= folder2;
        fItems[PACK]= pack1;
        fItems[CU]= cuA;
        fItems[EXCLUDED_FILE]= excludedElements[0];
        fItems[FILE]= file;
        fItems[EXCLUDED_PACK]= excludedElements[1];
        fItems[DEFAULT_PACK]= defaultPack;
        fItems[JAR]= jarRoot;
        fItems[ZIP]= removedZipFile[0];
        
        return fProject;
    }
    
//    protected int getID(ClasspathModifierAction action) {
//        return Integer.parseInt(action.getId());
//    }
    
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
//        fListener= new IPackageExplorerActionListener() {
//            public void handlePackageExplorerActionEvent(PackageExplorerActionEvent event) {
//                ClasspathModifierAction[] actions= event.getEnabledActions();
//                if (actions.length != expectedValues.length) {
//                	assertTrue(false);
//                }
//                for(int i= 0; i < actions.length; i++) {
//                    assertTrue(getID(actions[i]) == expectedValues[i]);
//                }
//            }
//        };
//        fActionGroup.addListener(fListener);
    }
    
    protected void reset() {
        fSelection.clear();
//        fActionGroup.removeListener(fListener);
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
    public IStructuredSelection getSelection() {
        return new StructuredSelection(fSelection);
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
    public OutputFolderQuery getOutputFolderQuery() throws JavaModelException {
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
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#getLinkFolderQuery()
     */
    public ILinkToQuery getLinkFolderQuery() throws JavaModelException {
        return null;
    }
    
    public IAddArchivesQuery getExternalArchivesQuery() throws JavaModelException {
        return null;
    }

    public IAddLibrariesQuery getLibrariesQuery() throws JavaModelException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#deleteCreatedResources()
     */
    public void deleteCreatedResources() {
    }

	/*
	 * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider#getRemoveLinkedFolderQuery()
	 */
	public IRemoveLinkedFolderQuery getRemoveLinkedFolderQuery() throws JavaModelException {
		return null;
	}

	public ICreateFolderQuery getCreateFolderQuery() throws JavaModelException {
		return null;
	}
}
