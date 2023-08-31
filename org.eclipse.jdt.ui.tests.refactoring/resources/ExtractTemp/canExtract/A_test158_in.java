package p; // 17, 28, 17, 71

import java.util.LinkedHashMap;
import java.util.Map;

public class A {
	private static final Version VERSION_1_4_0_M3= Version.parse("1.4.0.M3");

	private static final Version VERSION_2_0_0_M1= Version.parse("2.0.0.M1");

	private static final Version VERSION_2_0_0_M6= Version.parse("2.0.0.M6");

	protected void resolveModel(ProjectRequest originalRequest) {
		ProjectRequest request= new ProjectRequest();
		// Kotlin supported as of M6
		final boolean kotlinSupport= VERSION_2_0_0_M6
				.compareTo(Version.safeParse(request.getBootVersion())) <= 0;
	}
}

class Version {
	private String version;

	public Version() {
		version= "";
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version= version;
	}

	public static Version safeParse(String version) {
		try {
			return parse(version);
		} catch (Exception ex) {
			return null;
		}
	}

	public int compareTo(Version v1) {
		if (v1.getVersion().equals(this.version))
			return 0;
		else
			return 1;
	}

	public static Version parse(String version) {
		Version v= new Version();
		v.setVersion(version);
		return v;
	}
}

class ProjectRequest {

	String bootVersion= "";

	public String getBootVersion() {
		return bootVersion;
	}

	public void setBootVersion(String bootVersion) {
		this.bootVersion= bootVersion;
	}

}
