import java.util.ArrayList;

import omf.OMF_Const;
import omf.OMF_DS;
import omf.OMF_Opcode;

/*
 * Created on Nov 10, 2006
 * Nov 10, 2006 2:25:20 AM
 */
/*
 * basically, this is an array of OMF_Opcodes.  However,
 * it handles miscellaneous byte/string data and combines multiple
 * DS records to make life easier (and less memorable).
 */

public class JunkPile
{
    private ArrayList fData;
    private OMF_Data fTemp;
    private int fPC;
    
    public JunkPile()
    {
        fData = new ArrayList();
        fTemp = new OMF_Data();
        fPC = 0;
    }
    
    public void add8(int data, int numsex)
    {
        fTemp.AppendInt8(data, numsex);
        fPC++;
    }
    public void add16(int data, int numsex)
    {
        fTemp.AppendInt16(data, numsex);
        fPC += 2;
    }
    public void add24(int data, int numsex)
    {
        fTemp.AppendInt24(data, numsex);
        fPC += 3;
    }
    public void add32(int data, int numsex)
    {
        fTemp.AppendInt32(data, numsex);
        fPC += 4;
    }
    
    public void add(byte data)
    {
        fTemp.AppendData(data);
        fPC++;
    }
    public void add(byte[] data)
    {
        fTemp.AppendData(data);
        fPC += data.length;
    }
    public void add(String data)
    {
        add(data.getBytes());
    }
    public void add(OMF_Const data)
    {
        fTemp.AppendData(data);
        fPC += data.CodeSize();
    }
    
    /*
     * this will attempt to merge OMF_DS records.
     */
    @SuppressWarnings("unchecked")
    public void add(OMF_DS data)
    {
        int size = fData.size();
        
        if (fTemp.CodeSize() == 0 && size > 0)
        {
            Object last = fData.get(size - 1);
            if (last instanceof OMF_DS)
            {
                int ds_size = ((OMF_DS)last).CodeSize() + data.CodeSize();
                fData.set(size - 1, new OMF_DS(ds_size));
                fPC += data.CodeSize();
                return;               
            }                      
        }
        
        if (fTemp.CodeSize() > 0)
        {
            fData.add(fTemp.toConst());
            fTemp.Reset();
        }
        fData.add(data);
        fPC += data.CodeSize();
    }
    
    @SuppressWarnings("unchecked")
    public void add(OMF_Opcode data)
    {
        if (data instanceof OMF_DS)
        {
            add((OMF_DS)data);
            return;
        }
        if (data instanceof OMF_Const)
        {
            add((OMF_Const)data);
            return;
        }
        
        if (fTemp.CodeSize() > 0)
        {
            fData.add(fTemp.toConst());
            fTemp.Reset();
        }
        fData.add(data);
        fPC += data.CodeSize();
    }
    
    @SuppressWarnings("unchecked")
    public void add(Expression data)
    {
        if (fTemp.CodeSize() > 0)
        {
            fData.add(fTemp.toConst());
            fTemp.Reset();
        }
        
        fData.add(data);
        fPC += data.Size();
    }
    public void add(ArrayList data)
    {
        for (Object o : data)
        {
            add_object(o);
        }
    }
    // this has a different name so it won't be called accidently.
    public void add_object(Object data)
    {
        if (data == null) return;
        if (data instanceof OMF_Opcode)
            add((OMF_Opcode)data);
        else if (data instanceof byte[])
            add((byte[])data);
        else if (data instanceof Expression)
            add((Expression)data);
        else if (data instanceof ArrayList)
            add((ArrayList)data);
        // skip it.
    }
    
    @SuppressWarnings("unchecked")
    public ArrayList GetArrayList()
    {
        if (fTemp.CodeSize() > 0)
        {
            fData.add(fTemp.toConst());
            fTemp.Reset();           
        }
        return fData;
    }
}
