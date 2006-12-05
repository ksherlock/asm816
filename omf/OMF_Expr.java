/*
 * Created on Dec 24, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package omf;

import java.util.ArrayList;

/**
 * @author Kelvin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class OMF_Expr extends OMF_Opcode {
	

	
	
	private int fGenBytes;
	//private int fPosition;
	private ArrayList fExpr;
	//private boolean fReduced;
	//private Object fValue;
	
	
    public OMF_Expr(int type, __OMF_Reader omf)
	{
		super(type);
		fGenBytes = omf.Read8();
		fExpr = OMF_Expression.ReadExpression(omf);
     		
	}
	public OMF_Expr(int type)
	{
		super(type);
		fGenBytes = 0;
		fExpr = new ArrayList();
	}
    public OMF_Expr(int type, int bytes, ArrayList expr)
    {
        super(type);
        fGenBytes = bytes;
        if (expr != null)
            fExpr = (ArrayList)expr.clone();
        else expr = new ArrayList();
    }
	public int CodeSize()
	{
		return fGenBytes;
	}
    /*
	public int Size(OMF omf)
	{
		if (fGenBytes == 0) return 0;
		
		int s = 2; // opcode + genbytes
		for(Iterator it = fExpr.iterator(); it.hasNext(); )
		{
			Object o = it.next();
			if (o instanceof OMF_Opcode)
				s += ((OMF_Opcode)o).Size(omf);
			else
				s += omf.NumberLength();
				
		}
		
		return s;
	}
    */
	
	public ArrayList Expression()
	{
	    return fExpr;
	}
    
    /*
     * this is here so EQU/GEQU can use it.
     */

    @Override
    public void Save(__OMF_Writer out)
    {
        out.Write8(fOpcode);
        out.Write8(fGenBytes);
        OMF_Expression.WriteExpression(fExpr, out);
        
    }	
}

