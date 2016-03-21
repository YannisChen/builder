/** fakery */
package com.tibco.sample.plugin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;


public class Dummy
    implements Serializable
{
    public Dummy(String thingString) 
    {
        m_thing = ( thingString == null) ? "" : thingString;
    }
    
    public void setThing(String thing)
    {
        m_thing = thing;
    }

    public String getThing ()
    {
        return m_thing;
    }
    
    private void readObject(ObjectInputStream ois)
        throws ClassNotFoundException, IOException
    {
        ois.defaultReadObject();
        if (m_thing == null)
            m_thing = "";
    }
    
    private void writeObject(ObjectOutputStream oos)
        throws IOException
    {
        oos.defaultWriteObject();
    }
    
    private boolean obfuscatedMethod()
    {
        NotPublic otherThing = new NotPublic(m_thing);
        if (m_thing.equals(otherThing.getNotVisible()))
            return true;
        return false;
    }

    public void useJDK7()
    {
	String path = "";
	try (BufferedReader br = new BufferedReader(new FileReader(path))) {
		String s = "";
	}	
	catch (IOException e) {}
    }

    private String m_thing;
    private static final long serialVersionUID = -1514795637200116995L;
}
