package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Manages a set of images that can show progress in the image itself.
 */
public class ProgressImages {
	static final int HEIGHT= 16;
	static Image[] fgOKImages= new Image[HEIGHT];
	static Image[] fgFailureImages= new Image[HEIGHT];
	
	static void load() {
		if (fgOKImages[0] != null)
			return;
			
		for (int i= 0; i < HEIGHT; i++) {
			String okname= "icons/smalllogo-o"+Integer.toString(i+1)+".gif"; //$NON-NLS-1$ //$NON-NLS-2$
			ImageDescriptor descriptor= ImageDescriptor.createFromFile(ProgressImages.class, okname);
			fgOKImages[i]= descriptor.createImage();
			String failurename= "icons/smalllogo-e"+Integer.toString(i+1)+".gif"; //$NON-NLS-1$ //$NON-NLS-2$
			descriptor= ImageDescriptor.createFromFile(ProgressImages.class, failurename);
			fgFailureImages[i]= descriptor.createImage();
		}
	}
	
	static void dispose() {
		if (fgOKImages[0] == null)
			return; 
		fgOKImages[0].dispose();
		fgFailureImages[0].dispose();
		
		for (int i= 0; i < HEIGHT; i++) {
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
		index= Math.min(Math.max(0, index), 15);

		if (errors + failures == 0)
			return fgOKImages[index];
		return fgFailureImages[index];
	}	
}
