/*
 * Created on Feb 21, 2006
 * Feb 21, 2006 10:27:55 PM
 */
package omf;

import java.util.ArrayList;
import java.util.Iterator;

/*
 * helper class for loading/saving expressions.
 */
public final class OMF_Expression
{
    static public final int EXPR_END = 0x00;
    static public final int EXPR_ADD = 0x01;
    static public final int EXPR_SUB = 0x02;
    static public final int EXPR_MUL = 0x03;
    static public final int EXPR_DIV = 0x04;
    static public final int EXPR_MOD = 0x05;
    static public final int EXPR_NEG = 0x06;
    static public final int EXPR_SHIFT = 0x07;
    static public final int EXPR_LAND = 0x08;
    static public final int EXPR_LOR = 0x09;
    static public final int EXPR_LEOR = 0x0a;
    static public final int EXPR_LNOT = 0x0b;   
    static public final int EXPR_LE = 0x0c; 
    static public final int EXPR_GE = 0x0d;
    static public final int EXPR_NE = 0x0e; 
    static public final int EXPR_LT = 0x0f; 
    static public final int EXPR_GT = 0x10; 
    static public final int EXPR_EQ = 0x11; 
    static public final int EXPR_BAND = 0x12;
    static public final int EXPR_BOR = 0x13;
    static public final int EXPR_BEOR = 0x14;
    static public final int EXPR_BNOT = 0x15;
    
    static public final int EXPR_CLC = 0x80;
    static public final int EXPR_ABS = 0x81;
    static public final int EXPR_WEAK = 0x82;
    static public final int EXPR_LABEL = 0x83;
    static public final int EXPR_LEN = 0x84;
    static public final int EXPR_TYPE = 0x85;
    static public final int EXPR_COUNT = 0x86;
    static public final int EXPR_REL = 0x87;    
    
    
    @SuppressWarnings("unchecked")
    public static ArrayList ReadExpression(__OMF_Reader omf)
    {
        ArrayList e = new ArrayList();
        
        int op;
        
        OMF_Opcode tmp;
        do
        {           
            op = omf.Read8();
            switch (op)
            {
            case EXPR_WEAK:
            case EXPR_LABEL:
            case EXPR_LEN:
            case EXPR_TYPE:
            case EXPR_COUNT:
                tmp = new OMF_Label(op, omf);
                e.add(tmp);
                break;
            case EXPR_ABS:
            case EXPR_REL:
                tmp = new OMF_Number(op, omf);
                e.add(tmp);
                break;
            default:
                e.add(new Integer(op));
            }   
        
        } while (op != EXPR_END);  
        
        return e;
    }
    public static void WriteExpression(ArrayList expr, __OMF_Writer out)
    {
        Iterator iter;
        if (expr == null) return;
        for (iter = expr.iterator(); iter.hasNext();)
        {
            Object o = iter.next();
            if (o instanceof Integer)
            {
                out.Write8(((Integer) o).byteValue());
                continue;
            }
            if (o instanceof OMF_Opcode)
            {
                ((OMF_Opcode) o).Save(out);
            }
        }
    }
    
    // cannot be instantiated.
    private OMF_Expression()
    {
    }
}
