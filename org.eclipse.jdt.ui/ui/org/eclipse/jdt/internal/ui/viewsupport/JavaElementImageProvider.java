/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

/**
 * Default strategy of the Java plugin for the construction of Java element icons.
 */
public class JavaElementImageProvider {
	
	/**
	 * Flags for the JavaImageLabelProvider:
	 * Generate images with overlays.
	 */
	public final static int OVERLAY_ICONS= 0x1;

	/**
	 * Generate small sized images.
	 */
	public final static int SMALL_ICONS= 0x2;

	/**
	 * Use the 'light' style for rendering types.
	 */	
	public final static int LIGHT_TYPE_ICONS= 0x4;
	
	/**
	 * Show error overrlay. 
	 */	
	public final static int OVERLAY_ERROR= 0x8;

	/**
	 * Show warning overrlay
	 */	
	public final static int OVERLAY_WARNING= 0x10;
	
	/**
	 * Show override overrlay. 
	 */	
	public final static int OVERLAY_OVERRIDE= 0x20;

	/**
	 * Show implements overrlay. 
	 */	
	public final static int OVERLAY_IMPLEMENTS= 0x40;
	
	private static final Point SMALL_SIZE= new Point(16, 16);
	private static final Point BIG_SIZE= new Point(22, 16);

	private static ImageDescriptor DESC_OBJ_PROJECT_CLOSED;	
	private static ImageDescriptor DESC_OBJ_PROJECT;	
	private static ImageDescriptor DESC_OBJ_FOLDER;
	{
		ISharedImages images= JavaPlugin.getDefault().getWorkbench().getSharedImages(); 
		DESC_OBJ_PROJECT_CLOSED= images.getImageDescriptor(ISharedImages.IMG_OBJ_PROJECT_CLOSED);
		DESC_OBJ_PROJECT= 		 images.getImageDescriptor(ISharedImages.IMG_OBJ_PROJECT);
		DESC_OBJ_FOLDER= 		 images.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}
	
	private ImageDescriptorRegistry fRegistry;
		
	public JavaElementImageProvider() {
		fRegistry= JavaPlugin.getImageDescriptorRegistry();
	}	
		
	/**
	 * Returns the icon for a given element. The icon depends on the element type
	 * and element properties. If configured, overlay icons are constructed for
	 * <code>ISourceReference</code>s.
	 * @param flags Flags as defined by the JavaImageLabelProvider
	 */
	public Image getImageLabel(Object element, int flags) {
		ImageDescriptor descriptor= null;
		if (element instanceof IJavaElement) {
			descriptor= getJavaImageDescriptor((IJavaElement) element, flags);
		} else if (element instanceof IAdaptable) {
			descriptor= getWorkbenchImageDescriptor((IAdaptable) element, flags);
		}
		if (descriptor != null) {
			return fRegistry.get(descriptor);
		}
		return null;
	}
	
	private boolean showOverlayIcons(int flags) {
		return (flags & OVERLAY_ICONS) != 0;
	}
	
	private boolean useLightIcons(int flags) {
		return (flags & LIGHT_TYPE_ICONS) != 0;
	}
	
	private boolean useSmallSize(int flags) {
		return (flags & SMALL_ICONS) != 0;
	}
	
	/**
	 * Returns an image descriptor for a java element. The descriptor includes overlays, if specified.
	 */
	public ImageDescriptor getJavaImageDescriptor(IJavaElement element, int flags) {
		int adornmentFlags= computeJavaAdornmentFlags(element, flags);
		Point size= useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
		return new JavaElementImageDescriptor(getBaseImageDescriptor(element, flags), adornmentFlags, size);
	}

	/**
	 * Returns an image descriptor for a IAdaptable. The descriptor includes overlays, if specified (only error ticks apply).
	 * Returns <code>null</code> if no image could be found.
	 */	
	public ImageDescriptor getWorkbenchImageDescriptor(IAdaptable adaptable, int flags) {
		IWorkbenchAdapter wbAdapter= (IWorkbenchAdapter) adaptable.getAdapter(IWorkbenchAdapter.class);
		if (wbAdapter == null) {
			return null;
		}
		ImageDescriptor descriptor= wbAdapter.getImageDescriptor(adaptable);
		if (descriptor == null) {
			return null;
		}
		int adornmentFlags= computeBasicAdornmentFlags(adaptable, flags);
		Point size= useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
		return new JavaElementImageDescriptor(descriptor, adornmentFlags, size);
	}
	
	// ---- Computation of base image key -------------------------------------------------
	
	/**
	 * Returns an image descriptor for a java element. This is the base image, no overlays.
	 */
	public ImageDescriptor getBaseImageDescriptor(IJavaElement element, int renderFlags) {
		try {			
			switch (element.getElementType()) {
				
				case IJavaElement.INITIALIZER:
				case IJavaElement.METHOD:
				case IJavaElement.FIELD: {
					IMember member= (IMember) element;
					if (member.getDeclaringType().isInterface())
						return JavaPluginImages.DESC_MISC_PUBLIC;
					
					int flags= member.getFlags();
					if (Flags.isPublic(flags))
						return JavaPluginImages.DESC_MISC_PUBLIC;
					if (Flags.isProtected(flags))
						return JavaPluginImages.DESC_MISC_PROTECTED;
					if (Flags.isPrivate(flags))
						return JavaPluginImages.DESC_MISC_PRIVATE;
					
					return JavaPluginImages.DESC_MISC_DEFAULT;
				}
				case IJavaElement.PACKAGE_DECLARATION:
					return JavaPluginImages.DESC_OBJS_PACKDECL;
				
				case IJavaElement.IMPORT_DECLARATION:
					return JavaPluginImages.DESC_OBJS_IMPDECL;
					
				case IJavaElement.IMPORT_CONTAINER:
					return JavaPluginImages.DESC_OBJS_IMPCONT;
				
				case IJavaElement.TYPE: {
					IType type= (IType) element;
					
					if (useLightIcons(renderFlags)) {
						if (type.isClass())
							return JavaPluginImages.DESC_OBJS_CLASSALT;
						else 
							return JavaPluginImages.DESC_OBJS_INTERFACEALT;
					}
					
					int flags= type.getFlags();
					boolean hasVisibility= Flags.isPublic(flags) || Flags.isPrivate(flags) || Flags.isProtected(flags);
					
					if (type.isClass())
						return hasVisibility ? JavaPluginImages.DESC_OBJS_CLASS : JavaPluginImages.DESC_OBJS_PCLASS;
					return hasVisibility ? JavaPluginImages.DESC_OBJS_INTERFACE : JavaPluginImages.DESC_OBJS_PINTERFACE;
				}

				case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
					IPackageFragmentRoot root= (IPackageFragmentRoot) element;
					if (root.isArchive()) {
						IPath attach= root.getSourceAttachmentPath();
						if (root.isExternal()) {
							if (attach == null) {
								return JavaPluginImages.DESC_OBJS_EXTJAR;
							} else {
								return JavaPluginImages.DESC_OBJS_EXTJAR_WSRC;
							}
						} else {
							if (attach == null) {
								return JavaPluginImages.DESC_OBJS_JAR;
							} else {
								return JavaPluginImages.DESC_OBJS_JAR_WSRC;
							}
						}							
					} else {
						return JavaPluginImages.DESC_OBJS_PACKFRAG_ROOT;
					}
				}
				case IJavaElement.PACKAGE_FRAGMENT:
					IPackageFragment fragment= (IPackageFragment)element;
					try {
						// show the folder icon for packages with only non Java resources
						// fix for: 1G5WN0V 
						if (!fragment.hasChildren() && (fragment.getNonJavaResources().length >0)) 
							return DESC_OBJ_FOLDER;
					} catch(JavaModelException e) {
						return DESC_OBJ_FOLDER;
					}
					return JavaPluginImages.DESC_OBJS_PACKAGE;
					
				case IJavaElement.COMPILATION_UNIT:
					return JavaPluginImages.DESC_OBJS_CUNIT;
					
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
					return JavaPluginImages.DESC_OBJS_CFILE;
					
				case IJavaElement.JAVA_PROJECT: 
					IJavaProject jp= (IJavaProject)element;
					if (jp.getProject().isOpen()) {
						IProject project= jp.getProject();
						IWorkbenchAdapter adapter= (IWorkbenchAdapter)project.getAdapter(IWorkbenchAdapter.class);
						if (adapter != null) {
							ImageDescriptor result= adapter.getImageDescriptor(project);
							if (result != null)
								return result;
						}
						return DESC_OBJ_PROJECT;
					}
					return DESC_OBJ_PROJECT_CLOSED;
					
				case IJavaElement.JAVA_MODEL:
					return JavaPluginImages.DESC_OBJS_JAVA_MODEL;
			}
			
			Assert.isTrue(false, JavaUIMessages.getString("JavaImageLabelprovider.assert.wrongImage")); //$NON-NLS-1$
			return null; //$NON-NLS-1$
		
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return JavaPluginImages.DESC_OBJS_GHOST;
		}
	}

	// ---- Methods to compute the adornments flags ---------------------------------
	
	private int computeJavaAdornmentFlags(IJavaElement element, int renderFlags) {
		
		int flags= computeBasicAdornmentFlags(element, renderFlags);
		
		if (showOverlayIcons(flags) && element instanceof ISourceReference) { 
			ISourceReference sourceReference= (ISourceReference)element;
			int modifiers= getModifiers(sourceReference);
		
			if (Flags.isAbstract(modifiers) && confirmAbstract((IMember) sourceReference))
				flags |= JavaElementImageDescriptor.ABSTRACT;
			if (Flags.isFinal(modifiers))
				flags |= JavaElementImageDescriptor.FINAL;
			if (Flags.isSynchronized(modifiers) && confirmSynchronized((IMember) sourceReference))
				flags |= JavaElementImageDescriptor.SYNCHRONIZED;
			if (Flags.isStatic(modifiers))
				flags |= JavaElementImageDescriptor.STATIC;
				
			if (sourceReference instanceof IType) {
				try {
					if (JavaModelUtil.hasMainMethod((IType)sourceReference))
						flags |= JavaElementImageDescriptor.RUNNABLE;
				} catch (JavaModelException e) {
					// do nothing. Can't compute runnable adornment.
				}
			}
		}
		return flags;
	}
	
	private int computeBasicAdornmentFlags(Object element, int renderFlags) {
		int flags= 0;
		if ((renderFlags & OVERLAY_ERROR) !=0) {
			flags |= JavaElementImageDescriptor.ERROR;
		}
		if ((renderFlags & OVERLAY_WARNING) !=0) {
			flags |= JavaElementImageDescriptor.WARNING;
		}		
		if ((renderFlags & OVERLAY_OVERRIDE) !=0) {
			flags |= JavaElementImageDescriptor.OVERRIDES;
		}
		if ((renderFlags & OVERLAY_IMPLEMENTS) !=0) {
			flags |= JavaElementImageDescriptor.IMPLEMENTS;
		}
		return flags;			
	}	
	
	private boolean confirmAbstract(IMember member) {
		 // Although all methods of a Java interface are abstract, the abstract 
		 // icon should not be shown.
		IType t= member.getDeclaringType();
		if (t == null && member instanceof IType)
			t= (IType) member;
		if (t != null) {
			try {
				return !t.isInterface();
			} catch (JavaModelException x) {
				// do nothing. Can't compute abstract state.
			}
		}
		return true;
	}
	
	private boolean confirmSynchronized(IMember member) {
		// Synchronized types are allowed but meaningless.
		return !(member instanceof IType);
	}
	
	private int getModifiers(ISourceReference sourceReference) {
		if (sourceReference instanceof IMember) {
			try {
				return ((IMember) sourceReference).getFlags();
			} catch (JavaModelException x) {
				// do nothing. Can't compute modifier state.
			}
		}
		return 0;
	}
	
	public void dispose() {
	}
	
}