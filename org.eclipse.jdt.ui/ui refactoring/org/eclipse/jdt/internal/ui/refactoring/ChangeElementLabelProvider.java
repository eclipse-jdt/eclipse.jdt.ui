/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextFileChange;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

public class ChangeElementLabelProvider extends LabelProvider {

	private JavaElementLabelProvider fJavaElementLabelProvider;
	private Map fChangeImageDescriptorMap;
	private Map fDescriptorImageMap= new HashMap();

	public ChangeElementLabelProvider() {
		fJavaElementLabelProvider= new JavaElementLabelProvider(
			JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_SMALL_ICONS);
	}
		
	public ChangeElementLabelProvider(Map changeImageDescriptorMap) {
		this();
		fChangeImageDescriptorMap= changeImageDescriptorMap;
	}
		
	public Image getImage(Object object) {
		if (object instanceof DefaultChangeElement) {
			Object element= ((DefaultChangeElement)object).getChange();
			return doGetImage(element);
		} else if (object instanceof TextEditChangeElement) {
			Object element= ((TextEditChangeElement)object).getTextEditChange();
			return doGetImage(element);
		} else if (object instanceof PseudoJavaChangeElement) {
			PseudoJavaChangeElement element= (PseudoJavaChangeElement)object;
			return fJavaElementLabelProvider.getImage(element.getJavaElement());
		}
		return super.getImage(object);
	}
	
	public String getText(Object object) {
		if (object instanceof DefaultChangeElement) {
			return ((DefaultChangeElement)object).getChange().getName();
		} else if (object instanceof TextEditChangeElement) {
			return ((TextEditChangeElement)object).getTextEditChange().getName();
		} else if (object instanceof PseudoJavaChangeElement) {
			PseudoJavaChangeElement element= (PseudoJavaChangeElement)object;
			return fJavaElementLabelProvider.getText(element.getJavaElement());
		}
		return super.getText(object);
	}
	
	private Image doGetImage(Object element) {
		ImageDescriptor descriptor= null;
		if (fChangeImageDescriptorMap != null) {
			descriptor= (ImageDescriptor)fChangeImageDescriptorMap.get(element.getClass());
		}
		if (descriptor == null) {
			if (element instanceof TextEditChangeElement) {
				descriptor= JavaPluginImages.DESC_OBJS_TEXT_EDIT;
			} else if (element instanceof ICompositeChange) {
				descriptor= JavaPluginImages.DESC_OBJS_COMPOSITE_CHANGE;	
			} else if (element instanceof CompilationUnitChange) {
				descriptor= JavaPluginImages.DESC_OBJS_CU_CHANGE;
			} else if (element instanceof TextFileChange) {
				descriptor= JavaPluginImages.DESC_OBJS_FILE_CHANGE;
			} else {
				descriptor= JavaPluginImages.DESC_OBJS_DEFAULT_CHANGE;
			}
		}
		Image image= (Image)fDescriptorImageMap.get(descriptor);
		if (image == null) {
			image= descriptor.createImage();
			fDescriptorImageMap.put(descriptor, image);
		}
		return image;
	}
}

