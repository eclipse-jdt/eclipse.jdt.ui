/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.util;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.CorextMessages;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaUIStatus;

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
	 * @return status describing the method's result. If <code>status.isOK()</code> returns <code>true</code> then the resources are committable.
	 *
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
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
		List<IResource> readOnlyFiles= new ArrayList<>();
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE && isReadOnly(resource))
				readOnlyFiles.add(resource);
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
		for (Map.Entry<IFile, Long> entry : oldTimeStamps.entrySet()) {
			IFile file = entry.getKey();
			if (!entry.getValue().equals(newTimeStamps.get(file)))
				modified= addModified(modified, file);
		}
		if (modified != null)
			return modified;
		return Status.OK_STATUS;
	}

	private static Map<IFile, Long> createModificationStampMap(List<IResource> files){
		Map<IFile, Long> map= new HashMap<>();
		for (IResource iResource : files) {
			IFile file= (IFile)iResource;
			map.put(file, file.getModificationStamp());
		}
		return map;
	}

	private static IStatus addModified(IStatus status, IFile file) {
		IStatus entry= JavaUIStatus.createError(
			IJavaStatusConstants.VALIDATE_EDIT_CHANGED_CONTENT,
			Messages.format(CorextMessages.Resources_fileModified, BasicElementLabels.getPathLabel(file.getFullPath(), false)),
			null);
		if (status == null) {
			return entry;
		} else if (status.isMultiStatus()) {
			((MultiStatus)status).add(entry);
			return status;
		} else {
			MultiStatus result= new MultiStatus(JavaManipulation.getPreferenceNodeId(),
				IJavaStatusConstants.VALIDATE_EDIT_CHANGED_CONTENT,
				CorextMessages.Resources_modifiedResources, null);
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
			Messages.format(CorextMessages.Resources_outOfSync, BasicElementLabels.getPathLabel(resource.getFullPath(), false)),
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
				CorextMessages.Resources_outOfSyncResources, null);
			result.add(status);
			result.add(entry);
			return result;
		}
	}

	/**
	 * This method is used to generate a list of local locations to
	 * be used in DnD for file transfers.
	 *
	 * @param resources the array of resources to get the local
	 *  locations for
	 * @return the local locations
	 */
	public static String[] getLocationOSStrings(IResource[] resources) {
		List<String> result= new ArrayList<>(resources.length);
		for (IResource resource : resources) {
			IPath location= resource.getLocation();
			if (location != null)
				result.add(location.toOSString());
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Returns the location of the given resource. For local
	 * resources this is the OS path in the local file system. For
	 * remote resource this is the URI.
	 *
	 * @param resource the resource
	 * @return the location string or <code>null</code> if the
	 *  location URI of the resource is <code>null</code>
	 */
	public static String getLocationString(IResource resource) {
		URI uri= resource.getLocationURI();
		if (uri == null)
			return null;
		return EFS.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())
			? new File(uri).getAbsolutePath()
			: uri.toString();
	}

	public static boolean isReadOnly(IResource resource) {
		ResourceAttributes resourceAttributes = resource.getResourceAttributes();
		if (resourceAttributes == null)  // not supported on this platform for this resource
			return false;
		return resourceAttributes.isReadOnly();
	}

	static void setReadOnly(IResource resource, boolean readOnly) {
		ResourceAttributes resourceAttributes = resource.getResourceAttributes();
		if (resourceAttributes == null) // not supported on this platform for this resource
			return;

		resourceAttributes.setReadOnly(readOnly);
		try {
			resource.setResourceAttributes(resourceAttributes);
		} catch (CoreException e) {
			JavaManipulationPlugin.log(e);
		}
	}
}
