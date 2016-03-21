package com.tibco.sample.plugin;

class NotPublic
{
    NotPublic(String s)
    {
        m_notVisible = s;
    }
    
    String getNotVisible()
    {
        return m_notVisible;
    }
    
    private String m_notVisible;
}