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
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.progress.IProgressService;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.internal.ui.search.SearchUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Abstract class for Java search actions.
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
 *
 * @since 2.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class FindAction extends SelectionDispatchAction {

	// A dummy which can't be selected in the UI
	private static final IJavaElement RETURN_WITHOUT_BEEP= JavaCore.create(JavaPlugin.getWorkspace().getRoot());

	private Class<?>[] fValidTypes;
	private JavaEditor fEditor;

	private int numberOfElements;
	private int processedElementIndex;


	FindAction(IWorkbenchSite site) {
		super(site);
		fValidTypes= getValidTypes();
		init();
	}

	FindAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	/**
	 * Called once by the constructors to initialize label, tooltip, image and help support of the action.
	 * To be overridden by implementors of this action.
	 */
	abstract void init();

	/**
	 * Called once by the constructors to get the list of the valid input types of the action.
	 * To be overridden by implementors of this action.
	 * @return the valid input types of the action
	 */
	abstract Class<?>[] getValidTypes();

	boolean canOperateOn(IStructuredSelection sel) {
		if (sel == null || sel.isEmpty()) {
			return false;
		}
		IJavaElement[] elements= getJavaElements(sel, true);
		for (IJavaElement iJavaElement : elements) {
			if (!canOperateOn(iJavaElement)) {
				return false;
			}
		}
		return true;
	}

	boolean canOperateOn(IJavaElement element) {
		if (element == null || fValidTypes == null || fValidTypes.length == 0 || !ActionUtil.isOnBuildPath(element))
			return false;

		for (Class<?> fValidType : fValidTypes) {
			if (fValidType.isInstance(element)) {
				if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
					return hasChildren((IPackageFragment)element);
				else
					return true;
			}
		}
		return false;
	}

	private boolean hasChildren(IPackageFragment packageFragment) {
		try {
			return packageFragment.hasChildren();
		} catch (JavaModelException ex) {
			return false;
		}
	}

	private IJavaElement getTypeIfPossible(IJavaElement o, boolean silent) {
		switch (o.getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				if (silent)
					return o;
				else
					return findType((ICompilationUnit)o, silent);
			case IJavaElement.CLASS_FILE:
				if (o instanceof IOrdinaryClassFile)
					return ((IOrdinaryClassFile)o).getType();
				return o;
			default:
				return o;
		}
	}

	IJavaElement[] getJavaElements(IStructuredSelection selection, boolean silent) {
		IJavaElement[] javaElements= new IJavaElement[selection.size()];
		int index= 0;
		for (Object firstElement : selection) {
			IJavaElement elem= null;
			if (firstElement instanceof IJavaElement)
				elem= (IJavaElement) firstElement;
			else if (firstElement instanceof IAdaptable)
				elem= ((IAdaptable) firstElement).getAdapter(IJavaElement.class);
			if (elem != null) {
				elem= getTypeIfPossible(elem, silent);
				javaElements[index++]= elem;
				if (elem == RETURN_WITHOUT_BEEP) {
					break;
				}
			}

		}
		return javaElements;
	}

	private void showOperationUnavailableDialog() {
		MessageDialog.openInformation(getShell(), SearchMessages.JavaElementAction_operationUnavailable_title, getOperationUnavailableMessage());
	}

	String getOperationUnavailableMessage() {
		return SearchMessages.JavaElementAction_operationUnavailable_generic;
	}

	private IJavaElement findType(ICompilationUnit cu, boolean silent) {
		IType[] types= null;
		try {
			types= cu.getAllTypes();
		} catch (JavaModelException ex) {
			if (JavaModelUtil.isExceptionToBeLogged(ex))
				ExceptionHandler.log(ex, SearchMessages.JavaElementAction_error_open_message);
			if (silent)
				return RETURN_WITHOUT_BEEP;
			else
				return null;
		}
		if (types.length == 1 || (silent && types.length > 0))
			return types[0];
		if (silent)
			return RETURN_WITHOUT_BEEP;
		if (types.length == 0)
			return null;
		String title= SearchMessages.JavaElementAction_typeSelectionDialog_title;
		String message = SearchMessages.JavaElementAction_typeSelectionDialog_message;
		int flags= (JavaElementLabelProvider.SHOW_DEFAULT);

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(flags));
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setElements(types);

		if (dialog.open() == Window.OK)
			return (IType)dialog.getFirstResult();
		else
			return RETURN_WITHOUT_BEEP;
	}

	/*
	 * Method declared on SelectionChangedAction.
	 */
	@Override
	public void run(IStructuredSelection selection) {
		IJavaElement[] elements= getJavaElements(selection, false);
		for (IJavaElement element : elements) {
			if (element == null || !element.exists()) {
				showOperationUnavailableDialog();
				return;
			} else if ((element == RETURN_WITHOUT_BEEP)
					|| !ActionUtil.isProcessable(getShell(), element)) {
				return;
			}
		}
		run(elements);
	}

	/*
	 * Method declared on SelectionChangedAction.
	 */
	@Override
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(fEditor))
			return;
		try {
			String title= SearchMessages.SearchElementSelectionDialog_title;
			String message= SearchMessages.SearchElementSelectionDialog_message;

			IJavaElement[] elements= SelectionConverter.codeResolveForked(fEditor, true);
			if (elements.length > 0 && canOperateOn(elements[0])) {
				IJavaElement element= elements[0];
				if (elements.length > 1)
					element= SelectionConverter.selectJavaElement(elements, getShell(), title, message);
				if (element != null)
					run(element);
			}
			else
				showOperationUnavailableDialog();
		} catch (InvocationTargetException ex) {
			String title= SearchMessages.Search_Error_search_title;
			String message= SearchMessages.Search_Error_codeResolve;
			ExceptionHandler.handle(ex, getShell(), title, message);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	/*
	 * Method declared on SelectionChangedAction.
	 */
	@Override
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}

	/*
	 * Method declared on SelectionChangedAction.
	 */
	@Override
	public void selectionChanged(ITextSelection selection) {
	}

	/**
	 * Executes this action for the given java element.
	 * @param element The java element to be found.
	 */
	public void run(IJavaElement element) {

		if (!ActionUtil.isProcessable(getShell(), element))
			return;

		run(new IJavaElement[] {element});
	}

	/**
	 * Executes this action for the given Java element List.
	 *
	 * @param elements the Java elements to be found
	 * @since 3.12
	 */
	public void run(IJavaElement[] elements) {
		// will return true except for debugging purposes.
		try {
			numberOfElements= elements.length;
			List<QuerySpecification> queryList= new ArrayList<>(numberOfElements);
			processedElementIndex= 0;
			for (IJavaElement element : elements) {
				processedElementIndex++;
				queryList.add(createQuery(element));
			}
			performNewSearch(new JavaSearchQuery(queryList));
		} catch (JavaModelException ex) {
			ExceptionHandler.handle(ex, getShell(), SearchMessages.Search_Error_search_notsuccessful_title, SearchMessages.Search_Error_search_notsuccessful_message);
		} catch (InterruptedException | IllegalArgumentException e) {
			// cancelled or no element
		}
	}

	private void performNewSearch(JavaSearchQuery query) {
		if (query.canRunInBackground()) {
			/*
			 * This indirection with Object as parameter is needed to prevent the loading
			 * of the Search plug-in: the VM verifies the method call and hence loads the
			 * types used in the method signature, eventually triggering the loading of
			 * a plug-in (in this case ISearchQuery results in Search plug-in being loaded).
			 */
			SearchUtil.runQueryInBackground(query);
		} else {
			IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
			/*
			 * This indirection with Object as parameter is needed to prevent the loading
			 * of the Search plug-in: the VM verifies the method call and hence loads the
			 * types used in the method signature, eventually triggering the loading of
			 * a plug-in (in this case it would be ISearchQuery).
			 */
			IStatus status= SearchUtil.runQueryInForeground(progressService, query);
			if (status.matches(IStatus.ERROR | IStatus.INFO | IStatus.WARNING)) {
				ErrorDialog.openError(getShell(), SearchMessages.Search_Error_search_title, SearchMessages.Search_Error_search_message, status);
			}
		}
	}

	/**
	 * Creates a query for the given element. Subclasses reimplement this method.
	 *
	 * @param element the element to create a query for
	 *
	 * @return returns the query
	 * @throws JavaModelException thrown when accessing the element failed
	 * @throws InterruptedException thrown when the user interrupted the query selection
	 */
	QuerySpecification createQuery(IJavaElement element) throws JavaModelException, InterruptedException {
		return createDefaultQuery(element);
	}

	QuerySpecification createDefaultQuery(IJavaElement element) {
		JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();
		IJavaSearchScope scope= factory.createWorkspaceScope(true);
		String description= factory.getWorkspaceScopeDescription(true);
		return new ElementQuerySpecification(element, getLimitTo(), scope, description);
	}

	/**
	 * @param element non null
	 * @param action non null
	 * @param toUpdate working sets array reference that will be updated in this method
	 * @return {@link QuerySpecification} for given object and action, that depends on action state
	 * @throws InterruptedException if working sets query fail
	 */
	static QuerySpecification createQueryWithWorkingSets(IJavaElement element, FindAction action, AtomicReference<IWorkingSet[]> toUpdate) throws InterruptedException {
		JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();

		final IWorkingSet[] workingSets;
		if (toUpdate.get() == null && action.isFirstElement()) {
			workingSets= factory.queryWorkingSets();
			if (workingSets == null) {
				return action.createDefaultQuery(element); // workspace
			}
			if (action.isMultiSelect()) {
				toUpdate.set(workingSets);
			}
		} else if (action.isMultiSelect() && action.isLastElement()) {
			workingSets = toUpdate.get();
			toUpdate.set(null);
		} else {
			workingSets = toUpdate.get();
		}
		SearchUtil.updateLRUWorkingSets(workingSets);
		String description= factory.getWorkingSetScopeDescription(workingSets, JavaSearchScopeFactory.NO_PROJ);
		return new LazyScopeQuerySpecification(element, action.getLimitTo(), () -> factory.createJavaSearchScope(workingSets, JavaSearchScopeFactory.NO_PROJ), description);
	}

	abstract int getLimitTo();

	IType getType(IJavaElement element) {
		if (element == null)
			return null;

		IType type= null;
		if (element.getElementType() == IJavaElement.TYPE)
			type= (IType)element;
		else if (element instanceof IMember)
			type= ((IMember)element).getDeclaringType();
		else if (element instanceof ILocalVariable) {
			type= (IType)element.getAncestor(IJavaElement.TYPE);
		}
		return type;
	}

	JavaEditor getEditor() {
		return fEditor;
	}

	boolean isMultiSelect() {
		return numberOfElements > 1;
	}

	boolean isLastElement() {
		return numberOfElements == processedElementIndex;
	}

	boolean isFirstElement() {
		return processedElementIndex == 1;
	}
}
