/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.actions.RetargetActionIDs;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.actions.IRefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.NewMoveWrapper;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;

public class ReorgGroup extends ContextMenuGroup {
	private static final String GROUP_NAME= IContextMenuConstants.GROUP_REORGANIZE;
	
	private IAction[] fBasicActions; //always added - just grayed out if disabled
	
	private UnifiedSite fSite;

	public ReorgGroup(UnifiedSite site){
		Assert.isNotNull(site);
		fSite= site;
	}	
	
	public void fill(IMenuManager manager, GroupContext context) {
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < fBasicActions.length; i++) {
			if (fBasicActions[i] instanceof IUpdate)
				((IUpdate)fBasicActions[i]).update();
			manager.appendToGroup(GROUP_NAME, fBasicActions[i]);
		}
	}
	
	private void createActions(ISelectionProvider p) {
		if (fBasicActions != null)
			return;
			
		fBasicActions= new IAction[] {	
			createCutAction(fSite, p),
			createCopyAction(fSite, p),
			createPasteAction(fSite, p),
			createDeleteAction(fSite, p),
		};
	}	
	
	public static void addGlobalReorgActions(UnifiedSite site, IActionBars actionBars, ISelectionProvider provider) {
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.COPY, createCopyAction(site, provider));
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.CUT, createCutAction(site, provider));
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.PASTE, createPasteAction(site, provider));
	}

	public static SelectionDispatchAction createCutAction(UnifiedSite site, ISelectionProvider p){
		SelectionDispatchAction a1= new CutSourceReferencesToClipboardAction(site);
		p.addSelectionChangedListener(a1);
		return a1;
	}
	
	public static SelectionDispatchAction createCopyAction(UnifiedSite site, ISelectionProvider p){
		SelectionDispatchAction a1= new CopyResourcesToClipboardAction(site);
		SelectionDispatchAction a2= new CopySourceReferencesToClipboardAction(site);
		SelectionDispatchAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.copy"), ReorgMessages.getString("copyAction.description"), a1, a2);//$NON-NLS-1$ //$NON-NLS-2$
		p.addSelectionChangedListener(dual);
		return dual;
	}
	
	public static SelectionDispatchAction createPasteAction(UnifiedSite site, ISelectionProvider p){
		SelectionDispatchAction a1= new PasteResourcesFromClipboardAction(site);
		SelectionDispatchAction a2= new PasteSourceReferencesFromClipboardAction(site);
		SelectionDispatchAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.paste"), ReorgMessages.getString("ReorgGroup.pasteAction.description"), a1, a2);//$NON-NLS-1$ //$NON-NLS-2$
		p.addSelectionChangedListener(dual);
		return dual;
	}
	
	public static SelectionDispatchAction createDeleteAction(UnifiedSite site, ISelectionProvider p){
		DeleteResourcesAction a1= new DeleteResourcesAction(site);
		DeleteSourceReferencesAction a2= new DeleteSourceReferencesAction(site);
		DualReorgAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.delete"), ReorgMessages.getString("deleteAction.description"), a1, a2); //$NON-NLS-1$ //$NON-NLS-2$
		p.addSelectionChangedListener(dual);
		return dual;
	}
	
	public static SelectionDispatchAction createPasteAction(final ISourceReference[] elements, Object target){
		return new PasteSourceReferencesFromClipboardAction(new MockUnifiedSite(new Object[]{target})){
			protected TypedSource[] getContentsToPaste(){
				List result= new ArrayList(elements.length);
				for(int i= 0; i < elements.length; i++){
					try {
						result.add(new TypedSource(elements[i]));
					} catch(JavaModelException e) {
						//ignore
					}
				}
				return (TypedSource[])result.toArray(new TypedSource[result.size()]);
			}
		};
	}
	
	public static DeleteSourceReferencesAction createDeleteSourceReferencesAction(ISourceReference[] elements){
		return new DeleteSourceReferencesAction(new MockUnifiedSite(elements));
	}	
	
	public static JdtCopyAction createDnDCopyAction(List elems, final IResource destination){
		JdtCopyAction action= new JdtCopyAction("#PASTE", new SimpleSelectionProvider(elems.toArray())){ //$NON-NLS-1$
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