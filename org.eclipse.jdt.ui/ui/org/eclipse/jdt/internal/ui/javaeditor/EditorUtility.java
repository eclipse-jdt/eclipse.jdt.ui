/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IMarker;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IStorage;import org.eclipse.core.runtime.CoreException;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.IBreakpointManager;import org.eclipse.jdt.core.IClassFile;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.ISourceReference;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.IWorkingCopy;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.debug.core.JDIDebugModel;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.ui.IEditorDescriptor;import org.eclipse.ui.IEditorInput;import org.eclipse.ui.IEditorPart;import org.eclipse.ui.IEditorRegistry;import org.eclipse.ui.IFileEditorInput;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.PartInitException;import org.eclipse.ui.PlatformUI;import org.eclipse.ui.part.FileEditorInput;

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
		}
		
		if (input != null) {
			IWorkbenchPage p= JavaPlugin.getDefault().getActivePage();
			if (p != null) {
				IEditorPart[] editors= p.getEditors();
				for (int i= 0; i < editors.length; i++) {
					if (input.equals(editors[i].getEditorInput()))
						return editors[i];
				}
			}
		}
		
		return null;
	}
		
	/**
	 * Opens a Java editor for the given element if the element is a
	 * Java compilation unit or a Java class file.
	 * @return the IEditorPart or null if wrong element type or opening failed
	 */
	public static IEditorPart openInEditor(Object inputElement) throws JavaModelException, PartInitException {
		if (inputElement instanceof IFile)
			return openInEditor((IFile) inputElement);
		
		if (inputElement instanceof IStorage) {
			JarEntryEditorInput jarEditorInput= new JarEntryEditorInput((IStorage)inputElement);
			return openInEditor(jarEditorInput, getEditorID(jarEditorInput, inputElement));
		}

		IEditorInput input= getEditorInput(inputElement);
		return openInEditor(input, getEditorID(input, inputElement));
	}
				
	/** 
	 * Selects a Java Element in an editor
	 */	
	public static void revealInEditor(IEditorPart part, ISourceReference element) {
		if (element != null && part instanceof JavaEditor) {
			((JavaEditor) part).setSelection(element);
		}
	}
	
	private static IEditorPart openInEditor(IFile file) throws PartInitException {
		if (file != null) {
			IWorkbenchPage p= JavaPlugin.getDefault().getActivePage();
			if (p != null)
				return p.openEditor(file);
		}
		return null;
	}
	
	private static IEditorPart openInEditor(IEditorInput input, String editorID) throws PartInitException {
		if (input != null) {
			IWorkbenchPage p= JavaPlugin.getDefault().getActivePage();
			if (p != null)
				return p.openEditor(input, editorID);
		}
		return null;
	}
	
	/**
	 *@deprecated	Made it public again for java debugger UI.
	 */
	public static String getEditorID(IEditorInput input, Object inputObject) {
		if (input instanceof ClassFileEditorInput) {
			return JavaUI.ID_CF_EDITOR;
		} 
		if (input instanceof IFileEditorInput) {
			return JavaUI.ID_CU_EDITOR;
		} 
		if (input instanceof JarEntryEditorInput) {
			IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
			IEditorDescriptor descriptor= registry.getDefaultEditor(((JarEntryEditorInput)input).getName());
			if (descriptor != null)
				return descriptor.getId();
			return null;
		}
		
		return null;
	}
	
	private static IEditorInput getEditorInput(IJavaElement element) throws JavaModelException {
		while (element != null) {
			if (element instanceof IWorkingCopy && ((IWorkingCopy) element).isWorkingCopy()) 
				element= ((IWorkingCopy) element).getOriginalElement();
				
			if (element instanceof ICompilationUnit) {
				ICompilationUnit unit= (ICompilationUnit) element;
					IResource resource= unit.getUnderlyingResource();
					if (resource instanceof IFile)
						return new FileEditorInput((IFile) resource);
			}
			
			if (element instanceof IClassFile)
				return new ClassFileEditorInput((IClassFile) element);
			
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
	 * Gets the working copy of an compilation unit opened in an editor
	 * @param part the editor part
	 * @param cu the original compilation unit (or another working copy)
	 * @return the working copy of the compilation unit, or null if not found
	 */	
	public static ICompilationUnit getWorkingCopy(ICompilationUnit cu) throws JavaModelException {
		if (cu.isWorkingCopy()) {
			return cu;
		}
		IEditorInput editorInput= getEditorInput(cu);
		return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput);
	}
	
	/** 
	 * Gets the working copy of an type opened in an editor
	 * @param part the editor part
	 * @param type the original type (or another working copy)
	 * @return the working copy of the type, or null if not found
	 */	
	public static IType getWorkingCopy(IType type) throws JavaModelException {
		ICompilationUnit cu= type.getCompilationUnit();
		if (cu != null) {
			ICompilationUnit workingCopy= getWorkingCopy(cu);
			if (workingCopy != null) {
				String typeQualifiedName= JavaModelUtility.getTypeQualifiedName(type);
				return JavaModelUtility.findTypeInCompilationUnit(workingCopy, typeQualifiedName);
			}
		}
		return null;
	}	
	
	public static boolean isDuplicateBreakpoint(String modelId, String markerType, IType containingType, int lineNumber) throws CoreException {
		IBreakpointManager m= DebugPlugin.getDefault().getBreakpointManager();
		IMarker[] bps= m.getBreakpoints(modelId);
		for (int i = 0; i < bps.length; i++) {
			IMarker existingBP = bps[i];
			if (existingBP.getType().equals(markerType)) {
				if (JDIDebugModel.getType(existingBP).equals(containingType)) {
					if (m.getLineNumber(existingBP) == lineNumber) {
						return true;
					}
				}
			}
		}
		return false;
	}

}