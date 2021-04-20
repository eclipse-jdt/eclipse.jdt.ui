/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *     Robert Konigsberg <konigsberg@google.com> - [JUnit] Leverage AbstractJavaLaunchConfigurationDelegate.getMainTypeName in JUnitLaunchConfigurationDelegate - https://bugs.eclipse.org/bugs/show_bug.cgi?id=280114
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *******************************************************************************/
package org.eclipse.jdt.junit.launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.URIUtil;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitRuntimeClasspathEntry;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.eclipse.jdt.internal.junit.util.IJUnitStatusConstants;

import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;


/**
 * Launch configuration delegate for a JUnit test as a Java application.
 *
 * <p>
 * Clients can instantiate and extend this class.
 * </p>
 * @since 3.3
 */
public class JUnitLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	private boolean fKeepAlive= false;
	private int fPort;
	private IJavaElement[] fTestElements;

	private static final String DEFAULT= "<default>"; //$NON-NLS-1$

	@Override
	public String showCommandLine(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
			VMRunnerConfiguration runConfig = getVMRunnerConfiguration(configuration, launch, mode, monitor);
			if (runConfig == null) {
				return ""; //$NON-NLS-1$
			}
			IVMRunner runner = getVMRunner(configuration, mode);
			String cmdLine = runner.showCommandLine(runConfig, launch, monitor);

			// check for cancellation
			if (monitor.isCanceled()) {
				return ""; //$NON-NLS-1$
			}
			return cmdLine;
		} finally {
			monitor.done();
		}
	}

	private VMRunnerConfiguration getVMRunnerConfiguration(ILaunchConfiguration configuration, ILaunch launch, String mode, IProgressMonitor monitor) throws CoreException {
		VMRunnerConfiguration runConfig = null;
			monitor.beginTask(MessageFormat.format("{0}...", configuration.getName()), 5); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return null;
		}

		try {
			if (JUnitLaunchConfigurationConstants.MODE_RUN_QUIETLY_MODE.equals(mode)) {
				launch.setAttribute(JUnitLaunchConfigurationConstants.ATTR_NO_DISPLAY, "true"); //$NON-NLS-1$
				mode = ILaunchManager.RUN_MODE;
			}

			monitor.subTask(JUnitMessages.JUnitLaunchConfigurationDelegate_verifying_attriburtes_description);

			try {
				preLaunchCheck(configuration, launch, new SubProgressMonitor(monitor, 2));
			} catch (CoreException e) {
				if (e.getStatus().getSeverity() == IStatus.CANCEL) {
					monitor.setCanceled(true);
					return null;
				}
				throw e;
			}
			// check for cancellation
			if (monitor.isCanceled()) {
				return null;
			}

			fKeepAlive= ILaunchManager.DEBUG_MODE.equals(mode) && configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING, false);
			fPort= evaluatePort();
			launch.setAttribute(JUnitLaunchConfigurationConstants.ATTR_PORT, String.valueOf(fPort));

			ITestKind testKind= getTestRunnerKind(configuration);
			IJavaProject javaProject= getJavaProject(configuration);
			if (TestKindRegistry.JUNIT3_TEST_KIND_ID.equals(testKind.getId()) || TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind.getId())) {
				fTestElements= evaluateTests(configuration, new SubProgressMonitor(monitor, 1));
			} else {
				IJavaElement testTarget= getTestTarget(configuration, javaProject);
				if (testTarget instanceof IPackageFragment || testTarget instanceof IPackageFragmentRoot || testTarget instanceof IJavaProject) {
					fTestElements= new IJavaElement[] { testTarget };
				} else {
					fTestElements= evaluateTests(configuration, new SubProgressMonitor(monitor, 1));
				}
			}

			String mainTypeName= verifyMainTypeName(configuration);


			File workingDir = verifyWorkingDirectory(configuration);
			String workingDirName = null;
			if (workingDir != null) {
				workingDirName= workingDir.getAbsolutePath();
			}

			// Environment variables
			String[] envp= getEnvironment(configuration);

			ArrayList<String> vmArguments= new ArrayList<>();
			ArrayList<String> programArguments= new ArrayList<>();
			collectExecutionArguments(configuration, vmArguments, programArguments);
			vmArguments.addAll(Arrays.asList(DebugPlugin.parseArguments(getVMArguments(configuration, mode))));
			if (JavaRuntime.isModularProject(javaProject)) {
				vmArguments.add("--add-modules=ALL-MODULE-PATH"); //$NON-NLS-1$
			}

			// VM-specific attributes
			Map<String, Object> vmAttributesMap= getVMSpecificAttributesMap(configuration);

			// Classpath and modulepath
			String[][] classpathAndModulepath= getClasspathAndModulepath(configuration);
			String[] classpath= classpathAndModulepath[0];
			String[] modulepath= classpathAndModulepath[1];

			if (TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(getTestRunnerKind(configuration).getId())) {
				if (!configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_DONT_ADD_MISSING_JUNIT5_DEPENDENCY, false)) {
					if (!Arrays.stream(classpath).anyMatch(s -> s.contains("junit-platform-launcher") || s.contains("org.junit.platform.launcher"))) { //$NON-NLS-1$ //$NON-NLS-2$
						try {
							JUnitRuntimeClasspathEntry x= new JUnitRuntimeClasspathEntry("org.junit.platform.launcher", null); //$NON-NLS-1$
							String entryString= new ClasspathLocalizer(Platform.inDevelopmentMode()).entryString(x);
							int length= classpath.length;
							System.arraycopy(classpath, 0, classpath= new String[length + 1], 0, length);
							classpath[length]= entryString;
						} catch (IOException | URISyntaxException e) {
							throw new CoreException(new Status(IStatus.ERROR, JUnitCorePlugin.CORE_PLUGIN_ID, IStatus.ERROR, "", e)); //$NON-NLS-1$
						}
					}
				}
			}

			// Create VM config
			runConfig= new VMRunnerConfiguration(mainTypeName, classpath);
			runConfig.setVMArguments(vmArguments.toArray(new String[vmArguments.size()]));
			runConfig.setProgramArguments(programArguments.toArray(new String[programArguments.size()]));
			runConfig.setEnvironment(envp);
			runConfig.setWorkingDirectory(workingDirName);
			runConfig.setVMSpecificAttributesMap(vmAttributesMap);
			runConfig.setPreviewEnabled(supportsPreviewFeatures(configuration));

			if (!JavaRuntime.isModularConfiguration(configuration)) {
				// Bootpath
				runConfig.setBootClassPath(getBootpath(configuration));
			} else {
				// module path
				runConfig.setModulepath(modulepath);
				if (!configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_MODULE_CLI_OPTIONS, true)) {
					runConfig.setOverrideDependencies(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MODULE_CLI_OPTIONS, "")); //$NON-NLS-1$
				} else {
					runConfig.setOverrideDependencies(getModuleCLIOptions(configuration));
				}
			}

			// check for cancellation
			if (monitor.isCanceled()) {
				return null;
			}
		}finally {
			// done the verification phase
			monitor.worked(1);
		}
		return runConfig;
	}

	@Override
	public synchronized void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		try {

			VMRunnerConfiguration runConfig = getVMRunnerConfiguration(configuration, launch, mode, monitor);
			if ( monitor.isCanceled() || runConfig == null) {
				return;
			}
			IVMRunner runner= getVMRunner(configuration, mode);
			monitor.subTask(JUnitMessages.JUnitLaunchConfigurationDelegate_create_source_locator_description);
			// set the default source locator if required
			setDefaultSourceLocator(launch, configuration);
			monitor.worked(1);

			// Launch the configuration - 1 unit of work
			runner.run(runConfig, launch, monitor);

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}
		} finally {
			fTestElements= null;
			monitor.done();
		}
	}

	private int evaluatePort() throws CoreException {
		int port= SocketUtil.findFreePort();
		if (port == -1) {
			abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_no_socket, null, IJavaLaunchConfigurationConstants.ERR_NO_SOCKET_AVAILABLE);
		}
		return port;
	}

	/**
	 * Performs a check on the launch configuration's attributes. If an attribute contains an invalid value, a {@link CoreException}
	 * with the error is thrown.
	 *
	 * @param configuration the launch configuration to verify
	 * @param launch the launch to verify
	 * @param monitor the progress monitor to use
	 * @throws CoreException an exception is thrown when the verification fails
	 */
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		try {
			IJavaProject javaProject= getJavaProject(configuration);
			if ((javaProject == null) || !javaProject.exists()) {
				abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_invalidproject, null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT);
			}
			ITestKind testKind= getTestRunnerKind(configuration);
			boolean isJUnit4Configuration= TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind.getId());
			boolean isJUnit5Configuration= TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(testKind.getId());
			if (!isJUnit5Configuration && !CoreTestSearchEngine.hasTestCaseType(javaProject)) {
				abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_junitnotonpath, null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
			}
			if (isJUnit4Configuration && !CoreTestSearchEngine.hasJUnit4TestAnnotation(javaProject)) {
				abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_junit4notonpath, null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
			}
			if (isJUnit5Configuration && !CoreTestSearchEngine.hasJUnit5TestAnnotation(javaProject)) {
				String msg= Messages.format(JUnitMessages.JUnitLaunchConfigurationDelegate_error_junit5notonpath, JUnitCorePlugin.JUNIT5_TESTABLE_ANNOTATION_NAME);
				abort(msg, null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
			}
		} finally {
			monitor.done();
		}
	}

	private ITestKind getTestRunnerKind(ILaunchConfiguration configuration) {
		ITestKind testKind= JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (testKind.isNull()) {
			testKind= TestKindRegistry.getDefault().getKind(TestKindRegistry.JUNIT3_TEST_KIND_ID); // backward compatible for launch configurations with no runner
		}
		return testKind;
	}

	@Override
	public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
		return "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"; //$NON-NLS-1$
	}

	/**
	 * Evaluates all test elements selected by the given launch configuration. The elements are of type
	 * {@link IType} or {@link IMethod}. At the moment it is only possible to run a single method or a set of types, but not
	 * mixed or more than one method at a time.
	 *
	 * @param configuration the launch configuration to inspect
	 * @param monitor the progress monitor
	 * @return returns all types or methods that should be ran
	 * @throws CoreException an exception is thrown when the search for tests failed
	 */
	protected IMember[] evaluateTests(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		IJavaProject javaProject= getJavaProject(configuration);

		IJavaElement testTarget= getTestTarget(configuration, javaProject);
		String testMethodName= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, ""); //$NON-NLS-1$
		if (testMethodName.length() > 0) {
			if (testTarget instanceof IType) {
				// If parameters exist, testMethodName is followed by a comma-separated list of fully qualified parameter type names in parentheses.
				// The testMethodName is required in this format by #collectExecutionArguments, hence it will be used as it is with the handle-only method IType#getMethod here.
				return new IMember[] { ((IType) testTarget).getMethod(testMethodName, new String[0]) };
			}
		}
		HashSet<IType> result= new HashSet<>();
		ITestKind testKind= getTestRunnerKind(configuration);
		testKind.getFinder().findTestsInContainer(testTarget, result, monitor);
		if (result.isEmpty()) {
			String msg= Messages.format(JUnitMessages.JUnitLaunchConfigurationDelegate_error_notests_kind, testKind.getDisplayName());
			abort(msg, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
		}
		return result.toArray(new IMember[result.size()]);
	}

	/**
	 * Collects all VM and program arguments. Implementors can modify and add arguments.
	 *
	 * @param configuration the configuration to collect the arguments for
	 * @param vmArguments a {@link List} of {@link String} representing the resulting VM arguments
	 * @param programArguments a {@link List} of {@link String} representing the resulting program arguments
	 * @exception CoreException if unable to collect the execution arguments
	 */
	protected void collectExecutionArguments(ILaunchConfiguration configuration, List<String> vmArguments, List<String> programArguments) throws CoreException {

		// add program & VM arguments provided by getProgramArguments and getVMArguments
		String pgmArgs= getProgramArguments(configuration);
		String vmArgs= getVMArguments(configuration);
		ExecutionArguments execArgs= new ExecutionArguments(vmArgs, pgmArgs);
		vmArguments.addAll(Arrays.asList(execArgs.getVMArgumentsArray()));
		programArguments.addAll(Arrays.asList(execArgs.getProgramArgumentsArray()));

		boolean isJUnit5= TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(getTestRunnerKind(configuration).getId());
		boolean isModularProject= JavaRuntime.isModularProject(getJavaProject(configuration));
		String addOpensTargets;
		if (isModularProject) {
			if (isJUnit5) {
				if (isOnModulePath(getJavaProject(configuration), "org.junit.jupiter.api.Test")) { //$NON-NLS-1$
					addOpensTargets= "org.junit.platform.commons,ALL-UNNAMED"; //$NON-NLS-1$
				} else {
					addOpensTargets= "ALL-UNNAMED"; //$NON-NLS-1$
				}
			} else {
				if (isOnModulePath(getJavaProject(configuration), "junit.framework.TestCase")) { //$NON-NLS-1$
					addOpensTargets= "junit,ALL-UNNAMED"; //$NON-NLS-1$
				} else {
					addOpensTargets= "ALL-UNNAMED"; //$NON-NLS-1$
				}
			}
		} else {
			addOpensTargets= null;
		}
		List<String> addOpensVmArgs= new ArrayList<>();

		/*
		 * The "-version" "3" arguments don't make sense and should eventually be removed.
		 * But we keep them for now, since users may want to run with older releases of
		 * org.eclipse.jdt.junit[4].runtime, where this is still
		 * read by org.eclipse.jdt.internal.junit.runner.RemoteTestRunner#defaultInit(String[])
		 * and used in org.eclipse.jdt.internal.junit.runner.DefaultClassifier#isComparisonFailure(Throwable).
		 * The JUnit4 equivalent of the latter method is already version-agnostic:
		 * org.eclipse.jdt.internal.junit4.runner.JUnit4TestListener#testFailure(Failure, boolean)
		 */
		programArguments.add("-version"); //$NON-NLS-1$
		programArguments.add("3"); //$NON-NLS-1$

		programArguments.add("-port"); //$NON-NLS-1$
		programArguments.add(String.valueOf(fPort));

		if (fKeepAlive)
			programArguments.add(0, "-keepalive"); //$NON-NLS-1$

		ITestKind testRunnerKind= getTestRunnerKind(configuration);

		programArguments.add("-testLoaderClass"); //$NON-NLS-1$
		programArguments.add(testRunnerKind.getLoaderClassName());
		programArguments.add("-loaderpluginname"); //$NON-NLS-1$
		programArguments.add(testRunnerKind.getLoaderPluginId());

		IJavaElement[] testElements= fTestElements;

		if (testElements.length == 1) { // a test name was specified just run the single test, or a test container was specified
			IJavaElement testElement= testElements[0];
			if (testElement instanceof IMethod) {
				IMethod method= (IMethod) testElement;
				programArguments.add("-test"); //$NON-NLS-1$
				programArguments.add(method.getDeclaringType().getFullyQualifiedName() + ':' + method.getElementName());
				collectAddOpensVmArgs(addOpensTargets, addOpensVmArgs, method, configuration);
			} else if (testElement instanceof IType) {
				IType type= (IType) testElement;
				programArguments.add("-classNames"); //$NON-NLS-1$
				programArguments.add(type.getFullyQualifiedName());
				collectAddOpensVmArgs(addOpensTargets, addOpensVmArgs, type, configuration);
			} else if (testElement instanceof IPackageFragment || testElement instanceof IPackageFragmentRoot || testElement instanceof IJavaProject) {
				Set<String> pkgNames= new HashSet<>();
				String fileName= createPackageNamesFile(testElement, testRunnerKind, pkgNames);
				programArguments.add("-packageNameFile"); //$NON-NLS-1$
				programArguments.add(fileName);
				for (String pkgName : pkgNames) {
					if (!DEFAULT.equals(pkgName)) { // skip --add-opens for default package
						collectAddOpensVmArgs(addOpensTargets, addOpensVmArgs, pkgName, configuration);
					}
				}
			} else {
				abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_wrong_input, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
			}
		} else if (testElements.length > 1) {
			String fileName= createTestNamesFile(testElements);
			programArguments.add("-testNameFile"); //$NON-NLS-1$
			programArguments.add(fileName);
			for (IJavaElement testElement : testElements) {
				collectAddOpensVmArgs(addOpensTargets, addOpensVmArgs, testElement, configuration);
			}
		}

		String testFailureNames= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES, ""); //$NON-NLS-1$
		if (testFailureNames.length() > 0) {
			programArguments.add("-testfailures"); //$NON-NLS-1$
			programArguments.add(testFailureNames);
		}

		String uniqueId= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_UNIQUE_ID, ""); //$NON-NLS-1$
		if (!uniqueId.trim().isEmpty()) {
			programArguments.add("-uniqueId"); //$NON-NLS-1$
			programArguments.add(uniqueId);
		}

		boolean hasIncludeTags= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_HAS_INCLUDE_TAGS, false);
		if (hasIncludeTags) {
			String includeTags= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_INCLUDE_TAGS, ""); //$NON-NLS-1$
			if (includeTags != null && !includeTags.trim().isEmpty()) {
				String[] tags= includeTags.split(","); //$NON-NLS-1$
				for (String tag : tags) {
					programArguments.add("--include-tag"); //$NON-NLS-1$
					programArguments.add(tag.trim());
				}
			}
		}

		boolean hasExcludeTags= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_HAS_EXCLUDE_TAGS, false);
		if (hasExcludeTags) {
			String excludeTags= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_EXCLUDE_TAGS, ""); //$NON-NLS-1$
			if (excludeTags != null && !excludeTags.trim().isEmpty()) {
				String[] tags= excludeTags.split(","); //$NON-NLS-1$
				for (String tag : tags) {
					programArguments.add("--exclude-tag"); //$NON-NLS-1$
					programArguments.add(tag.trim());
				}
			}
		}

		if (addOpensTargets != null) {
			vmArguments.addAll(addOpensVmArgs);
		}
	}

	private static boolean isOnModulePath(IJavaProject javaProject, String typeToCheck) {
		try {
			IType type= javaProject.findType(typeToCheck);
			if (type == null)
				return false;
			IPackageFragmentRoot packageFragmentRoot= (IPackageFragmentRoot) type.getPackageFragment().getParent();
			IClasspathEntry resolvedClasspathEntry= packageFragmentRoot.getResolvedClasspathEntry();
			return Arrays.stream(resolvedClasspathEntry.getExtraAttributes())
					.anyMatch(p -> IClasspathAttribute.MODULE.equals(p.getName()) && "true".equals(p.getValue())); //$NON-NLS-1$
		} catch (JavaModelException e) {
			// if anything goes wrong, assume true (in the worst case, user get a warning because of a redundant add-opens)
			return true;
		}
	}

	private void collectAddOpensVmArgs(String addOpensTargets, List<String> addOpensVmArgs, IJavaElement javaElem, ILaunchConfiguration configuration) throws CoreException {
		if (addOpensTargets != null) {
			IPackageFragment pkg= getParentPackageFragment(javaElem);
			if (pkg != null) {
				String pkgName= pkg.getElementName();
				collectAddOpensVmArgs(addOpensTargets, addOpensVmArgs, pkgName, configuration);
			}
		}
	}

	private void collectAddOpensVmArgs(String addOpensTargets, List<String> addOpensVmArgs, String pkgName, ILaunchConfiguration configuration) throws CoreException {
		if (addOpensTargets != null) {
			IJavaProject javaProject= getJavaProject(configuration);
			String sourceModuleName= javaProject.getModuleDescription().getElementName();
			addOpensVmArgs.add("--add-opens"); //$NON-NLS-1$
			addOpensVmArgs.add(sourceModuleName + "/" + pkgName + "=" + addOpensTargets); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private IPackageFragment getParentPackageFragment(IJavaElement element) {
		IJavaElement parent= element.getParent();
		while (parent != null) {
			if (parent instanceof IPackageFragment) {
				return (IPackageFragment) parent;
			}
			parent= parent.getParent();
		}
		return null;
	}

	private String createPackageNamesFile(IJavaElement testContainer, ITestKind testRunnerKind, Set<String> pkgNames) throws CoreException {
		try {
			File file= File.createTempFile("packageNames", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			file.deleteOnExit();

			try (BufferedWriter bw= new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
				if (testContainer instanceof IPackageFragment) {
					pkgNames.add(getPackageName(testContainer.getElementName()));
				} else if (testContainer instanceof IPackageFragmentRoot) {
					addAllPackageFragments((IPackageFragmentRoot) testContainer, pkgNames);
				} else if (testContainer instanceof IJavaProject) {
					for (IPackageFragmentRoot pkgFragmentRoot : ((IJavaProject) testContainer).getPackageFragmentRoots()) {
						if (!pkgFragmentRoot.isExternal() && !pkgFragmentRoot.isArchive()) {
							addAllPackageFragments(pkgFragmentRoot, pkgNames);
						}
					}
				} else {
					abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_wrong_input, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
				}
				if (pkgNames.isEmpty()) {
					String msg= Messages.format(JUnitMessages.JUnitLaunchConfigurationDelegate_error_notests_kind, testRunnerKind.getDisplayName());
					abort(msg, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
				} else {
					for (String pkgName : pkgNames) {
						bw.write(pkgName);
						bw.newLine();
					}
				}
			}
			return file.getAbsolutePath();
		} catch (IOException | JavaModelException e) {
			throw new CoreException(new Status(IStatus.ERROR, JUnitCorePlugin.CORE_PLUGIN_ID, IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}

	private Set<String> addAllPackageFragments(IPackageFragmentRoot pkgFragmentRoot, Set<String> pkgNames) throws JavaModelException {
		for (IJavaElement child : pkgFragmentRoot.getChildren()) {
			if (child instanceof IPackageFragment && ((IPackageFragment) child).hasChildren()) {
				pkgNames.add(getPackageName(child.getElementName()));
			}
		}
		return pkgNames;
	}

	private String getPackageName(String elementName) {
		if (elementName.isEmpty()) {
			return DEFAULT;
		}
		return elementName;
	}

	private String createTestNamesFile(IJavaElement[] testElements) throws CoreException {
		try {
			File file= File.createTempFile("testNames", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			file.deleteOnExit();
			try (BufferedWriter bw= new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));) {
				for (IJavaElement testElement : testElements) {
					if (testElement instanceof IType) {
						IType type= (IType) testElement;
						String testName= type.getFullyQualifiedName();
						bw.write(testName);
						bw.newLine();
					} else {
						abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_wrong_input, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
					}
				}
			}
			return file.getAbsolutePath();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, JUnitCorePlugin.CORE_PLUGIN_ID, IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}

	@Override
	public String[][] getClasspathAndModulepath(ILaunchConfiguration configuration) throws CoreException {
		String[][] cpmp= super.getClasspathAndModulepath(configuration);
		String[] cp= cpmp[0];

		ITestKind kind= getTestRunnerKind(configuration);
		List<String> junitEntries= new ClasspathLocalizer(Platform.inDevelopmentMode()).localizeClasspath(kind);

		String[] classPath= new String[cp.length + junitEntries.size()];
		Object[] jea= junitEntries.toArray();
		System.arraycopy(cp, 0, classPath, 0, cp.length);
		System.arraycopy(jea, 0, classPath, cp.length, jea.length);

		cpmp[0]= classPath;

		return cpmp;
	}

	/**
	 * @deprecated The call to
	 *             {@link JUnitLaunchConfigurationDelegate#getClasspath(ILaunchConfiguration)
	 *             getClasspath(ILaunchConfiguration)} in
	 *             {@link JUnitLaunchConfigurationDelegate#launch(ILaunchConfiguration, String, ILaunch, IProgressMonitor)
	 *             launch(...)} has been replaced with the call to
	 *             {@link JUnitLaunchConfigurationDelegate#getClasspathAndModulepath(ILaunchConfiguration)
	 *             getClasspathAndModulepath(ILaunchConfiguration)}.
	 *
	 */
	@Override
	@Deprecated
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		String[] cp= super.getClasspath(configuration);

		ITestKind kind= getTestRunnerKind(configuration);
		List<String> junitEntries = new ClasspathLocalizer(Platform.inDevelopmentMode()).localizeClasspath(kind);

		String[] classPath= new String[cp.length + junitEntries.size()];
		Object[] jea= junitEntries.toArray();
		System.arraycopy(cp, 0, classPath, 0, cp.length);
		System.arraycopy(jea, 0, classPath, cp.length, jea.length);
		return classPath;
	}

	private static class ClasspathLocalizer {

		private boolean fInDevelopmentMode;

		public ClasspathLocalizer(boolean inDevelopmentMode) {
			fInDevelopmentMode = inDevelopmentMode;
		}

		public List<String> localizeClasspath(ITestKind kind) {
			JUnitRuntimeClasspathEntry[] entries= kind.getClasspathEntries();
			List<String> junitEntries= new ArrayList<>();

			for (JUnitRuntimeClasspathEntry entrie : entries) {
				try {
					addEntry(junitEntries, entrie);
				} catch (IOException | URISyntaxException e) {
					Assert.isTrue(false, entrie.getPluginId() + " is available (required JAR)"); //$NON-NLS-1$
				}
			}
			return junitEntries;
		}

		private void addEntry(List<String> junitEntries, final JUnitRuntimeClasspathEntry entry) throws IOException, MalformedURLException, URISyntaxException {
			String entryString= entryString(entry);
			if (entryString != null)
				junitEntries.add(entryString);
		}

		private String entryString(final JUnitRuntimeClasspathEntry entry) throws IOException, MalformedURLException, URISyntaxException {
			if (inDevelopmentMode()) {
				try {
					return localURL(entry.developmentModeEntry());
				} catch (IOException e3) {
					// fall through and try default
				}
			}
			return localURL(entry);
		}

		private boolean inDevelopmentMode() {
			return fInDevelopmentMode;
		}

		private String localURL(JUnitRuntimeClasspathEntry jar) throws IOException, MalformedURLException, URISyntaxException {
			Bundle bundle= JUnitCorePlugin.getDefault().getBundle(jar.getPluginId());
			URL url;
			if (jar.getPluginRelativePath() == null) {
				String bundleClassPath= bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
				url= bundleClassPath != null ? bundle.getEntry(bundleClassPath) : null;
				if (url == null) {
					url= bundle.getEntry("/"); //$NON-NLS-1$
				}
			} else {
				url= bundle.getEntry(jar.getPluginRelativePath());
			}

			if (url == null)
				throw new IOException();
			return URIUtil.toFile(URIUtil.toURI(FileLocator.toFileURL(url))).getAbsolutePath(); // See bug 503050
		}
	}

	private final IJavaElement getTestTarget(ILaunchConfiguration configuration, IJavaProject javaProject) throws CoreException {
		String containerHandle = configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, ""); //$NON-NLS-1$
		if (containerHandle.length() != 0) {
			 IJavaElement element= JavaCore.create(containerHandle);
			 if (element == null || !element.exists()) {
				 abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_input_element_deosn_not_exist, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
			 }
			 return element;
		}
		String testTypeName= getMainTypeName(configuration);
		if (testTypeName != null && testTypeName.length() != 0) {
			IType type= javaProject.findType(testTypeName);
			if (type != null && type.exists()) {
				return type;
			}
		}
		abort(JUnitMessages.JUnitLaunchConfigurationDelegate_input_type_does_not_exist, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
		return null; // not reachable
	}

	@Override
	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, JUnitCorePlugin.CORE_PLUGIN_ID, code, message, exception));
	}

}
