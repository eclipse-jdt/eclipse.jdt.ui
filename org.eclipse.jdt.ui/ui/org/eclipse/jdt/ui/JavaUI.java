/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.SharedImages;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.MainTypeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.MultiMainTypeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.MultiTypeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * Central access point for the Java UI plug-in (id <code>"org.eclipse.jdt.ui"</code>).
 * This class provides static methods for:
 * <ul>
 *  <li> creating various kinds of selection dialogs to present a collection
 *       of Java elements to the user and let them make a selection.</li>
 *  <li> opening a Java editor on a compilation unit.</li> 
 * </ul>
 * <p>
 * This class provides static methods and fields only; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
 */
public final class JavaUI {
	
	private static ISharedImages fgSharedImages= null;
	
	private JavaUI() {
		// prevent instantiation of JavaUI.
	}
	
	/**
	 * The id of the Java plugin (value <code>"org.eclipse.jdt.ui"</code>).
	 */	
	public static final String ID_PLUGIN= "org.eclipse.jdt.ui";
	
	/**
	 * The id of the Java perspective
	 * (value <code>"org.eclipse.jdt.ui.JavaPerspective"</code>).
	 */	
	public static final String ID_PERSPECTIVE= 		"org.eclipse.jdt.ui.JavaPerspective";
	
	/**
	 * The id of the Java action set
	 * (value <code>"org.eclipse.jdt.ui.JavaActionSet"</code>).
	 */
	public static final String ID_ACTION_SET= 		"org.eclipse.jdt.ui.JavaActionSet";
	
	/**
	 * The editor part id of the editor that presents Java compilation units
	 * (value <code>"org.eclipse.jdt.ui.CompilationUnitEditor"</code>).
	 */	
	public static final String ID_CU_EDITOR=			"org.eclipse.jdt.ui.CompilationUnitEditor";
	
	/**
	 * The editor part id of the editor that presents Java binary class files
	 * (value <code>"org.eclipse.jdt.ui.ClassFileEditor"</code>).
	 */
	public static final String ID_CF_EDITOR=			"org.eclipse.jdt.ui.ClassFileEditor";
	
	/**
	 * The editor part id of the code snippet editor
	 * (value <code>"org.eclipse.jdt.ui.SnippetEditor"</code>).
	 */
	public static final String ID_SNIPPET_EDITOR= 		"org.eclipse.jdt.ui.SnippetEditor";

	/**
	 * The view part id of the Packages view
	 * (value <code>"org.eclipse.jdt.ui.PackageExplorer"</code>).
	 * <p>
	 * When this id is used to access
	 * a view part with <code>IWorkbenchPage.findView</code> or 
	 * <code>showView</code>, the returned <code>IViewPart</code>
	 * can be safely cast to an <code>IPackagesViewPart</code>.
	 * </p>
	 *
	 * @see IPackagesViewPart
	 * @see org.eclipse.ui.IWorkbenchPage#findView
	 * @see org.eclipse.ui.IWorkbenchPage#showView
	 */ 
	public static final String ID_PACKAGES= 			"org.eclipse.jdt.ui.PackageExplorer";
	
	/** 
	 * The view part id of the type hierarchy part.
	 * (value <code>"org.eclipse.jdt.ui.TypeHierarchy"</code>).
	 * <p>
	 * When this id is used to access
	 * a view part with <code>IWorkbenchPage.findView</code> or 
	 * <code>showView</code>, the returned <code>IViewPart</code>
	 * can be safely cast to an <code>ITypeHierarchyViewPart</code>.
	 * </p>
	 *
	 * @see ITypeHierarchyViewPart
	 * @see org.eclipse.ui.IWorkbenchPage#findView
	 * @see org.eclipse.ui.IWorkbenchPage#showView
	 */ 
	public static final String ID_TYPE_HIERARCHY= 		"org.eclipse.jdt.ui.TypeHierarchy";
	
	/**
	 * The class org.eclipse.debug.core.model.IProcess allows attaching
	 * String properties to processes. The Java UI contributes a property
	 * page for IProcess that will show the contents of the property
	 * with this key.
	 * The intent of this property is to show the command line a process
	 * was launched with.
	 * @deprecated
	 */
	public final static String ATTR_CMDLINE= JavaPlugin.getPluginId()+".launcher.cmdLine";

	/**
	 * Returns the shared images for the Java UI.
	 *
	 * @return the shared images manager
	 */
	public static ISharedImages getSharedImages() {
		if (fgSharedImages == null)
			fgSharedImages= new SharedImages();
			
		return fgSharedImages;
	}
	 
	/**
	 * Creates a selection dialog that lists all packages of the given Java project.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected packages (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param project the Java project
	 * @param style flags defining the style of the dialog; the only valid flag is
	 *   <code>IJavaElementSearchStyles.CONSIDER_BINARIES</code>, indicating that 
	 *   packages from binary package fragment roots should be included in addition
	 *   to those from source package fragment roots
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IJavaProject project, int style) throws JavaModelException {
		Assert.isTrue((style | IJavaElementSearchConstants.CONSIDER_BINARIES) == IJavaElementSearchConstants.CONSIDER_BINARIES);
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();	
		List consideredRoots= null;
		if ((style & IJavaElementSearchConstants.CONSIDER_BINARIES) != 0) {
			consideredRoots= Arrays.asList(roots);
		} else {
			consideredRoots= new ArrayList(roots.length);
			for (int i= 0; i < roots.length; i++) {
				IPackageFragmentRoot root= roots[i];
				if (root.getKind() != IPackageFragmentRoot.K_BINARY)
					consideredRoots.add(root);
					
			}
		}
		
		int flags= JavaElementLabelProvider.SHOW_DEFAULT;
		if (consideredRoots.size() > 1)
			flags= flags | JavaElementLabelProvider.SHOW_CONTAINER;

		List packages= new ArrayList();
		Iterator iter= consideredRoots.iterator();
		while(iter.hasNext()) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)iter.next();
			packages.addAll(Arrays.asList(root.getChildren()));
		}			
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(parent, new JavaElementLabelProvider(flags), false, false);
		dialog.setElements(packages);
		return dialog;
	}
	
	/**
	 * Creates a selection dialog that lists all packages under the given package 
	 * fragment root.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected packages (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param root the package fragment root
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IPackageFragmentRoot root) throws JavaModelException {
		List packages= new ArrayList();
		packages.addAll(Arrays.asList(root.getChildren()));
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(parent, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), false, false);
		dialog.setElements(packages);
		return dialog;		
	}

	/**
	 * Creates a selection dialog that lists all types in the given scope.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected packages (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param scope the scope that limits which types are included
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaElementSearchStyles.CONSIDER_CLASSES</code>,
	 *   <code>CONSIDER_INTERFACES</code>, or their bitwise OR 
	 *   (equivalent to <code>CONSIDER_TYPES</code>)
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean multipleSelection) throws JavaModelException {
		Assert.isTrue((style | IJavaElementSearchConstants.CONSIDER_TYPES) == IJavaElementSearchConstants.CONSIDER_TYPES);
		SelectionDialog dialog= null;
		if (multipleSelection) {
			dialog= new MultiTypeSelectionDialog(parent, context, scope, style, true);
		} else {
			dialog= new TypeSelectionDialog(parent, context, scope, style, true, true);
		}
		return dialog;
	}
		
	/**
	 * Creates a selection dialog that lists all types in the given scope containing 
	 * a standard <code>main</code> method.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected packages (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * <p>
	 * [Issue: IJavaSearchScope is not currently part of the Java core API.]
	 * </p>
	 * <p>
	 * [Issue: SelectionDialogs must be parented. shell must not be null.]
	 * </p>
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param scope the scope that limits which types are included
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaElementSearchStyles.CONSIDER_BINARIES</code>,
	 *   <code>CONSIDER_EXTERNAL_JARS</code>, or their bitwise OR, or <code>0</code>
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @return a new selection dialog
	 */
	public static SelectionDialog createMainTypeDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean multipleSelection) {
		SelectionDialog dialog= null;
		if (multipleSelection) {
			dialog= new MultiMainTypeSelectionDialog(parent, context, scope, style, true);
		} else {
			dialog= new MainTypeSelectionDialog(parent, context, scope, style, true);
		}
		return dialog;
	}

	/**
	 * Creates a selection dialog that lists all types in the given project.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected packages (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param project the Java project
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaElementSearchStyles.CONSIDER_CLASSES</code>,
	 *   <code>CONSIDER_INTERFACES</code>, or their bitwise OR 
	 *   (equivalent to <code>CONSIDER_TYPES</code>)
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context, IProject project, int style, boolean multipleSelection) throws JavaModelException {
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IResource[] { project });
		return createTypeDialog(parent, context, scope, style, multipleSelection);
	}
	
	/**
	 * Opens a Java editor on the given Java compilation unit of class file. 
	 * If there already is an open Java editor for the given element, it is returned.
	 * <p>
	 * [Issue: Explain semantics of opening an editor on a class file.]
	 * </p>
	 * <p>
	 * [Issue: Explain under which conditions it returns null, throws JavaModelException,
	 *  or throws JavaModelException.
	 * ]
	 * </p>
	 *
	 * @param element the input element; either a compilation unit 
	 *   (<code>ICompilationUnit</code>) or a class file (</code>IClassFile</code>)
	 * @return the editor, or </code>null</code> if wrong element type or opening failed
	 * @exception JavaModelException if ???
	 * @exception PartInitException if ???	 
	 */
	public static IEditorPart openInEditor(IJavaElement element) throws JavaModelException, PartInitException {
		return EditorUtility.openInEditor(element);
	}

	/** 
	 * Reveals the source range of the given source reference element in the
	 * given editor. No checking is done if the editor displays a compilation unit or
	 * class file that contains the given source reference.
	 * <p>
	 * [Issue: Explain what is meant by that last sentence.]
	 * </p>
	 * <p>
	 * [Issue: Explain what happens if the source reference is from some other
	 *  compilation unit, editor is not open, etc.]
	 * </p>
	 *
	 * @param part the editor displaying the compilation unit or class file
	 * @param element the source reference element defining the source range to be
	 *   revealed
	 */	
	public static void revealInEditor(IEditorPart part, ISourceReference element) {
		EditorUtility.revealInEditor(part, element);
	}
	 
	/**
	 * Returns the working copy manager for the Java UI plug-in.
	 *
	 * @return the working copy manager for the Java UI plug-in
	 */
	public static IWorkingCopyManager getWorkingCopyManager() {
		return JavaPlugin.getDefault().getWorkingCopyManager();
	}
}