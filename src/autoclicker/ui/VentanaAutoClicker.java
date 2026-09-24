package autoclicker.ui;

import autoclicker.modelo.BotonMouse;
import autoclicker.modelo.ConfiguracionClics;
import autoclicker.servicio.ClickerListener;
import autoclicker.servicio.ClickerService;

import javax.swing.*;
import java.awt.*;

/**
 * Interfaz gráfica. Solo lee lo que el usuario configura, se lo pasa
 * al servicio y muestra el estado que el servicio le reporta.
 */
public class VentanaAutoClicker extends JFrame implements ClickerListener {

    private final ClickerService servicio;

    private final JSpinner intervaloSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 600_000, 10));
    private final JSpinner clicsSpinner     = new JSpinner(new SpinnerNumberModel(0, 0, 10_000_000, 1));
    private final JSpinner esperaSpinner    = new JSpinner(new SpinnerNumberModel(3, 0, 60, 1));
    private final JComboBox<BotonMouse> botonCombo = new JComboBox<>(BotonMouse.values());
    private final JCheckBox dobleCheck = new JCheckBox("Doble clic");

    private final JButton iniciarBtn = new JButton("Iniciar");
    private final JButton detenerBtn = new JButton("Detener");
    private final JLabel estadoLabel = new JLabel("Detenido", SwingConstants.CENTER);

    public VentanaAutoClicker(ClickerService servicio) {
        super("AutoClicker");
        this.servicio = servicio;

        construirInterfaz();

        iniciarBtn.addActionListener(e -> iniciar());
        detenerBtn.addActionListener(e -> servicio.detener());
        detenerBtn.setEnabled(false);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        servicio.setListener(this);
    }

    // ---- Acciones ----

    private void iniciar() {
        try {
            ConfiguracionClics config = leerConfiguracion();
            habilitarControles(false);
            servicio.iniciar(config);
        } catch (IllegalArgumentException | NullPointerException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Configuración inválida", JOptionPane.WARNING_MESSAGE);
        }
    }

    private ConfiguracionClics leerConfiguracion() {
        return new ConfiguracionClics(
                ((Number) intervaloSpinner.getValue()).longValue(),
                ((Number) clicsSpinner.getValue()).intValue(),
                ((Number) esperaSpinner.getValue()).intValue(),
                (BotonMouse) botonCombo.getSelectedItem(),
                dobleCheck.isSelected());
    }

    // ---- ClickerListener (llegan desde el hilo de clics) ----

    @Override
    public void alCambiarEstado(String mensaje) {
        SwingUtilities.invokeLater(() -> estadoLabel.setText(mensaje));
    }

    @Override
    public void alTerminar(String mensajeFinal) {
        SwingUtilities.invokeLater(() -> {
            estadoLabel.setText(mensajeFinal);
            habilitarControles(true);
        });
    }

    // ---- Construcción de la interfaz ----

    private void construirInterfaz() {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        form.add(new JLabel("Intervalo (ms):"));
        form.add(intervaloSpinner);
        form.add(new JLabel("Nº de clics (0 = infinito):"));
        form.add(clicsSpinner);
        form.add(new JLabel("Espera antes de iniciar (s):"));
        form.add(esperaSpinner);
        form.add(new JLabel("Botón del mouse:"));
        form.add(botonCombo);
        form.add(new JLabel(""));
        form.add(dobleCheck);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        botones.add(iniciarBtn);
        botones.add(detenerBtn);

        JLabel ayuda = new JLabel(
                "<html><center>Para detener: botón Detener o lleva el mouse<br>" +
                "a la esquina superior izquierda de la pantalla.</center></html>",
                SwingConstants.CENTER);
        ayuda.setFont(ayuda.getFont().deriveFont(Font.PLAIN, 11f));

        estadoLabel.setFont(estadoLabel.getFont().deriveFont(Font.BOLD, 13f));

        JPanel sur = new JPanel(new BorderLayout(0, 8));
        sur.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        sur.add(botones, BorderLayout.NORTH);
        sur.add(estadoLabel, BorderLayout.CENTER);
        sur.add(ayuda, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
    }

    private void habilitarControles(boolean habilitar) {
        iniciarBtn.setEnabled(habilitar);
        detenerBtn.setEnabled(!habilitar);
        intervaloSpinner.setEnabled(habilitar);
        clicsSpinner.setEnabled(habilitar);
        esperaSpinner.setEnabled(habilitar);
        botonCombo.setEnabled(habilitar);
        dobleCheck.setEnabled(habilitar);
    }
}
