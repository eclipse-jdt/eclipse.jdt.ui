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
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.IImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

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
 * @since 2.1
 */
public class AddDelegateMethodsAction extends SelectionDispatchAction {

	private static boolean fgReplaceFlag = false;
	private static boolean fgOverrideFinalsFlag = false;

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
	}

	//---- Structured Viewer -----------------------------------------------------------

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void run(IStructuredSelection selection) {
		try {
			IField[] selectedFields = getSelectedFields(selection);
			if (canEnableOn(selectedFields)) {
				run(selectedFields[0].getDeclaringType(), selectedFields, false);
				return;
			}
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IType)
				run((IType) firstElement, new IField[0], false);
			else if (firstElement instanceof ICompilationUnit)
				run(JavaElementUtil.getMainType((ICompilationUnit) firstElement), new IField[0], false);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.error.actionfailed")); //$NON-NLS-1$
		}

	}

	private boolean canEnable(IStructuredSelection selection) throws JavaModelException {
		if (canEnableOn(getSelectedFields(selection)))
			return true;

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType))
			return canEnableOn((IType) selection.getFirstElement());

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return canEnableOn(JavaElementUtil.getMainType((ICompilationUnit) selection.getFirstElement()));

		return false;
	}

	private static boolean canEnableOn(IType type) throws JavaModelException {
		if (type == null || type.getCompilationUnit() == null)
			return false;

		return canEnableOn(type.getFields());
	}

	private static boolean canEnableOn(IField[] fields) throws JavaModelException {
		if (fields == null) {
			return false;
		}
		int count = 0;
		for (int i = 0; i < fields.length; i++) {
			if (!hasPrimitiveType(fields[i])) {
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
						// remember the cu of the first element
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
		showUI(type, preselected);
	}

	//---- Java Editior --------------------------------------------------------------

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(ITextSelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void run(ITextSelection selection) {
		try {
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

	/**build ui */
	private void showUI(IType type, IField[] preselected) {
		try {
			FieldContentProvider provider = new FieldContentProvider(type);
			Methods2FieldLabelProvider methodLabel = new Methods2FieldLabelProvider();
			CheckedTreeSelectionDialog dialog = new CheckedTreeSelectionDialog(getShell(), methodLabel, provider);
			dialog.setValidator(new ISelectionStatusValidator() {
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
									key = createSignatureKey(((Methods2Field) selection[i]).fMethod);
								} catch (JavaModelException e) {
									return new StatusInfo(StatusInfo.ERROR, e.toString());
								}
							}
							if (!map.add(key)) { //$NON-NLS-1$
								state = new StatusInfo(IStatus.ERROR, ActionMessages.getString("AddDelegateMethodsAction.duplicate_methods")); //$NON-NLS-1$
								break;
							} else {
								String message;
								if (count == 1) {
									message = ActionMessages.getFormattedString("AddDelegateMethodsAction.selectioninfo.one", String.valueOf(count)); //$NON-NLS-1$
								} else {
									message = ActionMessages.getFormattedString("AddDelegateMethodsAction.selectioninfo.more", String.valueOf(count)); //$NON-NLS-1$
								}
								state = new StatusInfo(IStatus.INFO, message);
							}
						}

					}
					return state;
				}
			});

			dialog.setSorter(new Methods2FieldSorter());
			dialog.setInput(provider);
			dialog.setContainerMode(true);
			dialog.setMessage(ActionMessages.getString("AddDelegateMethodsAction.message")); //$NON-NLS-1$
			dialog.setTitle(ActionMessages.getString("AddDelegateMethodsAction.title")); //$NON-NLS-1$
			dialog.setExpandedElements(preselected);

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
				type = (IType) JavaModelUtil.toWorkingCopy(type);
				IMethod[] createdMethods = processResults(methods, type);
				if (createdMethods != null && createdMethods.length > 0) {
					EditorUtility.revealInEditor(part, createdMethods[0]);
				}
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.error.actionfailed")); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, DIALOG_TITLE, ActionMessages.getString("AddDelegateMethodsAction.error.actionfailed")); //$NON-NLS-1$
		}

	}
	/**Runnable for the operation*/
	private static class ResultRunner implements IWorkspaceRunnable {
		/**List with Methods2Field*/
		List fList = null;
		/**Type to add methods to*/
		IType fType = null;

		ArrayList fCreatedMethods;

		public ResultRunner(List resultList, IType type) {
			fList = resultList;
			fType = type;
			fCreatedMethods = new ArrayList();
		}

		public IMethod[] getCreatedMethods() {
			return (IMethod[]) fCreatedMethods.toArray(new IMethod[fCreatedMethods.size()]);
		}

		public void run(IProgressMonitor monitor) throws CoreException {
			String message = ActionMessages.getFormattedString("AddDelegateMethodsAction.monitor.message", String.valueOf(fList.size())); //$NON-NLS-1$

			monitor.beginTask(message, fList.size());

			// the preferences
			CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings();
			boolean addComments = settings.createComments;

			// already existing methods
			IMethod[] existingMethods = fType.getMethods();
			//the delemiter used
			String lineDelim = StubUtility.getLineDelimiterUsed(fType);
			// the indent used + 1
			int indent = StubUtility.getIndentUsed(fType) + 1;

			// perhaps we have to add import statements
			final ImportsStructure imports =
				new ImportsStructure(fType.getCompilationUnit(), settings.importOrder, settings.importThreshold, true);

			for (int i = 0; i < fList.size(); i++) {
				//long time=System.currentTimeMillis();
				//check for cancel each iteration
				if (monitor.isCanceled()) {
					if (i > 0) {
						imports.create(false, null);
					}
					return;
				}

				ITypeHierarchy typeHierarchy = fType.newSupertypeHierarchy(null);
				String content = null;
				Methods2Field wrapper = (Methods2Field) fList.get(i);
				IMethod curr = wrapper.fMethod;
				IField field = wrapper.fField;
				IMethod overwrittenMethod =
					JavaModelUtil.findMethodImplementationInHierarchy(
						typeHierarchy,
						fType,
						curr.getElementName(),
						curr.getParameterTypes(),
						curr.isConstructor());
				if (overwrittenMethod == null) {
					content = createStub(field, curr, addComments, overwrittenMethod, imports);
				} else {
					int flags = overwrittenMethod.getFlags();
					if (Flags.isFinal(flags) || Flags.isPrivate(flags)) {
						// we could ask before overwriting final methods
						if (fgOverrideFinalsFlag) {
							System.out.println("method final"); //$NON-NLS-1$
							System.out.println(overwrittenMethod);
						}
					}

					IMethod declaration =
						JavaModelUtil.findMethodDeclarationInHierarchy(
							typeHierarchy,
							fType,
							curr.getElementName(),
							curr.getParameterTypes(),
							curr.isConstructor());
					content = createStub(field, declaration, addComments, overwrittenMethod, imports);
				}
				IJavaElement sibling = null;
				IMethod existing =
					JavaModelUtil.findMethod(
						curr.getElementName(),
						curr.getParameterTypes(),
						curr.isConstructor(),
						existingMethods);
				if (existing != null) {
					// we could ask before replacing a method
					if (fgReplaceFlag) {
						System.out.println("method does already exists"); //$NON-NLS-1$
						System.out.println(existing);
						sibling = StubUtility.findNextSibling(existing);
						existing.delete(false, null);
					} else {
						continue;
					}
				} else if (curr.isConstructor() && existingMethods.length > 0) {
					// add constructors at the beginning
					sibling = existingMethods[0];
				}

				String formattedContent = StubUtility.codeFormat(content, indent, lineDelim) + lineDelim;
				IMethod created = fType.createMethod(formattedContent, sibling, true, null);
				fCreatedMethods.add(created);

				monitor.worked(1);
				//System.out.println(System.currentTimeMillis()-time +" for #"+i);
			}

			imports.create(false, null);
		}

		private String createStub(
			IField field,
			IMethod curr,
			boolean addComment,
			IMethod overridden,
			IImportsStructure imports)
			throws CoreException {
			String methodName = curr.getElementName();
			String[] paramNames = curr.getParameterNames();
			String returnTypSig = curr.getReturnType();

			StringBuffer buf = new StringBuffer();
			if (addComment) {
				String comment =
					StubUtility.getMethodComment(
						fType.getCompilationUnit(),
						fType.getElementName(),
						methodName,
						paramNames,
						curr.getExceptionTypes(),
						returnTypSig,
						overridden);
				if (comment != null) {
					buf.append(comment);
				}
			}

			String methodDeclaration = null;
			if (fType.isClass()) {
				StringBuffer body = new StringBuffer();
				if (!Signature.SIG_VOID.equals(returnTypSig)) {
					body.append("return "); //$NON-NLS-1$
				}
				if (JdtFlags.isStatic(curr)) {
					body.append(resolveTypeOfField(field).getElementName());
				} else {
					body.append(field.getElementName());
				}
				body.append('.').append(methodName).append('(');
				for (int i = 0; i < paramNames.length; i++) {
					body.append(paramNames[i]);
					if (i < paramNames.length - 1)
						body.append(',');
				}
				body.append(");"); //$NON-NLS-1$
				methodDeclaration = body.toString();
			}

			StubUtility.genMethodDeclaration(fType.getElementName(), curr, methodDeclaration, imports, buf);

			return buf.toString();
		}

	}

	/**creates methods in class*/
	private IMethod[] processResults(List list, IType type) throws InvocationTargetException {
		if (list.size() == 0)
			return null;

		ResultRunner resultRunner = new ResultRunner(list, type);
		IRunnableContext runnableContext = new ProgressMonitorDialog(getShell());
		try {
			runnableContext.run(false, true, new WorkbenchRunnableAdapter(resultRunner));
		} catch (InterruptedException e) {
			// cancel pressed
			return null;
		}
		return resultRunner.getCreatedMethods();
	}

	/** The  model (content provider) for the field-methods tree */
	private static class FieldContentProvider implements ITreeContentProvider {

		private TreeMap fTreeMap = null;
		private HashMap fFieldMap = null;

		private HashMap fFilter = null;

		/**
		 * Method FieldContentProvider.
		 * @param type	outer type to insert in (hide final methods in tree))
		 */
		FieldContentProvider(IType type) throws JavaModelException {
			//hiding final methods
			fFilter = new HashMap();

			//mapping name to methods
			fTreeMap = new TreeMap();
			//mapping name to field
			fFieldMap = new HashMap();

			buildModel(type);
		}

		private void buildModel(IType type) throws JavaModelException {
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
				ArrayList accessMethods = new ArrayList();

				//show public methods; hide constructors + final methods
				for (int j = 0; j < methods.length; j++) {
					boolean publicField = Flags.isPublic(methods[j].getFlags());
					boolean constructor = methods[j].isConstructor();
					boolean finalExist = fFilter.get(createSignatureKey(methods[j])) != null;
					if (publicField && !constructor && !finalExist)
						accessMethods.add(new Methods2Field(methods[j], fields[i]));
				}
				Object[] m = accessMethods.toArray();
				Methods2Field[] mf = new Methods2Field[m.length];
				for (int j = 0; j < m.length; j++) {
					mf[j] = (Methods2Field) m[j];
				}
				fTreeMap.put(fields[i].getElementName(), mf);
				fFieldMap.put(fields[i].getElementName(), fields[i]);

			}
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
			return element instanceof IField ? true : false;
		}

		public Object[] getElements(Object inputElement) {
			if ((inputElement != null) && (inputElement instanceof FieldContentProvider)) {
				Set set = fTreeMap.keySet();
				Object[] o = set.toArray();
				Object[] fields = new Object[o.length];
				for (int i = 0; i < o.length; i++) {
					fields[i] = fFieldMap.get(o[i]);
				}
				return fields;
			}
			return null;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	/**to map from dialog results to corresponding fields*/
	private static class Methods2Field {

		public Methods2Field(IMethod method, IField field) {
			fMethod = method;
			fField = field;
		}
		/**method to wrap*/
		IMethod fMethod = null;
		/**field where method is declared*/
		IField fField = null;
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
				return fMethodLabel.getImage(wrapper.fMethod);
			} else if (element instanceof IJavaElement) {
				return fMethodLabel.getImage(element);
			}
			return null;
		}

		public String getText(Object element) {
			if (element instanceof Methods2Field) {
				Methods2Field wrapper = (Methods2Field) element;
				return fMethodLabel.getText(wrapper.fMethod);
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
				element = ((Methods2Field) element).fMethod;
			return fSorter.category(element);
		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof Methods2Field)
				e1 = ((Methods2Field) e1).fMethod;
			if (e2 instanceof Methods2Field)
				e2 = ((Methods2Field) e2).fMethod;
			return fSorter.compare(viewer, e1, e2);
		}

		public Collator getCollator() {
			return fSorter.getCollator();
		}
	}

	/**return all methods of all super types, minus dups*/
	private static IMethod[] resolveMethodsHierarchy(IType type) throws JavaModelException {
		HashMap map = new HashMap();

		ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);

		IType[] types = hierarchy.getAllTypes();
		for (int i = 0; i < types.length; i++) {
			IMethod[] methods = types[i].getMethods();
			for (int j = 0; j < methods.length; j++) {
				map.put(createSignatureKey(methods[j]), methods[j]);
			}
		}
		ArrayList list = new ArrayList();
		list.addAll(map.values());
		IMethod[] methods = new IMethod[list.size()];
		for (int i = 0; i < methods.length; i++) {
			methods[i] = (IMethod) list.get(i);
		}

		return methods;
	}

	/**returns a non null array of final methods of the type*/
	private static IMethod[] resolveFinalMethods(IType type) throws JavaModelException {

		//Interfaces are java.lang.Objects
		if (type.isInterface()) {
			type = JavaModelUtil.findType(type.getJavaProject(), "java.lang.Object"); //$NON-NLS-1$
		}

		IMethod[] methods = resolveMethodsHierarchy(type);
		ArrayList list = new ArrayList(methods.length);
		for (int i = 0; i < methods.length; i++) {
			boolean isFinal = Flags.isFinal(methods[i].getFlags());
			if (isFinal)
				list.add(methods[i]);
		}
		IMethod[] finalMethods = new IMethod[list.size()];
		for (int i = 0; i < finalMethods.length; i++) {
			finalMethods[i] = (IMethod) list.get(i);
		}
		return finalMethods;
	}

	/**creates a key used for hashmaps for a method signature (name+arguments(fqn))*/
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
	 * if field is primitve null is returned.
	 * if field is array java.lang.Object is returned.
	 **/
	private static IType resolveTypeOfField(IField field) throws JavaModelException {
		boolean isPrimitive = hasPrimitiveType(field);
		boolean isArray = Signature.getArrayCount(field.getTypeSignature()) > 0;
		if (!isPrimitive && !isArray) {
			String typeName = JavaModelUtil.getResolvedTypeName(field.getTypeSignature(), field.getDeclaringType());
			//if the cu has errors its possible no type name is resolved
			IType type = typeName != null ? field.getJavaProject().findType(typeName) : null;
			return type;
		} else if (isArray) {
			return JavaModelUtil.findType(field.getJavaProject(), "java.lang.Object"); //$NON-NLS-1$
		}
		return null;

	}
}