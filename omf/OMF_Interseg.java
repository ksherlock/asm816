/*
 * Created on Feb 13, 2006
 * Feb 13, 2006 10:10:18 PM
 */
package omf;

/*
 * $E3 INTERSEG - This record is used in the relocation dictionary of a load
 * segment, and contains a patch to a long call to an external reference. The
 * INTERSEG record is used to patch an address in a load segment with a
 * reference to another address in a different load segment. It contains two
 * 1-byte counts followed by an offset, a 2-byte file number, a 2-byte segment
 * number, and a second offset. The first count is the number of bytes to be
 * relocated, and the second count is a bit-shift operator, telling how many
 * times to shift the relocated address before inserting the result into memory.
 * If the bit-shift operator is positive, then the number is shifted to the
 * left, filling vacated bit positions with 0’s. If the bit-shift operator is
 * negative, then the number is shifted right. The first offset is the location,
 * relative to the start of the segment, of the number that is to be relocated.
 * If the reference is to a static segment, then the file number, segment
 * number, and second offset correspond to the subroutine referenced. The file
 * number is always one. For example, suppose the segment includes an
 * instruction like
 * 
 * jsl ext
 * 
 * where the label ext is an external reference to a location in a static
 * segment. If this instruction is at relative address $720 within its segment
 * and ext is at relative address $345 in segment $000A in file $0001, then the
 * linker creates an INTERSEG record in the relocation dictionary that looks
 * like this (note that the values are stored low-byte first, as specified by
 * NUMSEX):
 * 
 * E3030020 07000001 000A0045 030000
 * 
 * which corresponds to the following values:
 * 
 * $E3 operation code $03 number of bytes to be relocated $00 bit-shift operator
 * $00000720 offset of instruction $0001 file number $000A segment number
 * $00000345 offset of subroutine referenced
 * 
 * When the loader processes the relocation dictionary, it uses the second
 * offset to find the JSL, and patches in the address corresponding to the file
 * number, segment number, and offset of the referenced subroutine. INTERSEG
 * records are used for any long-address reference to a static segment
 * 
 */
public class OMF_Interseg extends OMF_Relocation
{
    //private int fNum;
    //private int fBitshift;
    //private int fOffset;
    private int fFile;
    private int fSegment;
    //private int fValue;
    
    public OMF_Interseg(__OMF_Reader omf)
    {
        super(0xe3);
        fNum = omf.Read8();
        fBitshift = omf.Read8();
        fOffset = omf.ReadNumber();
        fFile = omf.Read16();
        fSegment = omf.Read16();
        fValue = omf.ReadNumber();
    }
    public OMF_Interseg(int number, int shift, int offset, int file, int segment, int value)
    {
        super(0xe3);
        fNum = number;
        fBitshift = shift;
        fOffset = offset;
        fFile = file;
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
        out.WriteNumber(fOffset);
        out.Write16(fFile);
        out.Write16(fSegment);
        out.WriteNumber(fValue);
        
    }
    @Override
    public boolean IsInterseg()
    {
        return true;
    }
    
    public int File()
    {
        return fFile;
    }
    public int Segment()
    {
        return fSegment;
    }
    
    
    /*
     * * $F6 cINTERSEG – This record is the compressed version of the INTERSEG
     * record. It is identical to the INTERSEG record, except that the offsets
     * are 2 bytes long rather than 4 bytes, the segment number is 1 byte rather
     * than 2 bytes, and it does not include the 2-byte file number. The
     * cINTERSEG record can be used only if both offsets are less than $FFFF
     * (65535), the segment number is less than 256, and the file number
     * associated with the reference is 1. References to segments in
     * run-time-library files must use INTERSEG records rather than cINTERSEG
     * records. 
     */
    @Override
    public OMF_Relocation Compress()
    {
        if (fFile == 1 && fSegment <= 0xff && fOffset <= 0xffff && fValue <= 0xffff)
        {
            return new OMF_CInterseg(fNum, fBitshift, fOffset, fSegment, fValue);
        }
        // TODO generate OMF_CInterseg
        return this;
    }
}
