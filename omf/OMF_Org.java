/*
 * OMF_Org.java
 *
 * Created on December 22, 2005, 3:23 PM
 */

package omf;




/**
 *
 * @author Kelvin
 */
public class OMF_Org extends OMF_Number{
    
	public OMF_Org(__OMF_Reader omf)
	{
		super(0xe1, omf);
	}
	public OMF_Org(int org)
	{
		super(0xe1, org);
	}
    
}
