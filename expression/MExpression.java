package expression;

import java.io.PrintStream;
import java.util.ArrayList;

import asm816.SymbolTable;

import omf.OMF_Expression;
import omf.OMF_Label;
import omf.OMF_Number;

/*
 * The RelExpression/ConstExpression child classes exist
 * only to reduce typing.
 * 
 */

public abstract class MExpression implements __Expression
{
    private Integer fValue;
    private boolean fRelative;

    protected MExpression(int value, boolean relative)
    {
        fRelative = relative;
        fValue = new Integer(value);
    }

    
    @SuppressWarnings("unchecked")
    public ArrayList Serialize(String root)
    {
        ArrayList out = new ArrayList();
        
        if (fRelative && root != null)
        {
            // convert to offset from the root.
            out.add(new OMF_Label(OMF_Expression.EXPR_LABEL, 
                    root));
            
            if (fValue.intValue() != 0)
            {
                out.add(new OMF_Number(OMF_Expression.EXPR_ABS, 
                        fValue.intValue()));
                out.add(new Integer(OMF_Expression.EXPR_ADD));
            }
        }
        else
        {
        
            out.add(new OMF_Number(fRelative ? 
                OMF_Expression.EXPR_REL : OMF_Expression.EXPR_ABS,
                fValue.intValue()));
        }

        //out.add(new Integer(OMF_Expression.EXPR_END));
        return out;
    }
    
    public __Expression Simplify(SymbolTable st, boolean deep)
    {
        return this;
    }

    public Integer Value()
    {
        return fValue;
    }

    public String toString()
    {
        return null;
    }

    public boolean isConst()
    {
        return true;
    }
    
    public void PrintValue(PrintStream ps)
    {
        // relative have * appended.
        ps.printf("$%1$04x%2$s", fValue.intValue(),
                fRelative ? "*" : "");
    }
    
    public int Type()
    {
        return fRelative 
            ? OMF_Expression.EXPR_REL : OMF_Expression.EXPR_ABS;
    }
    
    /*
     * static math functions.  return a cloned copy with
     *  appropriate value and relative flag.
     */
    
    static public MExpression opNegate(MExpression m)
    {
        return 
            m.fRelative 
                ? new RelExpression(-m.fValue) 
                        : new ConstExpression(-m.fValue);
    }
    
    static public MExpression opAdd(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0: // absolute + absolute = absolute
        case 3: // relative + relative = absolute
            return new ConstExpression(m1.fValue + m2.fValue);
            
        case 1:
        case 2:
            return new RelExpression(m1.fValue + m2.fValue);
        }
        return null;
    }

    static public MExpression opSub(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
              
        switch(bits)
        {
        case 0: // absolute + absolute = absolute
        case 3: // relative + relative = absolute
            return new ConstExpression(m1.fValue - m2.fValue);

        case 1:
        case 2:
            return new RelExpression(m1.fValue + m2.fValue);
        }
        return null;
    }    

    static public MExpression opMul(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
            return new ConstExpression(m1.fValue * m2.fValue);
        default:
            return null;
        }
    }
    
    static public MExpression opDiv(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        // TODO -- / 0 error?
        switch(bits)
        {
        case 0:
            return new ConstExpression(m1.fValue / m2.fValue);
        default:
            return null;
        }
    }

    static public MExpression opMod(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
            return new ConstExpression(m1.fValue % m2.fValue);
        default:
            return null;
        }
    }
    
    static public MExpression opShift(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
            int v = m1.fValue.intValue();
            int shift = m2.fValue.intValue();
            if (shift < 0) v = v >> shift;
            else v = v << shift;
            return new ConstExpression(v);
        default:
            return null;
        }
    }
    
    /*
    public Object clone()
    {
        Object copy;
        try
        {
            copy = super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
        return copy;
    }
    
    private final MExpression cloneWithValue(int value)
    {
        MExpression copy = (MExpression)clone();
        copy.fValue = new Integer(value);
        return copy;
    }
    */
    
    private static final int check_bits(MExpression m1, MExpression m2)
    {
        int bits = 0;
        if (m1.fRelative) bits |= 1;
        if (m2.fRelative) bits |= 2;
        
        return bits;
    }
}
