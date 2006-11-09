/*
 * Created on Feb 16, 2006
 * Feb 16, 2006 8:52:38 PM
 */
package omf;

public interface __OMF_Reader
{
    public int Version();
    public boolean IsOK();
    public int Read8();
    public int Read16();
    public int Read24();
    public int Read32();
    public int ReadNumber();
    public String ReadString();
    public byte[] ReadBytes(int count);
}
