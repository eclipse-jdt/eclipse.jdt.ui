/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;



import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;

import org.eclipse.jdt.core.CompletionProposal;

import org.eclipse.jdt.ui.text.java.ProposalLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Implementation of the <code>IContextInformation</code> interface.
 */
public final class ProposalContextInformation implements IContextInformation, IContextInformationExtension {
	
	private final CompletionProposal fProposal;
	
	/* lazy cache */
	private ProposalLabelProvider fLabelProvider;
	private String fContextDisplayString;
	private String fInformationDisplayString;
	private Image fImage;

	/**
	 * Creates a new context information.
	 */
	public ProposalContextInformation(CompletionProposal proposal) {
		fProposal= proposal;
	}
	
	/*
	 * @see IContextInformation#equals
	 */
	public boolean equals(Object object) {
		if (object instanceof IContextInformation) {
			IContextInformation contextInformation= (IContextInformation) object;
			boolean equals= getInformationDisplayString().equalsIgnoreCase(contextInformation.getInformationDisplayString());
			if (getContextDisplayString() != null) 
				equals= equals && getContextDisplayString().equalsIgnoreCase(contextInformation.getContextDisplayString());
			return equals;
		}
		return false;
	}
	
	/*
	 * @see IContextInformation#getInformationDisplayString()
	 */
	public String getInformationDisplayString() {
		if (fInformationDisplayString == null) {
			fInformationDisplayString= getLabelProvider().createParameterList(fProposal);
		}
		return fInformationDisplayString;
	}
	
	/*
	 * @see IContextInformation#getImage()
	 */
	public Image getImage() {
		if (fImage == null) {
			ImageDescriptor descriptor= getLabelProvider().createImageDescriptor(fProposal);
			if (descriptor != null)
				fImage= JavaPlugin.getImageDescriptorRegistry().get(descriptor);
		}
		return fImage;
	}
	
	/*
	 * @see IContextInformation#getContextDisplayString()
	 */
	public String getContextDisplayString() {
		if (fContextDisplayString == null) {
			fContextDisplayString= getLabelProvider().createLabel(fProposal);
		}
		return fContextDisplayString;
	}
	
	/*
	 * @see IContextInformationExtension#getContextInformationPosition()
	 */
	public int getContextInformationPosition() {
		if (fProposal.getCompletion().length == 0)
			return fProposal.getCompletionLocation() + 1;
		return -1;
	}
	
	private ProposalLabelProvider getLabelProvider() {
		if (fLabelProvider == null)
			fLabelProvider= new ProposalLabelProvider();
		return fLabelProvider;
	}
}
