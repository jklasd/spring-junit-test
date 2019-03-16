package bbkj.org.cn;

import javax.swing.JFrame;

import bbkj.org.cn.window.MainWindow;

public class Launch {
	
	public static void main(String[] args) {
		MainWindow win = new MainWindow();
		
		win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		win.setVisible(true);
	}

}
