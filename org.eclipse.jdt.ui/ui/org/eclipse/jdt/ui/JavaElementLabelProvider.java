/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.util.JdtHackFinder;
import org.eclipse.jdt.internal.ui.viewsupport.JavaImageLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaTextLabelProvider;

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
	 * Flag (bit mask) indicating that the label should include the method return type.
	 */
	public final static int SHOW_RETURN_TYPE=				0x001;
	
	/**
	 * Flag (bit mask) indicating that the label should include method parameter types.
	 */
	public final static int SHOW_PARAMETERS=				0x002;
	
	/**
	 * Flag (bit mask) indicating that the label should include its container.
	 * For example, include the name of the type enclosing a field.
	 */
	public final static int SHOW_CONTAINER=				0x004;

	/**
	 * Flag (bit mask) indicating that the label should include its container
	 * with full qualification.
	 * For example, include the fully qualified name of the type enclosing a field.
	 */
	public final static int SHOW_CONTAINER_QUALIFICATION=	0x008;

	/**
	 * Flag (bit mask) indicating that the label should include overlay icons
	 * for element type and modifiers.
	 */
	public final static int SHOW_OVERLAY_ICONS=			0x010;

	/**
	 * Flag (bit mask) indicating that the label should include the field type.
	 */
	public final static int SHOW_TYPE=					0x020;

	/**
	 * Flag (bit mask) indicating that the label should include the name of the
	 * package fragment root.
	 */
	public final static int SHOW_ROOT=					0x040;
	
	/**
	 * Flag (bit mask) indicating that the label's qualification of an element should
	 * be shown after the name.
	 */
	public final static int SHOW_POSTIFIX_QUALIFICATION=		0x080;

	/**
	 * Flag (bit mask) indicating that the label should show the icons with no space
	 * reserved for overlays.
	 */
	public final static int SHOW_SMALL_ICONS= 			0x100;
	
	/**
	 * Constant (value <code>0</code>) indicating that the label should show 
	 * the basic images only.
	 */
	public final static int SHOW_BASICS= 0x000;
	
	/**
	 * Constant indicating the default label rendering.
	 * Currently the default is qquivalent to
	 * <code>SHOW_PARAMETERS | SHOW_OVERLAY_ICONS</code>.
	 */
	public final static int SHOW_DEFAULT= new Integer(SHOW_PARAMETERS | SHOW_OVERLAY_ICONS).intValue();

	private JavaTextLabelProvider fTextLabelProvider;
	private JavaImageLabelProvider fImageLabelProvider;
	private WorkbenchLabelProvider fWorkbenchLabelProvider;
	
	// map images for JarEntryFiles, key = extension - value = image
	// the cached images will be disposed wen the label provider is disposed.
	private Map fJarImageMap= new HashMap(10);
	/**
	 * Creates a new label provider.
	 *
	 * @param flags the initial options; a bitwise OR of <code>SHOW_* </code> constants
	 */
	public JavaElementLabelProvider(int flags) {
		fTextLabelProvider= new JavaTextLabelProvider(flags);
		fImageLabelProvider= new JavaImageLabelProvider(flags);
		fWorkbenchLabelProvider= new WorkbenchLabelProvider();
	}

	/**
	 * Turns on the rendering options specified in the given flags.
	 *
	 * @param flags the options; a bitwise OR of <code>SHOW_* </code> constants
	 */
	public void turnOn(int flags) {
		fTextLabelProvider.turnOn(flags);
		fImageLabelProvider.turnOn(flags);
	}
	
	/**
	 * Turns off the rendering options specified in the given flags.
	 *
	 * @param flags the initial options; a bitwise OR of <code>SHOW_* </code> constants
	 */
	public void turnOff(int flags) {
		fTextLabelProvider.turnOff(flags);
		fImageLabelProvider.turnOff(flags);
	}

	/* (non-Javadoc)
	 * @see ILabelProvider#getImage
	 */
	public Image getImage(Object element) {

		if (element instanceof IJavaElement) {
			IJavaElement e= (IJavaElement) element;
			return fImageLabelProvider.getLabelImage(e);
		}

		Image result= fWorkbenchLabelProvider.getImage(element);
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
			IJavaElement e= (IJavaElement) element;
			return fTextLabelProvider.getTextLabel(e);
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
