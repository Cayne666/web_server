package web_server;

import java.beans.Encoder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server{

	public static void main(String[] args)throws IOException {
		// 初始化服务端socket并且绑定9999端口
		ServerSocket serverSocket =new ServerSocket(2345);
		System.out.println("monitor starts successfully");
		//创建一个线程池
		ExecutorService executorService = Executors.newFixedThreadPool(100);
		while (true) {
			//等待客户端的连接
			Socket socket = serverSocket.accept();
			System.out.println("receive a request");
			Runnable runnable = ()-> {
				BufferedReader bufferedReader =null;				
				try {
					InputStream in = socket.getInputStream();
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());
					bufferedReader =new BufferedReader(new InputStreamReader(in, "UTF-8"));			
					String requestLine = bufferedReader.readLine();
					System.out.println(requestLine);
					//读取一行数据
					String str;
					String rangeString=null;
					//通过while循环不断读取信息，
					while ((str = bufferedReader.readLine()).length() != 0) {
						//输出打印
						if(str.indexOf("Range")!=-1) {
							rangeString=str;
						}
						System.out.println(str);
					}
					StringTokenizer tokens = new StringTokenizer(requestLine);
					tokens.nextToken(); // 跳过method
					String fileName = tokens.nextToken();
					fileName=URLDecoder.decode(fileName, "UTF-8");//对文件名称进行url解码，防止出现中文名称导致找不到文件
					String statusLine = null;
					String contentTypeLine = null;
					String ContentRangeLine = null;
					String AcceptRangesLine = null;
					String ContentLengthLine = null;
					String entityBody = null;
					if(fileName.equals("/")) {
						statusLine = "HTTP/1.1 200 OK\r\n";
						contentTypeLine = "Content-type: text/html\r\n";
						out.writeBytes(statusLine);
						out.writeBytes(contentTypeLine);
						out.writeBytes("\r\n");
						byte[] buffer = new byte[1024];
						int bytes = 0;
						FileInputStream fis =new FileInputStream("./index.html");
						while ((bytes = fis.read(buffer)) != -1) {
							out.write(buffer, 0, bytes);
						}
						fis.close();
						out.close();
						bufferedReader.close();
						socket.close();
					}
					else {
						fileName = "." + fileName;//构造请求文件名
						System.out.println(fileName);

						FileInputStream file = null;
						boolean fileExist = true;
						// 判断请求对象是否存在
						try {
							file = new FileInputStream(fileName);
							System.out.println("FileFound!");
						} catch (FileNotFoundException e) {
							System.out.println("FileNotFound!");
							fileExist = false;
						}

						if (fileExist) {// 请求文件存在构造响应的status 和 contentType
							contentTypeLine = "Content-type: " + contentType(fileName) + "\r\n";
							statusLine = "HTTP/1.1 200 OK\r\n";
						} else {// 请求文件不存在
							statusLine = "HTTP/1.1 404\r\n";
							contentTypeLine = "Content-type: " + contentType(fileName) + "\r\n";
							//404页面内容
							String html404="<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\r\n" + 
									"<html xmlns=\"http://www.w3.org/1999/xhtml\">\r\n" + 
									"<head>\r\n" + 
									"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\r\n" + 
									"<title>404 NOT FOUND</title>\r\n" + 
									"<style type=\"text/css\">\r\n" + 
									".head404{ width:580px; height:234px; margin:50px auto 0 auto; background:url(https://www.daixiaorui.com/Public/images/head404.png) no-repeat; }\r\n" + 
									".txtbg404{ width:499px; height:169px; margin:10px auto 0 auto; background:url(https://www.daixiaorui.com/Public/images/txtbg404.png) no-repeat;}\r\n" + 
									".txtbg404 .txtbox{ width:390px; position:relative; top:30px; left:60px;color:#eee; font-size:13px;}\r\n" + 
									".txtbg404 .txtbox p {margin:5px 0; line-height:18px;}\r\n" + 
									".txtbg404 .txtbox .paddingbox { padding-top:15px;}\r\n" + 
									".txtbg404 .txtbox p a { color:#eee; text-decoration:none;}\r\n" + 
									".txtbg404 .txtbox p a:hover { color:#FC9D1D; text-decoration:underline;}\r\n" + 
									"</style>\r\n" + 
									"</head>\r\n" + 
									"\r\n" + 
									"<body bgcolor=\"#494949\">\r\n" + 
									"   	<div class=\"head404\" style=\"margin-top: 200px;\"></div>\r\n" + 
									"</body>\r\n" + 
									"</html>\r\n" + 
									" ";
							entityBody = html404;
						}
						
						//如果检测到请求文件为mp3或mp4时，如果使用的浏览器为chrome内核，则应响应浏览器的Range Header请求，否则无法对音频或视频进行拖动进度条操作
						if(contentType(fileName).equals("audio/mp3")||contentType(fileName).equals("video/mp4")) {
							if(rangeString.equals("")==false) {
								long range = Long.valueOf(rangeString.substring(rangeString.indexOf("=") + 1, rangeString.indexOf("-")));//截取range
				                //响应请求有Content-Range，Accept-Range和Content-Range，同时还要把状态码改为206
								ContentRangeLine = "Content-Range: bytes " + String.valueOf(range + "-"+(file.available() - 1)) + "/"+file.available()+"\r\n";
				                AcceptRangesLine="AcceptRange-Range: bytes\r\n";
				                ContentLengthLine="Content-Length: " + (file.available()-range) + "\r\n";
								statusLine = "HTTP/1.1 206 OK\r\n";
				                
				                System.out.println(ContentLengthLine);
				                System.out.println(ContentRangeLine);
				                System.out.println(AcceptRangesLine);
				                
				                out.writeBytes(statusLine);
				                out.writeBytes(AcceptRangesLine);
				                out.writeBytes(ContentRangeLine);		                
				                out.writeBytes(ContentLengthLine);
								
							}      
						}else
							out.writeBytes(statusLine);
						
							out.writeBytes(contentTypeLine);
						
						out.writeBytes("\r\n");
						if (fileExist) {//写入文件流
							byte[] buffer = new byte[1024];
							int bytes = 0;
							while ((bytes = file.read(buffer)) != -1) {
								out.write(buffer, 0, bytes);
							}
							file.close();
						} else {//进入404页面
							out.writeBytes(entityBody);
						}
						out.close();
						bufferedReader.close();
						socket.close();
					}
					
				}catch (IOException e) {
					e.printStackTrace();
				}
			};
			executorService.submit(runnable);
		}
	}

	private static String contentType(String fileName)

	{
		//根据文件名返回相应的contentType
		String type=fileName.substring(fileName.lastIndexOf(".")+1);
		switch(type) {
		case "htm":return "text/html";
		case "html":return "text/html";
		case "jpg":return "image/jpeg";
		case "jepg":return "image/jpeg";
		case "png":return "image/png";
		case "css":return "text/css";
		case "gif":return "image/gif";
		case "jsp":return "text/html";
		case "mp3":return "audio/mp3";
		case "mp4":return "video/mp4";
		}
		return "application/octet-stream";
		}
	
}



