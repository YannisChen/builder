package com.tibco.devtools.workspace.installer.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;


public class FeatureList extends ResourceList<String, AnyItem.FeatureItem>
{
    public FeatureList(Project p)
    {
        super(p);
    }

    public void addFeature(AnyItem.FeatureItem feature)
    {
        add(feature);
    }

    protected FeatureList getReferencedObject()
        throws BuildException
    {
        Object o = getProject().getReference(getRefid().getRefId());
        if (o != null)
        {
            if (this.getClass().equals(o.getClass()))
                return (FeatureList)o;
            else throw new BuildException("The object referred to by the id " + getRefid().getRefId() + " is not of type " + this.getClass());
        }
        return null;
    }

}

