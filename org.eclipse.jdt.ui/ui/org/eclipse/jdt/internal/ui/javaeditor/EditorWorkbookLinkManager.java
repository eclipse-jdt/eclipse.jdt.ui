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
package org.eclipse.jdt.internal.ui.javaeditor;


import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.EditorArea;
import org.eclipse.ui.internal.EditorPane;
import org.eclipse.ui.internal.EditorPresentation;
import org.eclipse.ui.internal.EditorWorkbook;
import org.eclipse.ui.internal.LayoutPart;
import org.eclipse.ui.internal.WorkbenchPage;

/**
 * Manage the linking between an editor selection and a target editor workbooks 
 */
public class EditorWorkbookLinkManager {
	
	private IEditorPart fSourceEditor;
		
	public EditorWorkbookLinkManager(IEditorPart editor) {
		fSourceEditor= editor;
	}
	
	/**
	 * open the given element in the linked workbook
	 */
	public void openInLinkedEditor(IJavaElement element) {
		if (fSourceEditor != null) {
			IWorkbenchSite site= fSourceEditor.getSite();
			WorkbenchPage page= (WorkbenchPage)site.getPage();
			
			// only link from active editor
			if (!isEditorActive(site))
				return;
				
			IEditorInput input= null;
			try {
				input= EditorUtility.getEditorInput(element);
			} catch (JavaModelException e) {
			}
			
			EditorPresentation presentation= page.getEditorPresentation();
			final EditorArea area= (EditorArea)presentation.getLayoutPart();
			final EditorWorkbook linkedWorkbook= findLinkedWorkbook(area);
			
			if (input == null || linkedWorkbook == null)
				return;
				
			JavaEditor editor= findOpenEditor(fSourceEditor, input, linkedWorkbook);
			if (editor == null)
				editor= findReusableEditor(fSourceEditor, input, linkedWorkbook);
						
			if (editor != null) {	
				openTargetEditor(element, input, editor);
				return;
			}
			openNewTargetEditor(linkedWorkbook, element, input);
		}
	}

	private void openNewTargetEditor(final EditorWorkbook workbook, final IJavaElement element, final IEditorInput input) {
		IWorkbenchSite site= fSourceEditor.getSite();
		final WorkbenchPage page= (WorkbenchPage)site.getPage();
		EditorPresentation presentation= page.getEditorPresentation();
		final EditorArea area= (EditorArea)presentation.getLayoutPart();

		IEditorPart editor= null;
		try {
			area.setActiveWorkbook(workbook, false);
			editor= EditorUtility.openInEditor(element, true);
		} catch (PartInitException e) {
		} catch (JavaModelException e) {
		}
		if (editor != null)
			revealElement(element, page, editor);
	}

	void revealElement(final IJavaElement element, final IWorkbenchPage page, IEditorPart editor) {
		EditorUtility.revealInEditor(editor, element);
		page.bringToTop(editor);
		page.activate(fSourceEditor);
	}
			
	private boolean isEditorActive(IWorkbenchSite site) {
		IWorkbenchWindow window= site.getWorkbenchWindow();
		IPartService service= window.getPartService();
		return service.getActivePart() == fSourceEditor;
	}
	
	/**
	 * TBD: need a way to set the linkedWorkbook
	 */
	private EditorWorkbook findLinkedWorkbook(EditorArea area) {
		ArrayList list= area.getEditorWorkbooks();
		EditorWorkbook activeWorkbook= area.getActiveWorkbook();
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			EditorWorkbook workbook= (EditorWorkbook) iter.next();
			if (workbook != activeWorkbook) {
				return workbook;
			}
		}
		return null;
	}

	private JavaEditor findReusableEditor(IEditorPart activeEditor, IEditorInput input, EditorWorkbook linkedWorkbook) {
		LayoutPart[] panes= linkedWorkbook.getChildren();
		for (int i= 0; i < panes.length; i++) {
			EditorPane pane= (EditorPane)panes[i];
			IEditorReference reference= pane.getEditorReference();
			IEditorPart part= (IEditorPart) reference.getPart(false);
			if (part == null) {
				activateEditorPart(reference);
				part= (IEditorPart) reference.getPart(false);
			}
			
			if (part != activeEditor) {
				// don't reuse dirty editors unless it is the
				// same as the active editor)
				if (part.isDirty() && !activeEditor.getEditorInput().equals(part.getEditorInput())) 
					continue;
				if (input instanceof IClassFileEditorInput && part instanceof ClassFileEditor) {
					return (JavaEditor)part;
				}
				if (input instanceof IFileEditorInput && part instanceof CompilationUnitEditor) {
					return (JavaEditor)part;
				}
			}
		}
		return null;
	}

	private JavaEditor findOpenEditor(IEditorPart activeEditor, IEditorInput input, EditorWorkbook linkedWorkbook) {
		LayoutPart[] panes= linkedWorkbook.getChildren();
		for (int i = 0; i < panes.length; i++) {
			EditorPane pane= (EditorPane)panes[i];
			IEditorReference reference= pane.getEditorReference();
			IEditorPart part= (IEditorPart) reference.getPart(false);
			if (part == null) {
				activateEditorPart(reference);
				part= (IEditorPart) reference.getPart(false);
			}
			if (part != activeEditor) {
				if (part instanceof JavaEditor) {
					if (part.getEditorInput().equals(input))
						return (JavaEditor)part;
				}
			}
		}
		return null;
	}

	/**
	 * Method activateEditorPart.
	 * @param reference
	 */
	private void activateEditorPart(final IEditorReference reference) {
		// hack should not activate the editor prematurely
		reference.getPart(true);
	}

	private void openTargetEditor(final IJavaElement curr, final IEditorInput input, final JavaEditor javaEditor) {
		IWorkbenchSite site= fSourceEditor.getSite();
		final IWorkbenchPage page= site.getPage();
			
		if (!javaEditor.getEditorInput().equals(input))
			javaEditor.setInput(input);
		revealElement(curr, page, javaEditor);
	}
}
