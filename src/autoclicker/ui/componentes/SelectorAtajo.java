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

    private String nombre = "";
    private boolean capturando = false;
    private boolean disponible = true;
    private Runnable alActivar = () -> { };

    public SelectorAtajo() {
        setFont(Tema.fuente(Font.BOLD, 13f));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
        repaint();
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
        setCursor(Cursor.getPredefinedCursor(disponible ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
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
        return new Dimension(260, 44);
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
        g2.setColor(resaltado ? p.acento() : p.borde());
        g2.setStroke(new BasicStroke(resaltado ? 1.6f : 1f));
        g2.draw(fondo);

        FontMetrics fm = g2.getFontMetrics(getFont());
        g2.setFont(getFont());
        int baseTexto = Math.round((h - fm.getHeight()) / 2f) + fm.getAscent();

        if (!disponible) {
            g2.setColor(p.textoDeshabilitado());
            g2.drawString("No disponible en este sistema", 14, baseTexto);
            g2.dispose();
            return;
        }

        if (capturando) {
            g2.setColor(p.acento());
            g2.drawString("Presiona una tecla o botón lateral…", 14, baseTexto);
            dibujarAyuda(g2, "Esc cancela", w, baseTexto, p);
            g2.dispose();
            return;
        }

        // Tecla con aspecto de "keycap"
        float anchoTecla = fm.stringWidth(nombre) + 22f;
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
        g2.drawString(nombre, Math.round(x + 11), Math.round(y + (altoTecla - fm.getHeight()) / 2f) + fm.getAscent());

        if (activo) dibujarAyuda(g2, "Clic para cambiar", w, baseTexto, p);
        g2.dispose();
    }

    private void dibujarAyuda(Graphics2D g2, String texto, float ancho, int base, Paleta p) {
        g2.setFont(Tema.fuente(Font.PLAIN, 12f));
        g2.setColor(p.textoSuave());
        int tw = g2.getFontMetrics().stringWidth(texto);
        g2.drawString(texto, Math.round(ancho - tw - 14), base);
    }
}
