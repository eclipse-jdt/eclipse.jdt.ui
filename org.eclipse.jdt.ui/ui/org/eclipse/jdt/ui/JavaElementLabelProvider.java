/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.viewsupport.IErrorTickProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Standard label provider for Java elements.
 * Use this class when you want to present the Java elements in a viewer.
 * <p>
 * The implementation also handles non-Java elements by forwarding the requests to an 
 * internal <code>WorkbenchLabelProvider</code>.
 * </p>
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @see org.eclipse.ui.model.WorkbenchLabelProvider
 */
public class JavaElementLabelProvider extends LabelProvider {
	
	/**
	 * Flag (bit mask) indicating that methods labels include the method return type. (prepended)
	 */
	public final static int SHOW_RETURN_TYPE=				0x001;
	
	/**
	 * Flag (bit mask) indicating that method label include method parameter types.
	 */
	public final static int SHOW_PARAMETERS=				0x002;
	
	/**
	 * Flag (bit mask) indicating that the label of a member should include the container.
	 * For example, include the name of the type enclosing a field.
	 * @deprecated Use SHOW_QUALIFIED or SHOW_ROOT instead
	 */
	public final static int SHOW_CONTAINER=				0x004;

	/**
	 * Flag (bit mask) indicating that the label of a type should be fully qualified.
	 * For example, include the fully qualified name of the type enclosing a type.
	 * @deprecated Use SHOW_QUALIFIED instead
	 */
	public final static int SHOW_CONTAINER_QUALIFICATION=	0x008;

	/**
	 * Flag (bit mask) indicating that the label should include overlay icons
	 * for element type and modifiers.
	 */
	public final static int SHOW_OVERLAY_ICONS=			0x010;

	/**
	 * Flag (bit mask) indicating thata field label should include the declared type.
	 */
	public final static int SHOW_TYPE=					0x020;

	/**
	 * Flag (bit mask) indicating that the label should include the name of the
	 * package fragment root (appended).
	 */
	public final static int SHOW_ROOT=					0x040;
	
	/**
	 * Flag (bit mask) indicating that the label qualification of a type should
	 * be shown after the name.
	 * @deprecated SHOW_POST_QUALIFIED instead
	 */
	public final static int SHOW_POSTIFIX_QUALIFICATION=		0x080;

	/**
	 * Flag (bit mask) indicating that the label should show the icons with no space
	 * reserved for overlays.
	 */
	public final static int SHOW_SMALL_ICONS= 			0x100;
	
	/**
	 * Flag (bit mask) indicating that the packagefragment roots from variables should
	 * be rendered with the variable in the name
	 */
	public final static int SHOW_VARIABLE= 			0x200;
	
	/**
	 * Flag (bit mask) indicating that Complation Units, Class Files, Types, Declarations and Members
	 * should be rendered qualified.
	 * Examples: java.lang.String, java.util.Vector.size()
	 */
	public final static int SHOW_QUALIFIED=				0x400;

	/**
	 * Flag (bit mask) indicating that Complation Units, Class Files, Types, Declarations and Members
	 * should be rendered qualified. The qualifcation is appended
	 * Examples: String - java.lang, size() - java.util.Vector
	 */
	public final static int SHOW_POST_QUALIFIED=	0x800;	
	
	
	/**
	 * Constant (value <code>0</code>) indicating that the label should show 
	 * the basic images only.
	 */
	public final static int SHOW_BASICS= 0x000;
	
	
	/**
	 * Constant indicating the default label rendering.
	 * Currently the default is equivalent to
	 * <code>SHOW_PARAMETERS | SHOW_OVERLAY_ICONS</code>.
	 */
	public final static int SHOW_DEFAULT= new Integer(SHOW_PARAMETERS | SHOW_OVERLAY_ICONS).intValue();

	private JavaElementImageProvider fImageLabelProvider;
	private WorkbenchLabelProvider fWorkbenchLabelProvider;

	private int fFlags;
	private int fImageFlags;
	private int fTextFlags;
	
	// map images for JarEntryFiles, key = extension - value = image
	// the cached images will be disposed wen the label provider is disposed.
	private Map fJarImageMap= new HashMap(10);

	/**
	 * Creates a new label provider with <code>SHOW_DEFAULT</code> flag.
	 *
	 * @see #SHOW_DEFAULT
	 * @since 2.0
	 */
	public JavaElementLabelProvider() {
		this(SHOW_DEFAULT);
	}

	/**
	 * Creates a new label provider.
	 *
	 * @param flags the initial options; a bitwise OR of <code>SHOW_* </code> constants
	 */
	public JavaElementLabelProvider(int flags) {
		fImageLabelProvider= new JavaElementImageProvider();
		fWorkbenchLabelProvider= new WorkbenchLabelProvider();
		fFlags= flags;
		updateImageProviderFlags();
		updateTextProviderFlags();		
	}
	
	private boolean getFlag( int flag) {
		return (fFlags & flag) != 0;
	}
	
	/**
	 * Turns on the rendering options specified in the given flags.
	 *
	 * @param flags the options; a bitwise OR of <code>SHOW_* </code> constants
	 */
	public void turnOn(int flags) {
		fFlags |= flags;
		updateImageProviderFlags();
		updateTextProviderFlags();
	}
	
	/**
	 * Turns off the rendering options specified in the given flags.
	 *
	 * @param flags the initial options; a bitwise OR of <code>SHOW_* </code> constants
	 */
	public void turnOff(int flags) {
		fFlags &= (~flags);
		updateImageProviderFlags();
		updateTextProviderFlags();
	}
	
	private void updateImageProviderFlags() {
		fImageFlags= 0;
		if (getFlag(SHOW_OVERLAY_ICONS)) {
			fImageFlags |= JavaElementImageProvider.OVERLAY_ICONS;
		}
		if (getFlag(SHOW_SMALL_ICONS)) {
			fImageFlags |= JavaElementImageProvider.SMALL_ICONS;
		}
	}	
	
	private void updateTextProviderFlags() {
		fTextFlags= 0;
		if (getFlag(SHOW_RETURN_TYPE)) {
			fTextFlags |= JavaElementLabels.M_PRE_RETURNTYPE;
		}
		if (getFlag(SHOW_PARAMETERS)) {
			fTextFlags |= JavaElementLabels.M_PARAMETER_TYPES;
		}		
		if (getFlag(SHOW_CONTAINER)) {
			fTextFlags |= JavaElementLabels.P_POST_QUALIFIED | JavaElementLabels.T_POST_QUALIFIED | JavaElementLabels.CF_POST_QUALIFIED  | JavaElementLabels.CU_POST_QUALIFIED | JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.F_POST_QUALIFIED;
		}
		if (getFlag(SHOW_POSTIFIX_QUALIFICATION)) {
			fTextFlags |= (JavaElementLabels.T_POST_QUALIFIED | JavaElementLabels.CF_POST_QUALIFIED  | JavaElementLabels.CU_POST_QUALIFIED);
		} else if (getFlag(SHOW_CONTAINER_QUALIFICATION)) {
			fTextFlags |=(JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED);
		}
		if (getFlag(SHOW_TYPE)) {
			fTextFlags |= JavaElementLabels.F_APP_TYPE_SIGNATURE;
		}
		if (getFlag(SHOW_ROOT)) {
			fTextFlags |= JavaElementLabels.APPEND_ROOT_PATH;
		}			
		if (getFlag(SHOW_VARIABLE)) {
			fTextFlags |= JavaElementLabels.ROOT_VARIABLE;
		}
		if (getFlag(SHOW_QUALIFIED)) {
			fTextFlags |= (JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED 
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED);
		}
		if (getFlag(SHOW_POST_QUALIFIED)) {
			fTextFlags |= (JavaElementLabels.F_POST_QUALIFIED | JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.I_POST_QUALIFIED 
			| JavaElementLabels.T_POST_QUALIFIED | JavaElementLabels.D_POST_QUALIFIED | JavaElementLabels.CF_POST_QUALIFIED  | JavaElementLabels.CU_POST_QUALIFIED);
		}		
	}

	/* (non-Javadoc)
	 * @see ILabelProvider#getImage
	 */
	public Image getImage(Object element) {
		Image result= fImageLabelProvider.getImageLabel(element, fImageFlags);
		if (result != null) {
			return result;
		}

		if (element instanceof IStorage) 
			return getImageForJarEntry((IStorage)element);

		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see ILabelProvider#getText
	 */
	public String getText(Object element) {

		if (element instanceof IJavaElement) {
			return JavaElementLabels.getElementLabel((IJavaElement) element, fTextFlags);
		}
	
		String text= fWorkbenchLabelProvider.getText(element);
		if (text.length() > 0) {
			return text;
		}

		if (element instanceof IStorage) {
			return ((IStorage)element).getName();
		}

		return super.getText(element);
	}

	/* (non-Javadoc)
	 * 
	 * @see IBaseLabelProvider#dispose
	 */
	public void dispose() {
		fWorkbenchLabelProvider.dispose();
		disposeJarEntryImages();
	}
	
	public void setErrorTickManager(IErrorTickProvider provider) {
		fImageLabelProvider.setErrorTickProvider(provider);
	}	
	
	/*
	 * Dispose the cached images for JarEntry files 
	 */
	private void disposeJarEntryImages() {
		Iterator each= fJarImageMap.values().iterator();
		while (each.hasNext()) {
			Image image= (Image)each.next();
			image.dispose();
		}
		fJarImageMap.clear();
	}
	
	/*
	 * Gets and caches an image for a JarEntryFile.
	 * The image for a JarEntryFile is retrieved from the EditorRegistry.
	 */ 
	private Image getImageForJarEntry(IStorage element) {
		String extension= element.getFullPath().getFileExtension();
		Image image= (Image)fJarImageMap.get(extension);
		if (image != null) 
			return image;
		IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
		ImageDescriptor desc= registry.getImageDescriptor(element.getName());
		image= desc.createImage();
		fJarImageMap.put(extension, image);
		return image;
	}
}
