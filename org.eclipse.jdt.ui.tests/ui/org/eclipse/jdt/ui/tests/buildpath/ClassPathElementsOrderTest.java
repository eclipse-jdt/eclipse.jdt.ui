/*******************************************************************************
 * Copyright (c) 2023 Raghunandana Murthappa and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raghunandana Murthappa - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.buildpath;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.LibrariesWorkbookPage;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

public class ClassPathElementsOrderTest {
	IJavaProject javaProject;

	private static final String NEW_JAR_PATH= "/data/test.jar";

	@Before
	public void setUp() throws CoreException, BackingStoreException {
		IWorkspaceRoot wsRoot= ResourcesPlugin.getWorkspace().getRoot();
		IProject genProj= wsRoot.getProject("java proj");
		if (!genProj.exists()) {
			genProj.create(null);
		}
		if (!genProj.isOpen()) {
			genProj.open(null);
		}

		//module and class path grouping available in compiler level 9 or above.
		IEclipsePreferences preferences= InstanceScope.INSTANCE.getNode("org.eclipse.jdt.core");
		preferences.put("org.eclipse.jdt.core.compiler.source", "10");
		preferences.flush();

		JavaModel javaModel= JavaModelManager.getJavaModelManager().getJavaModel();
		javaProject= new JavaProject(genProj, javaModel);
	}

	public void tearDown() throws CoreException {
		javaProject.getProject().delete(true, null);
	}

	/**
	 * This test invokes addElement API to add new class path entry. In this case also class path
	 * elements orders must be preserved and newly added entry should be added at the end.
	 */
	@Test
	public void testClassPathOrder() {
		Collection<CPListElement> elements= new ArrayList<>();
		CPListElement cont1= new CPListElement(javaProject, IClasspathEntry.CPE_CONTAINER,
				new Path("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-17"), null);
		cont1.createAttributeElement(CPListElement.MODULE, Boolean.TRUE.toString(), false);
		elements.add(cont1);
		CPListElement src1= new CPListElement(javaProject, IClasspathEntry.CPE_SOURCE, new Path("/hw2/src"), null);
		elements.add(src1);
		CheckedListDialogField<CPListElement> classPathList= new CheckedListDialogField<>(null, null, null);
		classPathList.setElements(elements);
		LibrariesWorkbookPage page= new LibrariesWorkbookPage(classPathList, null);
		page.init(javaProject);
		CPListElement cpElement= new CPListElement(javaProject, IClasspathEntry.CPE_LIBRARY, new Path(NEW_JAR_PATH), null);
		assertEquals("There should be 2 classpath elements before adding class path element", 2, classPathList.getElements().size());
		assertEquals("java container should be at the top", cont1, classPathList.getElements().get(0));
		assertEquals("src should be at position 1", src1, classPathList.getElements().get(1));
		page.addElement(cpElement);
		assertEquals("There should be 3 classpath elements after adding class path element", 3, classPathList.getElements().size());
		assertEquals("java container should be at the top", cont1, classPathList.getElements().get(0));
		assertEquals("src should be at position 1", src1, classPathList.getElements().get(1));
		assertEquals("newly added class path entry must be at the end of the list", NEW_JAR_PATH, classPathList.getElements().get(2).getPath().toString());
	}

	/**
	 * This test case adds an external jar to the Libraries Tab of the Java Build Path properties
	 * page. And test if the class path order is as expected after adding new jar
	 */
	@Test
	public void testIntegrationClassPathOrderAddingLibrary() {
		Collection<CPListElement> elements= new ArrayList<>();
		CPListElement cont1= new CPListElement(javaProject, IClasspathEntry.CPE_CONTAINER,
				new Path("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-17"), null);
		cont1.createAttributeElement(CPListElement.MODULE, Boolean.TRUE.toString(), false);
		elements.add(cont1);
		CPListElement cont2= new CPListElement(javaProject, IClasspathEntry.CPE_CONTAINER,
				new Path("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.3"), null);
		cont2.createAttributeElement(CPListElement.MODULE, Boolean.TRUE.toString(), false);
		elements.add(cont2);
		CPListElement src1= new CPListElement(javaProject, IClasspathEntry.CPE_SOURCE, new Path("/hw2/src"), null);
		elements.add(src1);
		CPListElement src2= new CPListElement(javaProject, IClasspathEntry.CPE_SOURCE, new Path("/hw2/src2"), null);
		elements.add(src2);
		CPListElement lib1= new CPListElement(javaProject, IClasspathEntry.CPE_LIBRARY, new Path("/data/swt.jar"), null);
		elements.add(lib1);
		CPListElement lib2= new CPListElement(javaProject, IClasspathEntry.CPE_LIBRARY, new Path("/data/jface.jar"), null);
		elements.add(lib2);
		CheckedListDialogField<CPListElement> classPathList= new CheckedListDialogField<>(null, null, null);
		classPathList.setElements(elements);
		TestableLibrariesWorkBookPage page= new TestableLibrariesWorkBookPage(classPathList, null);
		page.init(javaProject);
		assertEquals("There should be 6 classpath elements before adding external jar", 6, classPathList.getElements().size());
		assertEquals("java container should be at the top", cont1, classPathList.getElements().get(0));
		assertEquals("src should be at position 2", src1, classPathList.getElements().get(2));
		page.addExternalJar();
		assertEquals("There should be 7 classpath elements after adding external jar", 7, classPathList.getElements().size());
		assertEquals("java container should be at the top", cont1, classPathList.getElements().get(0));
		assertEquals("src should be at position 2", src1, classPathList.getElements().get(2));
		assertEquals("newly added jar must be at the end of the list", NEW_JAR_PATH, classPathList.getElements().get(6).getPath().toString());
	}

	public class TestableLibrariesWorkBookPage extends LibrariesWorkbookPage {

		public TestableLibrariesWorkBookPage(CheckedListDialogField<CPListElement> classPathList, IWorkbenchPreferenceContainer pageContainer) {
			super(classPathList, pageContainer);
		}

		public void addExternalJar() {
			getLibrariesAdapter().customButtonPressed(null, IDX_ADDEXT);
		}

		@Override
		protected CPListElement[] openExtJarFileDialog(CPListElement existing) {
			IPath ele1Path= new Path(NEW_JAR_PATH);
			CPListElement cpElement= new CPListElement(javaProject, IClasspathEntry.CPE_LIBRARY, ele1Path, null);
			CPListElement[] list= new CPListElement[1];
			list[0]= cpElement;
			return list;
		}

		/**
		 * Actual implementation returns selected elements from tree viewer UI. We have not created
		 * real tree viewer UI in test. So we are getting the input of tree viewer and returning
		 * second element in the list.
		 *
		 * @return ClassPath root node.
		 */
		@Override
		protected List<Object> getSelectedLibraryElements() {
			List<Object> rootList= new ArrayList<>();
			List<CPListElement> allEle= getLibraryElements();
			//There are 2 root nodes in Library tab. ModulePath and ClassPath
			if (allEle.size() >= 2) {
				//second element is always a ClassPath.
				rootList.add(allEle.get(1));
			}
			return rootList;
		}

		@Override
		protected boolean getRootExpansionState(TreeListDialogField<CPListElement> list, boolean isClassPathRoot) {
			return isClassPathRoot;
		}
	}

}
