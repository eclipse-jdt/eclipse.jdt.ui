/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.jeview.views;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;

import org.eclipse.ui.ide.IDE;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;

import org.eclipse.jdt.jeview.EditorUtility;
import org.eclipse.jdt.jeview.JEPluginImages;
import org.eclipse.jdt.jeview.JEViewPlugin;
import org.eclipse.jdt.jeview.properties.JarEntryResourceProperties;
import org.eclipse.jdt.jeview.properties.JavaElementProperties;
import org.eclipse.jdt.jeview.properties.MarkerProperties;
import org.eclipse.jdt.jeview.properties.ResourceProperties;


public class JavaElementView extends ViewPart implements IShowInSource, IShowInTarget {
	TreeViewer fViewer;
	private DrillDownAdapter fDrillDownAdapter;
	JERoot fInput;
	
	private Action fFocusAction;
	private Action fFindTypeAction;
	private Action fResetAction;
	private Action fCodeSelectAction;
	private Action fElementAtAction;
	private Action fCreateFromHandleAction;
	private Action fRefreshAction;
	TreeCopyAction fCopyAction;
	private Action fCompareAction;
	private Action fPropertiesAction;
	Action fDoubleClickAction;
	
	private PropertySheetPage fPropertySheetPage;

	
	private static class JEViewSelectionProvider implements ISelectionProvider {
		private final TreeViewer fViewer;
		ListenerList fSelectionChangedListeners= new ListenerList();

		public JEViewSelectionProvider(TreeViewer viewer) {
			fViewer= viewer;
			fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					fireSelectionChanged();
				}
			});
		}

		void fireSelectionChanged() {
			if (fSelectionChangedListeners != null) {
				SelectionChangedEvent event= new SelectionChangedEvent(this, getSelection());
				
				Object[] listeners= fSelectionChangedListeners.getListeners();
				for (int i= 0; i < listeners.length; i++) {
					ISelectionChangedListener listener= (ISelectionChangedListener) listeners[i];
					listener.selectionChanged(event);
				}
			}
		}
		
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			fSelectionChangedListeners.add(listener);
		}

		public ISelection getSelection() {
			IStructuredSelection selection= (IStructuredSelection) fViewer.getSelection();
			ArrayList<Object> externalSelection= new ArrayList<Object>();
			for (Iterator<?> iter= selection.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof JavaElement) {
					IJavaElement javaElement= ((JavaElement) element).getJavaElement();
					if (! (javaElement instanceof IJavaModel)) // various selection listeners assume getJavaProject() is non-null 
						externalSelection.add(javaElement);
				} else if (element instanceof JEResource) {
					IResource resource= ((JEResource) element).getResource();
					if (! (resource instanceof IWorkspaceRoot)) // various selection listeners assume getProject() is non-null
						externalSelection.add(resource);
				} else if (element instanceof JEAttribute) {
					externalSelection.add(((JEAttribute) element).getWrappedObject());
				}
			}
			return new StructuredSelection(externalSelection);
		}

		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			fSelectionChangedListeners.remove(listener);
		}

		public void setSelection(ISelection selection) {
			//not supported
		}
	}
	
	public JavaElementView() {
		//
	}

	@Override
	public void createPartControl(Composite parent) {
		fViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		fDrillDownAdapter = new DrillDownAdapter(fViewer) {
			@SuppressWarnings("synthetic-access")
			@Override
			protected void updateNavigationButtons() {
				super.updateNavigationButtons();
				if (fViewer.getInput() instanceof JEAttribute && ! fViewer.getInput().equals(fInput)) {
					setContentDescription(((JEAttribute) fViewer.getInput()).getLabel());
				} else {
					setContentDescription("");
				}
				
			}
		};
		fViewer.setContentProvider(new JEViewContentProvider());
		fViewer.setLabelProvider(new JEViewLabelProvider());
		reset();
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		getSite().setSelectionProvider(new JEViewSelectionProvider(fViewer));
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				fCopyAction.setEnabled(! event.getSelection().isEmpty());
			}
		});
		contributeToActionBars();
	}

	void reset() {
		setSingleInput(getJavaModel());
	}

	private IJavaModel getJavaModel() {
		return JavaCore.create(JEViewPlugin.getWorkspace().getRoot());
	}

	void setSingleInput(Object javaElementOrResource) {
		setInput(Collections.singleton(javaElementOrResource));
	}
	
	void setInput(Collection<?> javaElementsOrResources) {
		fInput= new JERoot(javaElementsOrResources);
		fViewer.setInput(fInput);
		ITreeContentProvider tcp= (ITreeContentProvider) fViewer.getContentProvider();
		Object[] elements= tcp.getElements(fInput);
		if (elements.length > 0) {
			fViewer.setSelection(new StructuredSelection(elements[0]));
			fViewer.setExpandedState(elements[0], true);
		}
		fDrillDownAdapter.reset();
	}

	void setEmptyInput() {
		setInput(Collections.emptySet());
	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				JavaElementView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, fViewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
		bars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), fRefreshAction);
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
		bars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), fPropertiesAction);
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(fCodeSelectAction);
		manager.add(fElementAtAction);
		manager.add(fCreateFromHandleAction);
		manager.add(fResetAction);
		manager.add(new Separator());
		manager.add(fRefreshAction);
	}

	void fillContextMenu(IMenuManager manager) {
		addFocusActionOrNot(manager);
		manager.add(fResetAction);
		manager.add(fRefreshAction);
		manager.add(new Separator());
		
		addFindTypeActionOrNot(manager);
		manager.add(new Separator());
		
		manager.add(fCopyAction);
		manager.add(new Separator());
		
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator());
		addCompareActionOrNot(manager);
		manager.add(fPropertiesAction);
	}

	private void addFocusActionOrNot(IMenuManager manager) {
		if (fViewer.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) fViewer.getSelection();
			if (structuredSelection.size() == 1) {
				Object first= structuredSelection.getFirstElement();
				if (first instanceof JavaElement) {
					String name= ((JavaElement) first).getJavaElement().getElementName();
					fFocusAction.setText("Fo&cus On '" + name + '\'');
					manager.add(fFocusAction);
				}
			}
		}
	}
	
	private void addFindTypeActionOrNot(IMenuManager manager) {
		if (fViewer.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) fViewer.getSelection();
			if (structuredSelection.size() == 1) {
				Object first= structuredSelection.getFirstElement();
				if (first instanceof JavaElement) {
					IJavaElement javaElement= ((JavaElement) first).getJavaElement();
					if (javaElement instanceof IJavaProject) {
						manager.add(fFindTypeAction);
					}
				}
			}
		}
	}
	
	private void addCompareActionOrNot(IMenuManager manager) {
		if (fViewer.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) fViewer.getSelection();
			if (structuredSelection.size() == 2) {
				manager.add(fCompareAction);
			}
		}
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(fCodeSelectAction);
		manager.add(fElementAtAction);
		manager.add(fResetAction);
		manager.add(fRefreshAction);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		fCodeSelectAction= new Action("Set Input from Editor (codeSelect)", JEPluginImages.IMG_SET_FOCUS_CODE_SELECT) {
			@Override public void run() {
				IEditorPart editor= getSite().getPage().getActiveEditor();
				if (editor == null) {
					setEmptyInput();
					return;
				}
				IEditorInput input= editor.getEditorInput();
				ISelectionProvider selectionProvider= editor.getSite().getSelectionProvider();
				if (input == null || selectionProvider == null) {
					setEmptyInput();
					return;
				}
				ISelection selection= selectionProvider.getSelection();
				if (! (selection instanceof ITextSelection)) {
					setEmptyInput();
					return;
				}
				IJavaElement javaElement= (IJavaElement) input.getAdapter(IJavaElement.class);
				if (javaElement == null) {
					setEmptyInput();
					return;
				}
				
				IJavaElement[] resolved;
				try {
					resolved= codeResolve(javaElement, (ITextSelection) selection);
				} catch (JavaModelException e) {
					setEmptyInput();
					return;
				}
				if (resolved.length == 0) {
					setEmptyInput();
					return;
				}
				
				setSingleInput(resolved[0]);
			}
		};
		fCodeSelectAction.setToolTipText("Set input from current editor's selection (codeSelect)");
		
		fElementAtAction= new Action("Set Input from Editor location (getElementAt)", JEPluginImages.IMG_SET_FOCUS) {
			@Override public void run() {
				IEditorPart editor= getSite().getPage().getActiveEditor();
				if (editor == null) {
					setEmptyInput();
					return;
				}
				IEditorInput input= editor.getEditorInput();
				ISelectionProvider selectionProvider= editor.getSite().getSelectionProvider();
				if (input == null || selectionProvider == null) {
					setEmptyInput();
					return;
				}
				ISelection selection= selectionProvider.getSelection();
				if (! (selection instanceof ITextSelection)) {
					setEmptyInput();
					return;
				}
				IJavaElement javaElement= (IJavaElement) input.getAdapter(IJavaElement.class);
				if (javaElement == null) {
					setEmptyInput();
					return;
				}
				
				IJavaElement resolved;
				try {
					resolved= getElementAtOffset(javaElement, (ITextSelection) selection);
				} catch (JavaModelException e) {
					setEmptyInput();
					return;
				}
				if (resolved == null) {
					setEmptyInput();
					return;
				}
				
				setSingleInput(resolved);
			}
		};
		fElementAtAction.setToolTipText("Set input from current editor's selection location (getElementAt)");
		
		fCreateFromHandleAction= new Action("Create From Handle...") {
			@Override public void run() {
				InputDialog dialog= new InputDialog(getSite().getShell(), "Create Java Element From Handle Identifier", "Handle identifier:", "", null);
				if (dialog.open() != Window.OK)
					return;
				String handleIdentifier= dialog.getValue();
				IJavaElement javaElement= JavaCore.create(handleIdentifier);
				setSingleInput(javaElement);
			}
		};
		
		fFocusAction= new Action() {
			@Override public void run() {
				Object selected= ((IStructuredSelection) fViewer.getSelection()).getFirstElement();
				setSingleInput(((JavaElement) selected).getJavaElement());
			}
		};
		fFocusAction.setToolTipText("Focus on Selection");
		
		fFindTypeAction= new Action() {
			@Override public void run() {
				Object selected= ((IStructuredSelection) fViewer.getSelection()).getFirstElement();
				final IJavaProject project= (IJavaProject) ((JavaElement) selected).getJavaElement();
				
				InputDialog dialog= new InputDialog(getSite().getShell(), "IJavaProject#findType(String fullyQualifiedName, IProgressMonitor pm)", "fullyQualifiedName:", "", null);
				if (dialog.open() != Window.OK)
					return;
				final String fullyQualifiedName= dialog.getValue();
				
				class Runner implements IRunnableWithProgress {
					IType type;
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							type= project.findType(fullyQualifiedName, monitor);
						} catch (JavaModelException e) {
							throw new InvocationTargetException(e);
						}
					}
				}
				Runner runner= new Runner();
				try {
					PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runner);
				} catch (InvocationTargetException e) {
					JEViewPlugin.log(e);
				} catch (InterruptedException e) {
					JEViewPlugin.log(e);
				}
				JavaElement element= new JavaElement(fInput, fullyQualifiedName, runner.type);
				fViewer.add(fInput, element);
				fViewer.setSelection(new StructuredSelection(element));
			}
		};
		fFindTypeAction.setText("findType(..)...");
		
		fResetAction= new Action("&Reset View", getJavaModelImageDescriptor()) {
			@Override public void run() {
				reset();
			}
		};
		fResetAction.setToolTipText("Reset View to JavaModel");
		
		fRefreshAction= new Action("Re&fresh", JEPluginImages.IMG_REFRESH) {
			@Override public void run() {
				BusyIndicator.showWhile(getSite().getShell().getDisplay(), new Runnable() {
					public void run() {
						fViewer.refresh();
					}
				});
			}
		};
		fRefreshAction.setToolTipText("Refresh");
		fRefreshAction.setActionDefinitionId("org.eclipse.ui.file.refresh");
		
		fCopyAction= new TreeCopyAction(new Tree[] {fViewer.getTree()});
		
		fPropertiesAction= new Action("&Properties", JEPluginImages.IMG_PROPERTIES) {
			@Override
			public void run() {
				String viewId = IPageLayout.ID_PROP_SHEET;
				IWorkbenchPage page= getViewSite().getPage();
				IViewPart view;
				try {
					view= page.showView(viewId);
					page.activate(JavaElementView.this);
					page.bringToTop(view);
				} catch (PartInitException e) {
					JEViewPlugin.log("could not find Properties view", e);
				}
			}
		};
		fPropertiesAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.PROPERTIES);
		
		fDoubleClickAction = new Action() {
			private Object fPreviousDouble;

			@Override public void run() {
				ISelection selection = fViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				
				boolean isSecondDoubleClick= (obj == fPreviousDouble);
				fPreviousDouble= isSecondDoubleClick ? null : obj;

				if (obj instanceof JavaElement) {
					IJavaElement javaElement= ((JavaElement) obj).getJavaElement();
					if (javaElement != null) {
						switch (javaElement.getElementType()) {
							case IJavaElement.JAVA_MODEL :
								break;
								
							case IJavaElement.JAVA_PROJECT :
							case IJavaElement.PACKAGE_FRAGMENT_ROOT :
							case IJavaElement.PACKAGE_FRAGMENT :
								ShowInPackageViewAction showInPackageViewAction= new ShowInPackageViewAction(getViewSite());
								showInPackageViewAction.run(javaElement);
								break;
								
							default :
								try {
									IEditorPart editorPart= JavaUI.openInEditor(javaElement);
									if (editorPart != null) {
										if (isSecondDoubleClick && javaElement instanceof ISourceReference && editorPart instanceof ITextEditor) {
											ISourceRange sourceRange= ((ISourceReference) javaElement).getSourceRange();
											EditorUtility.selectInEditor((ITextEditor) editorPart, sourceRange.getOffset(), sourceRange.getLength());
										} else {
											JavaUI.revealInEditor(editorPart, javaElement);
										}
									}
								} catch (PartInitException e) {
									showAndLogError("Could not open editor.", e); //$NON-NLS-1$
								} catch (JavaModelException e) {
									showAndLogError("Could not open editor.", e); //$NON-NLS-1$
								}
						}
					}
					
				} else if (obj instanceof Error) {
					Error error= (Error) obj;
					JEViewPlugin.log(error.getException());
					
				} else if (obj instanceof JEMarker) {
					JEMarker marker= (JEMarker) obj;
					try {
						IDE.openEditor(getSite().getPage(), marker.getMarker());
					} catch (PartInitException e) {
						showAndLogError("Could not open editor.", e); //$NON-NLS-1$
					}
				}
			}
		};
		
		fCompareAction= new Action() {
			@Override public void run() {
				Object[] selection= ((IStructuredSelection) fViewer.getSelection()).toArray();
				Object first= ((JEAttribute) selection[0]).getWrappedObject();
				Object second= ((JEAttribute) selection[1]).getWrappedObject();
				boolean identical= first == second;
				boolean equals1= first != null && first.equals(second);
				boolean equals2= second != null && second.equals(first);
				boolean inconsistentEquals= equals1 != equals2;
				
				String msg= "==: " + identical + "\nequals(..): " + (inconsistentEquals ? "INCONSISTENT" : equals1);
				MessageDialog.openInformation(fViewer.getTree().getShell(), "Comparison", msg);
			}
		};
		fCompareAction.setText("C&ompare with Each Other...");

	}

	
	static IJavaElement[] codeResolve(IJavaElement input, ITextSelection selection) throws JavaModelException {
		if (input instanceof ICodeAssist) {
			if (input instanceof ICompilationUnit) {
				reconcile((ICompilationUnit) input);
			}
			IJavaElement[] elements= ((ICodeAssist)input).codeSelect(selection.getOffset(), selection.getLength());
			if (elements != null && elements.length > 0)
				return elements;
		}
		return new IJavaElement[0];
	}
	
	static IJavaElement getElementAtOffset(IJavaElement input, ITextSelection selection) throws JavaModelException {
		if (input instanceof ICompilationUnit) {
			ICompilationUnit cunit= (ICompilationUnit) input;
			reconcile(cunit);
			IJavaElement ref= cunit.getElementAt(selection.getOffset());
			if (ref == null)
				return input;
			else
				return ref;
		} else if (input instanceof IClassFile) {
			IJavaElement ref= ((IClassFile)input).getElementAt(selection.getOffset());
			if (ref == null)
				return input;
			else
				return ref;
		}
		return input;
	}
	
	/* see JavaModelUtil.reconcile((ICompilationUnit) input) */
	static void reconcile(ICompilationUnit unit) throws JavaModelException {
		synchronized(unit)  {
			unit.reconcile(
				ICompilationUnit.NO_AST, 
				false /* don't force problem detection */, 
				null /* use primary owner */, 
				null /* no progress monitor */);
		}
	}
	
	private ImageDescriptor getJavaModelImageDescriptor() {
		JavaElementLabelProvider lp= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS);
		Image modelImage= lp.getImage(getJavaModel());
		ImageDescriptor modelImageDescriptor= ImageDescriptor.createFromImage(modelImage);
		lp.dispose();
		return modelImageDescriptor;
	}

	private void hookDoubleClickAction() {
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				fDoubleClickAction.run();
			}
		});
	}
	
	void showAndLogError(String message, CoreException e) {
		JEViewPlugin.log(message, e);
		ErrorDialog.openError(getSite().getShell(), "JavaElement View", message, e.getStatus()); //$NON-NLS-1$
	}
	
	void showAndLogError(String message, Exception e) {
		IStatus status= new Status(IStatus.ERROR, JEViewPlugin.getPluginId(), 0, message, e);
		JEViewPlugin.log(status);
		ErrorDialog.openError(getSite().getShell(), "JavaElement View", null, status); //$NON-NLS-1$
	}
	
	void showMessage(String message) {
		MessageDialog.openInformation(
			fViewer.getControl().getShell(),
			"JavaElement View",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		fViewer.getControl().setFocus();
	}

	public ShowInContext getShowInContext() {
		return new ShowInContext(null, getSite().getSelectionProvider().getSelection());
	}

	public boolean show(ShowInContext context) {
		ISelection selection= context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= ((IStructuredSelection) selection);
			if (structuredSelection.size() >= 1) {
				Set<Object> input= new LinkedHashSet<Object>();
				for (Iterator<?> iter = structuredSelection.iterator(); iter.hasNext();) {
					Object item= iter.next();
					if (item instanceof IJavaElement) {
						input.add(item);
					} else if (item instanceof IResource) {
						IJavaElement je= JavaCore.create((IResource) item);
						if (je == null) {
							input.add(item);
						} else {
							input.add(je);
						}
					} else if (item instanceof IJarEntryResource) {
						input.add(item);
					}
				}
				if (input.size() > 0) {
					setInput(input);
					return true;
				}
			}
		}
		
		Object input= context.getInput();
		if (input instanceof IEditorInput) {
			Object elementOfInput= getElementOfInput((IEditorInput)context.getInput());
			if (elementOfInput != null) {
				setSingleInput(elementOfInput);
				return true;
			}
		}

		return false;
	}
	
	Object getElementOfInput(IEditorInput input) {
		Object adapted= input.getAdapter(IClassFile.class);
		if (adapted != null) {
			return adapted;
		}
		
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput)input).getFile();
			IJavaElement javaElement= JavaCore.create(file);
			if (javaElement != null) {
				return javaElement;
			} else {
				return file;
			}
		} else if (input instanceof IStorageEditorInput) {
			try {
				return ((IStorageEditorInput) input).getStorage();
			} catch (CoreException e) {
				return null;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IPropertySheetPage.class) {
			return getPropertySheetPage();
		}
		return super.getAdapter(adapter);
	}

	private PropertySheetPage getPropertySheetPage() {
		if (fPropertySheetPage == null) {
			final PropertySheetPage propertySheetPage= new PropertySheetPage();
			propertySheetPage.setPropertySourceProvider(new IPropertySourceProvider() {
				public IPropertySource getPropertySource(Object object) {
					if (object instanceof IJavaElement)
						return new JavaElementProperties((IJavaElement) object);
					else if (object instanceof IResource)
						return new ResourceProperties((IResource) object);
					else if (object instanceof IMarker)
						return new MarkerProperties((IMarker) object);
					else if (object instanceof IJarEntryResource)
						return new JarEntryResourceProperties((IJarEntryResource) object);
					else
						return null;
				}
			});
			fPropertySheetPage= propertySheetPage;
		}
		return fPropertySheetPage;
	}
}