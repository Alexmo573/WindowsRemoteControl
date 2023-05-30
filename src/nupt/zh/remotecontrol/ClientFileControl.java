package nupt.zh.remotecontrol;

import javax.swing.*;
import java.awt.*;

//C:\Users\28131\Desktop\control\hello.txt
//192.168.127.129

public class ClientFileControl extends JPanel implements Runnable{
    Client client;
    JLabel label;

    Thread thread;
    ClientFileControl(Client client){
        this.client=client;
        this.thread=new Thread(this);

        // 设置JPanel的布局为网格布局，2行2列
        setLayout(new GridLayout(3, 1));

        // 添加姓名标签和文本框
        JLabel pathLable = new JLabel("文件路径：");
        add(pathLable);
        JTextField fileField = new JTextField();
        add(fileField);

        // 添加提交按钮
        JButton button = new JButton("下载");
        button.addActionListener(e -> {
            // 获取路径的值
            String path = fileField.getText();
            System.out.println(path);
            if(!StringUtil.isValidPath(path)){
                JOptionPane.showMessageDialog(ClientFileControl.this, "文件路径错误，请重新输入。");
                pathLable.requestFocus();
                fileField.selectAll();
                return;
            }
            try {
                Thread.sleep(600);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            // 在这里执行提交操作
            if(client.fileControlCommand(path)){
                JOptionPane.showMessageDialog(ClientFileControl.this, "下载成功");
                pathLable.requestFocus();
                fileField.selectAll();
                return;
            }else{
                JOptionPane.showMessageDialog(ClientFileControl.this, "下载失败");
                pathLable.requestFocus();
                fileField.selectAll();
                return;
            }
        });
        add(button);

    }

    public void start(){
        thread.start();
    }
    public void run(){

            JFrame frame = new JFrame("MyPanel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 200);
            frame.add(this);
            frame.setVisible(true);

    }
}
