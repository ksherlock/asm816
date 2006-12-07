package mpw;
/*
 * Created on Nov 29, 2006
 * Nov 29, 2006 10:20:25 PM
 */

public enum MPW_Directive
{
    /*
     * 
     */
    IMPLIED_ANOP,
    /*
     * code and data module definitions
     */
    PROC,
    ENDPROC,
    /*
    FUNC,
    ENDFUNC,
    MAIN,
    ENDMAIN,
    */
    CODE,
    DATA,
    END,
    
    /*
     * symbol definitions 
     */
    
    EQU,
    SET,
    
    /*
     * data definitions
     */
    DC,
    DCB,
    DS,
    
    /*
     * records
     */
    RECORD,
    ENDR,
    WITH,
    ENDWITH,
    
    /*
     * linker and scope
     */
    
    EXPORT,
    ENTRY,
    IMPORT,
    CODEREFS,
    DATAREFS,
    SEG,
    COMMENT,
    
    /*
     * Assembly options
     */
    
    MACHINE,
    STRING,
    CASE,
    BLANKS,
    
    ALIGN,
    ORG,
    
    /*
     * file control
     */
    INCLUDE,
    LOAD,
    DUMP,
    ERRLOG,
    
    /*
     * Listing control
     */
    PAGESIZE,
    TITLE,
    PRINT,
    EJECT,
    SPACE,
    
    /*
     * Macros
     */
    MACRO,
    ENDM,
    MEND,
    
    /*
     * aii specific
     */
    LONGA,
    LONGI,
    MSB,
    STACKDP,
    ENDSTACK,
    SEGATTR,
    INIT,
    ENDI,
    CODECHK,
    DATACHK,
    
    
    /*
     * M68k only?
     */
    MC68881,
    MC68851,
    BRANCH,
    FORWARD,
    OPT,
    REG,
    FREG,
    OPWORD,
    
    
    
}
