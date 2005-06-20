package org.eclipse.jdt.jeview.views;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.SWT;
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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;

import org.eclipse.jdt.jeview.JEViewPlugin;


public class JavaElementView extends ViewPart implements IShowInSource, IShowInTarget {
	private TreeViewer fViewer;
	private DrillDownAdapter fDrillDownAdapter;
	private Action fAction1;
	private Action fAction2;
	private Action fDoubleClickAction;

	
	private class JEViewSelectionProvider implements ISelectionProvider {
		ListenerList fListeners= new ListenerList();

		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			fListeners.add(listener);
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
			fListeners.remove(listener);
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
		fDrillDownAdapter = new DrillDownAdapter(fViewer);
		fViewer.setContentProvider(new JEViewContentProvider());
		fViewer.setLabelProvider(new JEViewLabelProvider());
		reset();
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		getSite().setSelectionProvider(new JEViewSelectionProvider());
		contributeToActionBars();
	}

	private void reset() {
		setInput(getJavaModel());
	}

	private IJavaModel getJavaModel() {
		return JavaCore.create(JEViewPlugin.getWorkspace().getRoot());
	}

	private void setInput(IJavaElement javaElement) {
		fViewer.setInput(javaElement);
		ITreeContentProvider tcp= (ITreeContentProvider) fViewer.getContentProvider();
		Object[] elements= tcp.getElements(javaElement);
		if (elements.length > 0) {
			fViewer.setSelection(new StructuredSelection(elements[0]));
			fViewer.setExpandedState(elements[0], true);
		}
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
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(fAction1);
		manager.add(new Separator());
		manager.add(fAction2);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(fAction1);
		manager.add(fAction2);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(fAction1);
		manager.add(fAction2);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		fAction1 = new Action() {
			@Override public void run() {
				reset();
			}
		};
		fAction1.setText("Reset View");
		fAction1.setToolTipText("Reset View to JavaModel");
		fAction1.setImageDescriptor(getJavaModelImageDescriptor());
		
		fAction2 = new Action() {
			@Override public void run() {
				showMessage("Action 2 executed");
			}
		};
		fAction2.setText("Action 2");
		fAction2.setToolTipText("Action 2 tooltip");
		fAction2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		fDoubleClickAction = new Action() {
			@Override public void run() {
				ISelection selection = fViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (! (obj instanceof JavaElement))
					return;
				
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
			}
		};
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
	
	private void showAndLogError(String message, CoreException e) {
		JEViewPlugin.log(message, e);
		ErrorDialog.openError(getSite().getShell(), "JavaElement View", message, e.getStatus()); //$NON-NLS-1$
	}
	
	private void showAndLogError(String message, Exception e) {
		IStatus status= new Status(IStatus.ERROR, JEViewPlugin.getPluginId(), 0, message, e);
		JEViewPlugin.log(status);
		ErrorDialog.openError(getSite().getShell(), "JavaElement View", null, status); //$NON-NLS-1$
	}
	
	private void showMessage(String message) {
		MessageDialog.openInformation(
			fViewer.getControl().getShell(),
			"JavaElement View",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
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
			if (structuredSelection.size() == 1) {
				Object first= structuredSelection.getFirstElement();
				if (first instanceof IJavaElement) {
					setInput((IJavaElement) first);
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

}