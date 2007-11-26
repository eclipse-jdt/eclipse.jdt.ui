/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.jarexport;

import java.io.File;
import java.util.Map;
import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarBuilder;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackageWizardPage;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class FatJarExportTests extends TestCase {

	private static final Class THIS= FatJarExportTests.class;

	public static Test suite() {
		return allTests();
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	private IJavaProject fProject;
	private IPackageFragmentRoot fMainRoot;

	/**
	 * {@inheritDoc}
	 */
	protected void setUp() throws Exception {
		fProject= ProjectTestSetup.getProject();

		Map options= fProject.getOptions(false);
		String compliance= JavaCore.VERSION_1_4;
		JavaModelUtil.setCompilanceOptions(options, compliance);
		JavaModelUtil.setDefaultClassfileOptions(options, compliance); // complete compliance options
		fProject.setOptions(options);

		fMainRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
		IPackageFragment fragment= fMainRoot.createPackageFragment("org.eclipse.jdt.ui.test", true, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.eclipse.jdt.ui.test;\n");
		buf.append("import mylib.Foo;\n");
		buf.append("public class Main {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        new Foo();\n");
		buf.append("        new Foo.FooInner();\n");
		buf.append("        new Foo.FooInner.FooInnerInner();\n");
		buf.append("    }\n");
		buf.append("}\n");
		fragment.createCompilationUnit("Main.java", buf.toString(), true, null);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, ProjectTestSetup.getDefaultClasspath());
	}

	private static String getFooContent() {
		StringBuffer buf= new StringBuffer();
		buf.append("package mylib;\n");
		buf.append("public class Foo {\n");
		buf.append("    public Foo() {\n");
		buf.append("        System.out.println(\"created \" + Foo.class.getName());\n");
		buf.append("    }\n");
		buf.append("    public static class FooInner {\n");
		buf.append("        public static class FooInnerInner {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		return buf.toString();
	}

	private static void createAndRunFatJar(IJavaProject project, String archiveName) throws Exception, CoreException {
		// create jar and check contents
		IPath jarLocation= assertFatJarExport(project, archiveName + ".jar");

		// run newly generated jar and check stdout
		String stdout= runJar(project, jarLocation.toOSString());
		// normalize EndOfLine to \n
		stdout= stdout.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
		// check for successful call of Foo
		String expected= "created mylib.Foo\n";
		assertEquals(expected, stdout);
	}

	private static IPath assertFatJarExport(IJavaProject project, String archiveName) throws Exception {
		//create class files
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);

		IMarker[] markers= ResourcesPlugin.getWorkspace().getRoot().findMarkers(null, true, IResource.DEPTH_INFINITE);
		for (int i= 0; i < markers.length; i++) {
			IMarker marker= markers[i];
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
				assertTrue((String) marker.getAttribute(IMarker.MESSAGE), false);
			}
		}

		//create data
		JarPackageData data= createJarPackageData(project, archiveName);

		//create archive
		ZipFile generatedArchive= createArchive(data);

		//assert archive content as expected
		assertNotNull(generatedArchive);
		assertNotNull(generatedArchive.getEntry("org/eclipse/jdt/ui/test/Main.class"));
		assertNotNull(generatedArchive.getEntry("mylib/Foo.class"));
		assertNotNull(generatedArchive.getEntry("mylib/Foo$FooInner.class"));
		assertNotNull(generatedArchive.getEntry("mylib/Foo$FooInner$FooInnerInner.class"));

		return data.getJarLocation();
	}

	private static JarPackageData createJarPackageData(IJavaProject project, String archiveName) throws CoreException {
		JarPackageData data= new JarPackageData();
		data.setJarBuilder(new FatJarBuilder());

		IPath destination= ResourcesPlugin.getWorkspace().getRoot().getLocation().append(archiveName);
		data.setJarLocation(destination);

		ILaunchConfiguration launchConfig= createTempLaunchConfig(project);

		MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, 0, "", null);
		Object[] children= FatJarPackageWizardPage.getSelectedElementsWithoutContainedChildren(launchConfig, data, new BusyIndicatorRunnableContext(), status);
		assertTrue(getProblems(status), status.getSeverity() == IStatus.OK || status.getSeverity() == IStatus.INFO);
		data.setElements(children);

		return data;
	}

	private static String getProblems(MultiStatus status) {
		StringBuffer result= new StringBuffer();

		IStatus[] children= status.getChildren();
		for (int i= 0; i < children.length; i++) {
			result.append(children[i].getMessage()).append("\n");
		}

		return result.toString();
	}

	/*
	 *  From org.eclipse.jdt.internal.debug.ui.launcher.JavaApplicationLaunchShortcut
	 *  
	 *  For internal use only (testing), clients must not call.
	 */
	public static ILaunchConfiguration createTempLaunchConfig(IJavaProject project) {
		String projectName= project.getElementName();

		String configname= "fatjar_cfg_eraseme_" + projectName; //$NON-NLS-1$
		ILaunchConfiguration config= null;
		ILaunchConfigurationWorkingCopy wc= null;
		try {
			ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType configType= launchManager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
			wc= configType.newInstance(null, launchManager.generateUniqueLaunchConfigurationNameFrom(configname));
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return null;
		}

		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "org.eclipse.jdt.ui.test.Main");
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
		try {
			config= wc.doSave();
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}

		return config;
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

	private static String runJar(IJavaProject project, String jarPath) throws CoreException {

		IVMInstall vmInstall= JavaRuntime.getVMInstall(project);
		if (vmInstall == null)
			vmInstall= JavaRuntime.getDefaultVMInstall();

		if (vmInstall == null)
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Could not find a VM Install"));

		IVMRunner vmRunner= vmInstall.getVMRunner(ILaunchManager.RUN_MODE);
		if (vmRunner == null)
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Could not create a VM Runner"));

		VMRunnerConfiguration vmConfig= new VMRunnerConfiguration("-jar", new String[] {});
		vmConfig.setWorkingDirectory(new File(jarPath).getParent());
		vmConfig.setProgramArguments(new String[] { jarPath });

		ILaunch launch= new Launch(null, ILaunchManager.RUN_MODE, null);
		vmRunner.run(vmConfig, launch, null);

		IProcess[] processes= launch.getProcesses();
		if (processes.length == 0)
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Could not launch jar"));

		int timeout= 30;
		while (timeout > 0 && !processes[0].isTerminated()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}

			timeout--;
		}
		if (!processes[0].isTerminated())
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Process did not terminate within timeout"));

		int exitCode= processes[0].getExitValue();

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		if (exitCode != 0) {
			String stdout= processes[0].getStreamsProxy().getOutputStreamMonitor().getContents();
			String errout= processes[0].getStreamsProxy().getErrorStreamMonitor().getContents();
			
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Run failed: exitcode=" + exitCode + ", stdout=[" + stdout + "], stderr=[" + errout + "]"));
		} else {
			return processes[0].getStreamsProxy().getOutputStreamMonitor().getContents();
		}
	}

	public void testExportSameSrcRoot() throws Exception {
		IPackageFragment pack= fMainRoot.createPackageFragment("mylib", true, null);
		try {
			pack.createCompilationUnit("Foo.java", getFooContent(), true, null);

			createAndRunFatJar(fProject, getName() + ".jar");
		} finally {
			pack.delete(true, null);
		}
	}

	public void testExportSrcRootWithOutputFolder() throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "other", new IPath[0], new IPath[0], "otherout");
		try {
			IPackageFragment pack= root.createPackageFragment("mylib", true, null);
			pack.createCompilationUnit("Foo.java", getFooContent(), true, null);

			createAndRunFatJar(fProject, getName() + ".jar");
		} finally {
			JavaProjectHelper.removeSourceContainer(fProject, root.getElementName());
		}
	}

	public void testExportOtherSrcRoot() throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "other");
		try {
			IPackageFragment pack= root.createPackageFragment("mylib", true, null);
			pack.createCompilationUnit("Foo.java", getFooContent(), true, null);

			createAndRunFatJar(fProject, getName() + ".jar");
		} finally {
			JavaProjectHelper.removeSourceContainer(fProject, root.getElementName());
		}
	}

	public void testExportOtherProject() throws Exception {
		IJavaProject otherProject= JavaProjectHelper.createJavaProject("OtherProject", "bin");
		try {
			otherProject.setRawClasspath(ProjectTestSetup.getDefaultClasspath(), null);

			IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(otherProject, "other");
			IPackageFragment pack= root.createPackageFragment("mylib", true, null);
			pack.createCompilationUnit("Foo.java", getFooContent(), true, null);

			JavaProjectHelper.addRequiredProject(fProject, otherProject);

			createAndRunFatJar(fProject, getName() + ".jar");
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, otherProject.getProject().getFullPath());
			JavaProjectHelper.delete(otherProject);
		}
	}

	public void testExportInternalLib() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_STDOUT);
		IPackageFragmentRoot root= JavaProjectHelper.addLibraryWithImport(fProject, Path.fromOSString(lib.getPath()), null, null);

		try {
			createAndRunFatJar(fProject, getName() + ".jar");
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, root.getPath());
		}
	}

	public void testExportExternalLib() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_STDOUT);
		IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, Path.fromOSString(lib.getPath()));

		try {
			createAndRunFatJar(fProject, getName() + ".jar");
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, root.getPath());
		}
	}

	public void testClassFolder() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_STDOUT);

		IPackageFragmentRoot root= JavaProjectHelper.addClassFolderWithImport(fProject, "cf", null, null, lib);
		try {
			createAndRunFatJar(fProject, getName() + ".jar");
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, root.getPath());
		}
	}

	public void testVariable() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_STDOUT);
		JavaCore.setClasspathVariable("MYLIB", Path.fromOSString(lib.getPath()), null);

		JavaProjectHelper.addVariableEntry(fProject, new Path("MYLIB"), null, null);
		try {
			createAndRunFatJar(fProject, getName() + ".jar");
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, new Path("MYLIB"));
		}
	}

	public void testSignedLibs() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_SIG);
		IPackageFragmentRoot root= JavaProjectHelper.addLibraryWithImport(fProject, Path.fromOSString(lib.getPath()), null, null);

		try {
			createAndRunFatJar(fProject, getName());
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, root.getPath());
		}
	}

	public void xtestNoMylib2() throws Exception {
		try {
			createAndRunFatJar(fProject, getName() + ".jar");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
