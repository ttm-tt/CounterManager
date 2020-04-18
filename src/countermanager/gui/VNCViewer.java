/* Copyright (C) 2020 Christoph Theis */

package countermanager.gui;

import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import static com.shinyhut.vernacular.client.rendering.ColorDepth.BPP_16_TRUE;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;

import static com.shinyhut.vernacular.client.rendering.ColorDepth.BPP_8_INDEXED;
import static java.awt.BorderLayout.CENTER;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.datatransfer.DataFlavor.stringFlavor;
import static java.awt.event.KeyEvent.*;
import static java.lang.Integer.parseInt;
import static java.lang.Thread.sleep;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import static javax.swing.JFrame.DISPOSE_ON_CLOSE;
import static javax.swing.JOptionPane.*;

public class VNCViewer extends JFrame {

    private VernacularConfig config;
    private VernacularClient client;

    private JMenuItem connectMenuItem;
    private JMenuItem disconnectMenuItem;

    private JMenuItem bpp8IndexedColorMenuItem;
    private JMenuItem bpp16TrueColorMenuItem;
    
    private JMenuItem scaleMenuItem;

    private Image lastFrame;
    
    private JScrollPane scrollPane;
    private JPanel    clientPanel;
    
    private int scale = 1;
    private boolean bpp16 = true;

    private AncestorListener focusRequester = new AncestorListener() {
        @Override
        public void ancestorAdded(AncestorEvent event) {
            event.getComponent().requestFocusInWindow();
        }

        @Override
        public void ancestorRemoved(AncestorEvent event) {
        }

        @Override
        public void ancestorMoved(AncestorEvent event) {
        }
    };

    private volatile boolean shutdown = false;

    private Thread clipboardMonitor = new Thread(() -> {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        String lastText = null;
        while (!shutdown) {
            try {
                if (connected()) {
                    String text = (String) clipboard.getData(stringFlavor);
                    if (text != null && !text.equals(lastText)) {
                        client.copyText(text);
                        lastText = text;
                    }
                }
                sleep(100L);
            } catch (Exception ignored) {
            }
        }
    });

    public VNCViewer() {
        initUI();
    }

    private void initUI() {
        final java.util.prefs.Preferences prefs = Preferences.userRoot().node("/de/countermanager/vncviewer");
        int x = prefs.getInt("left", 0);
        int y = prefs.getInt("top", 0);
        int w = prefs.getInt("width", 800);
        int h = prefs.getInt("height", 600);
        
        scale = prefs.getBoolean("scale", false) ? 2 : 1;
        bpp16 = prefs.getBoolean("bpp16", true);
        
        setTitle("Vernacular VNC");
        setSize(w, h);
        setLocation(x, y);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                prefs.putInt("left", getX());
                prefs.putInt("top", getY());
                prefs.putInt("width", getWidth());
                prefs.putInt("height", getHeight());
                
                prefs.putBoolean("scale", scale != 1);
                prefs.putBoolean("bpp16", bpp16);
                
                try {
                    prefs.flush();
                } catch (BackingStoreException ex) {

                }
                
                disconnect();
                shutdown = true;
                try {
                    clipboardMonitor.join();
                } catch (InterruptedException ignored) {
                }
                super.windowClosing(event);
            }
        });

        addMenu();
        addDrawingSurface();
        addMouseListeners();
        addKeyListener();
        initialiseVernacularClient();
        clipboardMonitor.start();
    }

    private void addKeyListener() {
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (connected()) {
                    client.handleKeyEvent(e);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (connected()) {
                    client.handleKeyEvent(e);
                }
            }
        });
    }

    private void addMouseListeners() {
        clientPanel.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (connected()) {
                    client.moveMouse(scaleMouseX(e.getX()), scaleMouseY(e.getY()));
                }
            }
        });
        clientPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (connected()) {
                    client.updateMouseButton(e.getButton() - 1, true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (connected()) {
                    client.updateMouseButton(e.getButton() - 1, false);
                }
            }
        });
    }

    private void addDrawingSurface() {
        clientPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (lastFrame != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(lastFrame, 0, 0, lastFrame.getWidth(null) / scale, lastFrame.getHeight(null) / scale, null);
                }
            }
        };
        
        scrollPane = new JScrollPane(clientPanel);        
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        add(scrollPane, CENTER);
    }

    private void initialiseVernacularClient() {
        config = new VernacularConfig();
        config.setColorDepth(bpp16 ? BPP_16_TRUE : BPP_8_INDEXED);
        config.setErrorListener(e -> {
            showMessageDialog(this, e.getMessage(), "Error", ERROR_MESSAGE);
            setMenuState(false);
        });
        config.setPasswordSupplier(this::showPasswordDialog);
        config.setScreenUpdateListener(this::renderFrame);
        config.setBellListener(v -> getDefaultToolkit().beep());
        config.setRemoteClipboardListener(t -> getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(t), null));
        client = new VernacularClient(config);
    }

    private void addMenu() {
        JMenuBar menu = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(VK_F);

        JMenu options = new JMenu("Options");
        options.setMnemonic(VK_O);

        connectMenuItem = new JMenuItem("Connect");
        connectMenuItem.setMnemonic(VK_C);
        connectMenuItem.addActionListener((ActionEvent event) -> showConnectDialog());

        disconnectMenuItem = new JMenuItem("Disconnect");
        disconnectMenuItem.setMnemonic(VK_D);
        disconnectMenuItem.setEnabled(false);
        disconnectMenuItem.addActionListener((ActionEvent event) -> disconnect());

        ButtonGroup colorDepths = new ButtonGroup();

        bpp8IndexedColorMenuItem = new JRadioButtonMenuItem("8-bit Indexed Color", !bpp16);
        bpp16TrueColorMenuItem = new JRadioButtonMenuItem("16-bit True Color", bpp16);
        colorDepths.add(bpp8IndexedColorMenuItem);
        colorDepths.add(bpp16TrueColorMenuItem);

        bpp8IndexedColorMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                bpp16 = false; config.setColorDepth(BPP_8_INDEXED);
            }
        });
        bpp16TrueColorMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                bpp16 = true; config.setColorDepth(BPP_16_TRUE);
            }
        });
        
        scaleMenuItem = new JCheckBoxMenuItem("Scale", scale != 1);
        scaleMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                scale = (scale == 1 ? 2 : 1); clientPanel.repaint();
            }
        });        

        JMenuItem exit = new JMenuItem("Exit");
        exit.setMnemonic(VK_X);
        exit.addActionListener((ActionEvent event) -> {
            disconnect();
            setVisible(false);
            dispose();
        });

        file.add(connectMenuItem);
        file.add(disconnectMenuItem);
        file.add(exit);
        options.add(bpp8IndexedColorMenuItem);
        options.add(bpp16TrueColorMenuItem);
        options.add(scaleMenuItem);
        menu.add(file);
        menu.add(options);
        setJMenuBar(menu);
    }

    private void showConnectDialog() {
        JPanel connectDialog = new JPanel();
        JTextField hostField = new JTextField(20);
        hostField.addAncestorListener(focusRequester);
        JTextField portField = new JTextField("5900");
        JLabel hostLabel = new JLabel("Host");
        hostLabel.setLabelFor(hostField);
        JLabel portLabel = new JLabel("Port");
        portLabel.setLabelFor(hostLabel);
        connectDialog.add(hostLabel);
        connectDialog.add(hostField);
        connectDialog.add(portLabel);
        connectDialog.add(portField);
        int choice = showConfirmDialog(this, connectDialog, "Connect", OK_CANCEL_OPTION);
        if (choice == OK_OPTION) {
            String host = hostField.getText();
            if (host == null || host.isEmpty()) {
                showMessageDialog(this, "Please enter a valid host", null, WARNING_MESSAGE);
                return;
            }
            int port;
            try {
                port = parseInt(portField.getText());
            } catch (NumberFormatException e) {
                showMessageDialog(this, "Please enter a valid port", null, WARNING_MESSAGE);
                return;
            }
            connect(host, port);
        }
    }

    private String showPasswordDialog() {
        String password = "";
        JPanel passwordDialog = new JPanel();
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.addAncestorListener(focusRequester);
        passwordDialog.add(passwordField);
        int choice = showConfirmDialog(this, passwordDialog, "Enter Password", OK_CANCEL_OPTION);
        if (choice == OK_OPTION) {
            password = new String(passwordField.getPassword());
        }
        return password;
    }

    public void connect(String host, int port) {
        setMenuState(true);
        lastFrame = null;
        client.start(host, port);
    }

    private void disconnect() {
        if (connected()) {
            client.stop();
        }
        setMenuState(false);
    }

    private void setMenuState(boolean running) {
        if (running) {
            connectMenuItem.setEnabled(false);
            disconnectMenuItem.setEnabled(true);
            bpp8IndexedColorMenuItem.setEnabled(false);
            bpp16TrueColorMenuItem.setEnabled(false);
        } else {
            connectMenuItem.setEnabled(true);
            disconnectMenuItem.setEnabled(false);
            bpp8IndexedColorMenuItem.setEnabled(true);
            bpp16TrueColorMenuItem.setEnabled(true);
        }
    }

    private boolean connected() {
        return client != null && client.isRunning();
    }

    private void renderFrame(Image frame) {
        if (resizeRequired(frame)) {
            resizeWindow(frame);
        }
        lastFrame = frame;
        repaint();
    }

    private boolean resizeRequired(Image frame) {
        if (frame == null)
            return false;
        
        int iw = frame.getWidth(null);
        int ih = frame.getHeight(null);
        int cw = clientPanel.getPreferredSize().width * scale;
        int ch = clientPanel.getPreferredSize().height * scale;
        
        return iw != cw || ih != ch;
    }

    private void resizeWindow(Image frame) {
        int remoteWidth = frame.getWidth(null);
        int remoteHeight = frame.getHeight(null);
        
        setWindowSize(remoteWidth, remoteHeight);
    }

    private void setWindowSize(int width, int height) {
        clientPanel.setPreferredSize(new Dimension(width / scale, height / scale));
        clientPanel.revalidate();
    }
    
    private int scaleMouseX(int x) {
        if (lastFrame == null) {
            return x;
        }
        return (int) (x * ((double) lastFrame.getWidth(null) / clientPanel.getPreferredSize().width));
    }

    private int scaleMouseY(int y) {
        if (lastFrame == null) {
            return y;
        }
        return (int) (y * ((double) lastFrame.getHeight(null) / clientPanel.getPreferredSize().height));
    }

    
}