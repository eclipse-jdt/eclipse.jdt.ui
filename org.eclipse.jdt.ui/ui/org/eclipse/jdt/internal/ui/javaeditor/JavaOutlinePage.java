package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.Enumeration;import java.util.Hashtable;import java.util.MissingResourceException;import java.util.ResourceBundle;import java.util.Vector;import org.eclipse.swt.SWT;import org.eclipse.swt.events.KeyAdapter;import org.eclipse.swt.events.KeyEvent;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Item;import org.eclipse.swt.widgets.Menu;import org.eclipse.swt.widgets.Tree;import org.eclipse.swt.widgets.Widget;import org.eclipse.jface.action.IAction;import org.eclipse.jface.action.IMenuListener;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.action.IStatusLineManager;import org.eclipse.jface.action.IToolBarManager;import org.eclipse.jface.action.MenuManager;import org.eclipse.jface.util.Assert;import org.eclipse.jface.util.ListenerList;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.ITreeContentProvider;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TreeViewer;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.viewers.ViewerFilter;import org.eclipse.jface.viewers.ViewerSorter;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.part.Page;import org.eclipse.ui.texteditor.IUpdate;import org.eclipse.ui.views.contentoutline.IContentOutlinePage;import org.eclipse.jdt.core.ElementChangedEvent;import org.eclipse.jdt.core.Flags;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IElementChangedListener;import org.eclipse.jdt.core.IField;import org.eclipse.jdt.core.IInitializer;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaElementDelta;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.IParent;import org.eclipse.jdt.core.ISourceRange;import org.eclipse.jdt.core.ISourceReference;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;import org.eclipse.jdt.internal.ui.actions.GenerateGroup;import org.eclipse.jdt.internal.ui.actions.JavaUIAction;import org.eclipse.jdt.internal.ui.actions.OpenHierarchyPerspectiveItem;import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringGroup;import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;import org.eclipse.jdt.internal.ui.util.ArrayUtility;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyHelper;import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;


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
								if (delta != null && fOutlineViewer != null) {
									fOutlineViewer.reconcile(delta);
								}
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
					if (getSorter() == null) {
						Widget w= findItem(fInput);
						if (w != null)
							update(w, delta);
					} else {
						// just for now
						refresh();
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
							
							if (isInnerType(je))
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
							ISourceRange rng= r.getSourceRange();
							int start= rng.getOffset();
							int end= start + rng.getLength() - 1;
							
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
										
									} else if (r.getSourceRange().getOffset() > start) {
										
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
						
						ISourceRange rng= reference.getSourceRange();
						return start <= (rng.getOffset() + rng.getLength() - 1) && rng.getOffset() <= end;
					
					} catch (JavaModelException x) {
						return false;
					}
				}
			};
			
			class LexicalSorter extends ViewerSorter {
				
				private static final int INNER_TYPES=		0;
				private static final int CONSTRUCTORS=		1;
				private static final int STATIC_INIT=		2;
				private static final int STATIC_METHODS=	3;
				private static final int INIT=				4;
				private static final int METHODS=			5;
				private static final int STATIC_FIELDS=		6;
				private static final int FIELDS=			7;
				private static final int OTHERS=			8;
				
				
				public boolean isSorterProperty(Object element, Object property) {
					return true;
				}
				
				public int category(Object element) {
					
					try {
						
						IJavaElement je= (IJavaElement) element;
						
						switch (je.getElementType()) {
							
							case IJavaElement.METHOD: {
								
								IMethod method= (IMethod) je;
								if (method.isConstructor())
									return CONSTRUCTORS;
									
								int flags= method.getFlags();
								return Flags.isStatic(flags) ? STATIC_METHODS : METHODS;
							}
							
							case IJavaElement.FIELD: {
								int flags= ((IField) je).getFlags();
								return Flags.isStatic(flags) ? STATIC_FIELDS : FIELDS;
							}
							
							case IJavaElement.INITIALIZER: {
								int flags= ((IInitializer) je).getFlags();
								return Flags.isStatic(flags) ? STATIC_INIT : INIT;
							}
						}
						
						if (isInnerType(je))
							return INNER_TYPES;
					
					} catch (JavaModelException x) {
					}
					
					return OTHERS;
				}
				
				public int compare(Viewer viewer, Object e1, Object e2) {
					// assumes that both elements are at the same structural level
					IJavaElement je= (IJavaElement) e1;
					if (je.getParent().getElementType() == IJavaElement.TYPE)
						return super.compare(viewer, e1, e2);
					
					// don't sort stuff which isn't inside a type
					return 0;
				}
			};
			
			class LexicalSortingAction extends JavaUIAction {
				
				private static final String IS_CHECKED_KEY= "isChecked";
				private static final String TOOLTIP_CHECKED_KEY= "tooltip.checked";
				private static final String TOOLTIP_UNCHECKED_KEY= "tooltip.unchecked";
				
				private LexicalSorter fSorter= new LexicalSorter();
				private String fPrefix;
				private ResourceBundle fBundle;
				
				public LexicalSortingAction(ResourceBundle bundle, String prefix) {
					super(bundle, prefix);
					
					fBundle= bundle;
					fPrefix= prefix;
					
					boolean checked= JavaPlugin.getDefault().getPreferenceStore().getBoolean(fPrefix + IS_CHECKED_KEY);
					valueChanged(checked, false);
				}
				
				public void run() {
					valueChanged(isChecked(), true);
				}
				
				private void valueChanged(boolean on, boolean store) {
					setChecked(on);
					fOutlineViewer.setSorter(on ? fSorter : null);
					
					try {
						String key= fPrefix + (on ? TOOLTIP_CHECKED_KEY : TOOLTIP_UNCHECKED_KEY);
						setToolTipText(fBundle.getString(key));
					} catch (MissingResourceException x) {
					}
					
					if (store)
						JavaPlugin.getDefault().getPreferenceStore().setValue(fPrefix + IS_CHECKED_KEY, on);
				}
			};
			
			class FieldFilter extends ViewerFilter {
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					return !(element instanceof IField);
				}
			}; 
			
			class VisibilityFilter extends ViewerFilter {
				
				public final static int PUBLIC=		0;
				public final static int PROTECTED=	1;
				public final static int PRIVATE=	2;
				public final static int DEFAULT=	3;
				public final static int NOT_STATIC=	4;
				
				private int fVisibility;
				
				public VisibilityFilter(int visibility) {
					fVisibility= visibility;
				}
				
				/* 
				 * 1GEWBY4: ITPJUI:ALL - filtering incorrect on interfaces.
				 */
				private boolean belongsToInterface(IMember member) {
					
					IType type= member.getDeclaringType();
					
					if (type != null) {
						try {
							return type.isInterface();
						} catch (JavaModelException x) {
							// ignore
						}
					}
					
					return false;
				}
				
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					
					if ( !(element instanceof IMember))
						return true;
					
					if (element instanceof IType) {
						IType type= (IType) element;
						IJavaElement parent= type.getParent();
						if (parent == null)
							return true;
						int elementType= parent.getElementType();
						if (elementType == IJavaElement.COMPILATION_UNIT || elementType == IJavaElement.CLASS_FILE)
							return true;
					}
						
					IMember member= (IMember) element;
					try {
						
						int flags= member.getFlags();
						switch (fVisibility) {
							case PUBLIC:
							    /* 1GEWBY4: ITPJUI:ALL - filtering incorrect on interfaces */
								return Flags.isPublic(flags) || belongsToInterface(member);
							case PROTECTED:
								return Flags.isProtected(flags);
							case PRIVATE:
								return Flags.isPrivate(flags);
							case DEFAULT: {
								/* 1GEWBY4: ITPJUI:ALL - filtering incorrect on interfaces */
								boolean dflt= !(Flags.isPublic(flags) || Flags.isProtected(flags) || Flags.isPrivate(flags));
								return dflt ? !belongsToInterface(member) : dflt;
							}
							case NOT_STATIC:
								return !Flags.isStatic(flags);
						}
					} catch (JavaModelException x) {
					}
					
					// unreachable
					return false;
				}
			}; 
			
			class FilterAction extends JavaUIAction {
				
				private static final String IS_CHECKED_KEY= "isChecked";
				private static final String TOOLTIP_CHECKED_KEY= "tooltip.checked";
				private static final String TOOLTIP_UNCHECKED_KEY= "tooltip.unchecked";
				
				private ViewerFilter fFilter;
				private String fPrefix;
				private ResourceBundle fBundle;
				
				public FilterAction(ResourceBundle bundle, String prefix, ViewerFilter filter) {
					super(bundle, prefix);
					
					fBundle= bundle;
					fPrefix= prefix;
					
					fFilter= filter;
					
					boolean checked= JavaPlugin.getDefault().getPreferenceStore().getBoolean(fPrefix + IS_CHECKED_KEY);
					valueChanged(checked, false);
				}
				
				public void run() {
					valueChanged(isChecked(), true);
				}
				
				private void valueChanged(boolean on, boolean store) {
					setChecked(on);
					if (on) 
						fOutlineViewer.addFilter(fFilter);
					else
						fOutlineViewer.removeFilter(fFilter);
						
					try {
						String key= fPrefix + (on ? TOOLTIP_CHECKED_KEY : TOOLTIP_UNCHECKED_KEY);
						setToolTipText(fBundle.getString(key));
					} catch (MissingResourceException x) {
					}
					
					if (store)
						JavaPlugin.getDefault().getPreferenceStore().setValue(fPrefix + IS_CHECKED_KEY, on);
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
		
		fOutlineViewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				handleKeyPressed(e);
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
	
	private void addOpenPerspectiveItem(IMenuManager menu) {
		ISelection s= getSelection();
		if (s.isEmpty() || ! (s instanceof IStructuredSelection))
			return;

		IStructuredSelection selection= (IStructuredSelection)s;
		if (selection.size() != 1)
			return;
			
		Object element= selection.getFirstElement();
		if (!(element instanceof IType))
			return;
		IType[] input= {(IType)element};
		// XXX should get the workbench window form the PartSite
		IWorkbenchWindow w= JavaPlugin.getActiveWorkbenchWindow();
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, new OpenHierarchyPerspectiveItem(w, input));
	}

	protected void contextMenuAboutToShow(IMenuManager menu) {
		
		JavaPlugin.createStandardGroups(menu);

		if (OrganizeImportsAction.canActionBeAdded(getSelection())) {
			addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "OrganizeImports");
		}
				
		addAction(menu, IContextMenuConstants.GROUP_OPEN, "OpenImportDeclaration");
		addAction(menu, IContextMenuConstants.GROUP_SHOW, "ShowInPackageView");
		addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "DeleteElement");
		addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "ReplaceWithEdition");
		addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "AddEdition");
		addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "AddMethodEntryBreakpoint");
				
		ContextMenuGroup.add(menu, fActionGroups, fOutlineViewer);
		
		addRefactoring(menu);
		addOpenPerspectiveItem(menu);	
	}
	
	/**
	 * @see Page#setFocus()
	 */
	public void setFocus() {
		if (fOutlineViewer != null)
			fOutlineViewer.getControl().setFocus();
	}
	
	/**
	 * @see Page#makeContributions(IMenuManager, IToolBarManager, IStatusLineManager)
	 */
	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		
		if (statusLineManager != null) {
			StatusBarUpdater updater= new StatusBarUpdater(statusLineManager);
			addSelectionChangedListener(updater);
		}
		
		JavaUIAction action= new LexicalSortingAction(JavaPlugin.getResourceBundle(), "Outliner.SortMembers.");
		action.setImageDescriptors("lcl16", "alphab_sort_co.gif");
		toolBarManager.add(action);
		
		action= new FilterAction(JavaPlugin.getResourceBundle(), "Outliner.HideFields.", new FieldFilter());
		action.setImageDescriptors("lcl16", "fields_co.gif");
		toolBarManager.add(action);
				
		action= new FilterAction(JavaPlugin.getResourceBundle(), "Outliner.HideStaticMembers.", new VisibilityFilter(VisibilityFilter.NOT_STATIC));		
		action.setImageDescriptors("lcl16", "static_co.gif");
		toolBarManager.add(action);
		
		action= new FilterAction(JavaPlugin.getResourceBundle(), "Outliner.HideNonePublicMembers.", new VisibilityFilter(VisibilityFilter.PUBLIC));		
		action.setImageDescriptors("lcl16", "public_co.gif");
		toolBarManager.add(action);
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
	
	/**
	 * Checkes whether a given Java element is an inner type.
	 */
	private boolean isInnerType(IJavaElement element) {
		
		if (element.getElementType() == IJavaElement.TYPE) {
			IJavaElement parent= element.getParent();
			int type= parent.getElementType();
			return (type != IJavaElement.COMPILATION_UNIT && type != IJavaElement.CLASS_FILE);
		}
		
		return false;		
	}
	
	/**
 	 * Handles key events in viewer.
 	 */
	private void handleKeyPressed(KeyEvent event) {
		
		if (event.stateMask != 0)
			return;
		
		IAction action= null;
		if (event.character == SWT.DEL) 
			action= getAction("DeleteElement");
		else if (event.keyCode == SWT.F4) {
			// Special case since Open Type Hierarchy is no action.
			(new OpenTypeHierarchyHelper()).open(getSelection(), fEditor.getSite().getWorkbenchWindow());
		}
			
		if (action != null && action.isEnabled())
			action.run();
	}
}