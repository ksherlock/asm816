package expression;
import java.io.PrintStream;
import java.util.ArrayList;

import asm816.SymbolTable;


import omf.OMF_Expression;
import omf.OMF_Label;

/*
 * container for expressions that are simply symbols.
 *
 */
public class SymbolExpression implements __Expression
{
    String fString;
    boolean fReducing;
    
    public SymbolExpression(String s)
    {
        fString = s;
        fReducing = false;
    }
  
    public Integer Value()
    {
        return null;
    }
    
    public String toString()
    {
        return fString;
    }

    public int Type()
    {
        return OMF_Expression.EXPR_LABEL;
    }
    
    @SuppressWarnings("unchecked")
    public ArrayList Serialize(String root)
    {
        ArrayList out = new ArrayList();
        out.add(new OMF_Label(OMF_Expression.EXPR_LABEL, fString));
        //out.add(new Integer(OMF_Expression.EXPR_END));
        return out;
    }

    public void PrintValue(PrintStream ps)
    {
       ps.print(fString);
    }
    
    public __Expression Simplify(SymbolTable st, boolean deep)
    {
        __Expression e;

        // TODO -- should throw error.
        if (fReducing) return this; 
        fReducing = true;
        
        e = st.Get(fString);
        
        if (e == null)
        {
            fReducing = false;
            return this;
        }
        
        if (deep) e = e.Simplify(st, deep);
        
        fReducing = false;
        return e;
    }

    public boolean isConst()
    {
        return false;
    }
    
    
}
