import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.Files;

public class HttpServer implements Runnable {

	public HttpServer() {
	}

	public HttpServer(int myCNo) {
		myClientNo = myCNo;
	}

	private int myClientNo;
	private static ServerSocket serverSocket = null;
	private static Socket[] server = new Socket[50]; // max client No = 50
	private static Thread[] worker = new Thread[50];

	public static void main(String[] args) {
		System.out.println("Http Server: ");
		int portNo = 16000;
		if (args.length > 1) {
			System.out.println("Too many arguments. Just type a valid port No as an input.");
			return;
		}
		try {
			portNo = Integer.parseInt(args[0]);
		} catch (Exception ex) {
			System.out.println(
					" Invalid port Number.\n A valid port number must be typed after program command (e.g. HttpServer 15000).");
			return;
		}

		if (portNo > 65535 || portNo < 1) {
			System.out.println(" Invalid port number.\n Your port number must be between 1 and 65535.");
			return;
		}

		try {
			serverSocket = new ServerSocket(portNo);

		} catch (Exception e) {
			System.out.println("Server unable to listen on port: " + Integer.toString(portNo) + "\n" + e.getMessage());
			return;
		}
		int clientNo = 0;
		while (!Thread.currentThread().isInterrupted()) {
			try {
				System.out
						.println("Waiting for client " + clientNo + " on port " + serverSocket.getLocalPort() + "...");
				server[clientNo] = serverSocket.accept();
				System.out.println("client " + clientNo + " Successfully connected to "
						+ server[clientNo].getRemoteSocketAddress());
				worker[clientNo] = new Thread(new HttpServer(clientNo));
				worker[clientNo].start();
				clientNo++;
			} catch (Exception ex) {
				try {
					serverSocket.close();
					for (int i = 0; i < clientNo + 1; i++) {
						server[i].close();
						worker[i].interrupt();
						worker[i].join();
					}
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ex.printStackTrace();
			}

		}
		try {
			serverSocket.close();
			for (int i = 0; i < clientNo + 1; i++) {
				server[i].close();
				worker[i].interrupt();
				worker[i].join();
			}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			BufferedReader inB = null;
			// PrintWriter outB =null;
			InputStream clientReq = server[myClientNo].getInputStream();
			inB = new BufferedReader(new InputStreamReader(clientReq));
			// outB = new PrintWriter(server[myClientNo].getOutputStream(), true);

			OutputStream outSoc = server[myClientNo].getOutputStream();
			DataOutputStream outB = new DataOutputStream(outSoc);

			int lineNo = 1;
			String methodReq = new String("Invalid");
			String fileName = new String("Invalid");
			String httpVer = new String("Invalid");
			boolean hostSeen = false;
			boolean msgProcessed = false;
			boolean httpVerSeen = false;
			boolean fileFound = false;
			FileInputStream fIn = null;
			byte buffer[] = new byte[1023];
			while (!Thread.currentThread().isInterrupted()) {
				if (inB.ready()) {
					String req = new String(inB.readLine());
					System.out.println("[Client Request] " + req);
					try {
						if (lineNo == 1) {
							int lastSpaceInd = 0;
							lastSpaceInd = req.indexOf(" ", lastSpaceInd);
							methodReq = req.substring(0, lastSpaceInd);
							int startInd = lastSpaceInd + 1;
							lastSpaceInd = req.indexOf(" ", startInd);
							fileName = req.substring(startInd, lastSpaceInd);
							httpVer = req.substring(lastSpaceInd);
							lineNo++;
						} else {
							// check whether host is described in req MSG
							int lastSpaceInd = 0;
							lastSpaceInd = req.indexOf(" ", lastSpaceInd);
							String hostStr = req.substring(0, lastSpaceInd);
							if (hostStr.compareToIgnoreCase("Host:") == 0)
								hostSeen = true;
							lineNo++;
						}
					} catch (Exception ex) {

					}
				}

				else

				{
					File file = null;
					if ((lineNo > 1) && (!msgProcessed)) {
						/*
						 * // test parser System.out.println("Line No: " + lineNo);
						 * System.out.println("Request Method: "+methodReq);
						 * System.out.println("File Name: "+fileName);
						 * System.out.println("httpStr: "+httpVer);
						 * System.out.println("isHost seen: "+hostSeen);
						 */
						int methodNo = 0;
						if (methodReq.compareToIgnoreCase("get") == 0)
							methodNo = 1;
						else if (methodReq.compareToIgnoreCase("head") == 0)
							methodNo = 2;
						else if (methodReq.compareToIgnoreCase("options") == 0)
							methodNo = 3;
						else if (methodReq.compareToIgnoreCase("post") == 0)
							methodNo = 4;
						else if (methodReq.compareToIgnoreCase("put") == 0)
							methodNo = 5;
						else if (methodReq.compareToIgnoreCase("delete") == 0)
							methodNo = 6;
						else if (methodReq.compareToIgnoreCase("trace") == 0)
							methodNo = 7;
						else if (methodReq.compareToIgnoreCase("connect") == 0)
							methodNo = 8;

						if (httpVer.compareToIgnoreCase(" HTTP/1.1") == 0)
							httpVerSeen = true;

						String directoryName = new String("public_html");
						Path p = Paths.get(directoryName + fileName);
						try {
							fIn = new FileInputStream(p.toString());
							fileFound = true;
						} catch (Exception e) {
							System.out.println(e.getMessage());
							fileFound = false;
						}
						int statusCode = 0;
						String reasonPhrase = new String("");
						long fileLen = 0;
						if (hostSeen && httpVerSeen) {
							if ((methodNo == 1) || (methodNo == 2)) {
								if (fileFound) {
									statusCode = 200;
									reasonPhrase = "OK";
									file = new File(p.toString());
									fileLen = file.length();
								} else {
									statusCode = 404;
									reasonPhrase = "Not Found";
								}
							} else if (methodNo > 0) {
								statusCode = 501;
								reasonPhrase = "Not Implemented";
							} else {
								statusCode = 400;
								reasonPhrase = "Bad Request";
							}
						} else {
							statusCode = 400;
							reasonPhrase = "Bad Request";
						}

						// System.out.println("hostSeen: "+hostSeen+"- httpVerSeen: "+httpVerSeen );

						String extension = new String("");
						String contentType = new String("");
						int ind = fileName.lastIndexOf('.');
						if (ind > 0) {
							extension = fileName.substring(ind + 1);
						}

						if (extension.compareToIgnoreCase("html") == 0 || extension.compareToIgnoreCase("htm") == 0)
							contentType = "text/html";
						else if (extension.compareToIgnoreCase("jpg") == 0
								|| extension.compareToIgnoreCase("jpeg") == 0)
							contentType = "image/jpeg";
						else if (extension.compareToIgnoreCase("gif") == 0)
							contentType = "image/gif";
						else if (extension.compareToIgnoreCase("pdf") == 0)
							contentType = "application/pdf";
						else
							contentType = "other/" + extension;

						// Sending Header Data Back to Client;
						String statusLine = "HTTP/1.1 " + statusCode + " " + reasonPhrase + "\r\n";
						// outB.println(statusLine);
						// System.out.println("[Server Response] "+statusLine);

						// outB.println("Server: NikiHttpServer/1.0\r\n");
						// System.out.println("[Server Response] Server: NikiHttpServer/1.0\r\n");
						statusLine = statusLine + "Server: NikiHttpServer/1.0\r\n";

						// outB.println("Content-Length: "+fileLen+"\r\n");
						// System.out.println("[Server Response] Content-Length: "+fileLen+"\r\n");

						statusLine = statusLine + "Content-Length: " + fileLen + "\r\n";

						// outB.println("Content-Type: "+contentType+"\r\n");
						// System.out.println("[Server Response] Content-Type: "+contentType+"\r\n");

						statusLine = statusLine + "Content-Type: " + contentType + "\r\n";

						statusLine = statusLine + "\r\n";
						System.out.println("[Server Response]:\n" + statusLine);
						outB.writeBytes(statusLine);

						if (statusCode == 200 && methodNo == 1) // Client requested to send a file
						{
							int payloadLen = 0;
							long totalByteRead = 0;

							while ((payloadLen = fIn.read(buffer)) != -1) // until reach EOF
							{
								outB.write(buffer, 0, payloadLen);
								totalByteRead += payloadLen;

							}

							fIn.close();

							// Files.copy(file.toPath(), outB);
							// outB2.writeBytes("\r\n");

						}

						server[myClientNo].close();

						msgProcessed = true;
						return;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
