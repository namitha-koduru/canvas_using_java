import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;

public class DrawingApp {

    public static void main(String[] args) {
        // Launch GUI safely on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🎨 Interactive Drawing Canvas");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);

            DrawingPanel canvas = new DrawingPanel(900, 600);
            frame.add(canvas, BorderLayout.CENTER);
            frame.add(createToolBar(canvas), BorderLayout.WEST);

            // Add keyboard shortcuts
            setupKeyBindings(frame.getRootPane(), canvas);

            frame.setVisible(true);
        });
    }

    /** Toolbar Creation */
    private static JToolBar createToolBar(DrawingPanel canvas) {
        JToolBar bar = new JToolBar(JToolBar.VERTICAL);
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Color Picker
        JButton colorBtn = new JButton("🎨 Color");
        colorBtn.addActionListener(e -> canvas.chooseColor());
        bar.add(colorBtn);
        bar.addSeparator(new Dimension(0, 10));

        // Eraser Toggle
        JToggleButton eraserBtn = new JToggleButton("🧽 Eraser");
        eraserBtn.addItemListener(e -> canvas.setEraserMode(e.getStateChange() == ItemEvent.SELECTED));
        bar.add(eraserBtn);
        bar.addSeparator(new Dimension(0, 10));

        // Brush Size
        JLabel sizeLabel = new JLabel("🖌 Brush Size");
        sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.add(sizeLabel);

        JSlider brushSlider = new JSlider(1, 50, canvas.getBrushSize());
        brushSlider.setMajorTickSpacing(10);
        brushSlider.setMinorTickSpacing(1);
        brushSlider.setPaintTicks(true);
        brushSlider.setPaintLabels(true);
        brushSlider.addChangeListener((ChangeEvent e) -> canvas.setBrushSize(brushSlider.getValue()));
        bar.add(brushSlider);
        bar.addSeparator(new Dimension(0, 10));

        // Clear Canvas
        JButton clearBtn = new JButton("🧼 Clear");
        clearBtn.addActionListener(e -> canvas.clearCanvas());
        bar.add(clearBtn);
        bar.addSeparator(new Dimension(0, 10));

        // Save Canvas
        JButton saveBtn = new JButton("💾 Save PNG");
        saveBtn.addActionListener(e -> canvas.saveCanvas());
        bar.add(saveBtn);
        bar.addSeparator(new Dimension(0, 10));

        // Info Label
        bar.add(Box.createVerticalGlue());
        JLabel info = new JLabel(
                "<html><center>Shortcuts:<br>Ctrl + C = Clear<br>Ctrl + E = Eraser<br>Ctrl + S = Save</center></html>");
        info.setHorizontalAlignment(SwingConstants.CENTER);
        bar.add(info);

        return bar;
    }

    /** Keyboard Shortcuts Setup */
    private static void setupKeyBindings(JRootPane root, DrawingPanel canvas) {
        InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = root.getActionMap();

        // Ctrl + C (Clear)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "clearCanvas");
        actionMap.put("clearCanvas", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.clearCanvas();
            }
        });

        // Ctrl + E (Toggle Eraser)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "toggleEraser");
        actionMap.put("toggleEraser", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.toggleEraser();
            }
        });

        // Ctrl + S (Save Image)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "saveImage");
        actionMap.put("saveImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.saveCanvas();
            }
        });
    }
}

/**
 * ------------------------------
 * Drawing Panel Implementation
 * ------------------------------
 */
class DrawingPanel extends JPanel {

    private BufferedImage canvasImage;
    private Graphics2D g2;
    private Color brushColor = Color.BLACK;
    private final Color backgroundColor = Color.WHITE;
    private boolean eraserMode = false;
    private int brushSize = 6;
    private Point lastPoint = null;

    /** Constructor */
    public DrawingPanel(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setBackground(backgroundColor);
        initCanvas(width, height);

        // Mouse Listener for Drawing
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                lastPoint = e.getPoint();
                drawPoint(lastPoint);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point current = e.getPoint();
                drawLine(lastPoint, current);
                lastPoint = current;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastPoint = null;
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    /** Initialize the Canvas */
    private void initCanvas(int w, int h) {
        canvasImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g2 = canvasImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        clearCanvas();
    }

    /** Draw a single point (for clicks) */
    private void drawPoint(Point p) {
        if (p == null)
            return;
        g2.setColor(eraserMode ? backgroundColor : brushColor);
        g2.fillOval(p.x - brushSize / 2, p.y - brushSize / 2, brushSize, brushSize);
        repaint();
    }

    /** Draw line between two points */
    private void drawLine(Point p1, Point p2) {
        if (p1 == null || p2 == null)
            return;
        g2.setColor(eraserMode ? backgroundColor : brushColor);
        g2.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        repaint();
    }

    /** Color Picker */
    public void chooseColor() {
        Color chosen = JColorChooser.showDialog(this, "Select Brush Color", brushColor);
        if (chosen != null) {
            brushColor = chosen;
            eraserMode = false;
        }
    }

    /** Clear Canvas */
    public void clearCanvas() {
        SwingUtilities.invokeLater(() -> {
            g2.setColor(backgroundColor);
            g2.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
            g2.setColor(brushColor);
            repaint();
        });
    }

    /** Save Canvas as PNG */
    public void saveCanvas() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Canvas as PNG");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            try {
                ImageIO.write(canvasImage, "png", file);
                JOptionPane.showMessageDialog(this, "Image saved to:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving file:\n" + ex.getMessage());
            }
        }
    }

    /** Toggle Eraser Mode */
    public void toggleEraser() {
        eraserMode = !eraserMode;
    }

    public void setEraserMode(boolean state) {
        eraserMode = state;
    }

    public void setBrushSize(int size) {
        brushSize = size;
    }

    public int getBrushSize() {
        return brushSize;
    }

    /** Paint Method (Repaint UI) */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvasImage, 0, 0, null);
    }
}
