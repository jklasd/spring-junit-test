package bbkj.org.cn.window;

import java.awt.Component;

import javax.swing.JPanel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainPanel extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4304137074748483104L;

	
	
	@Override
	public Component add(Component comp) {
		log.info("height:{}",this.getSize().getHeight());
		return super.add(comp);
	}
}
