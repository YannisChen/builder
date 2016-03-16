package com.tibco.devtools.workspace.installer.tasks;

import java.net.URL;

public abstract class AnyItem<T> {

    public T getValue() {
        return m_value;
    }

    public T checkNullVariable() {
        return AnyItem.checkNullVariable(m_value);
    }

    public static <T> T checkNullVariable(T var) {
        if (var == null)
            return null;
        String value = var.toString();
        if (  (value.length() == 0)
           || (value.trim().length() == 0)
           || (value.indexOf("${") >= 0) ) {
            return null;
        }
        return var;
    }

    public static class FeatureItem extends AnyItem<String> {

        public void setFeature(String featureid) {
            m_value = featureid;
        }
    }

    public static class UrlItem extends AnyItem<URL> {
        public void setUrl(URL url) {
            m_value = url;
        }
    }

    protected T m_value;

}
