# Funcionalidad de todos los packages

### config/: configuración de seguridad, JWT, y clientes externos como Gemini.

### controller/: endpoints REST que recibe el frontend (/api/...).

### service/: lógica de negocio, validaciones, llamadas a APIs externas.

### repository/: acceso a base de datos con JPA.

### model/: entidades persistentes y modelos de datos.

### dto/: objetos que viajan entre frontend y backend.

### util/: funciones auxiliares (construcción de prompts, plantillas, JWT).

### resources/: configuración, plantillas de correo, archivos estáticos.

## Rutas principales
### Frontend → DiagnosisRequestDTO → DiagnosisEntity → DiagnosisResponse → Frontend.

## DiagnosisRequestDTO → DiagnosisController → DiagnosisService → DiagnosisRepository → DiagnosisEntity → DiagnosisResponse → Frontend