/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;


public class CPVariableElementLabelProvider extends LabelProvider {
	
	private Image fVariableImage;
	private Image fJARImage;
	private Image fFolderImage;
	private boolean fShowResolvedVariables;
	
	public CPVariableElementLabelProvider(boolean showResolvedVariables) {
		ImageRegistry reg= JavaPlugin.getDefault().getImageRegistry();
		fJARImage= reg.get(JavaPluginImages.IMG_OBJS_EXTJAR);
		fFolderImage= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		fShowResolvedVariables= showResolvedVariables;
	}
	
	/*
	 * @see LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof CPVariableElement) {
			CPVariableElement curr= (CPVariableElement) element;
			IPath path= curr.getPath();
			if (path.toFile().isFile()) {
				return fJARImage;
			}
			return fFolderImage;
		}
		return super.getImage(element);
	}

	/*
	 * @see LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof CPVariableElement) {
			CPVariableElement curr= (CPVariableElement)element;
			String name= curr.getName();
			IPath path= curr.getPath();
			StringBuffer buf= new StringBuffer(name);
			if (curr.isReserved()) {
				buf.append(' ');
				buf.append(NewWizardMessages.getString("CPVariableElementLabelProvider.reserved")); //$NON-NLS-1$
			}
			if (fShowResolvedVariables || !curr.isReserved()) {
				if (path != null) {
					buf.append(" - "); //$NON-NLS-1$
					if (!path.isEmpty()) {
						buf.append(path.toOSString());
					} else {
						buf.append(NewWizardMessages.getString("CPVariableElementLabelProvider.empty")); //$NON-NLS-1$
					}
				}
			}
			return buf.toString();
		}		
		
		
		return super.getText(element);
	}

}
