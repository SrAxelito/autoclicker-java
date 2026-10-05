# Software de terceros

AutoClicker se publica bajo la licencia MIT (archivo `LICENSE`). Además usa
el software de terceros que se detalla aquí, cada uno bajo su propia licencia.

## JNativeHook 2.2.2

- **Qué hace:** permite escuchar el teclado y el mouse aunque la ventana de
  AutoClicker no esté activa. Se usa para los atajos globales y la grabación.
- **Copyright:** (C) 2006-2021 Alexander Barker.
- **Licencia:** GNU Lesser General Public License, versión 3 (LGPL-3.0). La
  LGPL son permisos adicionales sobre la GNU General Public License, versión
  3, así que se incluyen los dos textos: `LGPL-3.0.md` y `GPL-3.0.md`, en
  esta misma carpeta.
- **Código fuente de esta versión:** <https://github.com/kwhat/jnativehook/tree/2.2.2>
- **Modificaciones:** ninguna. Se distribuye el archivo original,
  `jnativehook-2.2.2.jar`, separado del código de AutoClicker.

### Cómo usar otra versión de JNativeHook

La licencia te da derecho a reemplazar la librería por una versión modificada
o más reciente que sea compatible. Como va en un archivo aparte, basta con
cambiar ese archivo por el tuyo conservando el nombre `jnativehook-2.2.2.jar`:

| Dónde usas AutoClicker | Carpeta donde está el archivo |
|---|---|
| Paquete de Windows | `AutoClicker\app\` |
| Paquete de macOS | `AutoClicker.app/Contents/app/` |
| Paquete de Linux | `AutoClicker/lib/app/` |
| Versión portable | la carpeta donde descomprimiste el `.zip` |

También puedes cambiar `lib/jnativehook-2.2.2.jar` en el código fuente y
volver a compilar con `construir.bat` o `construir.sh`.

## Entorno de Java (OpenJDK)

Los paquetes para Windows, macOS y Linux incluyen un entorno de ejecución de
Java creado con `jlink` a partir de OpenJDK, para que no haga falta instalar
Java. OpenJDK se distribuye bajo la GNU General Public License, versión 2,
con la excepción "Classpath". Sus avisos legales completos están en la
carpeta `legal` del entorno de Java incluido en cada paquete. La versión
portable no incluye Java.

## JUnit 5 (solo para desarrollo)

Las pruebas usan JUnit 5, bajo la Eclipse Public License 2.0. No forma parte
de AutoClicker ni se distribuye con él: `probar.bat` y `probar.sh` lo
descargan al ejecutar las pruebas.