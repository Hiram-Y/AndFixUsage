package com.qihuan.andfixdemo.base;
/**
 * 补丁json对象
 */
public class ApathConfig{
	//下载地址
	private String apatchUrl;
	//补丁版本
	private String apatchVersion;
	//补丁名字 （可用于判断本地补丁的唯一性）
	private String apatchName;
	//补丁MD5 （判断补丁是否完整）
	private String apatchMd5;

	public String getApatchUrl() {
		return apatchUrl;
	}

	public void setApatchUrl(String apatchUrl) {
		this.apatchUrl = apatchUrl;
	}

	public String getApatchVersion() {
		return apatchVersion;
	}

	public void setApatchVersion(String apatchVersion) {
		this.apatchVersion = apatchVersion;
	}

	public String getApatchName() {
		return apatchName;
	}

	public void setApatchName(String apatchName) {
		this.apatchName = apatchName;
	}

	public String getApatchMd5() {
		return apatchMd5;
	}

	public void setApatchMd5(String apatchMd5) {
		this.apatchMd5 = apatchMd5;
	}
}