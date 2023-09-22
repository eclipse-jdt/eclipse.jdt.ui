package p; // 7, 26, 7, 50

import javax.swing.*;

public class A {
    public static void main(String [] args) {
        JPanel serverPanel= Util.createServerPanel();
		Util.configPanel(serverPanel);
    }
}
class Util{
    public static JPanel createServerPanel() {
        return new JPanel();
    }
    public static void configPanel(JPanel panel) {
        System.out.println("config");
    }
}