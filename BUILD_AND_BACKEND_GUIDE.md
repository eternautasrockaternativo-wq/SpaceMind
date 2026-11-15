# SPACE MIND — Guía de Compilación y Configuración de Backend para Nueva Cuenta de AI Studio

Este documento detalla los requisitos técnicos de compilación, la configuración exacta del backend (**Firebase AI Logic + App Check**) y el checklist de diagnóstico para construir y ejecutar **SPACE MIND** correctamente al migrar, exportar o importar el proyecto en otra cuenta de **Google AI Studio**.

---

## 1. Resumen de Arquitectura y Toolchain del Build

El proyecto está configurado con versiones acopladas y validadas. **No modifiques estas versiones** salvo necesidad explícita:

- **Android Gradle Plugin (AGP):** `9.1.1`
- **Kotlin:** `2.2.10`
- **KSP:** `2.3.5`
- **Java Compatibility:** `VERSION_11`
- **SDK:** `compileSdk = 36` (`minorApiLevel = 1`), `targetSdk = 36`, `minSdk = 24`
- **Compose BOM:** `2024.09.00`
- **Firebase BoM:** `34.17.0`
- **Namespace:** `com.example` (no renombrar los paquetes fuente `com/example/`)
- **Application ID:** `com.aistudio.spacemind.kxvrzt`
- **Archivo `metadata.json`:** Debe conservar `"majorCapabilities": ["MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"]`.

### Reglas críticas de compilación en AI Studio
1. **No crear ni subir `local.properties`:** En AI Studio la variable `ANDROID_SDK_ROOT` viene preconfigurada en el contenedor. Si el proyecto exportado/importado contiene un `local.properties` de otra máquina, **elimínalo**.
2. **Usar `gradle` (no `./gradlew`):** Si ejecutas comandos en el entorno de AI Studio, utiliza `gradle assembleDebug` o `gradle :app:testDebugUnitTest`.
3. **No crear `debug.keystore` manualmente:** Nunca ejecutes `keytool -genkey`. El entorno genera y gestiona `debug.keystore` automáticamente en la raíz.
4. **Conservar `signingConfigs` y `buildTypes` en `app/build.gradle.kts`:** No elimines ni alteres `debugConfig` ni `release`.
5. **Conservar `.env.example` en la raíz:** El plugin `secrets-gradle-plugin` lo usa como archivo de respaldo. No requiere `GEMINI_API_KEY` porque la IA usa Firebase AI Logic + App Check.

---

## 2. Configuración Requerida del Servidor / Backend

**SPACE MIND** no utiliza un servidor backend propio, Firestore ni autenticación de usuarios con contraseña. Su único servicio en la nube es **Firebase AI Logic (`ai_logic`)** protegido por **Firebase App Check**, utilizando el modelo `gemini-3.8-flash` para interpretar respuestas de voz en formato JSON estructurado.

### ¿Por qué puede fallar en la nueva cuenta?
Al transferir o abrir el proyecto en otra cuenta de AI Studio:
- Cada cuenta/entorno tiene su propio certificado de firma de depuración (`debug.keystore` con distinto **SHA-1** y **SHA-256**).
- Los archivos `app/google-services.json` y `app/src/main/res/values/firebase_applet_config.xml` de la cuenta anterior pueden faltar, apuntar a un proyecto de otra cuenta o no tener registrados los tokens de App Check del nuevo entorno.
- Sin aprovisionamiento de `ai_logic` en la nueva cuenta, las llamadas a Gemini fallarán en ejecución con errores **HTTP 403 / App Check** (aunque la UI seguirá operando gracias al respaldo en *Modo Local*).

### Pasos obligatorios en la nueva cuenta de AI Studio

En la nueva cuenta, el agente de AI Studio debe ejecutar el flujo de aprovisionamiento de Firebase para **habilitar `ai_logic`** (no se debe configurar manualmente desde la consola web):

1. **Verificar o materializar `debug.keystore`:**
   - Si `debug.keystore` aún no existe en la raíz del proyecto, ejecutar una primera compilación (`compile_applet` / `gradle assembleDebug`) para que el entorno lo genere.
   - Extraer las huellas del certificado de la nueva cuenta:
     ```bash
     keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
   - Obtener exactamente los valores `SHA1` (59 caracteres) y `SHA256` (95 caracteres), sin prefijos (`SHA1:` / `SHA256:`) ni espacios.

2. **Aceptar términos y aprovisionar `ai_logic` en AI Studio:**
   - Solicitar consentimiento al usuario para Firebase (`show_aistudio_ui` con `ui_type: "firebase_setup"` y `requested_products: ["ai_logic"]`).
   - Verificar aceptación con `GetFirebaseTos`.
   - Llamar a **`ProvisionFirebase`** (o `set_up_firebase` con `requestedProducts = ["ai_logic"]` si RPC no está disponible) enviando:
     - `platform`: `"android"`
     - `package_name`: `"com.aistudio.spacemind.kxvrzt"` (debe coincidir exactamente con `applicationId` en `app/build.gradle.kts`)
     - `sha1_certificate`: `<SHA1 de la nueva cuenta>`
     - `sha256_certificate`: `<SHA256 de la nueva cuenta>`
     - `requested_products`: `["ai_logic"]`

3. **Archivos que genera automáticamente el aprovisionamiento:**
   - `app/google-services.json` (con el `project_id` y `mobilesdk_app_id` de la nueva cuenta).
   - `app/src/main/res/values/firebase_applet_config.xml` (con `recaptcha_site_key` para builds release).
   - Registro automático de tokens de depuración de **App Check** en el backend.

4. **Verificación de inicialización en el cliente (ya implementada en el código):**
   - `app/src/main/java/com/example/ai/AppCheckInit.kt` define `configureAppCheck(context, intent)`.
   - En `MainActivity.onCreate()`, `configureAppCheck(this, intent)` se ejecuta **antes** de `setContent` y de cualquier uso de Firebase.
   - En modo `BuildConfig.DEBUG`, persiste `FIREBASE_APPCHECK_DEBUG_TOKEN` de forma síncrona con `.commit()`.
   - En `VoiceAiNluService.kt`, `Firebase.ai.generativeModel("gemini-3.8-flash", ...)` se inicializa de forma perezosa (`by lazy`).

---

## 3. Dependencias y Permisos Verificados

### Dependencias Firebase activas (`app/build.gradle.kts`)
- `implementation(platform(libs.firebase.bom))`
- `implementation(libs.firebase.ai)`
- `implementation(libs.firebase.appcheck.recaptcha)`
- `implementation(libs.firebase.appcheck.debug)`

> **Nota:** Firestore y Firebase Auth están comentados intencionalmente porque no se utilizan en esta aplicación. No deben descomentarse salvo que se agregue base de datos en la nube y Google Sign-In.

### Permisos en `app/src/main/AndroidManifest.xml`
- `android.permission.INTERNET`
- `android.permission.VIBRATE`
- `android.permission.RECORD_AUDIO` (solicitado en tiempo de ejecución únicamente al activar el micrófono)
- Bloque `<queries>` para `android.speech.RecognitionService` y `android.intent.action.TTS_SERVICE` (obligatorio en Android 11+ para reconocimiento y síntesis de voz).

---

## 4. Checklist Rápido si Falla el Build en la Nueva Cuenta

Si la compilación falla al importar el código en la otra cuenta, revisa en orden:

1. **¿Existe `local.properties`?** → Bórralo.
2. **¿Falta `.env.example` o tiene un `.env` corrupto?** → Asegúrate de que exista `.env.example` en la raíz (puede estar sin variables activas).
3. **¿Falta algún recurso o imagen en `app/src/main/res/drawable/`?** → Verifica que se hayan copiado `astronaut_earth.jpg`, `hero_astronaut.jpg` y `ic_space_mind.jpg`.
4. **¿Error en `processDebugGoogleServices` o `google-services.json` inválido?** → Elimina el `app/google-services.json` y `app/src/main/res/values/firebase_applet_config.xml` de la cuenta anterior y vuelve a ejecutar `ProvisionFirebase` (`requested_products: ["ai_logic"]`) en la cuenta nueva.
5. **¿Falla al ejecutar pruebas Robolectric?** → En `ExampleRobolectricTest.kt` mantener `@Config(sdk = [34])`.
6. **¿Error de memoria o caché Gradle?** → Confirmar en `gradle.properties`:
   - `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8`
   - `kotlin.compiler.execution.strategy=in-process`
   - `googleServices.missing.passthrough=true`

---

## 5. Prompt Listo para Copiar en la Nueva Cuenta de AI Studio

Si vas a pedirle al asistente de AI Studio de la otra cuenta que deje el proyecto compilando y con el backend conectado, puedes darle esta instrucción:

> *"Lee `BUILD_AND_BACKEND_GUIDE.md`. Verifica por qué está fallando el build en esta cuenta (elimina `local.properties` si existe), re-aprovisiona Firebase AI Logic (`requested_products: ["ai_logic"]`) con `ProvisionFirebase` usando el `debug.keystore` y `package_name` (`com.aistudio.spacemind.kxvrzt`) de esta cuenta para regenerar `google-services.json` y `firebase_applet_config.xml`, y ejecuta la compilación y los tests unitarios para confirmar que funciona."*
