package com.lzs.tools.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * FTP操作，主要用于上传文件、下载文件、删除文件及文件夹、获取文件加下文件名字列表
 * 
 * @author
 * @version 1.0
 * @see
 * @since
 */
public class FtpUtil {

	public static class FtpConfig {
		// ftp ip地址
		private String url;
		// ftp登录端口号
		private int port;
		// 登录用户名
		private String userName;
		// 登录密码
		private String password;

		public FtpConfig(String url, int port, String userName, String password) {
			this.url = url;
			this.port = port;
			this.userName = userName;
			this.password = password;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}

	// 日志
	private static Log logger = LogFactory.getLog(FtpUtil.class);

	/**
	 * 连接Ftp服务器
	 * 
	 * @return SUCCESS:成功 其他:失败信息
	 * @throws Exception
	 * @see [类、类#方法、类#成员] Create Author:> Time:<Aug 30, 2014> Ver:< >
	 */
	public static FTPClient getClient(FtpConfig config) throws Exception {
		try {
			if (config == null || StringUtils.isBlank(config.getUrl())) {
				throw new Exception("连接参数有误！");
			}
			FTPClient ftp = new FTPClient();

			// 不设端口 默认使用默认端口登录21
			if (config.getPort() == 0) {
				ftp.connect(config.getUrl());
			} else {
				ftp.connect(config.getUrl(), config.getPort());
			}

			// 下面三行代码必须要有,而且不能改变编码格式否则不能正确下载中文文件
			ftp.setControlEncoding("GBK");
			FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_NT);
			conf.setServerLanguageCode("zh");

			// 验证通道连接是否建立成功(回传响应码)
			if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
				ftp.disconnect();
				throw new Exception("连接通道建立失败");
			}
			ftp.enterLocalPassiveMode();
			// 登录
			if (!ftp.login(config.userName, config.password)) {
				throw new Exception("登录失败！");
			}
			logger.info("Ftp" + config.getUrl() + "登录成功!;port:" + config.getPort());
			return ftp;
		} catch (Exception e) {
			logger.error(
					"FTP连接异常，ip:" + config.getUrl() + ";port:" + config.getPort() + ";userName:" + config.getUserName(),
					e);
			throw e;
		}
	}

	/**
	 * 关闭ftp连接 <功能详细描述> void
	 * 
	 * @see [类、类#方法、类#成员] Create Author:<> Time:<Aug 30, 2014> Ver:< >
	 */
	public static void closeConnection(FTPClient ftp) {
		if (null != ftp && ftp.isConnected()) {
			try {
				InetAddress ip = ftp.getRemoteAddress();
				ftp.disconnect();
				logger.info("FTP" + ip.getCanonicalHostName() + "连接关闭");
			} catch (Exception e) {
				logger.error("FTP连接关闭异常", e);
			}
		}
	}

	/**
	 * 上传文件到ftp（会覆盖同名文件）
	 * 
	 * @param config
	 *            - 连接信息
	 * @param localFile
	 *            - 本地文件
	 * @param remoteFilePath
	 *            - 远程文件路径
	 * @param remoteFileName
	 *            - 远程文件名
	 * @return
	 * @throws Exception
	 */
	public static void uploadFile(FtpConfig config, String localFile, String remoteFilePath, String remoteFileName)
			throws Exception {
		if (StringUtils.isBlank(localFile) || StringUtils.isBlank(remoteFilePath)) {
			throw new Exception("请求参数有误");
		}
		FileInputStream in = null;
		FTPClient ftp = null;
		try {
			ftp = getClient(config);
			ftp.setFileType(FTP.BINARY_FILE_TYPE);

			if (StringUtils.isBlank(remoteFilePath)) {
				remoteFileName = localFile.substring(localFile.lastIndexOf("/") + 1);
			}
			// 确保文件路径存在
			ftp.makeDirectory(remoteFilePath);

			if (!ftp.changeWorkingDirectory(remoteFilePath)) {
				logger.error("转至目录[" + remoteFilePath + "]失败");
				throw new Exception("转至目录[" + remoteFilePath + "]失败");
			}

			// 上传之前先删除原来文件,防止重复(文件不存不报异常)
			ftp.deleteFile(remoteFileName);

			in = new FileInputStream(new File(localFile));
			ftp.storeFile(new String(remoteFileName.getBytes("GBK"), "iso-8859-1"), in);
		} catch (Exception e) {
			logger.error("上传FTP文件异常:", e);
			throw e;
		} finally {
			if (in != null) {
				in.close();
			}
			closeConnection(ftp);
		}
	}

	/**
	 * 上传文件到ftp（会覆盖同名文件）
	 * @param ftp - 连接客户端（方法内不会关闭连接）
	 * @param localFile
	 * @param remoteFilePath
	 * @param remoteFileName
	 * @throws Exception
	 */
	public static void uploadFile(FTPClient ftp, String localFile, String remoteFilePath, String remoteFileName)
			throws Exception {
		if (ftp == null) {
			throw new RuntimeException("FTPClient can not be null!");
		}
		if (StringUtils.isBlank(localFile) || StringUtils.isBlank(remoteFilePath)) {
			throw new Exception("请求参数有误");
		}
		FileInputStream in = null;
		try {
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			if (StringUtils.isBlank(remoteFilePath)) {
				remoteFileName = localFile.substring(localFile.lastIndexOf("/") + 1);
			}
			// 确保文件路径存在
			ftp.makeDirectory(remoteFilePath);

			if (!ftp.changeWorkingDirectory(remoteFilePath)) {
				logger.error("转至目录[" + remoteFilePath + "]失败");
				throw new Exception("转至目录[" + remoteFilePath + "]失败");
			}

			// 上传之前先删除原来文件,防止重复(文件不存不报异常)
			ftp.deleteFile(remoteFileName);

			in = new FileInputStream(new File(localFile));
			ftp.storeFile(new String(remoteFileName.getBytes("GBK"), "iso-8859-1"), in);
		} catch (Exception e) {
			logger.error("上传FTP文件异常:", e);
			throw e;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * 获取文件夹下文件名称列表 <功能详细描述>
	 * 
	 * @param remotePath
	 *            文件夹路径
	 * @return List<String> 文件名称列表
	 * @see [类、类#方法、类#成员] Create Author:<> Time:<Aug 20, 2014> Ver:< >
	 */
	public static List<String> getFileList(FtpConfig config, String remotePath) {
	
		if (StringUtils.isBlank(remotePath)) {
			return new ArrayList<String>(0);
		}
		FTPClient ftp = null;
		try {
			ftp = getClient(config);
			if (ftp.changeWorkingDirectory(remotePath)) {
				String[] str = ftp.listNames();
				if (null == str || str.length < 0) {
					return new ArrayList<String>(0);
				}
				return Arrays.asList(str);
			}
		} catch (Exception e) {
			logger.error("获取文件列表异常", e);
			e.printStackTrace();
		}
		finally {
			closeConnection(ftp);
		}
		return new ArrayList<String>(0);
	}

	/**
	 * 获取文件夹下文件名称列表 <功能详细描述>
	 * 
	 * @param remotePath
	 *            文件夹路径
	 * @return List<String> 文件名称列表
	 * @see [类、类#方法、类#成员] Create Author:<> Time:<Aug 20, 2014> Ver:< >
	 */
	public static List<String> getFileList(FTPClient ftp, String remotePath) {
		if (ftp == null) {
			throw new RuntimeException("FTPClient can not be null!");
		}
		if (StringUtils.isBlank(remotePath)) {
			return new ArrayList<String>(0);
		}
		try {
			if (ftp.changeWorkingDirectory(remotePath)) {
				String[] str = ftp.listNames();
				if (null == str || str.length < 0) {
					return new ArrayList<String>(0);
				}
				return Arrays.asList(str);
			}
		} catch (Exception e) {
			logger.error("获取文件列表异常", e);
			e.printStackTrace();
		}
		
		return new ArrayList<String>(0);
	}
	
	/**
	 * 从ftp下载文件到指定文件中
	 * @param config
	 * @param remoteFile
	 * @param localFile
	 * @throws Exception
	 */
	public static void download(FtpConfig config, String remoteFile, String localFile) throws Exception {
		if (StringUtils.isBlank(remoteFile) || StringUtils.isBlank(localFile)) {
			throw new Exception("参数有误");
		}
		FileOutputStream oStream = null;
		int idx = remoteFile.lastIndexOf("/") + 1;
		String remotePath = remoteFile.substring(0, idx);
		String remoteFileName = remoteFile.substring(idx);
		FTPClient ftp = null;
		try {
			ftp = getClient(config);
			// 切换到指定目录下
			if (ftp.changeWorkingDirectory(remotePath)) {
				oStream = new FileOutputStream(new File(localFile));

				if (!ftp.retrieveFile(remoteFileName, oStream)) {
					logger.error("从Ftp上下载文件失败！" + remoteFileName);
					throw new Exception("从Ftp上下载文件失败！" + remoteFileName);
				}
			} else {
				logger.error("不能正常切换至目录" + remotePath);
				throw new Exception("不能正常切换至目录" + remotePath);
			}
		} catch (Exception e) {
			logger.info("下载FTP文件异常" + remoteFileName, e);
			throw new Exception("下载FTP文件异常" + remoteFileName, e);
		} finally {
			if (oStream != null) {
				oStream.close();
			}
			closeConnection(ftp);
		}
	}

	/**
	 * 从ftp下载文件到指定文件中
	 * @param config
	 * @param remoteFile
	 * @param localFile
	 * @throws Exception
	 */
	public static void download(FTPClient ftp, String remoteFile, String localFile) throws Exception {
		if (ftp == null) {
			throw new RuntimeException("FTPClient can not be null!");
		}
		if (StringUtils.isBlank(remoteFile) || StringUtils.isBlank(localFile)) {
			throw new Exception("参数有误");
		}
		FileOutputStream oStream = null;
		int idx = remoteFile.lastIndexOf("/") + 1;
		String remotePath = remoteFile.substring(0, idx);
		String remoteFileName = remoteFile.substring(idx);
		try {
			// 切换到指定目录下
			if (ftp.changeWorkingDirectory(remotePath)) {
				oStream = new FileOutputStream(new File(localFile));

				if (!ftp.retrieveFile(remoteFileName, oStream)) {
					logger.error("从Ftp上下载文件失败！" + remoteFileName);
					throw new Exception("从Ftp上下载文件失败！" + remoteFileName);
				}
			} else {
				logger.error("不能正常切换至目录" + remotePath);
				throw new Exception("不能正常切换至目录" + remotePath);
			}
		} catch (Exception e) {
			logger.info("下载FTP文件异常" + remoteFileName, e);
			throw new Exception("下载FTP文件异常" + remoteFileName, e);
		} finally {
			if (oStream != null) {
				oStream.close();
			}
		}
	}
	
	/**
	 * 从ftp下载文件到指定文件
	 * @param config
	 * @param remoteFile
	 * @param localFile
	 * @throws Exception
	 */
	public static void download(FtpConfig config, String remoteFile, File localFile) throws Exception {
		if (StringUtils.isBlank(remoteFile) || localFile == null) {
			throw new Exception("参数有误");
		}
		FileOutputStream oStream = null;
		int idx = remoteFile.lastIndexOf("/") + 1;
		String remotePath = remoteFile.substring(0, idx);
		String remoteFileName = remoteFile.substring(idx);
		FTPClient ftp = null;
		try {
			ftp = getClient(config);
			// 切换到指定目录下
			if (ftp.changeWorkingDirectory(remotePath)) {
				oStream = new FileOutputStream(localFile);

				if (!ftp.retrieveFile(remoteFileName, oStream)) {
					logger.error("从Ftp上下载文件失败！" + remoteFileName);
					throw new Exception("从Ftp上下载文件失败！" + remoteFileName);
				}
			} else {
				logger.error("不能正常切换至目录" + remotePath);
				throw new Exception("不能正常切换至目录" + remotePath);
			}
		} catch (Exception e) {
			logger.info("下载FTP文件异常" + remoteFileName, e);
			throw new Exception("下载FTP文件异常" + remoteFileName, e);
		} finally {
			if (oStream != null) {
				oStream.close();
			}
			closeConnection(ftp);
		}
	}
	/**
	 * 从ftp下载文件到指定文件
	 * @param config
	 * @param remoteFile
	 * @param localFile
	 * @throws Exception
	 */
	public static void download(FTPClient ftp, String remoteFile, File localFile) throws Exception {
		if (ftp == null) {
			throw new RuntimeException("FTPClient can not be null!");
		}
		if (StringUtils.isBlank(remoteFile) || localFile == null) {
			throw new Exception("参数有误");
		}
		FileOutputStream oStream = null;
		int idx = remoteFile.lastIndexOf("/") + 1;
		String remotePath = remoteFile.substring(0, idx);
		String remoteFileName = remoteFile.substring(idx);
		try {
			// 切换到指定目录下
			if (ftp.changeWorkingDirectory(remotePath)) {
				oStream = new FileOutputStream(localFile);

				if (!ftp.retrieveFile(remoteFileName, oStream)) {
					logger.error("从Ftp上下载文件失败！" + remoteFileName);
					throw new Exception("从Ftp上下载文件失败！" + remoteFileName);
				}
			} else {
				logger.error("不能正常切换至目录" + remotePath);
				throw new Exception("不能正常切换至目录" + remotePath);
			}
		} catch (Exception e) {
			logger.info("下载FTP文件异常" + remoteFileName, e);
			throw new Exception("下载FTP文件异常" + remoteFileName, e);
		} finally {
			if (oStream != null) {
				oStream.close();
			}
		}
	}
	
	/**
	 * 从ftp下载文件到文件流（不关闭文件流）
	 * @param config
	 * @param remoteFile
	 * @param oStream
	 * @throws Exception
	 */
	public static void download(FtpConfig config, String remoteFile, OutputStream oStream) throws Exception {
		if (StringUtils.isBlank(remoteFile) || oStream == null) {
			throw new Exception("参数有误");
		}
		int idx = remoteFile.lastIndexOf("/") + 1;
		String remotePath = remoteFile.substring(0, idx);
		String remoteFileName = remoteFile.substring(idx);
		FTPClient ftp = null;
		try {
			ftp = getClient(config);
			// 切换到指定目录下
			if (ftp.changeWorkingDirectory(remotePath)) {
				if (!ftp.retrieveFile(remoteFileName, oStream)) {
					logger.error("从Ftp上下载文件失败！" + remoteFileName);
					throw new Exception("从Ftp上下载文件失败！" + remoteFileName);
				}
			} else {
				logger.error("不能正常切换至目录" + remotePath);
				throw new Exception("不能正常切换至目录" + remotePath);
			}
		} catch (Exception e) {
			logger.info("下载FTP文件异常" + remoteFileName, e);
			throw new Exception("下载FTP文件异常" + remoteFileName, e);
		} finally {
			closeConnection(ftp);
		}
	}
	

	/**
	 * 从ftp下载文件到文件流（不关闭文件流）
	 * @param config
	 * @param remoteFile
	 * @param oStream
	 * @throws Exception
	 */
	public static void download(FTPClient ftp, String remoteFile, OutputStream oStream) throws Exception {
		if (ftp == null) {
			throw new RuntimeException("FTPClient can not be null!");
		}
		if (StringUtils.isBlank(remoteFile) || oStream == null) {
			throw new Exception("参数有误");
		}
		int idx = remoteFile.lastIndexOf("/") + 1;
		String remotePath = remoteFile.substring(0, idx);
		String remoteFileName = remoteFile.substring(idx);
		try {
			// 切换到指定目录下
			if (ftp.changeWorkingDirectory(remotePath)) {
				if (!ftp.retrieveFile(remoteFileName, oStream)) {
					logger.error("从Ftp上下载文件失败！" + remoteFileName);
					throw new Exception("从Ftp上下载文件失败！" + remoteFileName);
				}
			} else {
				logger.error("不能正常切换至目录" + remotePath);
				throw new Exception("不能正常切换至目录" + remotePath);
			}
		} catch (Exception e) {
			logger.info("下载FTP文件异常" + remoteFileName, e);
			throw new Exception("下载FTP文件异常" + remoteFileName, e);
		}
	}
	
	/**
	 * 删除Ftp上的文件夹，递归删除
	 * 
	 * @param client
	 *            Ftp对象
	 * @param pathName
	 *            文件夹路径
	 * @return SUCCESS:成功 其他:失败
	 */
	public static boolean deleteDirectory(FtpConfig config, String pathName) {
		FTPClient ftp = null;
		try {
			ftp = getClient(config);
			return removeRecursive(ftp, pathName);
		} catch (Exception e) {
			logger.error("删除指定文件夹" + pathName + "异常：", e);
			return false;
		} finally {
			closeConnection(ftp);
		}
	}

	/**
	 * 删除Ftp上的文件夹，递归删除
	 * 
	 * @param client
	 *            Ftp对象
	 * @param pathName
	 *            文件夹路径
	 * @return SUCCESS:成功 其他:失败
	 */
	public static boolean deleteDirectory(FTPClient ftp, String pathName) {
		if (ftp == null) {
			throw new RuntimeException("FTPClient can not be null!");
		}
		try {
			return removeRecursive(ftp, pathName);
		} catch (Exception e) {
			logger.error("删除指定文件夹" + pathName + "异常：", e);
			return false;
		}
	}
	
	private static boolean removeRecursive(FTPClient ftp, String pathName) {
		boolean ret = true;
		try {
			FTPFile[] files = ftp.listFiles(pathName);
			if (null != files && files.length > 0) {
				for (FTPFile file : files) {
					if (file.isDirectory()) {
						ret = ret && removeRecursive(ftp, pathName + "/" + file.getName());

						// 切换到父目录，不然删不掉文件夹
						ftp.changeWorkingDirectory(pathName.substring(0, pathName.lastIndexOf("/")));
						ftp.removeDirectory(pathName);
					} else {
						if (!ftp.deleteFile(pathName + "/" + file.getName())) {
							logger.error("删除指定文件" + pathName + "/" + file.getName() + "失败!");
							ret = false;
						}
					}
				}
			}
			// 切换到父目录，不然删不掉文件夹
			ftp.changeWorkingDirectory(pathName.substring(0, pathName.lastIndexOf("/")));
			return ret && ftp.removeDirectory(pathName);

		} catch (IOException e) {
			logger.error("删除指定文件夹" + pathName + "异常：", e);
			return false;
		}

	}

	/**
	 * 删除指定文件
	 * 
	 * @param filePath
	 *            文件路径(含文件名)
	 * @see [类、类#方法、类#成员]
	 * @return SUCCESS:成功 其他:失败信息
	 */
	public static boolean deleteFile(FtpConfig config, String filePath) {
		FTPClient ftp = null;
		try {
			ftp = getClient(config);
			if (StringUtils.isNotEmpty(filePath)) {
				if (!ftp.deleteFile(filePath)) {
					logger.error("删除ftp文件" + filePath + "失败！");
					return false;
				}
			}
		} catch (Exception e) {
			logger.error("删除ftp文件" + filePath + "异常！", e);
			return false;
		} finally {
			closeConnection(ftp);
		}
		return true;
	}
	/**
	 * 删除指定文件
	 * 
	 * @param filePath
	 *            文件路径(含文件名)
	 * @see [类、类#方法、类#成员]
	 * @return SUCCESS:成功 其他:失败信息
	 */
	public static boolean deleteFile(FTPClient ftp, String filePath) {
		if (ftp == null) {
			throw new RuntimeException("FTPClient can not be null!");
		}
		try {
			if (StringUtils.isNotEmpty(filePath)) {
				if (!ftp.deleteFile(filePath)) {
					logger.error("删除ftp文件" + filePath + "失败！");
					return false;
				}
			}
		} catch (Exception e) {
			logger.error("删除ftp文件" + filePath + "异常！", e);
			return false;
		}
		return true;
	}
	public static void main(String[] args) throws IOException {
		// System.out.println(deletefile("G:/Q"));
		//
		// FtpUtil ftpUtil = new FtpUtil("192.168.132.110", "21", "a", "a");
		// System.out.println(ftpUtil.loginToFtpServer());
		//
		// System.out.println(ftpUtil.removeDirectoryALLFile("/home/tbcs/zhangvb/epay/20140820"));

		// FTPClient client=new FTPClient();
		// client.connect("192.168.132.110");
		// client.login("a","a");
		// System.out.println(client.removeDirectory("/home/tbcs/zhangvb/epay/test"));
	}
}