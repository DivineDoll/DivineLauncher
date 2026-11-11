<p align="center">
  <img src="docs/icon.png" width="128" alt="Icono de DivineLauncher">
</p>

<h1 align="center">DivineLauncher</h1>

<p align="center">
  Un launcher de Minecraft para Android con un estilo dorado inspirado en Modrinth.<br>
  Java Edition y Bedrock en una sola app, totalmente personalizable.
</p>

<p align="center">
  <img alt="Licencia" src="https://img.shields.io/badge/licencia-LGPL--3.0-FFC107">
  <img alt="Plataforma" src="https://img.shields.io/badge/plataforma-Android-FFC107">
  <img alt="Idiomas" src="https://img.shields.io/badge/idiomas-ES%20%7C%20EN-FFC107">
</p>

<p align="center"><a href="README.md">Read in English</a></p>

---

> DivineLauncher es un proyecto no oficial. No está hecho, aprobado ni afiliado con Mojang Studios ni Microsoft.

## Lo principal

| | |
|---|---|
| **Estilo** | Interfaz oscura con acento dorado, tarjetas redondeadas y pestañas en píldora, en vertical y horizontal. Cambia los colores de acento, fondo, barra lateral y botón Jugar, además del banner y los temas. |
| **Instancias** | Icono, color y fondo propios. Etiquetas, favoritas, duplicar, exportar como `.zip` o `.mrpack`, importar `.zip`, `.mrpack` y packs de CurseForge. |
| **Pantalla por instancia** | Pestañas de mods (activar, desactivar, eliminar, buscar en Modrinth, revisar actualizaciones), mundos (respaldos manuales y automáticos), registros y apariencia. |
| **Bedrock** | Detecta el juego instalado. Add-ons, texturas y skins en un solo lugar. |
| **Controles** | Diseños táctiles, soporte de gamepad, paquetes de botones (normales o estilo Bedrock) globales o por instancia, F1 oculta los controles. |
| **En el juego** | FPS movible, menú de ajustes rápidos, grabador de pantalla integrado (sin audio). |
| **Rendimiento** | MobileGlues y otros renderizadores, perfiles de memoria y resolución. |
| **Skins** | Gestor con vista previa 3D. |
| **Idiomas** | Español e inglés. |

## Capturas

Las capturas van en [`docs/screenshots/`](docs/screenshots). Enlázalas aquí cuando las agregues:

```markdown
![Inicio](docs/screenshots/home.png)
```

## Instalación

Compílalo desde el código (abajo) o descarga el APK de las ejecuciones de GitHub Actions de este repositorio.

## Compilar

```
./gradlew :app_mojo:assembleDebug
```

### Clave de CurseForge

Buscar en CurseForge necesita una clave de API gratuita de <https://console.curseforge.com>.

- **GitHub Actions:** agrega un secreto del repositorio llamado `CURSEFORGE_API_KEY`. El workflow ya se lo pasa a la compilación.
- **Compilación local:** pon la clave en `curseforge_key.txt` junto a `gradlew`, o define la variable de entorno `CURSEFORGE_API_KEY`.
- **En la app:** pestaña Bedrock, Add-ons, botón «Key». También se usa para la búsqueda de mods de Java.

No subas la clave al repositorio.

## Créditos y licencia

DivineLauncher es un fork de [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), que se basa en [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). Está bajo la licencia [GNU LGPLv3](LICENSE). Mira [NOTICE-FORK.md](NOTICE-FORK.md).

## Contribuir

Toda ayuda es bienvenida, no solo código. Envía los cambios como pull request explicando qué hacen y cómo probarlos.

## Third party components, licenses and sources (when applicable)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt).
- [Holy GL4ES](https://github.com/artdeell/gl4es_extra_extra/): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE).<br>
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html).<br>
- [GLFW](https://github.com/MojoLauncher/glfw): [zlib license](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [SDL](https://github.com/MojoLauncher/MojoSDL): [zlib license](https://github.com/MojoLauncher/MojoSDL/blob/main/LICENSE.txt)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): 3-Clause BSD license
- [LWJGL3](https://github.com/LWJGL/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md).
- [mojoexec](https://github.com/MojoLauncher/mojoexec): [MIT License](https://github.com/MojoLauncher/mojoexec/blob/master/LICENSE)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html).
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java sandboxing security manager): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt).
- [bhook](https://github.com/bytedance/bhook) (Used for exit code trapping): [MIT license](https://github.com/bytedance/bhook/blob/main/LICENSE).
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (Used for authorisation via ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE).
- [alsoft](https://github.com/kcat/openal-soft/) (Audio output library): [GNU LIBRARY GENERAL PUBLIC LICENSE](https://github.com/kcat/openal-soft/blob/master/COPYING) and [modified PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft).
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE).
- Thanks to [Mineskin](https://mineskin.eu/) for providing Minecraft avatars.
