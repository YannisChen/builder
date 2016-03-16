package com.tibco.devtools.workspace.installer.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;

public abstract class ResourceList<T1, T2 extends AnyItem<T1>>
    extends DataType
{
    public ResourceList(Project p)
    {
        super();
        setProject(p);
        m_resources = new ArrayList<T2>();
    }

    public void add(T2 var)
    {
        m_resources.add(var);
    }
    
    public void addAll(List<T2> var)
    {
        for(T2 item : var){
            if(!m_resources.contains(item)){
        	    add(item);
            }
        }
    }

    public boolean isEmpty()
    {
        return m_resources.isEmpty();
    }

    public void clear()
    {
        m_resources.clear();
    }

    public List<T1> normalizeAnyItems() throws BuildException
    {
        List<T2> input = getResources();
        List<T1> result = new ArrayList<T1>(input == null ? 0 : input.size());
        if (input != null)
        {
            for (AnyItem<T1> item : input)
            {
                if (item.checkNullVariable() != null)
                    result.add(item.getValue() );
            }
        }

        return result;
    }

    protected abstract ResourceList<T1, T2> getReferencedObject() throws BuildException;

    protected List<T2> getResources() throws BuildException
    {
        if (isReference())
        {
            // no test for circularity.
            ResourceList<T1, T2> refObj = getReferencedObject();
            if (refObj != null)
                return refObj.getResources();
        }
        return m_resources;
    }

    protected List<T2> m_resources;
}
