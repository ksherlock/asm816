/*
 * Created on Nov 11, 2006
 * Nov 11, 2006 3:06:14 PM
 */

public class ByteBuffer
{
    public ByteBuffer()
    {
        this(ALLOC_SIZE);
    }
    public ByteBuffer(int capacity)
    {
        fData = null;
        fAlloc = 0;
        fLength = 0;
        resizeto(capacity);       
    }
    public ByteBuffer(ByteBuffer data)
    {
        this(data.length() + ALLOC_SIZE);
        append(data);        
    }
    public ByteBuffer(byte[] data)
    {
        this(data.length);
        append(data);
    }
    public ByteBuffer(String data)
    {
        this(data.getBytes());
    }
    
    
    public int length()
    {
        return fLength;
    }
    public int capacity()
    {
        return fAlloc;
    }
    public byte[] getBytes()
    {
        if (fAlloc == 0 || fLength == 0) return new byte[0];
        if (fAlloc == fLength) return fData.clone();
        byte[] tmp = new byte[fLength];
        for (int i = 0; i < fLength; i++)
            tmp[i] = fData[i];
        return tmp;
    }
    
    public void append(int data)
    {
        append((byte)(data & 0xff));
    }
    public void append(ByteBuffer data)
    {
        append(data.getBytes(), data.length());
    }
    public void append(byte[] data)
    {
        append(data, data.length);
    }
    public void append(byte data)
    {
        resizeto(fLength + 1);
        fData[fLength++] = data;
    }
    public void append(byte[] data, int length)
    {
        resizeto(fLength + length);
        for (int i = 0; i < length; i++)
        {
            fData[fLength++] = data[i];
        }
    }
    
    private final void resizeto(int size)
    {
        if (fAlloc <= size)
        {
            fAlloc = (size + ALLOC_SIZE - 1) & (~(ALLOC_SIZE - 1));
            byte[] tmp = new byte[fAlloc];
            if (fData != null && fLength != 0)
            {
                System.arraycopy(fData, 0, tmp, 0, fLength);
            }
            fData = tmp;          
        }        
    }
    
    
    
    
    private byte []fData;
    private int fAlloc;
    private int fLength;
    
    private static final int ALLOC_SIZE = 16;
}
