/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit.buildpath;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.junit.JUnitCore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;

public class JUnitContainerInitializer extends ClasspathContainerInitializer {

	private static final IStatus NOT_SUPPORTED= new Status(IStatus.ERROR, JUnitCorePlugin.CORE_PLUGIN_ID, ClasspathContainerInitializer.ATTRIBUTE_NOT_SUPPORTED, "", null); //$NON-NLS-1$
	private static final IStatus READ_ONLY= new Status(IStatus.ERROR, JUnitCorePlugin.CORE_PLUGIN_ID, ClasspathContainerInitializer.ATTRIBUTE_READ_ONLY, "", null); //$NON-NLS-1$

	/**
	 * @deprecated just for compatibility
	 */
	@Deprecated
	private final static String JUNIT3_8_1= "3.8.1"; //$NON-NLS-1$
	private final static String JUNIT3= "3"; //$NON-NLS-1$
	private final static String JUNIT4= "4"; //$NON-NLS-1$
	private final static String JUNIT5= "5"; //$NON-NLS-1$

	private static class JUnitContainer implements IClasspathContainer {

		private final IClasspathEntry[] fEntries;
		private final IPath fPath;

		public JUnitContainer(IPath path, IClasspathEntry[] entries) {
			fPath= path;
			fEntries= entries;
		}

		@Override
		public IClasspathEntry[] getClasspathEntries() {
			return fEntries;
		}

		@Override
		public String getDescription() {
			if (JUnitCore.JUNIT5_CONTAINER_PATH.equals(fPath)) {
				return JUnitMessages.JUnitContainerInitializer_description_junit5;
			}
			if (JUnitCore.JUNIT4_CONTAINER_PATH.equals(fPath)) {
				return JUnitMessages.JUnitContainerInitializer_description_junit4;
			}
			return JUnitMessages.JUnitContainerInitializer_description_junit3;
		}

		@Override
		public int getKind() {
			return IClasspathContainer.K_APPLICATION;
		}

		@Override
		public IPath getPath() {
			return fPath;
		}

	}


	public JUnitContainerInitializer() {
	}

	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		if (isValidJUnitContainerPath(containerPath)) {
			JUnitContainer container= getNewContainer(containerPath);
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, 	new IClasspathContainer[] { container }, null);
		}

	}

	private static JUnitContainer getNewContainer(IPath containerPath) {
		List<IClasspathEntry> entriesList= new ArrayList<>();
		IClasspathEntry entry= null;
		IClasspathEntry entry2= null;
		String version= containerPath.segment(1);
		if (null != version) switch (version) {
		case JUNIT3_8_1:
		case JUNIT3:
			entry= BuildPathSupport.getJUnit3LibraryEntry();
			if (entry == null) { // JUnit 4 includes most of JUnit 3, so let's cheat
				entry= BuildPathSupport.getJUnit4as3LibraryEntry();
			}
			break;
		case JUNIT4:
			entry= BuildPathSupport.getJUnit4LibraryEntry();
			entry2= BuildPathSupport.getHamcrestCoreLibraryEntry();
			break;
		case JUNIT5:
			entriesList.add(BuildPathSupport.getJUnitJupiterApiLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitJupiterEngineLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitJupiterMigrationSupportLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitJupiterParamsLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitPlatformCommonsLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitPlatformEngineLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitPlatformLauncherLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitPlatformRunnerLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitPlatformSuiteApiLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitPlatformSuiteEngineLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitPlatformSuiteCommonsLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitVintageEngineLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitOpentest4jLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnitApiGuardianLibraryEntry());
			entriesList.add(BuildPathSupport.getJUnit4LibraryEntry());
			entriesList.add(BuildPathSupport.getHamcrestCoreLibraryEntry());
			break;
		default:
			break;
		}
		IClasspathEntry[] entries;
		if (!entriesList.isEmpty() ) {
			entries= entriesList.toArray(new IClasspathEntry[entriesList.size()]);
		} else if (entry == null) {
			entries= new IClasspathEntry[] { };
		} else if (entry2 == null) {
			entries= new IClasspathEntry[] { entry };
		} else {
			entries= new IClasspathEntry[] { entry, entry2 };
		}
		return new JUnitContainer(containerPath, entries);
	}


	private static boolean isValidJUnitContainerPath(IPath path) {
		return path != null && path.segmentCount() == 2 && JUnitCore.JUNIT_CONTAINER_ID.equals(path.segment(0));
	}

	@Override
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		return true;
	}

	@Override
	public IStatus getAccessRulesStatus(IPath containerPath, IJavaProject project) {
		return READ_ONLY;
	}

	@Override
	public IStatus getSourceAttachmentStatus(IPath containerPath, IJavaProject project) {
		return READ_ONLY;
	}

	@Override
	public IStatus getAttributeStatus(IPath containerPath, IJavaProject project, String attributeKey) {
		if (IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attributeKey)) {
			return Status.OK_STATUS;
		}
		return NOT_SUPPORTED;
	}


	@Override
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
		IEclipsePreferences preferences= InstanceScope.INSTANCE.getNode(JUnitCorePlugin.CORE_PLUGIN_ID);

		IClasspathEntry[] entries= containerSuggestion.getClasspathEntries();
		if (entries.length >= 1 && isValidJUnitContainerPath(containerPath)) {
			String version= containerPath.segment(1);

			// only modifiable entry is Javadoc location
			for (IClasspathEntry entry : entries) {
				String preferenceKey= getPreferenceKey(entry, version);
				IClasspathAttribute[] extraAttributes= entry.getExtraAttributes();
				if (extraAttributes.length == 0) {
					// Revert to default
					String defaultValue= DefaultScope.INSTANCE.getNode(JUnitCorePlugin.CORE_PLUGIN_ID).get(preferenceKey, ""); //$NON-NLS-1$
					if (!defaultValue.equals(preferences.get(preferenceKey, defaultValue))) {
						preferences.put(preferenceKey, defaultValue);
					}

					/*
					* The following would be correct, but would not allow to revert to the default.
					* There's no concept of "default value" for a classpath attribute, see
					* org.eclipse.jdt.internal.ui.preferences.JavadocConfigurationBlock.performDefaults()
					*/
					// preferenceStore.setValue(preferenceKey, "");
				} else {
					for (IClasspathAttribute attrib : extraAttributes) {
						if (IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attrib.getName())) {
							if (preferenceKey != null) {
								preferences.put(preferenceKey, attrib.getValue());
							}
							break;
						}
					}
				}
			}
			rebindClasspathEntries(project.getJavaModel(), containerPath);
		}
	}

	private String getPreferenceKey(IClasspathEntry entry, String version) {
		if (JUNIT3.equals(version)) {
			return JUnitPreferencesConstants.JUNIT3_JAVADOC;
		} else {
			String lastSegment= entry.getPath().lastSegment();
			if (JUNIT4.equals(version)) {
				if (lastSegment.contains("junit")) { //$NON-NLS-1$
					return JUnitPreferencesConstants.JUNIT4_JAVADOC;
				} else {
					return JUnitPreferencesConstants.HAMCREST_CORE_JAVADOC;
				}
			} else if (JUNIT5.equals(version)) {
				if (lastSegment.contains("jupiter.api") || lastSegment.contains("jupiter-api")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_JUPITER_API_JAVADOC;
				} else if (lastSegment.contains("jupiter.engine") || lastSegment.contains("jupiter-engine")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_JUPITER_ENGINE_JAVADOC;
				} else if (lastSegment.contains("jupiter.migrationsupport") || lastSegment.contains("jupiter-migrationsupport")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_JUPITER_MIGRATIONSUPPORT_JAVADOC;
				} else if (lastSegment.contains("jupiter.params") || lastSegment.contains("jupiter-params")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_JUPITER_PARAMS_JAVADOC;
				} else if (lastSegment.contains("platform.commons") || lastSegment.contains("platform-commons")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_PLATFORM_COMMONS_JAVADOC;
				} else if (lastSegment.contains("platform.engine") || lastSegment.contains("platform-engine")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_PLATFORM_ENGINE_JAVADOC;
				} else if (lastSegment.contains("platform.launcher") || lastSegment.contains("platform-launcher")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_PLATFORM_LAUNCHER_JAVADOC;
				} else if (lastSegment.contains("platform.runner") || lastSegment.contains("platform-runner")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_PLATFORM_RUNNER_JAVADOC;
				} else if (lastSegment.contains("platform.suite.api") || lastSegment.contains("platform-suite-api")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_PLATFORM_SUITE_API_JAVADOC;
				} else if (lastSegment.contains("platform.suite.engine") || lastSegment.contains("platform-suite-engine")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_PLATFORM_SUITE_ENGINE_JAVADOC;
				} else if (lastSegment.contains("platform.suite.commons") || lastSegment.contains("platform-suite-commons")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_PLATFORM_SUITE_COMMONS_JAVADOC;
				} else if (lastSegment.contains("vintage.engine") || lastSegment.contains("vintage-engine")) { //$NON-NLS-1$ //$NON-NLS-2$
					return JUnitPreferencesConstants.JUNIT_VINTAGE_ENGINE_JAVADOC;
				} else if (lastSegment.contains("opentest4j")) { //$NON-NLS-1$
					return JUnitPreferencesConstants.JUNIT_OPENTEST4J_JAVADOC;
				} else if (lastSegment.contains("apiguardian")) { //$NON-NLS-1$
					return JUnitPreferencesConstants.JUNIT_APIGUARDIAN_JAVADOC;
				} else if (lastSegment.contains("junit")) { //$NON-NLS-1$
					return JUnitPreferencesConstants.JUNIT4_JAVADOC;
				} else {
					return JUnitPreferencesConstants.HAMCREST_CORE_JAVADOC;
				}
			}
		}
		return null;
	}

	private static void rebindClasspathEntries(IJavaModel model, IPath containerPath) throws JavaModelException {
		ArrayList<IJavaProject> affectedProjects= new ArrayList<>();

		IJavaProject[] projects= model.getJavaProjects();
		for (IJavaProject project : projects) {
			IClasspathEntry[] entries= project.getRawClasspath();
			for (IClasspathEntry curr : entries) {
				if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER && containerPath.equals(curr.getPath())) {
					affectedProjects.add(project);
				}
			}
		}
		if (!affectedProjects.isEmpty()) {
			IJavaProject[] affected= affectedProjects.toArray(new IJavaProject[affectedProjects.size()]);
			IClasspathContainer[] containers= new IClasspathContainer[affected.length];
			for (int i= 0; i < containers.length; i++) {
				containers[i]= getNewContainer(containerPath);
			}
			JavaCore.setClasspathContainer(containerPath, affected, containers, null);
		}
	}

	/**
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getDescription(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public String getDescription(IPath containerPath, IJavaProject project) {
		if (isValidJUnitContainerPath(containerPath)) {
			String version= containerPath.segment(1);
			if (null != version) switch (version) {
			case JUNIT3_8_1:
			case JUNIT3:
				return JUnitMessages.JUnitContainerInitializer_description_initializer_junit3;
			case JUNIT4:
				return JUnitMessages.JUnitContainerInitializer_description_initializer_junit4;
			case JUNIT5:
				return JUnitMessages.JUnitContainerInitializer_description_initializer_junit5;
			default:
				break;
			}
		}
		return JUnitMessages.JUnitContainerInitializer_description_initializer_unresolved;
	}

	@Override
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		return containerPath;
	}

}
