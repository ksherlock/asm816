/*
 * Created on Feb 13, 2006
 * Feb 13, 2006 10:08:33 PM
 */
package omf;

/*
 * $E8 MEM - The operand is two absolute NUMLEN byte values specifying an
 * absolute range of memory which must be reserved. This is not needed or
 * supported on the Apple IIGS
 */
public class OMF_Mem extends OMF_Opcode
{
    private int a,b;
    public OMF_Mem(__OMF_Reader omf)
    {
        super(0xe8);
        a = omf.ReadNumber();
        b = omf.ReadNumber();
    }
    public OMF_Mem(int arg1, int arg2)
    {
        super(0xe8);
        a = arg1;
        b = arg2;
    }
    @Override
    public void Save(__OMF_Writer out)
    {
        out.Write8(fOpcode);
        out.WriteNumber(a);
        out.WriteNumber(b);
    }
    @Override
    public int CodeSize()
    {
        return 0;
    }
}
