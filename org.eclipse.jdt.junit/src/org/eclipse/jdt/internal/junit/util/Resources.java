/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;


public class Resources {

	private Resources() {
	}

	/**
	 * Checks if the given resource is in sync with the underlying file system.
	 *
	 * @param resource the resource to be checked
	 * @return IStatus status describing the check's result. If <code>status.
	 * isOK()</code> returns <code>true</code> then the resource is in sync
	 */
	public static IStatus checkInSync(IResource resource) {
		return checkInSync(new IResource[] {resource});
	}

	/**
	 * Checks if the given resources are in sync with the underlying file
	 * system.
	 *
	 * @param resources the resources to be checked
	 * @return IStatus status describing the check's result. If <code>status.
	 *  isOK() </code> returns <code>true</code> then the resources are in sync
	 */
	public static IStatus checkInSync(IResource[] resources) {
		IStatus result= null;
		for (IResource resource : resources) {
			if (!resource.isSynchronized(IResource.DEPTH_INFINITE)) {
				result= addOutOfSync(result, resource);
			}
		}
		if (result != null)
			return result;
		return Status.OK_STATUS;
	}

	/**
	 * Makes the given resource committable. Committable means that it is
	 * writeable and that its content hasn't changed by calling
	 * <code>validateEdit</code> for the given resource on <code>IWorkspace</code>.
	 *
	 * @param resource the resource to be checked
	 * @param context the context passed to <code>validateEdit</code>
	 * @return IStatus status describing the method's result. If <code>status.
	 * isOK()</code> returns <code>true</code> then the resource are committable
	 *
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.
	 * resources.IFile[], java.lang.Object)
	 */
	public static IStatus makeCommittable(IResource resource, Object context) {
		return makeCommittable(new IResource[] { resource }, context);
	}

	/**
	 * Makes the given resources committable. Committable means that all
	 * resources are writeable and that the content of the resources hasn't
	 * changed by calling <code>validateEdit</code> for a given file on
	 * <code>IWorkspace</code>.
	 *
	 * @param resources the resources to be checked
	 * @param context the context passed to <code>validateEdit</code>
	 * @return IStatus status describing the method's result. If <code>status.
	 * isOK()</code> returns <code>true</code> then the add resources are
	 * committable
	 *
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static IStatus makeCommittable(IResource[] resources, Object context) {
		List<IFile> readOnlyFiles= new ArrayList<>();
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE && resource.getResourceAttributes().isReadOnly())
				readOnlyFiles.add((IFile) resource);
		}
		if (readOnlyFiles.isEmpty())
			return Status.OK_STATUS;

		Map<IFile, Long> oldTimeStamps= createModificationStampMap(readOnlyFiles);
		IStatus status= ResourcesPlugin.getWorkspace().validateEdit(
			readOnlyFiles.toArray(new IFile[readOnlyFiles.size()]), context);
		if (!status.isOK())
			return status;

		IStatus modified= null;
		Map<IFile, Long> newTimeStamps= createModificationStampMap(readOnlyFiles);
		for (IFile file : oldTimeStamps.keySet()) {
			if (!oldTimeStamps.get(file).equals(newTimeStamps.get(file)))
				modified= addModified(modified, file);
		}
		if (modified != null)
			return modified;
		return Status.OK_STATUS;
	}

	private static Map<IFile, Long> createModificationStampMap(List<IFile> files){
		Map<IFile, Long> map= new HashMap<>();
		for (IFile file : files) {
			map.put(file, file.getModificationStamp());
		}
		return map;
	}

	private static IStatus addModified(IStatus status, IFile file) {
		IStatus entry= JUnitStatus.createError(Messages.format(JUnitMessages.Resources_fileModified, BasicElementLabels.getPathLabel(file.getFullPath(), false)));
		if (status == null) {
			return entry;
		} else if (status.isMultiStatus()) {
			((MultiStatus)status).add(entry);
			return status;
		} else {
			MultiStatus result= new MultiStatus(JUnitPlugin.getPluginId(),
				IJUnitStatusConstants.VALIDATE_EDIT_CHANGED_CONTENT,
			JUnitMessages.Resources_modifiedResources, null);
			result.add(status);
			result.add(entry);
			return result;
		}
	}

	private static IStatus addOutOfSync(IStatus status, IResource resource) {
		IStatus entry= new Status(
			IStatus.ERROR,
			ResourcesPlugin.PI_RESOURCES,
			IResourceStatus.OUT_OF_SYNC_LOCAL,
		Messages.format(JUnitMessages.Resources_outOfSync, BasicElementLabels.getPathLabel(resource.getFullPath(), false)),
			null);
		if (status == null) {
			return entry;
		} else if (status.isMultiStatus()) {
			((MultiStatus)status).add(entry);
			return status;
		} else {
			MultiStatus result= new MultiStatus(
				ResourcesPlugin.PI_RESOURCES,
				IResourceStatus.OUT_OF_SYNC_LOCAL,
			JUnitMessages.Resources_outOfSyncResources, null);
			result.add(status);
			result.add(entry);
			return result;
		}
	}
}