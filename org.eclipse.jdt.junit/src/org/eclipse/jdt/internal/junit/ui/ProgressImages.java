package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Manages a set of images that can show progress in the image itself.
 */
public class ProgressImages {
	static final int HEIGHT= 16;
	static Image[] fgOKImages= new Image[HEIGHT+1];
	static Image[] fgFailureImages= new Image[HEIGHT+1];
	
	static void load() {
		if (fgOKImages[0] != null)
			return;
			
		ImageDescriptor basedesc= ImageDescriptor.createFromFile(ProgressImages.class, "icons/smalllogo.gif"); //$NON-NLS-1$
		fgOKImages[0]= basedesc.createImage();
		fgFailureImages[0]= fgOKImages[0];
		
		for (int i= 1; i <= HEIGHT; i++) {
			String okname= "icons/smalllogo-o"+Integer.toString(i)+".gif"; //$NON-NLS-1$ //$NON-NLS-2$
			ImageDescriptor descriptor= ImageDescriptor.createFromFile(ProgressImages.class, okname);
			fgOKImages[i]= descriptor.createImage();
			String failurename= "icons/smalllogo-e"+Integer.toString(i)+".gif"; //$NON-NLS-1$ //$NON-NLS-2$
			descriptor= ImageDescriptor.createFromFile(ProgressImages.class, failurename);
			fgFailureImages[i]= descriptor.createImage();
		}
	}
	
	static void dispose() {
		if (fgOKImages[0] == null)
			return;
		fgOKImages[0].dispose();
		fgFailureImages[0].dispose();
		
		for (int i= 1; i <= HEIGHT; i++) {
			fgOKImages[i].dispose();
			fgOKImages[i]= null;
			fgFailureImages[i].dispose();
			fgFailureImages[i]= null;
		}
	}
	
	static Image getImage(int current, int total, int errors, int failures) {
		if (total == 0)
			return fgOKImages[0];
		int index= (current*HEIGHT)/total;
		if (errors + failures == 0)
			return fgOKImages[index];
		return fgFailureImages[index];
	}	
}
