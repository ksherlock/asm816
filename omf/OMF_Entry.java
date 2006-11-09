/*
 * Created on Feb 13, 2006
 * Feb 13, 2006 11:32:37 PM
 */
package omf;

/*
 * $F4 ENTRY - This record is used in a run-time library dictionary. It contains
 * a two-byte segment number, followed by a label that is the name of the code
 * segment or global entry point. Run-time libraries are not used on the Apple
 * IIGS, since tools serve the same purpose much more effectively.
 */

/*
 * COFF documentation states there is an offset between the segment and the label.
 */
public class OMF_Entry extends OMF_Opcode
{
    private int fSegment;
    private int fOffset;
    private String fLabel;
    
    public OMF_Entry(__OMF_Reader omf)
    {
        super(0xf4);
        fSegment = omf.Read16();
        fOffset = omf.ReadNumber();
        fLabel = omf.ReadString();
    }
    
    public OMF_Entry(int segment, int offset, String label)
    {
        super(0xf4);
        fSegment = segment;
        fOffset = offset;
        fLabel = label;
    }
    
    public String toString()
    {
        return fLabel;
    }
    public int Segment()
    {
        return fSegment;
    }
    public int Offset()
    {
        return fOffset;
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
        out.Write16(fSegment);
        out.WriteNumber(fOffset);
        out.WriteString(fLabel);
    }
}
