package com.hdekker.doors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "doors.reports")
public class Config {

	// login
	public String userName;
	public String passWord;
	public String baseURL;
	
	// DOORSng constants
	public static final String path_Auth = "/jts/auth/authrequired";
	public static final String path_Auth_Sec_Check = "/jts/auth/j_security_check";
	public static final String userVal = "j_username";
	public static final String passVal = "j_password";
	
	public static final String attr_ProjectURI = "projectURI";
	public static final String attr_TypeName = "typeName";
	public static final String uri_Path_Resources = "/rm/publish/resources";
	
	public String tempFolderForRawXML;
	
	// application specific config
	public String valueArtifactType;
	public String valueRMProject;
	
	public String getTempFolderForRawXML() {
		return tempFolderForRawXML;
	}
	public void setTempFolderForRawXML(String tempFolderForRawXML) {
		this.tempFolderForRawXML = tempFolderForRawXML;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassWord() {
		return passWord;
	}
	public void setPassWord(String passWord) {
		this.passWord = passWord;
	}
	public String getBaseURL() {
		return baseURL;
	}
	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}
	public String getValueArtifactType() {
		return valueArtifactType;
	}
	public void setValueArtifactType(String valueArtifactType) {
		this.valueArtifactType = valueArtifactType;
	}
	public String getValueRMProject() {
		return valueRMProject;
	}
	public void setValueRMProject(String valueRMProject) {
		this.valueRMProject = valueRMProject;
	}
	
}
