/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.core.util.ISourceAttribute;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jdt.ui.jarpackager.IJarDescriptionWriter;
import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.jdt.ui.jarpackager.JarWriter;

/**
 * Operation for exporting a resource and its children to a new  JAR file.
 */
public class JarFileExportOperation extends WorkspaceModifyOperation implements IJarExportRunnable {

	private static class MessageMultiStatus extends MultiStatus {
		MessageMultiStatus(String pluginId, int code, String message, Throwable exception) {
			super(pluginId, code, message, exception);
		}
		/*
		 * allows to change the message
		 */
		protected void setMessage(String message) {
			super.setMessage(message);
		}
	}

	private JarWriter fJarWriter;
	private JarPackageData fJarPackage;
	private JarPackageData[] fJarPackages;
	private Shell fParentShell;
	private Map fJavaNameToClassFilesMap;
	private IContainer fClassFilesMapContainer;
	private Set fExportedClassContainers;
	private MessageMultiStatus fStatus;
	private StandardJavaElementContentProvider fJavaElementContentProvider;
	private boolean fFilesSaved;
	
	/**
	 * Creates an instance of this class.
	 *
	 * @param	jarPackage	the JAR package specification
	 * @param	parent	the parent for the dialog,
	 * 			or <code>null</code> if no dialog should be presented
	 */
	public JarFileExportOperation(JarPackageData jarPackage, Shell parent) {
		this(new JarPackageData[] {jarPackage}, parent);
	}

	/**
	 * Creates an instance of this class.
	 *
	 * @param	jarPackages		an array with JAR package data objects
	 * @param	parent			the parent for the dialog,
	 * 			or <code>null</code> if no dialog should be presented
	 */
	public JarFileExportOperation(JarPackageData[] jarPackages, Shell parent) {
		this(parent);
		fJarPackages= jarPackages;
	}

	private JarFileExportOperation(Shell parent) {
		fParentShell= parent;
		fStatus= new MessageMultiStatus(JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		fJavaElementContentProvider= new StandardJavaElementContentProvider();
	}

	protected void addToStatus(CoreException ex) {
		IStatus status= ex.getStatus();
		String message= ex.getLocalizedMessage();
		if (message == null || message.length() < 1) {
			message= JarPackagerMessages.getString("JarFileExportOperation.coreErrorDuringExport"); //$NON-NLS-1$
			status= new Status(status.getSeverity(), status.getPlugin(), status.getCode(), message, ex);
		}		
		fStatus.add(status);
	}

	/**
	 * Adds a new info to the list with the passed information.
	 * Normally the export operation continues after a warning.
	 * @param	message		the message
	 * @param	error 	the throwable that caused the warning, or <code>null</code>
	 */
	protected void addInfo(String message, Throwable error) {
		fStatus.add(new Status(IStatus.INFO, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, error));
	}

	/**
	 * Adds a new warning to the list with the passed information.
	 * Normally the export operation continues after a warning.
	 * @param	message		the message
	 * @param	error	the throwable that caused the warning, or <code>null</code>
	 */
	protected void addWarning(String message, Throwable error) {
		fStatus.add(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, error));
	}
	
	/**
	 * Adds a new error to the list with the passed information.
	 * Normally an error terminates the export operation.
	 * @param	message		the message
	 * @param	error 	the throwable that caused the error, or <code>null</code>
	 */
	protected void addError(String message, Throwable error) {
		fStatus.add(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, error));
	}

	/**
	 * Answers the number of file resources specified by the JAR package.
	 *
	 * @return int
	 */
	protected int countSelectedElements() {
		Set enclosingJavaProjects= new HashSet(10);
		int count= 0;
		
		int n= fJarPackage.getElements().length;
		for (int i= 0; i < n; i++) {
			Object element= fJarPackage.getElements()[i];
			
			IJavaProject javaProject= getEnclosingJavaProject(element);
			if (javaProject != null)
				enclosingJavaProjects.add(javaProject);
			
			IResource resource= null;
			if (element instanceof IJavaElement) {
				IJavaElement je= (IJavaElement)element;
				try {
					resource= je.getUnderlyingResource();
				} catch (JavaModelException ex) {
					continue;
				}
				
				// Should not happen since we only export source files
				if (resource == null)
					continue;
			}
			else
				resource= (IResource)element;

			if (resource.getType() == IResource.FILE)
				count++;
			else
				count += getTotalChildCount((IContainer)resource);
		}
		
		if (fJarPackage.areOutputFoldersExported()) {
			if (!fJarPackage.areJavaFilesExported())
				count= 0;
			Iterator iter= enclosingJavaProjects.iterator();
			while (iter.hasNext()) {
				IJavaProject javaProject= (IJavaProject)iter.next();
				IContainer[] outputContainers;
				try {
					outputContainers= getOutputContainers(javaProject);
				} catch (CoreException ex) {
					addToStatus(ex);
					continue;
				}
				for (int i= 0; i < outputContainers.length; i++)
					count += getTotalChildCount(outputContainers[i]);

			}
		}
		
		return count;
	}
	
	private int getTotalChildCount(IContainer container) {
		IResource[] members;
		try {
			members= container.members();
		} catch (CoreException ex) {
			return 0;
		}
		int count= 0;
		for (int i= 0; i < members.length; i++) {
			if (members[i].getType() == IResource.FILE)
				count++;
			else
				count += getTotalChildCount((IContainer)members[i]);
		}
		return count;
	}

	/**
	 * Exports the passed resource to the JAR file
	 *
	 * @param element the resource or JavaElement to export
	 */
	protected void exportElement(Object element, IProgressMonitor progressMonitor) throws InterruptedException {
		int leadSegmentsToRemove= 1;
		IPackageFragmentRoot pkgRoot= null;
		boolean isInJavaProject= false;
		IResource resource= null;
		IJavaProject jProject= null;
		if (element instanceof IJavaElement) {
			isInJavaProject= true;
			IJavaElement je= (IJavaElement)element;
			int type= je.getElementType();
			if (type != IJavaElement.CLASS_FILE && type != IJavaElement.COMPILATION_UNIT) {
				exportJavaElement(progressMonitor, je);
				return;
			}
			try {
				resource= je.getUnderlyingResource();
			} catch (JavaModelException ex) {
				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.underlyingResourceNotFound", je.getElementName()), ex); //$NON-NLS-1$
				return;
			}
			jProject= je.getJavaProject();
			pkgRoot= JavaModelUtil.getPackageFragmentRoot(je);
		}
		else
			resource= (IResource)element;

		if (!resource.isAccessible()) {
			addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.resourceNotFound", resource.getFullPath()), null); //$NON-NLS-1$
			return;
		}

		if (resource.getType() == IResource.FILE) {
			if (!resource.isLocal(IResource.DEPTH_ZERO))
				try {
					resource.setLocal(true , IResource.DEPTH_ZERO, progressMonitor);
				} catch (CoreException ex) {
					addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.resourceNotLocal", resource.getFullPath()), ex); //$NON-NLS-1$
					return;
				}
			if (!isInJavaProject) {
				// check if it's a Java resource
				try {
					isInJavaProject= resource.getProject().hasNature(JavaCore.NATURE_ID);
				} catch (CoreException ex) {
					addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.projectNatureNotDeterminable", resource.getFullPath()), ex); //$NON-NLS-1$
					return;
				}
				if (isInJavaProject) {
					jProject= JavaCore.create(resource.getProject());
					try {
						IPackageFragment pkgFragment= jProject.findPackageFragment(resource.getFullPath().removeLastSegments(1));
						if (pkgFragment != null)
							pkgRoot= JavaModelUtil.getPackageFragmentRoot(pkgFragment);
						else
							pkgRoot= findPackageFragmentRoot(jProject, resource.getFullPath().removeLastSegments(1));
					} catch (JavaModelException ex) {
						addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.javaPackageNotDeterminable", resource.getFullPath()), ex); //$NON-NLS-1$
						return;
					}
				}
			}
			
			if (pkgRoot != null) {
				leadSegmentsToRemove= pkgRoot.getPath().segmentCount();
				boolean isOnBuildPath;
				isOnBuildPath= jProject.isOnClasspath(resource);
				if (!isOnBuildPath || (mustUseSourceFolderHierarchy() && !pkgRoot.getElementName().equals(IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH)))
					leadSegmentsToRemove--;
			}
			
			IPath destinationPath= resource.getFullPath().removeFirstSegments(leadSegmentsToRemove);
			
			boolean isInOutputFolder= false;
			if (isInJavaProject) {
				try {
					isInOutputFolder= jProject.getOutputLocation().isPrefixOf(resource.getFullPath());
				} catch (JavaModelException ex) {
					isInOutputFolder= false;
				}
			}
			
			exportClassFiles(progressMonitor, pkgRoot, resource, jProject, destinationPath);
			exportResource(progressMonitor, pkgRoot, isInJavaProject, resource, destinationPath, isInOutputFolder);

			progressMonitor.worked(1);
			ModalContext.checkCanceled(progressMonitor);

		} else
			exportContainer(progressMonitor, (IContainer)resource);
	}

	private void exportJavaElement(IProgressMonitor progressMonitor, IJavaElement je) throws InterruptedException {
		if (je.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT && ((IPackageFragmentRoot)je).isArchive())
			return;

		Object[] children= fJavaElementContentProvider.getChildren(je);
		for (int i= 0; i < children.length; i++)
			exportElement(children[i], progressMonitor);
	}

	private void exportResource(IProgressMonitor progressMonitor, IResource resource, int leadingSegmentsToRemove) throws InterruptedException {
		if (resource instanceof IContainer) {
			IContainer container= (IContainer)resource;
			IResource[] children;
			try {
				children= container.members();
			} catch (CoreException e) {
				// this should never happen because an #isAccessible check is done before #members is invoked
				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.errorDuringExport", container.getFullPath()), e); //$NON-NLS-1$
				return;
			}
			for (int i= 0; i < children.length; i++)
				exportResource(progressMonitor, children[i], leadingSegmentsToRemove);
		} else if (resource instanceof IFile) {
			try {
				IPath destinationPath= resource.getFullPath().removeFirstSegments(leadingSegmentsToRemove);
				progressMonitor.subTask(JarPackagerMessages.getFormattedString("JarFileExportOperation.exporting", destinationPath.toString())); //$NON-NLS-1$
				fJarWriter.write((IFile)resource, destinationPath);
			} catch (CoreException ex) {
				Throwable realEx= ex.getStatus().getException();
				if (realEx instanceof ZipException && realEx.getMessage() != null && realEx.getMessage().startsWith("duplicate entry:")) //$NON-NLS-1$
					addWarning(ex.getMessage(), realEx);
				else
					addToStatus(ex);
			} finally {
				progressMonitor.worked(1);
				ModalContext.checkCanceled(progressMonitor);
			}
		}
	}

	private void exportContainer(IProgressMonitor progressMonitor, IContainer container) throws InterruptedException {
		if (container.getType() == IResource.FOLDER && isOutputFolder((IFolder)container))
			return;
		
		IResource[] children= null;
		try {
			children= container.members();
		} catch (CoreException e) {
			// this should never happen because an #isAccessible check is done before #members is invoked
			addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.errorDuringExport", container.getFullPath()), e); //$NON-NLS-1$
		}
		for (int i= 0; i < children.length; i++)
			exportElement(children[i], progressMonitor);
	}

	private IPackageFragmentRoot findPackageFragmentRoot(IJavaProject jProject, IPath path) throws JavaModelException {
		if (jProject == null || path == null || path.segmentCount() <= 0)
			return null;
		IPackageFragmentRoot pkgRoot= jProject.findPackageFragmentRoot(path);
		if (pkgRoot != null)
			return pkgRoot;
		else 
			return findPackageFragmentRoot(jProject, path.removeLastSegments(1));
	}

	private void exportResource(IProgressMonitor progressMonitor, IPackageFragmentRoot pkgRoot, boolean isInJavaProject, IResource resource, IPath destinationPath, boolean isInOutputFolder) {

		// Handle case where META-INF/MANIFEST.MF is part of the exported files
		if (fJarPackage.areClassFilesExported() && destinationPath.toString().equals("META-INF/MANIFEST.MF")) {//$NON-NLS-1$
			if (fJarPackage.isManifestGenerated())
				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.didNotAddManifestToJar", resource.getFullPath()), null); //$NON-NLS-1$
			return;
		}

		boolean isNonJavaResource= !isInJavaProject || pkgRoot == null;
		boolean isInClassFolder= false;
		try {
			isInClassFolder= pkgRoot != null && !pkgRoot.isArchive() && pkgRoot.getKind() == IPackageFragmentRoot.K_BINARY;
		} catch (JavaModelException ex) {
			addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.cantGetRootKind", resource.getFullPath()), ex); //$NON-NLS-1$
		}
		if ((fJarPackage.areClassFilesExported() &&
					((isNonJavaResource || (pkgRoot != null && !isJavaFile(resource) && !isClassFile(resource)))
					|| isInClassFolder && isClassFile(resource)))
			|| (fJarPackage.areJavaFilesExported() && (isNonJavaResource || (pkgRoot != null && !isClassFile(resource)) || (isInClassFolder && isClassFile(resource) && !fJarPackage.areClassFilesExported())))) {
			try {
				progressMonitor.subTask(JarPackagerMessages.getFormattedString("JarFileExportOperation.exporting", destinationPath.toString())); //$NON-NLS-1$
				fJarWriter.write((IFile) resource, destinationPath);
			} catch (CoreException ex) {
				Throwable realEx= ex.getStatus().getException();
				if (realEx instanceof ZipException && realEx.getMessage() != null && realEx.getMessage().startsWith("duplicate entry:")) //$NON-NLS-1$
					addWarning(ex.getMessage(), realEx);
				else
					addToStatus(ex);
			}
		}					
	}

	private boolean isOutputFolder(IFolder folder) {
		try {
			IJavaProject javaProject= JavaCore.create(folder.getProject());
			IPath outputFolderPath= javaProject.getOutputLocation();
			return folder.getFullPath().equals(outputFolderPath);
		} catch (JavaModelException ex) {
			return false;
		}
	}

	private void exportClassFiles(IProgressMonitor progressMonitor, IPackageFragmentRoot pkgRoot, IResource resource, IJavaProject jProject, IPath destinationPath) {
		if (fJarPackage.areClassFilesExported() && isJavaFile(resource) && pkgRoot != null) {
			try {
				if (!jProject.isOnClasspath(resource))
					return;

				// find corresponding file(s) on classpath and export
				Iterator iter= filesOnClasspath((IFile)resource, destinationPath, jProject, pkgRoot, progressMonitor);
				IPath baseDestinationPath= destinationPath.removeLastSegments(1);
				while (iter.hasNext()) {
					IFile file= (IFile)iter.next();
					if (!resource.isLocal(IResource.DEPTH_ZERO))						
						file.setLocal(true , IResource.DEPTH_ZERO, progressMonitor);
					IPath classFilePath= baseDestinationPath.append(file.getName());
					progressMonitor.subTask(JarPackagerMessages.getFormattedString("JarFileExportOperation.exporting", classFilePath.toString())); //$NON-NLS-1$
					fJarWriter.write(file, classFilePath);
				}
			} catch (CoreException ex) {
				addToStatus(ex);
			}
		}
	}

	/**
	 * Exports the resources as specified by the JAR package.
	 */
	protected void exportSelectedElements(IProgressMonitor progressMonitor) throws InterruptedException {
		fExportedClassContainers= new HashSet(10);
		Set enclosingJavaProjects= new HashSet(10);
		int n= fJarPackage.getElements().length;
		for (int i= 0; i < n; i++) {
			Object element= fJarPackage.getElements()[i];
			exportElement(element, progressMonitor);
			if (fJarPackage.areOutputFoldersExported()) {
				IJavaProject javaProject= getEnclosingJavaProject(element);
				if (javaProject != null)
					enclosingJavaProjects.add(javaProject);
			}
		}
		if (fJarPackage.areOutputFoldersExported())
			exportOutputFolders(progressMonitor, enclosingJavaProjects);
	}
	
	private IJavaProject getEnclosingJavaProject(Object element) {
		if (element instanceof IJavaElement) {
			return ((IJavaElement)element).getJavaProject();
		} else if (element instanceof IResource) {
			IProject project= ((IResource)element).getProject();
			try {
				if (project.hasNature(JavaCore.NATURE_ID))
					return JavaCore.create(project);
			} catch (CoreException ex) {
				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.projectNatureNotDeterminable", project.getFullPath()), ex); //$NON-NLS-1$
			}
		}
		return null;
	}
	
	private void exportOutputFolders(IProgressMonitor progressMonitor, Set javaProjects) throws InterruptedException {
		if (javaProjects == null)
			return;
		
		Iterator iter= javaProjects.iterator();
		while (iter.hasNext()) {
			IJavaProject javaProject= (IJavaProject)iter.next();
			IContainer[] outputContainers;
			try {
				outputContainers= getOutputContainers(javaProject);
			} catch (CoreException ex) {
				addToStatus(ex);
				continue;
			}
			for (int i= 0; i < outputContainers.length; i++)
				exportResource(progressMonitor, outputContainers[i], outputContainers[i].getFullPath().segmentCount());

		}
	}
	
	private IContainer[] getOutputContainers(IJavaProject javaProject) throws CoreException {
		Set outputPaths= new HashSet();
		boolean includeDefaultOutputPath= false;
		IPackageFragmentRoot[] roots= javaProject.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if (roots[i] != null) {
				IClasspathEntry cpEntry= roots[i].getRawClasspathEntry();
				if (cpEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath location= cpEntry.getOutputLocation();
					if (location != null)
						outputPaths.add(location);
					else
						includeDefaultOutputPath= true;
				}
			}
		}
		
		if (includeDefaultOutputPath) {
			// Use default output location
			outputPaths.add(javaProject.getOutputLocation());
		}
		
		// Convert paths to containers
		Set outputContainers= new HashSet(outputPaths.size());
		Iterator iter= outputPaths.iterator();
		while (iter.hasNext()) {
			IPath path= (IPath)iter.next();
			if (javaProject.getProject().getFullPath().equals(path))
				outputContainers.add(javaProject.getProject());
			else {
				IFolder outputFolder= createFolderHandle(path);
				if (outputFolder == null || !outputFolder.isAccessible()) {
					String msg= JarPackagerMessages.getString("JarFileExportOperation.outputContainerNotAccessible"); //$NON-NLS-1$
					addToStatus(new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, msg, null)));
				} else
					outputContainers.add(outputFolder);
			}
		}
		return (IContainer[])outputContainers.toArray(new IContainer[outputContainers.size()]);
	}

	/**
	 * Returns an iterator on a list with files that correspond to the
	 * passed file and that are on the classpath of its project.
	 *
	 * @param	file			the file for which to find the corresponding classpath resources
	 * @param	pathInJar		the path that the file has in the JAR (i.e. project and source folder segments removed)
	 * @param	javaProject		the javaProject that contains the file
	 * @return	the iterator over the corresponding classpath files for the given file
	 * @deprecated As of 2.1 use the method with additional IPackageFragmentRoot paramter
	 */
	protected Iterator filesOnClasspath(IFile file, IPath pathInJar, IJavaProject javaProject, IProgressMonitor progressMonitor) throws CoreException {
		return filesOnClasspath(file, pathInJar, javaProject, null, progressMonitor);
	}
	
	/**
	 * Returns an iterator on a list with files that correspond to the
	 * passed file and that are on the classpath of its project.
	 *
	 * @param	file			the file for which to find the corresponding classpath resources
	 * @param	pathInJar		the path that the file has in the JAR (i.e. project and source folder segments removed)
	 * @param	javaProject		the javaProject that contains the file
	 * @param	pkgRoot			the package fragment root that contains the file
	 * @return	the iterator over the corresponding classpath files for the given file
	 */
	protected Iterator filesOnClasspath(IFile file, IPath pathInJar, IJavaProject javaProject, IPackageFragmentRoot pkgRoot, IProgressMonitor progressMonitor) throws CoreException {
		// Allow JAR Package to provide its own strategy
		IFile[] classFiles= fJarPackage.findClassfilesFor(file);
		if (classFiles != null)
			return Arrays.asList(classFiles).iterator();

		if (!isJavaFile(file))
			return Collections.EMPTY_LIST.iterator();

		IPath outputPath= null;
		if (pkgRoot != null) {
			IClasspathEntry cpEntry= pkgRoot.getRawClasspathEntry();
			if (cpEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE)
				outputPath= cpEntry.getOutputLocation();
		}
		if (outputPath == null)
			// Use default output location
			outputPath= javaProject.getOutputLocation();
				
		IContainer outputContainer;		
		if (javaProject.getProject().getFullPath().equals(outputPath))
			outputContainer= javaProject.getProject();
		else {
			outputContainer= createFolderHandle(outputPath);
			if (outputContainer == null || !outputContainer.isAccessible()) {
				String msg= JarPackagerMessages.getString("JarFileExportOperation.outputContainerNotAccessible"); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, msg, null));
			}
		}

		// Java CU - search files with .class ending
		boolean hasErrors= hasCompileErrors(file);
		boolean hasWarnings= hasCompileWarnings(file);
		boolean canBeExported= canBeExported(hasErrors, hasWarnings);
		if (!canBeExported)
			return Collections.EMPTY_LIST.iterator();
		reportPossibleCompileProblems(file, hasErrors, hasWarnings, canBeExported);
		IContainer classContainer= outputContainer;
		if (pathInJar.segmentCount() > 1)
			classContainer= outputContainer.getFolder(pathInJar.removeLastSegments(1));
			
		if (fExportedClassContainers.contains(classContainer))
			return Collections.EMPTY_LIST.iterator();
			
		if (fClassFilesMapContainer == null || !fClassFilesMapContainer.equals(classContainer)) {
			fJavaNameToClassFilesMap= buildJavaToClassMap(classContainer);
			if (fJavaNameToClassFilesMap == null) {
				// Could not fully build map. fallback is to export whole directory
				IPath location= classContainer.getLocation();
				String containerName= "";  //$NON-NLS-1$
				if (location != null)
					containerName= location.toFile().toString();
				String msg= JarPackagerMessages.getFormattedString("JarFileExportOperation.missingSourceFileAttributeExportedAll", containerName); //$NON-NLS-1$
				addInfo(msg, null);
				fExportedClassContainers.add(classContainer);
				return getClassesIn(classContainer);
			}
			fClassFilesMapContainer= classContainer;
		}
		ArrayList classFileList= (ArrayList)fJavaNameToClassFilesMap.get(file.getName());
		if (classFileList == null || classFileList.isEmpty()) {
			String msg= JarPackagerMessages.getFormattedString("JarFileExportOperation.classFileOnClasspathNotAccessible", file.getFullPath()); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, msg, null));
		}
		return classFileList.iterator();
	}

	private Iterator getClassesIn(IContainer classContainer) throws CoreException {
		IResource[] resources= classContainer.members();
		List files= new ArrayList(resources.length);
		for (int i= 0; i < resources.length; i++)
			if (resources[i].getType() == IResource.FILE && isClassFile(resources[i]))
				files.add(resources[i]);
		return files.iterator();
	}

	/**
	 * Answers whether the given resource is a Java file.
	 * The resource must be a file whose file name ends with ".java".
	 * 
	 * @return a <code>true<code> if the given resource is a Java file
	 */
	boolean isJavaFile(IResource file) {
		return file != null
			&& file.getType() == IResource.FILE
			&& file.getFileExtension() != null
			&& file.getFileExtension().equalsIgnoreCase("java"); //$NON-NLS-1$
	}

	/**
	 * Answers whether the given resource is a class file.
	 * The resource must be a file whose file name ends with ".class".
	 * 
	 * @return a <code>true<code> if the given resource is a class file
	 */
	boolean isClassFile(IResource file) {
		return file != null
			&& file.getType() == IResource.FILE
			&& file.getFileExtension() != null
			&& file.getFileExtension().equalsIgnoreCase("class"); //$NON-NLS-1$
	}

	/*
	 * Builds and returns a map that has the class files
	 * for each java file in a given directory
	 */
	private Map buildJavaToClassMap(IContainer container) throws CoreException {
		if (container == null || !container.isAccessible())
			return new HashMap(0);
		/*
		 * XXX: Bug 6584: Need a way to get class files for a java file (or CU)
		 */
		IClassFileReader cfReader= null;
		IResource[] members= container.members();
		Map map= new HashMap(members.length);
		for (int i= 0;  i < members.length; i++) {
			if (isClassFile(members[i])) {
				IFile classFile= (IFile)members[i];
				IPath location= classFile.getLocation();
				if (location != null) {
					File file= location.toFile();
					cfReader= ToolFactory.createDefaultClassFileReader(location.toOSString(), IClassFileReader.CLASSFILE_ATTRIBUTES);
					if (cfReader != null) {
						ISourceAttribute sourceAttribute= cfReader.getSourceFileAttribute();
						if (sourceAttribute == null) {
							/*
							 * Can't fully build the map because one or more
							 * class file does not contain the name of its 
							 * source file.
							 */
							addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.classFileWithoutSourceFileAttribute", file), null); //$NON-NLS-1$
							return null;
						}
						String javaName= new String(sourceAttribute.getSourceFileName());
						Object classFiles= map.get(javaName);
						if (classFiles == null) {
							classFiles= new ArrayList(3);
							map.put(javaName, classFiles);
						}
						((ArrayList)classFiles).add(classFile);
					}
				}
			}		
		}
		return map;
	}

	/**
	 * Creates a file resource handle for the file with the given workspace path.
	 * This method does not create the file resource; this is the responsibility
	 * of <code>createFile</code>.
	 *
	 * @param filePath the path of the file resource to create a handle for
	 * @return the new file resource handle
	 */
	protected IFile createFileHandle(IPath filePath) {
		if (filePath.isValidPath(filePath.toString()) && filePath.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFile(filePath);
		else
			return null;
	}

	/**
	 * Creates a folder resource handle for the folder with the given workspace path.
	 *
	 * @param folderPath the path of the folder to create a handle for
	 * @return the new folder resource handle
	 */
	protected IFolder createFolderHandle(IPath folderPath) {
		if (folderPath.isValidPath(folderPath.toString()) && folderPath.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFolder(folderPath);
		else
			return null;
	}

	/**
	 * Returns the status of this operation.
	 * The result is a status object containing individual
	 * status objects.
	 *
	 * @return the status of this operation
	 */
	public IStatus getStatus() {
		String message= null;
		switch (fStatus.getSeverity()) {
			case IStatus.OK:
				message= ""; //$NON-NLS-1$
				break;
			case IStatus.INFO:
				message= JarPackagerMessages.getString("JarFileExportOperation.exportFinishedWithInfo"); //$NON-NLS-1$
				break;
			case IStatus.WARNING:
				message= JarPackagerMessages.getString("JarFileExportOperation.exportFinishedWithWarnings"); //$NON-NLS-1$
				break;
			case IStatus.ERROR:
				if (fJarPackages.length > 1)
					message= JarPackagerMessages.getString("JarFileExportOperation.creationOfSomeJARsFailed"); //$NON-NLS-1$
				else
					message= JarPackagerMessages.getString("JarFileExportOperation.jarCreationFailed"); //$NON-NLS-1$
				break;
			default:
				// defensive code in case new severity is defined
				message= ""; //$NON-NLS-1$
				break;
		}
		fStatus.setMessage(message);
		return fStatus;
	}

	/**
	 * Answer a boolean indicating whether the passed child is a descendant
	 * of one or more members of the passed resources collection
	 *
	 * @param	resources	a List contain potential parents
	 * @param	child		the resource to test
	 * @return	a <code>boolean</code> indicating if the child is a descendant
	 */
	protected boolean isDescendant(List resources, IResource child) {
		if (child.getType() == IResource.PROJECT)
			return false;

		IResource parent= child.getParent();
		if (resources.contains(parent))
			return true;

		return isDescendant(resources, parent);
	}

	protected boolean canBeExported(boolean hasErrors, boolean hasWarnings) throws CoreException {
		return (!hasErrors && !hasWarnings)
			|| (hasErrors && fJarPackage.areErrorsExported())
			|| (hasWarnings && fJarPackage.exportWarnings());
	}

	protected void reportPossibleCompileProblems(IFile file, boolean hasErrors, boolean hasWarnings, boolean canBeExported) {
		if (hasErrors) {
			if (canBeExported)
				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.exportedWithCompileErrors", file.getFullPath()), null); //$NON-NLS-1$
			else
				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.notExportedDueToCompileErrors", file.getFullPath()), null); //$NON-NLS-1$
		}
		if (hasWarnings) {
			if (canBeExported)
				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.exportedWithCompileWarnings", file.getFullPath()), null); //$NON-NLS-1$
			else
				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.notExportedDueToCompileWarnings", file.getFullPath()), null); //$NON-NLS-1$
		}
	}

	/**
	 * Exports the resources as specified by the JAR package.
	 * 
	 * @param	progressMonitor	the progress monitor that displays the progress
	 * @see	#getStatus()
	 */
	protected void execute(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
		int count= fJarPackages.length;
		progressMonitor.beginTask("", count); //$NON-NLS-1$
		try {
			for (int i= 0; i < count; i++) {
				SubProgressMonitor subProgressMonitor= new SubProgressMonitor(progressMonitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
				fJarPackage= fJarPackages[i];
				if (fJarPackage != null)
					singleRun(subProgressMonitor);
			}
		} finally {
			progressMonitor.done();
		}
	}

	public void singleRun(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
		try {
			if (!preconditionsOK())
				throw new InvocationTargetException(null, JarPackagerMessages.getString("JarFileExportOperation.jarCreationFailedSeeDetails")); //$NON-NLS-1$
			int totalWork= countSelectedElements();
			if ((!isAutoBuilding() && fJarPackage.isBuildingIfNeeded() && fJarPackage.areGeneratedFilesExported()) || fFilesSaved) {
				int subMonitorTicks= totalWork/10;
				totalWork += subMonitorTicks;
				progressMonitor.beginTask("", totalWork); //$NON-NLS-1$
				SubProgressMonitor subProgressMonitor= new SubProgressMonitor(progressMonitor, subMonitorTicks, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
				buildProjects(subProgressMonitor);
			} else
				progressMonitor.beginTask("", totalWork); //$NON-NLS-1$
						
			fJarWriter= fJarPackage.createJarWriter(fParentShell);
			exportSelectedElements(progressMonitor);
			if (getStatus().getSeverity() != IStatus.ERROR) {
				progressMonitor.subTask(JarPackagerMessages.getString("JarFileExportOperation.savingFiles")); //$NON-NLS-1$
				saveFiles();
			}
		} catch (CoreException ex) {
			addToStatus(ex);
		} finally {
			try {
				if (fJarWriter != null)
					fJarWriter.close();
			} catch (CoreException ex) {
				addToStatus(ex);
			}
			progressMonitor.done();
		}
	}
	
	protected boolean preconditionsOK() {
		if (!fJarPackage.areGeneratedFilesExported() && !fJarPackage.areJavaFilesExported()) {
			addError(JarPackagerMessages.getString("JarFileExportOperation.noExportTypeChosen"), null); //$NON-NLS-1$
			return false;
		}
		if (fJarPackage.getElements() == null || fJarPackage.getElements().length == 0) {
			addError(JarPackagerMessages.getString("JarFileExportOperation.noResourcesSelected"), null); //$NON-NLS-1$
			return false;
		}
		if (fJarPackage.getAbsoluteJarLocation() == null) {
			addError(JarPackagerMessages.getString("JarFileExportOperation.invalidJarLocation"), null); //$NON-NLS-1$
			return false;
		}
		File targetFile= fJarPackage.getAbsoluteJarLocation().toFile();
		if (targetFile.exists() && !targetFile.canWrite()) {
			addError(JarPackagerMessages.getString("JarFileExportOperation.jarFileExistsAndNotWritable"), null); //$NON-NLS-1$
			return false;
		}
		if (!fJarPackage.isManifestAccessible()) {
			addError(JarPackagerMessages.getString("JarFileExportOperation.manifestDoesNotExist"), null); //$NON-NLS-1$
			return false;
		}
		if (!fJarPackage.isMainClassValid(new BusyIndicatorRunnableContext())) {
			addError(JarPackagerMessages.getString("JarFileExportOperation.invalidMainClass"), null); //$NON-NLS-1$
			return false;
		}
		
		if (fParentShell == null)
			// no checking if shell is null
			return true;

		IFile[] unsavedFiles= getUnsavedFiles();
		if (unsavedFiles.length > 0)
			return saveModifiedResourcesIfUserConfirms(unsavedFiles);

		return true;
	}

	/**
	 * Returns the files which are not saved and which are
	 * part of the files being exported.
	 * 
	 * @return an array of unsaved files
	 */
	private IFile[] getUnsavedFiles() {
		IEditorPart[] dirtyEditors= getDirtyEditors(fParentShell);
		Set unsavedFiles= new HashSet(dirtyEditors.length);
		if (dirtyEditors.length > 0) {
			List selection= JarPackagerUtil.asResources(fJarPackage.getElements());
			for (int i= 0; i < dirtyEditors.length; i++) {
				if (dirtyEditors[i].getEditorInput() instanceof IFileEditorInput) {
					IFile dirtyFile= ((IFileEditorInput)dirtyEditors[i].getEditorInput()).getFile();
					if (JarPackagerUtil.contains(selection, dirtyFile)) {
						unsavedFiles.add(dirtyFile);
					}
				}
			}
		}
		return (IFile[])unsavedFiles.toArray(new IFile[unsavedFiles.size()]);
	}

	/**
	 * Asks the user to confirm to save the modified resources.
	 * 
	 * @return true if user pressed OK.
	 */
	private boolean confirmSaveModifiedResources(final IFile[] dirtyFiles) {
		if (dirtyFiles == null || dirtyFiles.length == 0)
			return true;

		// Get display for further UI operations
		Display display= fParentShell.getDisplay();
		if (display == null || display.isDisposed())
			return false;

		// Ask user to confirm saving of all files
		final int[] intResult= new int[1];
		Runnable runnable= new Runnable() {
			public void run() {
				ConfirmSaveModifiedResourcesDialog dlg= new ConfirmSaveModifiedResourcesDialog(fParentShell, dirtyFiles);
				intResult[0]= dlg.open();
			}
		};
		display.syncExec(runnable);

		return intResult[0] == IDialogConstants.OK_ID;
	}

	/**
	 * Asks to confirm to save the modified resources
	 * and save them if OK is pressed.
	 * 
	 * @return true if user pressed OK and save was successful.
	 */
	private boolean saveModifiedResourcesIfUserConfirms(IFile[] dirtyFiles) {
		if (confirmSaveModifiedResources(dirtyFiles))
			return saveModifiedResources(dirtyFiles);

		// Report unsaved files
		for (int i= 0; i < dirtyFiles.length; i++)
			addError(JarPackagerMessages.getFormattedString("JarFileExportOperation.fileUnsaved", dirtyFiles[i].getFullPath()), null); //$NON-NLS-1$
		return false;
	}

	/**
	 * Save all of the editors in the workbench.  
	 * 
	 * @return true if successful.
	 */
	private boolean saveModifiedResources(final IFile[] dirtyFiles) {
		// Get display for further UI operations
		Display display= fParentShell.getDisplay();
		if (display == null || display.isDisposed())
			return false;
		
		final boolean[] retVal= new boolean[1];
		Runnable runnable= new Runnable() {
			public void run() {
				try {
					PlatformUI.getWorkbench().getProgressService().runInUI(
						PlatformUI.getWorkbench().getProgressService(),
						createSaveModifiedResourcesRunnable(dirtyFiles),
						ResourcesPlugin.getWorkspace().getRoot());
					retVal[0]= true;
				} catch (InvocationTargetException ex) {
					addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingModifiedResources"), ex); //$NON-NLS-1$
					JavaPlugin.log(ex);
					retVal[0]= false;
				} catch (InterruptedException ex) {
						Assert.isTrue(false); // Can't happen. Operation isn't cancelable.
						retVal[0]= false;
				}
	 		}
		};
		fFilesSaved= false;
		display.syncExec(runnable);
		if (retVal[0])
			fFilesSaved= true;
		return retVal[0];
	}

	private IRunnableWithProgress createSaveModifiedResourcesRunnable(final IFile[] dirtyFiles) {
		return new IRunnableWithProgress() {
			public void run(final IProgressMonitor pm) {
				IEditorPart[] editorsToSave= getDirtyEditors(fParentShell);
				pm.beginTask(JarPackagerMessages.getString("JarFileExportOperation.savingModifiedResources"), editorsToSave.length); //$NON-NLS-1$
				try {
					List dirtyFilesList= Arrays.asList(dirtyFiles);
					for (int i= 0; i < editorsToSave.length; i++) {
						if (editorsToSave[i].getEditorInput() instanceof IFileEditorInput) {
							IFile dirtyFile= ((IFileEditorInput)editorsToSave[i].getEditorInput()).getFile();					
							if (dirtyFilesList.contains((dirtyFile)))
								editorsToSave[i].doSave(new SubProgressMonitor(pm, 1));
						}
						pm.worked(1);
					}
				} finally {
					pm.done();
				}
			}
		};
	}

	protected void saveFiles() {
		// Save the manifest
		if (fJarPackage.areGeneratedFilesExported() && fJarPackage.isManifestGenerated() && fJarPackage.isManifestSaved()) {
			try {
				saveManifest();
			} catch (CoreException ex) {
				addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingManifest"), ex); //$NON-NLS-1$
			} catch (IOException ex) {
				addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingManifest"), ex); //$NON-NLS-1$
			}
		}
		
		// Save the description
		if (fJarPackage.isDescriptionSaved()) {
			try {
				saveDescription();
			} catch (CoreException ex) {
				addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingDescription"), ex); //$NON-NLS-1$
			} catch (IOException ex) {
				addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingDescription"), ex); //$NON-NLS-1$
			}
		}
	}

	private IEditorPart[] getDirtyEditors(Shell parent) {
		Display display= parent.getDisplay();
		final Object[] result= new Object[1];
		display.syncExec(
			new Runnable() {
				public void run() {
					result[0]= JavaPlugin.getDirtyEditors();
				}
			}
		);
		return (IEditorPart[])result[0];
	}

	protected void saveDescription() throws CoreException, IOException {
		// Adjust JAR package attributes
		if (fJarPackage.isManifestReused())
			fJarPackage.setGenerateManifest(false);
		ByteArrayOutputStream objectStreamOutput= new ByteArrayOutputStream();
		IJarDescriptionWriter writer= fJarPackage.createJarDescriptionWriter(objectStreamOutput);
		ByteArrayInputStream fileInput= null;
		try {
			writer.write(fJarPackage);
			fileInput= new ByteArrayInputStream(objectStreamOutput.toByteArray());
			IFile descriptionFile= fJarPackage.getDescriptionFile();
			if (descriptionFile.isAccessible()) {
				if (fJarPackage.allowOverwrite() || JarPackagerUtil.askForOverwritePermission(fParentShell, descriptionFile.getFullPath().toString()))
					descriptionFile.setContents(fileInput, true, true, null);
			} else
				descriptionFile.create(fileInput, true, null);
		} finally {
			if (fileInput != null)
				fileInput.close();
			if (writer != null)
				writer.close();
		}
	}

	protected void saveManifest() throws CoreException, IOException {
		ByteArrayOutputStream manifestOutput= new ByteArrayOutputStream();
		ByteArrayInputStream fileInput= null;
		try {
			Manifest manifest= fJarPackage.getManifestProvider().create(fJarPackage);
			manifest.write(manifestOutput);
			fileInput= new ByteArrayInputStream(manifestOutput.toByteArray());
			IFile manifestFile= fJarPackage.getManifestFile();
			if (manifestFile.isAccessible()) {
				if (fJarPackage.allowOverwrite() || JarPackagerUtil.askForOverwritePermission(fParentShell, manifestFile.getFullPath().toString()))
					manifestFile.setContents(fileInput, true, true, null);
			} else
				manifestFile.create(fileInput, true, null);
		} finally {
			if (manifestOutput != null)
				manifestOutput.close();
			if (fileInput != null)
				fileInput.close();
		}
	}
	
	private boolean isAutoBuilding() {
		return ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
	}

	private void buildProjects(IProgressMonitor progressMonitor) {
		Set builtProjects= new HashSet(10);
		Object[] elements= fJarPackage.getElements();
		for (int i= 0; i < elements.length; i++) {
			IProject project= null;
			Object element= elements[i];
			if (element instanceof IResource)
				project= ((IResource)element).getProject();
			else if (element instanceof IJavaElement)
				project= ((IJavaElement)element).getJavaProject().getProject();
			if (project != null && !builtProjects.contains(project)) {
				try {
					project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, progressMonitor);
				} catch (CoreException ex) {
					String message= JarPackagerMessages.getFormattedString("JarFileExportOperation.errorDuringProjectBuild", project.getFullPath()); //$NON-NLS-1$
					addError(message, ex);
				} finally {
					// don't try to build same project a second time even if it failed
					builtProjects.add(project);
				}
			}
		}
	}

	/**
	 * Tells whether the given resource (or its children) have compile errors.
	 * The method acts on the current build state and does not recompile.
	 * 
	 * @param resource the resource to check for errors
	 * @return <code>true</code> if the resource (and its children) are error free
	 * @throws CoreException import org.eclipse.core.runtime.CoreException if there's a marker problem
	 */
	private boolean hasCompileErrors(IResource resource) throws CoreException {
		IMarker[] problemMarkers= resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		for (int i= 0; i < problemMarkers.length; i++) {
			if (problemMarkers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
				return true;
		}
		return false;
	}

	/**
	 * Tells whether the given resource (or its children) have compile errors.
	 * The method acts on the current build state and does not recompile.
	 * 
	 * @param resource the resource to check for errors
	 * @return <code>true</code> if the resource (and its children) are error free
	 * @throws CoreException import org.eclipse.core.runtime.CoreException if there's a marker problem
	 */
	private boolean hasCompileWarnings(IResource resource) throws CoreException {
		IMarker[] problemMarkers= resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		for (int i= 0; i < problemMarkers.length; i++) {
			if (problemMarkers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_WARNING)
				return true;
		}
		return false;
	}

	private boolean mustUseSourceFolderHierarchy() {
		return fJarPackage.useSourceFolderHierarchy() && fJarPackage.areJavaFilesExported() && !fJarPackage.areGeneratedFilesExported();
	}
}
