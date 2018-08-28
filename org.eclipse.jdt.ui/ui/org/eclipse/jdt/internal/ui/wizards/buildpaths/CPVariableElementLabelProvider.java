/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;


public class CPVariableElementLabelProvider extends LabelProvider {

	// shared, do not dispose:
	private Image fJARImage;
	private Image fFolderImage;

	private Image fDeprecatedJARImage;
	private Image fDeprecatedFolderImage;

	private boolean fHighlightReadOnly;

	public CPVariableElementLabelProvider(boolean highlightReadOnly) {
		ImageRegistry reg= JavaPlugin.getDefault().getImageRegistry();
		fJARImage= reg.get(JavaPluginImages.IMG_OBJS_EXTJAR);
		fFolderImage= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		fDeprecatedJARImage= new DecorationOverlayIcon(fJARImage, JavaPluginImages.DESC_OVR_DEPRECATED, IDecoration.TOP_LEFT).createImage();
		fDeprecatedFolderImage= new DecorationOverlayIcon(fFolderImage, JavaPluginImages.DESC_OVR_DEPRECATED, IDecoration.TOP_LEFT).createImage();

		fHighlightReadOnly= highlightReadOnly;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof CPVariableElement) {
			CPVariableElement curr= (CPVariableElement) element;
			IPath path= curr.getPath();
			if (path.toFile().isFile()) {
				return curr.isDeprecated() ? fDeprecatedJARImage : fJARImage;
			}
			return curr.isDeprecated() ? fDeprecatedFolderImage : fFolderImage;
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof CPVariableElement) {
			CPVariableElement curr= (CPVariableElement)element;
			String name= curr.getName();
			IPath path= curr.getPath();

			String result= name;
			ArrayList<String> restrictions= new ArrayList<>(2);

			if (curr.isReadOnly() && fHighlightReadOnly) {
				restrictions.add(NewWizardMessages.CPVariableElementLabelProvider_read_only);
			}
			if (curr.isDeprecated()) {
				restrictions.add(NewWizardMessages.CPVariableElementLabelProvider_deprecated);
			}
			if (restrictions.size() == 1) {
				result= Messages.format(NewWizardMessages.CPVariableElementLabelProvider_one_restriction, new Object[] {result, restrictions.get(0)});
			} else if (restrictions.size() == 2) {
				result= Messages.format(NewWizardMessages.CPVariableElementLabelProvider_two_restrictions, new Object[] {result, restrictions.get(0), restrictions.get(1)});
			}

			if (path != null) {
				String appendix;
				if (!path.isEmpty()) {
					appendix= BasicElementLabels.getPathLabel(path, true);
				} else {
					appendix= NewWizardMessages.CPVariableElementLabelProvider_empty;
				}
				result= Messages.format(NewWizardMessages.CPVariableElementLabelProvider_appendix, new Object[] {result, appendix});
			}

			return result;
		}


		return super.getText(element);
	}

	@Override
	public void dispose() {
		super.dispose();
		fDeprecatedFolderImage.dispose();
		fDeprecatedJARImage.dispose();
	}

}
