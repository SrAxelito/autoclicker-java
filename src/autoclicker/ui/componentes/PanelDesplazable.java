package autoclicker.ui.componentes;

import autoclicker.ui.tema.Tema;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

/**
 * Zona que se desplaza en vertical cuando su contenido no cabe.
 * Mientras cabe se ve igual que un panel normal; cuando no, aparece a la
 * derecha una barra fina con los colores del tema. Nunca se desplaza en
 * horizontal: el contenido siempre ocupa el ancho disponible.
 */
public class PanelDesplazable extends JScrollPane {

    private static final int ANCHO_BARRA = 8;
    private static final int SEPARACION_BARRA = 6;   // espacio entre el contenido y la barra
    private static final int LARGO_MINIMO_BARRA = 32;
    private static final int PASO = 28;              // píxeles por cada paso de la rueda del mouse

    public PanelDesplazable(JComponent contenido) {
        super(new Vista(contenido), VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
        setBorder(null);
        setViewportBorder(null);
        setOpaque(false);
        getViewport().setOpaque(false);

        JScrollBar barra = getVerticalScrollBar();
        barra.setUI(new BarraFina());
        barra.setOpaque(false);
        barra.setFocusable(false);
        barra.setUnitIncrement(PASO);
        barra.setPreferredSize(new Dimension(SEPARACION_BARRA + ANCHO_BARRA, 0));
    }

    // ---- Contenido: ocupa el ancho visible y conserva su alto ----

    private static final class Vista extends JPanel implements Scrollable {

        Vista(JComponent contenido) {
            super(new BorderLayout());
            setOpaque(false);
            add(contenido, BorderLayout.NORTH);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) { return PASO; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
            return Math.max(PASO, visible.height - PASO);
        }
        @Override public boolean getScrollableTracksViewportWidth()  { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ---- Barra sin flechas ni fondo: solo el tirador redondeado ----

    private static final class BarraFina extends BasicScrollBarUI {

        @Override protected JButton createDecreaseButton(int orientacion) { return botonInvisible(); }
        @Override protected JButton createIncreaseButton(int orientacion) { return botonInvisible(); }

        private static JButton botonInvisible() {
            JButton boton = new JButton();
            Dimension nada = new Dimension(0, 0);
            boton.setPreferredSize(nada);
            boton.setMinimumSize(nada);
            boton.setMaximumSize(nada);
            boton.setFocusable(false);
            return boton;
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            return new Dimension(ANCHO_BARRA, LARGO_MINIMO_BARRA);
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle pista) {
            // Sin fondo: se ve lo que hay detrás.
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle tirador) {
            if (tirador.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);
            boolean resaltado = isDragging || isThumbRollover();
            g2.setColor(resaltado ? Tema.paleta().textoSuave() : Tema.paleta().borde());
            g2.fill(new RoundRectangle2D.Float(
                    tirador.x + tirador.width - ANCHO_BARRA, tirador.y,
                    ANCHO_BARRA, tirador.height, ANCHO_BARRA, ANCHO_BARRA));
            g2.dispose();
        }
    }
}