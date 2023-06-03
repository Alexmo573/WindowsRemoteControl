package nupt.zh.remotecontrol;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * 客户端，控制端
 *
 * @author zh
 *
 */
public class Client extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

	private static final long serialVersionUID = -806686211556049511L;
	private String controlIp;
	private int controlPort;
	private BufferedImage controlDesktop;

	public Client(String controlIp, int controlPort) {
		this.controlIp = controlIp;
		this.controlPort = controlPort;

		setFocusable(true);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addMouseWheelListener(this);
		this.addKeyListener(this);

	}

	public void start() {
		ControlCarrier command = new ControlCarrier();
		sendControlCommand(command);
	}

	@Override
	public void paintComponent(Graphics g) {
		if (controlDesktop != null) {
			g.drawImage(controlDesktop, 0, 0, this);
		}
	}

	//文件下载函数
	public boolean fileControlCommand(String filePath,String action) {
		ControlCarrier command=new ControlCarrier();
		//成功标志
		boolean flag=false;
		//设置上传还是下载
		command.setType(action);

		ObjectOutputStream objectOut = null;
		ObjectInputStream objectInput = null;
		try {
			Socket commandSocket = new Socket(InetAddress.getByName(controlIp), controlPort);
			//Socket fileSocket = new Socket(InetAddress.getByName(controlIp), controlPort+1);
			if("download".equals(action)){
				//发送命令
				objectOut = new ObjectOutputStream(new BufferedOutputStream(commandSocket.getOutputStream()));
				//设置路径
				command.setFilePath(filePath);
				objectOut.writeObject(command);
				objectOut.flush();
				//停止一小会，等对方接收
				Thread.sleep(100);
				System.out.println("发送命令："+command.getFilePath()+command.getType());
				//接收被控制端文件
				objectInput = new ObjectInputStream(new BufferedInputStream(commandSocket.getInputStream()));
				ControlCarrier fileCarrier = (ControlCarrier) objectInput.readObject();
				System.out.println("接收的文件名"+fileCarrier.getFilePath());
				if(fileCarrier.getFilePath()!=null){
					//创建文件
					File file = new File(System.getProperty("user.dir")+"\\"+fileCarrier.getFilePath());
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					//接收文件
					//DataInputStream dataInputStream = new DataInputStream(fileSocket.getInputStream());
					DataInputStream dataInputStream = new DataInputStream(commandSocket.getInputStream());
					//输出到本地文件
					byte[] bytes = new byte[1024 * 8];
					int len;
					while((len=dataInputStream.read(bytes))>0){
						fileOutputStream.write(bytes,0,len);
					}
					//fileOutputStream.write(fileCarrier.getFile());
					//关闭流
					dataInputStream.close();
					fileOutputStream.close();
					/*fileSocket.close();*/
					commandSocket.close();
					flag=true;
				}
			}
			if("upload".equals(action)){
				File file=new File(filePath);
				//文件存在
				if(file.exists()){

					//文件存在，发送命令
					objectOut = new ObjectOutputStream(new BufferedOutputStream(commandSocket.getOutputStream()));
					//传输文件名
					command.setFilePath(file.getName());
					objectOut.writeObject(command);
					objectOut.flush();
					//停止一小会，等对方接收
					Thread.sleep(100);

					//传输文件
					DataOutputStream dataOutputStream = new DataOutputStream(commandSocket.getOutputStream());
					FileInputStream fileInputStream = new FileInputStream(file);
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
					flag=true;
				}

			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			if (objectOut != null) {
				try {
					objectOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (objectInput != null) {
				try {
					objectInput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return flag;
		}
	}

	private void sendControlCommand(ControlCarrier command) {
		ObjectOutputStream objectOut = null;
		ObjectInputStream objectInput = null;
		try {
			Socket commandSocket = new Socket(InetAddress.getByName(controlIp), controlPort);

			//发送命令
			objectOut = new ObjectOutputStream(new BufferedOutputStream(commandSocket.getOutputStream()));
			objectOut.writeObject(command);
			objectOut.flush();

			objectInput = new ObjectInputStream(new BufferedInputStream(commandSocket.getInputStream()));

			//接收被控制端图像
			ControlCarrier desktopState = (ControlCarrier) objectInput.readObject();
			displayDesktopState(desktopState);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (objectOut != null) {
				try {
					objectOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (objectInput != null) {
				try {
					objectInput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void displayDesktopState(ControlCarrier desktopState) {
		ByteArrayInputStream bin = new ByteArrayInputStream(desktopState.getDesktopImg());
		try {
			controlDesktop = ImageIO.read(bin);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "被控制端已断开连接，或网络异常!");
		}
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		ControlCarrier command = new ControlCarrier();
		command.setType("mouseClicked");
		if (e.getClickCount() == 1) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				command.setMousePressBtn(InputEvent.BUTTON1_MASK);
				command.setMouseReleaseBtn(InputEvent.BUTTON1_MASK);
			} else if (e.getButton() == MouseEvent.BUTTON2) {
				command.setMousePressBtn(InputEvent.BUTTON2_MASK);
				command.setMouseReleaseBtn(InputEvent.BUTTON2_MASK);
			} else if (e.getButton() == MouseEvent.BUTTON3) {
				command.setMousePressBtn(InputEvent.BUTTON3_MASK);
				command.setMouseReleaseBtn(InputEvent.BUTTON3_MASK);
			}
		} else if (e.getClickCount() == 2) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				command.setMousePressBtn(InputEvent.BUTTON1_MASK);
				command.setMouseReleaseBtn(InputEvent.BUTTON1_MASK);
				command.setMousePressBtn(InputEvent.BUTTON1_MASK);
				command.setMouseReleaseBtn(InputEvent.BUTTON1_MASK);
			} else if (e.getButton() == MouseEvent.BUTTON2) {
				command.setMousePressBtn(InputEvent.BUTTON2_MASK);
				command.setMouseReleaseBtn(InputEvent.BUTTON2_MASK);
				command.setMousePressBtn(InputEvent.BUTTON2_MASK);
				command.setMouseReleaseBtn(InputEvent.BUTTON2_MASK);
			} else if (e.getButton() == MouseEvent.BUTTON3) {
				command.setMousePressBtn(InputEvent.BUTTON3_MASK);
				command.setMouseReleaseBtn(InputEvent.BUTTON3_MASK);
				command.setMousePressBtn(InputEvent.BUTTON3_MASK);
				command.setMouseReleaseBtn(InputEvent.BUTTON3_MASK);
			}
		}
		sendControlCommand(command);
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {
		ControlCarrier command = new ControlCarrier();
		command.setType("mousePressed");
		if (e.getButton() == MouseEvent.BUTTON1) {
			command.setMousePressBtn(InputEvent.BUTTON1_MASK);
		} else if (e.getButton() == MouseEvent.BUTTON2) {
			command.setMousePressBtn(InputEvent.BUTTON2_MASK);
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			command.setMousePressBtn(InputEvent.BUTTON3_MASK);
		}
		sendControlCommand(command);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		ControlCarrier command = new ControlCarrier();
		command.setType("mouseReleased");
		if (e.getButton() == MouseEvent.BUTTON1) {
			command.setMouseReleaseBtn(InputEvent.BUTTON1_MASK);
		} else if (e.getButton() == MouseEvent.BUTTON2) {
			command.setMouseReleaseBtn(InputEvent.BUTTON2_MASK);
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			command.setMouseReleaseBtn(InputEvent.BUTTON3_MASK);
		}
		sendControlCommand(command);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		ControlCarrier command = new ControlCarrier();
		command.setType("mouseDragged");
		command.setMousePressBtn(e.getButton());
		command.setMouseX(e.getX());
		command.setMouseY(e.getY());
		sendControlCommand(command);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		ControlCarrier command = new ControlCarrier();
		command.setType("mouseMoved");
		command.setMouseX(e.getX());
		command.setMouseY(e.getY());
		sendControlCommand(command);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		ControlCarrier command = new ControlCarrier();
		command.setType("mouseWheelMoved");
		command.setWheelAmt(e.getWheelRotation());
		sendControlCommand(command);
	}

	@Override
	public void keyPressed(KeyEvent e) {

		ControlCarrier command = new ControlCarrier();
		command.setType("keyPressed");
		List<Integer> keyPress = new ArrayList<Integer>();
		if (e.isControlDown()) {
			keyPress.add(KeyEvent.VK_CONTROL);
		}
		if (e.isAltDown()) {
			keyPress.add(KeyEvent.VK_ALT);
		}
		if (e.isShiftDown()) {
			keyPress.add(KeyEvent.VK_SHIFT);
		}
		keyPress.add(e.getKeyCode());
		command.setKeyPressCode(keyPress);
		sendControlCommand(command);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		ControlCarrier command = new ControlCarrier();
		command.setType("keyReleased");
		List<Integer> keyRelease = new ArrayList<Integer>();
		keyRelease.add(e.getKeyCode());
		command.setKeyReleaseCode(keyRelease);
		sendControlCommand(command);
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(getWidth(), getHeight());
	}

}
