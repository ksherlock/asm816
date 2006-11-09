/*
 * OMF_Reloc.java
 *
 * Created on December 22, 2005, 5:43 PM
 */

package omf;



/**
 *
 * @author Kelvin
 */
public class OMF_CReloc extends OMF_Relocation
{
    //int fNum;
    //int fBitshift;
    //int fOffset;
    //int fValue;
    
    public OMF_CReloc(__OMF_Reader omf)
    {
        super(0xf5);
        fNum = omf.Read8();
        fBitshift = omf.Read8();
        fOffset = omf.Read16();
        fValue = omf.Read16();
    }
    
    /** Creates a new instance of OMF_Reloc */
    public OMF_CReloc(int num, int bitshift, int offset, int value)
    {
        super(0xf5);
        fNum = num;
        fBitshift = bitshift;
        fOffset = offset;
        fValue = value;
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
        out.Write8(fNum);
        out.Write8(fBitshift);
        out.Write16(fOffset);
        out.Write16(fValue);        
    }

    @Override
    public boolean IsInterseg()
    {
        return false;
    }

    @Override
    public OMF_Relocation Compress()
    {
        return this;
    }
    
}
