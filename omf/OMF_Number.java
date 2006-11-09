/*
 * OMF_Number.java
 *
 * Created on December 23, 2005, 1:19 PM
 */

package omf;
/**
 *
 * @author Kelvin
 */
public class OMF_Number extends OMF_Opcode
{
    private int fValue;
    /** Creates a new instance of OMF_Number */
    public OMF_Number(int opcode, __OMF_Reader omf)
    {
    	super(opcode);
    	fValue = omf.ReadNumber();   	
    }
    public OMF_Number(int opcode, int number)
    {
    	super(opcode);
    	fValue = number;
    }
    public int Value()
    {
        return fValue;
    }
    public void SetValue(int value)
    {
        fValue = value;
    }
   
    
    @Override
    public void Save(__OMF_Writer out)
    {
        out.Write8(fOpcode);
        out.WriteNumber(fValue);       
    }
    @Override
    public int CodeSize()
    {
        return 0;
    }
    
}
