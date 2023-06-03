package nupt.zh.remotecontrol;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 被控制端
 *
 * @author zh
 *
 */
public class Server extends JPanel implements Runnable {

	private static final long serialVersionUID = -927388268343256207L;
	private ServerSocket server;
	private Thread thread;
	private Robot controlMouseRobot;
	private JButton releaseConnect;
	private JLabel label;
	private int port;
	public Server(String ip, int port) throws IOException, AWTException {
		this.port=port;
		server = new ServerSocket(port, 1, InetAddress.getByName(ip));
		thread = new Thread(this);
		controlMouseRobot = new Robot();

		label = new JLabel("监听" + ip + ":" + port);

		releaseConnect = new JButton("断开连接");
		this.add(releaseConnect);
		releaseConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		this.add(label);
		this.add(releaseConnect);
	}

	public void start() {
		thread.start();
	}

	@SuppressWarnings("deprecation")
	public void stop() {
		label.setText("已断开连接");
		thread.stop();
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		while (true) {
			ObjectInputStream request = null;
			ObjectOutputStream response = null;
			try {
				Socket client = server.accept();
				label.setText("被控制中");
				response = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
				request = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
				ControlCarrier carrier = (ControlCarrier) request.readObject();

				System.out.println("收到命令:" + carrier);

				if (carrier.getMouseX() != -1 && carrier.getMouseY() != -1) {
					controlMouseRobot.mouseMove(carrier.getMouseX(), carrier.getMouseY());
				}

				if (carrier.getMousePressBtn() != -1) {
					controlMouseRobot.mousePress(carrier.getMousePressBtn());
				}

				if (carrier.getMouseReleaseBtn() != -1) {
					controlMouseRobot.mouseRelease(carrier.getMouseReleaseBtn());
				}

				if (carrier.getWheelAmt() != -1) {
					controlMouseRobot.mouseWheel(carrier.getWheelAmt());
				}

				for (Integer pressKey : carrier.getKeyPressCode()) {
					controlMouseRobot.keyPress(pressKey);
				}

				for (Integer releaseKey : carrier.getKeyReleaseCode()) {
					controlMouseRobot.keyRelease(releaseKey);
				}
				System.out.println(carrier.getType());
				//发送文件回客户端
				if("download".equals(carrier.getType())){
					File file=new File(carrier.getFilePath());
					//创建发送的对象
					ControlCarrier fileCarrier = new ControlCarrier();
					System.out.println("文件是否存在"+file.exists());
					//文件存在
					if(file.exists()){
						//传输文件名回去，说明文件存在
						fileCarrier.setFilePath(file.getName());
						//response = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
						response.writeObject(fileCarrier);
						response.flush();
						/*//创建文件传输socket
						ServerSocket  serverSocket= new ServerSocket(port+1);
						Socket fileSocket=serverSocket.accept();
						DataOutputStream dataOutputStream = new DataOutputStream(fileSocket.getOutputStream());*/
						DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
						FileInputStream fileInputStream = new FileInputStream(file);
						/*byte[] bytes = fileInputStream.readAllBytes();
						fileCarrier.setFile(bytes);
						fileCarrier.setFilePath(file.getName());*/
						byte[] bytes = new byte[1024 * 8];
						int len;
						while((len=fileInputStream.read(bytes))>0){
							dataOutputStream.write(bytes,0,len);
						}
						//从后往前逐个关闭流
						fileInputStream.close();
						dataOutputStream.close();
						/*fileSocket.close();
						serverSocket.close();*/
					}else{
						//文件不存在发送的是空对象
						//response = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
						response.writeObject(fileCarrier);
						response.flush();
					}
				}

				if("upload".equals(carrier.getType())){
					//接收文件名

					File file = new File(System.getProperty("user.dir") + "\\" + carrier.getFilePath());

					FileOutputStream fileOutputStream = new FileOutputStream(file);
					//接收文件
					//DataInputStream dataInputStream = new DataInputStream(fileSocket.getInputStream());
					DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
					//输出到本地文件
					byte[] bytes = new byte[1024 * 8];
					int len;
					while ((len = dataInputStream.read(bytes)) > 0) {
						fileOutputStream.write(bytes, 0, len);
					}
					//fileOutputStream.write(fileCarrier.getFile());
					//关闭流
					dataInputStream.close();
					fileOutputStream.close();
				}

				//发送桌面图像回客户端
				Dimension desktopSize = Toolkit.getDefaultToolkit().getScreenSize();
				BufferedImage curDesktop = controlMouseRobot.createScreenCapture(new Rectangle(desktopSize));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.write(curDesktop, "jpg", out);
				ControlCarrier desktopState = new ControlCarrier();
				desktopState.setDesktopImg(out.toByteArray());

				response.writeObject(desktopState);
				response.flush();

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {

				if (request != null) {
					try {
						request.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (response != null) {
					try {
						response.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

}
