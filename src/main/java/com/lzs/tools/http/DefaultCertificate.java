package com.lzs.tools.http;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * 璇佷功淇′换绠＄悊鍣紙鐢ㄤ簬https璇锋眰锛�,淇′换鎵�鏈夎瘉涔�
 * 
 * @author lzs
 */
public class DefaultCertificate implements X509TrustManager {

	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	}

	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}
}