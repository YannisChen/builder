/** fakery */
package com.tibco.sample.plugin.sub;

import com.tibco.sample.plugin.Dummy;

public class SubDummy extends Dummy
{
    public SubDummy(String thingString) 
    {
        super(thingString);
    }
    
    public String getWhatsit()
    {
        return m_whatsit;
    }
    
    public void setWhatsit(String whatsit)
    {
        m_whatsit = whatsit;
    }
    
    private static final long serialVersionUID = -1514795637200116000L;
    private String m_whatsit;
}