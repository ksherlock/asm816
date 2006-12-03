/*
 * Created on Dec 1, 2006
 * Dec 1, 2006 3:11:38 PM
 */
package expression;

import java.io.PrintStream;
import java.util.ArrayList;

import asm816.SymbolTable;

import omf.OMF;
import omf.OMF_Equ;
import omf.OMF_Expr;
import omf.OMF_Expression;
import omf.OMF_Opcode;
import omf.OMF_RelExpr;

public class Expression implements __Expression
{
    public Expression()
    {
        fSize = 0;
        fType = OMF.OMF_EXPR;
        fName = null;
        fData = null;
    }
    public Expression(__Expression e)
    {
        this();
        fData = e;
    }

    
    @SuppressWarnings("unchecked")
    public OMF_Opcode toOpcode(String root)
    {
        ArrayList expr = fData.Serialize(root);
        // need to end it.
        expr.add(new Integer(OMF_Expression.EXPR_END));
        
        switch(fType)
        {

        case OMF.OMF_GEQU:
        case OMF.OMF_EQU:
            return new OMF_Equ(fType, fName, 0, 'N', false, expr);
            
        case OMF.OMF_RELEXPR:
            // TODO -- need to verify relexpressions
            return new OMF_RelExpr(fSize, 1, expr);
        case OMF.OMF_EXPR:
        case OMF.OMF_LEXPR:
        case OMF.OMF_BKEXPR:
        case OMF.OMF_ZPEXPR:
            return new OMF_Expr(fType, fSize, expr);
        case 0:
            return new OMF_Expr(OMF.OMF_EXPR, fSize, expr);
        }
        // should throw error
        return null;
    }  
    
    public __Expression Simplify(SymbolTable st, boolean deep)
    {
        if (fData != null)
        {
            fData = fData.Simplify(st, deep);
        }
        return fData;
    }    
    
    public void PrintValue(PrintStream ps)
    {
        if (fData != null)
            fData.PrintValue(ps);
    }    
    
    public Integer Value()
    {
        return fData == null ? null : fData.Value();
    }
    public String toString()
    {
        return fData == null ? null : fData.toString();
    }
    
    public ArrayList Serialize(String root)
    {
        return null;
    }
    
    public boolean isConst()
    {
        return fData == null ? fData.isConst() : false;
    }
    
    
    public int Type()
    {
        return fType;
    }
    public void SetType(int type)
    {
        fType = type;
    }
    
    public String Name()
    {
        return fName;
    }
    public void SetName(String name)
    {
        fName = name;
    }
    public int Size()
    {
        return fSize;
    }
    public void SetSize(int size)
    {
        fSize = size;
    }
    

    

    
    private int fSize;
    private String fName;
    private int fType;
    private __Expression fData;
}
