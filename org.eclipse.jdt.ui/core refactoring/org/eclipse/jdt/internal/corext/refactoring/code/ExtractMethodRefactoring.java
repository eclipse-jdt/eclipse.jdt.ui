/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeBlock;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeBlockEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.MethodBlock;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SimpleNameRenamer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.RangeMarker;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

/**
 * Extracts a method in a compilation unit based on a text selection range.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class ExtractMethodRefactoring extends Refactoring {

	private ICompilationUnit fCUnit;
	private ImportEdit fImportEdit;
	private int fSelectionStart;
	private int fSelectionLength;
	private int fSelectionEnd;
	private AST fAST;
	private ExtractMethodAnalyzer fAnalyzer;
	private String fVisibility;
	private String fMethodName;
	private boolean fThrowRuntimeExceptions;
	private List fParameterInfos;
	private Set fUsedNames;

	private static final String EMPTY= ""; //$NON-NLS-1$
	private static final String BLANK= " "; //$NON-NLS-1$
	private static final String RETURN= "return"; //$NON-NLS-1$
	private static final String RETURN_BLANK= "return "; //$NON-NLS-1$
	private static final String SEMICOLON= ";"; //$NON-NLS-1$
	private static final String COMMA_BLANK= ", "; //$NON-NLS-1$
	private static final String STATIC= "static"; //$NON-NLS-1$
	

	private static class UsedNamesCollector extends ASTVisitor {
		private Set result= new HashSet();
		private Set fIgnore= new HashSet();
		public static Set perform(ASTNode[] nodes) {
			UsedNamesCollector collector= new UsedNamesCollector();
			for (int i= 0; i < nodes.length; i++) {
				nodes[i].accept(collector);
			}
			return collector.result;
		}
		public boolean visit(FieldAccess node) {
			Expression exp= node.getExpression();
			if (exp != null)
				fIgnore.add(node.getName());
			return true;
		}
		public void endVisit(FieldAccess node) {
			fIgnore.remove(node.getName());
		}
		public boolean visit(MethodInvocation node) {
			Expression exp= node.getExpression();
			if (exp != null)
				fIgnore.add(node.getName());
			return true;
		}
		public void endVisit(MethodInvocation node) {
			fIgnore.remove(node.getName());
		}
		public boolean visit(QualifiedName node) {
			fIgnore.add(node.getName());
			return true;
		}
		public void endVisit(QualifiedName node) {
			fIgnore.remove(node.getName());
		}
		public boolean visit(SimpleName node) {
			if (!fIgnore.contains(node))
				result.add(node.getIdentifier());
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			result.add(node.getName().getIdentifier());
			// don't dive into type declaration since they open a new
			// context.
			return false;
		}
	}
	
	/**
	 * Creates a new extract method refactoring.
	 *
	 * @param cu the compilation unit which is going to be modified.
	 * @param accessor a callback object to access the source this refactoring is working on.
	 */
	public ExtractMethodRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings) throws JavaModelException {
		Assert.isNotNull(cu);
		Assert.isNotNull(settings);
		fCUnit= cu;
		fImportEdit= new ImportEdit(cu, settings);
		fMethodName= "extracted"; //$NON-NLS-1$
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fSelectionEnd= fSelectionStart + fSelectionLength - 1;
	}
	
	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	 public String getName() {
	 	return RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.name", new String[]{fMethodName, fCUnit.getElementName()}); //$NON-NLS-1$
	 }

	/**
	 * Checks if the refactoring can be activated. Activation typically means, if a
	 * corresponding menu entry can be added to the UI.
	 *
	 * @param pm a progress monitor to report progress during activation checking.
	 * @return the refactoring status describing the result of the activation check.	 
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("ExtractMethodRefactoring.checking_selection"), 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			
			if (fSelectionStart < 0 || fSelectionLength == 0)
				return mergeTextSelectionStatus(result);
			
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCUnit})));
			if (result.hasFatalError())
				return result;
			
			CompilationUnit root= AST.parseCompilationUnit(fCUnit, true);
			fAST= root.getAST();
			root.accept(createVisitor());
			
			result.merge(fAnalyzer.checkActivation());
			if (result.hasFatalError())
				return result;
			if (fVisibility == null) {
				setVisibility("private"); //$NON-NLS-1$
			}
			initializeParameterInfos();
			initializeUsedNames();
			return result;
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally {
			pm.worked(1);
			pm.done();
		}
	}
	
	private ASTVisitor createVisitor() throws JavaModelException {
		fAnalyzer= new ExtractMethodAnalyzer(fCUnit, Selection.createFromStartLength(fSelectionStart, fSelectionLength));
		return fAnalyzer;
	}
	
	/**
	 * Sets the method name to be used for the extracted method.
	 *
	 * @param name the new method name.
	 */	
	public void setMethodName(String name) {
		fMethodName= name;
	}
	
	/**
	 * Returns the method name to be used for the extracted method.
	 * @return the method name to be used for the extracted method.
	 */
	public String getMethodName() {
		return fMethodName;
	} 
	
	/**
	 * Sets the visibility of the new method.
	 * 
	 * @param visibility the visibility of the new method. Valid values are
	 *  "public", "protected", "", and "private"
	 */
	public void setVisibility(String visibility) {
		fVisibility= visibility;
	}
	
	/**
	 * Returns the visibility of the new method.
	 * 
	 * @return the visibility of the new method
	 */
	public String getVisibility() {
		return fVisibility;
	}
	
	/**
	 * Returns the parameter infos.
	 * @return a list of parameter infos.
	 */
	public List getParameterInfos() {
		return fParameterInfos;
	}
	
	/**
	 * Sets whether the new method signature throws runtime exceptions.
	 * 
	 * @param throwsRuntimeExceptions flag indicating if the new method
	 * 	throws runtime exceptions
	 */
	public void setThrowRuntimeExceptions(boolean throwRuntimeExceptions) {
		fThrowRuntimeExceptions= throwRuntimeExceptions;
	}
	
	/**
	 * Checks if the new method name is a valid method name. This method doesn't
	 * check if a method with the same name already exists in the hierarchy. This
	 * check is done in <code>checkInput</code> since it is expensive.
	 */
	public RefactoringStatus checkMethodName() {
		return Checks.checkMethodName(fMethodName);
	}
	
	/**
	 * Checks if the parameter names are valid.
	 */
	public RefactoringStatus checkParameterNames() {
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= (ParameterInfo)iter.next();
			result.merge(Checks.checkIdentifier(parameter.getNewName()));
			for (Iterator others= fParameterInfos.iterator(); others.hasNext();) {
				ParameterInfo other= (ParameterInfo) others.next();
				if (parameter != other && other.getNewName().equals(parameter.getNewName())) {
					result.addError(RefactoringCoreMessages.getFormattedString(
						"ExtractMethodRefactoring.error.sameParameter", //$NON-NLS-1$
						other.getNewName()));
					return result;
				}
			}
			if (parameter.isRenamed() && fUsedNames.contains(parameter.getNewName())) {
				result.addError(RefactoringCoreMessages.getFormattedString(
					"ExtractMethodRefactoring.error.nameInUse", //$NON-NLS-1$
					parameter.getNewName()));
				return result;
			}
		}
		return result;
	}
	
	/**
	 * Returns the names already in use in the selected statements/expressions.
	 * 
	 * @return names already in use.
	 */
	public Set getUsedNames() {
		return fUsedNames;
	}
	
	/* (non-Javadoc)
	 * Method declared in Refactoring
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("ExtractMethodRefactoring.checking_new_name"), 2); //$NON-NLS-1$
		pm.subTask(EMPTY);
		
		RefactoringStatus result= checkMethodName();
		result.merge(checkParameterNames());
		pm.worked(1);
		
		MethodDeclaration node= fAnalyzer.getEnclosingMethod();
		if (node != null) {
			fAnalyzer.checkInput(result, fMethodName, fCUnit.getJavaProject(), fAST);
			pm.worked(1);
		
		}
		pm.done();
		return result;
	}
	
	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		if (fMethodName == null)
			return null;
		
		fAnalyzer.aboutToCreateChange();
		MethodDeclaration method= fAnalyzer.getEnclosingMethod();
		String sourceMethodName= method.getName().getIdentifier();
		
		CompilationUnitChange result= null;
		try {
			result= new CompilationUnitChange(
				RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.change_name", new String[]{fMethodName, sourceMethodName}),  //$NON-NLS-1$
				fCUnit);
		
			ITypeBinding[] exceptions= fAnalyzer.getExceptions(fThrowRuntimeExceptions, fAST);
			for (int i= 0; i < exceptions.length; i++) {
				ITypeBinding exception= exceptions[i];
				fImportEdit.addImport(Bindings.getFullyQualifiedImportName(exception));
			}
			
			if (fAnalyzer.generateImport()) {
				fImportEdit.addImport(ASTNodes.asString(fAnalyzer.getReturnType()));
			}
		
			if (!fImportEdit.isEmpty())
				result.addTextEdit(RefactoringCoreMessages.getString("ExtractMethodRefactoring.organize_imports"), fImportEdit); //$NON-NLS-1$
			
			TextBuffer buffer= null;
			try {
				// This is cheap since the compilation unit is already open in a editor.
				buffer= TextBuffer.create((IFile)WorkingCopyUtil.getOriginal(fCUnit).getResource());
				String delimiter= buffer.getLineDelimiter(buffer.getLineOfOffset(method.getStartPosition()));
				// Inserting the new method
				result.addTextEdit(RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.add_method", fMethodName), //$NON-NLS-1$
					createNewMethodEdit(buffer));
			
				// Replacing the old statements with the new method call.
				result.addTextEdit(RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.substitute_with_call", fMethodName), //$NON-NLS-1$
					SimpleTextEdit.createReplace(fSelectionStart, fSelectionLength, createCall(buffer, delimiter)));
			} finally {
				TextBuffer.release(buffer);
			}
			
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		return result;
	}
	
	/**
	 * Returns the signature of the new method.
	 * 
	 * @return the signature of the extracted method
	 */
	public String getSignature() {
		return getSignature(fMethodName);
	}
	
	/**
	 * Returns the signature of the new method.
	 * 
	 * @param methodName the method name used for the new method
	 * @return the signature of the extracted method
	 */
	public String getSignature(String methodName) {
		StringBuffer buffer= new StringBuffer(fVisibility);		
		if (fVisibility.length() > 0)
			buffer.append(BLANK);
			
		if (Modifier.isStatic(fAnalyzer.getEnclosingMethod().getModifiers()) || fAnalyzer.getForceStatic()) {
			buffer.append(STATIC);
			buffer.append(BLANK);
		}
		
		Type returnType= fAnalyzer.getReturnType();
		if (returnType != null) {
			if (fAnalyzer.generateImport()) {
				buffer.append(ASTNodes.getTypeName(returnType));
			} else {
				buffer.append(ASTNodes.asString(returnType));
			}
			buffer.append(BLANK);
		}

		buffer.append(methodName);

		appendParameters(buffer);
		appendThrownExceptions(buffer);		
		return buffer.toString();
	}
	
	//---- Helper methods ------------------------------------------------------------------------
	
	private void initializeParameterInfos() {
		IVariableBinding[] arguments= fAnalyzer.getArguments();
		fParameterInfos= new ArrayList(arguments.length);
		ASTNode root= fAnalyzer.getEnclosingMethod();
		for (int i= 0; i < arguments.length; i++) {
			IVariableBinding argument= arguments[i];
			if (argument == null)
				continue;
			VariableDeclaration declaration= ASTNodes.findVariableDeclaration(argument, root);
			ParameterInfo info= new ParameterInfo(getType(declaration), argument.getName(), i);
			info.setData(argument);
			fParameterInfos.add(info);
		}
	}
	
	private void initializeUsedNames() {
		fUsedNames= UsedNamesCollector.perform(fAnalyzer.getSelectedNodes());
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= (ParameterInfo)iter.next();
			fUsedNames.remove(parameter.getOldName());
		}
	}
	
	private RefactoringStatus mergeTextSelectionStatus(RefactoringStatus status) {
		status.addFatalError(RefactoringCoreMessages.getString("ExtractMethodRefactoring.no_set_of_statements")); //$NON-NLS-1$
		return status;	
	}
	
	private TextEdit createNewMethodEdit(TextBuffer buffer) throws CoreException {
		MethodDeclaration method= fAnalyzer.getEnclosingMethod();
		final int methodStart= method.getStartPosition();
		MethodBlock methodBlock= new MethodBlock(getSignature(), createMethodBody(buffer));
		final int spacing= MethodBlock.probeSpacing(buffer, method);
		// +1 (e.g <=) for an extra newline since we insert the new code at
		// the end of a method declaration (e.g. right after the closing }
		CodeBlockEdit result= CodeBlockEdit.createInsert(methodStart + method.getLength(), methodBlock, spacing + 1);
		return result;
	}
	
	private CodeBlock createMethodBody(TextBuffer buffer) throws CoreException {
		CodeBlock body= createStatementBlock(buffer);
		
		// We extract an expression
		boolean extractsExpression= fAnalyzer.isExpressionSelected();
		if (extractsExpression) {
			ITypeBinding binding= fAnalyzer.getExpressionBinding();
			if (binding != null && (!binding.isPrimitive() || !"void".equals(binding.getName()))) //$NON-NLS-1$
				body.prependToLine(0, RETURN_BLANK);
		}
		
		if (sourceNeedsSemicolon()) {
			body.appendToLine(body.size() - 1, SEMICOLON);
		}
		
		// Locals that are not passed as an arguments since the extracted method only
		// writes to them
		IVariableBinding[] methodLocals= fAnalyzer.getMethodLocals();
		// Do in reverse order since we prepend
		for (int i= methodLocals.length -1; i >= 0; i--) {
			if (methodLocals[i] != null)
				body.prepend(getLocalDeclaration(methodLocals[i]));
		}

		IVariableBinding returnValue= fAnalyzer.getReturnValue();
		if (returnValue != null) {
			body.append(RETURN_BLANK + returnValue.getName() + SEMICOLON);
		}
		return body;
	}
	
	private CodeBlock createStatementBlock(TextBuffer original) throws CoreException {
		// ToDo: we should remove this copy. To do so we have to use AST rewrite all over
		// the place.
		TextBuffer buffer= TextBuffer.create(original.getContent());
		List bindings= new ArrayList(2);
		List newNames= new ArrayList(2);
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= (ParameterInfo)iter.next();
			if (parameter.isRenamed()) {
				bindings.add(parameter.getData());
				newNames.add(parameter.getNewName());
			}
		}
		ASTRewrite rewriter= new ASTRewrite(fAnalyzer.getEnclosingMethod());
		SimpleNameRenamer.perform(
			rewriter, 
			(IBinding[]) bindings.toArray(new IBinding[bindings.size()]),
			(String[]) newNames.toArray(new String[newNames.size()]),
			fAnalyzer.getSelectedNodes());
		TextBufferEditor editor= new TextBufferEditor(buffer);
		RangeMarker root= new RangeMarker(fSelectionStart, fSelectionLength);
		rewriter.rewriteNode(buffer, root, null);
		editor.add(root);
		editor.performEdits(null);
		TextRange range= root.getTextRange();
		return new CodeBlock(buffer, range.getOffset(), range.getLength());
	}
	
	private String createCall(TextBuffer buffer, String delimiter) {
		int tabWidth= CodeFormatterUtil.getTabWidth();
		int firstLineIndent= Strings.computeIndent(buffer.getLineContentOfOffset(fSelectionStart), tabWidth);
		StringBuffer code= new StringBuffer();
		
		IVariableBinding[] locals= fAnalyzer.getCallerLocals();
		for (int i= 0; i < locals.length; i++) {
			appendLocalDeclaration(code, locals[i]);
			code.append(SEMICOLON);
			code.append(delimiter);
		}
				
		int returnKind= fAnalyzer.getReturnKind();
		switch (returnKind) {
			case ExtractMethodAnalyzer.ACCESS_TO_LOCAL:
				IVariableBinding binding= fAnalyzer.getReturnLocal();
				if (binding != null) {
					appendLocalDeclaration(code, binding);
				} else {
					binding= fAnalyzer.getReturnValue();
					code.append(binding.getName());
				}
				code.append(" = "); //$NON-NLS-1$
				break;
			case ExtractMethodAnalyzer.RETURN_STATEMENT_VALUE:
				// We return a value. So the code must look like "return extracted();"
				code.append(RETURN_BLANK);
				break;
		}
		
		code.append(fMethodName);
		code.append("("); //$NON-NLS-1$
		for (int i= 0; i < fParameterInfos.size(); i++) {
			if (i > 0)
				code.append(COMMA_BLANK);
			code.append(((ParameterInfo)fParameterInfos.get(i)).getOldName());
		}		
		code.append(")"); //$NON-NLS-1$
						
		if (callNeedsSemicolon())
			code.append(SEMICOLON);
			
		// We have a void return statement. The code looks like
		// extracted();
		// return;	
		if (returnKind == ExtractMethodAnalyzer.RETURN_STATEMENT_VOID && !fAnalyzer.isLastStatementSelected()) {
			code.append(delimiter);
			code.append(RETURN);
			code.append(SEMICOLON);
		}
		
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		String result= formatter.format(code.toString(), firstLineIndent, null, delimiter);
		
		// we have to do this after formatting
		int pos= fSelectionStart + fSelectionLength;
		TextRegion region= buffer.getLineInformationOfOffset(pos);
		if (region.getOffset() == pos)
			result= result + delimiter;
		
		region= buffer.getLineInformationOfOffset(fSelectionStart);
		String selectedLine= buffer.getContent(region.getOffset(), fSelectionStart - region.getOffset());
		int indent= Strings.computeIndent(selectedLine, tabWidth);
		return Strings.trimIndent(result,  indent, tabWidth);
	}


	private boolean callNeedsSemicolon() {
		ASTNode node= fAnalyzer.getLastSelectedNode();
		return node instanceof Statement;
	}
	
	private boolean sourceNeedsSemicolon() {
		ASTNode node= fAnalyzer.getLastSelectedNode();
		return node instanceof Expression;
	}	
	
	private void appendParameters(StringBuffer buffer) {
		buffer.append('('); //$NON-NLS-1$
		int size= fParameterInfos.size();
		for (int i= 0; i < size; i++) {
			ParameterInfo info= (ParameterInfo)fParameterInfos.get(i);
			if (i > 0)
				buffer.append(COMMA_BLANK);
			appendModifiers(buffer, info);
			buffer.append(info.getNewTypeName());
			buffer.append(BLANK);
			buffer.append(info.getNewName());
		}
		buffer.append(')'); //$NON-NLS-1$
	}
	
	private void appendModifiers(StringBuffer buffer, ParameterInfo parameter) {
		ASTNode root= fAnalyzer.getEnclosingMethod();
		VariableDeclaration declaration= ASTNodes.findVariableDeclaration((IVariableBinding)parameter.getData(), root);
		appendModifiers(buffer, declaration);
	}
	
	private void appendModifiers(StringBuffer buffer, VariableDeclaration declaration) {
		String modifiers= ASTNodes.modifierString(ASTNodes.getModifiers(declaration));
		if (modifiers.length() > 0) {
			buffer.append(modifiers);
			buffer.append(BLANK);
		}
	}
	
	private void appendThrownExceptions(StringBuffer buffer) {
		ITypeBinding[] exceptions= fAnalyzer.getExceptions(fThrowRuntimeExceptions, fAST);
		if (exceptions.length == 0)
			return;
			
		buffer.append(" throws "); //$NON-NLS-1$
		for (int i= 0; i < exceptions.length; i++) {
			ITypeBinding exception= exceptions[i];
			if (i > 0)
				buffer.append(", "); //$NON-NLS-1$
			buffer.append(exception.getName());
		}
	}
	
	private void appendLocalDeclaration(StringBuffer buffer, IVariableBinding local) {
		ASTNode root= fAnalyzer.getEnclosingMethod();
		VariableDeclaration declaration= ASTNodes.findVariableDeclaration(local, root);
		appendModifiers(buffer, declaration);
		buffer.append(getType(declaration));
		buffer.append(BLANK);
		buffer.append(local.getName());
	}
	
	private String getLocalDeclaration(IVariableBinding local) {
		StringBuffer buffer= new StringBuffer();
		appendLocalDeclaration(buffer, local);
		buffer.append(SEMICOLON);
		return buffer.toString();
	}
	
	private String getType(VariableDeclaration declaration) {
		return ASTNodes.asString(ASTNodes.getType(declaration));
	}
}