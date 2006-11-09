/*
 * Created on Feb 2, 2006
 *
 * common root class for local/global/equ/gequ opcodes.
 */
package omf;


/**
 * @author Kelvin
 *
 * Local label (only meaningful in a DATA segment.
 *
 * [E6] LABEL LENGTH TYPE PRIVATE
 * 
 * LABEL is a lablength label
 * LENGTH is 1 byte in omf v 0 and 1, 2 bytes in omf v 2
 * TYPE is 1 byte
 * PRIVATE is 1 byte 
 */
public class OMF_Local extends OMF_Opcode {
	private String fLabel;
	private int fLength;
	private int fType;
	private boolean fPrivate;
	//private int fLocation;
	//private OMF fSegment;
	
	public OMF_Local(int opcode, __OMF_Reader omf)
	{
		super(opcode);
		fLabel = omf.ReadString();
		fLength = (omf.Version() < 2) ? omf.Read8() : omf.Read16();
		fType = omf.Read8();
		fPrivate = omf.Read8() != 0;		
	}
	public OMF_Local(int opcode, String label, int length, int type, boolean priv)
	{
		super(opcode);
		fLabel = label;
		fLength = length;
		fPrivate = priv;
		
	}
	public int CodeSize()
	{
	    return 0;
	}
	
	public boolean Private()
	{
		return fPrivate;
	}
	public int Type()
	{
		return fType;
	}
	public int Length()
	{
		return fLength;
	}

	public String toString()
	{
	    return this.fLabel;
	}
    @Override
    public void Save(__OMF_Writer out)
    {
        out.Write8(fOpcode);
        out.WriteString(fLabel);
        if (out.Version() < 2)
            out.Write8(fLength);
        else
            out.Write16(fLength);
        out.Write8(fType);
        out.Write8(fPrivate ? 1 : 0);
    }
}
