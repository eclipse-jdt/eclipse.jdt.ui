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
package org.eclipse.jdt.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.IBufferFactory;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.SharedImages;
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
	public static final String ID_PLUGIN= "org.eclipse.jdt.ui"; //$NON-NLS-1$
	
	/**
	 * The id of the Java perspective
	 * (value <code>"org.eclipse.jdt.ui.JavaPerspective"</code>).
	 */	
	public static final String ID_PERSPECTIVE= 		"org.eclipse.jdt.ui.JavaPerspective"; //$NON-NLS-1$
	
	/**
	 * The id of the Java hierarchy perspective
	 * (value <code>"org.eclipse.jdt.ui.JavaHierarchyPerspective"</code>).
	 */	
	public static final String ID_HIERARCHYPERSPECTIVE= "org.eclipse.jdt.ui.JavaHierarchyPerspective"; //$NON-NLS-1$

	/**
	 * The id of the Java action set
	 * (value <code>"org.eclipse.jdt.ui.JavaActionSet"</code>).
	 */
	public static final String ID_ACTION_SET= "org.eclipse.jdt.ui.JavaActionSet"; //$NON-NLS-1$

	/**
	 * The id of the Java Element Creation action set
	 * (value <code>"org.eclipse.jdt.ui.JavaElementCreationActionSet"</code>).
	 * 
	 * @since 2.0
	 */
	public static final String ID_ELEMENT_CREATION_ACTION_SET= "org.eclipse.jdt.ui.JavaElementCreationActionSet"; //$NON-NLS-1$
	
	/**
	 * The id of the Java Coding action set
	 * (value <code>"org.eclipse.jdt.ui.CodingActionSet"</code>).
	 * 
	 * @since 2.0
	 */
	public static final String ID_CODING_ACTION_SET= "org.eclipse.jdt.ui.CodingActionSet"; //$NON-NLS-1$

	/**
	 * The id of the Java action set for open actions
	 * (value <code>"org.eclipse.jdt.ui.A_OpenActionSet"</code>).
	 * 
	 * @since 2.0
	 */
	public static final String ID_OPEN_ACTION_SET= "org.eclipse.jdt.ui.A_OpenActionSet"; //$NON-NLS-1$

	/**
	 * The id of the Java Search action set
	 * (value <code>org.eclipse.jdt.ui.SearchActionSet"</code>).
	 * 
	 * @since 2.0
	 */
	public static final String ID_SEARCH_ACTION_SET= "org.eclipse.jdt.ui.SearchActionSet"; //$NON-NLS-1$
	
	/**
	 * The editor part id of the editor that presents Java compilation units
	 * (value <code>"org.eclipse.jdt.ui.CompilationUnitEditor"</code>).
	 */	
	public static final String ID_CU_EDITOR=			"org.eclipse.jdt.ui.CompilationUnitEditor"; //$NON-NLS-1$
	
	/**
	 * The editor part id of the editor that presents Java binary class files
	 * (value <code>"org.eclipse.jdt.ui.ClassFileEditor"</code>).
	 */
	public static final String ID_CF_EDITOR=			"org.eclipse.jdt.ui.ClassFileEditor"; //$NON-NLS-1$
	
	/**
	 * The editor part id of the code snippet editor
	 * (value <code>"org.eclipse.jdt.ui.SnippetEditor"</code>).
	 */
	public static final String ID_SNIPPET_EDITOR= 		"org.eclipse.jdt.ui.SnippetEditor"; //$NON-NLS-1$

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
	 * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
	 * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
	 */ 
	public static final String ID_PACKAGES= 			"org.eclipse.jdt.ui.PackageExplorer"; //$NON-NLS-1$
	
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
	 * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
	 * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
	 */ 
	public static final String ID_TYPE_HIERARCHY= 		"org.eclipse.jdt.ui.TypeHierarchy"; //$NON-NLS-1$

	/** 
	 * The view part id of the source (declaration) view.
	 * (value <code>"org.eclipse.jdt.ui.SourceView"</code>).
	 *
	 * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
	 * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
	 * @since 3.0
	 */ 
	public static final String ID_SOURCE_VIEW=	"org.eclipse.jdt.ui.SourceView"; //$NON-NLS-1$
	
	/** 
	 * The view part id of the Javadoc view.
	 * (value <code>"org.eclipse.jdt.ui.JavadocView"</code>).
	 *
	 * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
	 * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
	 * @since 3.0
	 */ 
	public static final String ID_JAVADOC_VIEW=	"org.eclipse.jdt.ui.JavadocView"; //$NON-NLS-1$
	
	/**
	 * The id of the Java Browsing Perspective
	 * (value <code>"org.eclipse.jdt.ui.JavaBrowsingPerspective"</code>).
	 * 
	 * @since 2.0
	 */
	public static String ID_BROWSING_PERSPECTIVE= "org.eclipse.jdt.ui.JavaBrowsingPerspective"; //$NON-NLS-1$

	/**
	 * The view part id of the Java Browsing Projects view
	 * (value <code>"org.eclipse.jdt.ui.ProjectsView"</code>).
	 * 
	 * @since 2.0
	 */
	public static String ID_PROJECTS_VIEW= "org.eclipse.jdt.ui.ProjectsView"; //$NON-NLS-1$

	/**
	 * The view part id of the Java Browsing Packages view
	 * (value <code>"org.eclipse.jdt.ui.PackagesView"</code>).
	 * 
	 * @since 2.0
	 */
	public static String ID_PACKAGES_VIEW= "org.eclipse.jdt.ui.PackagesView"; //$NON-NLS-1$

	/**
	 * The view part id of the Java Browsing Types view
	 * (value <code>"org.eclipse.jdt.ui.TypesView"</code>).
	 * 
	 * @since 2.0
	 */
	public static String ID_TYPES_VIEW= "org.eclipse.jdt.ui.TypesView"; //$NON-NLS-1$

	/**
	 * The view part id of the Java Browsing Members view
	 * (value <code>"org.eclipse.jdt.ui.MembersView"</code>).
	 * 
	 * @since 2.0
	 */
	public static String ID_MEMBERS_VIEW= "org.eclipse.jdt.ui.MembersView"; //$NON-NLS-1$

	/**
	 * The class org.eclipse.debug.core.model.IProcess allows attaching
	 * String properties to processes. The Java UI contributes a property
	 * page for IProcess that will show the contents of the property
	 * with this key.
	 * The intent of this property is to show the command line a process
	 * was launched with.
	 * @deprecated
	 */
	public final static String ATTR_CMDLINE= "org.eclipse.jdt.ui.launcher.cmdLine"; //$NON-NLS-1$

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
	 * and subsequently extracting the selected package (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param project the Java project
	 * @param style flags defining the style of the dialog; the valid flags are:
	 *   <code>IJavaElementSearchConstants.CONSIDER_BINARIES</code>, indicating that 
	 *   packages from binary package fragment roots should be included in addition
	 *   to those from source package fragment roots;
	 *   <code>IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS</code>, indicating that
	 *   packages from required projects should be included as well.
	 * @param filter the initial pattern to filter the set of packages. For example "com" shows 
	 * all packages starting with "com". The meta character '?' representing any character and 
	 * '*' representing any string are supported. Clients can pass an empty string if no filtering 
	 * is required.
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 * 
	 * @since 2.0
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IJavaProject project, int style, String filter) throws JavaModelException {
		Assert.isTrue((style | IJavaElementSearchConstants.CONSIDER_BINARIES | IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS) ==
			(IJavaElementSearchConstants.CONSIDER_BINARIES | IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS));

		IPackageFragmentRoot[] roots= null;
		if ((style & IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS) != 0) {
		    roots= project.getAllPackageFragmentRoots();
		} else {	
			roots= project.getPackageFragmentRoots();	
		}
		
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
			flags= flags | JavaElementLabelProvider.SHOW_ROOT;

		List packages= new ArrayList();
		Iterator iter= consideredRoots.iterator();
		while(iter.hasNext()) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)iter.next();
			packages.addAll(Arrays.asList(root.getChildren()));
		}			
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(parent, new JavaElementLabelProvider(flags));
		dialog.setIgnoreCase(false);
		dialog.setElements(packages.toArray()); // XXX inefficient
		dialog.setFilter(filter);
		return dialog;
	}

	/**
	 * Creates a selection dialog that lists all packages of the given Java project.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected package (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param project the Java project
	 * @param style flags defining the style of the dialog; the valid flags are:
	 *   <code>IJavaElementSearchConstants.CONSIDER_BINARIES</code>, indicating that 
	 *   packages from binary package fragment roots should be included in addition
	 *   to those from source package fragment roots;
	 *   <code>IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS</code>, indicating that
	 *   packages from required projects should be included as well.
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IJavaProject project, int style) throws JavaModelException {
		return createPackageDialog(parent, project, style, ""); //$NON-NLS-1$
	}
	
	/**
	 * Creates a selection dialog that lists all packages under the given package 
	 * fragment root.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected package (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param root the package fragment root
	 * @param filter the initial pattern to filter the set of packages. For example "com" shows 
	 * all packages starting with "com". The meta character '?' representing any character and 
	 * '*' representing any string are supported. Clients can pass an empty string if no filtering 
	 * is required.
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 * 
	 * @since 2.0
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IPackageFragmentRoot root, String filter) throws JavaModelException {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(parent, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
		dialog.setIgnoreCase(false);
		dialog.setElements(root.getChildren());
		dialog.setFilter(filter);
		return dialog;
	}

	/**
	 * Creates a selection dialog that lists all packages under the given package 
	 * fragment root.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected package (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param root the package fragment root
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IPackageFragmentRoot root) throws JavaModelException {
		return createPackageDialog(parent, root, ""); //$NON-NLS-1$
	}

	/**
	 * Creates a selection dialog that lists all types in the given scope.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected type(s) (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param scope the scope that limits which types are included
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaElementSearchConstants.CONSIDER_CLASSES</code>,
	 *   <code>CONSIDER_INTERFACES</code>, or their bitwise OR 
	 *   (equivalent to <code>CONSIDER_TYPES</code>)
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @param filter the initial pattern to filter the set of types. For example "Abstract" shows 
	 * all types starting with "abstract". The meta character '?' representing any character and 
	 * '*' representing any string are supported. Clients can pass an empty string if no filtering 
	 * is required.
	 * @exception JavaModelException if the selection dialog could not be opened
	 * 
	 * @since 2.0
	 */
	public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean multipleSelection, String filter) throws JavaModelException {
		int elementKinds= 0;
		if (style == IJavaElementSearchConstants.CONSIDER_TYPES) {
			elementKinds= IJavaSearchConstants.TYPE;
		} else if (style == IJavaElementSearchConstants.CONSIDER_INTERFACES) {
			elementKinds= IJavaSearchConstants.INTERFACE;
		} else if (style == IJavaElementSearchConstants.CONSIDER_CLASSES) {
			elementKinds= IJavaSearchConstants.CLASS;
		} else {
			Assert.isTrue(false, "illegal style"); //$NON-NLS-1$
		}
		if (multipleSelection) {
			MultiTypeSelectionDialog dialog= new MultiTypeSelectionDialog(parent, context, elementKinds, scope);
			dialog.setMessage(JavaUIMessages.getString("JavaUI.defaultDialogMessage")); //$NON-NLS-1$
			dialog.setFilter(filter);
			return dialog;			
		} else {
			TypeSelectionDialog dialog= new TypeSelectionDialog(parent, context, elementKinds, scope);
			dialog.setMessage(JavaUIMessages.getString("JavaUI.defaultDialogMessage")); //$NON-NLS-1$
			dialog.setFilter(filter);
			return dialog;
		}
	}

	/**
	 * Creates a selection dialog that lists all types in the given scope.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected type(s) (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param scope the scope that limits which types are included
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaElementSearchConstants.CONSIDER_CLASSES</code>,
	 *   <code>CONSIDER_INTERFACES</code>, or their bitwise OR 
	 *   (equivalent to <code>CONSIDER_TYPES</code>)
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean multipleSelection) throws JavaModelException {
		return createTypeDialog(parent, context, scope, style, multipleSelection, "");//$NON-NLS-1$
	}
		
	/**
	 * Creates a selection dialog that lists all types in the given scope containing 
	 * a standard <code>main</code> method.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected type(s) (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param scope the scope that limits which types are included
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaElementSearchConstants.CONSIDER_BINARIES</code>,
	 *   <code>CONSIDER_EXTERNAL_JARS</code>, or their bitwise OR, or <code>0</code>
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @param filter the initial pattern to filter the set of types containg a main method. For 
	 * example "App" shows all types starting with "app". The meta character '?' representing 
	 * any character and '*' representing any string are supported. Clients can pass an empty 
	 * string if no filtering is required.
	 * @return a new selection dialog
	 * 
	 * @since 2.0
	 */
	public static SelectionDialog createMainTypeDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean multipleSelection, String filter) {
		if (multipleSelection) {
			MultiMainTypeSelectionDialog dialog= new MultiMainTypeSelectionDialog(parent, context, scope, style);
			dialog.setFilter(filter);
			return dialog;
		} else {
			MainTypeSelectionDialog dialog= new MainTypeSelectionDialog(parent, context, scope, style);
			dialog.setFilter(filter);
			return dialog;
		}		
	}

	/**
	 * Creates a selection dialog that lists all types in the given scope containing 
	 * a standard <code>main</code> method.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected type(s) (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param scope the scope that limits which types are included
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaElementSearchConstants.CONSIDER_BINARIES</code>,
	 *   <code>CONSIDER_EXTERNAL_JARS</code>, or their bitwise OR, or <code>0</code>
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @return a new selection dialog
	 */
	public static SelectionDialog createMainTypeDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean multipleSelection) {
		return createMainTypeDialog(parent, context, scope, style, multipleSelection, "");//$NON-NLS-1$
	}
	
	/**
	 * Creates a selection dialog that lists all types in the given project.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected type(s) (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param project the Java project
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaElementSearchConstants.CONSIDER_CLASSES</code>,
	 *   <code>CONSIDER_INTERFACES</code>, or their bitwise OR 
	 *   (equivalent to <code>CONSIDER_TYPES</code>)
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @return a new selection dialog
	 * @exception JavaModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context, IProject project, int style, boolean multipleSelection) throws JavaModelException {
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaProject[] { JavaCore.create(project) });
		return createTypeDialog(parent, context, scope, style, multipleSelection);
	}
	
	/**
	 * Opens a Java editor on the given Java element. The element can be a compilation unit 
	 * or class file. If there already is an open Java editor for the given element, it is returned.
	 *
	 * @param element the input element; either a compilation unit 
	 *   (<code>ICompilationUnit</code>) or a class file (</code>IClassFile</code>)
	 * @return the editor, or </code>null</code> if wrong element type or opening failed
	 * @exception PartInitException if the editor could not be initialized
	 * @exception JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its underlying resource
	 */
	public static IEditorPart openInEditor(IJavaElement element) throws JavaModelException, PartInitException {
		return EditorUtility.openInEditor(element);
	}

	/** 
	 * Reveals the source range of the given source reference element in the
	 * given editor. No checking is done if the editor displays a compilation unit or
	 * class file that contains the given source reference. The editor simply reveals
	 * the source range denoted by the given source reference.
	 *
	 * @param part the editor displaying the compilation unit or class file
	 * @param element the source reference element defining the source range to be revealed
	 * 
	 * @deprecated use <code>revealInEditor(IEditorPart, IJavaElement)</code> instead
	 */	
	public static void revealInEditor(IEditorPart part, ISourceReference element) {
		if (element instanceof IJavaElement)
			revealInEditor(part, (IJavaElement) element);
	}
	
	/** 
	 * Reveals the given java element  in the given editor. If the element is not an instance
	 * of <code>ISourceReference</code> this method result in a NOP. If it is a source
	 * reference no checking is done if the editor displays a compilation unit or class file that 
	 * contains the source reference element. The editor simply reveals the source range 
	 * denoted by the given element.
	 * 
	 * @param part the editor displaying a compilation unit or class file
	 * @param element the element to be revealed
	 * 
	 * @since 2.0
	 */
	public static void revealInEditor(IEditorPart part, IJavaElement element) {
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
	
	/**
	 * Answers the shared working copies currently registered for the Java plug-in.
	 * Note that the returned array can include working copies that are
	 * not on the class path of a Java project.
	 * 
	 * @return the list of shared working copies
	 * 
	 * @see org.eclipse.jdt.core.JavaCore#getSharedWorkingCopies(org.eclipse.jdt.core.IBufferFactory)
	 * @since 2.0
	 * @deprecated Use {@link JavaCore#getWorkingCopies(org.eclipse.jdt.core.WorkingCopyOwner)} instead with <code>null</code> as
	 * argument for owner.
	 */
	public static IWorkingCopy[] getSharedWorkingCopies() {
		return JavaCore.getSharedWorkingCopies(getBufferFactory());
	}
	
	/**
	 * Answers the shared working copies that are on the class path of a Java
	 * project currently registered for the Java plug-in.
	 * 
	 * 
	 * @return the list of shared working copies
	 * 
	 * @see #getSharedWorkingCopies()
	 * @since 2.1
	 * @deprecated Use {@link JavaCore#getWorkingCopies(org.eclipse.jdt.core.WorkingCopyOwner)} instead and filter the list
	 * with {@link IJavaProject#isOnClasspath(IJavaElement)}.
	 */
	public static IWorkingCopy[] getSharedWorkingCopiesOnClasspath() {
		IWorkingCopy[] wcs= getSharedWorkingCopies();
		List result= new ArrayList(wcs.length);
		for (int i = 0; i < wcs.length; i++) {
			IWorkingCopy wc= wcs[i];
			if (wc instanceof IJavaElement) {
				IJavaElement je= (IJavaElement)wc;
				if (je.getJavaProject().isOnClasspath(je)) {
					result.add(wc);
				}
			}
		}
		return (IWorkingCopy[])result.toArray(new IWorkingCopy[result.size()]);
	}
	
	/**
	 * Returns the BufferFactory for the Java UI plug-in.
	 *
	 * @return the BufferFactory for the Java UI plug-in
	 * 
	 * @see org.eclipse.jdt.core.IBufferFactory
	 * @since 2.0
	 * @deprecated {@link IBufferFactory} has been replaced by {@link org.eclipse.jdt.core.WorkingCopyOwner}.
	 * The Java UI plug-in uses the <i>primary working copy owner</i> that can be accessed with <code>null</code> in
	 * API's that require an owner
	 */
	public static IBufferFactory getBufferFactory() {
		return JavaPlugin.getDefault().getBufferFactory();
	}

	/**
	 * Returns the DocumentProvider used for Java compilation units.
	 *
	 * @return the DocumentProvider for Java compilation units.
	 * 
	 * @see IDocumentProvider
	 * @since 2.0
	 */
	public static IDocumentProvider getDocumentProvider() {
		return JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
	}
		
	/**
	 * Sets the Javadoc location for an archive with the given path.
	 * 
	 * @param archivePath the path of the library; this can be an workspace path
	 * or an external path in case of an external library.
	 * @param url The Javadoc location to set. This location should contain index.html and
	 * a file 'package-list'. <code>null</code> clears the current documentation
	 * location.
	 * 
	 * @since 2.0
	 */
	public static void setLibraryJavadocLocation(IPath archivePath, URL url) {
		JavaDocLocations.setLibraryJavadocLocation(archivePath, url);
	}
	
	/**
	 * Sets the Javadoc locations for archives with the given paths. 
	 * 
	 * @param archivePaths The paths of the libraries; this can be an workspace path
	 * or an external path in case of an external library.
	 * @param urls The Javadoc locations to set corresponsinmg to the archive path of the same index. These locations should contain index.html and
	 * a file 'package-list'. <code>null</code> is a valid location entry and clears the current documentation
	 * location. The length of the arry must be equals to the number of archive paths passed.
	 * 
	 * @since 3.0
	 */
	public static void setLibraryJavadocLocations(IPath[] archivePaths, URL[] urls) {
		Assert.isTrue(archivePaths != null && urls != null && urls.length == archivePaths.length);
		JavaDocLocations.setLibraryJavadocLocations(archivePaths, urls);
	}

	/**
	 * Returns the Javadoc location for an archive or <code>null</code> if no
	 * location is available.
	 * 
	 * @param archivePath the path of the library. This can be an workspace path
	 * or an external path in case of an external library.
	 * 
	 * @since 2.0
	 */	
	public static URL getLibraryJavadocLocation(IPath archivePath) {
		return JavaDocLocations.getLibraryJavadocLocation(archivePath);
	}
	
	/**
	 * Sets the Javadoc location for a Java project. This location is used for
	 * all types located in the project's source folders.
	 * 
	 * @param project the project
	 * @param url The Javadoc location to set. This location should contain index.html and
	 * a file 'package-list'. <code>null</code> clears the current documentation
	 * location.
	 * 
	 * @since 2.1
	 */
	public static void setProjectJavadocLocation(IJavaProject project, URL url) {
		JavaDocLocations.setProjectJavadocLocation(project, url);
	}

	/**
	 * Returns the Javadoc location for a Java project or <code>null</code> if no
	 * location is available. This location is used for all types located in the project's
	 * source folders.
	 * 
	 * @param project the project
	 * 
	 * @since 2.1
	 */	
	public static URL getProjectJavadocLocation(IJavaProject project) {
		return JavaDocLocations.getProjectJavadocLocation(project);
	}	

	/**
	 * Returns the Javadoc base URL for an element. The base location contains the
	 * index file. This location doesn't have to exist. Returns
	 * <code>null</code> if no javadoc location has been attached to the
	 * element's library or project. Example of a returned URL is <i>http://www.
	 * junit. org/junit/javadoc</i>.
	 * 
	 * @param element The element for which the doc URL is requested.
	 * 
	 * @since 2.0
	 */		
	public static URL getJavadocBaseLocation(IJavaElement element) throws JavaModelException {	
		return JavaDocLocations.getJavadocBaseLocation(element);
	}
	
	/**
	 * Returns the Javadoc URL for an element. Example of a returned URL is
	 * <i>http://www.junit.org/junit/javadoc/junit/extensions/TestSetup.html</i>.
	 * This returned location doesn't have to exist. Returns <code>null</code>
	 * if no javadoc location has been attached to the element's library or
	 * project.
	 * 
	 * @param element The element for which the doc URL is requested.
	 * @param includeAnchor If set, the URL contains an anchor for member references:
	 * <i>http://www.junit.org/junit/javadoc/junit/extensions/TestSetup.html#run(junit.framework.TestResult)</i>. Note
	 * that this involves type resolving and is a more expensive call than without anchor.
	 * 
	 * @since 2.0
	 */		
	public static URL getJavadocLocation(IJavaElement element, boolean includeAnchor) throws JavaModelException {
		return JavaDocLocations.getJavadocLocation(element, includeAnchor);
	}
}
