/*
 * Created on Feb 16, 2006
 * Feb 16, 2006 8:53:45 PM
 */
package omf;

public interface __OMF_Writer
{
    public int Version();
    public boolean IsOK();
    public void Write8(int n);
    public void Write16(int n);
    public void Write24(int n);
    public void Write32(int n);
    public void WriteNumber(int n);
    public void WriteString(String s);
    public void WriteBytes(byte[] b);
    public void WriteBytes(byte[] b, int count);
}