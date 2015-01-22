package jboot.loader.bootstrapper.splash;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;

import java.io.File;

import java.net.URL;

import java.util.StringTokenizer;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.plaf.basic.BasicProgressBarUI;


public class SplashScreen extends JWindow implements ISplashScreen {
    private static final long serialVersionUID = 6837399669639205928L;
    private static final int TEXT_MARGIN_WIDTH = 20;
    private ImageIcon imageIcon;
    private JLabel imageLabel;
    private JPanel southPanel;
    private FlowLayout southPanelFlowLayout;
    private JProgressBar progressBar;
    private BorderLayout borderLayout;
    private Font font;
    private boolean imageFound;
    private boolean splashRemoved;


    public SplashScreen(String path) {
        File file = new File(path);
        if (file.exists()) {
            imageIcon = new ImageIcon(file.getAbsolutePath());
            imageFound = true;
        } else {
            URL url = this.getClass().getResource(path);
            if (url != null) {
                imageIcon = new ImageIcon(url);
                imageFound = true;
            }
        }
        initSplashScreen();
    }


    @Override
    public void showSplashScreen() {
        if (imageFound && !splashRemoved) {
            setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
            imageLabel.setIcon(imageIcon);
            progressBar.setForeground(new Color(191, 4, 79, 215));
            progressBar.setBorderPainted(false);
            progressBar.setUI(new BasicProgressBarUI() {
                    @Override
                    protected Color getSelectionBackground() {
                        return Color.black;
                    }

                    @Override
                    protected Color getSelectionForeground() {
                        return Color.black;
                    }
                });
            font = new Font(null, Font.PLAIN, 12);
            progressBar.setFont(font);
            progressBar.setPreferredSize(new Dimension(imageIcon.getIconWidth(), 25));

            southPanel.setPreferredSize(progressBar.getPreferredSize());
            southPanel.setMaximumSize(new Dimension(progressBar.getPreferredSize().width + 10, progressBar.getPreferredSize().height));
            southPanel.setBackground(new Color(180, 180, 180, 125));
            southPanel.add(progressBar);
            southPanel.setLayout(southPanelFlowLayout);

            this.getContentPane().setLayout(borderLayout);
            this.getContentPane().add(imageLabel, BorderLayout.CENTER);
            this.getContentPane().add(southPanel, BorderLayout.SOUTH);
            splashRemoved = false;

            com.sun.awt.AWTUtilities.setWindowOpaque(this, false);
            setLocationRelativeTo(null);
            this.pack();
            this.setVisible(true);
        }
    }

    @Override
    public void removeSplashScreen() {
        this.dispose();
        imageIcon = null;
        imageLabel = null;
        southPanel = null;
        southPanelFlowLayout = null;
        progressBar = null;
        borderLayout = null;
        font = null;
        splashRemoved = true;
        /*
         * The below is a workaround in order to maintain the awt event dispatch thread as the boot-class loader since the original 
         * eventQueue caches the CCL at its construction time. This can only effectively be called after setting the context class loader in jboot.loader.bootstrapper.DefaultBootstrapLoader.initClassLoader(List<ModelInfo>, UpgradePolicy)
         * Reference: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4786277
         */
        Toolkit tk = Toolkit.getDefaultToolkit(); 
        EventQueue eq = tk.getSystemEventQueue();
        eq.push(new EventQueue());
    }

    @Override
    public void setMaximumProgress(int maxValue) {
        if (imageFound && !splashRemoved) {
            progressBar.setIndeterminate(false);
            progressBar.setMaximum(maxValue);
            progressBar.setValue(0);
        }
    }

    @Override
    public void incrementProgress(int progress) {
        if (imageFound && !splashRemoved) {
            progressBar.setIndeterminate(false);
            progressBar.setValue(progressBar.getValue() + progress);
        }
    }

    @Override
    public void setMessage(String message) {
        if (imageFound && !splashRemoved) {
            progressBar.setString(trimMessage(message));
            progressBar.setStringPainted(true);
        }
    }

    @Override
    public void clearMessage() {
        if (imageFound && !splashRemoved) {
            progressBar.setString("");
            progressBar.setStringPainted(false);
        }
    }

    private String trimMessage(String message) {
        FontMetrics fontMetric = progressBar.getFontMetrics(font);
        int stringWidth = fontMetric.stringWidth(message);
        while (stringWidth > (imageIcon.getIconWidth() - (TEXT_MARGIN_WIDTH * 2))) {
            StringTokenizer tokenizer = new StringTokenizer(message);
            String lastToken = "";
            while (tokenizer.hasMoreTokens()) {
                lastToken = tokenizer.nextToken();
            }
            message = message.substring(0, message.length() - lastToken.length() - 1).concat("...");
            stringWidth = fontMetric.stringWidth(message);
        }
        return message;
    }

    private void initSplashScreen() {
        imageLabel = new JLabel();
        southPanel = new JPanel();
        southPanelFlowLayout = new FlowLayout();
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        borderLayout = new BorderLayout();
    }
}
