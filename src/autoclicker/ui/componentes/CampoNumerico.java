package autoclicker.ui.componentes;

import autoclicker.ui.tema.Tema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Campo numérico con botones − y + a los lados y una unidad (ms, s...).
 * Reemplaza al JSpinner con un aspecto más limpio.
 */
public class CampoNumerico extends JPanel {

    private final int minimo;
    private final int maximo;
    private final int paso;
    private int valor;

    private final JTextField campo = new CampoTexto();
    private final Etiqueta unidad;
    private final BotonPaso menos = new BotonPaso(false);
    private final BotonPaso mas = new BotonPaso(true);

    public CampoNumerico(int valorInicial, int minimo, int maximo, int paso, String textoUnidad) {
        super(new BorderLayout(4, 0));
        this.minimo = minimo;
        this.maximo = maximo;
        this.paso = paso;
        this.valor = limitar(valorInicial);

        setOpaque(false);
        setBorder(new EmptyBorder(4, 4, 4, 4));

        campo.setOpaque(false);
        campo.setBorder(new EmptyBorder(0, 4, 0, 4));
        campo.setFont(Tema.fuente(Font.BOLD, 15f));
        campo.setHorizontalAlignment(SwingConstants.RIGHT);
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new SoloDigitos(9));

        campo.addActionListener(e -> confirmar());
        campo.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(campo::selectAll);
                repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                confirmar();
                repaint();
            }
        });

        unidad = new Etiqueta(textoUnidad, Etiqueta.Rol.SUAVE, Tema.fuente(Font.PLAIN, 13f));

        JPanel centro = new JPanel(new BorderLayout(2, 0));
        centro.setOpaque(false);
        centro.add(campo, BorderLayout.CENTER);
        centro.add(unidad, BorderLayout.EAST);

        menos.addActionListener(e -> cambiar(-paso));
        mas.addActionListener(e -> cambiar(paso));

        add(menos, BorderLayout.WEST);
        add(centro, BorderLayout.CENTER);
        add(mas, BorderLayout.EAST);

        mostrar();
    }

    public int getValor() {
        confirmar();
        return valor;
    }

    public void setValor(int nuevo) {
        valor = limitar(nuevo);
        mostrar();
    }

    @Override
    public void setEnabled(boolean habilitado) {
        super.setEnabled(habilitado);
        campo.setEnabled(habilitado);
        menos.setEnabled(habilitado);
        mas.setEnabled(habilitado);
        unidad.setAtenuada(!habilitado);
        repaint();
    }

    // ---- Lógica interna ----

    private void cambiar(int delta) {
        confirmar();
        setValor(valor + delta);
    }

    private void confirmar() {
        String texto = campo.getText().trim();
        long numero = texto.isEmpty() ? minimo : Long.parseLong(texto);
        valor = limitar(numero);
        mostrar();
    }

    private int limitar(long n) {
        return (int) Math.max(minimo, Math.min(maximo, n));
    }

    private void mostrar() {
        String texto = String.valueOf(valor);
        if (!texto.equals(campo.getText())) campo.setText(texto);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        RoundRectangle2D forma = new RoundRectangle2D.Float(
                0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, Tema.RADIO_CAMPO, Tema.RADIO_CAMPO);
        g2.setColor(isEnabled() ? Tema.paleta().campo() : Tema.paleta().deshabilitado());
        g2.fill(forma);
        boolean enfocado = campo.isFocusOwner();
        g2.setColor(enfocado ? Tema.paleta().acento() : Tema.paleta().borde());
        g2.setStroke(new BasicStroke(enfocado ? 1.6f : 1f));
        g2.draw(forma);
        g2.dispose();
    }

    // ---- Campo de texto que toma sus colores del tema activo ----

    private static final class CampoTexto extends JTextField {
        CampoTexto() {
            super(5);
        }

        @Override public Color getForeground()        { return Tema.paleta().texto(); }
        @Override public Color getDisabledTextColor() { return Tema.paleta().textoDeshabilitado(); }
        @Override public Color getCaretColor()        { return Tema.paleta().acento(); }
        @Override public Color getSelectedTextColor() { return Tema.paleta().texto(); }
        @Override public Color getSelectionColor() {
            return Tema.mezclar(Tema.paleta().acento(), Tema.paleta().campo(), 0.7f);
        }
    }

    // ---- Botón − / + ----

    private static final class BotonPaso extends JButton {
        private final boolean esMas;

        BotonPaso(boolean esMas) {
            this.esMas = esMas;
            setPreferredSize(new Dimension(30, 30));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);
            int w = getWidth();
            int h = getHeight();

            if (isEnabled() && (getModel().isRollover() || getModel().isPressed())) {
                g2.setColor(getModel().isPressed() ? Tema.mezclar(Tema.paleta().borde(), Color.BLACK, 0.06f) : Tema.paleta().borde());
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
            }

            g2.setColor(!isEnabled() ? Tema.paleta().textoDeshabilitado()
                    : getModel().isRollover() ? Tema.paleta().texto() : Tema.paleta().textoSuave());
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2f;
            float cy = h / 2f;
            float r = 5f;
            g2.draw(new Line2D.Float(cx - r, cy, cx + r, cy));
            if (esMas) g2.draw(new Line2D.Float(cx, cy - r, cx, cy + r));
            g2.dispose();
        }
    }

    // ---- Solo permite escribir dígitos, con un máximo de caracteres ----

    private static final class SoloDigitos extends DocumentFilter {
        private final int maxLargo;

        SoloDigitos(int maxLargo) {
            this.maxLargo = maxLargo;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, texto, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int largo, String texto, AttributeSet attr)
                throws BadLocationException {
            if (texto == null) texto = "";
            String digitos = texto.replaceAll("\\D", "");
            int espacio = maxLargo - (fb.getDocument().getLength() - largo);
            if (digitos.length() > espacio) digitos = digitos.substring(0, Math.max(0, espacio));
            super.replace(fb, offset, largo, digitos, attr);
        }
    }
}
