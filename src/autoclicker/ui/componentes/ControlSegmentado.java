package autoclicker.ui.componentes;

import autoclicker.ui.tema.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Grupo de opciones donde solo una puede estar seleccionada, mostrado
 * como una barra dividida en segmentos. Reemplaza al JComboBox y al JCheckBox.
 * Se puede usar con el mouse o con las flechas izquierda/derecha.
 *
 * @param <T> tipo de valor que representa cada opción
 */
public class ControlSegmentado<T> extends JComponent {

    private static final int RELLENO = 4;

    private final T[] valores;
    private final String[] etiquetas;
    private int seleccionado = 0;
    private int sobre = -1;
    private final List<Consumer<T>> oyentes = new CopyOnWriteArrayList<>();

    /** Usa el toString() de cada valor como texto del segmento. */
    public ControlSegmentado(T[] valores) {
        this(valores, textos(valores));
    }

    public ControlSegmentado(T[] valores, String[] etiquetas) {
        if (valores.length == 0 || valores.length != etiquetas.length) {
            throw new IllegalArgumentException("Debe haber un texto por cada valor.");
        }
        this.valores = valores;
        this.etiquetas = etiquetas;

        setFont(Tema.fuente(Font.BOLD, 13f));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter raton = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                requestFocusInWindow();
                seleccionar(indiceEn(e.getX()));
            }
            @Override public void mouseMoved(MouseEvent e) {
                int nuevo = indiceEn(e.getX());
                if (nuevo != sobre) { sobre = nuevo; repaint(); }
            }
            @Override public void mouseExited(MouseEvent e) {
                sobre = -1;
                repaint();
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (!isEnabled()) return;
                if (e.getKeyCode() == KeyEvent.VK_LEFT)  seleccionar(seleccionado - 1);
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) seleccionar(seleccionado + 1);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e)   { repaint(); }
        });
    }

    public T getSeleccion() {
        return valores[seleccionado];
    }

    public void setSeleccion(T valor) {
        for (int i = 0; i < valores.length; i++) {
            if (valores[i].equals(valor)) { seleccionar(i); return; }
        }
    }

    /** Se ejecuta cada vez que el usuario (o el código) cambia la selección. */
    public void alCambiar(Consumer<T> accion) {
        oyentes.add(accion);
    }

    @Override
    public void setEnabled(boolean habilitado) {
        super.setEnabled(habilitado);
        setCursor(Cursor.getPredefinedCursor(habilitado ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    // ---- Lógica interna ----

    private void seleccionar(int indice) {
        if (indice < 0 || indice >= valores.length || indice == seleccionado) return;
        seleccionado = indice;
        repaint();
        oyentes.forEach(o -> o.accept(valores[seleccionado]));
    }

    private int indiceEn(int x) {
        float ancho = (getWidth() - 2f * RELLENO) / valores.length;
        int i = (int) ((x - RELLENO) / ancho);
        return Math.max(0, Math.min(valores.length - 1, i));
    }

    private static String[] textos(Object[] valores) {
        String[] t = new String[valores.length];
        for (int i = 0; i < valores.length; i++) t[i] = String.valueOf(valores[i]);
        return t;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int maxTexto = 0;
        for (String e : etiquetas) maxTexto = Math.max(maxTexto, fm.stringWidth(e));
        return new Dimension(valores.length * (maxTexto + 32) + 2 * RELLENO, 40);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        if (!isEnabled()) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));

        float w = getWidth();
        float h = getHeight();

        RoundRectangle2D fondo = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Tema.RADIO_CAMPO, Tema.RADIO_CAMPO);
        g2.setColor(Tema.paleta().campo());
        g2.fill(fondo);
        g2.setColor(isFocusOwner() ? Tema.paleta().acento() : Tema.paleta().borde());
        g2.draw(fondo);

        float ancho = (w - 2 * RELLENO) / valores.length;
        FontMetrics fm = g2.getFontMetrics(getFont());
        g2.setFont(getFont());

        for (int i = 0; i < valores.length; i++) {
            float x = RELLENO + i * ancho;
            boolean activo = i == seleccionado;

            if (activo) {
                g2.setColor(isEnabled() ? Tema.paleta().acento() : Tema.paleta().textoDeshabilitado());
                g2.fill(new RoundRectangle2D.Float(x, RELLENO, ancho, h - 2 * RELLENO,
                        Tema.RADIO_CAMPO - 3, Tema.RADIO_CAMPO - 3));
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(i == sobre && isEnabled() ? Tema.paleta().texto() : Tema.paleta().textoSuave());
            }

            String texto = etiquetas[i];
            int tx = Math.round(x + (ancho - fm.stringWidth(texto)) / 2f);
            int ty = Math.round((h - fm.getHeight()) / 2f) + fm.getAscent();
            g2.drawString(texto, tx, ty);
        }
        g2.dispose();
    }
}
