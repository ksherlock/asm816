/*
 * Created on Feb 13, 2006
 * Feb 13, 2006 10:25:37 PM
 */
package omf;

import java.util.ArrayList;

/*
 * $EE RELEXPR - the first byte is the number of bytes to generate, and is
 * <=NUMLEN. This is followed by a NUMLEN byte displacement from the current
 * location counter, which is the origin for a relative branch. An expression of
 * the same format as that for $EB follows this value. The expression is
 * resolved as a NUMLEN byte absolute address, then a relative branch is
 * generated from the origin to the computed destination. The result is
 * truncated to the needed number of bytes, and checked to insure that no range
 * errors resulted from the truncation.
 */
public class OMF_RelExpr extends OMF_Opcode
{
    private int fGenBytes;
    private int fDisplace;
    private ArrayList fExpr;
    public OMF_RelExpr(__OMF_Reader omf)
    {
        super(0xee);
        fGenBytes = omf.Read8(); 
        fDisplace = omf.ReadNumber();
        fExpr = OMF_Expression.ReadExpression(omf);
    }
    public OMF_RelExpr(int bytes, int displace, ArrayList expr)
    {
        super(0xee);
        fGenBytes = bytes;
        fDisplace = displace;
        if (expr != null)
            fExpr = (ArrayList)expr.clone();
        else
            fExpr = new ArrayList();
    }
    public ArrayList Expression()
    {
        return fExpr;
    }
    public int CodeSize()
    {
        return fGenBytes;
    }
    public int Displacement()
    {
        return fDisplace; // always 1 for 6502/65816.
    }
    @Override
    public void Save(__OMF_Writer out)
    {
        out.Write8(fOpcode);
        out.Write8(fGenBytes);
        out.WriteNumber(fDisplace);
        OMF_Expression.WriteExpression(fExpr, out);       
    }
    
}
