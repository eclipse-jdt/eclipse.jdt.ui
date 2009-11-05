/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 213638 [jar exporter] create ANT build file for current settings
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 220257 [jar application] ANT build file does not create Class-Path Entry in Manifest
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 243163 [jar exporter] export directory entries in "Runnable JAR File"
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 219530 [jar application] add Jar-in-Jar ClassLoader option
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262766 [jar exporter] ANT file for Jar-in-Jar option contains relative path to jar-rsrc-loader.zip
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262763 [jar exporter] remove Built-By attribute in ANT files from Fat JAR Exporter
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.jarexport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

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
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarAntExporter;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackageWizardPage;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarRsrcUrlBuilder;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackageWizardPage.CopyLibraryHandler;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackageWizardPage.ExtractLibraryHandler;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackageWizardPage.LibraryHandler;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackageWizardPage.PackageLibraryHandler;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

public class FatJarExportTests extends TestCase {

	private static final Class THIS= FatJarExportTests.class;

	private static final int JAVA_RUN_TIMEOUT= 50; // 10th of a second
	
	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
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
		JavaModelUtil.setComplianceOptions(options, compliance);
		JavaModelUtil.setDefaultClassfileOptions(options, compliance); // complete compliance options
		fProject.setOptions(options);

		fMainRoot= JavaProjectHelper.addSourceContainer(fProject, "src"); //$NON-NLS-1$
		IPackageFragment fragment= fMainRoot.createPackageFragment("org.eclipse.jdt.ui.test", true, null); //$NON-NLS-1$
		StringBuffer buf= new StringBuffer();
		buf.append("package org.eclipse.jdt.ui.test;\n"); //$NON-NLS-1$
		buf.append("import mylib.Foo;\n"); //$NON-NLS-1$
		buf.append("public class Main {\n"); //$NON-NLS-1$
		buf.append("    public static void main(String[] args) {\n"); //$NON-NLS-1$
		buf.append("        new Foo();\n"); //$NON-NLS-1$
		buf.append("        new Foo.FooInner();\n"); //$NON-NLS-1$
		buf.append("        new Foo.FooInner.FooInnerInner();\n"); //$NON-NLS-1$
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		fragment.createCompilationUnit("Main.java", buf.toString(), true, null); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, ProjectTestSetup.getDefaultClasspath());
	}

	private static String getFooContent() {
		StringBuffer buf= new StringBuffer();
		buf.append("package mylib;\n"); //$NON-NLS-1$
		buf.append("public class Foo {\n"); //$NON-NLS-1$
		buf.append("    public Foo() {\n"); //$NON-NLS-1$
		buf.append("        System.out.println(\"created \" + Foo.class.getName());\n"); //$NON-NLS-1$
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("    public static class FooInner {\n"); //$NON-NLS-1$
		buf.append("        public static class FooInnerInner {\n"); //$NON-NLS-1$
		buf.append("        }\n"); //$NON-NLS-1$
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		return buf.toString();
	}

	private static JarPackageData createAndRunFatJar(IJavaProject project, String testName, boolean compressJar, LibraryHandler libraryHandler) throws Exception, CoreException {
		JarPackageData data= null;
		// create jar and check contents
		switch (libraryHandler.getID()) {
			case ExtractLibraryHandler.ID: {
				data= assertFatJarExport(project, testName, compressJar, libraryHandler);
				break;
			}
			case PackageLibraryHandler.ID: {
				data= assertFatJarWithLoaderExport(project, testName, compressJar, libraryHandler);
				break;
			}
			case CopyLibraryHandler.ID: {
				data= assertFatJarWithSubfolderExport(project, testName, compressJar, libraryHandler);
				break;
			}
			default: {
				fail("invalid library handling '" + libraryHandler.getID() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
		}

		// run newly generated jar and check stdout
		String stdout= runJar(project, data.getJarLocation().toOSString());
		// normalize EndOfLine to \n
		stdout= stdout.replaceAll("\r\n", "\n").replaceAll("\r", "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		// check for successful call of Foo
		String expected= "created mylib.Foo\n"; //$NON-NLS-1$
		assertEquals(expected, stdout);
		
		return data;
	}

	private static JarPackageData assertFatJarExport(IJavaProject project, String testName, boolean compressJar, LibraryHandler libraryHandler) throws Exception {
		//create class files
		buildProject();

		//create data
		JarPackageData data= createJarPackageData(project, testName, libraryHandler);

		// set compression
		data.setCompress(compressJar);
		
		//create archive
		ZipFile generatedArchive= createArchive(data);

		//assert archive content as expected
		assertNotNull(generatedArchive);
		assertNotNull(generatedArchive.getEntry("org/eclipse/jdt/ui/test/Main.class")); //$NON-NLS-1$
		assertNotNull(generatedArchive.getEntry("mylib/Foo.class")); //$NON-NLS-1$
		assertNotNull(generatedArchive.getEntry("mylib/Foo$FooInner.class")); //$NON-NLS-1$
		assertNotNull(generatedArchive.getEntry("mylib/Foo$FooInner$FooInnerInner.class")); //$NON-NLS-1$
		
		generatedArchive.close();

		MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, 0, "", null); //$NON-NLS-1$
		
		FatJarAntExporter antExporter= libraryHandler.getAntExporter(antScriptLocation(testName), data.getAbsoluteJarLocation(), createTempLaunchConfig(project));
		antExporter.run(status);
		assertTrue(getProblems(status), status.getSeverity() == IStatus.OK || status.getSeverity() == IStatus.INFO);
		
		return data;
	}

	private static JarPackageData assertFatJarWithLoaderExport(IJavaProject project, String testName, boolean compressJar, LibraryHandler libraryHandler) throws Exception {
		//create class files
		buildProject();

		//create data with Jar-in-Jar Loader
		JarPackageData data= createJarPackageData(project, testName, libraryHandler);

		// set compression
		data.setCompress(compressJar);

		//create archive
		ZipFile generatedArchive= createArchive(data);

		//assert archive content as expected
		assertNotNull(generatedArchive);
		assertNotNull(generatedArchive.getEntry("org/eclipse/jdt/ui/test/Main.class")); //$NON-NLS-1$
		// get loader entry
		ZipEntry loaderClassEntry= generatedArchive.getEntry("org/eclipse/jdt/internal/jarinjarloader/JarRsrcLoader.class"); //$NON-NLS-1$
		assertNotNull(loaderClassEntry);
		// check version of class file JarRsrcLoader (jdk 1.3.1 = version 45.3)
		InputStream in = generatedArchive.getInputStream(loaderClassEntry);
		int magic = 0;
		for (int i= 0; i < 4; i++)
			magic = (magic << 8) + in.read();
		int minorVersion = ((in.read() << 8) + in.read());
		int majorVersion = ((in.read() << 8) + in.read());
		in.close();
		assertEquals("loader is a class file", 0xCAFEBABE, magic); //$NON-NLS-1$
		assertEquals("loader compiled with JDK 1.3.1", "45.3", majorVersion + "." + minorVersion); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		generatedArchive.close();

		MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, 0, "", null); //$NON-NLS-1$
		
		FatJarAntExporter antExporter= libraryHandler.getAntExporter(antScriptLocation(testName), data.getAbsoluteJarLocation(), createTempLaunchConfig(project));
		antExporter.run(status);
		assertTrue(getProblems(status), status.getSeverity() == IStatus.OK || status.getSeverity() == IStatus.INFO);
		
		// check that jar-rsrc-loader.zip file was created next to build.xml
		IPath zipLocation= antScriptLocation(testName).removeLastSegments(1).append(FatJarRsrcUrlBuilder.JAR_RSRC_LOADER_ZIP);
		assertTrue("loader zip missing: " + zipLocation.toOSString(), zipLocation.toFile().exists());

		return data;
	}

	private static JarPackageData assertFatJarWithSubfolderExport(IJavaProject project, String testName, boolean compressJar, LibraryHandler libraryHandler) throws Exception {
		//create class files
		buildProject();

		//create data with Jar-in-Jar Loader
		JarPackageData data= createJarPackageData(project, testName, libraryHandler);

		// set compression
		data.setCompress(compressJar);

		//create archive
		ZipFile generatedArchive= createArchive(data);

		//assert archive content as expected
		assertNotNull(generatedArchive);
		assertNotNull(generatedArchive.getEntry("org/eclipse/jdt/ui/test/Main.class")); //$NON-NLS-1$

		// check for libraries sub-folder
		File jarFile= new File(generatedArchive.getName());
		String subFolderName= jarFile.getName().replaceFirst("^(.*)[.]jar$", "$1_lib"); //$NON-NLS-1$//$NON-NLS-2$
		File subFolderDir= new File(jarFile.getParentFile(), subFolderName);
		assertTrue("actual: '" + subFolderDir.toString() + "'", subFolderDir.isDirectory()); //$NON-NLS-1$//$NON-NLS-2$

		generatedArchive.close();

		MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, 0, "", null); //$NON-NLS-1$

		FatJarAntExporter antExporter= libraryHandler.getAntExporter(antScriptLocation(testName), data.getAbsoluteJarLocation(), createTempLaunchConfig(project));
		antExporter.run(status);
		assertTrue(getProblems(status), status.getSeverity() == IStatus.OK || status.getSeverity() == IStatus.INFO);

		return data;
	}

	private static void buildProject() throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);

		IMarker[] markers= ResourcesPlugin.getWorkspace().getRoot().findMarkers(null, true, IResource.DEPTH_INFINITE);
		for (int i= 0; i < markers.length; i++) {
			IMarker marker= markers[i];
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
				assertTrue((String) marker.getAttribute(IMarker.MESSAGE), false);
			}
		}
	}

	
	private static IPath antScriptLocation(String testName) {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().append("build_" + testName + ".xml"); //$NON-NLS-1$//$NON-NLS-2$
	}

	private static JarPackageData createJarPackageData(IJavaProject project, String testName, LibraryHandler libraryHandler) throws CoreException {
		JarPackageData data= new JarPackageData();
		data.setOverwrite(true);
		data.setIncludeDirectoryEntries(true);
		
		IPath destination= ResourcesPlugin.getWorkspace().getRoot().getLocation().append(testName + ".jar"); //$NON-NLS-1$
		data.setJarLocation(destination);

		ILaunchConfiguration launchConfig= createTempLaunchConfig(project);

		MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, 0, "", null); //$NON-NLS-1$
		Object[] children= FatJarPackageWizardPage.getSelectedElementsWithoutContainedChildren(launchConfig, data, new BusyIndicatorRunnableContext(), status);
		assertTrue(getProblems(status), status.getSeverity() == IStatus.OK || status.getSeverity() == IStatus.INFO);
		data.setElements(children);
		
		data.setJarBuilder(libraryHandler.getBuilder(data));
		
		return data;
	}

	private static String getProblems(MultiStatus status) {
		StringBuffer result= new StringBuffer();

		IStatus[] children= status.getChildren();
		for (int i= 0; i < children.length; i++) {
			result.append(children[i].getMessage()).append("\n"); //$NON-NLS-1$
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
			wc= configType.newInstance(null, launchManager.generateLaunchConfigurationName(configname));
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return null;
		}

		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "org.eclipse.jdt.ui.test.Main"); //$NON-NLS-1$
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
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Could not find a VM Install")); //$NON-NLS-1$

		IVMRunner vmRunner= vmInstall.getVMRunner(ILaunchManager.RUN_MODE);
		if (vmRunner == null)
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Could not create a VM Runner")); //$NON-NLS-1$

		VMRunnerConfiguration vmConfig= new VMRunnerConfiguration("-jar", new String[] {}); //$NON-NLS-1$
		vmConfig.setWorkingDirectory(new File(jarPath).getParent());
		vmConfig.setProgramArguments(new String[] { jarPath });

		ILaunch launch= new Launch(null, ILaunchManager.RUN_MODE, null);
		vmRunner.run(vmConfig, launch, null);

		IProcess[] processes= launch.getProcesses();
		if (processes.length == 0)
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Could not launch jar")); //$NON-NLS-1$

		int timeout= JAVA_RUN_TIMEOUT;
		while (timeout > 0 && !processes[0].isTerminated()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}

			timeout--;
		}
		if (!processes[0].isTerminated())
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Process did not terminate within timeout")); //$NON-NLS-1$

		int exitCode= processes[0].getExitValue();

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		if (exitCode != 0) {
			String stdout= processes[0].getStreamsProxy().getOutputStreamMonitor().getContents();
			String errout= processes[0].getStreamsProxy().getErrorStreamMonitor().getContents();
			
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Run failed: exitcode=" + exitCode + ", stdout=[" + stdout + "], stderr=[" + errout + "]")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		} else {
			return processes[0].getStreamsProxy().getOutputStreamMonitor().getContents();
		}
	}

	private static void assertAntScript(JarPackageData data, IPath antScriptLocation, LibraryHandler libraryHandler, String[] filesets, String[] zipfilesets) throws Exception {
		String archiveName= data.getAbsoluteJarLocation().lastSegment();
		switch (libraryHandler.getID()) {
			case ExtractLibraryHandler.ID: {
				assertAntScriptExtract(archiveName, antScriptLocation, filesets, zipfilesets);
				break;
			}
			case PackageLibraryHandler.ID: {
				assertAntScriptPackage(archiveName, antScriptLocation, filesets, zipfilesets);
				break;
			}
			case CopyLibraryHandler.ID: {
				assertAntScriptCopy(archiveName, antScriptLocation, filesets, zipfilesets);
				break;
			}
			default: {
				fail("unknown library handling '" + libraryHandler.getID() + "'"); //$NON-NLS-1$//$NON-NLS-2$
				break;
			}
		}
	}

	private static void assertAntScriptCopy(String archiveName, IPath antScriptLocation, String[] filesets, String[] zipfilesets) throws Exception {
		String subfolderName= archiveName.replaceFirst("^(.*)[.]jar$", "$1_lib"); //$NON-NLS-1$//$NON-NLS-2$
			
		String projectNameValue= "Create Runnable Jar for Project TestSetupProject"; //$NON-NLS-1$
		projectNameValue+= " with libraries in sub-folder"; //$NON-NLS-1$

		Element xmlProject= readXML(antScriptLocation);
		assertEquals("project", xmlProject.getNodeName()); //$NON-NLS-1$
		assertEquals("create_run_jar", xmlProject.getAttribute("default")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(projectNameValue, xmlProject.getAttribute("name")); //$NON-NLS-1$

		Element xmlTarget= (Element)xmlProject.getElementsByTagName("target").item(0); //$NON-NLS-1$
		assertEquals("create_run_jar", xmlTarget.getAttribute("name")); //$NON-NLS-1$//$NON-NLS-2$

		Element xmlJar= (Element)xmlTarget.getElementsByTagName("jar").item(0); //$NON-NLS-1$
		assertTrue("actual: " + xmlJar.getAttribute("destfile"), xmlJar.getAttribute("destfile").endsWith(archiveName)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		Element xmlManifest= (Element)xmlJar.getElementsByTagName("manifest").item(0); //$NON-NLS-1$

		Element xmlAttribute1= (Element)xmlManifest.getElementsByTagName("attribute").item(0); //$NON-NLS-1$
		assertEquals("Main-Class", xmlAttribute1.getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("org.eclipse.jdt.ui.test.Main", xmlAttribute1.getAttribute("value")); //$NON-NLS-1$ //$NON-NLS-2$

		Element xmlAttribute2= (Element)xmlManifest.getElementsByTagName("attribute").item(1); //$NON-NLS-1$
		assertEquals("Class-Path", xmlAttribute2.getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("actual value: " + xmlAttribute2.getAttribute("value"), xmlAttribute2.getAttribute("value").startsWith(".")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		NodeList xmlFilesets= xmlJar.getElementsByTagName("fileset"); //$NON-NLS-1$
		assertEquals(filesets.length, xmlFilesets.getLength());

		for (int i= 0; i < xmlFilesets.getLength(); i++) {
			String dir= ((Element)xmlFilesets.item(i)).getAttribute("dir"); //$NON-NLS-1$
			boolean found= false;
			for (int j= 0; j < filesets.length; j++) {
				if (dir.endsWith(filesets[j])) {
					found= true;
					break;
				}
			}
			assertTrue("found fileset: '" + dir + "'", found); //$NON-NLS-1$//$NON-NLS-2$
		}

		Element xmlDelete= (Element)xmlTarget.getElementsByTagName("delete").item(0); //$NON-NLS-1$
		assertTrue("actual: " + xmlDelete.getAttribute("dir"), xmlDelete.getAttribute("dir").endsWith(subfolderName)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		Element xmlMkdir= (Element)xmlTarget.getElementsByTagName("mkdir").item(0); //$NON-NLS-1$
		assertTrue("actual: " + xmlMkdir.getAttribute("dir"), xmlMkdir.getAttribute("dir").endsWith(subfolderName)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		NodeList xmlCopies= xmlTarget.getElementsByTagName("copy"); //$NON-NLS-1$
		assertEquals(zipfilesets.length, xmlCopies.getLength());

		for (int i= 0; i < xmlCopies.getLength(); i++) {
			String absLibPath= ((Element)xmlCopies.item(i)).getAttribute("file"); //$NON-NLS-1$
			String libName= new File(absLibPath).getName();
			boolean found= false;
			for (int j= 0; j < zipfilesets.length; j++) {
				if (libName.equals(zipfilesets[j])) {
					found= true;
					break;
				}
			}
			assertTrue("find zipfileset lib: '" + libName + "'", found); //$NON-NLS-1$  //$NON-NLS-2$
		}
		
	}

	private static void assertAntScriptPackage(String archiveName, IPath antScriptLocation, String[] filesets, String[] zipfilesets) throws Exception {
		String projectNameValue= "Create Runnable Jar for Project TestSetupProject"; //$NON-NLS-1$
		projectNameValue+= " with Jar-in-Jar Loader"; //$NON-NLS-1$

		Element xmlProject= readXML(antScriptLocation);
		assertEquals("project", xmlProject.getNodeName()); //$NON-NLS-1$
		assertEquals("create_run_jar", xmlProject.getAttribute("default")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(projectNameValue, xmlProject.getAttribute("name")); //$NON-NLS-1$

		Element xmlTarget= (Element)xmlProject.getElementsByTagName("target").item(0); //$NON-NLS-1$
		assertEquals("create_run_jar", xmlTarget.getAttribute("name")); //$NON-NLS-1$//$NON-NLS-2$

		Element xmlJar= (Element)xmlTarget.getElementsByTagName("jar").item(0); //$NON-NLS-1$
		assertTrue("actual: " + xmlJar.getAttribute("destfile"), xmlJar.getAttribute("destfile").endsWith(archiveName)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		Element xmlManifest= (Element)xmlJar.getElementsByTagName("manifest").item(0); //$NON-NLS-1$

		Element xmlAttribute1= (Element)xmlManifest.getElementsByTagName("attribute").item(0); //$NON-NLS-1$
		assertEquals("Main-Class", xmlAttribute1.getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader", xmlAttribute1.getAttribute("value")); //$NON-NLS-1$ //$NON-NLS-2$

		Element xmlAttribute2= (Element)xmlManifest.getElementsByTagName("attribute").item(1); //$NON-NLS-1$
		assertEquals("Rsrc-Main-Class", xmlAttribute2.getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("org.eclipse.jdt.ui.test.Main", xmlAttribute2.getAttribute("value")); //$NON-NLS-1$ //$NON-NLS-2$

		Element xmlAttribute3= (Element)xmlManifest.getElementsByTagName("attribute").item(2); //$NON-NLS-1$
		assertEquals("Class-Path", xmlAttribute3.getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(".", xmlAttribute3.getAttribute("value")); //$NON-NLS-1$ //$NON-NLS-2$

		Element xmlAttribute4= (Element)xmlManifest.getElementsByTagName("attribute").item(3); //$NON-NLS-1$
		assertEquals("Rsrc-Class-Path", xmlAttribute4.getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("actual value: " + xmlAttribute4.getAttribute("value"), xmlAttribute4.getAttribute("value").startsWith("./")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		NodeList xmlFilesets= xmlJar.getElementsByTagName("fileset"); //$NON-NLS-1$
		assertEquals(filesets.length, xmlFilesets.getLength());

		NodeList xmlZipfilesets= xmlJar.getElementsByTagName("zipfileset"); //$NON-NLS-1$
		assertEquals(zipfilesets.length + 1, xmlZipfilesets.getLength());

		for (int i= 0; i < xmlFilesets.getLength(); i++) {
			String dir= ((Element)xmlFilesets.item(i)).getAttribute("dir"); //$NON-NLS-1$
			boolean found= false;
			for (int j= 0; j < filesets.length; j++) {
				if (dir.endsWith(filesets[j])) {
					found= true;
					break;
				}
			}
			assertTrue("found fileset: '" + dir + "'", found); //$NON-NLS-1$//$NON-NLS-2$
		}

		for (int i= 0; i < xmlZipfilesets.getLength(); i++) {
			String libName= ((Element)xmlZipfilesets.item(i)).getAttribute("includes"); //$NON-NLS-1$
			boolean found= false;
			if (libName.equals("")) { //$NON-NLS-1$
				libName= ((Element)xmlZipfilesets.item(i)).getAttribute("src"); //$NON-NLS-1$
				found= libName.equals(FatJarRsrcUrlBuilder.JAR_RSRC_LOADER_ZIP); //$NON-NLS-1$
			}
			for (int j= 0; j < zipfilesets.length; j++) {
				if (libName.equals(zipfilesets[j])) {
					found= true;
					break;
				}
			}
			assertTrue("find zipfileset lib: '" + libName + "'", found); //$NON-NLS-1$  //$NON-NLS-2$
		}
	}
	
	private static void assertAntScriptExtract(String archiveName, IPath antScriptLocation, String[] filesets, String[] zipfilesets) throws Exception {
		String projectNameValue= "Create Runnable Jar for Project TestSetupProject"; //$NON-NLS-1$

		Element xmlProject= readXML(antScriptLocation);
		assertEquals("project", xmlProject.getNodeName()); //$NON-NLS-1$
		assertEquals("create_run_jar", xmlProject.getAttribute("default")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(projectNameValue, xmlProject.getAttribute("name")); //$NON-NLS-1$

		Element xmlTarget= (Element)xmlProject.getElementsByTagName("target").item(0); //$NON-NLS-1$
		assertEquals("create_run_jar", xmlTarget.getAttribute("name")); //$NON-NLS-1$//$NON-NLS-2$

		Element xmlJar= (Element)xmlTarget.getElementsByTagName("jar").item(0); //$NON-NLS-1$
		assertTrue("actual: " + xmlJar.getAttribute("destfile"), xmlJar.getAttribute("destfile").endsWith(archiveName)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("mergewithoutmain", xmlJar.getAttribute("filesetmanifest")); //$NON-NLS-1$ //$NON-NLS-2$

		Element xmlManifest= (Element)xmlJar.getElementsByTagName("manifest").item(0); //$NON-NLS-1$

		Element xmlAttribute1= (Element)xmlManifest.getElementsByTagName("attribute").item(0); //$NON-NLS-1$
		assertEquals("Main-Class", xmlAttribute1.getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("org.eclipse.jdt.ui.test.Main", xmlAttribute1.getAttribute("value")); //$NON-NLS-1$ //$NON-NLS-2$

		Element xmlAttribute2= (Element)xmlManifest.getElementsByTagName("attribute").item(1); //$NON-NLS-1$
		assertEquals("Class-Path", xmlAttribute2.getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(".", xmlAttribute2.getAttribute("value")); //$NON-NLS-1$ //$NON-NLS-2$

		NodeList xmlFilesets= xmlJar.getElementsByTagName("fileset"); //$NON-NLS-1$
		assertEquals(filesets.length, xmlFilesets.getLength());

		NodeList xmlZipfilesets= xmlJar.getElementsByTagName("zipfileset"); //$NON-NLS-1$
		assertEquals(zipfilesets.length, xmlZipfilesets.getLength());

		for (int i= 0; i < xmlFilesets.getLength(); i++) {
			String dir= ((Element)xmlFilesets.item(i)).getAttribute("dir"); //$NON-NLS-1$
			boolean found= false;
			for (int j= 0; j < filesets.length; j++) {
				if (dir.endsWith(filesets[j])) {
					found= true;
					break;
				}
			}
			assertTrue("found fileset: '" + dir + "'", found); //$NON-NLS-1$//$NON-NLS-2$
		}

		for (int i= 0; i < xmlZipfilesets.getLength(); i++) {
			String excludes= ((Element)xmlZipfilesets.item(i)).getAttribute("excludes"); //$NON-NLS-1$
			assertEquals("META-INF/*.SF", excludes); //$NON-NLS-1$
			String src= ((Element)xmlZipfilesets.item(i)).getAttribute("src"); //$NON-NLS-1$
			boolean found= false;
			for (int j= 0; j < zipfilesets.length; j++) {
				if (src.endsWith(zipfilesets[j])) {
					found= true;
					break;
				}
			}
			assertTrue("found zipfileset: '" + src + "'", found); //$NON-NLS-1$  //$NON-NLS-2$
		}
	}

	/**
	 * Helper class to open a xml file
	 * 
	 * @param xmlFilePath path to xml file to read
	 * @return root element of the parsed xml-document
	 * @throws Exception if anything went wrong
	 */
	private static Element readXML(IPath xmlFilePath) throws Exception {
		InputStream in = null;
		try {
			in = new FileInputStream(xmlFilePath.toFile());
			DocumentBuilder parser= DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
			Element root= parser.parse(new InputSource(in)).getDocumentElement();
			in.close();
			
			return root;
		} finally {
			if (in != null)
				in.close();
		}
	}

	public void testExportSameSrcRoot() throws Exception {
		IPackageFragment pack= fMainRoot.createPackageFragment("mylib", true, null); //$NON-NLS-1$
		try {
			pack.createCompilationUnit("Foo.java", getFooContent(), true, null); //$NON-NLS-1$

			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()), new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$

			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(), new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$
		} finally {
			pack.delete(true, null);
		}
	}
	

	public void testExportSrcRootWithOutputFolder() throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "other", new IPath[0], new IPath[0], "otherout"); //$NON-NLS-1$  //$NON-NLS-2$
		try {
			IPackageFragment pack= root.createPackageFragment("mylib", true, null); //$NON-NLS-1$
			pack.createCompilationUnit("Foo.java", getFooContent(), true, null); //$NON-NLS-1$

			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin", "TestSetupProject/otherout" },//$NON-NLS-1$  //$NON-NLS-2$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$

			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin", "TestSetupProject/otherout" },//$NON-NLS-2$  //$NON-NLS-1$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$

			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(), new String[] { "TestSetupProject/bin", "TestSetupProject/otherout" },//$NON-NLS-2$  //$NON-NLS-1$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$
		} finally {
			JavaProjectHelper.removeSourceContainer(fProject, root.getElementName());
		}
	}

	public void testExportOtherSrcRoot() throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "other"); //$NON-NLS-1$
		try {
			IPackageFragment pack= root.createPackageFragment("mylib", true, null); //$NON-NLS-1$
			pack.createCompilationUnit("Foo.java", getFooContent(), true, null); //$NON-NLS-1$

			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, new String[] { "rtstubs15.jar" }); //$NON-NLS-1$ //$NON-NLS-2$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$

			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$
		} finally {
			JavaProjectHelper.removeSourceContainer(fProject, root.getElementName());
		}
	}

	public void testExportOtherProject() throws Exception {
		IJavaProject otherProject= JavaProjectHelper.createJavaProject("OtherProject", "bin"); //$NON-NLS-1$  //$NON-NLS-2$
		try {
			otherProject.setRawClasspath(ProjectTestSetup.getDefaultClasspath(), null);

			IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(otherProject, "other"); //$NON-NLS-1$
			IPackageFragment pack= root.createPackageFragment("mylib", true, null); //$NON-NLS-1$
			pack.createCompilationUnit("Foo.java", getFooContent(), true, null); //$NON-NLS-1$

			JavaProjectHelper.addRequiredProject(fProject, otherProject);

			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin", "OtherProject/bin" }, new String[] { "rtstubs15.jar" }); //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin", "OtherProject/bin" },//$NON-NLS-1$  //$NON-NLS-2$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$

			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(),
					new String[] { "TestSetupProject/bin", "OtherProject/bin" },//$NON-NLS-1$  //$NON-NLS-2$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, otherProject.getProject().getFullPath());
			JavaProjectHelper.delete(otherProject);
		}
	}

	public void testExportInternalLib() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_STDOUT);
		IPackageFragmentRoot root= JavaProjectHelper.addLibraryWithImport(fProject, Path.fromOSString(lib.getPath()), null, null);

		try {
			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar", "TestSetupProject/mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin" },//$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
			
			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(),
					new String[] { "TestSetupProject/bin" },//$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, root.getPath());
		}
	}

	public void testExportInternalLib_UncompressedJar() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_STDOUT);
		IPackageFragmentRoot root= JavaProjectHelper.addLibraryWithImport(fProject, Path.fromOSString(lib.getPath()), null, null);

		try {
			JarPackageData data= createAndRunFatJar(fProject, getName(), false, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, new String[] { "rtstubs15.jar", "TestSetupProject/mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
			
			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, root.getPath());
		}
	}

	public void testExportExternalLib() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_STDOUT);
		IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, Path.fromOSString(lib.getPath()));

		try {
			// normal Jar
			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, new String[] { "rtstubs15.jar", "testresources/mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$

			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(), new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, root.getPath());
		}
	}

	public void testClassFolder() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_STDOUT);

		IPackageFragmentRoot root= JavaProjectHelper.addClassFolderWithImport(fProject, "cf", null, null, lib); //$NON-NLS-1$
		try {
			// normal Jar
			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin", "TestSetupProject/cf" }, new String[] { "rtstubs15.jar" }); //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin", "TestSetupProject/cf" },//$NON-NLS-1$  //$NON-NLS-2$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$
			
			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(), new String[] { "TestSetupProject/bin", "TestSetupProject/cf" },//$NON-NLS-1$  //$NON-NLS-2$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, root.getPath());
		}
	}

	public void testVariable() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_STDOUT);
		JavaCore.setClasspathVariable("MYLIB", Path.fromOSString(lib.getPath()), null); //$NON-NLS-1$

		JavaProjectHelper.addVariableEntry(fProject, new Path("MYLIB"), null, null); //$NON-NLS-1$
		try {
			// normal Jar
			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, new String[] { "rtstubs15.jar", "testresources/mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
			
			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(), new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_stdout.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, new Path("MYLIB")); //$NON-NLS-1$
		}
	}

	public void testSignedLibs() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB_SIG);
		IPackageFragmentRoot root= JavaProjectHelper.addLibraryWithImport(fProject, Path.fromOSString(lib.getPath()), null, null);

		try {
			// normal Jar
			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, new String[] { "rtstubs15.jar", "TestSetupProject/mylib_sig.jar" }); //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_sig.jar" }); //$NON-NLS-1$  //$NON-NLS-2$

			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(), new String[] { "TestSetupProject/bin" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar", "mylib_sig.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, root.getPath());
		}
	}
	
	public void testExternalClassFolder() throws Exception {
		File classFolder= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/externalClassFolder/"));//$NON-NLS-1$
		assertTrue("class folder not found", classFolder != null && classFolder.exists());//$NON-NLS-1$
		IPackageFragmentRoot externalRoot= JavaProjectHelper.addLibrary(fProject, Path.fromOSString(classFolder.getPath()), null, null);

		try {
			JarPackageData data= createAndRunFatJar(fProject, getName(), true, new ExtractLibraryHandler());
			assertAntScript(data, antScriptLocation(getName()),
					new ExtractLibraryHandler(),
					new String[] { "TestSetupProject/bin", "testresources/externalClassFolder" }, new String[] { "rtstubs15.jar" }); //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
			
			// Jar-in-Jar loader
			data= createAndRunFatJar(fProject, getName() + "_JiJ", true, new PackageLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_JiJ"), //$NON-NLS-1$
					new PackageLibraryHandler(),
					new String[] { "TestSetupProject/bin", "testresources/externalClassFolder" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$  //$NON-NLS-2$

			// sub-folder libraries
			data= createAndRunFatJar(fProject, getName() + "_SL", true, new CopyLibraryHandler()); //$NON-NLS-1$
			assertAntScript(data, antScriptLocation(getName() + "_SL"), //$NON-NLS-1$
					new CopyLibraryHandler(), new String[] { "TestSetupProject/bin", "testresources/externalClassFolder" }, //$NON-NLS-1$
					new String[] { "rtstubs15.jar" }); //$NON-NLS-1$  //$NON-NLS-2$
		} finally {
			JavaProjectHelper.removeFromClasspath(fProject, externalRoot.getPath());
		}
	}
}
