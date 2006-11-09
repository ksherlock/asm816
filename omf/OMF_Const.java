/*
 * OMF_Data.java
 *
 * Created on December 22, 2005, 3:15 PM
 */

package omf;

/**
 *
 * @author Kelvin
 */
public class OMF_Const extends OMF_Opcode
{
    protected byte[] fData;
    protected int fLength;
    
    /** Creates a new instance of OMF_Data */
    public OMF_Const(byte[] data)
    {       
        super(0xf2);
        if (data == null)
        {
            fLength = 0;
            fData = null;
        }
        else
        {
            int length = data.length;
            fData = new byte[length];
            fLength = length;
            System.arraycopy(data, 0, fData, 0, fLength);
        }       
    }
    public OMF_Const(byte[] data, int length)
    {
        super(0xf2);
        if (data == null)
        {
            fLength = 0;
            fData = null;
        }
        else
        {
            fData = new byte[length];
            fLength = length;
            System.arraycopy(data, 0, fData, 0, fLength);
        }
    }
    protected OMF_Const()
    {
        super(0xf2);
        fData = null;
        fLength = 0;
    }

    public OMF_Const(int length, __OMF_Reader omf)
    {
        /*
         * this handles Const and LConst to make life a little easier.
         */
    	super(0xf2);
    	if (length == 0xf2)
        {
            fLength = omf.Read32();
        }
        else fLength = length;
   		fData = omf.ReadBytes(fLength);
    }
    
    public byte[] Data()
    {
        return fData;
    }

    public int CodeSize()
    {
    	return fLength;
    }

    
    @Override
    public void Save(__OMF_Writer out)
    {
        if (fData == null) return;
        if (fLength == 0) return;
        
        if (fLength < 0xe0)
        {
            out.Write8(fLength);
        }
        else
        {
            out.Write8(0xf2);
            out.Write32(fLength);
        }
        out.WriteBytes(fData, fLength);       
    }
}
