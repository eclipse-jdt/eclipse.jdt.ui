package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.IProblem;

/**
  */
public class QuickFixProcessor implements ICorrectionProcessor {

	public static boolean hasCorrections(int problemId) {
		switch (problemId) {
			case IProblem.UnterminatedString:
			case IProblem.UnusedImport:
			case IProblem.DuplicateImport:
			case IProblem.CannotImportPackage:
			case IProblem.ConflictingImport:			
			case IProblem.UndefinedMethod:
			case IProblem.UndefinedConstructor:
			case IProblem.ParameterMismatch:
			case IProblem.MethodButWithConstructorName:
			case IProblem.UndefinedField:
			case IProblem.UndefinedName:
			case IProblem.PublicClassMustMatchFileName:
			case IProblem.PackageIsNotExpectedPackage:
			case IProblem.UndefinedType:
			case IProblem.FieldTypeNotFound:
			case IProblem.ArgumentTypeNotFound:
			case IProblem.ReturnTypeNotFound:
			case IProblem.SuperclassNotFound:
			case IProblem.ExceptionTypeNotFound:
			case IProblem.InterfaceNotFound: 
			case IProblem.TypeMismatch:
			case IProblem.UnhandledException:
			case IProblem.UnreachableCatch:
			case IProblem.VoidMethodReturnsValue:
			case IProblem.ShouldReturnValue:
			case IProblem.MissingReturnType:
			case IProblem.NonExternalizedStringLiteral:
			case IProblem.NonStaticAccessToStaticField:
			case IProblem.NonStaticAccessToStaticMethod:
			case IProblem.StaticMethodRequested:
			case IProblem.NonStaticFieldFromStaticInvocation:
			case IProblem.InstanceMethodDuringConstructorInvocation:
			case IProblem.InstanceFieldDuringConstructorInvocation:			
			case IProblem.NotVisibleMethod:
			case IProblem.NotVisibleConstructor:
			case IProblem.NotVisibleType:
			case IProblem.SuperclassNotVisible:
			case IProblem.InterfaceNotVisible:
			case IProblem.FieldTypeNotVisible:
			case IProblem.ArgumentTypeNotVisible:
			case IProblem.ReturnTypeNotVisible:
			case IProblem.ExceptionTypeNotVisible:
			case IProblem.NotVisibleField:
			case IProblem.ImportNotVisible:
			case IProblem.BodyForAbstractMethod:
			case IProblem.AbstractMethodInAbstractClass:
			case IProblem.AbstractMethodMustBeImplemented:	
			case IProblem.BodyForNativeMethod:
			case IProblem.OuterLocalMustBeFinal:
			case IProblem.UninitializedLocalVariable:
				return true;
			default:
				return false;
		}
	}
	
	public void process(ICorrectionContext context, List proposals) throws CoreException {
		int id= context.getProblemId();
		if (id == 0) { // no proposals for none-problem locations
			return;
		}
		
		switch (id) {
			case IProblem.UnterminatedString:
				String quoteLabel= CorrectionMessages.getString("JavaCorrectionProcessor.addquote.description"); //$NON-NLS-1$
				int pos= InsertCorrectionProposal.moveBack(context.getOffset() + context.getLength(), context.getOffset(), "\n\r", context.getCompilationUnit()); //$NON-NLS-1$
				proposals.add(new InsertCorrectionProposal(quoteLabel, context.getCompilationUnit(), pos, "\"", 0)); //$NON-NLS-1$ 
				break;
			case IProblem.UnusedImport:
			case IProblem.DuplicateImport:
			case IProblem.CannotImportPackage:
			case IProblem.ConflictingImport:            
				ReorgCorrectionsSubProcessor.removeImportStatementProposals(context, proposals);
				break;
			case IProblem.UndefinedMethod:
				UnresolvedElementsSubProcessor.getMethodProposals(context, false, proposals);
				break;
			case IProblem.UndefinedConstructor:
				UnresolvedElementsSubProcessor.getConstructorProposals(context, proposals);
				break;					
			case IProblem.ParameterMismatch:
				UnresolvedElementsSubProcessor.getMethodProposals(context, true, proposals); 
				break;
			case IProblem.MethodButWithConstructorName:	
				ReturnTypeSubProcessor.addMethodWithConstrNameProposals(context, proposals);
				break;
			case IProblem.UndefinedField:
			case IProblem.UndefinedName:
				UnresolvedElementsSubProcessor.getVariableProposals(context, proposals);
				break;					
			case IProblem.PublicClassMustMatchFileName:
				ReorgCorrectionsSubProcessor.getWrongTypeNameProposals(context, proposals);
				break;
			case IProblem.PackageIsNotExpectedPackage:
				ReorgCorrectionsSubProcessor.getWrongPackageDeclNameProposals(context, proposals);
				break;
			case IProblem.UndefinedType:
			case IProblem.FieldTypeNotFound:
			case IProblem.ArgumentTypeNotFound:
			case IProblem.ReturnTypeNotFound:
			case IProblem.SuperclassNotFound:
			case IProblem.ExceptionTypeNotFound:
			case IProblem.InterfaceNotFound: 
				UnresolvedElementsSubProcessor.getTypeProposals(context, proposals);
				break;	
			case IProblem.TypeMismatch:
				LocalCorrectionsSubProcessor.addCastProposals(context, proposals);
				break;
			case IProblem.UnhandledException:
				LocalCorrectionsSubProcessor.addUncaughtExceptionProposals(context, proposals);
				break;
			case IProblem.UnreachableCatch:
				LocalCorrectionsSubProcessor.addUnreachableCatchProposals(context, proposals);
				break;
			case IProblem.VoidMethodReturnsValue:
				ReturnTypeSubProcessor.addVoidMethodReturnsProposals(context, proposals);
				break;
			case IProblem.MissingReturnType:
				ReturnTypeSubProcessor.addMissingReturnTypeProposals(context, proposals);
				break;
			case IProblem.ShouldReturnValue:
				ReturnTypeSubProcessor.addMissingReturnStatementProposals(context, proposals);
				break;
			case IProblem.NonExternalizedStringLiteral:
				LocalCorrectionsSubProcessor.addNLSProposals(context, proposals);
				break;
			case IProblem.NonStaticAccessToStaticField:
			case IProblem.NonStaticAccessToStaticMethod:
				LocalCorrectionsSubProcessor.addInstanceAccessToStaticProposals(context, proposals);
				break;
			case IProblem.StaticMethodRequested:
			case IProblem.NonStaticFieldFromStaticInvocation:
			case IProblem.InstanceMethodDuringConstructorInvocation:
			case IProblem.InstanceFieldDuringConstructorInvocation:
				ModifierCorrectionSubProcessor.addNonAccessibleMemberProposal(context, proposals, false); 
				break;				
			case IProblem.NotVisibleMethod:
			case IProblem.NotVisibleConstructor:
			case IProblem.NotVisibleType:
			case IProblem.SuperclassNotVisible:
			case IProblem.InterfaceNotVisible:
			case IProblem.FieldTypeNotVisible:
			case IProblem.ArgumentTypeNotVisible:
			case IProblem.ReturnTypeNotVisible:
			case IProblem.ExceptionTypeNotVisible:
			case IProblem.NotVisibleField:
			case IProblem.ImportNotVisible:
				ModifierCorrectionSubProcessor.addNonAccessibleMemberProposal(context, proposals, true); 
				break;
			case IProblem.BodyForAbstractMethod:
			case IProblem.AbstractMethodInAbstractClass:
				ModifierCorrectionSubProcessor.addAbstractMethodProposals(context, proposals); 
				break;
			case IProblem.AbstractMethodMustBeImplemented:
				LocalCorrectionsSubProcessor.addUnimplementedMethodsProposals(context, proposals);
				break;
			case IProblem.BodyForNativeMethod:
				ModifierCorrectionSubProcessor.addNativeMethodProposals(context, proposals);
				break;
			case IProblem.OuterLocalMustBeFinal:				
				ModifierCorrectionSubProcessor.addNonFinalLocalProposal(context, proposals);
				break;
			case IProblem.UninitializedLocalVariable:
				LocalCorrectionsSubProcessor.addUninitializedLocalVariableProposal(context, proposals);
				break;						
			default:
		}
	}

}
