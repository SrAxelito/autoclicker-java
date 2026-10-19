package autoclicker.servicio;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.PointerInfo;

/**
 * Comprueba si el cursor se encuentra en el dispositivo de pantalla objetivo
 * utilizando la API de AWT.
 *
 * El dispositivo de pantalla se resuelve una única vez al instanciar este objeto
 * (al inicio de la sesión) para evitar consultas continuas al entorno gráfico.
 */
public class UbicacionCursorAwt implements UbicacionCursor {

    private final GraphicsDevice dispositivoObjetivo;

    public UbicacionCursorAwt(int monitor) {
        this(resolverDispositivo(monitor));
    }

    public UbicacionCursorAwt(GraphicsDevice dispositivoObjetivo) {
        this.dispositivoObjetivo = dispositivoObjetivo;
    }

    private static GraphicsDevice resolverDispositivo(int monitor) {
        if (monitor < 0) return null;
        try {
            GraphicsDevice[] pantallas = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getScreenDevices();
            if (monitor < pantallas.length) {
                return pantallas[monitor];
            }
        } catch (Exception e) {
            // En entornos headless o errores del subsistema gráfico
        }
        return null;
    }

    @Override
    public boolean estaEnMonitorObjetivo() {
        if (dispositivoObjetivo == null) {
            return false;
        }
        try {
            PointerInfo info = MouseInfo.getPointerInfo();
            if (info == null) {
                return false;
            }
            return dispositivoObjetivo.equals(info.getDevice());
        } catch (Exception e) {
            return false;
        }
    }
}
