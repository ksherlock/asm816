/*
 * OMF_Lablen.java
 *
 * Created on December 23, 2005, 1:04 PM
 */

package omf;
import java.lang.String;
/**
 *
 * @author Kelvin
 */
public class OMF_Label extends OMF_Opcode
{
   String fData;


    public OMF_Label(int opcode, __OMF_Reader omf)
    {
        super(opcode);
        
        fData = omf.ReadString();
    }
    
    public OMF_Label(int opcode, String label)
    {
        super(opcode);
        fData = label;
    }
    public OMF_Label(int opcode, OMF_Label l)
    {
    	super(opcode);
    	fData = l.fData;
    }
    
    public String toString()
    {
    	return fData;
    }
    
    public boolean equals(Object o)
    {
    	if (o instanceof OMF_Label)
    	{
    		return  fData.equals(((OMF_Label)o).fData);
    	}
        if (o instanceof String)
        {
            return fData.equals(o);
        }
    	return false;
    }
    public int hashCode()
    {
    	return fData.hashCode();
    }

    @Override
    public int CodeSize()
    {
        return 0;
    }

    @Override
    public void Save(__OMF_Writer out)
    {
        out.Write8(fOpcode);
        out.WriteString(fData);
        
    }
}
