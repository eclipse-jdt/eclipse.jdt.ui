/*******************************************************************************
 * Copyright (c) 2024 Erik Brangs and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Erik Brangs - initial implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.launcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

class MockLaunchConfig implements ILaunchConfiguration {

	private String fProjectName;
	private String fTestRunnerKind;
	private String fMainTypeName;
	private String fContainerHandle;

	public void setProjectName(String projectName) {
		fProjectName= projectName;
	}

	public void setTestRunnerKind(String testRunnerKind) {
		fTestRunnerKind= testRunnerKind;
	}

	public void setContainerHandle(String containerHandle) {
		fContainerHandle= containerHandle;
	}


	@Override
	public String getAttribute(String attributeName, String defaultValue) throws CoreException {
		if (IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME.equals(attributeName)) {
			return fProjectName;
		} else if (JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND.equals(attributeName)) {
			return fTestRunnerKind;
		} else if (IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME.equals(attributeName)) {
			return fMainTypeName;
		} else if (JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER.equals(attributeName)) {
			return fContainerHandle;
		}
		return defaultValue;
	}

	@Override
	public List<String> getAttribute(String attributeName, List<String> defaultValue) throws CoreException {
		return defaultValue;
	}

	@Override
	public boolean getAttribute(String attributeName, boolean defaultValue) throws CoreException {
		return defaultValue;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean contentsEqual(ILaunchConfiguration configuration) {
		return false;
	}

	@Override
	public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
		return null;
	}

	@Override
	public void delete() throws CoreException {
	}

	@Override
	public void delete(int flag) throws CoreException {
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public int getAttribute(String attributeName, int defaultValue) throws CoreException {
		return 0;
	}

	@Override
	public Set<String> getAttribute(String attributeName, Set<String> defaultValue) throws CoreException {
		return null;
	}

	@Override
	public Map<String, String> getAttribute(String attributeName, Map<String, String> defaultValue) throws CoreException {
		return null;
	}

	@Override
	public Map<String, Object> getAttributes() throws CoreException {
		return null;
	}

	@Override
	public String getCategory() throws CoreException {
		return null;
	}

	@Override
	public IFile getFile() {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public IPath getLocation() {
		return null;
	}

	@Override
	public IResource[] getMappedResources() throws CoreException {
		return null;
	}

	@Override
	public String getMemento() throws CoreException {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Set<String> getModes() throws CoreException {
		return null;
	}

	@Override
	public ILaunchDelegate getPreferredDelegate(Set<String> modes) throws CoreException {
		return null;
	}

	@Override
	public ILaunchConfigurationType getType() throws CoreException {
		return null;
	}

	@Override
	public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
		return null;
	}

	@Override
	public boolean hasAttribute(String attributeName) throws CoreException {
		return false;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isMigrationCandidate() throws CoreException {
		return false;
	}

	@Override
	public boolean isWorkingCopy() {
		return false;
	}

	@Override
	public ILaunch launch(String mode, IProgressMonitor monitor) throws CoreException {
		return null;
	}

	@Override
	public ILaunch launch(String mode, IProgressMonitor monitor, boolean build) throws CoreException {
		return null;
	}

	@Override
	public ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register) throws CoreException {
		return null;
	}

	@Override
	public void migrate() throws CoreException {
	}

	@Override
	public boolean supportsMode(String mode) throws CoreException {
		return false;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public ILaunchConfiguration getPrototype() throws CoreException {
		return null;
	}

	@Override
	public boolean isAttributeModified(String attribute) throws CoreException {
		return false;
	}

	@Override
	public boolean isPrototype() {
		return false;
	}

	@Override
	public Collection<ILaunchConfiguration> getPrototypeChildren() throws CoreException {
		return null;
	}

	@Override
	public int getKind() throws CoreException {
		return 0;
	}

	@Override
	public Set<String> getPrototypeVisibleAttributes() throws CoreException {
		return null;
	}

	@Override
	public void setPrototypeAttributeVisibility(String attribute, boolean visible) throws CoreException {
	}

}
