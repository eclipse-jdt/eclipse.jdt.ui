/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *   Martin Moebius
 * *****************************************************************************/

package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation.Methods2Field;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;

/**
 * Creates delegate methods for a type's fields. Opens a dialog with a list of
 * fields for which delegate methods can be generated. User is able to check or
 * uncheck items before methods are generated.
 * <p>
 * Will open the parent compilation unit in a Java editor. The result is 
 * unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>IField</code> or <code>IType</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * Contributors:
 *   Martin Moebius: m.moebius@gmx.de - bug: 28793 
 * @since 2.1
 */
public class AddDelegateMethodsAction extends SelectionDispatchAction {
	private static final String DIALOG_TITLE = ActionMessages.getString("AddDelegateMethodsAction.error.title"); //$NON-NLS-1$
	private CompilationUnitEditor fEditor;	

	/**
	 * Creates a new <code>AddDelegateMethodsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddDelegateMethodsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("AddDelegateMethodsAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("AddDelegateMethodsAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddDelegateMethodsAction.tooltip")); //$NON-NLS-1$

		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_DELEGATE_METHODS_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public AddDelegateMethodsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor = editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(editor) != null);
	}

	//---- Structured Viewer -----------------------------------------------------------

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}
	
	private boolean canEnable(IStructuredSelection selection) throws JavaModelException {
		if (getSelectedFields(selection) != null)
			return true;

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			IType type= (IType) selection.getFirstElement();
			//	look if class: not cheap but done by all source generation actions
			// disable locals until create method is supported by jdt.core (bug 44395)
			return type.getCompilationUnit() != null && type.isClass() && !type.isLocal(); 
		}

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return true;

		return false;
	}	

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		try {
			IField[] selectedFields = getSelectedFields(selection);
			if (canRunOn(selectedFields)) {
				run(selectedFields[0].getDeclaringType(), selectedFields, false);
				return;
			}
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IType)
				run((IType) firstElement, new IField[0], false);
			else if (firstElement instanceof ICompilationUnit)
				run(JavaElementUtil.getMainType((ICompilationUnit) firstElement), new IField[0], false);
			else
				MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.not_applicable")); //$NON-NLS-1$
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.error.actionfailed")); //$NON-NLS-1$
		}

	}

	private static boolean canRunOn(IType type) throws JavaModelException {
		if (type == null || type.getCompilationUnit() == null || type.isInterface())
			return false;

		return canRunOn(type.getFields());
	}

	private static boolean canRunOn(IField[] fields) throws JavaModelException {
		if (fields == null) {
			return false;
		}
		int count = 0;
		for (int i = 0; i < fields.length; i++) {
			if (!hasPrimitiveType(fields[i]) || isArray(fields[i])) {
				count++;
			}
		}
		return (count > 0);
	}

	/*
	 * Returns fields in the selection or <code>null</code> if the selection is 
	 * empty or not valid.
	 */
	private IField[] getSelectedFields(IStructuredSelection selection) {
		List elements = selection.toList();
		int nElements = elements.size();
		if (nElements > 0) {
			IField[] res = new IField[nElements];
			ICompilationUnit cu = null;
			for (int i = 0; i < nElements; i++) {
				Object curr = elements.get(i);
				if (curr instanceof IField) {
					IField fld = (IField) curr;

					if (i == 0) {
						// remember the CU of the first element
						cu = fld.getCompilationUnit();
						if (cu == null) {
							return null;
						}
					} else if (!cu.equals(fld.getCompilationUnit())) {
						// all fields must be in the same CU
						return null;
					}
					try {
						if (fld.getDeclaringType().isInterface()) {
							// no delegates for fields in interfaces or fields with 
							return null;
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
						return null;
					}

					res[i] = fld;
				} else {
					return null;
				}
			}
			return res;
		}
		return null;
	}

	private void run(IType type, IField[] preselected, boolean editor) throws CoreException {
		if (!ElementValidator.check(type, getShell(), DIALOG_TITLE, editor))
			return;
		if (!ActionUtil.isProcessable(getShell(), type))
			return;
		if(!canRunOn(type)){
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.not_applicable")); //$NON-NLS-1$
			return;
		}			
		showUI(type, preselected);
	}

	//---- Java Editor --------------------------------------------------------------

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isProcessable(getShell(), fEditor))
				return;		
			
			IJavaElement[] elements = SelectionConverter.codeResolve(fEditor);
			if (elements.length == 1 && (elements[0] instanceof IField)) {
				IField field = (IField) elements[0];
				run(field.getDeclaringType(), new IField[] { field }, true);
				return;
			}
			IJavaElement element = SelectionConverter.getElementAtOffset(fEditor);
			if (element != null) {
				IType type = (IType) element.getAncestor(IJavaElement.TYPE);
				if (type != null) {
					if (type.getFields().length > 0) {
						run(type, new IField[0], true);
						return;
					}
				}
			}
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.not_applicable")); //$NON-NLS-1$
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.error.actionfailed")); //$NON-NLS-1$
		}
	}

	//---- Helpers -------------------------------------------------------------------

	private static class AddDelegateMethodsActionStatusValidator implements ISelectionStatusValidator {
		private static int fEntries;
			
		AddDelegateMethodsActionStatusValidator(int entries) {
			fEntries= entries;
		}

		public IStatus validate(Object[] selection) {
			StatusInfo state = new StatusInfo();
			if (selection != null && selection.length > 0) {
				HashSet map = new HashSet(selection.length);
				int count = 0;
				for (int i = 0; i < selection.length; i++) {
					Object key = selection[i];
					if (selection[i] instanceof Methods2Field) {
						count++;
						try {
							key = createSignatureKey(((Methods2Field) selection[i]).method);
						} catch (JavaModelException e) {
							return new StatusInfo(IStatus.ERROR, e.toString());
						}
					}
					if (!map.add(key)) { //$NON-NLS-1$
						state = new StatusInfo(IStatus.ERROR, ActionMessages.getString("AddDelegateMethodsAction.duplicate_methods")); //$NON-NLS-1$
						break;
					} else {
						String message;
						message = ActionMessages.getFormattedString("AddDelegateMethodsAction.selectioninfo.more", //$NON-NLS-1$ 
																	new Object[] { String.valueOf(count), String.valueOf(fEntries)} );
						state = new StatusInfo(IStatus.INFO, message);
					}
				}

			}	
			else
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
				
			return state;
		}			
	}
	
	private static ISelectionStatusValidator createValidator(int entries) {
		AddDelegateMethodsActionStatusValidator validator= new AddDelegateMethodsActionStatusValidator(entries);
		return validator;
	}
	
	private void showUI(IType type, IField[] preselected) {
		try {
			AddDelegateMethodsContentProvider provider = new AddDelegateMethodsContentProvider(type);
			Methods2FieldLabelProvider methodLabel = new Methods2FieldLabelProvider();

			SourceActionDialog dialog = new SourceActionDialog(getShell(), methodLabel, provider, fEditor, type, false);			
			dialog.setValidator(createValidator(provider.getNumEntries()));
			Methods2FieldSorter sorter= new Methods2FieldSorter();
			dialog.setSorter(sorter);
			dialog.setInput(new Object());			
			dialog.setContainerMode(true);
			dialog.setMessage(ActionMessages.getString("AddDelegateMethodsAction.message")); //$NON-NLS-1$
			dialog.setTitle(ActionMessages.getString("AddDelegateMethodsAction.title")); //$NON-NLS-1$
			
			Object[] elements= provider.getElements(null);			
			sorter.sort(null, elements);
			Object[] expand= {elements[0]};
			dialog.setExpandedElements(expand);
			dialog.setInitialSelections(preselected);
			dialog.setSize(60, 18);
			int result = dialog.open();
			if (result == Window.OK) {
				Object[] o = dialog.getResult();
				if (o == null)
					return;

				ArrayList methods = new ArrayList(o.length);
				for (int i = 0; i < o.length; i++) {
					if (o[i] instanceof Methods2Field)
						methods.add(o[i]);
				}

				IEditorPart part = EditorUtility.openInEditor(type);
				
				IRewriteTarget target= (IRewriteTarget) part.getAdapter(IRewriteTarget.class);
				IMethod[] createdMethods= null;
				try {
					if (target != null) {
						target.beginCompoundChange();
					}
					// pass dialog based information to the operation 
					IJavaElement elementPosition= dialog.getElementPosition();
						
					CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings();
					settings.createComments= dialog.getGenerateComment();										
					createdMethods= processResults(methods, type, elementPosition, settings);
				} finally {
					if (target != null) {
						target.endCompoundChange();
					}
				}
				
				if (createdMethods != null && createdMethods.length > 0) {
					JavaModelUtil.reconcile(type.getCompilationUnit());
					EditorUtility.revealInEditor(part, createdMethods[0]);
				}
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.error.actionfailed")); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.error.actionfailed")); //$NON-NLS-1$
		}
	}
	
	
	/**creates methods in class*/
	private IMethod[] processResults(List list, IType type, IJavaElement elementPosition, CodeGenerationSettings settings) throws InvocationTargetException {
		if (list.size() == 0)
			return null;
				
		AddDelegateMethodsOperation op = new AddDelegateMethodsOperation(list, settings, type, elementPosition);
		IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
		if (context == null) {
			context= new BusyIndicatorRunnableContext();
		}
		try {
			PlatformUI.getWorkbench().getProgressService().runInUI(
				context, new WorkbenchRunnableAdapter(op, op.getScheduleRule()), op.getScheduleRule());
		} catch (InterruptedException e) {
			// cancel pressed
			return null;
		}
		return op.getCreatedMethods();
	}

	/** The  model (content provider) for the field-methods tree */
	private static class AddDelegateMethodsContentProvider implements ITreeContentProvider {

		private Map fTreeMap= null;
		private Map fFieldMap= null;
		private Map fFilter= null;
		private int fNumEntries;

		/**
		 * Method FieldContentProvider.
		 * @param type	outer type to insert in (hide final methods in tree))
		 */
		AddDelegateMethodsContentProvider(IType type) throws JavaModelException {
			fFilter= new HashMap();				//hiding final methods
			fTreeMap= new TreeMap();			//mapping name to methods
			fFieldMap= new HashMap();			//mapping name to field

			fNumEntries= buildModel(type);
		}
		
		public int getNumEntries() {
			return fNumEntries;
		}

		/* Builds the entry list for the tree, and returns the number of entries in it */
		private int buildModel(IType type) throws JavaModelException {
			int numEntries= 0;
			IField[] fields = type.getFields();

			//build map to filter against
			IMethod[] finMeths = resolveFinalMethods(type);
			for (int i = 0; i < finMeths.length; i++) {
				fFilter.put(createSignatureKey(finMeths[i]), finMeths[i]);
			}

			IMethod[] filter = type.getMethods();
			for (int i = 0; i < filter.length; i++) {
				fFilter.put(createSignatureKey(filter[i]), filter[i]);
			}

			for (int i = 0; i < fields.length; i++) {
				IType fieldType = resolveTypeOfField(fields[i]);
				if (fieldType == null)
					continue;
				IMethod[] methods = resolveMethodsHierarchy(fieldType);
				List accessMethods = new ArrayList();

				//show public methods; hide constructors + final methods
				for (int j = 0; j < methods.length; j++) {
					boolean publicField = JavaModelUtil.isVisible(methods[j], type.getPackageFragment());
					boolean constructor = methods[j].isConstructor();
					boolean finalExist = fFilter.get(createSignatureKey(methods[j])) != null;
					if (publicField && !constructor && !finalExist) {
						accessMethods.add(new Methods2Field(methods[j], fields[i]));
						numEntries++;
					}
				}
				Object[] m = accessMethods.toArray();
				Methods2Field[] mf = new Methods2Field[m.length];
				for (int j = 0; j < m.length; j++) {
					mf[j] = (Methods2Field) m[j];
				}
				fTreeMap.put(fields[i].getElementName(), mf);
				fFieldMap.put(fields[i].getElementName(), fields[i]);

			}
			return numEntries;
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IField) {
				return (Object[]) fTreeMap.get(((IField) parentElement).getElementName());
			}
			return null;
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return element instanceof IField;
		}

		public Object[] getElements(Object inputElement) {
			Object[] o = fTreeMap.keySet().toArray();
			Object[] fields = new Object[o.length];
			for (int i = 0; i < o.length; i++) {
				fields[i] = fFieldMap.get(o[i]);
			}
			return fields;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	/**just to wrap JavaElementLabelProvider using my Methods2Field*/
	private static class Methods2FieldLabelProvider implements ILabelProvider {
		/**Delegate to forward calls*/
		JavaElementLabelProvider fMethodLabel = null;

		public Methods2FieldLabelProvider() {
			fMethodLabel = new JavaElementLabelProvider();
			fMethodLabel.turnOn(JavaElementLabelProvider.SHOW_TYPE);
		}
		public Image getImage(Object element) {
			if (element instanceof Methods2Field) {
				Methods2Field wrapper = (Methods2Field) element;
				return fMethodLabel.getImage(wrapper.method);
			} else if (element instanceof IJavaElement) {
				return fMethodLabel.getImage(element);
			}
			return null;
		}

		public String getText(Object element) {
			if (element instanceof Methods2Field) {
				Methods2Field wrapper = (Methods2Field) element;
				return fMethodLabel.getText(wrapper.method);
			} else if (element instanceof IJavaElement) {
				return fMethodLabel.getText(element);
			}
			return null;
		}

		public void addListener(ILabelProviderListener listener) {
			fMethodLabel.addListener(listener);
		}

		public void dispose() {
			fMethodLabel.dispose();
		}

		public boolean isLabelProperty(Object element, String property) {
			return fMethodLabel.isLabelProperty(element, property);
		}

		public void removeListener(ILabelProviderListener listener) {
			fMethodLabel.removeListener(listener);
		}

	}

	/** and a delegate for the sorter*/
	private static class Methods2FieldSorter extends ViewerSorter {
		JavaElementSorter fSorter = new JavaElementSorter();
		public int category(Object element) {
			if (element instanceof Methods2Field)
				element = ((Methods2Field) element).method;
			return fSorter.category(element);
		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof Methods2Field)
				e1 = ((Methods2Field) e1).method;
			if (e2 instanceof Methods2Field)
				e2 = ((Methods2Field) e2).method;
			return fSorter.compare(viewer, e1, e2);
		}

		public Collator getCollator() {
			return fSorter.getCollator();
		}
	}

	/**return all methods of all super types, minus duplicates*/
	private static IMethod[] resolveMethodsHierarchy(IType type) throws JavaModelException {
		Map map = new HashMap();

		IType[] superTypes = JavaModelUtil.getAllSuperTypes(type, new NullProgressMonitor());

		addMethodsToMapping(map, type);
		for (int i = 0; i < superTypes.length; i++) {
			addMethodsToMapping(map, superTypes[i]);
		}
		return (IMethod[]) map.values().toArray(new IMethod[map.values().size()]);
	}

	private static void addMethodsToMapping(Map map, IType type) throws JavaModelException {
		IMethod[] methods = type.getMethods();
		for (int i = 0; i < methods.length; i++) {
			map.put(createSignatureKey(methods[i]), methods[i]);
		}
	}

	/**returns a non null array of final methods of the type*/
	private static IMethod[] resolveFinalMethods(IType type) throws JavaModelException {

		//Interfaces are java.lang.Objects
		if (type.isInterface()) {
			type = getJavaLangObject(type.getJavaProject());//$NON-NLS-1$
		}

		IMethod[] methods = resolveMethodsHierarchy(type);
		List list = new ArrayList(methods.length);
		for (int i = 0; i < methods.length; i++) {
			boolean isFinal = Flags.isFinal(methods[i].getFlags());
			if (isFinal)
				list.add(methods[i]);
		}
		return (IMethod[]) list.toArray(new IMethod[list.size()]);
	}

	/**creates a key used for hash maps for a method signature (name+arguments(fqn))*/
	private static String createSignatureKey(IMethod method) throws JavaModelException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(method.getElementName());
		String[] args = method.getParameterTypes();
		for (int i = 0; i < args.length; i++) {
			String signature;
			if (isUnresolved(args[i])) {
				int acount = Signature.getArrayCount(args[i]);
				if (acount > 0) {
					String arg = args[i];
					int index = arg.lastIndexOf(Signature.C_ARRAY);
					arg = arg.substring(index + 1);
					signature = Signature.toString(arg);
				} else {
					signature = Signature.toString(args[i]);
				}

				String[][] fqn = method.getDeclaringType().resolveType(signature);
				if (fqn != null) {
					buffer.append(fqn[0][0]).append('.').append(fqn[0][1]);
					//TODO check for [][]
					for (int j = 0; j < acount; j++) {
						buffer.append("[]"); //$NON-NLS-1$
					}
				}
			}else{
				signature=Signature.toString(args[i]);
				buffer.append(signature);
			}
		}
		return buffer.toString();
	}

	private static boolean isUnresolved(String signature) {
		boolean flag = false;

		char c=Signature.getElementType(signature).charAt(0);
		boolean primitive=(c!= Signature.C_RESOLVED && c != Signature.C_UNRESOLVED);
		if(primitive)
			return flag;

		int acount = Signature.getArrayCount(signature);
		if (acount > 0) {
			int index = signature.lastIndexOf(Signature.C_ARRAY);
			c = signature.charAt(index + 1);
		} else {
			c = signature.charAt(0);
		}
		switch (c) {
			case Signature.C_RESOLVED :
				flag=false;
				break;
			case Signature.C_UNRESOLVED :
				flag=true;
				break;
			default :
				throw new IllegalArgumentException();
		}
		return flag;
	}

	private static boolean hasPrimitiveType(IField field) throws JavaModelException {
		String signature = field.getTypeSignature();
		char first = Signature.getElementType(signature).charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED);
	}

	/** 
	 * returns Type of field.
	 * 
	 * if field is primitive null is returned.
	 * if field is array java.lang.Object is returned.
	 **/
	private static IType resolveTypeOfField(IField field) throws JavaModelException {
		boolean isPrimitive = hasPrimitiveType(field);
		boolean isArray = isArray(field);
		if (!isPrimitive && !isArray) {
			String typeName = JavaModelUtil.getResolvedTypeName(field.getTypeSignature(), field.getDeclaringType());
			//if the CU has errors its possible no type name is resolved
			return typeName != null ? field.getJavaProject().findType(typeName) : null;
		} else if (isArray) {
			return getJavaLangObject(field.getJavaProject()); 
		}
		return null;

	}

	private static IType getJavaLangObject(IJavaProject project) throws JavaModelException {
		return JavaModelUtil.findType(project, "java.lang.Object");//$NON-NLS-1$
	}

	private static boolean isArray(IField field) throws JavaModelException {
		return Signature.getArrayCount(field.getTypeSignature()) > 0;
	}
}