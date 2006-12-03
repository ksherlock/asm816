package asm816;
import omf.*;

/*
 * Created on Feb 16, 2006
 * Feb 16, 2006 8:40:30 PM
 * 
 * This class extends OMF_Const to allow 
 * resizing the data array
 * appending data
 * modifying data.
 * 
 */

public class OMF_Data extends OMF_Const
{
    private int fAlloc;
    
    
    public OMF_Data()
    {
        super();
        fAlloc = 0;
    }
    
    public OMF_Data(byte[] data)
    {
        this(data, data.length);
    }
    public OMF_Data(byte[] data, int length)
    {
        super();
        fAlloc = 0;
        ResizeTo(length);
        AppendData(data, length);
    }
    

    
    public void AppendData(byte[] data)
    {
        AppendData(data, data.length);
    }
    
    public void AppendData(byte[] data, int size)
    {
        ResizeBy(size);
        System.arraycopy(data, 0, fData, fLength, size);
        fLength += size;
    }
    public void AppendData(byte b)
    {
        ResizeBy(1);
        fData[fLength++] = b;
    }
    public void AppendData(OMF_Const data)
    {
        AppendData(data.Data(), data.CodeSize());
    }
    public void AppendData(OMF_DS ds)
    {
        AppendData(ds.CodeSize(), (byte)0);
    }
    public void AppendData(OMF_Opcode op)
    {
        if (op instanceof OMF_Const)
        {
            AppendData((OMF_Const)op);
        }
        else
        {
            int size = op.CodeSize();
            if (size > 0)
            {
                AppendData(size, (byte)0);
            }
        }
        
    }
    
    public void AppendData(int count, byte b)
    {
        ResizeBy(count);
        for (int i = 0; i < count; i++)
        {
            fData[fLength++] = b;
        }       
    }
    
    private void ResizeBy(int add)
    {
        if (fLength + add > fAlloc)
        {
            fAlloc = (fLength + add + 4095) & (~4095); 
            byte[] tmp = new byte[fAlloc];
            if (fData != null && fLength != 0)
            {
                System.arraycopy(fData, 0, tmp, 0, fLength);
            }
            fData = tmp;
        }
    }
    private void ResizeTo(int size)
    {
        if (fAlloc <= size)
        {
            fAlloc = (size + 4095) & (~4095);
            byte[] tmp = new byte[fAlloc];
            if (fData != null && fLength != 0)
            {
                System.arraycopy(fData, 0, tmp, 0, fLength);
            }
            fData = tmp;          
        }
    }
    
    public void Modify(int location, int value, int count, int numsex)
    {
        int i;
        if (numsex == 0)
        {
            for (i = 0; i < count; i++)
            {
                fData[location + i] = (byte)(value & 0xff);
                value = value >> 8;
            }
        }
        else
        {
            location += count - 1;
            for (i = 0; i < count; i++)
            {
                fData[location - i] = (byte)(value & 0xff);
                value = value >> 8;
            }            
        }
    }
    
    public void AppendInt8(int num, int numsex)
    {
        ResizeBy(1);
        fData[fLength++] = (byte)(num & 0xff);
    }
    
    public void AppendInt16(int num, int numsex)
    {
        ResizeBy(2);
        if (numsex == 0)
        {
            fData[fLength++] = (byte)(num & 0xff);
            fData[fLength++] = (byte)((num >> 8) & 0xff);
        }
        else
        {           
            fData[fLength++] = (byte)((num >> 8) & 0xff); 
            fData[fLength++] = (byte)(num & 0xff);
        }
    }
    public void AppendInt24(int num, int numsex)
    {
        ResizeBy(3);
        if (numsex == 0)
        {
            fData[fLength++] = (byte)(num & 0xff);
            fData[fLength++] = (byte)((num >> 8) & 0xff);
            fData[fLength++] = (byte)((num >> 16) & 0xff);
        }
        else
        {    
            fData[fLength++] = (byte)((num >> 16) & 0xff);
            fData[fLength++] = (byte)((num >> 8) & 0xff); 
            fData[fLength++] = (byte)(num & 0xff);
        }
    }
    public void AppendInt32(int num, int numsex)
    {
        ResizeBy(4);
        if (numsex == 0)
        {
            fData[fLength++] = (byte)(num & 0xff);
            fData[fLength++] = (byte)((num >> 8) & 0xff);
            fData[fLength++] = (byte)((num >> 16) & 0xff);
            fData[fLength++] = (byte)((num >> 24) & 0xff);
        }
        else
        {    
            fData[fLength++] = (byte)((num >> 24) & 0xff);
            fData[fLength++] = (byte)((num >> 16) & 0xff);
            fData[fLength++] = (byte)((num >> 8) & 0xff); 
            fData[fLength++] = (byte)(num & 0xff);
        }
    }
    
    public int ByteAt(int location)
    {
        if (location <0 || location >= fLength) return 0;
        return fData[location];
    }
    
    public void Reset()
    {
        fAlloc = 0;
        fLength = 0;
        fData = null;
    }
 
    public byte[] toByte()
    {
        if (fLength == 0) return null;
        byte[] out = new byte[fLength];
        System.arraycopy(fData, 0, out, 0, fLength);
        return out;
    }
    /*
    public byte[] Data()
    {
        return fData;
    }
    */    
    
    public void Save(__OMF_Writer out)
    {
        if (fLength  == 0) return;
        //if (fSize < 0xe0)
        //{
        //    out.Write8(fSize);
        //}
        //else
        //{
            out.Write8(0xf2);
            out.Write32(fLength);
        //}
        out.WriteBytes(fData, fLength);
    }
    public OMF_Const toConst()
    {
        return new OMF_Const(fData, fLength);   
    }
}
