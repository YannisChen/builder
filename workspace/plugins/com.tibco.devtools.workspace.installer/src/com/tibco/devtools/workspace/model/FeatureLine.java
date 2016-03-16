package com.tibco.devtools.workspace.model;

public class FeatureLine{
	public final Target<VersionInfo> target;
	public final String url;
	public final String jarName;

	public FeatureLine(Target<VersionInfo> target){
		this.target= target;
		this.url= null;
		this.jarName = toJarName(target);
	}
	
	public FeatureLine(Target<VersionInfo> target, String url){
		this.target= target;
		this.url= url;
		this.jarName = toJarName(target);
	}
	
	public FeatureLine(String jarName){
		this.target = null;
		this.url= "features/" + jarName;
		this.jarName = jarName;
	}
	
	private String toJarName(Target<VersionInfo> target){
		StringBuilder sb = new StringBuilder(target.getTargetId());
		sb.append("_");
		sb.append(target.getVersion());
		sb.append(".jar");
		return sb.toString();
	}
	
	@Override
	public String toString(){
		return jarName;
	}
	
	@Override
	public int hashCode() {
		return jarName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FeatureLine other = (FeatureLine) obj;
		if (jarName == null) {
			if (other.jarName != null)
				return false;
		} else if (!jarName.equals(other.jarName))
			return false;
		return true;
	}
}