package com.lzs.tools.http;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Iterator;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * HTTP宸ュ叿绫伙紝鍙戣捣http/https璇锋眰
 * 
 * @author lzs
 * 
 */
public class HttpUtil {
	private static Logger log = Logger.getLogger(HttpUtil.class);
	//POST
	public static final String METHOD_POST = "POST";
	//GET
	public static final String METHOD_GET = "GET";
	// https
	private static String HTTP_PROTOCOL_HTTPS = "https://";
	// 瓒呮椂鏃堕棿
	public static final int CONNECT_TIMEOUT = 5000;
	// 榛樿缂栫爜
	public static final String DEFAULT_CHARSET = "UTF-8";
		
	// 璁㎎RE鐩镐俊鎵�鏈夌殑璇佷功
	private static TrustManager[] tm = { new DefaultCertificate() };
	
	// 鍜屽绯荤粺鐨勫煙鍚嶅拰璇佷功鍩熷悕銆�
	private static final AllowAllHostnameVerifier ALL_HOSTNAME_VERIFIER = new AllowAllHostnameVerifier();

	public static TrustManager[] getTm() {
		return tm;
	}

	public static void setTm(TrustManager[] tm) {
		HttpUtil.tm = tm;
	}

	/**
	 * 鍙戝嚭http鎴杊ttps璇锋眰锛屾牴鎹姹傚崗璁垽鏂姹傜被鍨�
	 * 
	 * @param url
	 *            - http鎴杊ttps璇锋眰鍦板潃,鍙傛暟闇�瑕�
	 * @param method
	 *            - GET鎴朠OST璇锋眰锛岄潪POST閮介粯璁や负GET
	 * @param content
	 *            - POST body涓殑璇锋眰鏁版嵁
	 * @return
	 * @throws Exception
	 */
	public static String request(String url, String method, String contentType, String content) {
		return request(url,method,contentType,content,CONNECT_TIMEOUT,DEFAULT_CHARSET,null);
	}

	private static void printParamsMap(Map<String, String> headers) {
		if(headers == null || headers.isEmpty()){
			return;
		}
		Iterator<String> it = headers.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			log.info(key + "=" + headers.get(key) + ";");
		}
	}

	/**
	 * 
	 * @param requestUrl
	 * @param requestMethod
	 * @param contentType
	 * @param content
	 * @param headers
	 * @return
	 */
	public static String request(String requestUrl, String requestMethod, String contentType, String content,
			int connectTimeout,String charsetName,
			Map<String,String> headers) {
		if (StringUtils.isBlank(requestUrl)) {
			throw new RuntimeException("Request url is empty.");
		}
		if(connectTimeout < 0){
			connectTimeout = 0;
		}
		if(StringUtils.isBlank(charsetName)){
			charsetName = DEFAULT_CHARSET;
		}
		log.info("start http request=================>\n" + requestUrl + "\nmethod=" + requestMethod + "\ncontent=" + content + "\n");
//		printParamsMap(headers);
		
		HttpURLConnection connection = null;
		OutputStreamWriter outputStreamWriter = null;
		InputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferReader = null;
		try {
			URL url = new URL(requestUrl);
			
			// https 鐗规畩澶勭悊
			if(requestUrl.startsWith(HTTP_PROTOCOL_HTTPS )){
				connection = (HttpsURLConnection) url.openConnection();
				setHttpsProperties((HttpsURLConnection)connection);
			}
			else{
				connection = (HttpURLConnection) url.openConnection();
			}
			// 璁剧疆璇锋眰鏂瑰紡锛圙ET/POST锛�
			connection.setRequestMethod(requestMethod);
			// 璁剧疆鏄惁inputStream outputStream 鍙敤
			if(requestMethod.equalsIgnoreCase(METHOD_POST)){
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setUseCaches(false);
			}
			//璁剧疆瓒呮椂鏃堕棿
			connection.setConnectTimeout(connectTimeout);
			//璁剧疆璇锋眰澶村睘鎬�
			setRequestProperty(connection, contentType, headers);
			
			if (METHOD_GET.equalsIgnoreCase(requestMethod)) {
				connection.connect();
			}
			// 褰撴湁鏁版嵁闇�瑕佹彁浜ゆ椂
			else {
				if(content == null){
					content = "";
				}
				// 娉ㄦ剰缂栫爜鏍煎紡锛岄槻姝腑鏂囦贡鐮�
				outputStreamWriter = new OutputStreamWriter(connection.getOutputStream(), charsetName);
				outputStreamWriter.write(content);
				outputStreamWriter.flush();
				outputStreamWriter.close();
				outputStreamWriter = null;
			}

			// 灏嗚繑鍥炵殑杈撳叆娴佽浆鎹㈡垚瀛楃涓�
			inputStream = connection.getInputStream();
			inputStreamReader = new InputStreamReader(inputStream, charsetName);
			bufferReader = new BufferedReader(inputStreamReader);
			StringBuffer buffer = new StringBuffer();
			int c;
			while ((c = bufferReader.read()) != -1) {
				buffer.append((char) c);
			}
			
			// 閲婃斁璧勬簮
			bufferReader.close();
			inputStreamReader.close();
			inputStream.close();
			inputStream = null;
			inputStreamReader = null;
			bufferReader = null;
			connection.disconnect();
			connection = null;
			
			return buffer.length() <= 0 ? null : buffer.toString();
		} catch (ConnectException ce) {
			log.error("杩炴帴鎺ュ彛瓒呮椂锛�"+requestUrl);
			throw new RuntimeException("杩炴帴鎺ュ彛瓒呮椂锛�"+requestUrl);
		} catch (SocketTimeoutException ste) {
			log.error("璋冪敤鎺ュ彛瓒呮椂锛屽湴鍧�锛�"+requestUrl);
			throw new RuntimeException("璋冪敤鎺ュ彛瓒呮椂锛屽湴鍧�锛�"+requestUrl);
		} catch (Exception e) {
			log.error("http request error:{}", e);
			throw new RuntimeException("http request error:{}"+e);
		}
		finally {
			try {
				if (bufferReader != null) {
					bufferReader.close();
				}
				if (inputStreamReader != null) {
					inputStreamReader.close();
				}
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStreamWriter != null) {
					outputStreamWriter.close();
				}
				if (connection != null) {
					connection.disconnect();
				}	
			} catch (IOException e) {
				log.error("http request error:{}", e);
				throw new RuntimeException(e);
			}
		}
	}

	private static void setHttpsProperties(HttpsURLConnection connection) 
			throws NoSuchAlgorithmException, NoSuchProviderException, KeyManagementException {
		// 鍒涘缓SSLContext瀵硅薄锛屽苟浣跨敤鎴戜滑鎸囧畾鐨勪俊浠荤鐞嗗櫒鍒濆鍖�
		SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
		sslContext.init(null, tm, new java.security.SecureRandom());
		// 浠庝笂杩癝SLContext瀵硅薄涓緱鍒癝SLSocketFactory瀵硅薄
		SSLSocketFactory ssf = sslContext.getSocketFactory();
		connection.setSSLSocketFactory(ssf);
		connection.setHostnameVerifier(ALL_HOSTNAME_VERIFIER);
	}

	/**
	 * 璁剧疆璇锋眰澶村睘鎬�,鏈夐粯璁ゅ��
	 * @param con
	 * @param contentType 
	 * @param headers
	 */
	private static void setRequestProperty(HttpURLConnection con, String contentType, Map<String, String> headers) {
		// 璁剧疆閫氱敤鐨勮姹傚睘鎬�
		con.setRequestProperty("accept", "*/*");
		con.setRequestProperty("connection", "Keep-Alive");
		con.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");

		// 璁剧疆璇锋眰灞炴�э紝鑻ヤ笌閫氱敤閲嶅锛屽垯瑕嗙洊閫氱敤
		if(headers != null && !headers.isEmpty()){
			Iterator<String> it = headers.keySet().iterator();
			while(it.hasNext()){
				String key = it.next();
				con.setRequestProperty(key, headers.get(key));
			}
		}
		if(StringUtils.isNotBlank(contentType)){
			con.setRequestProperty("Content-Type", contentType);
		}
	}

	
	/**
	 * 妯℃嫙form-data涓婁紶鏂囦欢<br>
	 * 
	 * -----------------------------7da2e536604c8<br>
	 * 銆�Content-Disposition: form-data; name="username"<br>
	 * 銆�hello word<br>
	 * 銆�-----------------------------7da2e536604c8<br>
	 * 銆�Content-Disposition: form-data; name="file1"; filename="D:\haha.txt"<br>
	 * 銆�Content-Type: text/plain<br>
	 * 銆�haha<br>
	 * 銆�hahaha<br>
	 * 銆�-----------------------------7da2e536604c8<br>
	 * 銆�Content-Disposition: form-data; name="file2"; filename="D:\huhu.txt"<br>
	 * 銆�Content-Type: text/plain<br>
	 * 銆�messi<br>
	 * 銆�huhu<br>
	 * 銆�-----------------------------7da2e536604c8--<br>
	 * 鐮旂┒涓嬭寰嬪彂鐜版湁濡備笅鍑犵偣鐗瑰緛<br>
	 * 銆�1.绗竴琛屾槸鈥� -----------------------------7d92221b604bc 鈥濅綔涓哄垎闅旂锛岀劧鍚庢槸鈥� \r\n 鈥� 鍥炶溅鎹㈣绗︺�� 杩欎釜7d92221b604bc 鍒嗛殧绗︽祻瑙堝櫒鏄殢鏈虹敓鎴愮殑銆�<br>
	 * 銆�2.绗簩琛屾槸Content-Disposition: form-data; name="file2"; filename="D:\huhu.txt";name=瀵瑰簲input鐨刵ame鍊硷紝filename瀵瑰簲瑕佷笂浼犵殑鏂囦欢鍚嶏紙鍖呮嫭璺緞鍦ㄥ唴锛夛紝
	 * 銆�3.绗笁琛屽鏋滄槸鏂囦欢灏辨湁Content-Type: text/plain锛涜繖閲屼笂浼犵殑鏄痶xt鏂囦欢鎵�浠ユ槸text/plain,濡傛灉涓婄┛鐨勬槸jpg鍥剧墖鐨勮瘽灏辨槸image/jpg浜嗭紝鍙互鑷繁璇曡瘯鐪嬬湅銆�<br>
	 * 銆�鐒跺悗灏辨槸鍥炶溅鎹㈣绗︺�� 銆�4.鍦ㄤ笅灏辨槸鏂囦欢鎴栧弬鏁扮殑鍐呭鎴栧�间簡銆傚锛歨ello word銆�<br>
	 * 銆�5.鏈�鍚庝竴琛屾槸-----------------------------7da2e536604c8--,娉ㄦ剰鏈�鍚庡浜嗕簩涓�--<br>
	 * 
	 * @param urlStr
	 *            - 涓婁紶鍦板潃
	 * @param paramMap
	 *            - 瀛楃涓插弬鏁帮紝key涓篿nput鎺т欢鐨刵ame锛寁alue涓哄叾鍊�
	 * @param fileMap
	 *            - 鏂囦欢锛宬ey涓篺ile鎺т欢name锛寁alue涓烘枃浠跺悕锛堝叏璺緞锛�
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static String postForm(String urlStr, String formContentType, Map<String, String> paramMap, Map<String, String> fileMap) {
		log.info("start http request=================>\n" + urlStr
				 + "\ncontentType=" + formContentType + "\n");
		printParamsMap(paramMap);
		String res = "";
		HttpURLConnection conn = null;
		// boundary灏辨槸request澶村拰涓婁紶鏂囦欢鍐呭鐨勫垎闅旂 ,鐢辨祻瑙堝櫒闅忔満浜х敓锛岃繖閲屽啓姝�
		String BOUNDARY = "---------------------------7d4a6d158c9";
		try {
			if(StringUtils.isBlank(formContentType)){
				formContentType = "application/form-data";
			}
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(30000);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod(METHOD_POST);
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)");
			conn.setRequestProperty("Content-Type", formContentType + "; boundary=" + BOUNDARY);

			OutputStream out = new DataOutputStream(conn.getOutputStream());
			// 鍐欏叆瀛楃涓插瀷鍙傛暟
			if (paramMap != null) {
				StringBuffer strBuf = new StringBuffer();
				Iterator iter = paramMap.entrySet().iterator();
				while (iter.hasNext()) {
					// 涓�涓敭鍊煎
					Map.Entry entry = (Map.Entry) iter.next();
					// input鎺т欢鐨刵ame
					String inputName = (String) entry.getKey();
					// input鎺т欢鐨剉alue
					String inputValue = (String) entry.getValue();
					if (inputValue == null) {
						continue;
					}
					// 鍦╞ody涓啓鍏�
					// -----------------------------7da2e536604c8
					// Content-Disposition: form-data; name="username"<br>
					//
					// hello word<br>
					strBuf.append("\r\n").append("--").append(BOUNDARY).append("\r\n");
					strBuf.append("Content-Disposition: form-data; name=\"" + inputName + "\"\r\n\r\n");
					strBuf.append(inputValue);
				}
				out.write(strBuf.toString().getBytes());
			}

			// form琛ㄥ崟涓殑鏂囦欢
			if (fileMap != null) {
				Iterator iter = fileMap.entrySet().iterator();
				while (iter.hasNext()) {
					// 涓�涓猣ile鎺т欢
					Map.Entry entry = (Map.Entry) iter.next();
					// 鎺т欢name
					String inputName = (String) entry.getKey();
					// 鎺т欢value,瀹為檯涓婃槸鏂囦欢鍚嶏紝鍏ㄨ矾寰�
					String inputValue = (String) entry.getValue();
					if (inputValue == null) {
						continue;
					}
					// 鑾峰彇骞惰鍙栨枃浠舵祦鍐欏叆body
					File file = new File(inputValue);
					String filename = file.getName();
					long filelength = file.length();
					String contentType = new MimetypesFileTypeMap().getContentType(file);
					// png鏍煎紡鏂囦欢
					if (filename.endsWith(".png")) {
						contentType = "image/png";
					}
					// 涓嶇煡閬撴枃浠剁被鍨�
					if (contentType == null || contentType.equals("")) {
						contentType = "application/octet-stream";
					}

					// 鍦╞ody涓啓鍏�
					// -----------------------------7da2e536604c8<br>
					// Content-Disposition: form-data; name="file2";
					// filename="D:\huhu.txt";filelength="284634"
					// Content-Type: text/plain
					//
					// 鏂囦欢鍐呭
					StringBuffer strBuf = new StringBuffer();
					strBuf.append("\r\n").append("--").append(BOUNDARY).append("\r\n");
					strBuf.append("Content-Disposition: form-data; " + "name=\"" + inputName + "\"; " + "filename=\"" + filename + "\";"
							+ "filelength=\"" + filelength + "\"\r\n");
					strBuf.append("Content-Type:" + contentType + "\r\n\r\n");

					out.write(strBuf.toString().getBytes());
					//
					// MultipartEntity entity = new
					// MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
					// entity.addPart("media", new FileBody((new
					// File("d:\\cl13.jpg")), "image/jpeg"));
					// //鍐欏叆鏂囦欢娴�
					DataInputStream in = new DataInputStream(new FileInputStream(file));
					int bytes = 0;
					long num = 0;
					byte[] bufferOut = new byte[1024];
					while ((bytes = in.read(bufferOut)) != -1) {
						num += bytes;
						out.write(bufferOut, 0, bytes);
					}
					System.out.print(filelength + "=======>" + num + "\n============>" + strBuf);
					in.close();
				}
			}

			// 浠�--BOUNDARY--缁撳熬
			byte[] endData = ("\r\n--" + BOUNDARY + "--\r\n").getBytes();
			out.write(endData);
			out.flush();
			out.close();

			// 璇诲彇杩斿洖鏁版嵁
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer buffer = new StringBuffer();
			int c;
			while ((c = reader.read()) != -1) {
				buffer.append((char) c);
			}
			res = buffer.length()==0?null:buffer.toString();
			reader.close();
			reader = null;
		} catch (Exception e) {
			System.out.println("鍙戦�丳OST璇锋眰鍑洪敊銆�" + urlStr);
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
				conn = null;
			}
		}
		log.info("http request return==============>" + res);
		return res;
	}

	/**
	 * 涓婁紶鏂囦欢
	 * 
	 * @param urlStr
	 *            - 璇锋眰鍦板潃
	 * @param filename
	 *            - 鏂囦欢鍚嶅叏璺緞
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public static String postMultipartEntity(String urlStr, String filename) throws IOException {

		String type = null;
		// png鏍煎紡鏂囦欢
		if (filename.endsWith(".png")) {
			type = "image/png";
		}
		// 涓嶇煡閬撴枃浠剁被鍨�
		if (type == null || type.equals("")) {
			type = "application/octet-stream";
		}

		HttpClient httpclient = new DefaultHttpClient();
		try {
			// 璁剧疆鏁版嵁
			HttpPost httppost = new HttpPost(urlStr);
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			entity.addPart("media", new FileBody((new File(filename)), type));
			httppost.setEntity(entity);
			// 鎵цpost璇锋眰
			HttpResponse response = httpclient.execute(httppost);
			// 缁撴灉杩斿洖
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == HttpStatus.SC_OK) {
				HttpEntity resEntity = response.getEntity();
				String res = EntityUtils.toString(resEntity);// httpclient鑷甫鐨勫伐鍏风被璇诲彇杩斿洖鏁版嵁
				EntityUtils.consume(resEntity);
				return res;
			} else {
				log.error("[HttpUtil]Post file failed,statusCode=" + statusCode);
			}
			return null;

		} finally {
			try {
				httpclient.getConnectionManager().shutdown();
			} catch (Exception ignore) {

			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static String getParmString( Map param) {
		StringBuffer result = null;
		if (param != null) {
			result = new StringBuffer();
			Iterator it = param.keySet().iterator();
			try {
				while (it.hasNext()) {
					String key = (String) it.next();
					result.append("&").append(key).append("=").append(URLEncoder.encode(param.get(key).toString(), "UTF-8"));
				}
				if (result.length() > 0) {
					result.deleteCharAt(0);
				}
			} catch (UnsupportedEncodingException e) {
				log.error("http request error:{}", e);
				e.printStackTrace();
			}
		}
		return result == null ? null : result.toString();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//
//		String filepath = "D:\\VID.mp4";
//		Map<String, String> fileMap = new HashMap<String, String>();
//		fileMap.put("file", filepath);
//
//		// Map<String, String> textMap = new HashMap<String, String>();
//		// textMap.put("name", "testname");
////		String token = null;
////		try {
////			token = TokenHelper.getNewToken("d555dfb635b94233b3c79eb7319a7f21", "ea1bc7058ac94b6ca7f1a95401ece8ea").getToken();
////		} catch (Exception e) {
////		}
////		System.out.print(token);
////		String urlStr = "https://api.yixin.im/cgi-bin/media/upload?access_token=" + token + "&type=video";
//
//		// String ret = postForm(urlStr, textMap, fileMap);
//		// System.out.println(ret);
//
//		try {
//			System.out.println(postMultipartEntity(urlStr, filepath));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		String s = "{\"appToken\":\"FA4F12C561646EC39CF6936446741486\",\"appUrl\":\"http://223.252.197.238/service/F40E00EC28EED3B3AD1E8660F113C1DC\",\"icon\":\"http://numen.nos.netease.com/null/image/1448861641147.png\",\"appId\":\"\",\"corpid\":227,\"state\":0,\"pid\":0,\"qrcodeUri\":\"\",\"corpName\":\"绮剧伒娴嬭瘯鍙穃",\"nick\":\"绮剧伒娴嬭瘯鍙穃",\"cardUri\":\"\",\"appSecret\":\"\"}";
//		System.out.println(request("https://api.yixin.im/private/corppa", "POST", "application/json", s));
//		
//		String[] array = new String[]{
//				"18920734959"
//		};
//		String url = "http://115.239.133.201:8081/SmsPush/send";
//		for(String s:array){
//			String result = HttpUtil.request(url, HttpUtil.METHOD_POST,null,
//					"account=yixinextend&sender=sender&receiver=" + s +"&businessID=888&"
//					+ "message=閲嶈閫氱煡锛氭渶杩戝彂鐜版湁娣樺疂鍗栧浠ラ珮浠烽潪娉曞嚭鍞晢鍔″彿鐮侊紝鎴戜滑宸蹭粙鍏ヨ皟鏌ャ�傚鏋滀綘鏄�氳繃娣樺疂璐拱鐨勫晢鍔″彿鐮侊紝璇峰敖蹇仈绯诲鏈�4008266868璇存槑鎯呭喌浠ュ厤褰卞搷浣犵殑姝ｅ父浣跨敤");
//			JSONObject json = JSONObject.parseObject(result);
//			System.out.println(json);
//		}
		
		String msg = "ddddd&uid=${UID}dddd";
		msg = msg.replaceAll("&uid=\\$\\{UID\\}", "&uid=112");
		System.out.println(msg);
		
	}
}