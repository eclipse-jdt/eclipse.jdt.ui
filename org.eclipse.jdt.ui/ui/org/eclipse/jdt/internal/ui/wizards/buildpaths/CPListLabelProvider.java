/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

class CPListLabelProvider extends LabelProvider {
		
	private String fNewLabel, fClassLabel, fCreateLabel;
	private Image fJarIcon, fExtJarIcon, fJarWSrcIcon, fExtJarWSrcIcon;
	private Image fFolderImage, fProjectImage, fVariableImage, fContainerImage;
	private Image fMissingLibaryImage, fMissingVariableImage;
	private Image fMissingFolderImage, fMissingProjectImage, fMissingContainerImage;
	
	public CPListLabelProvider() {
		fNewLabel= NewWizardMessages.getString("CPListLabelProvider.new"); //$NON-NLS-1$
		fClassLabel= NewWizardMessages.getString("CPListLabelProvider.classcontainer"); //$NON-NLS-1$
		fCreateLabel= NewWizardMessages.getString("CPListLabelProvider.willbecreated"); //$NON-NLS-1$
		ImageRegistry reg= JavaPlugin.getDefault().getImageRegistry();
		
		fJarIcon= reg.get(JavaPluginImages.IMG_OBJS_JAR);
		fExtJarIcon= reg.get(JavaPluginImages.IMG_OBJS_EXTJAR);
		fJarWSrcIcon= reg.get(JavaPluginImages.IMG_OBJS_JAR_WSRC);
		fExtJarWSrcIcon= reg.get(JavaPluginImages.IMG_OBJS_EXTJAR_WSRC);
		fFolderImage= reg.get(JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT);
		
		fVariableImage= reg.get(JavaPluginImages.IMG_OBJS_ENV_VAR);
		
		
		
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		fProjectImage= workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT);
	
		fMissingLibaryImage= reg.get(JavaPluginImages.IMG_OBJS_MISSING_JAR);
		fMissingVariableImage= reg.get(JavaPluginImages.IMG_OBJS_MISSING_ENV_VAR);
		fMissingFolderImage= reg.get(JavaPluginImages.IMG_OBJS_MISSING_PACKFRAG_ROOT);
		fMissingProjectImage= workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT_CLOSED);
		
		fContainerImage= reg.get(JavaPluginImages.IMG_OBJS_LIBRARY);
		fMissingContainerImage= reg.get(JavaPluginImages.IMG_OBJS_LIBRARY);
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
			
	public Image getImage(Object element) {
		if (element instanceof CPListElement) {
			CPListElement cpentry= (CPListElement)element;
			switch (cpentry.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					if (!cpentry.isMissing()) {
						return fFolderImage;
					} else {
						return fMissingFolderImage;
					}
				case IClasspathEntry.CPE_LIBRARY:
					if (!cpentry.isMissing()) {
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
					} else {
						return fMissingLibaryImage;
					}
				case IClasspathEntry.CPE_PROJECT:
					if (!cpentry.isMissing()) {
						return fProjectImage;
					} else {
						return fMissingProjectImage;
					}				
				case IClasspathEntry.CPE_VARIABLE:
					if (!cpentry.isMissing()) {
						return fVariableImage;
					} else {
						return fMissingVariableImage;
					}
				case IClasspathEntry.CPE_CONTAINER:
					if (!cpentry.isMissing()) {
						return fContainerImage;
					} else {
						return fMissingContainerImage;
					}					
				default:
					// pass						
			}
		}
		return null;
	}


}	