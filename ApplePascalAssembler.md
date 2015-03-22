# Introduction #

Apple Pascal was a port of UCSD Pascal for the Apple II series.  It included a 6500-series assembler based on UCSD Adaptable Assembler and "The Last Assembler" (TLA) from the University of Waterloo.

# Constants #

Numbers are hexadecimal unless followed by a `.`.  Numbers must begin with a number (which is to say `FF` should be written as `0FF`

# Operators #

Unary operators:

+, -, ~

Binary operators

+, -,  ^, `*`, /, %,  |, &, =, <>

= and <> are only valid in `.IF` expressions.

All operators have the same precedence but may be grouped with < and >.


# Identifiers #

`[A-Za-z][A-Za-z0-9_]+`

# Labels #

may optionally be followed by a colon.


## Local Labels ##

have $ as the first character, limit of 21.  local labels are invalidated when the next non-local label is encountered.
May not be used with the `.EQU` directive.


# Directives #

`    .PROC <identifier>[,expression]`
`    .FUNC <identifier>[,expression]`

Identify a procedure or function, respectively.  An option argument count (default is 0) may be provided which indicates the number of word parameters that will be passed in.

The function (or procedure) ends via `.END`, `.PROC`, or `.FUNC`

`[label] .ASCII "<character string>"`

generate text.

`[label] .BYTE [valuelist]`

Generates one or more bytes.

`[label] .BLOCK <length>[,value]`

Generates `<length>` bytes of `<value>` (default is 0)

`[label] .WORD <valuelist>`

Generates one or more 16-bit words.

`<label> .EQU <value>`

Assigns a value to a label.  `<label>` may not be a local label. `$` denotes the current location counter.


`    .ORG <value>`

Generates 0s until the location counter is equal to `<value>`

`    .ABSOLUTE`

Cause `.ORG` to be interpreted as an absolute memory location. Allows labels to be treated as absolute numbers. Must be declared before the first `.PROC` or `.FUNC`


`    .INTERP`

???


# Macro Directives #

```
    .MACRO <identifier>
    ; macro body
   .ENDM
```

Parameters (up to 9) may be referenced as `%1` ... `%9`

# Conditional Assembly Directives #

`[label] .IF <expression>`
`    .ELSE`
`    .ENDC`

# Other Directives #

`.DEF`
`.REF`
`.LIST`
`.NOLIST`
`.MACROLIST`
`.NOMACROLIST`
`.PATCHLIST`
`.NOPATCHLIST`
`.PAGE`
`.TITLE`
`.CONST`
`.PUBLIC`
`.PRIVATE`
`.INCLUDE`