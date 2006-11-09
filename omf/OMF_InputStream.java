/*
 * Created on Feb 16, 2006
 * Feb 16, 2006 8:58:38 PM
 */
package omf;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class OMF_InputStream extends ByteArrayInputStream implements
        __OMF_Reader
{
    private int fNumsex;
    private int fNumsize;
    private int fLablen;
    private boolean fOK;
    private int fVersion;
    
    public OMF_InputStream(byte[] data, OMF_Segment omf)
    {
        super(data);
        fNumsex = omf.NumberSex();
        fNumsize = omf.NumberLength();
        fLablen = omf.LabelLength();
        fOK = true;
        fVersion = omf.Version();
        
    }
    public int Read8()
    {
        if (this.available() < 1)
        {
            fOK = false;
            return 0;
        }
        return this.read();
    }

    public int Read16()
    {
        if (this.available() < 2)
        {
            fOK = false;
            return 0;
        }
        
        if (fNumsex == 0)
        {
            return this.read() | (this.read() << 8);        
        }
        else
        {
            return (this.read() << 8) | this.read();
        }
    }

    public int Read24()
    {
        if (this.available() < 3)
        {
            fOK = false;
            return 0;
        }
        
        if (fNumsex == 0)
        {
            return this.read() | (this.read() << 8) | (this.read() << 16);        
        }
        else
        {
            return (this.read() << 16) | (this.read() << 8) | this.read();
        }
    }

    public int Read32()
    {
        if (this.available() < 4)
        {
            fOK = false;
            return 0;
        }
        
        if (fNumsex == 0)
        {
            return this.read() 
                | (this.read() << 8) 
                | (this.read() << 16)
                | (this.read() << 24);
        }
        else
        {
            return (this.read() << 24)
                | (this.read() << 16) 
                | (this.read() << 8) 
                | this.read();
        }
    }

    public int ReadNumber()
    {
        switch (fNumsize)
        {
        case 1: return Read8();
        case 2: return Read16();
        case 3: return Read24();
        case 4: return Read32();
        }
        fOK = false;
        return 0;
    }

    public String ReadString()
    {
        byte[] tmp;
        int i;
        i = fLablen;
        if (i == 0)
            i = this.Read8();
        
        tmp = new byte[i];
        try {
            this.read(tmp);
        } catch (IOException e) {
            fOK = false;
            return null;
        }
        return new String(tmp);
    }
    public byte[] ReadBytes(int count)
    {
        int i;
        byte[] out = new byte[count];
        try {
            i = this.read(out);
        } catch (IOException e) {
            fOK = false;
            return null;
        }
        if (i != count) fOK = false;
        return out;
    }
    public boolean IsOK()
    {
        return fOK;
    }
    public int Version()
    {
        return fVersion;
    }
    

}
