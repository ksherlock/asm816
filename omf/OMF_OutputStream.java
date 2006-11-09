/*
 * Created on Feb 16, 2006
 * Feb 16, 2006 9:16:13 PM
 */
package omf;

import java.io.ByteArrayOutputStream;

public class OMF_OutputStream extends ByteArrayOutputStream implements __OMF_Writer
{
    private int fNumsex;
    private int fNumsize;
    private int fLablen;
    private boolean fOK;
    private int fVersion;

    public OMF_OutputStream(OMF_Segment omf)
    {
        fNumsex = omf.NumberSex();
        fNumsize = omf.NumberLength();
        fLablen = omf.LabelLength();
        fOK = true;
        fVersion = omf.Version();
    }
    
    public boolean IsOK()
    {
        return fOK;
    }

    public void Write8(int n)
    {
        this.write(n);
    }

    public void Write16(int n)
    {
        if (fNumsex == 0)
        {
            this.write(n & 0xff);
            this.write((n >> 8) & 0xff);
        }
        else
        {
            this.write((n >> 8) & 0xff);
            this.write(n & 0xff);
        }
    }

    public void Write24(int n)
    {
        if (fNumsex == 0)
        {
            this.write(n & 0xff);
            this.write((n >> 8) & 0xff);
            this.write((n >> 16) & 0xff);
        }
        else
        {
            this.write((n >> 16) & 0xff);
            this.write((n >> 8) & 0xff);
            this.write(n & 0xff);
        }
    }

    public void Write32(int n)
    {
        if (fNumsex == 0)
        {
            this.write(n & 0xff);
            this.write((n >> 8) & 0xff);
            this.write((n >> 16) & 0xff);
            this.write((n >> 24) & 0xff);
        }
        else
        {
            this.write((n >> 24) & 0xff);
            this.write((n >> 16) & 0xff);
            this.write((n >> 8) & 0xff);
            this.write(n & 0xff);
        }
    }

    public void WriteNumber(int n)
    {
        switch (fNumsize)
        {
        case 1: Write8(n); break;
        case 2: Write16(n); break;
        case 3: Write24(n); break;
        case 4: Write32(n); break;
        default:
            fOK = false;
        }
    }

    public void WriteString(String s)
    {
        if (s == null) s = "";
        int length = s.length();

        if (fLablen == 0)
        {
            this.write(length);
            this.write(s.getBytes(),0, length);
        }
        else
        {
            WriteString(s, fLablen);
        }

    }

    public void WriteString(String s, int len)
    {
        if (s == null)
            s = "";
        int length = s.length();


        if (length == len)
        {
            this.write(s.getBytes(), 0, len);
        }
        else if (length > len)
        {
            this.write(s.getBytes(), 0, len);
        }
        else
        {
            this.write(s.getBytes(), 0, length);
            for (int i = length; i < len; i++)
                this.write(' ');
        }

    }
    
    public void WriteBytes(byte[] b)
    {
        if (b == null) return;
        
        this.write(b, 0, b.length);
        
    }

    public void WriteBytes(byte[] b, int count)
    {
        if (b == null) return;
        this.write(b, 0, count);   
    }
    public int Version()
    {
        return fVersion;
    }

}
