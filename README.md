MiniJava Compiler
=================

[Version 1.0.0 - 04/15/2014]

The following is a LL(1) compiler for a subset of Java, denoted MiniJava. As of now, it appears to work for the most
part, passing the regression tests performed after each segment.

Noted bugs:
* Changing a field of an object in an array causes crashes
  This is not really a surprise, considering the Contextual Analysis aspect of the project
  is in fairly rough shape.

As of now, I do not plan on implementing more things to this, unless there is some potential for extra credit.
