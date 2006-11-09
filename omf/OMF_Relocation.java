/*
 * Created on Feb 20, 2006
 * Feb 20, 2006 7:46:49 PM
 */
package omf;

public abstract class  OMF_Relocation extends OMF_Opcode
{
    protected int fBitshift;
    protected int fOffset;
    protected int fValue;
    protected int fNum;
    
    protected OMF_Relocation(int opcode)
    {
        super(opcode);
        fBitshift = 0;
        fValue = 0;
        fOffset = 0;
        fNum = 0;
    }
    abstract public boolean IsInterseg();
    public int Shift()
    {
        return fBitshift;
    }
    public int Offset()
    {
        return fOffset;
    }
    public int Value()
    {
        return fValue;
    }
    public int Size()
    {
        return fNum;
    }
    abstract public OMF_Relocation Compress();
}
