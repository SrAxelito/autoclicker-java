## Tipo de cambio

Marca con una `x` la opción correspondiente:

- [ ] `feat`: Nueva funcionalidad.
- [ ] `fix`: Corrección de un error.
- [ ] `docs`: Modificación o adición en la documentación.
- [ ] `refactor`: Refactorización de código sin cambios en funcionalidad ni contratos.
- [ ] `test`: Adición o actualización de pruebas unitarias o de integración.
- [ ] `chore`: Tareas de mantenimiento, dependencias o configuración de build.

---

## Descripción

Describe brevemente el propósito de este Pull Request, qué problema resuelve o qué objetivo persigue.

### Resumen técnico
- 

---

## Cambios por capas

Indica los módulos o capas impactadas por los cambios:

- [ ] **UI / Presentación:** (`autoclicker.ui`)
- [ ] **Dominio / Modelo:** (`autoclicker.modelo`)
- [ ] **Servicios / Lógica de negocio:** (`autoclicker.servicio`)
- [ ] **Persistencia / Configuración:** (`autoclicker.persistencia`)
- [ ] **Scripts / Build / CI:** (`probar.*`, `construir.*`, `.github/workflows/*`)

---

## Checklist de pruebas y validación

Antes de solicitar revisión, confirma los siguientes pasos:

- [ ] Se ejecutaron las pruebas locales con `probar.bat` (Windows) o `./probar.sh` (Linux/macOS) y finalizaron con éxito (`Todas las pruebas pasaron`).
- [ ] No se generaron advertencias ni errores durante la compilación (`javac`).
- [ ] Si aplica, se verificó el empaquetado y ejecución con `construir.bat` / `./construir.sh`.
- [ ] El código sigue las convenciones y estilo del proyecto.

---

## Evidencia

Pega aquí capturas de pantalla, fragmentos de log o salida de consola que demuestren que los cambios funcionan como se espera:

```text
<!-- Pega aquí la salida de pruebas o logs relevantes -->
```
