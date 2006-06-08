/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitDescriptorChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempDeclarationFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class InlineTempRefactoring extends ScriptableRefactoring {

	private static final String ID_INLINE_TEMP= "org.eclipse.jdt.ui.inline.temp"; //$NON-NLS-1$

	private int fSelectionStart;
	private int fSelectionLength;
	private ICompilationUnit fCu;
	
	//the following fields are set after the construction
	private VariableDeclaration fTempDeclaration;
	private CompilationUnit fCompilationUnitNode;
	private int[] fReferenceOffsets;

	/**
	 * Creates a new inline constant refactoring.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	public InlineTempRefactoring(ICompilationUnit unit, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= unit;
	}
	public RefactoringStatus checkIfTempSelected(CompilationUnit node) {
		Assert.isNotNull(node);
		fCompilationUnitNode= node;

		fTempDeclaration= TempDeclarationFinder.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);

		if (fTempDeclaration == null){
			String message= RefactoringCoreMessages.InlineTempRefactoring_select_temp;
			return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, message);
		}

		if (fTempDeclaration.getParent() instanceof FieldDeclaration)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTemRefactoring_error_message_fieldsCannotBeInlined); 
		
		return new RefactoringStatus();
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.InlineTempRefactoring_name; 
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			
			RefactoringStatus result= Checks.validateModifiesFiles(
				ResourceUtil.getFiles(new ICompilationUnit[]{fCu}),
				getValidationContext());
			if (result.hasFatalError())
				return result;
				
			if (! fCu.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_syntax_errors); 
								
			result.merge(checkSelection());
			if (result.hasFatalError())
				return result;
			
			result.merge(checkInitializer());	
			return result;
		} finally {
			pm.done();
		}	
	}

    private RefactoringStatus checkInitializer() {
		if (fTempDeclaration.getInitializer().getNodeType() == ASTNode.NULL_LITERAL)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTemRefactoring_error_message_nulLiteralsCannotBeInlined);
		return null;
	}

	private RefactoringStatus checkSelection() {
		if (fTempDeclaration.getParent() instanceof MethodDeclaration)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_method_parameter); 
		
		if (fTempDeclaration.getParent() instanceof CatchClause)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_exceptions_declared); 
		
		if (ASTNodes.getParent(fTempDeclaration, ASTNode.FOR_STATEMENT) != null){
			ForStatement forStmt= (ForStatement)ASTNodes.getParent(fTempDeclaration, ASTNode.FOR_STATEMENT);
			for (Iterator iter= forStmt.initializers().iterator(); iter.hasNext();) {
				if (ASTNodes.isParent(fTempDeclaration, (Expression) iter.next()))
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_for_initializers); 
			}
		}
		
		if (fTempDeclaration.getInitializer() == null){
			String message= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_not_initialized, fTempDeclaration.getName().getIdentifier());
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
				
		return checkAssignments();
	}
	
	private RefactoringStatus checkAssignments() {
		TempAssignmentFinder assignmentFinder= new TempAssignmentFinder(fTempDeclaration);
		fCompilationUnitNode.accept(assignmentFinder);
		if (! assignmentFinder.hasAssignments())
			return new RefactoringStatus();
		int start= assignmentFinder.getFirstAssignment().getStartPosition();
		int length= assignmentFinder.getFirstAssignment().getLength();
		ISourceRange range= new SourceRange(start, length);
		RefactoringStatusContext context= JavaStatusContext.create(fCu, range);	
		String message= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_assigned_more_once, fTempDeclaration.getName().getIdentifier());
		return RefactoringStatus.createFatalErrorStatus(message, context);
	}
	
	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			return new RefactoringStatus();
		} finally {
			pm.done();
		}	
	}
	
	//----- changes

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.InlineTempRefactoring_preview, 2);
			final Map arguments= new HashMap();
			String project= null;
			IJavaProject javaProject= fCu.getJavaProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			final IVariableBinding binding= fTempDeclaration.resolveBinding();
			String text= null;
			final IMethodBinding method= binding.getDeclaringMethod();
			if (method != null)
				text= BindingLabelProvider.getBindingLabel(method, JavaElementLabels.ALL_FULLY_QUALIFIED);
			else
				text= '{' + JavaElementLabels.ELLIPSIS_STRING + '}';
			final String description= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_descriptor_description_short, binding.getName());
			final String header= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_descriptor_description, new String[] { BindingLabelProvider.getBindingLabel(binding, JavaElementLabels.ALL_FULLY_QUALIFIED), text});
			final JavaRefactoringDescriptorComment comment= new JavaRefactoringDescriptorComment(project, this, header);
			comment.addSetting(Messages.format(RefactoringCoreMessages.InlineTempRefactoring_original_pattern, BindingLabelProvider.getBindingLabel(binding, JavaElementLabels.ALL_FULLY_QUALIFIED)));
			final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(ID_INLINE_TEMP, project, description, comment.asString(), arguments, RefactoringDescriptor.NONE);
			arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCu));
			arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_SELECTION, new Integer(fSelectionStart).toString() + " " + new Integer(fSelectionLength).toString()); //$NON-NLS-1$
			final CompilationUnitDescriptorChange result= new CompilationUnitDescriptorChange(descriptor, RefactoringCoreMessages.InlineTempRefactoring_inline, fCu);
			inlineTemp(result, new SubProgressMonitor(pm, 1));
			removeTemp(result);
			return result;
		} finally {
			pm.done();
		}
	}

	private void inlineTemp(TextChange change, IProgressMonitor pm) throws JavaModelException {
		int[] offsets= getReferenceOffsets();
		pm.beginTask("", offsets.length); //$NON-NLS-1$
		String changeName= RefactoringCoreMessages.InlineTempRefactoring_inline_edit_name; 
		int length= fTempDeclaration.getName().getIdentifier().length();
		for(int i= 0; i < offsets.length; i++){
			int offset= offsets[i];
            String sourceToInline= getInitializerSource(needsBrackets(offset), isArrayInitializer());
			TextChangeCompatibility.addTextEdit(change, changeName, new ReplaceEdit(offset, length, sourceToInline));
			pm.worked(1);	
		}
	}
	
    private boolean needsBrackets(int offset) {
		Expression initializer= fTempDeclaration.getInitializer();

		if (initializer instanceof Assignment)//for esthetic reasons
			return true;
		    	
		SimpleName inlineSite= getReferenceAtOffset(offset);
    	if (inlineSite == null)
    		return true;
    		
    	return ASTNodes.substituteMustBeParenthesized(initializer, inlineSite);
    }
    
    private boolean isArrayInitializer() {
    	return (fTempDeclaration.getInitializer().getNodeType()==ASTNode.ARRAY_INITIALIZER);
    }

	private SimpleName getReferenceAtOffset(int offset) {
    	SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(offset, fTempDeclaration.getName().getIdentifier().length()), true);
    	fCompilationUnitNode.accept(analyzer);
    	ASTNode reference= analyzer.getFirstSelectedNode();
    	if(!isReference(reference))
    		return null;
    	return (SimpleName) reference;			
	}
	
	private boolean isReference(ASTNode node) {
		if(!(node instanceof SimpleName))
			return false;
		if(!((SimpleName) node).getIdentifier().equals(fTempDeclaration.getName().getIdentifier()))
			return false;
		return true;
	}

	private void removeTemp(TextChange change) throws JavaModelException {
		//TODO: FIX ME - multi declarations
		
		if (fTempDeclaration.getParent() instanceof VariableDeclarationStatement){
			VariableDeclarationStatement vds= (VariableDeclarationStatement)fTempDeclaration.getParent();
			if (vds.fragments().size() == 1){
				removeDeclaration(change, vds.getStartPosition(), vds.getLength());
				return;
			} else {
				//TODO: FIX ME
				return;
			}
		}
		
		removeDeclaration(change, fTempDeclaration.getStartPosition(), fTempDeclaration.getLength());
	}
	
	private void removeDeclaration(TextChange change, int offset, int length)  throws JavaModelException {
		ISourceRange range= SourceRangeComputer.computeSourceRange(new SourceRange(offset, length), fCu.getSource());
		String changeName= RefactoringCoreMessages.InlineTempRefactoring_remove_edit_name; 
		TextChangeCompatibility.addTextEdit(change, changeName, new DeleteEdit(range.getOffset(), range.getLength()));
	}
	
	private String getInitializerSource(boolean brackets, boolean isArrayInitializer) throws JavaModelException {
		if (brackets)
			return '(' + getModifiedInitializerSource(isArrayInitializer) + ')';
		else
			return getModifiedInitializerSource(isArrayInitializer);
	}
	
	private String getModifiedInitializerSource(boolean isArrayInitializer) throws JavaModelException {
		if (isArrayInitializer)
			return "new " + getTypeNameWithExtraArrayDimensions() + getRawInitializerSource(); //$NON-NLS-1$
		else
			return getRawInitializerSource();
	}
	
	private String getTypeNameWithExtraArrayDimensions() throws JavaModelException {
		if (fTempDeclaration instanceof SingleVariableDeclaration) {
			return getRawTypeName( ((SingleVariableDeclaration) fTempDeclaration).getType());
		} else if (fTempDeclaration instanceof VariableDeclarationFragment) {
			String name= getRawTypeName( ((VariableDeclarationStatement) fTempDeclaration.getParent()).getType());
			for (int i= 0; i < ((VariableDeclarationFragment) fTempDeclaration).getExtraDimensions(); i++)
				name+= "[]"; //$NON-NLS-1$
			return name;
		}
		Assert.isTrue(false, "Must be either SingleVariableDeclaration or VariableDeclarationFragment"); //$NON-NLS-1$
		return null;
	}
	
	private String getRawTypeName(Type selection) throws JavaModelException {
		int start= selection.getStartPosition();
		int end= start + selection.getLength();
		return fCu.getSource().substring(start, end);
	}

	private String getRawInitializerSource() throws JavaModelException{
		int start= fTempDeclaration.getInitializer().getStartPosition();
		int length= fTempDeclaration.getInitializer().getLength();
		int end= start + length;
		return fCu.getSource().substring(start, end);
	}

	public int[] getReferenceOffsets() {
		if (fReferenceOffsets == null) {
			TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(fTempDeclaration, false);
			analyzer.perform();
			fReferenceOffsets= analyzer.getReferenceOffsets();
		}
		return fReferenceOffsets;
	}

	public VariableDeclaration getTempDeclaration() {
		return fTempDeclaration;
	}

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String selection= extended.getAttribute(JavaRefactoringDescriptor.ATTRIBUTE_SELECTION);
			if (selection != null) {
				int offset= -1;
				int length= -1;
				final StringTokenizer tokenizer= new StringTokenizer(selection);
				if (tokenizer.hasMoreTokens())
					offset= Integer.valueOf(tokenizer.nextToken()).intValue();
				if (tokenizer.hasMoreTokens())
					length= Integer.valueOf(tokenizer.nextToken()).intValue();
				if (offset >= 0 && length >= 0) {
					fSelectionStart= offset;
					fSelectionLength= length;
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { selection, JavaRefactoringDescriptor.ATTRIBUTE_SELECTION}));
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptor.ATTRIBUTE_SELECTION));
			final String handle= extended.getAttribute(JavaRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaElement element= JavaRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaElement.COMPILATION_UNIT)
					return createInputFatalStatus(element, ID_INLINE_TEMP);
				else {
					fCu= (ICompilationUnit) element;
		        	final ASTParser parser= ASTParser.newParser(AST.JLS3);
		        	parser.setResolveBindings(true);
		        	parser.setSource(fCu);
		        	if (checkIfTempSelected((CompilationUnit) parser.createAST(null)).hasFatalError())
						return createInputFatalStatus(element, ID_INLINE_TEMP);
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptor.ATTRIBUTE_INPUT));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
