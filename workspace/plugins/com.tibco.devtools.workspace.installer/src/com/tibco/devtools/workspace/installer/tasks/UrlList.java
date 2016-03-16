package com.tibco.devtools.workspace.installer.tasks;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

public class UrlList extends ResourceList<URL, AnyItem.UrlItem>
{
    public UrlList(Project p)
    {
        super(p);
    }

    public void addUpdateSite(AnyItem.UrlItem url)
    {
        add(url);
    }
    
    public void addOtherUrlList(UrlList list)
    {
        m_subResources.add(list);
    }

    protected UrlList getReferencedObject()
        throws BuildException
    {
        Object o = getProject().getReference(getRefid().getRefId());
        if (o != null)
        {
            if (this.getClass().equals(o.getClass()))
                return (UrlList)o;
            else throw new BuildException("The object referred to by the id " + getRefid().getRefId() + " is not of type " + this.getClass());
        }
        return null;
    }
    
    protected List<AnyItem.UrlItem> getResources() throws BuildException {
        if (isReference()) {
            UrlList refObj = getReferencedObject();
            if (refObj != null) {
                addAll(refObj.m_resources);
                for (UrlList aList : refObj.m_subResources) {
                    addAll(aList.getResources());
                }
            }
        } else {
            for (UrlList aList : m_subResources) {
                addAll(aList.getResources());
            }
        }
        return m_resources;
    }

    private List<UrlList> m_subResources = new ArrayList<UrlList>();
}

