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
public class OMF_Reloc extends OMF_Relocation
{
    //int fNum;
    //int fBitshift;
    //int fOffset;
    //int fValue;
    
    public OMF_Reloc(__OMF_Reader omf)
    {
        super(0xe2);
        fNum = omf.Read8();
        fBitshift = omf.Read8();
        fOffset = omf.Read32();
        fValue = omf.Read32();
    }
    
    /** Creates a new instance of OMF_Reloc */
    public OMF_Reloc(int num, int bitshift, int offset, int value)
    {
        super(0xe2);
        fNum = num;
        fBitshift = bitshift;
        fOffset = offset;
        fValue = value;
    }
    
    public OMF_Relocation Compress()
    {
        if (fOffset <= 0xffff && fValue <= 0xffff)
        {
            return new OMF_CReloc(fNum, fBitshift, fOffset, fValue);
        }
        return this;
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
        out.Write32(fOffset);
        out.Write32(fValue);        
    }

    @Override
    public boolean IsInterseg()
    {
        return false;
    }
    
}
