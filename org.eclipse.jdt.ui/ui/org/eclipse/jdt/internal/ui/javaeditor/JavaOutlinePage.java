package org.eclipse.jdt.internal.ui.javaeditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GenerateGroup;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringGroup;
import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;
import org.eclipse.jdt.internal.ui.util.ArrayUtility;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;


/**
 * The content outline page of the Java editor. The viewer implements a proprietary
 * update mechanism based on Java model deltas. It does not react on domain changes.
 * It is specified to show the content of ICompilationUnits and IClassFiles.
 */
class JavaOutlinePage extends Page implements IContentOutlinePage {
	
			
			
			/**
			 * The element change listener of the java outline viewer.
			 * @see IElementChangedListener
			 */
			class ElementChangedListener implements IElementChangedListener {
				
				public void elementChanged(final ElementChangedEvent e) {
					Display d= getControl().getDisplay();
					if (d != null) {
						d.asyncExec(new Runnable() {
							public void run() {
								IJavaElementDelta delta= findElement( (ICompilationUnit) fInput, e.getDelta());
								if (delta != null && fOutlineViewer != null)
									fOutlineViewer.reconcile(delta);
							}
						});
					}
				}
				
				protected IJavaElementDelta findElement(ICompilationUnit unit, IJavaElementDelta delta) {
					
					if (delta == null || unit == null)
						return null;
					
					IJavaElement element= delta.getElement();
					
					if (unit.equals(element))
						return delta;
					
					if (element.getElementType() > IJavaElement.CLASS_FILE)
						return null;
						
					IJavaElementDelta[] children= delta.getAffectedChildren();
					if (children == null || children.length == 0)
						return null;
						
					for (int i= 0; i < children.length; i++) {
						IJavaElementDelta d= findElement(unit, children[i]);
						if (d != null)
							return d;
					}
					
					return null;
				}
			};
			
			/**
			 * Content provider for the children of an ICompilationUnit or
			 * an IClassFile
			 * @see ITreeContentProvider
			 */
			class ChildrenProvider implements ITreeContentProvider {
				
				protected ElementChangedListener fListener;
				
				protected boolean matches(IJavaElement element) {
					if (element.getElementType() == IJavaElement.METHOD) {
						String name= element.getElementName();
						return (name != null && name.indexOf('<') >= 0);
					}
					return false;
				}
				
				protected IJavaElement[] filter(IJavaElement[] children) {
					boolean initializers= false;
					for (int i= 0; i < children.length; i++) {
						if (matches(children[i])) {
							initializers= true;
							break;
						}
					}
							
					if (!initializers)
						return children;
						
					Vector v= new Vector();
					for (int i= 0; i < children.length; i++) {
						if (matches(children[i]))
							continue;
						v.addElement(children[i]);
					}
					
					IJavaElement[] result= new IJavaElement[v.size()];
					v.copyInto(result);
					return result;
				}
				
				public Object[] getChildren(Object parent) {
					if (parent instanceof IParent) {
						IParent c= (IParent) parent;
						try {
							return filter(c.getChildren());
						} catch (JavaModelException x) {
							JavaPlugin.getDefault().logErrorStatus("JavaOutlinePage.ChildrenProvider.getChildren", x.getStatus());
						}
					}
					return ArrayUtility.getEmptyArray();
				}
				
				public Object[] getElements(Object parent) {
					return getChildren(parent);
				}
				
				public Object getParent(Object child) {
					if (child instanceof IJavaElement) {
						IJavaElement e= (IJavaElement) child;
						return e.getParent();
					}
					return null;
				}
				
				public boolean hasChildren(Object parent) {
					if (parent instanceof IParent) {
						IParent c= (IParent) parent;
						try {
							IJavaElement[] children= filter(c.getChildren());
							return (children != null && children.length > 0);
						} catch (JavaModelException x) {
							JavaPlugin.getDefault().logErrorStatus("JavaOutlinePage.ChildrenProvider.hasChildren", x.getStatus());
						}
					}
					return false;
				}
				
				public boolean isDeleted(Object o) {
					return false;
				}
				
				public void dispose() {
					if (fListener != null) {
						JavaCore.removeElementChangedListener(fListener);
						fListener= null;
					}
				}
				
				/**
				 * @see IContentProvider#inputChanged(Viewer, Object, Object)
				 */
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					
					boolean remove= (oldInput instanceof ICompilationUnit && fListener != null);
					boolean add= (newInput instanceof ICompilationUnit);
					
					if (remove && add)
						return;
						
					if (remove) {
						JavaCore.removeElementChangedListener(fListener);
						fListener= null;
					}
					
					if (add) {
						if (fListener == null)
							fListener= new ElementChangedListener();
						JavaCore.addElementChangedListener(fListener);
					}
				}
			};
			
			
			class JavaOutlineViewer extends TreeViewer {
				
				public JavaOutlineViewer(Tree tree) {
					super(tree);
					setAutoExpandLevel(ALL_LEVELS);
				}
				
				/**
				 * Investigates the given element change event and if affected incrementally
				 * updates the outline.
				 */
				public void reconcile(IJavaElementDelta delta) {
					Widget w= findItem(fInput);
					if (w != null) {
						update(w, delta);
					}
				}
				
				/**
				 * @see TreeViewer#internalExpandToLevel
				 */
				protected void internalExpandToLevel(Widget node, int level) {
					if (node instanceof Item) {
						Item i= (Item) node;
						if (i.getData() instanceof IJavaElement) {
							IJavaElement je= (IJavaElement) i.getData();
							if (je.getElementType() == IJavaElement.IMPORT_CONTAINER)
								return;
						}
					}
					super.internalExpandToLevel(node, level);
				}
				
				protected void reuseTreeItem(Item item, Object element) {
					
					// remove children
					Item[] c= getChildren(item);
					if (c != null && c.length > 0) {
						for (int k= 0; k < c.length; k++) {
							if (c[k].getData() != null)
								disassociate(c[k]);
							c[k].dispose();
						}
					}
					
					updateItem(item, element);
					
					internalExpandToLevel(item, ALL_LEVELS);
					
				}
				
				/**
				 * @see TreeViewer#createTreeItem
				 */
				protected void createTreeItem(Widget parent, Object element, int ix) {
					Item[] children= getChildren(parent);
					boolean expand= (parent instanceof Item && (children == null || children.length == 0));
					
					Item item= newItem(parent, SWT.NULL, ix);				
					updateItem(item, element);
					updatePlus(item, element);
					
					if (expand)
						setExpanded((Item) parent, true);
						
					internalExpandToLevel(item, ALL_LEVELS);
				}
				
				protected boolean mustUpdateParent(IJavaElementDelta delta, IJavaElement element) {
					if (element instanceof IMethod) {
						if ((delta.getKind() & IJavaElementDelta.ADDED) != 0) {
							return JavaModelUtility.isMainMethod((IMethod)element);
						}
						return "main".equals(element.getElementName());
					}
					return false;
				}
					
				protected void update(Widget w, IJavaElementDelta delta) {
					
					Item item;
					Object element;
					
					IJavaElementDelta[] affected= delta.getAffectedChildren();
					Item[] children= getChildren(w);

					boolean doUpdateParent= false;
										
					Vector deletions= new Vector();
					go1: for (int i= 0; i < children.length; i++) {
						item= children[i];
						element= item.getData();
						for (int j= 0; j < affected.length; j++) {
							IJavaElement affectedElement= affected[j].getElement();
							if (affectedElement.equals(element)) {
								int status= affected[j].getKind();
								// removed
								if ((status & IJavaElementDelta.REMOVED) != 0) {
									deletions.addElement(item);
									doUpdateParent= doUpdateParent || mustUpdateParent(affected[j], affectedElement);
									continue go1;
								}
								// changed
								if ((status & IJavaElementDelta.CHANGED) != 0) {
									int change= affected[j].getFlags();
									doUpdateParent= doUpdateParent || mustUpdateParent(affected[j], affectedElement);
									if ((change & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_MODIFIERS)) != 0)
										updateItem(item, (Object) affected[j].getElement());
									if ((change & IJavaElementDelta.F_CHILDREN) != 0)
										update(item, affected[j]);
									continue go1;							
								}
							}
						}
					}
							
					// add at the right position
					IJavaElementDelta[] add= delta.getAddedChildren();		
					go2: for (int i= 0; i < add.length; i++) {
						
						try {
							IJavaElement e= add[i].getElement();
							ISourceReference r= (ISourceReference)e ;
							doUpdateParent= doUpdateParent || mustUpdateParent(add[i], e);
							int start= r.getSourceRange().getStartIndex();
							int end= r.getSourceRange().getEndIndex();
							
							Item last= null;
							item= null;
							children= getChildren(w);
							
							for (int j= 0; j < children.length; j++) {
								item= children[j];
								r= (ISourceReference) item.getData();
								
								if (r == null) {
									// parent node collapsed and not be opened before -> do nothing
									continue go2;
								}
									
								try {
									if (overlaps(r, start, end)) {
										
										// be tolerant if the delta is not correct, or if 
										// the tree has been updated other than by a delta
										reuseTreeItem(item, (Object) add[i].getElement());
										continue go2;
										
									} else if (r.getSourceRange().getStartIndex() > start) {
										
										if (last != null && deletions.contains(last)) {
											// reuse item
											deletions.removeElement(last);
											reuseTreeItem(last, (Object) add[i].getElement());
										} else {
											// nothing to reuse
											createTreeItem(w, (Object) add[i].getElement(), j);
										}
										continue go2;
									}
									
								} catch (JavaModelException x) {
									// stumbled over deleted element
								}
								
								last= item;
							}
						
							// add at the end of the list
							if (last != null && deletions.contains(last)) {
								// reuse item
								deletions.removeElement(last);
								reuseTreeItem(last, (Object) add[i].getElement());
							} else {
								// nothing to reuse
								createTreeItem(w, (Object) add[i].getElement(), -1);
							}
						
						} catch (JavaModelException x) {
							// the element to be added is not present -> don't add it
						}
					}
					
					
					// remove items which haven't been reused
					Enumeration e= deletions.elements();
					while (e.hasMoreElements()) {
						item= (Item) e.nextElement();
						disassociate(item);
						item.dispose();
					}
					
					if (doUpdateParent)
						updateItem(w, delta.getElement());
				}
				
				protected boolean overlaps(ISourceReference reference, int start, int end) {
					try {
						
						ISourceRange range= reference.getSourceRange();
						return start <= range.getEndIndex() && range.getStartIndex() <= end;
					
					} catch (JavaModelException x) {
						return false;
					}
				}
			};

	
	private IJavaElement fInput;
	private String fContextMenuID;
	private Menu fMenu;
	private JavaOutlineViewer fOutlineViewer;
	private JavaEditor fEditor;
	
	private ListenerList fSelectionChangedListeners= new ListenerList();
	private Hashtable fActions= new Hashtable();
	private ContextMenuGroup[] fActionGroups;
	

	public JavaOutlinePage(String contextMenuID, JavaEditor editor) {
		super();
		
		Assert.isNotNull(editor);
		
		fContextMenuID= contextMenuID;
		fEditor= editor;
	}
	

	private void fireSelectionChanged(ISelection selection) {
		SelectionChangedEvent event= new SelectionChangedEvent(this, selection);
		Object[] listeners= fSelectionChangedListeners.getListeners();
		for (int i= 0; i < listeners.length; ++i)
			((ISelectionChangedListener) listeners[i]).selectionChanged(event);
	}
		
	/**
	 * @see IPage#createControl
	 */
	public void createControl(Composite parent) {
		
		Tree tree= new Tree(parent, SWT.MULTI);
		
		fOutlineViewer= new JavaOutlineViewer(tree);		
		fOutlineViewer.setContentProvider(new ChildrenProvider());
		fOutlineViewer.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS | JavaElementLabelProvider.SHOW_TYPE));
				
		MenuManager manager= new MenuManager(fContextMenuID, fContextMenuID);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				contextMenuAboutToShow(manager);
			}
		});
		fMenu= manager.createContextMenu(tree);
		tree.setMenu(fMenu);
		
		fActionGroups= new ContextMenuGroup[] { new GenerateGroup(), new JavaSearchGroup() };
					
		fOutlineViewer.setInput(fInput);	
		fOutlineViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				fireSelectionChanged(e.getSelection());
			}
		});
	}
	
	public void dispose() {
		
		if (fEditor == null)
			return;
			
		fEditor.outlinePageClosed();
		fEditor= null;
		
		Object[] listeners= fSelectionChangedListeners.getListeners();
		for (int i= 0; i < listeners.length; i++)
			fSelectionChangedListeners.remove(listeners[i]);
		fSelectionChangedListeners= null;
		
		if (fMenu != null && !fMenu.isDisposed()) {
			fMenu.dispose();
			fMenu= null;
		}
		
		super.dispose();
	}
	
	public Control getControl() {
		if (fOutlineViewer != null)
			return fOutlineViewer.getControl();
		return null;
	}
	
	public void setInput(IJavaElement inputElement) {
		fInput= inputElement;		
		if (fOutlineViewer != null)
			fOutlineViewer.setInput(fInput);
	}
		
	public void select(ISourceReference reference) {
		if (fOutlineViewer != null) {
			
			ISelection s= StructuredSelection.EMPTY;
			if (reference != null)
				s= new StructuredSelection(reference);
				
			fOutlineViewer.setSelection(s, true);
		}
	}
	
	public void setAction(String actionID, IAction action) {
		Assert.isNotNull(actionID);
		if (action == null)
			fActions.remove(actionID);
		else
			fActions.put(actionID, action);
	}
	
	public IAction getAction(String actionID) {
		Assert.isNotNull(actionID);
		return (IAction) fActions.get(actionID);
	}
	
	/**
	 * Convenience method to add the action installed under the given actionID to the
	 * specified group of the menu.
	 */
	 protected void addAction(IMenuManager menu, String group, String actionID) {
	 	IAction action= getAction(actionID);
	 	if (action != null) {
	 		if (action instanceof IUpdate)
	 			((IUpdate) action).update();
	 			
	 		if (action.isEnabled()) {
		 		IMenuManager subMenu= menu.findMenuUsingPath(group);
		 		if (subMenu != null)
		 			subMenu.add(action);
		 		else
		 			menu.appendToGroup(group, action);
	 		}
	 	}
	 }
	 
	private void addRefactoring(IMenuManager menu){
		MenuManager refactoring= new MenuManager(RefactoringResources.getResourceString("Refactoring.submenu"));
		ContextMenuGroup.add(refactoring, new ContextMenuGroup[] { new RefactoringGroup() }, fOutlineViewer);
		if (!refactoring.isEmpty())
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, refactoring);
	}
	
	protected void contextMenuAboutToShow(IMenuManager menu) {
		
		JavaPlugin.createStandardGroups(menu);

		if (OrganizeImportsAction.canActionBeAdded(getSelection())) {
			addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "OrganizeImports");
		}
				
		addAction(menu, IContextMenuConstants.GROUP_OPEN, "OpenImportDeclaration");
		addAction(menu, IContextMenuConstants.GROUP_SHOW, "ShowInPackageView");
		addAction(menu, IContextMenuConstants.GROUP_SHOW, "ShowTypeHierarchy");
		addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "DeleteElement");
		addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "ReplaceWithEdition");
		addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "AddEdition");
		addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "AddMethodEntryBreakpoint");
		
		ContextMenuGroup.add(menu, fActionGroups, fOutlineViewer);
		addRefactoring(menu);	
	}
	
	/**
	 * @see Page#setFocus()
	 */
	public void setFocus() {
		if (fOutlineViewer != null)
			fOutlineViewer.getControl().setFocus();
	}
	
	/**
	 * @see ISelectionProvider#addSelectionChangedListener(ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.add(listener);
	}
	
	/**
	 * @see ISelectionProvider#removeSelectionChangedListener(ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.remove(listener);
	}
	
	/**
	 * @see ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		if (fOutlineViewer == null)
			return StructuredSelection.EMPTY;
		return fOutlineViewer.getSelection();
	}
	
	/**
	 * @see ISelectionProvider#setSelection(ISelection)
	 */
	public void setSelection(ISelection selection) {
		if (fOutlineViewer != null)
			fOutlineViewer.setSelection(selection);		
	}
}