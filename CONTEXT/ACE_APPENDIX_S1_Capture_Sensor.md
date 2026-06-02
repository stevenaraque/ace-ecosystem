# Apéndice 1 — Sistema 1: Captura de Sensor (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.1  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 1)  
> **Nota:** Este apéndice utiliza los tipos de datos nativos de **Google Health Services** (`androidx.health:health-services-client`). No se define un tipo de dato propio para la frecuencia cardíaca; se adopta la semántica, estructura y contrato que la librería impone.

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 1 en cuatro subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los cuatro subsistemas son:

1. **Subsistema de Comunicación Inter-Dispositivo** (cómo se hablan el reloj y el móvil).
2. **Subsistema de Recolección** (cómo el reloj obtiene la frecuencia cardíaca vía Health Services).
3. **Subsistema de Recepción** (cómo el móvil recibe y deserializa los datos nativos).
4. **Subsistema de Bufferización** (cómo el móvil almacena temporalmente las muestras).

---

## 2. Subsistema de Comunicación Inter-Dispositivo

### 2.1. Principio rector
El reloj y el móvil no se conocen directamente como dispositivos Bluetooth. Google Play Services proporciona una capa de abstracción llamada **Wearable Data Layer**, que elige automáticamente el transporte (Bluetooth Low Energy, Bluetooth clásico o WiFi) según disponibilidad, batería y latencia.

A.C.E utiliza dos mecanismos de esta capa, cada uno con semántica distinta:

| Mecanismo | Semántica | Persistencia | Uso en A.C.E |
|-----------|-----------|--------------|--------------|
| **DataClient** | Clave-valor sincronizado entre nodos. Cada dato tiene un URI único (path + timestamp). | **Sí.** El SO bufferiza internamente si el par no está conectado. Capacidad estimada: ~100–200 items o hasta 24 horas. Luego descarta los más antiguos. | **Canal de datos del sensor.** El reloj escribe cada muestra de frecuencia cardíaca en un path prefijado. El móvil recibe automáticamente cuando esté al alcance. |
| **MessageClient** | Mensaje fire-and-forget. No se almacena. | **No.** Si el destinatario no está conectado en el momento del envío, el mensaje se pierde. | **Canal de comandos.** El móvil envía órdenes de control (`START`, `STOP`) al reloj. Si el reloj no está al alcance, el comando se pierde; esto es aceptable porque el usuario puede detener la sesión desde la interfaz del móvil. |

### 2.2. Contrato de paths
Todo dato que circula por el Data Layer debe registrarse en un path jerárquico. A.C.E reserva los siguientes:

- `/ace/health/heart_rate` → Muestras individuales de frecuencia cardíaca, provenientes de Health Services (del reloj al móvil).
- `/ace/session/{id}/status` → Comandos de sesión (del móvil al reloj).

El path incluye obligatoriamente un identificador temporal en la URI para garantizar que cada muestra sea un nodo único en el grafo de datos sincronizados. Si dos muestras compartieran URI, la segunda sobrescribiría a la primera.

### 2.3. Formato de carga útil
El reloj no inventa un formato propio. Utiliza la representación serializada que Health Services proporciona para el tipo `HEART_RATE_BPM`. Al enviar a través del DataClient, la carga útil contiene:

- `value`: valor numérico (double) representando latidos por minuto.
- `time_interval`: intervalo de tiempo del sistema (start y end) que Health Services asigna a la muestra. En el caso de frecuencia cardíaca instantánea, start y end suelen coincidir o representar una ventana de un segundo.
- `data_type`: identificador semántico del tipo de dato, que el móvil utiliza para confirmar que la muestra recibida es efectivamente `HEART_RATE_BPM` y no otro tipo de Health Services.

El móvil interpreta el campo `value` como el dato crudo `bpm` y el campo de tiempo como el `timestamp` de captura. No aplica transformaciones matemáticas ni correcciones de zona horaria.

### 2.4. Invariantes de comunicación
- El reloj **nunca** inicia una transmisión por iniciativa propia. Solo transmite cuando el móvil le ha enviado previamente un `START` (o cuando el buffer interno del SO sincroniza datos pendientes tras reconexión).
- El móvil **nunca** solicita datos al reloj. El modelo es puramente push: el reloj emite, el móvil escucha.
- No existe conexión directa Wear OS → Backend. Toda comunicación pasa obligatoriamente por el móvil.

---

## 3. Subsistema de Recolección (Wear OS / Health Services)

### 3.1. Fuente de datos
El reloj obtiene la frecuencia cardíaca a través de la API **Health Services** (`androidx.health:health-services-client`), que abstrae el hardware específico del fabricante. La aplicación A.C.E no accede al sensor físico directamente; se suscribe a un flujo de datos del sistema operativo usando el tipo nativo `HEART_RATE_BPM`.

### 3.2. Modo de adquisición
Health Services entrega muestras mediante un patrón de **callback**: cada vez que el sensor tiene una nueva lectura, el sistema invoca una función registrada por la aplicación. La frecuencia de invocación depende del hardware y del estado del dispositivo, pero en contexto de ejercicio es típicamente de una muestra por segundo (1 Hz).

El tipo de dato `HEART_RATE_BPM` define su valor como un `Double` y su intervalo temporal como un `Duration` relativo al arranque del dispositivo. La aplicación A.C.E no redefine estos tipos; los consume tal cual los expone la librería.

### 3.3. Responsabilidades del reloj
| Responsabilidad | Detalle |
|-----------------|---------|
| Activar el flujo | Registrar el callback de `MeasureClient` para `HEART_RATE_BPM` cuando recibe `START` del móvil. |
| Desactivar el flujo | Cancelar el callback cuando recibe `STOP` del móvil o cuando la sesión se aborta. |
| Formatear | Empaquetar el objeto `HeartRate` nativo de Health Services en la carga útil del DataClient, preservando `value` y `time_interval`. |
| Emitir | Escribir el dato en el DataClient bajo el path `/ace/health/heart_rate`. |
| Escuchar comandos | Mantener un listener en MessageClient bajo el path `/ace/session/{id}/status` para recibir `START` y `STOP`. |

### 3.4. Qué NO hace el reloj
- No agrupa muestras en lotes.
- No calcula promedios, máximos ni mínimos.
- No conoce el concepto de "bloque" ni de "sesión" más allá de los comandos START/STOP.
- No decide si una muestra es válida o sospechosa.
- No persiste muestras en disco local del reloj.
- No redefine los tipos de Health Services; los usa como entidades opacas que solo transporta.

---

## 4. Subsistema de Recepción (APK / Móvil)

### 4.1. Interfaz de entrada
El móvil implementa un listener del DataClient que se ejecuta cada vez que el sistema operativo detecta un cambio en el grafo de datos sincronizados. Este listener recibe un lote de eventos (uno o varios) y debe procesarlos secuencialmente.

### 4.2. Procesamiento de eventos
Para cada evento recibido:

1. **Filtrar por path.** Solo se procesan eventos cuya URI coincide exactamente con `/ace/health/heart_rate`.
2. **Validar tipo semántico.** Se verifica que el dato recibido declare explícitamente el tipo `HEART_RATE_BPM` (o su identificador equivalente en la serialización del DataClient). Si el tipo no coincide, se descarta.
3. **Extraer carga útil.** Se deserializa el objeto nativo para obtener:
   - `value` → interpretado como latidos por minuto (entero o double, según lo que imponga Health Services).
   - `time_interval` → interpretado como el timestamp de captura, convertido a epoch millis si fuera necesario según la política de tiempo de Health Services.
4. **Validar rango.** Se verifica que `value` sea un número positivo dentro de rangos fisiológicos razonables (ej. 30–250). Si falla, se descarta silenciosamente y se registra en log interno.
5. **Inyectar al buffer.** Se envía al subsistema de bufferización (ver sección 5).
6. **Notificar a la UI.** Se emite un evento interno para que la interfaz de usuario muestre la frecuencia cardíaca en tiempo real.

### 4.3. Detección de desconexión
El móvil no recibe un evento explícito de "el reloj se desconectó". En su lugar, infiere la desconexión por **timeout**:

- Si el tiempo transcurrido desde la última muestra recibida supera un umbral configurado (por defecto: 5 segundos), el móvil asume que el reloj está fuera de alcance o que el sensor se detuvo.
- Este estado se refleja en la interfaz como un indicador visual (por ejemplo, un chip de color amarillo) pero **no** detiene automáticamente la sesión de ejercicio. La decisión de pausar o detener corresponde al usuario o al Sistema 2.

### 4.4. Invariantes de recepción
- El móvil no solicita retransmisión de muestras perdidas. Si el buffer del SO se saturó y descartó datos, esos datos se consideran irrecuperables.
- El móvil no modifica el timestamp recibido. No aplica correcciones de zona horaria ni de deriva de reloj.
- El móvil no persiste cada muestra individual en base de datos local. La persistencia ocurre solo cuando el Sistema 2 cierra un bloque.
- El móvil no recalcula el valor de BPM; consume el que Health Services generó y el reloj transmitió.

---

## 5. Subsistema de Bufferización (APK / Móvil)

### 5.1. Naturaleza del buffer
El móvil mantiene una **estructura de datos en memoria RAM** (no en disco) con política de reemplazo circular (FIFO). Su propósito es acumular las muestras de una ventana temporal hasta que el Sistema 2 decida cerrar un bloque de ejercicio.

Cada entrada del buffer es una tupla que encapsula:
- `bpm`: el valor numérico extraído del tipo nativo `HEART_RATE_BPM`.
- `timestamp`: el instante de captura asociado por Health Services.

### 5.2. Capacidad
La capacidad del buffer se dimensiona para contener exactamente la ventana de captura de un bloque: **300 muestras**, equivalente a 5 minutos de adquisición a 1 Hz.

### 5.3. Comportamiento ante llenado
- Si el buffer alcanza su capacidad máxima y aún no se ha cerrado el bloque, la muestra más antigua se descarta para dar entrada a la nueva.
- Esta pérdida es aceptable porque el Sistema 2 debe cerrar el bloque antes de los 300 segundos (con tolerancia de ±10%). Si el buffer se llena, indica una anomalía en el Sistema 2 que debe ser manejada allí.

### 5.4. Operaciones soportadas
| Operación | Descripción |
|-----------|-------------|
| **Put** | Insertar una nueva muestra al final. Complejidad O(1). |
| **Drain** | Extraer todas las muestras acumuladas, vaciando el buffer. Invocado por el Sistema 2 al cerrar un bloque. Complejidad O(n). |
| **Peek last** | Consultar el timestamp de la última muestra sin extraerla. Usado por el detector de desconexión. Complejidad O(1). |

### 5.5. Qué NO hace el buffer
- No escribe a SQLite.
- No calcula agregados (promedio, máximo, mínimo). Eso es responsabilidad del Sistema 2 al drenar el buffer.
- No filtra muestras anómalas (por ejemplo, 220 bpm). Eso es responsabilidad del Sistema 5 (Cálculo de XP) o del Sistema 2 al cerrar el bloque.
- No reinterpreta los tipos nativos de Health Services; almacena los valores ya extraídos.

---

## 6. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         WEAR OS (Reloj)                              │
│                                                                      │
│  ┌──────────────┐    ┌────────────────────────┐    ┌──────────────┐  │
│  │ Sensor de FC │───►│ Health Services API    │───►│ DataClient   │  │
│  │ (hardware)   │    │ (MeasureClient)        │    │ (/ace/health/│  │
│  └──────────────┘    │ Tipo: HEART_RATE_BPM   │    │  heart_rate) │  │
│                      └────────────────────────┘    └──────────────┘  │
│                                                      │               │
│  ┌───────────────────────────────────────────────────┘               │
│  │ MessageClient (listener)                                           │
│  │  path: /ace/session/{id}/status  ←  START / STOP (desde móvil)     │
│  └───────────────────────────────────────────────────────────────────┘
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Wearable Data Layer
                              │ (Bluetooth / WiFi, gestionado por SO)
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         APK (Móvil)                                  │
│                                                                      │
│  ┌──────────────────┐    ┌──────────────────┐    ┌────────────────┐  │
│  │ DataClient       │───►│ OnDataChanged    │───►│ Buffer Circular│  │
│  │ Listener         │    │ (filtra path y   │    │ (RAM, 300 max) │  │
│  │                  │    │  tipo HEART_RATE)│    │                │  │
│  └──────────────────┘    └──────────────────┘    └────────────────┘  │
│                              │                          │            │
│                              ▼                          ▼            │
│                        ┌──────────┐              ┌──────────────┐      │
│                        │ UI en vivo│              │ Sistema 2    │      │
│                        │ (FC actual)│              │ (cierra bloque)│      │
│                        └──────────┘              └──────────────┘      │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │ Timeout Detector: si >5s sin muestra → indicador desconectado   │ │
│  └─────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. Decisiones Arquitectónicas Consolidadas (Sistema 1)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Tipo de dato de FC** | `HEART_RATE_BPM` nativo de Health Services | Reutilizamos la semántica, validación y estructura que Google define. No inventamos un tipo propio. |
| **Mecanismo de datos** | DataClient | Bufferización automática por el SO si hay desconexión. |
| **Mecanismo de comandos** | MessageClient | Semántica fire-and-forget es suficiente para START/STOP. |
| **Dato único transmitido** | HeartRate sample (tipo nativo) | Speed, Distance, Calories, etc., son derivados o se manejan en Sistema 2. |
| **Timestamp** | Intervalo temporal que Health Services asocia a la muestra | Fuente de verdad temporal única. El móvil no reinterpreta. |
| **Buffer del móvil** | RAM circular, 300 muestras | Suficiente para 5 minutos a 1 Hz. No toca disco. |
| **Detección de desconexión** | Timeout por silencio (>5s) | No hay evento de desconexión explícito en el Data Layer. |
| **Reloj NO persiste** | Sin SQLite/Room en Wear OS | Reduce tamaño de APK del reloj. El SO bufferiza si es necesario. |
| **Móvil NO solicita** | Modelo push puro | El reloj emite; el móvil consume. No hay polling ni ACKs. |

---

## 8. Glosario de Términos (Sistema 1)

| Término | Definición |
|---------|------------|
| **Health Services** | Librería oficial de Google (`androidx.health:health-services-client`) que expone sensores de salud y fitness en Wear OS mediante tipos de datos estandarizados. |
| **HEART_RATE_BPM** | Tipo de dato nativo de Health Services que representa una muestra instantánea de frecuencia cardíaca. Su valor es un `Double` y su intervalo temporal un `Duration`. |
| **MeasureClient** | Cliente de Health Services que permite suscribirse a flujos de datos de sensor en tiempo real mediante callbacks. |
| **Muestra** | Un par `(bpm, timestamp)` individual, obtenido del campo `value` y `time_interval` del tipo `HEART_RATE_BPM`. |
| **DataClient** | API de sincronización clave-valor entre nodos Wear OS. Persistente y automática. |
| **MessageClient** | API de mensajería instantánea entre nodos. No persistente. |
| **Path** | URI jerárquica que identifica el tipo de dato en el Data Layer. |
| **Buffer circular** | Estructura FIFO de capacidad fija en RAM. Sobrescribe la entrada más antigua cuando está llena. |
| **Drain** | Operación de extracción completa del buffer, entregando todas las muestras acumuladas y vaciando la estructura. |
| **Timeout de desconexión** | Umbral de tiempo sin recibir muestras tras el cual el móvil asume que el reloj está desconectado. |

---

## 9. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 2 — Sesión** | El Sistema 2 cierra un bloque cada ~300s y solicita un `drain()` del buffer. | Sistema 2 → Buffer |
| **Sistema 5 — XP** | El Sistema 5 recibe las muestras drenadas y calcula métricas agregadas (avg, max) para derivar XP. | Buffer → Sistema 5 (vía Sistema 2) |
| **Sistema 8 — Notificaciones** | Si el timeout de desconexión se dispara durante una sesión activa, el Sistema 8 puede mostrar una notificación persistente. | Buffer → Sistema 8 |
