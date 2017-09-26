About rtstubs*.jar
*****************

Many JDT tests require a specific JRE version that may be newer or older than
the default JRE that is being used to run the tests. To create a stable
environment, tests usually put rtstubs*.jar on the classpath.

rtstubs*.jar contain class file stubs for a few of the most basic java.*
packages. The class files only contain the APIs, but no method bodies etc.
(except for required super constructor invocations).

The process of creating a new rtstubs*.jar is partly automated, but there's
still quite some manual work involved to get a source project that compiles
without errors.

Here are the generator classes that likely need some tweaks in the future:
- org.eclipse.jdt.testplugin.util.CreateStubsAction: add packages to
    DEFAULT_PACKAGES if there are many references to them
- org.eclipse.jdt.internal.corext.refactoring.binary.StubCreator: Make sure new
    or newly used language constructs are properly handled.

Building rtstubs*.jar
*********************
- Have org.eclipse.jdt.ui.tests in your JDT source workspace.
- Start a runtime workbench.
- Create a Java project with the JRE for which you need to generate stubs
   (e.g. "JDK9")

- Select all the modules. This will use the hardcoded default list of
   packages. In Java 9: from modules java.base, java.compiler, java.desktop, and
   java.sql. Before Java 9: select rt.jar. 
   (in CreateStubsAction, add additionally required packages there).
- Alternatively, select the package(s) for which to (re-)create stubs.

- Context menu > jdt.ui test tools > Create stubs in project...
- Enter name of target project (e.g. "stubs9") and click OK. Use a new project
   or one that doesn't contain any JRE System Library on the build path -- we
   want a self-sufficient source folder that doesn't depend on binaries.

- Team > Share Project... > Git, and commit a base version in case you later
   want to revert.

- Manually remove everything from the java.beans package, except for:
PropertyChangeEvent
PropertyChangeListener

- Manually remove the following: (hint: use Ctrl+Shift+C to comment lines or
   Ctrl+Shift+/ to comment supertypes)
  - default-accessible types with errors (Java Browsing perspective or
     "Java Type Indicator" decorator help :-)
  - all methods/implements clauses with references to javax, sun, or java.time.*
     (could maybe generate java.time* in the future if this avoids manual work)

- Make sure there are no compile errors remaining.

- Create a module-info.java that declares module java.base and exports all
   packages.

- On project preference page 'Java Compiler', uncheck all options for
   'Classfile Generation'

- Export generated class files to rtstubs*.jar
