/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.jarexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;

public class PlainJarExportTests {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	@Rule
	public TestName tn= new TestName();

	@BeforeClass
	public static void setUpTest() {
		System.setProperty("jdt.bug.367669", "non-null");
	}

	private IJavaProject fProject;
	private IPackageFragmentRoot fMainRoot;
	private ICompilationUnit fCU;

	@Before
	public void setUp() throws Exception {
		fProject= pts.getProject();

		Map<String, String> options= fProject.getOptions(false);
		String compliance= JavaCore.VERSION_1_4;
		JavaModelUtil.setComplianceOptions(options, compliance);
		JavaModelUtil.setDefaultClassfileOptions(options, compliance);
		fProject.setOptions(options);

		fMainRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
		IPackageFragment fragment= fMainRoot.createPackageFragment("org.eclipse.jdt.ui.test", true, null);
		String str= """
			package org.eclipse.jdt.ui.test;
			public class Main {
			    public class MainInner {
			    }
			    public static void main(String[] args) {
			        new Main() {
			           \s
			        }.hashCode();
			    }
			}
			""";
		fCU= fragment.createCompilationUnit("Main.java", str, true, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, pts.getDefaultClasspath());
	}

	@Test
	public void exportCu() throws Exception {
		JarPackageData data= createJarPackageData();

		data.setElements(new Object[] { fCU });
		data.setExportClassFiles(true);

		ArrayList<String> entries;
		try (ZipFile jar= createArchive(data)) {
			entries= getSortedEntries(jar);
		}
		List<String> expected= Arrays.asList("META-INF/MANIFEST.MF\n", "org/eclipse/jdt/ui/test/Main$1.class\n", "org/eclipse/jdt/ui/test/Main$MainInner.class\n", "org/eclipse/jdt/ui/test/Main.class\n");
		assertEquals(expected.toString(), entries.toString());
	}

	@Test
	public void exportFile() throws Exception {
		JarPackageData data= createJarPackageData();

		data.setElements(new Object[] { fCU.getResource() });
		data.setExportClassFiles(true);

		ArrayList<String> entries;
		try (ZipFile jar= createArchive(data)) {
			entries= getSortedEntries(jar);
		}
		List<String> expected= Arrays.asList("META-INF/MANIFEST.MF\n", "org/eclipse/jdt/ui/test/Main$1.class\n", "org/eclipse/jdt/ui/test/Main$MainInner.class\n", "org/eclipse/jdt/ui/test/Main.class\n");

		assertEquals(expected.toString(), entries.toString());
	}

	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=229052
	@Test
	public void externalClassFolder() throws Exception {
		JarPackageData data= createJarPackageData();

		File classFolder= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/externalClassFolder/"));//$NON-NLS-1$
		assertNotNull("class folder not found", classFolder);//$NON-NLS-1$
		assertTrue("class folder not found", classFolder.exists());//$NON-NLS-1$

		IPackageFragmentRoot externalRoot= JavaProjectHelper.addLibrary(fProject, Path.fromOSString(classFolder.getPath()), null, null);

		data.setElements(new Object[] { fCU.getResource(), externalRoot });
		data.setExportClassFiles(true);

		ArrayList<String> entries;
		try (ZipFile jar= createArchive(data)) {
			entries= getSortedEntries(jar);
		}
		List<String> expected= Arrays.asList("META-INF/MANIFEST.MF\n", "org/eclipse/jdt/ui/test/Main$1.class\n", "org/eclipse/jdt/ui/test/Main$MainInner.class\n", "org/eclipse/jdt/ui/test/Main.class\n");
		assertEquals(expected.toString(), entries.toString());
	}

	private JarPackageData createJarPackageData() {
		JarPackageData data= new JarPackageData();
		data.setJarLocation(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(getName() + ".jar"));
		data.setBuildIfNeeded(true);
		data.setOverwrite(true);
		return data;
	}

	private String getName() {
		return tn.getMethodName();
	}

	private static ZipFile createArchive(JarPackageData data) throws Exception, CoreException {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		IJarExportRunnable op= data.createJarExportRunnable(window.getShell());
		window.run(false, false, op);

		IStatus status= op.getStatus();
		if (status.getSeverity() == IStatus.ERROR)
			throw new CoreException(status);

		return JarPackagerUtil.createZipFile(data.getJarLocation());
	}

	private static ArrayList<String> getSortedEntries(ZipFile jar) {
		ArrayList<String> entries= new ArrayList<>();
		for (Enumeration<? extends ZipEntry> entriesEnum= jar.entries(); entriesEnum.hasMoreElements(); ) {
			ZipEntry entry= entriesEnum.nextElement();
			entries.add(entry.getName() + "\n");
		}
		Collections.sort(entries);
		return entries;
	}
}
