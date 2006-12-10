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
        return fRelative ? null : fValue;
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
    
    static public MExpression opLNot(MExpression m)
    {
        return m.fRelative 
            ? null : new ConstExpression(m.fValue.intValue() == 0 ? 1 : 0);
    }
 
    static public MExpression opNot(MExpression m)
    {
        return m.fRelative 
            ? null : new ConstExpression(~(m.fValue.intValue()));
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
        
        if (m2.fValue == 0) return null;
        
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
        
        if (m2.fValue == 0) return null;
        
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
 
    
    static public MExpression opAnd(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
            return new ConstExpression(m1.fValue & m2.fValue);
        default:
            return null;
        }
    }

    static public MExpression opOr(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
            return new ConstExpression(m1.fValue | m2.fValue);
        default:
            return null;
        }
    } 
 
    static public MExpression opEor(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
            return new ConstExpression(m1.fValue ^ m2.fValue);
        default:
            return null;
        }
    }   
    
    
    /*
     * Logical and, or, eor
     * 
     * treats input/output as boolean values
     * returns 1/0
     */
    static public MExpression opLAnd(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
            return new ConstExpression(m1.fValue != 0 && m2.fValue != 0 ? 1 : 0);
        default:
            return null;
        }
    } 
    
    static public MExpression opLOr(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
            return new ConstExpression(m1.fValue != 0 || m2.fValue != 0 ? 1 : 0);
        default:
            return null;
        }
    }    
    
    static public MExpression opLEor(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
            boolean b1,b2;
            b1 = m1.fValue != 0;
            b2 = m2.fValue != 0;
            return new ConstExpression(b1 ^ b2 ? 1 : 0);
        default:
            return null;
        }
    }
    
    /*
     * comparison functions
     * 
     * return 1/0 
     */
    static public MExpression opLE(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
        case 3:
            return new ConstExpression(m1.fValue <= m2.fValue ? 1 : 0);
        default:
            return null;
        }
    }    
 
    static public MExpression opGE(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
        case 3:
            return new ConstExpression(m1.fValue >= m2.fValue ? 1 : 0);
        default:
            return null;
        }
    }
    
    static public MExpression opLT(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
        case 3:
            return new ConstExpression(m1.fValue < m2.fValue ? 1 : 0);
        default:
            return null;
        }
    }
    
    static public MExpression opGT(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
        case 3:
            return new ConstExpression(m1.fValue > m2.fValue ? 1 : 0);
        default:
            return null;
        }
    }  
    
    
    static public MExpression opEQ(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
        case 3:
            return new ConstExpression(m1.fValue.intValue() == m2.fValue.intValue() ? 1 : 0);
        default:
            return null;
        }
    }  
    
    
    static public MExpression opNE(MExpression m1, MExpression m2)
    {
        int bits = check_bits(m1, m2);
        
        switch(bits)
        {
        case 0:
        case 3:
            return new ConstExpression(m1.fValue.intValue() != m2.fValue.intValue() ? 1 : 0);
        default:
            return null;
        }
    }  
    
    /*
     * returns:
     * 0 if both expressions are absolute numbers.
     * 1 or 2 if one is absolute, the other is relative
     * 3 if both are relative.
     */
    private static final int check_bits(MExpression m1, MExpression m2)
    {
        int bits = 0;
        if (m1.fRelative) bits |= 1;
        if (m2.fRelative) bits |= 2;
        
        return bits;
    }
}
