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

## AuthController.java
- Endpoints:
- POST /api/auth/login → recibe { email, phone } y devuelve token/usuario.
- GET /api/auth/profile/{id} → devuelve UserData.
- Clases necesarias:
- AuthRequestDTO (email, phone).
- UserDataDTO (ya lo tenemos definido desde el frontend, hay que mapearlo). 
## DiagnosisController.java
- Endpoints:
- POST /api/diagnoses → guarda diagnóstico con UserData + Message[].
- GET /api/diagnoses/user/{userId} → lista diagnósticos de un usuario.
- GET /api/diagnoses/{id} → obtiene diagnóstico por ID.
- Clases necesarias:
- DiagnosisDTO (id, userData, chatHistory, status, folio, createdAt).
- MessageDTO (role, text, isError, suggestions, timestamp).

