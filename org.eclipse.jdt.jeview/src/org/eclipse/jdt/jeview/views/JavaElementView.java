package org.eclipse.jdt.jeview.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.ListenerList;
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
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;

import org.eclipse.jdt.jeview.JEPluginImages;
import org.eclipse.jdt.jeview.JEViewPlugin;
import org.eclipse.jdt.jeview.properties.JavaElementProperties;
import org.eclipse.jdt.jeview.properties.ResourceProperties;


public class JavaElementView extends ViewPart implements IShowInSource, IShowInTarget {
	TreeViewer fViewer;
	private DrillDownAdapter fDrillDownAdapter;
	private JERoot fInput;
	
	private Action fResetAction;
	private Action fCodeSelectAction;
	private Action fCreateFromHandleAction;
	private Action fRefreshAction;
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
			ArrayList<IAdaptable> externalSelection= new ArrayList<IAdaptable>();
			for (Iterator iter= selection.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof JavaElement) {
					externalSelection.add(((JavaElement) element).getJavaElement());
				} else if (element instanceof JEResource) {
					externalSelection.add(((JEResource) element).getResource());
				} else {
					//TODO: support for other node types?
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
		contributeToActionBars();
	}

	void reset() {
		setInput(getJavaModel());
	}

	private IJavaModel getJavaModel() {
		return JavaCore.create(JEViewPlugin.getWorkspace().getRoot());
	}

	void setInput(IJavaElement javaElement) {
		setInput(Collections.singleton(javaElement));
	}
	
	void setInput(Collection<? extends IJavaElement> javaElements) {
		fInput= new JERoot(javaElements);
		fViewer.setInput(fInput);
		ITreeContentProvider tcp= (ITreeContentProvider) fViewer.getContentProvider();
		Object[] elements= tcp.getElements(fInput);
		if (elements.length > 0) {
			fViewer.setSelection(new StructuredSelection(elements[0]));
			fViewer.setExpandedState(elements[0], true);
		}
		fDrillDownAdapter.reset();
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
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(fCodeSelectAction);
		manager.add(fCreateFromHandleAction);
		manager.add(fResetAction);
		manager.add(new Separator());
		manager.add(fRefreshAction);
	}

	void fillContextMenu(IMenuManager manager) {
		manager.add(fResetAction);
		manager.add(fRefreshAction);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator());
		manager.add(fPropertiesAction);
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(fCodeSelectAction);
		manager.add(fResetAction);
		manager.add(fRefreshAction);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		fCodeSelectAction= new Action("Set Input from Editor", JEPluginImages.IMG_SET_FOCUS) {
			@Override public void run() {
				IEditorPart editor= getSite().getPage().getActiveEditor();
				IEditorInput input= editor.getEditorInput();
				ISelectionProvider selectionProvider= editor.getSite().getSelectionProvider();
				if (input == null || selectionProvider == null)
					return;
				ISelection selection= selectionProvider.getSelection();
				if (! (selection instanceof ITextSelection))
					return;
				IJavaElement javaElement= (IJavaElement) input.getAdapter(IJavaElement.class);
				if (javaElement == null)
					return;
				IJavaElement[] resolved;
				try {
					resolved= codeResolve(javaElement, (ITextSelection) selection);
				} catch (JavaModelException e) {
					return;
				}
				if (resolved.length == 0)
					return;
				
				setInput(resolved[0]);
			}
		};
		fCodeSelectAction.setToolTipText("Set input from current editor's selection");
		//TODO: getElementAt
		
		fCreateFromHandleAction= new Action("Create From Handle...") {
			@Override public void run() {
				InputDialog dialog= new InputDialog(getSite().getShell(), "Create Java Element From Handle Identifier", "Handle identifier:", "", null);
				if (dialog.open() != Window.OK)
					return;
				String handleIdentifier= dialog.getValue();
				IJavaElement javaElement= JavaCore.create(handleIdentifier);
				setInput(javaElement);
			}
		};
		
		fResetAction= new Action("Reset View", getJavaModelImageDescriptor()) {
			@Override public void run() {
				reset();
			}
		};
		fResetAction.setToolTipText("Reset View to JavaModel");
		
		fRefreshAction= new Action("Refresh", JEPluginImages.IMG_REFRESH) {
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
		
		fPropertiesAction= new Action("Properties", JEPluginImages.IMG_PROPERTIES) {
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
		
		fDoubleClickAction = new Action() {
			@Override public void run() {
				ISelection selection = fViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof JavaElement) {
					IJavaElement javaElement= ((JavaElement) obj).getJavaElement();
					switch (javaElement.getElementType()) {
						case IJavaElement.JAVA_MODEL :
						case IJavaElement.JAVA_PROJECT :
						case IJavaElement.PACKAGE_FRAGMENT_ROOT :
						case IJavaElement.PACKAGE_FRAGMENT :
							ShowInPackageViewAction showInPackageViewAction= new ShowInPackageViewAction(getViewSite());
							showInPackageViewAction.run(javaElement);
							break;
							
						default :
							try {
								IEditorPart editorPart= JavaUI.openInEditor(javaElement);
								if (editorPart != null)
									JavaUI.revealInEditor(editorPart, javaElement);
							} catch (PartInitException e) {
								showAndLogError("Could not open editor.", e); //$NON-NLS-1$
							} catch (JavaModelException e) {
								showAndLogError("Could not open editor.", e); //$NON-NLS-1$
							}
					}
					
				} else if (obj instanceof Error) {
					Error error= (Error) obj;
					JEViewPlugin.log(error.getException());
				}
			}
		};
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
				Set<IJavaElement> input= new LinkedHashSet<IJavaElement>();
				for (Iterator iter = structuredSelection.iterator(); iter.hasNext();) {
					Object first= iter.next();
					if (first instanceof IJavaElement) {
						input.add((IJavaElement) first);
					} else if (first instanceof IResource) {
						IJavaElement je= JavaCore.create((IResource) first);
						if (je != null) {
							input.add(je);
						}
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
			IJavaElement elementOfInput= getElementOfInput((IEditorInput)context.getInput());
			if (elementOfInput != null) {
				setInput(elementOfInput);
				return true;
			}
		}

		return false;
	}
	
	IJavaElement getElementOfInput(IEditorInput input) {
		Object adapted= input.getAdapter(IClassFile.class);
		if (adapted != null)
			return (IClassFile) adapted;
		
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput)input).getFile();
			IJavaElement javaElement= JavaCore.create(file);
			if (javaElement != null)
				return javaElement;
		}
		
//		if (input instanceof JarEntryEditorInput)
//			return ((JarEntryEditorInput)input).getStorage();
		return null;
	}
	
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
					else
						return null;
				}
			});
			fPropertySheetPage= propertySheetPage;
		}
		return fPropertySheetPage;
	}
}