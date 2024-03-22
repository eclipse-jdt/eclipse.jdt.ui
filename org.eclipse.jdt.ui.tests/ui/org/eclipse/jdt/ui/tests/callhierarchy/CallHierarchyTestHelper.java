/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *             (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.callhierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.ui.tests.core.rules.Java17ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class CallHierarchyTestHelper {
    private static final String[] EMPTY= new String[0];

    private IJavaProject fJavaProject1;
    private IJavaProject fJavaProject2;
    private IJavaProject fJavaProject3;
    private IType fType1;
    private IType fType2;
    private IType fTypeP;
	private IType fFooImplAType;
	private IType fFooImplBType;
	private IType fFooType;
	private IType fAbsType;
	private IType fAbsI1Type;
	private IType fAbsI2Type;
    private IPackageFragment fPack1;
    private IPackageFragment fPack2;
    private IPackageFragment fPack3;

    private IMethod fMethod1;
    private IMethod fMethod2;
    private IMethod fMethod3;
    private IMethod fMethod4;
    private IMethod fRecursiveMethod1;
    private IMethod fRecursiveMethod2;
	private IMethod fCalleeMethod;
	private IMethod fAbsCalleeMethod;
	private IMethod fFooMethod;
	private IMethod fFooImplMethod_A;
	private IMethod fFooImplMethod_B;
	private IMethod fAbsFooMethod;
	private IMethod fAbsI1FooMethod;
	private IMethod fAbsI2FooMethod;

    public void setUp() throws Exception {
        fJavaProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
        fJavaProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
        fJavaProject3= JavaProjectHelper.createJavaProject("TestProject3", "bin");
        assertBuildWithoutErrors(fJavaProject1);
        assertBuildWithoutErrors(fJavaProject2);
        assertBuildWithoutErrors(fJavaProject3);
        fType1= null;
        fType2= null;
        fTypeP= null;
        fFooImplAType= null;
        fFooImplBType= null;
        fAbsI1Type= null;
        fAbsI2Type= null;
        fAbsType= null;
        fFooType= null;
        fPack1= null;
        fPack2= null;
        fPack3= null;
    }

    public void tearDown() throws Exception {
        JavaProjectHelper.delete(fJavaProject1);
        JavaProjectHelper.delete(fJavaProject2);
        JavaProjectHelper.delete(fJavaProject3);
    }


    /**
     * Creates two simple classes, A and B. Sets the instance fields fType1 and fType2.
     */
    public void createSimpleClasses() throws Exception {
        createPackages();


        ICompilationUnit cu1= fPack1.getCompilationUnit("A.java");

        fType1=
            cu1.createType(
                """
					public class A {
					public A() {
					}
					 \
					public void method1() {
					}
					 \
					public void method2() {
					  method1();
					}
					 \
					public void recursiveMethod1() {
					  recursiveMethod2();
					 \
					}
					 \
					public void recursiveMethod2() {
					  recursiveMethod1();
					 \
					}
					}
					""",
                null,
                true,
                null);

        ICompilationUnit cu2= fPack2.getCompilationUnit("B.java");
        fType2=
            cu2.createType(
                "public class B extends pack1.A {\npublic void method3() { method1(); method2(); }\n public void method4() { method3(); }\n}\n",
                null,
                true,
                null);

        assertBuildWithoutErrors(fJavaProject1);
        assertBuildWithoutErrors(fJavaProject2);
    }

    /**
     * Creates two simple classes, A and its subclass B, where B calls A's implicit constructor explicitly. Sets the instance fields fType1 and fType2.
     */
    public void createImplicitConstructorClasses() throws Exception {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("A.java");
        fType1=
            cu1.createType(
                "public class A {\n public void method1() { }\n public void method2() { method1(); }\n public void recursiveMethod1() { recursiveMethod2(); }\n public void recursiveMethod2() { recursiveMethod1(); }\n}\n",
                null,
                true,
                null);

        ICompilationUnit cu2= fPack2.getCompilationUnit("B.java");
        fType2=
            cu2.createType(
                "public class B extends pack1.A {\n public B(String name) { super(); }\n}\n",
                null,
                true,
                null);
        assertBuildWithoutErrors(fPack1);
    }

    /**
     * Creates an inner class and sets the class attribute fType1.
     */
    public void createInnerClass() throws Exception {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("Outer.java");
        fType1=
            cu1.createType(
                """
					public class Outer {
					private Inner inner= new Inner();
					class Inner { public void innerMethod1() { outerMethod1(); }
					 public void innerMethod2() { innerMethod1(); }
					 }
					public void outerMethod1() { }
					 public void outerMethod2() { inner.innerMethod2(); }
					}""",
                null,
                true,
                null);
        assertBuildWithoutErrors(fPack1);
    }

    /**
     * Creates an anonymous inner class and sets the class attribute fType1.
     */
    public void createAnonymousInnerClass() throws Exception {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("AnonymousInner.java");
        fType1=
            cu1.createType(
                """
					public class AnonymousInner {
					  Object anonClass = new Object() {
					    void anotherMethod() {
					      someMethod();
					    }
					  };
					  void someMethod() {
					  }
					}
					""",
                null,
                true,
                null);

        ICompilationUnit cu2= fPack2.getCompilationUnit("Outer.java");
        fType2=
            cu2.createType(
                """
					public class Outer {
					    interface Intf {
					         public void foo();
					    }
					    class Clazz {
					         public void foo() { };
					    }
					    public void anonymousOnInterface() {
					        new Intf() {
					            public void foo() {
					                someMethod();
					            }
					        };
					    }
					    public void anonymousOnClass() {
					        new Clazz() {
					            public void foo() {
					                someMethod();
					            }
					        };
					    }
					    public void someMethod() { }
					}
					""",
                null,
                true,
                null);
        assertBuildWithoutErrors(fPack1);
    }

    /**
     * Creates an anonymous inner class inside another method and sets the class attribute fType1.
     */
    public void createAnonymousInnerClassInsideMethod() throws Exception {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("AnonymousInnerInsideMethod.java");
        fType1=
            cu1.createType(
                """
					public class AnonymousInnerInsideMethod {
					  void m() {
					    System.out.println("before");
					    Runnable runnable = new Runnable() {
					      public void run() {
					        System.out.println("run");
					      }
					    };
					    runnable.run();
					  }
					}
					""",
                null,
                true,
                null);
        assertBuildWithoutErrors(fPack1);
    }

	/**
	 * Creates a class with various lambda function definitions and calls
	 */
	public void createClassWithLambdaCalls() throws Exception {
		createPackages();

		ICompilationUnit cu= fPack1.getCompilationUnit("Snippet.java");
		fType1= cu.createType("""
			public class Snippet {
				static Function<? super String, ? extends String> mapper1 = y -> transform(y);
				Function<? super String, ? extends String> mapper2 = y -> transform(y);
			
				static {
					 mapper1 = y -> transform(y);
				}
			
				public Snippet() {
					mapper2 = y -> transform(y);
				}
			
				public static void main(String[] args) {
					mapper1 = y -> transform(y);
				}
			
				Object[] funcCall() {
					return List.of("aaa").stream().map(y -> transform(y)).toArray();
				}
			
				static String transform(String s) {
			     x();
					return s.toUpperCase();
				}
			 static String x() {\
			     return null;
			}
			}""",
				null,
				true,
				null);
		cu.createImport("java.util.List", fType1, null);
		cu.createImport("java.util.function.Function", fType1, null);
		fMethod1= fType1.getMethod("x", EMPTY);
		fMethod2= fType1.getMethod("transform", new String[] { "QString;" });
		Assert.assertNotNull(fMethod1);
		Assert.assertNotNull(fMethod2);
		assertBuildWithoutErrors(fPack1);
	}

    /**
     * Creates a class with a static initializer and sets the class attribute fType1.
     */
    public void createStaticInitializerClass() throws Exception {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("Initializer.java");
        fType1=
            cu1.createType(
                "public class Initializer { static { someMethod(); }\n public static void someMethod() { }\n }\n",
                null,
                true,
                null);
        assertBuildWithoutErrors(fPack1);
    }

    /**
     * Creates a record class, OneRecord and sets the instance field fType1.
     */
    public void createRecordClasses() throws Exception {
    	ProjectTestSetup projectsetup= new Java17ProjectTestSetup(false);
		fJavaProject3.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJavaProject3, false);

		IPackageFragmentRoot fSourceFolder= JavaProjectHelper.addSourceContainer(fJavaProject3, "src");

		String MODULE_INFO_FILE_CONTENT = """
			module test {
			}
			""";

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		fPack3= fSourceFolder.createPackageFragment("test", false, null);

    	JavaProjectHelper.set16CompilerOptions(fJavaProject1, true);

        ICompilationUnit cu1= fPack3.getCompilationUnit("Outer.java");

        fType1=
            cu1.createType(
                    """
						public class Outer {
						record OneRecord(int number1, int number2) {
						  OneRecord() { this(1, 2); }
						}
						 \
						    public static void method1() {
						        new OneRecord(3, 5);
						    }
						    public static void method2() {
						        new OneRecord();
						    }
						    public static void method3() {
						        method1();
						        method2();
						    }
						}
						""",
                null,
                true,
                null);
        assertBuildWithoutErrors(fJavaProject3);
    }

	/**
	 * Creates a record class and a type that references a field of the record.
	 */
	public void createRecordWithCalleeClasses() throws Exception {
		ProjectTestSetup projectsetup= new Java17ProjectTestSetup(false);
		fJavaProject3.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJavaProject3, false);

		IPackageFragmentRoot fSourceFolder= JavaProjectHelper.addSourceContainer(fJavaProject3, "src");

		String MODULE_INFO_FILE_CONTENT= """
			module test {
			}
			""";

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		fPack3= fSourceFolder.createPackageFragment("test", false, null);

		JavaProjectHelper.set16CompilerOptions(fJavaProject1, true);

		ICompilationUnit cu1= fPack3.getCompilationUnit("RecordTest.java");
		ICompilationUnit cu2= fPack3.getCompilationUnit("CallerTest.java");

		fType1= cu1.createType(
				"""
				record RecordTest(int number1) {
				}
				""",
				null,
				true,
				null);
		fType2= cu2.createType(
				"""
				public class CallerTest {
					void foo(RecordTest r) {
						var n = r.number1();
					}
				}
				""",
				null,
				true,
				null);
		assertBuildWithoutErrors(fJavaProject3);
	}

    public void createCalleeClasses() throws Exception {
        createPackages();

        ICompilationUnit cu3= fPack2.getCompilationUnit("P.java");
        fTypeP=
        		cu3.createType(
                    """
						public class P {
						  private A handler;
						  private Abs absHandler;
						
						  public void callFoo() {
						     handler.foo();
						  }
						  public void callAbsFoo() {
						     absHandler.absFoo();
						  }
						
						}""",
                    null,
                    true,
                    null);

        ICompilationUnit cu4= fPack2.getCompilationUnit("A.java");
        fFooType= cu4.createType(
        		"""
					public interface A {
					   void foo();
					}
					"""
                , null, true, null);


        ICompilationUnit cu5= fPack2.getCompilationUnit("AImpl.java");
        fFooImplAType= cu5.createType(
                """
					public class AImpl implements A {
					  public void foo() {
					      System.out.println();
					  }
					}
					"""
                , null, true, null);

        ICompilationUnit cu6= fPack2.getCompilationUnit("BImpl.java");
        fFooImplBType= cu6.createType(
                """
					public class BImpl implements A {
					  public void foo() {
					      System.out.printf("");
					  }
					}
					"""
                , null, true, null);


        ICompilationUnit cu7= fPack2.getCompilationUnit("Abs.java");
        fAbsType= cu7.createType(
        		"""
					public abstract class Abs {
					   abstract void absFoo();
					}
					"""
                , null, true, null);

        ICompilationUnit cu8= fPack2.getCompilationUnit("AbsI1.java");
        fAbsI1Type= cu8.createType(
        		"""
					public class AbsI1 extends Abs {
					   void absFoo() {}
					}
					"""
                , null, true, null);

        ICompilationUnit cu9= fPack2.getCompilationUnit("AbsI2.java");
        fAbsI2Type= cu9.createType(
        		"""
					public class AbsI2 extends Abs {
					   void absFoo() {}
					}
					"""
                , null, true, null);

        assertBuildWithoutErrors(fJavaProject1);
        assertBuildWithoutErrors(fJavaProject2);

    }

    /**
     * Creates two packages (pack1 and pack2) in different projects. Sets the
     * instance fields fPack1 and fPack2.
     */
    public void createPackages() throws Exception {
        JavaProjectHelper.addRTJar9(fJavaProject1);

        IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
        fPack1= root1.createPackageFragment("pack1", true, null);
        assertBuildWithoutErrors(fPack1);

        JavaProjectHelper.addRTJar9(fJavaProject2);
        JavaProjectHelper.addRequiredProject(fJavaProject2, fJavaProject1);

        IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fJavaProject2, "src");
        fPack2= root2.createPackageFragment("pack2", true, null);
        assertBuildWithoutErrors(fPack2);
    }

    /**
     * Returns all error markers on given resource, recursively
     */
    public List<IMarker> getErrorMarkers(IResource resource) throws CoreException {
        IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_INFINITE);
        List<IMarker> errorMarkers = new ArrayList<>();
        for (IMarker marker : markers) {
            if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
                errorMarkers.add(marker);
            }
        }
        return errorMarkers;
    }

    public static List<String> convertMarkers(IMarker [] markers) throws Exception {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < markers.length; i++) {
            IMarker marker = markers[i];
            StringBuilder sb = new StringBuilder("Marker #");
            sb.append(i).append("[");
            sb.append(marker.getAttribute("message", null));
            sb.append(" at line: ").append(marker.getAttribute("lineNumber", 0));
            sb.append("], ");
            result.add(sb.toString());
        }
        return result;
    }

    /**
     * Verifies that no error markers exist in the given resource.
     *
     * @param element
     *            The resource that is searched for error markers
     */
    protected void assertBuildWithoutErrors(IJavaElement element) throws Exception {
    	IResource resource= element.getResource();
    	Assert.assertNotNull("Given element has no resource: " + element, resource);
    	resource.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		assertNoErrorMarkers(resource);
    }

    /**
     * Verifies that no error markers exist in the given resource.
     *
     * @param resource
     *            The resource that is searched for error markers
     */
    protected void assertNoErrorMarkers(IResource resource) throws Exception {
        List<IMarker> errorMarkers = getErrorMarkers(resource);
        List<String> messages = convertMarkers(errorMarkers.toArray(new IMarker[errorMarkers.size()]));
        Assert.assertEquals("No error marker expected, but found markers with messages: " + messages.toString(), 0,
                errorMarkers.size());
    }

    /**
     * Asserts that all the expected methods were found in the call results.
     */
    public void assertCalls(Collection<IMember> expectedMembers, Collection<MethodWrapper> calls) {
        Collection<IMember> foundMembers= new ArrayList<>();

		for (MethodWrapper element : calls) {
			foundMembers.add(element.getMember());
		}

        Assert.assertEquals("Wrong number of calls", expectedMembers.size(), calls.size());
        Assert.assertTrue("One or more members not found", foundMembers.containsAll(expectedMembers));
    }

    /**
     * Asserts that all the expected methods were found in the call results.
     */
    public void assertCalls(Collection<IMember> expectedMembers, MethodWrapper[] callResults) {
        assertCalls(expectedMembers, Arrays.asList(callResults));
    }

    /**
     * Asserts that all the expected methods were found in the call results.
     */
    public void assertCalls(IMember[] expectedMembers, Object[] callResults) {
        assertCalls(Arrays.asList(expectedMembers), Arrays.stream(callResults).map(MethodWrapper.class::cast).toList());
    }

    public MethodWrapper findMethodWrapper(IMethod method, Object[] methodWrappers) {
        MethodWrapper thirdLevelMethodWrapper= null;
		for (Object methodWrapper : methodWrappers) {
			if (method.equals(((MethodWrapper) methodWrapper).getMember())) {
				thirdLevelMethodWrapper= (MethodWrapper) methodWrapper;
				break;
			}
		}
        return thirdLevelMethodWrapper;
    }

    public IJavaProject getJavaProject2() {
        return fJavaProject2;
    }

    public IPackageFragment getPackage1() {
        return fPack1;
    }

    public IPackageFragment getPackage2() {
        return fPack2;
    }

	public IType getType1() {
        return fType1;
    }

    public IType getType2() {
        return fType2;
    }

    public IType getTypeP() {
    	return fTypeP;
    }

    public IType getFooImplAType() {
		return fFooImplAType;
	}

    public IType getFooImplBType() {
		return fFooImplBType;
	}

    public IType getFooType() {
		return fFooType;
	}

    public IType getAbsType() {
		return fAbsType;
	}

    public IType getAbsI1Type() {
		return fAbsI1Type;
	}

    public IType getAbsI2Type() {
		return fAbsI2Type;
	}

    public IMethod getMethod1() {
        if (fMethod1 == null) {
            fMethod1= getType1().getMethod("method1", EMPTY);
        }
        return fMethod1;
    }

    public IMethod getMethod2() {
        if (fMethod2 == null) {
            fMethod2= getType1().getMethod("method2", EMPTY);
        }
        return fMethod2;
    }

    public IMethod getMethod3() {
        if (fMethod3 == null) {
            fMethod3= getType2().getMethod("method3", EMPTY);
        }
        return fMethod3;
    }

    public IMethod getMethod4() {
        if (fMethod4 == null) {
            fMethod4= getType2().getMethod("method4", EMPTY);
        }
        return fMethod4;
    }

    public IMethod getRecursiveMethod1() {
        if (fRecursiveMethod1 == null) {
            fRecursiveMethod1= getType1().getMethod("recursiveMethod1", EMPTY);
        }
        return fRecursiveMethod1;
    }

    public IMethod getRecursiveMethod2() {
        if (fRecursiveMethod2 == null) {
            fRecursiveMethod2= getType1().getMethod("recursiveMethod2", EMPTY);
        }
        return fRecursiveMethod2;
    }

    public IMethod getCalleeMethod() {
        if (fCalleeMethod == null) {
            fCalleeMethod= getTypeP().getMethod("callFoo", EMPTY);
        }
        return fCalleeMethod;
    }

    public IMethod getAbsCalleeMethod() {
        if (fAbsCalleeMethod == null) {
        	fAbsCalleeMethod= getTypeP().getMethod("callAbsFoo", EMPTY);
        }
        return fAbsCalleeMethod;
    }

	public IMethod getFooImplMethod_A() {
        if (fFooImplMethod_A == null) {
        	fFooImplMethod_A= getFooImplAType().getMethod("foo", EMPTY);
        }
        return fFooImplMethod_A;
	}

	public IMethod getFooImplMethod_B() {
        if (fFooImplMethod_B == null) {
        	fFooImplMethod_B= getFooImplBType().getMethod("foo", EMPTY);
        }
        return fFooImplMethod_B;
	}

	public IMethod getFooMethod() {
        if (fFooMethod == null) {
        	fFooMethod= getFooType().getMethod("foo", EMPTY);
        }
        return fFooMethod;
	}

	public IMethod getAbsFooMethod() {
        if (fAbsFooMethod == null) {
        	fAbsFooMethod= getAbsType().getMethod("absFoo", EMPTY);
        }
        return fAbsFooMethod;
	}

	public IMethod getAbsI1FooMethod() {
        if (fAbsI1FooMethod == null) {
        	fAbsI1FooMethod= getAbsI1Type().getMethod("absFoo", EMPTY);
        }
        return fAbsI1FooMethod;
	}

	public IMethod getAbsI2FooMethod() {
        if (fAbsI2FooMethod == null) {
        	fAbsI2FooMethod= getAbsI2Type().getMethod("absFoo", EMPTY);
        }
        return fAbsI2FooMethod;
	}
}
