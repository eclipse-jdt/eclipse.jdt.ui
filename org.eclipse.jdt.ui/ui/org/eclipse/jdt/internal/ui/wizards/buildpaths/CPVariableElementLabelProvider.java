/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.wizards.buildpaths;import org.eclipse.swt.graphics.Image;import org.eclipse.core.runtime.IPath;import org.eclipse.jface.resource.ImageRegistry;import org.eclipse.jface.viewers.LabelProvider;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class CPVariableElementLabelProvider extends LabelProvider {
		private static final String RESERVED= "CPVariableElementLabelProvider.reserved";	
	private Image fVariableImage;
	
	public CPVariableElementLabelProvider() {
		ImageRegistry reg= JavaPlugin.getDefault().getImageRegistry();
		fVariableImage= reg.get(JavaPluginImages.IMG_OBJS_ENV_VAR);
	}
	
	/**
	 * @see LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof CPVariableElement) {
			return fVariableImage;
		}
		return super.getImage(element);
	}

	/**
	 * @see LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof CPVariableElement) {
			CPVariableElement curr= (CPVariableElement)element;
			String name= curr.getName();
			IPath path= curr.getPath();
			StringBuffer buf= new StringBuffer(name);			if (!curr.isReserved()) {
				if (path != null) {
					buf.append(" - ");
					buf.append(path.toString());
				}			} else {				buf.append(' ');				buf.append(JavaPlugin.getResourceString(RESERVED));			}			return buf.toString();
		}		
		
		
		return super.getText(element);
	}

}
