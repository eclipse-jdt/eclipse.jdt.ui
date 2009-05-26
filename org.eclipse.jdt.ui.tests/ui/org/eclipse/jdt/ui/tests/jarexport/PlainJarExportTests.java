/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.jarexport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;


public class PlainJarExportTests extends TestCase {

	private static final Class THIS= PlainJarExportTests.class;

	public static Test suite() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	private IJavaProject fProject;
	private IPackageFragmentRoot fMainRoot;
	private ICompilationUnit fCU;

	protected void setUp() throws Exception {
		fProject= ProjectTestSetup.getProject();

		Map options= fProject.getOptions(false);
		String compliance= JavaCore.VERSION_1_4;
		JavaModelUtil.setComplianceOptions(options, compliance);
		JavaModelUtil.setDefaultClassfileOptions(options, compliance);
		fProject.setOptions(options);

		fMainRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
		IPackageFragment fragment= fMainRoot.createPackageFragment("org.eclipse.jdt.ui.test", true, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.eclipse.jdt.ui.test;\n");
		buf.append("public class Main {\n");
		buf.append("    public class MainInner {\n");
		buf.append("    }\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        new Main() {\n");
		buf.append("            \n");
		buf.append("        }.hashCode();\n");
		buf.append("    }\n");
		buf.append("}\n");
		fCU= fragment.createCompilationUnit("Main.java", buf.toString(), true, null);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, ProjectTestSetup.getDefaultClasspath());
	}
	
	
	public void testExportCu() throws Exception {
		JarPackageData data= createJarPackageData();
		
		data.setElements(new Object[] { fCU });
		data.setExportClassFiles(true);
		
		ZipFile jar= createArchive(data);
		ArrayList entries= getSortedEntries(jar);
		jar.close();
		List expected= Arrays.asList(new String[] {
				"META-INF/MANIFEST.MF\n",
				"org/eclipse/jdt/ui/test/Main$1.class\n",
				"org/eclipse/jdt/ui/test/Main$MainInner.class\n",
				"org/eclipse/jdt/ui/test/Main.class\n",
		});
		assertEquals(expected.toString(), entries.toString());
	}

	public void testExportFile() throws Exception {
		JarPackageData data= createJarPackageData();

		data.setElements(new Object[] { fCU.getResource() });
		data.setExportClassFiles(true);
		
		ZipFile jar= createArchive(data);
		ArrayList entries= getSortedEntries(jar);
		jar.close();
		List expected= Arrays.asList(new String[] { "META-INF/MANIFEST.MF\n", "org/eclipse/jdt/ui/test/Main$1.class\n",
				"org/eclipse/jdt/ui/test/Main$MainInner.class\n",
				"org/eclipse/jdt/ui/test/Main.class\n", });

		assertEquals(expected.toString(), entries.toString());
	}
	
	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=229052
	public void testExternalClassFolder() throws Exception {
		JarPackageData data= createJarPackageData();

		File classFolder= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/externalClassFolder/"));//$NON-NLS-1$
		assertTrue("class folder not found", classFolder != null && classFolder.exists());//$NON-NLS-1$

		IPackageFragmentRoot externalRoot= JavaProjectHelper.addLibrary(fProject, Path.fromOSString(classFolder.getPath()), null, null);

		data.setElements(new Object[] { fCU.getResource(), externalRoot });
		data.setExportClassFiles(true);

		ZipFile jar= createArchive(data);
		ArrayList entries= getSortedEntries(jar);
		jar.close();
		List expected= Arrays.asList(new String[] {
				"META-INF/MANIFEST.MF\n",
				"org/eclipse/jdt/ui/test/Main$1.class\n",
				"org/eclipse/jdt/ui/test/Main$MainInner.class\n",
				"org/eclipse/jdt/ui/test/Main.class\n", });
		assertEquals(expected.toString(), entries.toString());
	}

	private JarPackageData createJarPackageData() {
		JarPackageData data= new JarPackageData();
		data.setJarLocation(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(getName() + ".jar"));
		data.setBuildIfNeeded(true);
		data.setOverwrite(true);
		return data;
	}

	private static ZipFile createArchive(JarPackageData data) throws Exception, CoreException {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	
		IJarExportRunnable op= data.createJarExportRunnable(window.getShell());
		window.run(false, false, op);
	
		IStatus status= op.getStatus();
		if (status.getSeverity() == IStatus.ERROR)
			throw new CoreException(status);
	
		return JarPackagerUtil.getArchiveFile(data.getJarLocation());
	}

	private static ArrayList getSortedEntries(ZipFile jar) {
		ArrayList entries= new ArrayList();
		for (Enumeration entriesEnum= jar.entries(); entriesEnum.hasMoreElements(); ) {
			ZipEntry entry= (ZipEntry) entriesEnum.nextElement();
			entries.add(entry.getName() + "\n");
		}
		Collections.sort(entries);
		return entries;
	}
}
