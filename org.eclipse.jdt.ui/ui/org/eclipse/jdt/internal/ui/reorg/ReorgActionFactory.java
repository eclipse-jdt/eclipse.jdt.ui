/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.DeleteAction;

public class ReorgActionFactory {
	private ReorgActionFactory(){
	}
	public static SelectionDispatchAction createCutAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction){
		String helpContextID= IJavaHelpContextIds.CUT_ACTION;
		SelectionDispatchAction a1= new CutSourceReferencesToClipboardAction(site, clipboard, pasteAction, helpContextID);

		ISharedImages workbenchImages= getWorkbenchSharedImages();
		a1.setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
		a1.setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		a1.setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_HOVER));
		return a1;
	}
	
	public static SelectionDispatchAction createCopyAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction){
		SelectionDispatchAction a1= new CopyResourcesToClipboardAction(site, clipboard, pasteAction);
		SelectionDispatchAction a2= new CopySourceReferencesToClipboardAction(site, clipboard, pasteAction);
		String helpContextID= IJavaHelpContextIds.COPY_ACTION;
		SelectionDispatchAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.copy"), ReorgMessages.getString("copyAction.description"), a1, a2, helpContextID);//$NON-NLS-1$ //$NON-NLS-2$

		ISharedImages workbenchImages= getWorkbenchSharedImages();
		dual.setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
		dual.setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		dual.setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_HOVER));
		return dual;
	}
	
	public static SelectionDispatchAction createPasteAction(IWorkbenchSite site, Clipboard clipboard){
		SelectionDispatchAction a1= new PasteResourcesFromClipboardAction(site, clipboard);
		SelectionDispatchAction a2= new PasteSourceReferencesFromClipboardAction(site, clipboard);
		String helpContextID= IJavaHelpContextIds.PASTE_ACTION;
		SelectionDispatchAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.paste"), ReorgMessages.getString("ReorgGroup.pasteAction.description"), a1, a2, helpContextID);//$NON-NLS-1$ //$NON-NLS-2$

		ISharedImages workbenchImages= getWorkbenchSharedImages();
		dual.setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
		dual.setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		dual.setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_HOVER));
		return dual;
	}
	
	public static DeleteAction createDeleteAction(Object[] elements){
		return new DeleteAction(new MockWorkbenchSite(elements));
	}	

	public static SelectionDispatchAction createDeleteAction(IWorkbenchSite site){
		return new DeleteAction(site);
	}
	
	public static ISharedImages getWorkbenchSharedImages() {
		return JavaPlugin.getDefault().getWorkbench().getSharedImages();
	}
	
	public static SelectionDispatchAction createPasteAction(final ISourceReference[] elements, Object target){
		return new PasteSourceReferencesFromClipboardAction(new MockWorkbenchSite(new Object[]{target}), null){
			protected TypedSource[] getContentsToPaste(){
				List result= new ArrayList(elements.length);
				for(int i= 0; i < elements.length; i++){
					try {
						result.add(TypedSource.create(elements[i]));
					} catch(JavaModelException e) {
						//ignore
					}
				}
				return (TypedSource[])result.toArray(new TypedSource[result.size()]);
			}
		};
	}
	
	public static JdtCopyAction createDnDCopyAction(List elems, final IResource destination){
		JdtCopyAction action= new JdtCopyAction(new MockWorkbenchSite(elems)){
			protected Object selectDestination(ReorgRefactoring ref) {
				return ClipboardActionUtil.tryConvertingToJava(destination);			
			}
		};
		return action;
	}
	
	public static JdtCopyAction createDnDCopyAction(IResource[] resourceData, final IResource destination){
		return createDnDCopyAction(ClipboardActionUtil.getConvertedResources(resourceData), destination);
	}	
}
