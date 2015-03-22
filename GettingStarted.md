# Introduction #

This document aims to provide step-by-step directions to compiling, running, and using Asm816 for IIgs software development.  I assume a basic knowledge of programming, the ability to install software on your computer, and basic command line knowledge.  At the moment, this document is incomplete, but I feel that some information is better than none.


# Getting it #

The asm816 software is only available as source code.  First, you need to install Java development tools so you can compile asm816:

  * Download and install the _Java Platform JDK_ from http://java.sun.com/javase/downloads/index.jsp
  * Download and install _Eclipse for Java Developers_ from http://www.eclipse.org/downloads
  * Download and install the _Subversion_ client from http://subversion.apache.org/

  * Now, start your favorite shell such as _Terminal_ in OSX or _cmd_ on Windows.
  * Create a new directory for your work.  For this example, I'll name the directory "iigs".  Change to that directory.
```
  mkdir iigs
  cd iigs
```
  * Finally, get the source code for asm816:
```
  svn checkout http://asm816.googlecode.com/svn/trunk/ asm816-read-only
```

# Compiling it #

  * Next, start Eclipse.
  * When Eclipse prompts for a "workspace," enter the directory you created above.  In my example, the directory is "iigs".
  * Within Eclipse, select the "File->New Java Project" menu item.  Select the "Create project from existing source" option and select the "asm816-read-only" directory.  Click the "Finish" button.

# Using it #

Sorry.  I haven't written this section yet.