/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.net.MalformedURLException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Manages a set of images that can show progress in the image itself.
 */
public class ProgressImages {
	static final int PROGRESS_STEPS= 9;
	
	static final String BASE= "prgss/"; //$NON-NLS-1$
	static final String FAILURE= "ff"; //$NON-NLS-1$
	static final String OK= "ss"; //$NON-NLS-1$
	
	Image[] fOKImages= new Image[PROGRESS_STEPS];
	Image[] fFailureImages= new Image[PROGRESS_STEPS];
	Image fMissingImage= null;
	
	void load() {
		if (isLoaded())
			return;
			
		for (int i= 0; i < PROGRESS_STEPS; i++) {
			String okname= BASE+OK+Integer.toString(i+1)+".gif"; //$NON-NLS-1$ 
			fOKImages[i]= createImage(okname);
			String failurename= BASE+FAILURE+Integer.toString(i+1)+".gif"; //$NON-NLS-1$ 
			fFailureImages[i]= createImage(failurename);
		}
	}

	Image createImage(String name) {
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(JUnitPlugin.makeIconFileURL(name));
			return id.createImage();
		} catch (MalformedURLException e) {
			// fall through
		}  
		if (fMissingImage == null) 
			fMissingImage= ImageDescriptor.getMissingImageDescriptor().createImage();
		return fMissingImage;
	}
	
	public void dispose() {
		if (!isLoaded())
			return; 
			
		if (fMissingImage != null)
			fMissingImage.dispose();	
				
		for (int i= 0; i < PROGRESS_STEPS; i++) {
			fOKImages[i].dispose();
			fOKImages[i]= null;
			fFailureImages[i].dispose();
			fFailureImages[i]= null;
		}
	}
	
	public Image getImage(int current, int total, int errors, int failures) {
		if (!isLoaded())
			load();
			
		if (total == 0)
			return fOKImages[0];
		int index= ((current*PROGRESS_STEPS)/total)-1;
		index= Math.min(Math.max(0, index), PROGRESS_STEPS-1);

		if (errors + failures == 0)
			return fOKImages[index];
		return fFailureImages[index];
	}
	
	boolean isLoaded() {
		return fOKImages[0] != null;
	}	
}
