Bytecode Outline plugin for Eclipse
-----------------------------------------------------------------

Bytecode Outline plugin shows disassembled bytecode of current java editor or
class file, allows bytecode compare for java/class files and shows ASMifier
code for current bytecode.

Have you already asked yourselves what the compiler does with your Java code?
Here is the answer ;)

The main reason for this plugin was my study how Java generates bytecode and my
interest in ASM framework. ASM is a great, fast and small bytecode manipulation
framework, licensed under the BSD License.

Bytecode Outline is free, see copyright. ASM is also free and parts of them are
included to Bytecode Outline plugin distribution. Please visit
http://asm.ow2.org/index.html to obtain latest information about ASM.

Bytecode Outline currently supports only Eclipse default Java editor.


Installation
-----------------------------------------------------------------

All what you need to install Bytecode Outline plugin is to unzip the zip file
into you %Eclipse%/dropins directory and restart Eclipse. You don't need to do
anymore to install Bytecode Outline.


Usage
-----------------------------------------------------------------

Window -> Show View -> Other -> Java -> Bytecode to see bytecode of current
Java editor/ Class file view.

If "Link with editor" is on, then any selection in Java editor will be followed
with selection of appropriated bytecode label, and vice - versa.

Note: this bi-directional selection could only works, if your bytecode contains
source lines/local variables information. Check your compiler settings, if you
are not sure that your compiler generates debug information.

If "show raw bytecode" is off, than local variable names will be shown instead
of indexes, full qualified names replaced with simply class names, and
primitive type abbreviations decoded to readable names.

If "show current element only" is on, then only bytecode of current
field/method node will be shown (if cursor is placed inside field/method name
or body).

Select two *.class/*.java files -> right click -> Compare with -> Each Other
Bytecode

Select one *.class/*.java file -> right click -> Compare with -> Another Class
Bytecode

to compare bytecode of selected class files. Compare works also for *.class
files included in any referenced *.jar library.
