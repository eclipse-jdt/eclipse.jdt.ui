/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;


import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;


/**
 * Default strategy of the Java plugin for the construction of Java element icons.
 */
public class JavaImageLabelProvider {
	
	static final Point SMALL_SIZE= new Point(16, 16);
	static final Point BIG_SIZE= new Point(22, 16);
	
	protected int fFlags;
	protected OverlayIconManager fIconManager; 
		
	public JavaImageLabelProvider(int flags) {
		fFlags= flags;
		JavaElementDescriptorFactory factory= new JavaElementDescriptorFactory();
		
		if ((flags & JavaElementLabelProvider.SHOW_SMALL_ICONS) != 0)
			fIconManager= new OverlayIconManager(factory, SMALL_SIZE);
		else 
			fIconManager= new OverlayIconManager(factory, BIG_SIZE);
	}
	
	public void setErrorTickManager(IErrorTickManager manager) {
		((JavaElementDescriptorFactory)fIconManager.getDescriptorFactory()).setErrorTickManager(manager);
	}
	
	public void turnOn(int flags) {
		fFlags |= flags;
	}
	
	public void turnOff(int flags) {
		fFlags &= (~flags);
	}
	
	
	protected boolean showOverlayIcons() {
		return (fFlags & JavaElementLabelProvider.SHOW_OVERLAY_ICONS) != 0;
	}
	
	/**
	 * Maps a Java element to an appropriate icon name.
	 */
	protected String getImageName(IJavaElement element) {
		try {
			
			switch (element.getElementType()) {
				
				case IJavaElement.INITIALIZER:
				case IJavaElement.METHOD:
				case IJavaElement.FIELD: {
					IMember member= (IMember) element;
					if (member.getDeclaringType().isInterface())
						return JavaPluginImages.IMG_MISC_PUBLIC;
					
					int flags= member.getFlags();
					if (Flags.isPublic(flags))
						return JavaPluginImages.IMG_MISC_PUBLIC;
					if (Flags.isProtected(flags))
						return JavaPluginImages.IMG_MISC_PROTECTED;
					if (Flags.isPrivate(flags))
						return JavaPluginImages.IMG_MISC_PRIVATE;
					
					return JavaPluginImages.IMG_MISC_DEFAULT;
				}
				case IJavaElement.PACKAGE_DECLARATION:
					return JavaPluginImages.IMG_OBJS_PACKDECL;
				
				case IJavaElement.IMPORT_DECLARATION:
					return JavaPluginImages.IMG_OBJS_IMPDECL;
					
				case IJavaElement.IMPORT_CONTAINER:
					return JavaPluginImages.IMG_OBJS_IMPCONT;
				
				case IJavaElement.TYPE: {
					IType type= (IType) element;
					int flags= type.getFlags();
					boolean hasVisibility= Flags.isPublic(flags) || Flags.isPrivate(flags) || Flags.isProtected(flags);
					
					if (type.isClass())
						return hasVisibility ? JavaPluginImages.IMG_OBJS_CLASS : JavaPluginImages.IMG_OBJS_PCLASS;
					return hasVisibility ? JavaPluginImages.IMG_OBJS_INTERFACE : JavaPluginImages.IMG_OBJS_PINTERFACE;
				}

				case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
					IPackageFragmentRoot root= (IPackageFragmentRoot) element;
					if (root.isArchive()) {
						IPath attach= root.getSourceAttachmentPath();
						if (root.isExternal()) {
							if (attach == null) {
								return JavaPluginImages.IMG_OBJS_EXTJAR;
							} else {
								return JavaPluginImages.IMG_OBJS_EXTJAR_WSRC;
							}
						} else {
							if (attach == null) {
								return JavaPluginImages.IMG_OBJS_JAR;
							} else {
								return JavaPluginImages.IMG_OBJS_JAR_WSRC;
							}
						}							
					} else {
						return JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT;
					}
				}
				case IJavaElement.PACKAGE_FRAGMENT:
					IPackageFragment fragment= (IPackageFragment)element;
					try {
						// show the folder icon for packages with only non Java resources
						// fix for: 1G5WN0V 
						if (!fragment.hasChildren() && (fragment.getNonJavaResources().length >0)) 
							return ISharedImages.IMG_OBJ_FOLDER;
					} catch(JavaModelException e) {
						return ISharedImages.IMG_OBJ_FOLDER;
					}
					return JavaPluginImages.IMG_OBJS_PACKAGE;
					
				case IJavaElement.COMPILATION_UNIT:
					return JavaPluginImages.IMG_OBJS_CUNIT;
					
				case IJavaElement.CLASS_FILE:
					/* this is too expensive for large packages
					try {
						IClassFile cfile= (IClassFile)element;
						if (cfile.isClass())
							return JavaPluginImages.IMG_OBJS_CFILECLASS;
						return JavaPluginImages.IMG_OBJS_CFILEINT;
					} catch(JavaModelException e) {
						// fall through;
					}*/
					return JavaPluginImages.IMG_OBJS_CFILE;
					
				case IJavaElement.JAVA_PROJECT: 
					IJavaProject jp= (IJavaProject)element;
					if (jp.getProject().isOpen()) {
						// fix: 1GF6CDH: ITPJUI:ALL - Packages view doesn't show the nature decoration
						if (showOverlayIcons()) {
							String imageId= getManagedId(jp);
							if (imageId != null)
								return imageId;
						}
						// end fix.
						return ISharedImages.IMG_OBJ_PROJECT;
					}
					return ISharedImages.IMG_OBJ_PROJECT_CLOSED;
			}
			
			Assert.isTrue(false, JavaUIMessages.getString("JavaImageLabelprovider.assert.wrongImage")); //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		
		} catch (CoreException x) {
			return JavaPluginImages.IMG_OBJS_GHOST;
		}
	}
	// 1GF6CDH: ITPJUI:ALL - Packages view doesn't show the nature decoration
	public String createBaseImageId(IJavaProject javaProject) throws CoreException {
		IProject project=javaProject.getProject();
		String[] natures= project.getDescription().getNatureIds();
		if (natures.length > 0)
			return ISharedImages.IMG_OBJ_PROJECT+natures[0];
		return ISharedImages.IMG_OBJ_PROJECT;
	}
	
	public String getManagedId(IJavaProject javaProject) throws CoreException {
		String id= createBaseImageId(javaProject);
		if (fIconManager.isManaged(id))
			return id;
		IProject project=javaProject.getProject();
		IWorkbenchAdapter adapter= (IWorkbenchAdapter)project.getAdapter(IWorkbenchAdapter.class);
		if (adapter != null) {
			ImageDescriptor desc= adapter.getImageDescriptor(project);
			if (desc != null) {
				fIconManager.manage(id, desc);
				return id;
			}
		}
		return null;
	}
	// end fix.

	/**
	 * Returns the icon for a given Java elements. The icon depends on the element type
	 * and element properties. If configured, overlay icons are constructed for
	 * <code>ISourceReference</code>s. Overlay icon construction is  done by the 
	 * <code>OverlayIconManager</code>.
	 */
	public Image getLabelImage(IJavaElement element) {
		String icon= getImageName(element);
				
		if (showOverlayIcons()) {
			if (icon != null) {
				return fIconManager.getIcon(icon, element);
			}
		} 
		Image img= JavaPluginImages.get(icon);
		if (img == null) {
			img= JavaPlugin.getDefault().getWorkbench().getSharedImages().getImage(icon);
		}
		return img;
	}
	
}