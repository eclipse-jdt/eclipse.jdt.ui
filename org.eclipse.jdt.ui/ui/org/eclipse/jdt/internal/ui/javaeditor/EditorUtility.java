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

package org.eclipse.jdt.internal.ui.javaeditor;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.swt.SWT;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A number of routines for working with JavaElements in editors
 *
 * Use 'isOpenInEditor' to test if an element is already open in a editor  
 * Use 'openInEditor' to force opening an element in a editor
 * With 'getWorkingCopy' you get the working copy (element in the editor) of an element
 */
public class EditorUtility {
	
	
	public static boolean isEditorInput(Object element, IEditorPart editor) {
		if (editor != null) {
			try {
				return editor.getEditorInput().equals(getEditorInput(element));
			} catch (JavaModelException x) {
				JavaPlugin.log(x.getStatus());
			}
		}
		return false;
	}
	
	/** 
	 * Tests if a cu is currently shown in an editor
	 * @return the IEditorPart if shown, null if element is not open in an editor
	 */	
	public static IEditorPart isOpenInEditor(Object inputElement) {
		IEditorInput input= null;
		
		try {
			input= getEditorInput(inputElement);
		} catch (JavaModelException x) {
			JavaPlugin.log(x.getStatus());
		}
		
		if (input != null) {
			IWorkbenchPage p= JavaPlugin.getActivePage();
			if (p != null) {
				return p.findEditor(input);
			}
		}
		
		return null;
	}
	
	/**
	 * Opens a Java editor for an element such as <code>IJavaElement</code>, <code>IFile</code>, or <code>IStorage</code>.
	 * The editor is activated by default.
	 * @return the IEditorPart or null if wrong element type or opening failed
	 */
	public static IEditorPart openInEditor(Object inputElement) throws JavaModelException, PartInitException {
		return openInEditor(inputElement, true);
	}
		
	/**
	 * Opens a Java editor for an element (IJavaElement, IFile, IStorage...)
	 * @return the IEditorPart or null if wrong element type or opening failed
	 */
	public static IEditorPart openInEditor(Object inputElement, boolean activate) throws JavaModelException, PartInitException {
		
		if (inputElement instanceof IFile)
			return openInEditor((IFile) inputElement, activate);
		
		IEditorInput input= getEditorInput(inputElement);
		if (input instanceof IFileEditorInput) {
			IFileEditorInput fileInput= (IFileEditorInput) input;
			return openInEditor(fileInput.getFile(), activate);
		}
		
		if (input != null)
			return openInEditor(input, getEditorID(input, inputElement), activate);
			
		return null;
	}
	
	/** 
	 * Selects a Java Element in an editor
	 */	
	public static void revealInEditor(IEditorPart part, IJavaElement element) {
		if (element != null && part instanceof JavaEditor) {
			((JavaEditor) part).setSelection(element);
		}
	}
	
	private static IEditorPart openInEditor(IFile file, boolean activate) throws PartInitException {
		if (file != null) {
			IWorkbenchPage p= JavaPlugin.getActivePage();
			if (p != null) {
				IEditorPart editorPart= IDE.openEditor(p, file, activate);
				initializeHighlightRange(editorPart);
				return editorPart;
			}
		}
		return null;
	}

	private static IEditorPart openInEditor(IEditorInput input, String editorID, boolean activate) throws PartInitException {
		if (input != null) {
			IWorkbenchPage p= JavaPlugin.getActivePage();
			if (p != null) {
				IEditorPart editorPart= p.openEditor(input, editorID, activate);
				initializeHighlightRange(editorPart);
				return editorPart;
			}
		}
		return null;
	}

	private static void initializeHighlightRange(IEditorPart editorPart) {
		if (editorPart instanceof ITextEditor) {
			IAction toggleAction= editorPart.getEditorSite().getActionBars().getGlobalActionHandler(ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY);
			if (toggleAction != null && toggleAction.isEnabled() && toggleAction.isChecked()) {
				if (toggleAction instanceof TextEditorAction) {
					// Reset the action 
					((TextEditorAction)toggleAction).setEditor(null);
					// Restore the action 
					((TextEditorAction)toggleAction).setEditor((ITextEditor)editorPart);
				} else {
					// Uncheck 
					toggleAction.run();
					// Check
					toggleAction.run();
				}
			}
		}
	}
	
	/**
	 *@deprecated	Made it public again for java debugger UI.
	 */
	public static String getEditorID(IEditorInput input, Object inputObject) {
		IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
		String inputName= input.getName();

		/*
		 * XXX: This is code copied from IDE.openEditor
		 *      Filed bug 50285 requesting API for getting the descriptor 
		 */ 
		
		// check for a default editor
		IEditorDescriptor editorDescriptor= registry.getDefaultEditor(inputName);
		
		// next check the OS for in-place editor (OLE on Win32)
		if (editorDescriptor == null && registry.isSystemInPlaceEditorAvailable(inputName))
			editorDescriptor= registry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
		
		// next check with the OS for an external editor
		if (editorDescriptor == null && registry.isSystemExternalEditorAvailable(inputName))
			editorDescriptor= registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		
		// next lookup the default text editor
		if (editorDescriptor == null)
			editorDescriptor= registry.findEditor("org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
		
		// if no valid editor found, bail out
		if (editorDescriptor == null)
			return null;
		
		return editorDescriptor.getId();
	}
	
	private static IEditorInput getEditorInput(IJavaElement element) throws JavaModelException {
		while (element != null) {			
			if (element instanceof ICompilationUnit) {
				ICompilationUnit unit= JavaModelUtil.toOriginal((ICompilationUnit) element);
					IResource resource= unit.getResource();
					if (resource instanceof IFile)
						return new FileEditorInput((IFile) resource);
			}
			
			if (element instanceof IClassFile)
				return new InternalClassFileEditorInput((IClassFile) element);
			
			element= element.getParent();
		}
		
		return null;
	}	

	public static IEditorInput getEditorInput(Object input) throws JavaModelException {
		if (input instanceof IJavaElement)
			return getEditorInput((IJavaElement) input);
			
		if (input instanceof IFile) 
			return new FileEditorInput((IFile) input);
		
		if (input instanceof IStorage) 
			return new JarEntryEditorInput((IStorage)input);
	
		return null;
	}
	
	/**
	 * If the current active editor edits a java element return it, else
	 * return null
	 */
	public static IJavaElement getActiveEditorJavaInput() {
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page != null) {
			IEditorPart part= page.getActiveEditor();
			if (part != null) {
				IEditorInput editorInput= part.getEditorInput();
				if (editorInput != null) {
					return (IJavaElement)editorInput.getAdapter(IJavaElement.class);
				}
			}
		}
		return null;	
	}
			
	/**
	 * Maps the localized modifier name to a code in the same
	 * manner as #findModifier.
	 * 
	 * @param modifierName the modifier name
	 * @return the SWT modifier bit, or <code>0</code> if no match was found
	 * @since 2.1.1
	 */
	public static int findLocalizedModifier(String modifierName) {
		if (modifierName == null)
			return 0;
		
		if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.CTRL)))
			return SWT.CTRL;
		if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.SHIFT)))
			return SWT.SHIFT;
		if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.ALT)))
			return SWT.ALT;
		if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.COMMAND)))
			return SWT.COMMAND;

		return 0;
	}

	/**
	 * Returns the modifier string for the given SWT modifier
	 * modifier bits.
	 * 
	 * @param stateMask	the SWT modifier bits
	 * @return the modifier string
	 * @since 2.1.1
	 */
	public static String getModifierString(int stateMask) {
		String modifierString= ""; //$NON-NLS-1$
		if ((stateMask & SWT.CTRL) == SWT.CTRL)
			modifierString= appendModifierString(modifierString, SWT.CTRL);
		if ((stateMask & SWT.ALT) == SWT.ALT)
			modifierString= appendModifierString(modifierString, SWT.ALT);
		if ((stateMask & SWT.SHIFT) == SWT.SHIFT)
			modifierString= appendModifierString(modifierString, SWT.SHIFT);
		if ((stateMask & SWT.COMMAND) == SWT.COMMAND)
			modifierString= appendModifierString(modifierString,  SWT.COMMAND);
		
		return modifierString;
	}

	/**
	 * Appends to modifier string of the given SWT modifier bit
	 * to the given modifierString.
	 * 
	 * @param modifierString	the modifier string
	 * @param modifier			an int with SWT modifier bit
	 * @return the concatenated modifier string
	 * @since 2.1.1
	 */
	private static String appendModifierString(String modifierString, int modifier) {
		if (modifierString == null)
			modifierString= ""; //$NON-NLS-1$
		String newModifierString= Action.findModifierString(modifier);
		if (modifierString.length() == 0)
			return newModifierString;
		return JavaEditorMessages.getFormattedString("EditorUtility.concatModifierStrings", new String[] {modifierString, newModifierString}); //$NON-NLS-1$
	}
	
	/**
	 * Returns the Java project for a given editor input or <code>null</code> if no corresponding
	 * Java project exists.
	 * 
	 * @param input the editor input
	 * @return the corresponding Java project
	 * 
	 * @since 3.0
	 */
	public static IJavaProject getJavaProject(IEditorInput input) {
		IJavaProject jProject= null;
		if (input instanceof IFileEditorInput) {
			IProject project= ((IFileEditorInput)input).getFile().getProject();
			if (project != null) {
				jProject= JavaCore.create(project);
				if (!jProject.exists())
					jProject= null;
			}
		} else if (input instanceof IClassFileEditorInput) {
			jProject= ((IClassFileEditorInput)input).getClassFile().getJavaProject();
		}
		return jProject;
	}
}
