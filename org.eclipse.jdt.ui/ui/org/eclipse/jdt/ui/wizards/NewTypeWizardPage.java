/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     John Kaplan, johnkaplantech@gmail.com - 108071 [code templates] template for body of newly created class
 *     Microsoft Corporation - [templates][content assist] - Extract the UI related code
 *     https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.filesystem.EFS;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateException;

import org.eclipse.ui.contentassist.ContentAssistHandler;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.manipulation.CodeGeneration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedConstructorsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.jdt.internal.corext.refactoring.TypeContextChecker;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.template.java.TemplateUtils;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Resources;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TableTextCellEditor;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.preferences.CodeTemplatePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.CompletionContextRequestor;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaPackageCompletionProcessor;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jdt.internal.ui.util.Progress;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.SuperInterfaceSelectionDialog;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonStatusDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * The class <code>NewTypeWizardPage</code> contains controls and validation routines
 * for a 'New Type WizardPage'. Implementors decide which components to add and to enable.
 * Implementors can also customize the validation code. <code>NewTypeWizardPage</code>
 * is intended to serve as base class of all wizards that create types like applets, servlets, classes,
 * interfaces, etc.
 * <p>
 * See {@link NewClassWizardPage} or {@link NewInterfaceWizardPage} for an
 * example usage of the <code>NewTypeWizardPage</code>.
 * </p>
 *
 * @see org.eclipse.jdt.ui.wizards.NewClassWizardPage
 * @see org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage
 * @see org.eclipse.jdt.ui.wizards.NewEnumWizardPage
 * @see org.eclipse.jdt.ui.wizards.NewAnnotationWizardPage
 *
 * @since 2.0
 */
public abstract class NewTypeWizardPage extends NewContainerWizardPage {

	/**
	 * Class used in stub creation routines to add needed imports to a compilation unit.
	 */
	public static class ImportsManager {

		/** AST root. */
		private final CompilationUnit fAstRoot;
		/** Imports rewrite. */
		private final ImportRewrite fImportsRewrite;

		/**
		 * Constructor with package visibility.
		 * @param astRoot AST root
		 */
		ImportsManager(CompilationUnit astRoot) {
			fAstRoot = astRoot;
			fImportsRewrite = StubUtility.createImportRewrite(astRoot, true);
		}

		/**
		 * Getter with package visibility.
		 * @return Compilation unit
		 */
		ICompilationUnit getCompilationUnit() {
			return fImportsRewrite.getCompilationUnit();
		}

		/**
		 * Adds a new import declaration that is sorted in the existing imports.
		 * If an import already exists or the import would conflict with an import
		 * of an other type with the same simple name, the import is not added.
		 *
		 * @param qualifiedTypeName The fully qualified name of the type to import (dot separated).
		 * @return Returns the simple type name that can be used in the code or the
		 * fully qualified type name if an import conflict prevented the import.
		 */
		public String addImport(String qualifiedTypeName) {
			return fImportsRewrite.addImport(qualifiedTypeName);
		}

		/**
		 * Adds a new import declaration that is sorted in the existing imports.
		 * If an import already exists or the import would conflict with an import
		 * of an other type with the same simple name, the import is not added.
		 *
		 * @param qualifiedTypeName The fully qualified name of the type to import (dot separated).
		 * @param insertPosition the offset where the import will be used
		 * @return Returns the simple type name that can be used in the code or the
		 * fully qualified type name if an import conflict prevented the import.
		 * @since 3.8
		 */
		public String addImport(String qualifiedTypeName, int insertPosition) {
			var context = new ContextSensitiveImportRewriteContext(fAstRoot, insertPosition, fImportsRewrite);
			return fImportsRewrite.addImport(qualifiedTypeName, context);
		}

		/**
		 * Adds a new import declaration that is sorted in the existing imports.
		 * If an import already exists or the import would conflict with an import
		 * of an other type with the same simple name, the import is not added.
		 *
		 * @param typeBinding the binding of the type to import
		 * @return Returns the simple type name that can be used in the code or the
		 * fully qualified type name if an import conflict prevented the import.
		 */
		public String addImport(ITypeBinding typeBinding) {
			return fImportsRewrite.addImport(typeBinding);
		}

		/**
		 * Adds a new import declaration that is sorted in the existing imports.
		 * If an import already exists or the import would conflict with an import
		 * of an other type with the same simple name, the import is not added.
		 *
		 * @param typeBinding the binding of the type to import
		 * @param insertPosition the offset where the import will be used
		 * @return Returns the simple type name that can be used in the code or the
		 * fully qualified type name if an import conflict prevented the import.
		 *
		 * @since 3.8
		 */
		public String addImport(ITypeBinding typeBinding, int insertPosition) {
			var context = new ContextSensitiveImportRewriteContext(fAstRoot, insertPosition, fImportsRewrite);
			return fImportsRewrite.addImport(typeBinding, context);
		}

		/**
		 * Adds a new import declaration for a static type that is sorted in the existing imports.
		 * If an import already exists or the import would conflict with an import
		 * of an other static import with the same simple name, the import is not added.
		 *
		 * @param declaringTypeName The qualified name of the static's member declaring type
		 * @param simpleName the simple name of the member; either a field or a method name.
		 * @param isField <code>true</code> specifies that the member is a field, <code>false</code> if it is a method.
		 * @return returns either the simple member name if the import was successful or else the qualified name if
		 * an import conflict prevented the import.
		 *
		 * @since 3.2
		 */
		public String addStaticImport(String declaringTypeName, String simpleName, boolean isField) {
			return fImportsRewrite.addStaticImport(declaringTypeName, simpleName, isField);
		}

		/**
		 * Package visibility! Creates and applies the rewrite.
		 * @param needsSave True if saving required
		 * @param monitor Progress monitor
		 * @throws CoreException Exception is thrown if the rewrite fails
		 */
		void create(boolean needsSave, IProgressMonitor monitor) throws CoreException {
			TextEdit edit = fImportsRewrite.rewriteImports(monitor);
			JavaModelUtil.applyEdit(fImportsRewrite.getCompilationUnit(), edit, needsSave, null);
		}

		/**
		 * Package visibility! Removes an import.
		 * @param qualifiedName Qualified name to be removed
		 */
		void removeImport(String qualifiedName) {
			fImportsRewrite.removeImport(qualifiedName);
		}

		/**
		 * Package visibility! Removes a static import.
		 * @param qualifiedName Qualified name to be removed
		 */
		void removeStaticImport(String qualifiedName) {
			fImportsRewrite.removeStaticImport(qualifiedName);
		}
	}

	/**
	 * This record represents a type creation result.
	 * @param createdType Created type
	 * @param imports Imports manager
	 * @param existingImports Existing imports
	 * @param connectedCU Connected compilation unit
	 * @param needsSave True if saving is required
	 * @param lineDelimiter Line delimiter
	 */
	private record TypeCreationResult(IType createdType, ImportsManager imports, Collection<String> existingImports,
		ICompilationUnit connectedCU, boolean needsSave, String lineDelimiter) {
	}

	/**
	 * This record represents type name variants (with and without type parameters).
	 * @param nameWithParams Type name with type parameters
	 * @param nameWithoutParams Type name without type parameters
	 * @param duplicate Flag indicating a duplicate
	 */
	private record TypeNameVariants(String nameWithParams, String nameWithoutParams, boolean duplicate) {
	}

	/**
	 * This class represents an interface wrapper.
	 */
	private static class InterfaceWrapper {
		/** Interface name. */
		private String interfaceName;
		/** The type. */
		private IType type;

		/**
		 * Constructor.
		 * @param interfaceName Interface name
		 */
		public InterfaceWrapper(String interfaceName) {
			this(interfaceName, null);
		}

		/**
		 * Constructor.
		 * @param interfaceName Interface name
		 * @param type The type
		 */
		public InterfaceWrapper(String interfaceName, IType type) {
			this.interfaceName = interfaceName;
			this.type = type;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int hashCode = interfaceName.hashCode();
			if (type != null) {
				hashCode &= type.hashCode();
			}
			return hashCode;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return obj != null && getClass().equals(obj.getClass())
					&& ((InterfaceWrapper) obj).interfaceName.equals(interfaceName)
					&& ((InterfaceWrapper) obj).type == type;
		}

		/**
		 * Sets the interface name and the type.
		 * @param newName Interface name
		 * @param type The type
		 */
		public void setInterfaceName(String newName, IType type) {
			this.type = type;
			this.interfaceName = newName;
		}

		/**
		 * Getter.
		 * @return The interface name
		 */
		public String getInterfaceName() {
			return this.interfaceName;
		}

		/**
		 * Getter.
		 * @return The type
		 */
		public IType getType() {
			return this.type;
		}
	}

	/**
	 * This class represents an interfaces list label provider.
	 */
	private static class InterfacesListLabelProvider extends LabelProvider {
		/** Interface image. */
		private Image fInterfaceImage;

		/**
		 * Constructor.
		 */
		public InterfacesListLabelProvider() {
			fInterfaceImage = JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE);
		}

		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		@Override
		public String getText(Object element) {
			return BasicElementLabels.getJavaElementName(((InterfaceWrapper) element).getInterfaceName());
		}

		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		@Override
		public Image getImage(Object element) {
			return fInterfaceImage;
		}
	}

	// -------- TypeFieldsAdapter --------

	/**
	 * This class represents a type fields adapter.
	 */
	private class TypeFieldsAdapter implements IStringButtonAdapter, IDialogFieldListener,
		IListAdapter<InterfaceWrapper>, SelectionListener {

		// -------- IStringButtonAdapter
		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(
		 * org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		@Override
		public void changeControlPressed(DialogField field) {
			typePageChangeControlPressed(field);
		}

		// -------- IListAdapter
		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#customButtonPressed(
		 * org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField, int)
		 */
		@Override
		public void customButtonPressed(ListDialogField<InterfaceWrapper> field, int index) {
			typePageCustomButtonPressed(field, index);
		}

		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#selectionChanged(
		 * org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		@Override
		public void selectionChanged(ListDialogField<InterfaceWrapper> field) {
			// Do nothing
		}

		// -------- IDialogFieldListener
		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(
		 * org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		@Override
		public void dialogFieldChanged(DialogField field) {
			typePageDialogFieldChanged(field);
		}

		/**
		 * {@inheritDoc}
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(
		 * org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		@Override
		public void doubleClicked(ListDialogField<InterfaceWrapper> field) {
		}

		/**
		 * {@inheritDoc}
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(SelectionEvent e) {
			typePageLinkActivated();
		}

		/**
		 * {@inheritDoc}
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			typePageLinkActivated();
		}
	}

	/** NUM_PATTERN. */
	private static final Pattern NUM_PATTERN = Pattern.compile("[0-9]+$"); //$NON-NLS-1$
	/** MIN_ONE_LETTER_PATTERN. */
	private static final Pattern MIN_ONE_LETTER_PATTERN = Pattern.compile(".*[a-zA-Z]+.*"); //$NON-NLS-1$
	/** PAGE_NAME. */
	private static final String PAGE_NAME = "NewTypeWizardPage"; //$NON-NLS-1$
	/** Field ID of the package input field. */
	protected static final String PACKAGE = PAGE_NAME + ".package";	 //$NON-NLS-1$
	/** Field ID of the enclosing type input field. */
	protected static final String ENCLOSING = PAGE_NAME + ".enclosing"; //$NON-NLS-1$
	/** Field ID of the enclosing type checkbox. */
	protected static final String ENCLOSINGSELECTION = ENCLOSING + ".selection"; //$NON-NLS-1$
	/** Field ID of the type name input field. */
	protected static final String TYPENAME = PAGE_NAME + ".typename"; //$NON-NLS-1$
	/** Field ID of the super type input field. */
	protected static final String SUPER = PAGE_NAME + ".superclass"; //$NON-NLS-1$
	/** Field ID of the super interfaces input field. */
	protected static final String INTERFACES = PAGE_NAME + ".interfaces"; //$NON-NLS-1$
	/** Field ID of the modifier check boxes. */
	protected static final String MODIFIERS = PAGE_NAME + ".modifiers"; //$NON-NLS-1$
	/** Field ID of the modifier check boxes. @since 3.25*/
	protected static final String SEALEDMODIFIERS = PAGE_NAME + ".sealedmodifiers"; //$NON-NLS-1$
	/** Field ID of the method stubs check boxes. */
	protected static final String METHODS = PAGE_NAME + ".methods"; //$NON-NLS-1$
	/** PUBLIC_INDEX. */
	private static final int PUBLIC_INDEX = 0;
	/** DEFAULT_INDEX. */
	private static final int DEFAULT_INDEX = 1;
	/** PRIVATE_INDEX. */
	private static final int PRIVATE_INDEX = 2;
	/** PROTECTED_INDEX. */
	private static final int PROTECTED_INDEX = 3;
	/** ABSTRACT_INDEX. */
	private static final int ABSTRACT_INDEX = 0;
	/** FINAL_INDEX. */
	private static final int FINAL_INDEX = 1;
	/** STATIC_INDEX. */
	private static final int STATIC_INDEX = 2;
	/** ENUM_ANNOT_STATIC_INDEX. */
	private static final int ENUM_ANNOT_STATIC_INDEX = 1;
	/** SEALED_FINAL_INDEX. */
	private static final int SEALED_FINAL_INDEX = 3;
	/** SEALED_INDEX. */
	private static final int SEALED_INDEX = 1;
	/** NON_SEALED_INDEX. */
	private static final int NON_SEALED_INDEX = 2;

	/**
	 * Constant to signal that the created type is a class.
	 * @since 3.1
	 */
	public static final int CLASS_TYPE = 1;

	/**
	 * Constant to signal that the created type is a interface.
	 * @since 3.1
	 */
	public static final int INTERFACE_TYPE = 2;

	/**
	 * Constant to signal that the created type is an enum.
	 * @since 3.1
	 */
	public static final int ENUM_TYPE = 3;

	/**
	 * Constant to signal that the created type is an annotation.
	 * @since 3.1
	 */
	public static final int ANNOTATION_TYPE = 4;

	/**
	 * Constant to signal that the created type is an record.
	 * @since 3.21
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public static final int RECORD_TYPE = 5;

	/** Public access flag. See The Java Virtual Machine Specification for more details. */
	public int F_PUBLIC = Flags.AccPublic;
	/** Private access flag. See The Java Virtual Machine Specification for more details. */
	public int F_PRIVATE = Flags.AccPrivate;
	/**  Protected access flag. See The Java Virtual Machine Specification for more details. */
	public int F_PROTECTED = Flags.AccProtected;
	/** Static access flag. See The Java Virtual Machine Specification for more details. */
	public int F_STATIC = Flags.AccStatic;
	/** Final access flag. See The Java Virtual Machine Specification for more details. */
	public int F_FINAL = Flags.AccFinal;
	/** Abstract property flag. See The Java Virtual Machine Specification for more details. */
	public int F_ABSTRACT = Flags.AccAbstract;
	/** Non-Sealed access Flag. */
	private int F_NON_SEALED = Flags.AccNonSealed;
	/** Sealed access Flag. */
	private int F_SEALED = Flags.AccSealed;

	/** Package dialog field. */
	private StringButtonStatusDialogField fPackageDialogField;
	/** Enclosing type selection. */
	private SelectionButtonDialogField fEnclosingTypeSelection;
	/** Enclosing type dialog field. */
	private StringButtonDialogField fEnclosingTypeDialogField;
	/** Can modify package flag. */
	private boolean fCanModifyPackage;
	/** Can modify enclosing type flag. */
	private boolean fCanModifyEnclosingType;
	/** Current package. */
	private IPackageFragment fCurrPackage;
	/** Current enclosing type. */
	private IType fCurrEnclosingType;
	/** Super class. */
	private IType fSuperClass;
	/** Type name dialog field. */
	private StringDialogField fTypeNameDialogField;
	/** Super class dialog field. */
	private StringButtonDialogField fSuperClassDialogField;
	/** Super interface dialog field. */
	private ListDialogField<InterfaceWrapper> fSuperInterfacesDialogField;
	/** Visibility modifier buttons. */
	private SelectionButtonDialogFieldGroup fAccMdfButtons;
	/** Other modifier buttons. */
	private SelectionButtonDialogFieldGroup fOtherMdfButtons;
	/** Sealed modifier buttons. */
	private SelectionButtonDialogFieldGroup fSealedMdfButtons;
	/** Flag for sealed supported. */
	private boolean fIsSealedSupported;
	/** Flag for super class reset. */
	private boolean fResetSuperClass = true;
	/** Used for compatibility: Wizards that don't show the comment button control will use the preferences settings. */
	private boolean fUseAddCommentButtonValue;
	/** The created types. */
	private List<IType> fCreatedTypes = new ArrayList<>();
	/** Current package completion processor. */
	private JavaPackageCompletionProcessor fCurrPackageCompletionProcessor;
	/** Enclosing type completion processor. */
	private JavaTypeCompletionProcessor fEnclosingTypeCompletionProcessor;
	/** Super class stub type context. */
	private StubTypeContext fSuperClassStubTypeContext;
	/** Super interface stub type context. */
	private StubTypeContext fSuperInterfaceStubTypeContext;
	/** Enclosing type status. */
	protected IStatus fEnclosingTypeStatus;
	/** Package status. */
	protected IStatus fPackageStatus;
	/** Type name status. */
	protected IStatus fTypeNameStatus;
	/** Super class status. */
	protected IStatus fSuperClassStatus;
	/** Modifier status. */
	protected IStatus fModifierStatus;
	/** Super interfaces status. */
	protected IStatus fSuperInterfacesStatus;

	/**
	 * This field is not intended to be referenced by clients.
	 * @noreference
	 */
	protected SelectionButtonDialogField fAddCommentButton;

	/**
	 * Sealed modifier status.
	 * @since 3.25
	 */
	protected IStatus fSealedModifierStatus;

	/**
	 * Sealed superclass status.
	 * @since 3.25
	 */
	protected IStatus fSealedSuperClassStatus;

	/**
	 * Sealed superinterfaces status.
	 * @since 3.25
	 */
	protected IStatus fSealedSuperInterfacesStatus;

	/** Type field adapter. */
	private TypeFieldsAdapter fTypeFieldAdapter;
	/** Type kind. */
	private int fTypeKind;

	/**
	 * Creates a new <code>NewTypeWizardPage</code>.
	 * @param isClass <code>true</code> if a new class is to be created; otherwise an interface is to be created
	 * @param pageName the wizard page's name
	 */
	public NewTypeWizardPage(boolean isClass, String pageName) {
		this(isClass ? CLASS_TYPE : INTERFACE_TYPE, pageName);
	}

	/**
	 * Creates a new <code>NewTypeWizardPage</code>.
	 * @param typeKind Signals the kind of the type to be created. Valid kinds are
	 * {@link #CLASS_TYPE}, {@link #INTERFACE_TYPE}, {@link #ENUM_TYPE} and {@link #ANNOTATION_TYPE}
	 * @param pageName the wizard page's name
	 * @since 3.1
	 */
	public NewTypeWizardPage(int typeKind, String pageName) {
		super(pageName);
		fTypeKind = typeKind;
		fCreatedTypes.clear();
		fTypeFieldAdapter = new TypeFieldsAdapter();
		fPackageDialogField = new StringButtonStatusDialogField(fTypeFieldAdapter);
		fPackageDialogField.setDialogFieldListener(fTypeFieldAdapter);
		fPackageDialogField.setLabelText(getPackageLabel());
		fPackageDialogField.setButtonLabel(NewWizardMessages.NewTypeWizardPage_package_button);
		fPackageDialogField.setStatusWidthHint(NewWizardMessages.NewTypeWizardPage_default);

		fEnclosingTypeSelection = new SelectionButtonDialogField(SWT.CHECK);
		fEnclosingTypeSelection.setDialogFieldListener(fTypeFieldAdapter);
		fEnclosingTypeSelection.setLabelText(getEnclosingTypeLabel());

		fEnclosingTypeDialogField = new StringButtonDialogField(fTypeFieldAdapter);
		fEnclosingTypeDialogField.setDialogFieldListener(fTypeFieldAdapter);
		fEnclosingTypeDialogField.setButtonLabel(NewWizardMessages.NewTypeWizardPage_enclosing_button);

		fTypeNameDialogField = new StringDialogField();
		fTypeNameDialogField.setDialogFieldListener(fTypeFieldAdapter);
		fTypeNameDialogField.setLabelText(getTypeNameLabel());

		fSuperClassDialogField = new StringButtonDialogField(fTypeFieldAdapter);
		fSuperClassDialogField.setDialogFieldListener(fTypeFieldAdapter);
		fSuperClassDialogField.setLabelText(getSuperClassLabel());
		fSuperClassDialogField.setButtonLabel(NewWizardMessages.NewTypeWizardPage_superclass_button);

		String[] addButtons = new String[] {
			NewWizardMessages.NewTypeWizardPage_interfaces_add,
			/* 1 */ null,
			NewWizardMessages.NewTypeWizardPage_interfaces_remove
		};
		fSuperInterfacesDialogField = new ListDialogField<>(fTypeFieldAdapter, addButtons,
				new InterfacesListLabelProvider());
		fSuperInterfacesDialogField.setDialogFieldListener(fTypeFieldAdapter);
		fSuperInterfacesDialogField.setTableColumns(new ListDialogField.ColumnsDescription(1, false));
		fSuperInterfacesDialogField.setLabelText(getSuperInterfacesLabel());
		fSuperInterfacesDialogField.setRemoveButtonIndex(2);

		String[] buttonNames1 = new String[] {
			NewWizardMessages.NewTypeWizardPage_modifiers_public,
			NewWizardMessages.NewTypeWizardPage_modifiers_default,
			NewWizardMessages.NewTypeWizardPage_modifiers_private,
			NewWizardMessages.NewTypeWizardPage_modifiers_protected
		};
		fAccMdfButtons = new SelectionButtonDialogFieldGroup(SWT.RADIO, buttonNames1, 4);
		fAccMdfButtons.setDialogFieldListener(fTypeFieldAdapter);
		fAccMdfButtons.setLabelText(getModifiersLabel());
		fAccMdfButtons.setSelection(0, true);

		String[] buttonNames2;
		if (fTypeKind == CLASS_TYPE) {
			buttonNames2 = new String[] {
				NewWizardMessages.NewTypeWizardPage_modifiers_abstract,
				NewWizardMessages.NewTypeWizardPage_modifiers_final,
				NewWizardMessages.NewTypeWizardPage_modifiers_static
			};
		} else {
			if (fTypeKind == ENUM_TYPE || fTypeKind == ANNOTATION_TYPE) {
				buttonNames2 = new String[] {
					NewWizardMessages.NewTypeWizardPage_modifiers_abstract,
					NewWizardMessages.NewTypeWizardPage_modifiers_static
				};
			} else {
				if (fTypeKind == RECORD_TYPE) {
					buttonNames2 = new String[] {NewWizardMessages.NewTypeWizardPage_modifiers_static};
				} else {
					buttonNames2 = new String[] {};
				}
			}
		}

		fOtherMdfButtons = new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames2, 4);
		fOtherMdfButtons.setDialogFieldListener(fTypeFieldAdapter);
		fAccMdfButtons.enableSelectionButton(PRIVATE_INDEX, false);
		fAccMdfButtons.enableSelectionButton(PROTECTED_INDEX, false);
		fOtherMdfButtons.enableSelectionButton(STATIC_INDEX, false);

		if (fTypeKind == ENUM_TYPE || fTypeKind == ANNOTATION_TYPE) {
			fOtherMdfButtons.enableSelectionButton(ABSTRACT_INDEX, false);
			fOtherMdfButtons.enableSelectionButton(ENUM_ANNOT_STATIC_INDEX, false);
		}

		fAddCommentButton = new SelectionButtonDialogField(SWT.CHECK);
		fAddCommentButton.setLabelText(NewWizardMessages.NewTypeWizardPage_addcomment_label);

		fUseAddCommentButtonValue = false; // only used when enabled

		fCurrPackageCompletionProcessor = new JavaPackageCompletionProcessor();
		fEnclosingTypeCompletionProcessor = new JavaTypeCompletionProcessor(false, false, true);

		fPackageStatus = new StatusInfo();
		fEnclosingTypeStatus = new StatusInfo();

		fCanModifyPackage = true;
		fCanModifyEnclosingType = true;
		updateEnableState();

		fTypeNameStatus = new StatusInfo();
		fSuperClassStatus = new StatusInfo();
		fSuperInterfacesStatus = new StatusInfo();
		fModifierStatus = new StatusInfo();
		fSealedModifierStatus = new StatusInfo();
		fSealedSuperClassStatus = new StatusInfo();
		fSealedSuperInterfacesStatus = new StatusInfo();
	}

	/**
	 * Initializes other buttons.
	 */
	private void initOtherButtons() {
		String[] buttonNames3 = null;
		switch (fTypeKind) {
			case CLASS_TYPE:
				if (fIsSealedSupported) {
					buttonNames3 = new String[] {
						NewWizardMessages.NewTypeWizardPage_none_label,
						NewWizardMessages.NewTypeWizardPage_modifiers_sealed,
						NewWizardMessages.NewTypeWizardPage_modifiers_non_sealed,
						NewWizardMessages.NewTypeWizardPage_modifiers_final
					};
				}
				break;
			case INTERFACE_TYPE:
				if (fIsSealedSupported) {
					buttonNames3 = new String[] {
						NewWizardMessages.NewTypeWizardPage_none_label,
						NewWizardMessages.NewTypeWizardPage_modifiers_sealed,
						NewWizardMessages.NewTypeWizardPage_modifiers_non_sealed
					};
				}
				break;
			default:
				// do nothing
		}
		fSealedMdfButtons = null;
		if (buttonNames3 != null) {
			fSealedMdfButtons = new SelectionButtonDialogFieldGroup(SWT.RADIO, buttonNames3, 4);
			fSealedMdfButtons.setDialogFieldListener(fTypeFieldAdapter);
			fSealedMdfButtons.enableSelectionButton(SEALED_INDEX, false);
			fSealedMdfButtons.enableSelectionButton(NON_SEALED_INDEX, false);
			fSealedMdfButtons.enableSelectionButton(SEALED_FINAL_INDEX, true);
			fOtherMdfButtons.enableSelectionButton(FINAL_INDEX, false);

			switch (fTypeKind) {
				case CLASS_TYPE:
				case INTERFACE_TYPE:
					if (fIsSealedSupported) {
						fSealedMdfButtons.enableSelectionButton(SEALED_INDEX, true);
					}
					break;
				default:
					// do nothing
			}
		}
	}

	/**
	 * Initializes all fields provided by the page with a given selection.
	 * @param elem the selection used to initialize this page or <code>null</code> if no selection was available
	 */
	protected void initTypePage(IJavaElement elem) {
		String initSuperclass = "java.lang.Object"; //$NON-NLS-1$
		ArrayList<String> initSuperinterfaces = new ArrayList<>(5);

		IJavaProject project = null;
		IPackageFragment pack = null;
		IType enclosingType = null;

		if (elem != null) {
			project = elem.getJavaProject();
			setIsNonSealedSupported(project);
			pack = (IPackageFragment) elem.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			if (pack == null && project != null) {
				pack = getPackage(project);
			}
			// evaluate the enclosing type
			IType typeInCU = (IType) elem.getAncestor(IJavaElement.TYPE);
			if (typeInCU != null) {
				if (typeInCU.getCompilationUnit() != null) {
					enclosingType = typeInCU;
				}
			} else {
				ICompilationUnit cu = (ICompilationUnit) elem.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (cu != null) {
					enclosingType = cu.findPrimaryType();
				}
			}

			try {
				IType type = null;
				if (elem.getElementType() == IJavaElement.TYPE) {
					type = (IType) elem;
					if (type.exists()) {
						String superName = SuperInterfaceSelectionDialog.getNameWithTypeParameters(type);
						if (type.isInterface()) {
							initSuperinterfaces.add(superName);
						} else {
							initSuperclass = superName;
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				// ignore this exception now
			}
		}

		String typeName = ""; //$NON-NLS-1$

		ITextSelection selection = getCurrentTextSelection();
		if (selection != null) {
			String text = selection.getText();
			if (text != null && validateJavaTypeName(text, project).isOK()) {
				typeName = getUniqueJavaTypeName(pack, text);
			}
		}

		setPackageFragment(pack, true);
		setEnclosingType(enclosingType, true);
		setEnclosingTypeSelection(false, true);

		setTypeName(typeName, true);
		setSuperClass(initSuperclass, true);
		setSuperInterfaces(initSuperinterfaces, true);

		setAddComments(StubUtility.doAddComments(project), true); // from project or workspace
	}

	/**
	 * Generate a unique type name for some initially given name under the given package fragment.
	 * @param pack the package fragment under which to check for uniqueness
	 * @param name the type name to check for uniqueness
	 * @return a type name string that is unique under the given package fragment. If the initial
	 * type name is not unique, it is suffixed with a number greater than or equal to 2.
	 * @since 3.17
	 */
	protected String getUniqueJavaTypeName(IPackageFragment pack, String name) {
		String typeName = name;
		if (pack != null) {
			IResource resource = null;
			boolean initial = true;
			while (resource == null || resource.exists()) {
				typeName = Signature.getSimpleName(typeName);
				Matcher m = NUM_PATTERN.matcher(typeName);
				if (m.find()) {
					// String ends with a number: increment it by 1
					BigDecimal newNumber = null;
					try {
						newNumber = new BigDecimal(m.group()).add(new BigDecimal(1));
						typeName = m.replaceFirst(newNumber.toPlainString());
					} catch (NumberFormatException e) {
						typeName = m.replaceFirst("2"); //$NON-NLS-1$
					}
				} else {
					typeName += (initial ? "" : "2"); //$NON-NLS-1$ //$NON-NLS-2$
					initial = false;
				}

				ICompilationUnit cu = pack.getCompilationUnit(getCompilationUnitName(typeName));
				resource = cu.getResource();
			}
		}
		return typeName;
	}

	/**
	 * Checks if the package field has to be pre-filled in this page and returns the package
	 * fragment to be used for that. The package fragment has the name of the project if the source
	 * folder does not contain any package and if the project name is a valid package name. If the
	 * source folder contains exactly one package then the name of that package is used as the
	 * package fragment's name. <code>null</code> is returned if none of the above is applicable.
	 *
	 * @param javaProject the containing Java project of the selection used to initialize this page
	 * @return the package fragment to be pre-filled in this page or <code>null</code> if no
	 * suitable package can be suggested for the given project
	 * @since 3.9
	 */
	private IPackageFragment getPackage(IJavaProject javaProject) {
		String packName = null;
		final IPackageFragmentRoot pkgFragmentRoot = getPackageFragmentRoot();
		IJavaElement[] packages = null;
		try {
			if (pkgFragmentRoot != null && pkgFragmentRoot.exists()) {
				packages = pkgFragmentRoot.getChildren();
				if (packages.length == 1) { // only default package -> use Project name
					packName = javaProject.getElementName();
					// validate package name
					IStatus status = validatePackageName(packName, javaProject);
					if (status.getSeverity() == IStatus.OK) {
						return pkgFragmentRoot.getPackageFragment(packName);
					}
				} else {
					int noOfPackages = 0;
					IPackageFragment thePackage = null;
					for (final IJavaElement pack : packages) {
						IPackageFragment pkg = (IPackageFragment) pack;
						// ignoring empty parent packages and default package
						if ((!pkg.hasSubpackages() || pkg.hasChildren()) && !pkg.isDefaultPackage()) {
							noOfPackages++;
							thePackage = pkg;
							if (noOfPackages > 1) {
								return null;
							}
						}
					}
					if (thePackage != null) { // use package name
						packName = thePackage.getElementName();
						return pkgFragmentRoot.getPackageFragment(packName);
					}
				}
			}
		} catch (JavaModelException e) {
			// fall through
		}
		return null;
	}

	/**
	 * Validates a Java type name.
	 * @param text Name to validate
	 * @param project Java project
	 * @return Validation status
	 */
	private static IStatus validateJavaTypeName(String text, IJavaProject project) {
		if (project == null || !project.exists()) {
			return JavaConventions.validateJavaTypeName(text, JavaCore.VERSION_1_3, JavaCore.VERSION_1_3, null);
		}
		return JavaConventionsUtil.validateJavaTypeName(text, project);
	}

	/**
	 * Validates a Java package name.
	 * @param text Name to validate
	 * @param project Java project
	 * @return Validation status
	 */
	private static IStatus validatePackageName(String text, IJavaProject project) {
		if (project == null || !project.exists()) {
			return JavaConventions.validatePackageName(text, JavaCore.VERSION_1_3, JavaCore.VERSION_1_3);
		}
		return JavaConventionsUtil.validatePackageName(text, project);
	}

	// -------- UI Creation ---------

	/**
	 * Returns the label that is used for the package input field.
	 * @return the label that is used for the package input field.
	 * @since 3.2
	 */
	protected String getPackageLabel() {
		return NewWizardMessages.NewTypeWizardPage_package_label;
	}

	/**
	 * Returns the label that is used for the enclosing type input field.
	 * @return the label that is used for the enclosing type input field.
	 * @since 3.2
	 */
	protected String getEnclosingTypeLabel() {
		return NewWizardMessages.NewTypeWizardPage_enclosing_selection_label;
	}

	/**
	 * Returns the label that is used for the type name input field.
	 * @return the label that is used for the type name input field.
	 * @since 3.2
	 */
	protected String getTypeNameLabel() {
		return NewWizardMessages.NewTypeWizardPage_typename_label;
	}

	/**
	 * Returns the label that is used for the modifiers input field.
	 * @return the label that is used for the modifiers input field
	 * @since 3.2
	 */
	protected String getModifiersLabel() {
		return NewWizardMessages.NewTypeWizardPage_modifiers_acc_label;
	}

	/**
	 * Returns the label that is used for the super class input field.
	 * @return the label that is used for the super class input field.
	 * @since 3.2
	 */
	protected String getSuperClassLabel() {
		return NewWizardMessages.NewTypeWizardPage_superclass_label;
	}

	/**
	 * Returns the label that is used for the super interfaces input field.
	 * @return the label that is used for the super interfaces input field.
	 * @since 3.2
	 */
	protected String getSuperInterfacesLabel() {
		if (fTypeKind != INTERFACE_TYPE) {
			return NewWizardMessages.NewTypeWizardPage_interfaces_class_label;
		}
		return NewWizardMessages.NewTypeWizardPage_interfaces_ifc_label;
	}

	/**
	 * Creates a separator line. Expects a <code>GridLayout</code> with at least 1 column.
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createSeparator(Composite composite, int nColumns) {
		(new Separator(SWT.SEPARATOR | SWT.HORIZONTAL)).doFillIntoGrid(composite, nColumns,
			convertHeightInCharsToPixels(1));
	}

	/**
	 * Creates the controls for the package name field. Expects a <code>GridLayout</code> with at least 4 columns.
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, nColumns);
		Text text = fPackageDialogField.getTextControl(null);
		BidiUtils.applyBidiProcessing(text, StructuredTextTypeHandlerFactory.JAVA);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());
		LayoutUtil.setHorizontalGrabbing(text);
		ControlContentAssistHelper.createTextContentAssistant(text, fCurrPackageCompletionProcessor);
		TextFieldNavigationHandler.install(text);
	}

	/**
	 * Creates the controls for the enclosing type name field. Expects a <code>GridLayout</code> with at
	 * least 4 columns.
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createEnclosingTypeControls(Composite composite, int nColumns) {
		// #6891
		Composite tabGroup = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
 		tabGroup.setLayout(layout);

		fEnclosingTypeSelection.doFillIntoGrid(tabGroup, 1);

		Text text = fEnclosingTypeDialogField.getTextControl(composite);
		SWTUtil.setAccessibilityText(text, NewWizardMessages.NewTypeWizardPage_enclosing_field_description);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = getMaxFieldWidth();
		gd.horizontalSpan = 2;
		text.setLayoutData(gd);

		Button button = fEnclosingTypeDialogField.getChangeControl(composite);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = SWTUtil.getButtonWidthHint(button);
		button.setLayoutData(gd);
		ControlContentAssistHelper.createTextContentAssistant(text, fEnclosingTypeCompletionProcessor);
		TextFieldNavigationHandler.install(text);
	}

	/**
	 * Creates the controls for the type name field. Expects a <code>GridLayout</code> with at least 2 columns.
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createTypeNameControls(Composite composite, int nColumns) {
		fTypeNameDialogField.doFillIntoGrid(composite, nColumns - 1);
		DialogField.createEmptySpace(composite);

		Text text = fTypeNameDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());
		TextFieldNavigationHandler.install(text);

		var textDeco = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
		var infoDeco = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		textDeco.setDescriptionText(NewWizardMessages.NewTypeWizardPage_info_NamesSeparatedBySemicolon);
		textDeco.setImage(infoDeco.getImage());

		text.addVerifyListener(e -> {
			if (fCanModifyPackage && ! fEnclosingTypeSelection.isSelected()
					&& e.start == 0 && e.end == ((Text) e.widget).getCharCount()) {
				String typeNameWithoutParameters = getTypeNameWithoutParameters(getTypeNameWithoutExtension(e.text));
				int lastDot = typeNameWithoutParameters.lastIndexOf('.');
				if (lastDot == -1 || lastDot == typeNameWithoutParameters.length() - 1) {
					return;
				}
				String pack = typeNameWithoutParameters.substring(0, lastDot);
				if (validatePackageName(pack, null).getSeverity() == IStatus.ERROR) {
					return;
				}
				fPackageDialogField.setText(pack);
				e.text = e.text.substring(lastDot + 1);
			}
		});
	}

	/**
	 * Creates the controls for the modifiers radio/checkbox buttons. Expects a
	 * <code>GridLayout</code> with at least 3 columns.
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createModifierControls(Composite composite, int nColumns) {
		LayoutUtil.setHorizontalSpan(fAccMdfButtons.getLabelControl(composite), 1);

		Control control = fAccMdfButtons.getSelectionButtonsGroup(composite);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = nColumns - 2;
		control.setLayoutData(gd);

		DialogField.createEmptySpace(composite);

		if (fTypeKind == CLASS_TYPE || fTypeKind == INTERFACE_TYPE) {
			DialogField.createEmptySpace(composite);

			control = fOtherMdfButtons.getSelectionButtonsGroup(composite);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			if (fSealedMdfButtons != null) {
				gd.horizontalSpan = nColumns - 1;
			} else {
				gd.horizontalSpan = nColumns - 2;
			}
			control.setLayoutData(gd);

			if (fSealedMdfButtons != null) {
				DialogField.createEmptySpace(composite);
				control = fSealedMdfButtons.getSelectionButtonsGroup(composite);
				gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				gd.horizontalSpan = nColumns - 2;
				control.setLayoutData(gd);
			}

			DialogField.createEmptySpace(composite);
		}
	}

	/**
	 * Creates the controls for the superclass name field. Expects a <code>GridLayout</code>
	 * with at least 3 columns.
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createSuperClassControls(Composite composite, int nColumns) {
		fSuperClassDialogField.doFillIntoGrid(composite, nColumns);
		Text text = fSuperClassDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());
		BidiUtils.applyBidiProcessing(text, StructuredTextTypeHandlerFactory.JAVA);

		JavaTypeCompletionProcessor superClassCompletionProcessor = new JavaTypeCompletionProcessor(false, false, true);
		superClassCompletionProcessor.setCompletionContextRequestor(new CompletionContextRequestor() {
			@Override
			public StubTypeContext getStubTypeContext() {
				return getSuperClassStubTypeContext(null);
			}
		});

		ControlContentAssistHelper.createTextContentAssistant(text, superClassCompletionProcessor);
		TextFieldNavigationHandler.install(text);
	}

	/**
	 * Creates the controls for the superclass name field. Expects a <code>GridLayout</code> with at least 3 columns.
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createSuperInterfacesControls(Composite composite, int nColumns) {
		fSuperInterfacesDialogField.doFillIntoGrid(composite, nColumns);
		final TableViewer tableViewer = fSuperInterfacesDialogField.getTableViewer();
		tableViewer.setColumnProperties(new String[] {"interface"}); //$NON-NLS-1$

		TableTextCellEditor cellEditor = new TableTextCellEditor(tableViewer, 0) {
			@Override
			protected Control createControl(Composite parent) {
				Control control = super.createControl(parent);
				BidiUtils.applyBidiProcessing(text, StructuredTextTypeHandlerFactory.JAVA);
				return control;
			}

			/**
			 * Sets the focus.
			 */
			@Override
			protected void doSetFocus() {
				if (text != null) {
					text.setFocus();
					text.setSelection(text.getText().length());
					checkSelection();
					checkDeleteable();
					checkSelectable();
				}
			}
		};
		var superInterfaceCompletionProcessor = new JavaTypeCompletionProcessor(false, false, true);
		superInterfaceCompletionProcessor.setCompletionContextRequestor(new CompletionContextRequestor() {
			@Override
			public StubTypeContext getStubTypeContext() {
				return getSuperInterfacesStubTypeContext(null);
			}
		});
		var contentAssistant = ControlContentAssistHelper.createJavaContentAssistant(superInterfaceCompletionProcessor);
		Text cellEditorText = cellEditor.getText();
		ContentAssistHandler.createHandlerForText(cellEditorText, contentAssistant);
		TextFieldNavigationHandler.install(cellEditorText);
		cellEditor.setContentAssistant(contentAssistant);

		tableViewer.setCellEditors(new CellEditor[] { cellEditor });
		tableViewer.setCellModifier(new ICellModifier() {
			@Override
			public void modify(Object element, String property, Object value) {
				if (element instanceof Item it) {
					element = it.getData();
				}

				IType interfaceType = null;
				try {
					interfaceType = findType(getJavaProject(), (String) value);
				} catch (JavaModelException e) {
					//do nothing
				}
				((InterfaceWrapper) element).setInterfaceName((String) value, interfaceType);

				fSuperInterfacesDialogField.elementChanged((InterfaceWrapper) element);
			}
			@Override
			public Object getValue(Object element, String property) {
				return ((InterfaceWrapper) element).getInterfaceName();
			}
			@Override
			public boolean canModify(Object element, String property) {
				return true;
			}
		});
		tableViewer.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.F2 && event.stateMask == 0) {
					ISelection selection = tableViewer.getSelection();
					if (! (selection instanceof IStructuredSelection)) {
						return;
					}
					IStructuredSelection structuredSelection = (IStructuredSelection) selection;
					tableViewer.editElement(structuredSelection.getFirstElement(), 0);
				}
			}
		});
		GridData gd = (GridData) fSuperInterfacesDialogField.getListControl(null).getLayoutData();
		if (fTypeKind == CLASS_TYPE) {
			gd.heightHint = convertHeightInCharsToPixels(3);
		} else {
			gd.heightHint = convertHeightInCharsToPixels(6);
		}
		gd.grabExcessVerticalSpace = false;
		gd.widthHint = getMaxFieldWidth();
	}

	/**
	 * Creates the controls for the preference page links. Expects a <code>GridLayout</code> with at least 3 columns.
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 * @since 3.1
	 */
	protected void createCommentControls(Composite composite, int nColumns) {
		Link link = new Link(composite, SWT.NONE);
		link.setText(NewWizardMessages.NewTypeWizardPage_addcomment_description);
		link.addSelectionListener(new TypeFieldsAdapter());
		link.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, nColumns, 1));
		DialogField.createEmptySpace(composite);
		fAddCommentButton.doFillIntoGrid(composite, nColumns - 1);
	}

	/**
	 * Creates the comment and link in the single line for the preference page links. Expects a
	 * <code>GridLayout</code> with at least 2 columns.
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 * @param isModule if it is module or package
	 * @return link is returned
	 * @since 3.18
	 */
	protected Link createCommentWithLinkControls(Composite composite, int nColumns, boolean isModule) {
		if (isModule) {
			DialogField.createEmptySpace(composite);
		}
		fAddCommentButton.doFillIntoGridWithoutMargin(composite, nColumns, !isModule);
		Link link = new Link(composite, SWT.NONE);
		link.setText(NewWizardMessages.NewTypeWizardPage_addcomment_description2);
		link.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		if (!isModule) {
			fAddCommentButton.setEnabled(false);
		}
		link.addSelectionListener(new TypeFieldsAdapter());
		return link;
	}

	/**
	 * Sets the focus on the type name input field.
	 */
	protected void setFocus() {
		if (fTypeNameDialogField.isEnabled()) {
			fTypeNameDialogField.setFocus();
		} else {
			setFocusOnContainer();
		}
	}

	/**
	 * Hook when type page link is activated.
	 */
	private void typePageLinkActivated() {
		IJavaProject project = getJavaProject();
		if (project != null) {
			PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(getShell(), project.getProject(),
					CodeTemplatePreferencePage.PROP_ID, null, null);
			dialog.open();
		} else {
			String title = NewWizardMessages.NewTypeWizardPage_configure_templates_title;
			String message = NewWizardMessages.NewTypeWizardPage_configure_templates_message;
			MessageDialog.openInformation(getShell(), title, message);
		}
	}

	/**
	 * Type page change control was pressed.
	 * @param field Dialog field
	 */
	private void typePageChangeControlPressed(DialogField field) {
		if (field == fPackageDialogField) {
			IPackageFragment pack = choosePackage();
			if (pack != null) {
				fPackageDialogField.setText(pack.getElementName());
			}
		} else if (field == fEnclosingTypeDialogField) {
			IType type = chooseEnclosingType();
			if (type != null) {
				setEnclosingType(type);
			}
		} else if (field == fSuperClassDialogField) {
			IType type = chooseSuperClass();
			setSuperClass(type);
		}
	}

	/**
	 * Type page custom button was pressed.
	 * @param field Dialog field
	 * @param index The index
	 */
	private void typePageCustomButtonPressed(DialogField field, int index) {
		if (field == fSuperInterfacesDialogField && index == 0) {
			chooseSuperInterfaces();
			List<InterfaceWrapper> interfaces = fSuperInterfacesDialogField.getElements();
			if (!interfaces.isEmpty()) {
				Object element = interfaces.get(interfaces.size() - 1);
				fSuperInterfacesDialogField.editElement(element);
			}
		}
	}

	/**
	 * A field on the type has changed. The fields' status and all dependent status are updated.
	 * @param field Dialog field
	 */
	private void typePageDialogFieldChanged(DialogField field) {
		String fieldName = null;
		if (field == fPackageDialogField) {
			fPackageStatus = packageChanged();
			updatePackageStatusLabel();
			fTypeNameStatus = typeNameChanged();
			fSuperClassStatus = superClassChanged();
			fieldName = PACKAGE;
		} else if (field == fEnclosingTypeDialogField) {
			fEnclosingTypeStatus = enclosingTypeChanged();
			fTypeNameStatus = typeNameChanged();
			fSuperClassStatus = superClassChanged();
			fieldName = ENCLOSING;
		} else if (field == fEnclosingTypeSelection) {
			updateEnableState();
			boolean isEnclosedType = isEnclosingTypeSelected();
			if (!isEnclosedType) {
				if (fAccMdfButtons.isSelected(PRIVATE_INDEX) || fAccMdfButtons.isSelected(PROTECTED_INDEX)) {
					fAccMdfButtons.setSelection(PRIVATE_INDEX, false);
					fAccMdfButtons.setSelection(PROTECTED_INDEX, false);
					fAccMdfButtons.setSelection(PUBLIC_INDEX, true);
				}
				if (fOtherMdfButtons.isSelected(STATIC_INDEX)) {
					fOtherMdfButtons.setSelection(STATIC_INDEX, false);
				}
			}
			fAccMdfButtons.enableSelectionButton(PRIVATE_INDEX, isEnclosedType);
			fAccMdfButtons.enableSelectionButton(PROTECTED_INDEX, isEnclosedType);
			fOtherMdfButtons.enableSelectionButton(STATIC_INDEX, isEnclosedType);
			fTypeNameStatus = typeNameChanged();
			fSuperClassStatus = superClassChanged();
			fieldName = ENCLOSINGSELECTION;
		} else if (field == fTypeNameDialogField) {
			fTypeNameStatus = typeNameChanged();
			fieldName = TYPENAME;
		} else if (field == fSuperClassDialogField) {
			setSuperClassType();
			fSuperClassStatus = superClassChanged();
			fSealedModifierStatus = sealedModifiersChanged();
			fieldName = SUPER;
		} else if (field == fSuperInterfacesDialogField) {
			fSuperInterfacesStatus = superInterfacesChanged();
			fSealedModifierStatus = sealedModifiersChanged();
			fieldName = INTERFACES;
		} else if (field == fOtherMdfButtons || field == fAccMdfButtons) {
			fModifierStatus = modifiersChanged();
			fieldName = MODIFIERS;
		} else if (field == fSealedMdfButtons) {
			fSealedModifierStatus = sealedModifiersChanged();
			fieldName = SEALEDMODIFIERS;
		} else {
			fieldName = METHODS;
		}
		// tell all others
		handleFieldChanged(fieldName);
	}

	// -------- update message ----------------

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jdt.ui.wizards.NewContainerWizardPage#handleFieldChanged(java.lang.String)
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (CONTAINER.equals(fieldName)) {
			fPackageStatus = packageChanged();
			fEnclosingTypeStatus = enclosingTypeChanged();
			fTypeNameStatus = typeNameChanged();
			fSuperClassStatus = superClassChanged();
			fSuperInterfacesStatus = superInterfacesChanged();
			fSealedModifierStatus = sealedModifiersChanged();
		}
	}

	// ---- set / get ----------------

	/**
	 * Returns the text of the package input field.
	 * @return the text of the package input field
	 */
	public String getPackageText() {
		return fPackageDialogField.getText();
	}

	/**
	 * Returns the text of the enclosing type input field.
	 * @return the text of the enclosing type input field
	 */
	public String getEnclosingTypeText() {
		return fEnclosingTypeDialogField.getText();
	}

	/**
	 * Returns the package fragment corresponding to the current input.
	 * @return a package fragment or <code>null</code> if the input could not be resolved.
	 */
	public IPackageFragment getPackageFragment() {
		if (!isEnclosingTypeSelected()) {
			return fCurrPackage;
		}
		return fCurrEnclosingType == null ? null : fCurrEnclosingType.getPackageFragment();
	}

	/**
	 * Sets the package fragment to the given value. The method updates the model and the text of the control.
	 * @param pack the package fragment to be set
	 * @param canBeModified if <code>true</code> the package fragment is editable; otherwise it is read-only.
	 */
	public void setPackageFragment(IPackageFragment pack, boolean canBeModified) {
		fCurrPackage = pack;
		fCanModifyPackage = canBeModified;
		String str = (pack == null) ? "" : pack.getElementName(); //$NON-NLS-1$
		fPackageDialogField.setText(str);
		updateEnableState();
		if (fCurrPackage != null) {
			setIsNonSealedSupported(fCurrPackage.getJavaProject());
		}
	}

	/**
	 * Returns the enclosing type corresponding to the current input.
	 * @return Enclosing type or <code>null</code> if the enclosing type is not selected or the input could not be
	 * resolved
	 */
	public IType getEnclosingType() {
		return isEnclosingTypeSelected() ? fCurrEnclosingType : null;
	}

	/**
	 * Sets the enclosing type. The method updates the underlying model and the text of the control.
	 * @param type the enclosing type
	 * @param canBeModified if <code>true</code> the enclosing type field is editable; otherwise it is read-only.
	 */
	public void setEnclosingType(IType type, boolean canBeModified) {
		fCurrEnclosingType = type;
		fCanModifyEnclosingType = canBeModified;
		String str = (type == null) ? "" : type.getFullyQualifiedName('.'); //$NON-NLS-1$
		fEnclosingTypeDialogField.setText(str);
		updateEnableState();
	}

	/**
	 * Returns the selection state of the enclosing type checkbox.
	 * @return the selection state of the enclosing type checkbox
	 */
	public boolean isEnclosingTypeSelected() {
		return fEnclosingTypeSelection.isSelected();
	}

	/**
	 * Sets the enclosing type checkbox's selection state.
	 * @param isSelected the checkbox's selection state
	 * @param canBeModified if <code>true</code> the enclosing type checkbox is
	 * modifiable; otherwise it is read-only.
	 */
	public void setEnclosingTypeSelection(boolean isSelected, boolean canBeModified) {
		fEnclosingTypeSelection.setSelection(isSelected);
		fEnclosingTypeSelection.setEnabled(canBeModified);
		updateEnableState();
	}

	/**
	 * Returns the type name entered into the type input field (without the default file extension
	 * <code>java</code>, if entered).
	 * @return the type name
	 */
	public String getTypeName() {
		String typeNameWithExtension = fTypeNameDialogField.getText();
		return getTypeNameWithoutExtension(typeNameWithExtension);
	}

	/**
	 * Returns the type name without extension (.java).
	 * @param typeNameWithExtension Type name possibly with extension
	 * @return Type name without extension
	 */
	private String getTypeNameWithoutExtension(String typeNameWithExtension) {
		if (!typeNameWithExtension.endsWith(JavaModelUtil.DEFAULT_CU_SUFFIX)) {
			return typeNameWithExtension;
		}
		int extensionOffset = typeNameWithExtension.lastIndexOf(JavaModelUtil.DEFAULT_CU_SUFFIX);
		return typeNameWithExtension.substring(0, extensionOffset);
	}

	/**
	 * Sets the type name input field's text to the given value. Method doesn't update the model.
	 * @param name the new type name
	 * @param canBeModified if <code>true</code> the type name field is editable; otherwise it is read-only.
	 */
	public void setTypeName(String name, boolean canBeModified) {
		fTypeNameDialogField.setText(name);
		fTypeNameDialogField.setEnabled(canBeModified);
	}

	/**
	 * Returns the selected modifiers.
	 * @return the selected modifiers
	 * @see Flags
	 */
	public int getModifiers() {
		int mdf = 0;
		if (fAccMdfButtons.isSelected(PUBLIC_INDEX)) {
			mdf += F_PUBLIC;
		} else if (fAccMdfButtons.isSelected(PRIVATE_INDEX)) {
			mdf += F_PRIVATE;
		} else if (fAccMdfButtons.isSelected(PROTECTED_INDEX)) {
			mdf += F_PROTECTED;
		}
		if (fOtherMdfButtons.isSelected(ABSTRACT_INDEX)) {
			mdf += F_ABSTRACT;
		}
		if (fOtherMdfButtons.isSelected(FINAL_INDEX)) {
			mdf += F_FINAL;
		}
		if (fOtherMdfButtons.isSelected(STATIC_INDEX)) {
			mdf += F_STATIC;
		}
		if (fSealedMdfButtons != null) {
			if (fSealedMdfButtons.isSelected(SEALED_FINAL_INDEX)) {
				mdf += F_FINAL;
			}
			if (fSealedMdfButtons.isSelected(NON_SEALED_INDEX)) {
				mdf += F_NON_SEALED;
			}
			if (fSealedMdfButtons.isSelected(SEALED_INDEX)) {
				mdf += F_SEALED;
			}
		}
		return mdf;
	}

	/**
	 * Sets the modifiers.
	 * @param modifiers <code>F_PUBLIC</code>, <code>F_PRIVATE</code>,
	 * <code>F_PROTECTED</code>, <code>F_ABSTRACT</code>, <code>F_FINAL</code>
	 * or <code>F_STATIC</code> or a valid combination.
	 * @param canBeModified if <code>true</code> the modifier fields are editable; otherwise they are read-only
	 * @see Flags
	 */
	public void setModifiers(int modifiers, boolean canBeModified) {
		if (Flags.isPublic(modifiers)) {
			fAccMdfButtons.setSelection(PUBLIC_INDEX, true);
		} else if (Flags.isPrivate(modifiers)) {
			fAccMdfButtons.setSelection(PRIVATE_INDEX, true);
		} else if (Flags.isProtected(modifiers)) {
			fAccMdfButtons.setSelection(PROTECTED_INDEX, true);
		} else {
			fAccMdfButtons.setSelection(DEFAULT_INDEX, true);
		}
		if (Flags.isAbstract(modifiers)) {
			fOtherMdfButtons.setSelection(ABSTRACT_INDEX, true);
		}
		if (Flags.isFinal(modifiers)) {
			if (fOtherMdfButtons.isEnabled(FINAL_INDEX)) {
				fOtherMdfButtons.setSelection(FINAL_INDEX, true);
			} else if (fSealedMdfButtons != null) {
				fSealedMdfButtons.setSelection(SEALED_FINAL_INDEX, true);
			}
		}
		if (Flags.isStatic(modifiers)) {
			fOtherMdfButtons.setSelection(STATIC_INDEX, true);
		}
		if (fSealedMdfButtons != null) {
			if (Flags.isSealed(modifiers)) {
				fSealedMdfButtons.setSelection(SEALED_INDEX, true);
			}
			if (Flags.isNonSealed(modifiers)) {
				fSealedMdfButtons.setSelection(NON_SEALED_INDEX, true);
			}
			fSealedModifierStatus = sealedModifiersChanged();
		}

		fAccMdfButtons.setEnabled(canBeModified);
		fOtherMdfButtons.setEnabled(canBeModified);
	}

	/**
	 * Returns the content of the superclass input field.
	 * @return the superclass name
	 */
	public String getSuperClass() {
		return fSuperClassDialogField.getText();
	}

	/**
	 * Sets the super class.
	 * @param type Super class
	 */
	private void setSuperClass(IType type) {
		if (type != null) {
			fSuperClass = type;
			fResetSuperClass = false;
			fSuperClassDialogField.setText(SuperInterfaceSelectionDialog.getNameWithTypeParameters(type));
			fResetSuperClass = true;
		}
	}

	/**
	 * Sets the enclosing type.
	 * @param type Enclosing type
	 */
	private void setEnclosingType(IType type) {
		if (type != null) {
			fEnclosingTypeDialogField.setText(type.getFullyQualifiedName('.'));
		}
	}

	/**
	 * Sets the super class name.
	 * @param name the new superclass name
	 * @param canBeModified  if <code>true</code> the superclass name field is editable; otherwise it is read-only.
	 */
	public void setSuperClass(String name, boolean canBeModified) {
		fSuperClassDialogField.setText(name);
		fSuperClassDialogField.setEnabled(canBeModified);
	}

	/**
	 * Sets the super class name.
	 * @param type the binding of superclass
	 * @param canBeModified if <code>true</code> the superclass name field is editable; otherwise it is read-only.
	 * @since 3.25
	 */
	public void setSuperClass(ITypeBinding type, boolean canBeModified) {
		fSuperClass = null;
		if (type != null) {
			IJavaElement jElem = type.getJavaElement();
			if (jElem instanceof IType ty) {
				fSuperClass = ty;
			}
			this.fResetSuperClass = false;
			setSuperClass(type.getQualifiedName(), canBeModified);
			this.fResetSuperClass = true;
		}
		fSuperClassStatus = superClassChanged();
	}

	/**
	 * Returns the chosen super interfaces.
	 * @return a list of chosen super interfaces. The list's elements
	 * are of type <code>String</code>
	 */
	public List<String> getSuperInterfaces() {
		return fSuperInterfacesDialogField.getElements().stream()
			.map(InterfaceWrapper::getInterfaceName)
			.collect(Collectors.toList());
	}

	/**
	 * Sets the super interfaces.
	 * @param interfacesNames a list of super interface. The method requires that
	 * the list's elements are of type <code>String</code>
	 * @param canBeModified if <code>true</code> the super interface field is editable; otherwise it is read-only.
	 */
	public void setSuperInterfaces(List<String> interfacesNames, boolean canBeModified) {
		List<InterfaceWrapper> interfaces = interfacesNames.stream()
			.map(InterfaceWrapper::new)
			.collect(Collectors.toList());
		fSuperInterfacesDialogField.setElements(interfaces);
		fSuperInterfacesDialogField.setEnabled(canBeModified);
	}

	/**
	 * Sets the super interfaces.
	 * @param interfaceBindings a list of super interface bindings. The method requires that
	 * the list's elements are of type <code>ITypeBinding</code>
	 * @param canBeModified if <code>true</code> the super interface field is editable; otherwise it is read-only.
	 * @since 3.25
	 */
	public void setSuperInterfacesList(List<ITypeBinding> interfaceBindings, boolean canBeModified) {
		ArrayList<InterfaceWrapper> interfaces = new ArrayList<>(interfaceBindings.size());
		for (ITypeBinding typeBinding : interfaceBindings) {
			IJavaElement jElem = typeBinding.getJavaElement();
			if (jElem instanceof IType ty) {
				interfaces.add(new InterfaceWrapper(typeBinding.getQualifiedName(), ty));
			} else {
				interfaces.add(new InterfaceWrapper(typeBinding.getQualifiedName()));
			}
		}
		fSuperInterfacesDialogField.setElements(interfaces);
		fSuperInterfacesDialogField.setEnabled(canBeModified);
		fSuperInterfacesStatus = superInterfacesChanged();
	}

	/**
	 * Adds a super interface to the end of the list and selects it if it is not in the list yet.
	 * @param superInterface the fully qualified type name of the interface.
	 * @return returns <code>true</code>if the interfaces has been added, <code>false</code>
	 * if the interface already is in the list.
	 * @since 3.2
	 */
	public boolean addSuperInterface(String superInterface) {
		return fSuperInterfacesDialogField.addElement(new InterfaceWrapper(superInterface));
	}

	/**
	 * Adds a super interface to the end of the list and selects it if it is not in the list yet.
	 * @param superInterface the fully qualified type name of the interface.
	 * @param type IType java element of the interface.
	 * @return returns <code>true</code>if the interfaces has been added, <code>false</code>
	 * if the interface already is in the list.
	 * @since 3.25
	 */
	public boolean addSuperInterface(String superInterface, IType type) {
		return fSuperInterfacesDialogField.addElement(new InterfaceWrapper(superInterface, type));
	}

	/**
	 * Sets 'Add comment' checkbox. The value set will only be used when creating source when
	 * the comment control is enabled (see {@link #enableCommentControl(boolean)}
	 * @param doAddComments if <code>true</code>, comments are added.
	 * @param canBeModified if <code>true</code> check box is editable; otherwise it is read-only.
	 * @since 3.1
	 */
	public void setAddComments(boolean doAddComments, boolean canBeModified) {
		fAddCommentButton.setSelection(doAddComments);
		fAddCommentButton.setEnabled(canBeModified);
	}

	/**
	 * Sets to use the 'Add comment' checkbox value. Clients that use the 'Add comment' checkbox
	 * additionally have to enable the control. This has been added for backwards compatibility.
	 * @param useAddCommentValue if <code>true</code>,
	 * @since 3.1
	 */
	public void enableCommentControl(boolean useAddCommentValue) {
		fUseAddCommentButtonValue = useAddCommentValue;
	}

	/**
	 * Returns if comments are added. This method can be overridden by clients.
	 * The selection of the comment control is taken if enabled (see {@link #enableCommentControl(boolean)}, otherwise
	 * the settings as specified in the preferences is used.
	 * @return Returns <code>true</code> if comments can be added
	 * @since 3.1
	 */
	public boolean isAddComments() {
		if (fUseAddCommentButtonValue) {
			return fAddCommentButton.isSelected();
		}
		return StubUtility.doAddComments(getJavaProject());
	}

	/**
	 * Returns the resource handle that corresponds to the compilation unit to was or will be created or modified.
	 * @return A resource or null if the page contains illegal values
	 * @since 3.0
	 */
	public IResource getModifiedResource() {
		IType enclosing = getEnclosingType();
		if (enclosing != null) {
			return enclosing.getResource();
		}
		IPackageFragment pack = getPackageFragment();
		return pack == null
			? null
			// We return only the first of the (maybe many) modified resources
			: splitTypeNames(fTypeNameDialogField.getText()).stream()
				.findFirst()
				.map(TypeNameVariants::nameWithoutParams)
				.map(this::getCompilationUnitName)
				.map(cuName -> pack.getCompilationUnit(cuName))
				.map(ICompilationUnit::getResource)
				.orElse(null);
	}

	// ----------- validation ----------

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jdt.ui.wizards.NewContainerWizardPage#containerChanged()
	 */
	@Override
	protected IStatus containerChanged() {
		IStatus status = super.containerChanged();
		IPackageFragmentRoot root = getPackageFragmentRoot();
		if ((fTypeKind == ANNOTATION_TYPE || fTypeKind == ENUM_TYPE) && !status.matches(IStatus.ERROR)) {
			if (root != null && !JavaModelUtil.is50OrHigher(root.getJavaProject())) {
				// error as createType will fail otherwise (bug 96928)
				return new StatusInfo(IStatus.ERROR, Messages.format(NewWizardMessages.NewTypeWizardPage_warning_NotJDKCompliant, BasicElementLabels.getJavaElementName(root.getJavaProject().getElementName())));
			}
			if (root != null && fTypeKind == ENUM_TYPE) {
				try {
					// if findType(...) == null then Enum is unavailable
					if (findType(root.getJavaProject(), "java.lang.Enum") == null) { //$NON-NLS-1$
						return new StatusInfo(IStatus.WARNING, NewWizardMessages.NewTypeWizardPage_warning_EnumClassNotFound);
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		}
		if ((fTypeKind == RECORD_TYPE) && !status.matches(IStatus.ERROR)) {
			if (root != null) {
				if (!JavaModelUtil.is16OrHigher(root.getJavaProject())) {
					return new StatusInfo(IStatus.ERROR, Messages.format(NewWizardMessages.NewTypeWizardPage_warning_NotJDKCompliant2, new String[] {BasicElementLabels.getJavaElementName(root.getJavaProject().getElementName()), "16" })); //$NON-NLS-1$
				}
				try {
					// if findType(...) == null then Record is unavailable
					if (findType(root.getJavaProject(), "java.lang.Record") == null) { //$NON-NLS-1$
						return new StatusInfo(IStatus.WARNING, NewWizardMessages.NewTypeWizardPage_warning_RecordClassNotFound);
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			} else {
				return new StatusInfo(IStatus.WARNING, NewWizardMessages.NewTypeWizardPage_warning_RecordClassNotFound);
			}
		}

		fCurrPackageCompletionProcessor.setPackageFragmentRoot(root);
		if (root != null) {
			fEnclosingTypeCompletionProcessor.setPackageFragment(root.getPackageFragment("")); //$NON-NLS-1$
		}
		return status;
	}

	/**
	 * A hook method that gets called when the package field has changed. The method validates the package name and
	 * returns the status of the validation. The validation also updates the package fragment model.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * @return the status of the validation
	 */
	protected IStatus packageChanged() {
		StatusInfo status = new StatusInfo();
		IPackageFragmentRoot root = getPackageFragmentRoot();
		fPackageDialogField.enableButton(root != null);
		IJavaProject project = root != null ? root.getJavaProject() : null;
		String packName = getPackageText();

		if (!packName.isEmpty()) {
			IStatus val = validatePackageName(packName, project);
			if (val.getSeverity() == IStatus.ERROR) {
				return status.withError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidPackageName,
						val.getMessage()));
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(Messages.format(NewWizardMessages.NewTypeWizardPage_warning_DiscouragedPackageName,
						val.getMessage()));
				// continue
			}
		} else {
			try {
				if (project != null && project.getModuleDescription() != null) {
					status.setError(NewWizardMessages.NewTypeWizardPage_error_PackageNameEmptyForModule);
				} else {
					status.setWarning(NewWizardMessages.NewTypeWizardPage_warning_DefaultPackageDiscouraged);
				}
			} catch (JavaModelException e) {
				status.setWarning(NewWizardMessages.NewTypeWizardPage_warning_DefaultPackageDiscouraged);
			}
		}

		if (project != null && root != null) {
			if (project.exists() && !packName.isEmpty()) {
				try {
					IPath rootPath = root.getPath();
					IPath outputPath = project.getOutputLocation();
					if (rootPath.isPrefixOf(outputPath) && !rootPath.equals(outputPath)) {
						// if the bin folder is inside of our root, don't allow to name a package
						// like the bin folder
						IPath packagePath = rootPath.append(packName.replace('.', '/'));
						if (outputPath.isPrefixOf(packagePath)) {
							return status.withError(NewWizardMessages.NewTypeWizardPage_error_ClashOutputLocation);
						}
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					// let pass
				}
			}

			fCurrPackage = root.getPackageFragment(packName);
			IResource resource = fCurrPackage.getResource();
			if (resource != null) {
				if (resource.isVirtual()) {
					return status.withError(NewWizardMessages.NewTypeWizardPage_error_PackageIsVirtual);
				}
				if (!ResourcesPlugin.getWorkspace().validateFiltered(resource).isOK()) {
					return status.withError(NewWizardMessages.NewTypeWizardPage_error_PackageNameFiltered);
				}
			}
		} else {
			status.setError(""); //$NON-NLS-1$
		}
		return status;
	}

	/**
	 * Updates the 'default' label next to the package field.
	 */
	private void updatePackageStatusLabel() {
		String msg = getPackageText().isEmpty() ? NewWizardMessages.NewTypeWizardPage_default : ""; //$NON-NLS-1$
		fPackageDialogField.setStatus(msg);
	}

	/**
	 * Updates the enable state of buttons related to the enclosing type selection checkbox.
	 */
	private void updateEnableState() {
		boolean enclosing = isEnclosingTypeSelected();
		fPackageDialogField.setEnabled(fCanModifyPackage && !enclosing);
		fEnclosingTypeDialogField.setEnabled(fCanModifyEnclosingType && enclosing);
		if (fTypeKind == ENUM_TYPE || fTypeKind == ANNOTATION_TYPE) {
			fOtherMdfButtons.enableSelectionButton(ABSTRACT_INDEX, enclosing);
			fOtherMdfButtons.enableSelectionButton(ENUM_ANNOT_STATIC_INDEX, enclosing);
		}
	}

	/**
	 * Hook method that gets called when the enclosing type name has changed. The method
	 * validates the enclosing type and returns the status of the validation. It also updates the enclosing type model.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * @return the status of the validation
	 */
	protected IStatus enclosingTypeChanged() {
		StatusInfo status = new StatusInfo();
		fCurrEnclosingType = null;
		IPackageFragmentRoot root = getPackageFragmentRoot();
		fEnclosingTypeDialogField.enableButton(root != null);

		if (root == null) {
			return status.withError(""); //$NON-NLS-1$
		}

		String enclName = getEnclosingTypeText();
		if (enclName.isEmpty()) {
			return status.withError(NewWizardMessages.NewTypeWizardPage_error_EnclosingTypeEnterName);
		}
		try {
			IType type = findType(root.getJavaProject(), enclName);
			if (type == null) {
				return status.withError(NewWizardMessages.NewTypeWizardPage_error_EnclosingTypeNotExists);
			}

			if (type.getCompilationUnit() == null) {
				return status.withError(NewWizardMessages.NewTypeWizardPage_error_EnclosingNotInCU);
			}
			if (!JavaModelUtil.isEditable(type.getCompilationUnit())) {
				return status.withError(NewWizardMessages.NewTypeWizardPage_error_EnclosingNotEditable);
			}

			fCurrEnclosingType = type;
			IPackageFragmentRoot enclosingRoot = JavaModelUtil.getPackageFragmentRoot(type);
			if (!enclosingRoot.equals(root)) {
				status.setWarning(NewWizardMessages.NewTypeWizardPage_warning_EnclosingNotInSourceFolder);
			}
			return status;
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return status.withError(NewWizardMessages.NewTypeWizardPage_error_EnclosingTypeNotExists);
		}
	}

	/**
	 * Finds the type by name.
	 * @param project Java project
	 * @param typeName Type name
	 * @return Type or <code>null</code>
	 * @throws JavaModelException In case of Java model problem
	 */
	private IType findType(IJavaProject project, String typeName) throws JavaModelException {
		return project.exists() ? project.findType(typeName) : null;
	}

	/**
	 * Answers the type name without parameters.
	 * @param typeNameWithParameters Type name with parameters
	 * @return Type name without parameters
	 */
	private static String getTypeNameWithoutParameters(String typeNameWithParameters) {
		int bracketOffset = typeNameWithParameters.indexOf('<');
		return bracketOffset == -1 ? typeNameWithParameters : typeNameWithParameters.substring(0, bracketOffset);
	}

	/**
	 * Hook method that is called when evaluating the name of the compilation unit to create. By default, a file
	 * extension <code>java</code> is added to the given type name, but implementors can override this behavior.
	 * @param typeName the name of the type to create the compilation unit for.
	 * @return the name of the compilation unit to be created for the given name
	 * @since 3.2
	 */
	protected String getCompilationUnitName(String typeName) {
		return typeName + JavaModelUtil.DEFAULT_CU_SUFFIX;
	}

	/**
	 * Hook method that gets called when the type name field has changed. The method validates the
	 * type name(s) and returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * @return Status of the validation
	 */
	protected IStatus typeNameChanged() {
		StatusInfo status = new StatusInfo();
		String typeNameWithExtension = fTypeNameDialogField.getText();
		// Must not be empty and must contain at least one letter
		if (typeNameWithExtension.isEmpty() || !MIN_ONE_LETTER_PATTERN.matcher(typeNameWithExtension).matches()) {
			return status.withError(NewWizardMessages.NewTypeWizardPage_error_EnterTypeName);
		}

		if (typeNameWithExtension.contains(JavaModelUtil.DEFAULT_CU_SUFFIX)) {
			status.setInfo(NewWizardMessages.NewTypeWizardPage_info_FileExtensionNotRequired);
		}

		IJavaProject project = getJavaProject();
		IJavaElement container = isEnclosingTypeSelected() ? getEnclosingType() : getPackageFragment();

		List<IStatus> valResults = splitTypeNames(typeNameWithExtension).stream()
			.map(tnv -> validateTypeName(tnv, project, container, status))
			.collect(Collectors.toList());

		valResults.add(status); // Add the status from above
		return valResults.stream()
			.sorted(Comparator.comparingInt(IStatus::getSeverity))
			.reduce((a, b) -> b) // To get the last element (which has highest severity)
			.orElse(status); // The else case cannot happen since we added a status to the list
	}

	/**
	 * Creates type name variants from the given parameter.
	 * @param typeNameWithExtension Type name with extension (if any) e. g. ".java"
	 * @param existing Existing type names (without parameters)
	 * @return Type name variants
	 */
	private TypeNameVariants createTypeNameVariants(String typeNameWithExtension, Collection<String> existing) {
		String typeNameWithParameters = getTypeNameWithoutExtension(typeNameWithExtension);
		String typeNameWithoutParameters = getTypeNameWithoutParameters(typeNameWithParameters);
		boolean duplicate = !existing.add(typeNameWithoutParameters);
		return new TypeNameVariants(typeNameWithParameters, typeNameWithoutParameters, duplicate);
	}

	/**
	 * Creates type name variants by splitting the type names.
	 * @param typeNamesWithExtension Type names with extension (if any) e. g. ".java" separated by comma.
	 * @return Type name variants
	 */
	private List<TypeNameVariants> splitTypeNames(String typeNamesWithExtension) {
		Collection<String> existing = new HashSet<>();
		// Using semicolon as separator. Can't use comma because of conflict with multiple type arguments like A<T, U>.
		return Arrays.stream(typeNamesWithExtension.split(";")) //$NON-NLS-1$ This is the separator
			.map(String::trim)
			.filter(str -> !str.isEmpty())
			.sorted()
			.map(typeNameWithExtension -> createTypeNameVariants(typeNameWithExtension, existing))
			// We do not reject the duplicates here because they are needed for validation purposes
			.collect(Collectors.toList());
	}

	/**
	 * Validates the type name and returns the status of the validation.
	 * @param tnv Type name variants
	 * @param project Java project
	 * @param container Containing Java element
	 * @param status Status info
	 * @return Status of the validation
	 */
	private IStatus validateTypeName(TypeNameVariants tnv, IJavaProject project, IJavaElement container,
			StatusInfo status) {

		if (tnv.duplicate()) {
			String name = tnv.nameWithoutParams();
			return status.withError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_DuplicateName, name));
		}

		String typeName = tnv.nameWithoutParams();
		if (typeName.indexOf('.') != -1) {
			return status.withError(NewWizardMessages.NewTypeWizardPage_error_QualifiedName);
		}

		IStatus val = validateJavaTypeName(typeName, project);
		String msg = val.getMessage();
		if (val.getSeverity() == IStatus.ERROR) {
			return status.withError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidTypeName, msg));
		} else if (val.getSeverity() == IStatus.WARNING) {
			status.setWarning(Messages.format(NewWizardMessages.NewTypeWizardPage_warning_TypeNameDiscouraged, msg));
			// continue checking
		}

		// must not exist
		if (isEnclosingTypeSelected() && container instanceof IType type && type.getType(typeName).exists()) {
			return status.withError(NewWizardMessages.NewTypeWizardPage_error_TypeNameExists);
		} else if (container instanceof IPackageFragment pack) {
			ICompilationUnit cu = pack.getCompilationUnit(getCompilationUnitName(typeName));
			IResource resource = cu.getResource();

			if (resource.exists()) {
				return status.withError(NewWizardMessages.NewTypeWizardPage_error_TypeNameExists);
			}
			if (!ResourcesPlugin.getWorkspace().validateFiltered(resource).isOK()) {
				return status.withError(NewWizardMessages.NewTypeWizardPage_error_TypeNameFiltered);
			}
			URI location = resource.getLocationURI();
			if (location != null) {
				try {
					if (EFS.getStore(location).fetchInfo().exists()) {
						return status.withError(NewWizardMessages.NewTypeWizardPage_error_TypeNameExistsDifferentCase);
					}
				} catch (CoreException e) {
					status.setError(Messages.format(
						NewWizardMessages.NewTypeWizardPage_error_uri_location_unkown,
						BasicElementLabels.getURLPart(Resources.getLocationString(resource))));
				}
			}
		}

		String typeNameWithParameters = tnv.nameWithParams();
		if (!typeNameWithParameters.equals(typeName) && project != null) {
			if (!JavaModelUtil.is50OrHigher(project)) {
				return status.withError(NewWizardMessages.NewTypeWizardPage_error_TypeParameters);
			}
			String typeDeclaration = String.format("class %s {}", typeNameWithParameters); //$NON-NLS-1$
			ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			parser.setSource(typeDeclaration.toCharArray());
			parser.setProject(project);
			CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
			IProblem[] problems = compilationUnit.getProblems();
			if (problems.length > 0) {
				return status.withError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidTypeName,
						problems[0].getMessage()));
			}
		}
		return status;
	}

	/**
	 * Hook method that gets called when the superclass name has changed. The method
	 * validates the superclass name and returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * @return the status of the validation
	 */
	protected IStatus superClassChanged() {
		StatusInfo status = new StatusInfo();
		IPackageFragmentRoot root = getPackageFragmentRoot();
		fSuperClassDialogField.enableButton(root != null);
		fSuperClassStubTypeContext = null;
		String sclassName = getSuperClass();

		if (sclassName.isEmpty()) {
			// accept the empty field (stands for java.lang.Object)
			return status;
		}
		if (fSuperClass != null) {
			try {
				if (fSuperClass.isRecord()) {
					return status.withError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidSuperClassRecord, BasicElementLabels.getJavaElementName(sclassName)));
				} else {
					int flags = fSuperClass.getFlags();
					if (Flags.isFinal(flags)) {
						return status.withError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidFinalSuperClass, BasicElementLabels.getJavaElementName(sclassName)));
					}
				}
			} catch (JavaModelException e) {
				//do nothing
			}
		}
		if (root != null) {
			Type type = TypeContextChecker.parseSuperClass(sclassName);
			if (type == null) {
				return status.withError(NewWizardMessages.NewTypeWizardPage_error_InvalidSuperClassName);
			}
			if (type instanceof ParameterizedType && ! JavaModelUtil.is50OrHigher(root.getJavaProject())) {
				return status.withError(NewWizardMessages.NewTypeWizardPage_error_SuperClassNotParameterized);
			}
		} else {
			status.setError(""); //$NON-NLS-1$
		}
		fSealedSuperClassStatus = validateSealedSuperClassStatus();
		enableSealedModifierButtonsIfRequired();
		return status;
	}

	/**
	 * Hook provided to get called when the super class get changed. This method validates
	 * if the sealed super class can be extended by this type
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 *
	 * @return the status of the validation
	 * @since 3.25
	 */
	private IStatus validateSealedSuperClassStatus() {
		StatusInfo status = new StatusInfo();
		if (fSuperClass != null) {
			StatusInfo validStatus = canSuperTypeBeExtended(fSuperClass);
			if (validStatus.isError()) {
				return validStatus;
			}
		}
		return status;
	}

	/**
	 * Enables sealed modifier buttons if required.
	 */
	private void enableSealedModifierButtonsIfRequired() {
		if (fSealedMdfButtons != null) {
			if (fIsSealedSupported) {
				if (fSealedSuperClassStatus != null && fSealedSuperClassStatus.isOK()
						&& fSealedSuperInterfacesStatus != null && fSealedSuperInterfacesStatus.isOK()
						&& isSuperTypeSealed()) {
					if (!fSealedMdfButtons.isEnabled(NON_SEALED_INDEX)) {
						fSealedMdfButtons.enableSelectionButton(NON_SEALED_INDEX, true);
					}
				} else {
					setSealedModifiersButtonDefault();
				}
			} else {
				setSealedModifiersButtonDefault();
			}
		}
	}

	/**
	 * Sets sealed modifier buttons default.
	 */
	private void setSealedModifiersButtonDefault() {
		if (fSealedMdfButtons != null) {
			if (isSealedButtonSelected(NON_SEALED_INDEX)) {
				fSealedMdfButtons.setSelection(0, true);
			}
			fSealedMdfButtons.enableSelectionButton(NON_SEALED_INDEX, false);
			fSealedMdfButtons.setSelection(NON_SEALED_INDEX, false);
			if (!fIsSealedSupported && isSealedButtonSelected(SEALED_INDEX)) {
				fSealedMdfButtons.enableSelectionButton(SEALED_INDEX, false);
				fSealedMdfButtons.setSelection(SEALED_INDEX, false);
				fSealedMdfButtons.setSelection(0, true);
			}
		}
	}

	/**
	 * Checks if sealed button is selected.
	 * @param index The index
	 * @return True if sealed button is selected
	 */
	private boolean isSealedButtonSelected(int index) {
		boolean selected = false;
		if (fSealedMdfButtons != null) {
			try {
				Button button = fSealedMdfButtons.getSelectionButton(index);
				if (button != null && button.isEnabled() && button.getSelection())  {
					selected = true;
				}
			} catch (NullPointerException npe) {
				//This will be hit when fSealedMdfButtons is not yet initialized.
				//do nothing
			}
		}
		return selected;
	}

	/**
	 * Checks if super type can be extended.
	 * @param type Super type
	 * @return True if super type can be extended
	 */
	private StatusInfo canSuperTypeBeExtended(IType type) {
		StatusInfo status = new StatusInfo();
		IJavaProject currJavaProject = getJavaProject();
		if (!fIsSealedSupported || type == null || currJavaProject == null) {
			return status;
		}
		try {
			if (type.isSealed()) {
				IJavaProject javaProject = type.getJavaProject();
				if (JavaModelUtil.is9OrHigher(currJavaProject)) {
					IModuleDescription projectModule = currJavaProject.getModuleDescription();
					if (projectModule != null && !projectModule.isAutoModule()) {
						if (!currJavaProject.equals(javaProject)) {
							String msg = NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperClassInDifferentModule;
							switch (fTypeKind) {
								case INTERFACE_TYPE:
									msg = NewWizardMessages.NewTypeWizardPage_error_interface_SealedSuperInterfaceInDifferentModule;
									break;
								case CLASS_TYPE:
									if (type.isInterface()) {
										msg = NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperInterfaceInDifferentModule;
									}
									break;
								default:
									break;
							}
							status.setError(msg);
						}
					} else if (fCurrPackage != null) {
						IPackageFragment scPkgFrag = type.getPackageFragment();
						if (!fCurrPackage.equals(scPkgFrag)) {
							if (currJavaProject.equals(javaProject)) {
								String msg = NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperClassInDifferentPackage;
								switch (fTypeKind) {
									case INTERFACE_TYPE:
										msg = NewWizardMessages.NewTypeWizardPage_error_interface_SealedSuperInterfaceInDifferentPackage;
										break;
									case CLASS_TYPE:
										if (type.isInterface()) {
											msg = NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperInterfaceInDifferentPackage;
										}
										break;
									default:
										break;
								}
								status.setError(msg);
							} else {
								String msg = NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperClassInDifferentProject;
								switch (fTypeKind) {
									case INTERFACE_TYPE:
										msg = NewWizardMessages.NewTypeWizardPage_error_interface_SealedSuperInterfaceInDifferentProject;
										break;
									case CLASS_TYPE:
										if (type.isInterface()) {
											msg = NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperInterfaceInDifferentProject;
										}
										break;
									default:
										break;
								}
								status.setError(msg);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			// do nothing
		}
		return status;
	}

	/**
	 * Checks if any of the super types is sealed or not.
	 * @return if any of the super types is sealed or not
	 * @since 3.25
	 */
	protected boolean isSuperTypeSealed() {
		boolean isSealed = false;
		if (fIsSealedSupported && fSuperClass != null) {
			try {
				if (fSuperClass.isSealed()) {
					isSealed = true;
				}
			} catch (JavaModelException e) {
				// do nothing
			}
		}
		if (!isSealed) {
			List<InterfaceWrapper> interfaces = fSuperInterfacesDialogField.getElements();
			for (InterfaceWrapper intWrap : interfaces) {
				try {
					IType type = intWrap.getType();
					if (type != null && type.isSealed()) {
						isSealed = true;
					}
				} catch (JavaModelException e) {
					//do nothing
				}
			}
		}
		return isSealed;
	}

	/**
	 * Checks if the super class is final.
	 * @return true if the super class is final
	 * @since 3.30
	 */
	protected boolean isSuperClassFinal() {
		try {
			return fSuperClass != null && Flags.isFinal(fSuperClass.getFlags());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Checks if the super class is sealed.
	 * @return True if the super class is sealed
	 */
	private boolean isSuperClassSealed() {
		boolean isSealed = false;
		if (fIsSealedSupported && fSuperClass != null) {
			try {
				if (fSuperClass.isSealed()) {
					isSealed = true;
				}
			} catch (JavaModelException e) {
				// do nothing
			}
		}
		return isSealed;
	}

	/**
	 * Checks if the valid sealed flag is selected.
	 * @return True if the valid sealed flag is selected
	 */
	private boolean isValidSealedFlagSelected() {
		if (fSealedMdfButtons == null) {
			return false;
		}
		return fSealedMdfButtons.isEnabled(SEALED_INDEX) && fSealedMdfButtons.isSelected(SEALED_INDEX)
			|| fSealedMdfButtons.isEnabled(NON_SEALED_INDEX) && fSealedMdfButtons.isSelected(NON_SEALED_INDEX)
			|| fTypeKind != INTERFACE_TYPE
				&& fSealedMdfButtons.isEnabled(SEALED_FINAL_INDEX)
				&& fSealedMdfButtons.isSelected(SEALED_FINAL_INDEX);
	}

	/**
	 * Gets the super class stub type context.
	 * @param typeName Type name
	 * @return The super class stub type context
	 */
	private StubTypeContext getSuperClassStubTypeContext(String typeName) {
		if (fSuperClassStubTypeContext == null) {
			String ctxTypeName = typeName == null ? JavaTypeCompletionProcessor.DUMMY_CLASS_NAME : typeName;
			fSuperClassStubTypeContext = TypeContextChecker.createSuperClassStubTypeContext(ctxTypeName,
					getEnclosingType(), getPackageFragment());
		}
		return fSuperClassStubTypeContext;
	}

	/**
	 * Hook method that gets called when the list of super interface has changed. The method
	 * validates the super interfaces and returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * @return the status of the validation
	 */
	protected IStatus superInterfacesChanged() {
		StatusInfo status = new StatusInfo();

		IPackageFragmentRoot root = getPackageFragmentRoot();
		fSuperInterfacesDialogField.enableButton(0, root != null);

		if (root != null) {
			List<InterfaceWrapper> elements = fSuperInterfacesDialogField.getElements();
			for (InterfaceWrapper element : elements) {
				String intfname = element.getInterfaceName();
				Type type = TypeContextChecker.parseSuperInterface(intfname);
				if (type == null) {
					return status.withError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidSuperInterfaceName, BasicElementLabels.getJavaElementName(intfname)));
				}
				if (type instanceof ParameterizedType && ! JavaModelUtil.is50OrHigher(root.getJavaProject())) {
					return status.withError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_SuperInterfaceNotParameterized, BasicElementLabels.getJavaElementName(intfname)));
				}
			}
		}
		fSealedSuperInterfacesStatus = validateSealedSuperInterfacesStatus();
		enableSealedModifierButtonsIfRequired();
		return status;
	}

	/**
	 * Hook provided to get called when the super interfaces get changed. This method validates
	 * if the sealed super interfaces can be extended/implemented by this type
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 *
	 * @return the status of the validation
	 * @since 3.25
	 */
	protected IStatus validateSealedSuperInterfacesStatus() {
		StatusInfo status = new StatusInfo();
		List<InterfaceWrapper> elements = fSuperInterfacesDialogField.getElements();
		for (InterfaceWrapper element : elements) {
			IType eType = element.getType();
			if (eType != null) {
				StatusInfo validStatus = canSuperTypeBeExtended(eType);
				if (validStatus.isError()) {
					status = validStatus;
				}
			}
		}
		return status;
	}

	/**
	 * Answers the super interfaces stub type context.
	 * @param typeName Type name
	 * @return Super interfaces stub type context
	 */
	private StubTypeContext getSuperInterfacesStubTypeContext(String typeName) {
		if (fSuperInterfaceStubTypeContext == null) {
			String ctxTypeName = typeName == null ? JavaTypeCompletionProcessor.DUMMY_CLASS_NAME : typeName;
			fSuperInterfaceStubTypeContext = TypeContextChecker.createSuperInterfaceStubTypeContext(ctxTypeName,
					getEnclosingType(), getPackageFragment());
		}
		return fSuperInterfaceStubTypeContext;
	}

	/**
	 * Hook method that gets called when the modifiers have changed. The method validates
	 * the modifiers and returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 *
	 * @return the status of the validation
	 */
	protected IStatus modifiersChanged() {
		StatusInfo status = new StatusInfo();
		int modifiers = getModifiers();
		if (Flags.isFinal(modifiers) && Flags.isAbstract(modifiers)) {
			status.setError(NewWizardMessages.NewTypeWizardPage_error_ModifiersFinalAndAbstract);
		}
		return status;
	}

	/**
	 * Hook method that gets called when the sealed modifiers have changed. The method validates
	 * the modifiers and returns the status of the validation.
	 * @return the status of the validation
	 */
	private IStatus sealedModifiersChanged() {
		StatusInfo status = new StatusInfo();
		if (isSuperTypeSealed() && !isValidSealedFlagSelected()) {
			String msg = NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_extend_superclass_notSelected_message;
			if (fTypeKind == INTERFACE_TYPE) {
				msg = NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedInterface_extend_superinterface_notSelected_message;
			} else if (!isSuperClassSealed()) {
				msg = NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_implement_superinterface_notSelected_message;
			}
			status.setError(msg);
		}
		return status;
	}

	// selection dialogs

	/**
	 * Opens a selection dialog that allows to select a package.
	 * @return returns the selected package or <code>null</code> if the dialog has been canceled.
	 * The caller typically sets the result to the package input field.
	 * <p>
	 * Clients can override this method if they want to offer a different dialog.
	 * </p>
	 * @since 3.2
	 */
	protected IPackageFragment choosePackage() {
		IPackageFragmentRoot froot = getPackageFragmentRoot();
		IJavaElement[] packages = null;
		try {
			if (froot != null && froot.exists()) {
				packages = froot.getChildren();
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		if (packages == null) {
			packages = new IJavaElement[0];
		}

		JavaElementLabelProvider provider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		var dialog = new ElementListSelectionDialog(getShell(), provider);
		dialog.setIgnoreCase(false);
		dialog.setTitle(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_title);
		dialog.setMessage(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_description);
		dialog.setEmptyListMessage(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_empty);
		dialog.setElements(packages);
		dialog.setHelpAvailable(false);

		IPackageFragment pack = getPackageFragment();
		if (pack != null) {
			dialog.setInitialSelections(pack);
		}

		return dialog.open() == Window.OK ? (IPackageFragment) dialog.getFirstResult() : null;
	}

	/**
	 * Opens a selection dialog that allows to select an enclosing type.
	 * @return returns the selected type or <code>null</code> if the dialog has been canceled.
	 * The caller typically sets the result to the enclosing type input field.
	 * <p>
	 * Clients can override this method if they want to offer a different dialog.
	 * </p>
	 * @since 3.2
	 */
	protected IType chooseEnclosingType() {
		IPackageFragmentRoot root = getPackageFragmentRoot();
		if (root == null) {
			return null;
		}

		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { root });
		IWizardContainer container = getWizard().getContainer();
		var dialog = new FilteredTypesSelectionDialog(getShell(), false, container, scope, IJavaSearchConstants.TYPE);
		dialog.setTitle(NewWizardMessages.NewTypeWizardPage_ChooseEnclosingTypeDialog_title);
		dialog.setMessage(NewWizardMessages.NewTypeWizardPage_ChooseEnclosingTypeDialog_description);
		dialog.setInitialPattern(Signature.getSimpleName(getEnclosingTypeText()));
		return dialog.open() == Window.OK ? (IType) dialog.getFirstResult() : null;
	}

	/**
	 * Opens a selection dialog that allows to select a super class.
	 * @return returns the selected type or <code>null</code> if the dialog has been canceled.
	 * The caller typically sets the result to the super class input field.
	 * 	<p>
	 * Clients can override this method if they want to offer a different dialog.
	 * </p>
	 * @since 3.2
	 */
	protected IType chooseSuperClass() {
		IJavaProject project = getJavaProject();
		if (project == null) {
			return null;
		}

		IJavaElement[] elements = new IJavaElement[] { project };
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements);
		IWizardContainer container = getWizard().getContainer();
		var dialog = new FilteredTypesSelectionDialog(getShell(), false, container, scope, IJavaSearchConstants.CLASS);
		dialog.setTitle(NewWizardMessages.NewTypeWizardPage_SuperClassDialog_title);
		dialog.setMessage(NewWizardMessages.NewTypeWizardPage_SuperClassDialog_message);
		dialog.setInitialPattern(getSuperClass());
		return dialog.open() == Window.OK ? (IType) dialog.getFirstResult() : null;
	}

	/**
	 * Sets the super class type.
	 */
	private void setSuperClassType() {
		if (this.fResetSuperClass) {
			fSuperClass = null;
			IJavaProject project = getJavaProject();
			if (project == null) {
				return;
			}
			try {
				this.fSuperClass = project.findType(getSuperClass());
			} catch (JavaModelException e) {
				//do nothing
			}
		}
	}

	/**
	 * Opens a selection dialog that allows to select the super interfaces. The selected interfaces are
	 * directly added to the wizard page using {@link #addSuperInterface(String)}.
	 * 	<p>
	 * Clients can override this method if they want to offer a different dialog.
	 * </p>
	 * @since 3.2
	 */
	protected void chooseSuperInterfaces() {
		IJavaProject project = getJavaProject();
		if (project == null) {
			return;
		}

		var dialog = new SuperInterfaceSelectionDialog(getShell(), getWizard().getContainer(), this, project);
		dialog.setTitle(getInterfaceDialogTitle());
		dialog.setMessage(NewWizardMessages.NewTypeWizardPage_InterfacesDialog_message);
		dialog.open();
	}

	/**
	 * Answers the interface dialog title.
	 * @return Interface dialog title
	 */
	private String getInterfaceDialogTitle() {
		return fTypeKind == INTERFACE_TYPE
			? NewWizardMessages.NewTypeWizardPage_InterfacesDialog_interface_title
			: NewWizardMessages.NewTypeWizardPage_InterfacesDialog_class_title;
	}

	// ---- creation ----------------

	/**
	 * Creates the new type(s) using the entered field values.
	 * @param monitor Progress monitor to report progress
	 * @throws CoreException Thrown when the creation failed
	 * @throws InterruptedException Thrown when the operation was canceled
	 */
	public void createType(IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		monitor.beginTask(NewWizardMessages.NewTypeWizardPage_operationdesc, 2);
		IPackageFragmentRoot root = getPackageFragmentRoot();
		IPackageFragment pack = getPackageFragment();

		if (pack == null) {
			pack = root.getPackageFragment(""); //$NON-NLS-1$
		}

		if (!pack.exists()) {
			String packName = pack.getElementName();
			pack = root.createPackageFragment(packName, true, Progress.subMonitor(monitor, 1));
		} else {
			monitor.worked(1);
		}

		try {
			boolean isInnerClass = isEnclosingTypeSelected();
			List<TypeNameVariants> tnvs = splitTypeNames(fTypeNameDialogField.getText());

			// Following is not done with a stream because of the declared exceptions
			List<IType> createdTypes = new ArrayList<>();
			for (TypeNameVariants tnv : tnvs) {
				createdTypes.add(createType(tnv, isInnerClass, pack, Progress.subMonitor(monitor, 1)));
			}
			fCreatedTypes = createdTypes; // Set the list of created types
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the new type using the given values.
	 * @param tnv Type name variants
	 * @param isInnerClass True if inner class
	 * @param pack Package fragment
	 * @param monitor a progress monitor to report progress
	 * @return Created type
	 * @throws CoreException Thrown when the creation failed
	 * @throws InterruptedException Thrown when the operation was canceled
	 */
	private IType createType(TypeNameVariants tnv, boolean isInnerClass, IPackageFragment pack,
			IProgressMonitor monitor) throws CoreException, InterruptedException {

		ICompilationUnit connectedCU = null;
		IType createdType = null;

		try {
			String typeName = tnv.nameWithoutParams();

			IType currType = null; // Handle to the type to be created (does usually not exist, can be null)
			if (isInnerClass) {
				IType type = getEnclosingType();
				if (type != null) {
					currType = type.getType(typeName);
				}
			} else {
				ICompilationUnit cu = pack.getCompilationUnit(getCompilationUnitName(typeName));
				currType = cu.getType(typeName);
			}

			TypeCreationResult tcr = isInnerClass
				? createInnerType(currType, tnv, monitor)
				: createNonInnerType(currType, tnv, pack, monitor);

			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}

			// add imports for superclass/interfaces, so types can be resolved correctly
			createdType = tcr.createdType();
			ICompilationUnit cu = createdType.getCompilationUnit();
			ImportsManager imports = tcr.imports();
			imports.create(false, Progress.subMonitor(monitor, 1));
			JavaModelUtil.reconcile(cu);

			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}

			// set up again
			CompilationUnit astRoot = createASTForImports(imports.getCompilationUnit());
			imports = new ImportsManager(astRoot);

			createTypeMembers(createdType, imports, Progress.subMonitor(monitor, 1));

			// add imports
			imports.create(false, Progress.subMonitor(monitor, 1));
			removeUnusedImports(cu, tcr.existingImports(), false);
			JavaModelUtil.reconcile(cu);

			ISourceRange range = createdType.getSourceRange();
			int indent = isInnerClass ? StubUtility.getIndentUsed(getEnclosingType()) + 1 : 0;
			IBuffer buf = cu.getBuffer();
			String originalContent = buf.getText(range.getOffset(), range.getLength());
			String lineDelimiter = tcr.lineDelimiter();

			String formattedContent = CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, originalContent,
					indent, lineDelimiter, FormatterProfileManager.getProjectSettings(pack.getJavaProject()));

			formattedContent = Strings.trimLeadingTabsAndSpaces(formattedContent);
			buf.replace(range.getOffset(), range.getLength(), formattedContent);
			if (!isInnerClass) {
				String fileComment = getFileComment(cu);
				if (fileComment != null && !fileComment.isEmpty()) {
					buf.replace(0, 0, fileComment + lineDelimiter);
				}
			}

			if (tcr.needsSave()) {
				cu.commitWorkingCopy(true, Progress.subMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}

			updateSealedSuperTypes(createdType, typeName);
			connectedCU = tcr.connectedCU();
		} finally {
			if (connectedCU != null) {
				connectedCU.discardWorkingCopy();
			}
		}
		return createdType;
	}

	/**
	 * Creates an inner type.
	 * @param currentType Current type
	 * @param tnv Type name variants
	 * @param monitor Progress monitor
	 * @return Type creation result
	 * @throws CoreException Thrown when the creation failed
	 */
	private TypeCreationResult createInnerType(IType currentType, TypeNameVariants tnv, IProgressMonitor monitor)
			throws CoreException {

		IType encType = getEnclosingType();
		ICompilationUnit parentCU = encType.getCompilationUnit();
		final boolean needsSave = !parentCU.isWorkingCopy(); // Must be before next line, do not move this further down
		parentCU.becomeWorkingCopy(Progress.subMonitor(monitor, 1)); // cu is now for sure (primary) a working copy

		CompilationUnit astRoot = createASTForImports(parentCU);
		ImportsManager imports = new ImportsManager(astRoot);

		// add imports that will be removed again. Having the imports solves 14661
		for (IType topLevelType : parentCU.getTypes()) {
			imports.addImport(topLevelType.getFullyQualifiedName('.'));
		}

		String lineDelimiter = StubUtility.getLineDelimiterUsed(encType);
		StringBuilder content = new StringBuilder();

		String comment = getTypeComment(tnv.nameWithoutParams, parentCU, lineDelimiter);
		if (comment != null) {
			content.append(comment);
			content.append(lineDelimiter);
		}

		content.append(constructTypeStub(currentType, tnv, parentCU, imports, lineDelimiter));
		IJavaElement sibling = null;
		if (encType.isEnum()) {
			for (IField field : encType.getFields()) {
				if (!field.isEnumConstant()) {
					sibling = field;
					break;
				}
			}
		} else {
			IJavaElement[] elems = encType.getChildren();
			sibling = elems.length > 0 ? elems[0] : null;
		}

		IType createdType = encType.createType(content.toString(), sibling, false, Progress.subMonitor(monitor, 2));
		Collection<String> existingImports = getExistingImports(astRoot);
		return new TypeCreationResult(createdType, imports, existingImports, parentCU, needsSave, lineDelimiter);
	}

	/**
	 * Creates an inner type.
	 * @param currentType Current type
	 * @param tnv Type name variants
	 * @param pack Package fragment
	 * @param monitor Progress monitor
	 * @return Type creation result
	 * @throws CoreException Thrown when the creation failed
	 */
	private TypeCreationResult createNonInnerType(IType currentType, TypeNameVariants tnv, IPackageFragment pack,
			IProgressMonitor monitor) throws CoreException {

		String lineDelimiter = StubUtility.getLineDelimiterUsed(pack.getJavaProject());
		String cuName = getCompilationUnitName(tnv.nameWithoutParams);
		var parentCU = pack.createCompilationUnit(cuName, "", false, Progress.subMonitor(monitor, 2)); //$NON-NLS-1$

		// create a working copy with a new owner
		parentCU.becomeWorkingCopy(Progress.subMonitor(monitor, 1)); // cu is now a (primary) working copy
		IBuffer buffer = parentCU.getBuffer();
		String simpleTypeStub = constructSimpleTypeStub(tnv.nameWithoutParams);
		String cuContent = constructCUContent(tnv.nameWithoutParams, parentCU, simpleTypeStub, lineDelimiter);
		buffer.setContents(cuContent);

		CompilationUnit astRoot = createASTForImports(parentCU);
		Collection<String> existingImports = getExistingImports(astRoot);

		ImportsManager imports = new ImportsManager(astRoot);
		// add an import that will be removed again. Having this import solves 14661
		imports.addImport(JavaModelUtil.concatenateName(pack.getElementName(), tnv.nameWithoutParams));

		String typeContent = constructTypeStub(currentType, tnv, parentCU, imports, lineDelimiter);

		int index = cuContent.lastIndexOf(simpleTypeStub);
		if (index == -1) {
			AbstractTypeDeclaration typeNode = (AbstractTypeDeclaration) astRoot.types().get(0);
			int start = ((ASTNode) typeNode.modifiers().get(0)).getStartPosition();
			int end = typeNode.getStartPosition() + typeNode.getLength();
			buffer.replace(start, end - start, typeContent);
		} else {
			buffer.replace(index, simpleTypeStub.length(), typeContent);
		}

		IType createdType = parentCU.getType(tnv.nameWithoutParams);
		return new TypeCreationResult(createdType, imports, existingImports, parentCU, true, lineDelimiter);
	}

	/**
	 * Creates the AST for imports.
	 * @param cu Compilation unit
	 * @return AST for imports
	 */
	private CompilationUnit createASTForImports(ICompilationUnit cu) {
		ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		parser.setFocalPosition(0);
		return (CompilationUnit) parser.createAST(null);
	}

	/**
	 * Gets the existing imports.
	 * @param root Compilation unit
	 * @return Existing imports
	 */
	private Collection<String> getExistingImports(CompilationUnit root) {
		List<ImportDeclaration> imports = root.imports();
		return imports.stream()
			.map(ASTNodes::asString)
			.collect(Collectors.toSet());
	}

	/**
	 * Removes unused imports.
	 * @param cu Compilation unit
	 * @param existingImports Existing imports
	 * @param needsSave True if saving required
	 * @throws CoreException If removing fails
	 */
	private void removeUnusedImports(ICompilationUnit cu, Collection<String> existingImports, boolean needsSave)
			throws CoreException {

		ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(cu);
		parser.setResolveBindings(true);

		CompilationUnit root = (CompilationUnit) parser.createAST(null);
		if (root.getProblems().length == 0) {
			return;
		}

		List<ImportDeclaration> importsDecls = root.imports();
		if (importsDecls.isEmpty()) {
			return;
		}

		ImportsManager imports = new ImportsManager(root);
		int importsEnd = ASTNodes.getExclusiveEnd(importsDecls.get(importsDecls.size() - 1));
		for (IProblem curr : root.getProblems()) {
			if (curr.getSourceEnd() < importsEnd) {
				int id = curr.getID();
				if (id == IProblem.UnusedImport || id == IProblem.NotVisibleType) {
					// not visible problems hide unused -> remove both
					int pos = curr.getSourceStart();
					for (ImportDeclaration decl : importsDecls) {
						if (decl.getStartPosition() <= pos && pos < decl.getStartPosition() + decl.getLength()) {
							if (existingImports.isEmpty() || !existingImports.contains(ASTNodes.asString(decl))) {
								String name = decl.getName().getFullyQualifiedName();
								if (decl.isOnDemand()) {
									name += ".*"; //$NON-NLS-1$
								}
								if (decl.isStatic()) {
									imports.removeStaticImport(name);
								} else {
									imports.removeImport(name);
								}
							}
							break;
						}
					}
				}
			}
		}
		imports.create(needsSave, null);
	}

	/**
	 * Uses the New Java file template from the code template page to generate a
	 * compilation unit with the given type content.
	 * @param typeName Type name
	 * @param cu The new created compilation unit
	 * @param typeContent The content of the type, including signature and type body.
	 * @param lineDelim The line delimiter to be used.
	 * @return String Returns the result of evaluating the new file template with the given type content.
	 * @throws CoreException when fetching the file comment fails or fetching the content for the
	 * new compilation unit fails
	 * @since 3.33
	 */
	protected String constructCUContent(String typeName, ICompilationUnit cu, String typeContent, String lineDelim)
			throws CoreException {

		String fileComment = getFileComment(cu, lineDelim);
		String typeComment = getTypeComment(typeName, cu, lineDelim);
		IPackageFragment pack = (IPackageFragment) cu.getParent();
		String content = CodeGeneration.getCompilationUnitContent(cu, fileComment, typeComment, typeContent, lineDelim);
		if (content != null) {
			ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			parser.setProject(cu.getJavaProject());
			parser.setSource(content.toCharArray());
			CompilationUnit unit = (CompilationUnit) parser.createAST(null);
			if ((pack.isDefaultPackage() || unit.getPackage() != null) && !unit.types().isEmpty()) {
				return content;
			}
		}
		StringBuilder buf = new StringBuilder();
		if (!pack.isDefaultPackage()) {
			buf.append("package ").append(pack.getElementName()).append(';'); //$NON-NLS-1$
		}
		buf.append(lineDelim).append(lineDelim);
		if (typeComment != null) {
			buf.append(typeComment).append(lineDelim);
		}
		buf.append(typeContent);
		return buf.toString();
	}

	/**
	 * Returns the created type or <code>null</code> if the type has not been created yet. The method
	 * only returns a valid type after <code>createType</code> has been called.
	 * @return the created type or <code>null</code>
	 * @see #createType(IProgressMonitor)
	 */
	public IType getCreatedType() {
		return fCreatedTypes.stream()
			.findFirst()
			.orElse(null);
	}

	// ---- construct CU body----------------

	/**
	 * Writes the super class.
	 * @param currentType Current type
	 * @param typeName Type name
	 * @param buf Write buffer
	 * @param imports Imports manager
	 */
	private void writeSuperClass(IType currentType, String typeName, StringBuffer buf, ImportsManager imports) {
		String superclass = getSuperClass();
		if (fTypeKind == CLASS_TYPE && !superclass.isEmpty() && !"java.lang.Object".equals(superclass)) { //$NON-NLS-1$
			buf.append(" extends "); //$NON-NLS-1$

			ITypeBinding binding = null;
			if (currentType != null) {
				StubTypeContext superClassStubTypeContext = getSuperClassStubTypeContext(typeName);
				binding = TypeContextChecker.resolveSuperClass(superclass, currentType, superClassStubTypeContext);
			}
			buf.append(binding == null ? imports.addImport(superclass) : imports.addImport(binding));
		}
	}

	/**
	 * Writes the super interfaces.
	 * @param currentType Current type
	 * @param typeName Type name
	 * @param buf Write buffer
	 * @param imports Imports manager
	 */
	private void writeSuperInterfaces(IType currentType, String typeName, StringBuffer buf, ImportsManager imports) {
		List<String> interfaces = getSuperInterfaces();
		int last = interfaces.size() - 1;
		if (last >= 0) {
			buf.append(fTypeKind == INTERFACE_TYPE ? " extends " : " implements ");  //$NON-NLS-1$  //$NON-NLS-2$
			String[] intfs = interfaces.toArray(new String[interfaces.size()]);
			ITypeBinding[] bindings;
			if (currentType != null) {
				StubTypeContext superInterfacesStubTypeCtx = getSuperInterfacesStubTypeContext(typeName);
				bindings = TypeContextChecker.resolveSuperInterfaces(intfs, currentType, superInterfacesStubTypeCtx);
			} else {
				bindings = new ITypeBinding[intfs.length];
			}
			for (int i = 0; i <= last; i++) {
				ITypeBinding binding = bindings[i];
				if (binding != null) {
					buf.append(imports.addImport(binding));
				} else {
					buf.append(imports.addImport(intfs[i]));
				}
				if (i < last) {
					buf.append(',');
				}
			}
		}
	}

	/**
	 * Constructs a simple type stub.
	 * @param typeName Type name
	 * @return A simple type stub
	 */
	private String constructSimpleTypeStub(String typeName) {
		return String.format("public class %s{}", typeName); //$NON-NLS-1$
	}

	/**
	 * Called from createType to construct the source for this type.
	 * @param currentType Current type
	 * @param tnv Type name variants
	 * @param parentCU Parent compilation unit
	 * @param imports Imports manager
	 * @param lineDelimiter Line delimiter to use
	 * @return Type stub
	 * @throws CoreException Thrown when the creation failed
	 */
	private String constructTypeStub(IType currentType, TypeNameVariants tnv, ICompilationUnit parentCU,
			ImportsManager imports, String lineDelimiter) throws CoreException {

		StringBuffer buf = new StringBuffer();
		int modifiers = getModifiers();
		buf.append(Flags.toString(modifiers));
		if (modifiers != 0) {
			buf.append(' ');
		}
		String type = ""; //$NON-NLS-1$
		String templateID = ""; //$NON-NLS-1$
		switch (fTypeKind) {
			case CLASS_TYPE:
				type = "class ";  //$NON-NLS-1$
				templateID = CodeGeneration.CLASS_BODY_TEMPLATE_ID;
				break;
			case INTERFACE_TYPE:
				type = "interface "; //$NON-NLS-1$
				templateID = CodeGeneration.INTERFACE_BODY_TEMPLATE_ID;
				break;
			case ENUM_TYPE:
				type = "enum "; //$NON-NLS-1$
				templateID = CodeGeneration.ENUM_BODY_TEMPLATE_ID;
				break;
			case ANNOTATION_TYPE:
				type = "@interface "; //$NON-NLS-1$
				templateID = CodeGeneration.ANNOTATION_BODY_TEMPLATE_ID;
				break;
			case RECORD_TYPE:
				type = "record "; //$NON-NLS-1$
				templateID = CodeGeneration.RECORD_BODY_TEMPLATE_ID;
				break;
			default:
				// Do nothing
		}
		buf.append(type);
		buf.append(tnv.nameWithParams);
		if (fTypeKind == RECORD_TYPE) {
			buf.append("()"); //$NON-NLS-1$
		}
		writeSuperClass(currentType, tnv.nameWithoutParams, buf, imports);
		writeSuperInterfaces(currentType, tnv.nameWithoutParams, buf, imports);

		buf.append(" {").append(lineDelimiter); //$NON-NLS-1$
		String typeBody = CodeGeneration.getTypeBody(templateID, parentCU, tnv.nameWithoutParams, lineDelimiter);
		if (typeBody != null) {
			buf.append(typeBody);
		} else {
			buf.append(lineDelimiter);
		}
		buf.append('}').append(lineDelimiter);
		return buf.toString();
	}

	/**
	 * Hook method that gets called from <code>createType</code> to support adding of
	 * unanticipated methods, fields, and inner types to the created type.
	 * <p>
	 * Implementers can use any methods defined on <code>IType</code> to manipulate the new type.
	 * </p>
	 * <p>
	 * The source code of the new type will be formatted using the platform's formatter. Needed
	 * imports are added by the wizard at the end of the type creation process using the given
	 * import manager.
	 * </p>
	 *
	 * @param newType the new type created via <code>createType</code>
	 * @param imports an import manager which can be used to add new imports
	 * @param monitor a progress monitor to report progress. Must not be <code>null</code>
	 * @throws CoreException thrown when creation of the type members failed
	 * @see #createType(IProgressMonitor)
	 */
	protected void createTypeMembers(IType newType, ImportsManager imports, IProgressMonitor monitor)
			throws CoreException {

		// default implementation does nothing
		// example would be
		// String mainMathod = "public void foo(Vector vec) {}"
		// createdType.createMethod(main, null, false, null);
		// imports.addImport("java.lang.Vector");
	}

	/**
	 * Gets the file template or <code>null</code>.
	 * @param parentCU the current compilation unit
	 * @return returns the file template or <code>null</code>
	 * @deprecated Instead of file templates, the new type code template specifies the stub for a compilation unit.
	 */
	@Deprecated
	protected String getFileComment(ICompilationUnit parentCU) {
		return null;
	}

	/**
	 * Hook method that gets called from <code>createType</code> to retrieve a file comment. This default
	 * implementation returns the content of the 'file comment' template or <code>null</code> if no comment should
	 * be created.
	 * @param parentCU the parent compilation unit
	 * @param lineDelimiter the line delimiter to use
	 * @return the file comment or <code>null</code> if a file comment is not desired
	 * @throws CoreException when fetching the file comment fails
	 * @since 3.1
	 */
	protected String getFileComment(ICompilationUnit parentCU, String lineDelimiter) throws CoreException {
		return isAddComments() ? CodeGeneration.getFileComment(parentCU, lineDelimiter) : null;
	}

	/**
	 * Checks if the input is a valid comment.
	 * @param template Input to check
	 * @return True if the input is a valid comment
	 */
	private boolean isValidComment(String template) {
		IScanner scanner = ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(template.toCharArray());
		try {
			int next = scanner.getNextToken();
			while (TokenScanner.isComment(next)) {
				next = scanner.getNextToken();
			}
			return next == ITerminalSymbols.TokenNameEOF;
		} catch (InvalidInputException e) {
			return false;
		}
	}

	/**
	 * Hook method that gets called from <code>createType</code> to retrieve a type comment. This default
	 * implementation returns the content of the 'type comment' template.
	 * @param typeName Type name
	 * @param parentCU the parent compilation unit
	 * @param lineDelim the line delimiter to use
	 * @return the type comment or <code>null</code> if a type comment is not desired
	 * @since 3.33
	 */
	protected String getTypeComment(String typeName, ICompilationUnit parentCU, String lineDelim) {
		if (!isAddComments()) {
			return null;
		}
		try {
			StringBuilder typeNameSB = new StringBuilder();
			if (isEnclosingTypeSelected()) {
				typeNameSB.append(getEnclosingType().getTypeQualifiedName('.')).append('.');
			}
			typeNameSB.append(typeName);
			String comment = CodeGeneration.getTypeComment(parentCU, typeNameSB.toString(), new String[0], lineDelim);
			if (comment != null && isValidComment(comment)) {
				return comment;
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Answers the template.
	 * @param name the name of the template
	 * @param parentCU the current compilation unit
	 * @return returns the template or <code>null</code>
	 * @deprecated Use getTemplate(String,ICompilationUnit,int)
	 */
	@Deprecated
	protected String getTemplate(String name, ICompilationUnit parentCU) {
		return getTemplate(name, parentCU, 0);
	}

	/**
	 * Returns the string resulting from evaluation the given template in the context of the given compilation unit.
	 * This accesses the normal template page, not the code templates. To use code templates use
	 * <code>constructCUContent</code> to construct a compilation unit stub or getTypeComment for the type comment.
	 *
	 * @param name the template to be evaluated
	 * @param parentCU the templates evaluation context
	 * @param pos Source offset into the parent compilation unit. The template is evaluated at the given source offset.
	 * @return return the template with the given name or <code>null</code> if the template could not be found.
	 */
	protected String getTemplate(String name, ICompilationUnit parentCU, int pos) {
		try {
			Template template = JavaPlugin.getDefault().getTemplateStore().findTemplate(name);
			if (template != null) {
				return TemplateUtils.evaluateTemplate(template, parentCU, pos);
			}
		} catch (CoreException | BadLocationException | TemplateException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Creates the stub for 'public static void main(String[] args)' method.
	 * @param type the type for which the main method is to be created
	 * @param imports an import manager to add all needed import statements
	 * @return the created method.
	 * @throws CoreException thrown when the creation fails.
	 * @since 3.25
	 */
	protected IMethod createMainMethod(IType type, ImportsManager imports) throws CoreException {
		if (type == null) {
			return null;
		}
		StringBuilder buf = new StringBuilder();
		final String lineDelim = "\n"; // OK, since content is formatted afterwards //$NON-NLS-1$
		ICompilationUnit cu = type.getCompilationUnit();
		String qName = type.getTypeQualifiedName('.');
		if (isAddComments()) {
			String comment = CodeGeneration.getMethodComment(cu, qName, "main", new String[] { "args" }, new String[0], Signature.createTypeSignature("void", true), null, lineDelim); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (comment != null) {
				buf.append(comment);
				buf.append(lineDelim);
			}
		}
		buf.append("public static void main("); //$NON-NLS-1$
		buf.append(imports.addImport("java.lang.String")); //$NON-NLS-1$
		buf.append("[] args) {"); //$NON-NLS-1$
		buf.append(lineDelim);
		String content = CodeGeneration.getMethodBodyContent(cu, qName, "main", false, "", lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
		if (content != null && !content.isEmpty()) {
			buf.append(content);
		}
		buf.append(lineDelim);
		buf.append("}"); //$NON-NLS-1$
		return type.createMethod(buf.toString(), null, false, null);
	}

	/**
	 * Creates the bodies of all unimplemented methods and constructors and adds them to the type.
	 * Method is typically called by implementers of <code>NewTypeWizardPage</code> to add
	 * needed method and constructors.
	 *
	 * @param type the type for which the new methods and constructor are to be created
	 * @param doConstructors if <code>true</code> unimplemented constructors are created
	 * @param doUnimplementedMethods if <code>true</code> unimplemented methods are created
	 * @param imports an import manager to add all needed import statements
	 * @param monitor a progress monitor to report progress
	 * @return the created methods.
	 * @throws CoreException thrown when the creation fails.
	 */
	protected IMethod[] createInheritedMethods(IType type, boolean doConstructors, boolean doUnimplementedMethods,
			ImportsManager imports, IProgressMonitor monitor) throws CoreException {

		SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
		final ICompilationUnit cu = type.getCompilationUnit();
		JavaModelUtil.reconcile(cu);
		IMethod[] typeMethods = type.getMethods();
		Set<String> handleIds = new HashSet<>(typeMethods.length);
		for (IMethod typeMethod : typeMethods) {
			handleIds.add(typeMethod.getHandleIdentifier());
		}
		CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject());
		settings.createComments = isAddComments();
		ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setSource(cu);
		CompilationUnit unit = (CompilationUnit) parser.createAST(subMonitor.newChild(1));
		final ITypeBinding binding = ASTNodes.getTypeBinding(unit, type);
		if (binding != null) {
			if (doUnimplementedMethods) {
				var operation = new AddUnimplementedMethodsOperation(unit, binding, null, -1, false, true, false);
				operation.setCreateComments(isAddComments());
				operation.run(subMonitor.newChild(1));
				createImports(imports, operation.getCreatedImports());
			}
			if (doConstructors) {
				var operation = new AddUnimplementedConstructorsOperation(unit, binding, null, -1, false, true, false,
						FormatterProfileManager.getProjectSettings(type.getJavaProject()));
				operation.setOmitSuper(true);
				operation.setCreateComments(isAddComments());
				operation.run(subMonitor.newChild(1));
				createImports(imports, operation.getCreatedImports());
			}
		}
		JavaModelUtil.reconcile(cu);
		ArrayList<IMethod> newMethods = new ArrayList<>();
		for (IMethod typeMethod : type.getMethods()) {
			if (!handleIds.contains(typeMethod.getHandleIdentifier())) {
				newMethods.add(typeMethod);
			}
		}
		IMethod[] methods = new IMethod[newMethods.size()];
		newMethods.toArray(methods);
		return methods;
	}

	/**
	 * Creates/adds imports.
	 * @param imports Imports manager
	 * @param createdImports Imports to add
	 */
	private void createImports(ImportsManager imports, String[] createdImports) {
		Arrays.stream(createdImports).forEach(imports::addImport);
	}

	/**
	 * Creates a type binding for the given type.
	 * @param type The type
	 * @return Created type binding
	 */
	private ITypeBinding createTypeBinding(IType type) {
		if (type == null) {
			return null;
		}
		ASTParser parser2 = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser2.setResolveBindings(true);
		parser2.setSource(type.getCompilationUnit());
		CompilationUnit unit = (CompilationUnit) parser2.createAST(new NullProgressMonitor());
		try {
			TypeDeclaration typeDecl = ASTNodeSearchUtil.getTypeDeclarationNode(type, unit);
			return typeDecl == null ? null : typeDecl.resolveBinding();
		} catch (JavaModelException e) {
			return null;
		}
	}

	/**
	 * Updates sealed superclass.
	 * @param createdType Created type
	 * @param typeName Type name
	 */
	private void updateSealedSuperClass(IType createdType, String typeName) {
		updateSealedSuperType(createdType, typeName, fSuperClass);
	}

	/**
	 * Updates sealed super interfaces.
	 * @param createdType Created type
	 * @param typeName Type name
	 */
	private void updateSealedSuperInterfaces(IType createdType, String typeName) {
		fSuperInterfacesDialogField.getElements().forEach(
				iw -> updateSealedSuperType(createdType, typeName, iw.getType()));
	}

	/**
	 * Updates sealed super types.
	 * @param createdType Created type
	 * @param typeName Type name
	 */
	private void updateSealedSuperTypes(IType createdType, String typeName) {
		if (createdType != null && isSuperTypeSealed()) {
			updateSealedSuperClass(createdType, typeName);
			updateSealedSuperInterfaces(createdType, typeName);
		}
	}

	/**
	 * Checks if the super type is also the enclosing type.
	 * @param superType Super type
	 * @return True if the super type is also the enclosing type
	 */
	private boolean isSuperTypeEnclosingType(IType superType) {
		return isEnclosingTypeSelected() && superType != null && superType.equals(fCurrEnclosingType);
	}

	/**
	 * Updates sealed super type.
	 * @param createdType Created type
	 * @param typeName Type name
	 * @param superType Super type
	 */
	private void updateSealedSuperType(IType createdType, String typeName, IType superType) {
		try {
			if (superType != null
					&& superType.isSealed() && createdType != null && !isSuperTypeEnclosingType(superType)) {
				String[] names = superType.getPermittedSubtypeNames();
				List<String> nameList = Arrays.asList(names);
				if (!nameList.contains(typeName)) {
					final ICompilationUnit cu = superType.getCompilationUnit();
					IPackageFragment parent = superType.getPackageFragment();
					IPackageFragment child = createdType.getPackageFragment();
					boolean importTobeAdded = false;
					if (parent != null && !parent.equals(child)) {
						importTobeAdded = true;
					}
					JavaModelUtil.reconcile(cu);
					ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
					parser.setResolveBindings(true);
					parser.setSource(cu);
					CompilationUnitRewrite cuRewrite = new CompilationUnitRewrite(cu);
					TypeDeclaration typeDecl = ASTNodeSearchUtil.getTypeDeclarationNode(superType, cuRewrite.getRoot());
					if (typeDecl != null) {
						ASTRewrite astRewrite = cuRewrite.getASTRewrite();
						AST ast = astRewrite.getAST();
						Type astType = ast.newSimpleType(ast.newSimpleName(typeName));
						astRewrite.getListRewrite(typeDecl, TypeDeclaration.PERMITS_TYPES_PROPERTY).insertLast(astType, null);
						TextEdit tEdit = astRewrite.rewriteAST();
						ImportRewrite importRewrite = null;
						if (importTobeAdded) {
							ITypeBinding typeBinding = createTypeBinding(createdType);
							if (typeBinding != null) {
								importRewrite = StubUtility.createImportRewrite(cu, true);
								var ctx = new ContextSensitiveImportRewriteContext(typeDecl.getRoot(), importRewrite);
								importRewrite.addImport(typeBinding, ctx);
							}
						}
						try {
							JavaModelUtil.applyEdit(cu, tEdit, true, new NullProgressMonitor());
							if (importRewrite != null) {
								TextEdit iEdit = importRewrite.rewriteImports(new NullProgressMonitor());
								JavaModelUtil.applyEdit(cu, iEdit, true, new NullProgressMonitor());
							}
							cu.commitWorkingCopy(true, new NullProgressMonitor());
						} catch (CoreException e) {
							//do nothing
						}
					}
				}
			}
		} catch (JavaModelException e) {
			// do nothing
		}
	}

	// ---- creation ----------------

	/**
	 * Returns the runnable that creates the type using the current settings.
	 * The returned runnable must be executed in the UI thread.
	 * @return the runnable to create the new type
	 */
	public IRunnableWithProgress getRunnable() {
		return monitor -> {
			try {
				if (monitor == null) {
					monitor = new NullProgressMonitor();
				}
				createType(monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
	}

	/**
	 * Sets the non sealed supported flag.
	 * @param project Java project
	 */
	private void setIsNonSealedSupported(IJavaProject project) {
		fIsSealedSupported = project != null && JavaModelUtil.is17OrHigher(project);
		initOtherButtons();
	}

	/**
	 * Getter.
	 * @return the error status based on super class
	 * @since 3.25
	 */
	public IStatus getSuperClassStatus() {
		return this.fSuperClassStatus;
	}

	/**
	 * Getter.
	 * @return the error status based on type name(s)
	 * @since 3.33
	 */
	public IStatus getTypeNameStatus() {
		return this.fTypeNameStatus;
	}

	/**
	 * Getter.
	 * @return the error status based on super interface
	 * @since 3.25
	 */
	public IStatus getSuperInterfaceStatus() {
		return this.fSuperInterfacesStatus;
	}

	/**
	 * Getter.
	 * @return the error status based on super class
	 * @since 3.25
	 */
	public IStatus getSealedSuperClassStatus() {
		return this.fSealedSuperClassStatus;
	}

	/**
	 * Getter.
	 * @return the error status based on super interface
	 * @since 3.25
	 */
	public IStatus getSealedSuperInterfaceStatus() {
		return this.fSealedSuperInterfacesStatus;
	}

	/**
	 * Getter.
	 * @return the error status based on super type
	 * @since 3.25
	 */
	public IStatus getSealedModifierStatus() {
		return this.fSealedModifierStatus;
	}
}