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

import org.eclipse.core.runtime.IPath;import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;



class CPListLabelProvider extends LabelProvider {

	private static final String R_NEW= "CPListLabelProvider.new";
	private static final String R_ISCLASS= "CPListLabelProvider.classcontainer";
		
	private String fNewLabel, fClassLabel;
	private Image fJarIcon, fExtJarIcon, fJarWSrcIcon, fExtJarWSrcIcon;
	private Image fFolderImage, fProjectImage, fVariableImage;
	
	public CPListLabelProvider() {
		fNewLabel= JavaPlugin.getResourceString(R_NEW);
		fClassLabel= JavaPlugin.getResourceString(R_ISCLASS);
		ImageRegistry reg= JavaPlugin.getDefault().getImageRegistry();
		
		fJarIcon= reg.get(JavaPluginImages.IMG_OBJS_JAR);
		fExtJarIcon= reg.get(JavaPluginImages.IMG_OBJS_EXTJAR);
		fJarWSrcIcon= reg.get(JavaPluginImages.IMG_OBJS_JAR_WSRC);
		fExtJarWSrcIcon= reg.get(JavaPluginImages.IMG_OBJS_EXTJAR_WSRC);
		fFolderImage= reg.get(JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT);
		
		fVariableImage= reg.get(JavaPluginImages.IMG_OBJS_ENV_VAR);
		
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		fProjectImage= workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT);
	}
	
	public String getText(Object element) {
		if (element instanceof CPListElement) {
			CPListElement cpentry= (CPListElement)element;
			IResource resource= cpentry.getResource();
			switch (cpentry.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY: {
					IPath path= cpentry.getPath();
					if (cpentry.getResource() instanceof IFolder) {
						StringBuffer buf= new StringBuffer(path.toString());
						buf.append(' ');
						buf.append(fClassLabel);
						if (!resource.exists()) {
							buf.append(' ');
							buf.append(fNewLabel);
						}
						return buf.toString();
					} else {
						return path.lastSegment() + " - " + path.removeLastSegments(1).toString();
					}
				}
				case IClasspathEntry.CPE_VARIABLE: {
					String name= cpentry.getPath().lastSegment();
					StringBuffer buf= new StringBuffer(name);
					IClasspathEntry entry= JavaCore.getClasspathVariable(name);
					if (entry != null) {
						buf.append(" - ");
						buf.append(entry.getPath());
					}
					return buf.toString();
				}
				case IClasspathEntry.CPE_PROJECT:
					return cpentry.getPath().lastSegment();
				case IClasspathEntry.CPE_SOURCE: {
					StringBuffer buf= new StringBuffer(cpentry.getPath().toString());
					if (!resource.exists()) {
						buf.append(' ');
						buf.append(fNewLabel);
					}
					return buf.toString();
				}
			}
		}
		return super.getText(element);
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
				case IClasspathEntry.CPE_VARIABLE:
					return fVariableImage;					
			}
		}
		return null;
	}
}	