/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

class CPListLabelProvider extends LabelProvider {
		
	private String fNewLabel, fClassLabel, fCreateLabel;
	private ImageDescriptor fJarIcon, fExtJarIcon, fJarWSrcIcon, fExtJarWSrcIcon;
	private ImageDescriptor fFolderImage, fProjectImage, fVariableImage, fContainerImage;
	
	private ImageDescriptorRegistry fRegistry;
	
	public CPListLabelProvider() {
		fNewLabel= NewWizardMessages.getString("CPListLabelProvider.new"); //$NON-NLS-1$
		fClassLabel= NewWizardMessages.getString("CPListLabelProvider.classcontainer"); //$NON-NLS-1$
		fCreateLabel= NewWizardMessages.getString("CPListLabelProvider.willbecreated"); //$NON-NLS-1$
		fRegistry= JavaPlugin.getImageDescriptorRegistry();
		
		fJarIcon= JavaPluginImages.DESC_OBJS_JAR;
		fExtJarIcon= JavaPluginImages.DESC_OBJS_EXTJAR;
		fJarWSrcIcon= JavaPluginImages.DESC_OBJS_JAR_WSRC;
		fExtJarWSrcIcon= JavaPluginImages.DESC_OBJS_EXTJAR_WSRC;
		fFolderImage= JavaPluginImages.DESC_OBJS_PACKFRAG_ROOT;
		fContainerImage= JavaPluginImages.DESC_OBJS_LIBRARY;
		fVariableImage= JavaPluginImages.DESC_OBJS_ENV_VAR;

		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		fProjectImage= workbench.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_PROJECT);
	}
	
	public String getText(Object element) {
		if (element instanceof CPListElement) {
			CPListElement cpentry= (CPListElement)element;
			IPath path= cpentry.getPath();
			switch (cpentry.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY: {
					IResource resource= cpentry.getResource();
					if (resource instanceof IFolder) {
						StringBuffer buf= new StringBuffer(path.makeRelative().toString());
						buf.append(' ');
						buf.append(fClassLabel);
						if (!resource.exists()) {
							buf.append(' ');
							if (cpentry.isMissing()) {
								buf.append(fCreateLabel);
							} else {
								buf.append(fNewLabel);
							}
						}
						return buf.toString();
					} else if (resource instanceof IFile) {
						if (ArchiveFileFilter.isArchivePath(path)) {
							String[] args= new String[] { path.lastSegment(), path.removeLastSegments(1).makeRelative().toString() };
							return NewWizardMessages.getFormattedString("CPListLabelProvider.twopart", args); //$NON-NLS-1$
						}
					} else {
						if (ArchiveFileFilter.isArchivePath(path)) {
							String[] args= new String[] { path.lastSegment(), path.removeLastSegments(1).toOSString() };
							return NewWizardMessages.getFormattedString("CPListLabelProvider.twopart", args); //$NON-NLS-1$
						}
					}
					// should not come here
					return path.makeRelative().toString();									
				}
				case IClasspathEntry.CPE_VARIABLE: {
					String name= path.makeRelative().toString();
					StringBuffer buf= new StringBuffer(name);
					IPath entryPath= JavaCore.getClasspathVariable(path.segment(0));
					if (entryPath != null) {
						buf.append(" - "); //$NON-NLS-1$
						buf.append(entryPath.append(path.removeFirstSegments(1)).toOSString());
					}
					return buf.toString();
				}
				case IClasspathEntry.CPE_PROJECT:
					return path.lastSegment();
				case IClasspathEntry.CPE_CONTAINER:
					try {
						IClasspathContainer container= JavaCore.getClasspathContainer(cpentry.getPath(), cpentry.getJavaProject());
						if (container != null) {
							return container.getDescription();
						}
					} catch (JavaModelException e) {
						
					}
					return path.toString();		
				case IClasspathEntry.CPE_SOURCE: {
					StringBuffer buf= new StringBuffer(path.makeRelative().toString());
					IResource resource= cpentry.getResource();
					if (resource != null && !resource.exists()) {
						buf.append(' ');
						if (cpentry.isMissing()) {
							buf.append(fCreateLabel);
						} else {
							buf.append(fNewLabel);
						}
					}
					return buf.toString();
				}
				default:
					// pass
			}
		}
		return super.getText(element);
	}
	
	private ImageDescriptor getBaseImage(CPListElement cpentry) {
		switch (cpentry.getEntryKind()) {
			case IClasspathEntry.CPE_SOURCE:
				return fFolderImage;
			case IClasspathEntry.CPE_LIBRARY:
				IResource res= cpentry.getResource();
				if (res == null) {
					if (cpentry.getSourceAttachmentPath() == null) {
						return fExtJarIcon;
					} else {
						return fExtJarWSrcIcon;
					}
				} else if (res instanceof IFile) {
					if (cpentry.getSourceAttachmentPath() == null) {
						return fJarIcon;
					} else {
						return fJarWSrcIcon;
					}
				} else {
					return fFolderImage;
				}
			case IClasspathEntry.CPE_PROJECT:
				return fProjectImage;
			case IClasspathEntry.CPE_VARIABLE:
				return fVariableImage;
			case IClasspathEntry.CPE_CONTAINER:
				return fContainerImage;
			default:
				return null;
		}
	}			
		
	public Image getImage(Object element) {
		if (element instanceof CPListElement) {
			CPListElement cpentry= (CPListElement) element;
			ImageDescriptor imageDescriptor= getBaseImage(cpentry);
			if (imageDescriptor != null) {
				if (cpentry.isMissing()) {
					imageDescriptor= new JavaElementImageDescriptor(imageDescriptor, JavaElementImageDescriptor.WARNING, JavaElementImageProvider.SMALL_SIZE);
				}
				return fRegistry.get(imageDescriptor);
			}
		}
		return null;
	}


}	