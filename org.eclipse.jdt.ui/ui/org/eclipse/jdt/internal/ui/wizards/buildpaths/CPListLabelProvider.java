/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;



class CPListLabelProvider extends LabelProvider {

	private static final String R_NEW= "CPListLabelProvider.new";
	private static final String R_ISCLASS= "CPListLabelProvider.classcontainer";
		
	private String fNewLabel, fClassLabel;
	private Image fJarIcon, fExtJarIcon, fJarWSrcIcon, fExtJarWSrcIcon;
	private Image fFolderImage, fProjectImage;
	
	public CPListLabelProvider() {
		fNewLabel= JavaPlugin.getResourceString(R_NEW);
		fClassLabel= JavaPlugin.getResourceString(R_ISCLASS);
		ImageRegistry reg= JavaPlugin.getDefault().getImageRegistry();
		
		fJarIcon= reg.get(JavaPluginImages.IMG_OBJS_JAR);
		fExtJarIcon= reg.get(JavaPluginImages.IMG_OBJS_EXTJAR);
		fJarWSrcIcon= reg.get(JavaPluginImages.IMG_OBJS_JAR_WSRC);
		fExtJarWSrcIcon= reg.get(JavaPluginImages.IMG_OBJS_EXTJAR_WSRC);
		fFolderImage= reg.get(JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT);
		
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		fProjectImage= workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT);
	}
	
	public String getText(Object element) {
		if (element instanceof CPListElement) {
			CPListElement cpentry= (CPListElement)element;
			String name= cpentry.getPath().toString();
			if (cpentry.getEntryKind() != IClasspathEntry.CPE_LIBRARY) {
				IResource resource= cpentry.getResource();
				if (resource == null || !resource.exists()) {				
					name = name + ' ' + fNewLabel;
				}
			} else { // CPE_LIBRARY
				if (cpentry.getResource() instanceof IFolder) {
					name = name + ' ' + fClassLabel;
				}
			}
			return name;
		}
		return "";
	}			
			
	public Image getImage(Object element) {
		if (element instanceof CPListElement) {
			CPListElement cpentry= (CPListElement)element;
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
			}
		}
		return null;
	}
}	