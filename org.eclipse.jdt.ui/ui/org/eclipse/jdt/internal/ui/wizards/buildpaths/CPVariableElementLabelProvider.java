package org.eclipse.jdt.internal.ui.wizards.buildpaths;import org.eclipse.swt.graphics.Image;import org.eclipse.jface.resource.ImageRegistry;import org.eclipse.jface.viewers.LabelProvider;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class CPVariableElementLabelProvider extends LabelProvider {
	
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
			StringBuffer buf= new StringBuffer();
			buf.append(curr.getName());
			buf.append(" - ");
			buf.append(curr.getPath().toString());
			return buf.toString();
		}		
		
		
		return super.getText(element);
	}

}
