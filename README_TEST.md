# Guía de Prueba para el Sistema de Autenticación

## Archivos de prueba creados

1. `test_auth.html` - Página HTML con JavaScript para probar registro y login
2. Esta guía (`README_TEST.md`)

## Requisitos previos

1. Servidor Spring Boot ejecutándose en `http://localhost:8080`
2. Dependencias necesarias en tu proyecto:
   - Spring Web
   - Jackson Databind (para JSON)

## Cómo probar

### Opción 1: Abrir directamente en el navegador (más simple para pruebas locales)

1. Abre el archivo `D:/Escritorio/BOBDSGit/BODS_2.0_BackEnd/test_auth.html` directamente en tu navegador web (Chrome, Firefox, Edge, etc.)
2. La aplicación intentará conectarse a `http://localhost:8080` para las operaciones de registro y login
3. Si ves errores de CORS, consulta la sección "Solución de problemas de CORS" abajo

### Opción 2: Servir desde un servidor web local

Para evitar problemas de CORS, puedes servir el archivo HTML desde un servidor local:

```bash
# Si tienes Python instalado:
python -m http.server 8081

# Luego abre en tu navegador: http://localhost:8081/test_auth.html
```

## Flujo de prueba recomendado

### Paso 1: Registro de usuario
1. Haz clic en la pestaña "Registro" (si no está ya seleccionada)
2. Llena el formulario con:
   - Nombre de Usuario: 3-30 caracteres alfanuméricos (ej: `usuario123`)
   - Contraseña: 3-12 caracteres con al menos una mayúscula, minúscula y número (ej: `Clave123`)
   - Email: formato válido de email (ej: `usuario@ejemplo.com`)
3. Haz clic en "Registrarse"
4. Deberías ver un mensaje de éxito en verde

### Paso 2: Inicio de sesión
1. Haz clic en la pestaña "Inicio de Sesión"
2. Llena el formulario con:
   - Email: el mismo email que usaste en el registro
   - Contraseña: la misma contraseña que usaste en el registro
3. Haz clic en "Iniciar Sesión"
4. Deberías ver un mensaje de éxito en verde con tu nombre de usuario

### Paso 3: Prueba de credenciales incorrectas
1. Intenta iniciar sesión con:
   - Email correcto pero contraseña incorrecta
   - Email incorrecto pero contraseña correcta
   - Ambos incorrectos
2. Deberías ver mensajes de error en rojo

## Solución de problemas comunes

### Problema: Errores de CORS
Si ves errores en la consola del navegador como:
```
Access to fetch at 'http://localhost:8080/api/auth/signup' from origin 'null' has been blocked by CORS policy
```

**Soluciones:**
1. Abre el archivo HTML directamente desde el navegador (no mediante `file://` en algunos navegadores puede funcionar mejor)
2. O agrega soporte CORS a tu controlador Spring Boot:
   ```java
   @CrossOrigin(origins = "*")  // En desarrollo solamente, ¡nunca en producción así!
   @RestController
   @RequestMapping("/api/auth")
   public class AuthController {
       // ...
   }
   ```

### Problema: Conexión rechazada
Si ves errores como:
```
Failed to fetch
```
o
```
net::ERR_CONNECTION_REFUSED
```

**Verifica que:**
1. Tu aplicación Spring Boot esté ejecutándose
2. Esté escuchando en el puerto 8080 (o ajusta `API_BASE_URL` en el HTML si usa otro puerto)
3. No haya firewalls bloqueando la conexión

## Próximos pasos para desarrollo

Cuando estés listo para pasar de pruebas a una aplicación real:

1. **Implementar seguridad adecuada**: Nunca almacenes contraseñas en texto plano en producción. Usa Spring Security con BCryptPasswordEncoder.

2. **Crear DTOs**: En lugar de recibir parámetros sueltos (`@RequestParam`), crea clases de transferencia de datos (DTOs) para recibir JSON en el cuerpo de la petición.

3. **Agregar validación robusta**: Usa las anotaciones de validación de Bean Validation (`@NotBlank`, `@Email`, `@Size`, `@Pattern`, etc.)

4. **Manejo de sesiones/tokens**: Implementa un sistema de autenticación basado en tokens (JWT) o sesiones seguras en lugar de solo retornar mensajes de éxito.

5. **Mejorar la UI**: Esta página HTML es funcional pero básica. Para una aplicación real, considera usar un framework frontend como React, Vue o Angular, o al menos mejorar el diseño con un framework CSS como Bootstrap.

## Estructura del JSON de usuarios

Los usuarios se almacenan en `data/usuarios.json` con este formato:
```json
[
  {
    "nombre": "usuario123",
    "password": "Clave123",  // ¡En producción esto debería estar hasheado!
    "email": "usuario@ejemplo.com"
  }
]
```

**¡Importante:** Este sistema almacena contraseñas en texto plano y es SOLO para fines educativos y de prueba. NUNCA uses este enfoque en una aplicación de producción real sin implementar adecuado hashing de contraseñas.

--- 
*Prueba realizada el: 2026-05-24*