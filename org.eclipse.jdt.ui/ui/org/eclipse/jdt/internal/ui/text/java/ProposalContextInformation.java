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
package org.eclipse.jdt.internal.ui.text.java;



import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;


/**
 * Implementation of the <code>IContextInformation</code> interface.
 */
public final class ProposalContextInformation implements IContextInformation, IContextInformationExtension {
	
	private String fContextDisplayString;
	private String fInformationDisplayString;
	private Image fImage;
	private int fPosition;

	/**
	 * Creates a new context information.
	 */
	public ProposalContextInformation() {
	}
	
	/**
	 * @param image the image to display when presenting the context information
	 */
	public void setImage(Image image) {
		fImage= image;
	}
	
	/**
	 * @param contextDisplayString the string to be used when presenting the context
	 *		may not be <code>null</code>
	 */
	public void setContextDisplayString(String contextDisplayString) {
		Assert.isNotNull(contextDisplayString);
		fContextDisplayString= contextDisplayString;
	}
	
	/**
	 * @param informationDisplayString the string to be displayed when presenting the context information,
	 *		may not be <code>null</code>
	 */
	public void setInformationDisplayString(String informationDisplayString) {
		Assert.isNotNull(informationDisplayString);
		fInformationDisplayString= informationDisplayString;
	}		

	/*
	 * @see IContextInformation#equals
	 */
	public boolean equals(Object object) {
		if (object instanceof IContextInformation) {
			IContextInformation contextInformation= (IContextInformation) object;
			boolean equals= fInformationDisplayString.equalsIgnoreCase(contextInformation.getInformationDisplayString());
			if (fContextDisplayString != null) 
				equals= equals && fContextDisplayString.equalsIgnoreCase(contextInformation.getContextDisplayString());
			return equals;
		}
		return false;
	}
	
	/*
	 * @see IContextInformation#getInformationDisplayString()
	 */
	public String getInformationDisplayString() {
		return fInformationDisplayString;
	}
	
	/*
	 * @see IContextInformation#getImage()
	 */
	public Image getImage() {
		return fImage;
	}
	
	/*
	 * @see IContextInformation#getContextDisplayString()
	 */
	public String getContextDisplayString() {
		if (fContextDisplayString != null)
			return fContextDisplayString;
		return fInformationDisplayString;
	}
	
	/*
	 * @see IContextInformationExtension#getContextInformationPosition()
	 */
	public int getContextInformationPosition() {
		return fPosition;
	}
	
	public void setContextInformationPosition(int position) {
		fPosition= position;
	}
}
