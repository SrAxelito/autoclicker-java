package autoclicker.ui.componentes;

import autoclicker.ui.tema.Paleta;
import autoclicker.ui.tema.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Muestra el atajo actual como una tecla. Al hacer clic entra en modo
 * captura y espera a que el usuario presione la tecla nueva.
 */
public class SelectorAtajo extends JComponent {

    private static final int DURACION_AVISO_MS = 2000;

    private String nombre = "";
    private String aviso = null;
    private boolean capturando = false;
    private boolean disponible = true;
    private Runnable alActivar = () -> { };
    private final Timer quitarAviso = new Timer(DURACION_AVISO_MS, e -> {
        aviso = null;
        repaint();
    });

    public SelectorAtajo() {
        setFont(Tema.fuente(Font.BOLD, 13f));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        quitarAviso.setRepeats(false);

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!puedeUsarse()) return;
                requestFocusInWindow();
                alActivar.run();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                boolean confirmar = e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE;
                if (confirmar && puedeUsarse() && !capturando) alActivar.run();
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e)   { repaint(); }
        });
    }

    public void setAlActivar(Runnable accion) {
        this.alActivar = (accion != null) ? accion : () -> { };
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
        repaint();
    }

    public void setCapturando(boolean capturando) {
        this.capturando = capturando;
        if (capturando) aviso = null;
        repaint();
    }

    public boolean estaCapturando() {
        return capturando;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
        setCursor(Cursor.getPredefinedCursor(disponible ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    /** Muestra un mensaje corto en rojo durante un par de segundos. */
    public void mostrarAviso(String mensaje) {
        aviso = mensaje;
        quitarAviso.restart();
        repaint();
    }

    @Override
    public void setEnabled(boolean habilitado) {
        super.setEnabled(habilitado);
        repaint();
    }

    private boolean puedeUsarse() {
        return isEnabled() && disponible;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(160, 44);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        Paleta p = Tema.paleta();
        float w = getWidth();
        float h = getHeight();
        boolean activo = puedeUsarse();

        RoundRectangle2D fondo = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Tema.RADIO_CAMPO, Tema.RADIO_CAMPO);
        g2.setColor(activo ? p.campo() : p.deshabilitado());
        g2.fill(fondo);
        boolean resaltado = capturando || (isFocusOwner() && activo);
        g2.setColor(aviso != null ? p.peligro() : resaltado ? p.acento() : p.borde());
        g2.setStroke(new BasicStroke(resaltado || aviso != null ? 1.6f : 1f));
        g2.draw(fondo);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int base = Math.round((h - fm.getHeight()) / 2f) + fm.getAscent();
        float disponibleAncho = w - 28;

        if (!disponible) {
            g2.setColor(p.textoDeshabilitado());
            g2.drawString(ajustar("No disponible en este sistema", "No disponible", fm, disponibleAncho), 14, base);
        } else if (aviso != null) {
            g2.setColor(p.peligro());
            g2.drawString(ajustar(aviso, "En uso", fm, disponibleAncho), 14, base);
        } else if (capturando) {
            FontMetrics fmAyuda = g2.getFontMetrics(Tema.fuente(Font.PLAIN, 12f));
            int anchoAyuda = fmAyuda.stringWidth("Esc cancela");
            boolean conAyuda = fm.stringWidth("Presiona una tecla…") + anchoAyuda + 20 <= disponibleAncho;
            float espacio = conAyuda ? disponibleAncho - anchoAyuda - 20 : disponibleAncho;
            g2.setColor(p.acento());
            g2.drawString(ajustar("Presiona una tecla o botón lateral…", "Presiona una tecla…", fm, espacio), 14, base);
            if (conAyuda) dibujarAyuda(g2, "Esc cancela", w, base, p);
        } else {
            float finTecla = dibujarTecla(g2, fm, h, activo, p);
            FontMetrics fmAyuda = g2.getFontMetrics(Tema.fuente(Font.PLAIN, 12f));
            if (activo && finTecla + 16 + fmAyuda.stringWidth("Clic para cambiar") + 14 <= w) {
                dibujarAyuda(g2, "Clic para cambiar", w, base, p);
            }
        }
        g2.dispose();
    }

    /** Dibuja el nombre del atajo como una tecla y devuelve dónde termina. */
    private float dibujarTecla(Graphics2D g2, FontMetrics fm, float h, boolean activo, Paleta p) {
        float anchoTecla = Math.min(fm.stringWidth(nombre) + 22f, getWidth() - 16f);
        float altoTecla = 28f;
        float x = 8f;
        float y = (h - altoTecla) / 2f - 1f;
        g2.setColor(activo ? Tema.mezclar(p.borde(), p.texto(), 0.12f) : p.borde());
        g2.fill(new RoundRectangle2D.Float(x, y + 2, anchoTecla, altoTecla, 8, 8));
        g2.setColor(activo ? p.tarjeta() : p.deshabilitado());
        g2.fill(new RoundRectangle2D.Float(x, y, anchoTecla, altoTecla, 8, 8));
        g2.setColor(p.borde());
        g2.draw(new RoundRectangle2D.Float(x, y, anchoTecla, altoTecla, 8, 8));

        g2.setColor(activo ? p.texto() : p.textoDeshabilitado());
        String texto = ajustar(nombre, nombre, fm, anchoTecla - 22);
        g2.drawString(texto, Math.round(x + 11), Math.round(y + (altoTecla - fm.getHeight()) / 2f) + fm.getAscent());
        return x + anchoTecla;
    }

    private void dibujarAyuda(Graphics2D g2, String texto, float ancho, int base, Paleta p) {
        g2.setFont(Tema.fuente(Font.PLAIN, 12f));
        g2.setColor(p.textoSuave());
        int tw = g2.getFontMetrics().stringWidth(texto);
        g2.drawString(texto, Math.round(ancho - tw - 14), base);
        g2.setFont(getFont());
    }

    /** Usa el texto largo si cabe, si no el corto, y si tampoco cabe lo recorta con "…". */
    private static String ajustar(String largo, String corto, FontMetrics fm, float ancho) {
        if (fm.stringWidth(largo) <= ancho) return largo;
        if (fm.stringWidth(corto) <= ancho) return corto;
        String t = corto;
        while (t.length() > 1 && fm.stringWidth(t + "…") > ancho) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}
