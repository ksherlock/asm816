/*
 * Created on Feb 13, 2006
 * Feb 13, 2006 10:10:18 PM
 */
package omf;

/*
 * $F6 cINTERSEG – This record is the compressed version of the INTERSEG record.
 * It is identical to the INTERSEG record, except that the offsets are 2 bytes
 * long rather than 4 bytes, the segment number is 1 byte rather than 2 bytes,
 * and it does not include the 2-byte file number. The cINTERSEG record can be
 * used only if both offsets are less than $FFFF (65535), the segment number is
 * less than 256, and the file number associated with the reference is 1.
 * References to segments in run-time-library files must use INTERSEG records
 * rather than cINTERSEG records. The following example compares an INTERSEG
 * record and a cINTERSEG record for the same reference (for an explanation of
 * each line of these records, see the discussion of the INTERSEG record):
 * 
 * INTERSEG cINTERSEG
 * 
 * $E3          $F6 
 * $03          $03 
 * $00          $00 
 * $00000720    $0720 
 * $0001 
 * $000A        $0A 
 * $00000345    $0345 
 * (15 bytes)   (8 bytes)
 * 
 * 
 */
public class OMF_CInterseg extends OMF_Relocation
{
    //private int fNum;
    //private int fBitshift;
    //private int fOffset;
    private int fSegment;
    //private int fValue;
    
    public OMF_CInterseg(__OMF_Reader omf)
    {
        super(0xf6);
        fNum = omf.Read8();
        fBitshift = omf.Read8();
        fOffset = omf.Read16();
        fSegment = omf.Read8();
        fValue = omf.Read16();
    }
    
    public OMF_CInterseg(int num, int bitshift, int offset, int segment, int value)
    {
        super(0xf6);
        fNum = num;
        fBitshift = bitshift;
        fOffset = offset;
        fSegment = segment;
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
        out.Write8(fSegment);
        out.Write16(fValue);
        
    }

    @Override
    public boolean IsInterseg()
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public OMF_Relocation Compress()
    {
        // TODO Auto-generated method stub
        return this;
    }
}
