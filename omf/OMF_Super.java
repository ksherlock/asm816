/*
 * Created on Feb 13, 2006
 * Feb 13, 2006 11:46:59 PM
 */
package omf;

/*
 * $F7 SUPER - Super records contain a series of cRELOC, cINTERSEG and INTERSEG
 * records, compacted into a short, tabular form. The difference between a
 * compacted OMF file and an uncompacted OMF file is that compacted OMF files
 * use SUPER records to reduce space and cut down on load time. SUPER records
 * are not covered in this appendix. For details on the format of SUPER records,
 * see volume 2 of Apple IIGS GS/OS Reference.
 * 
 */
public class OMF_Super extends OMF_Opcode
{
    private int fLength;
    private int fType;
    private byte[] fData;
    public OMF_Super(__OMF_Reader omf)
    {
        super(0xf7);
        fLength = omf.Read32();
        fType = omf.Read8();
        fData = omf.ReadBytes(fLength - 1);
    }
    public int Length()
    {
        return fLength;
    }
    public byte[] Data()
    {
        return fData;
    }
    public int Type()
    {
        return fType;
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
        out.Write32(fLength);
        out.Write8(fType);
        out.WriteBytes(fData);
        
    }
}
