/*
 * Created on Dec 28, 2005
 * 
 * EQU/GEQU support.
 *
 */
package omf;

import java.util.ArrayList;

public class OMF_Equ extends OMF_Local {

    private ArrayList fExpr;

    public OMF_Equ(int type, __OMF_Reader omf)
	{
	    super(type, omf);
        fExpr = OMF_Expression.ReadExpression(omf);
	}
    public OMF_Equ(int type, String label, int length, int attr, boolean priv, ArrayList expr)
    {
        super(type, label, length, attr, priv);
        fExpr = expr;
    }

	public int CodeSize()
	{
		return 0;
	}

	public ArrayList Expression()
	{
	    return fExpr;
	}
    public void Save(__OMF_Writer out)
    {
        super.Save(out);
        OMF_Expression.WriteExpression(fExpr, out);
    }

	
}
