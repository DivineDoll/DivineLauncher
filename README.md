<p align="center">
  <img src="docs/icon.png" width="128" alt="DivineLauncher icon">
</p>

<h1 align="center">DivineLauncher</h1>

<p align="center">
  A Minecraft launcher for Android with a golden, Modrinth-inspired look.<br>
  Java Edition and Bedrock in one app, fully customizable.
</p>

<p align="center">
  <img alt="License" src="https://img.shields.io/badge/license-LGPL--3.0-FFC107">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-FFC107">
  <img alt="Languages" src="https://img.shields.io/badge/languages-EN%20%7C%20ES-FFC107">
</p>

<p align="center"><a href="README_ES.md">Leer en español</a></p>

---

> DivineLauncher is an unofficial project. It is not made, approved or affiliated with Mojang Studios or Microsoft.

## Highlights

| | |
|---|---|
| **Look** | Dark interface with a gold accent, rounded cards and pill tabs, in portrait and landscape. Change accent, background, sidebar and Play button colors, banner and themes. |
| **Instances** | Own icon, color and background per instance. Tags, favorites, duplicate, export as `.zip` or `.mrpack`, import `.zip`, `.mrpack` and CurseForge packs. |
| **Per-instance hub** | Tabs for mods (enable, disable, delete, Modrinth search, update check), worlds (manual and automatic backups), logs and look. |
| **Bedrock** | Detects the installed game. Add-ons, textures and skins in one place. |
| **Controls** | Touch layouts, gamepad support, button packs (normal or Bedrock style) globally or per instance, F1 hides the controls. |
| **In game** | Movable FPS readout, quick settings drawer, built-in screen recorder (no audio). |
| **Performance** | MobileGlues and other renderers, profiles for memory and resolution. |
| **Skins** | Manager with 3D preview. |
| **Languages** | English and Spanish. |

## Screenshots

Screenshots go in [`docs/screenshots/`](docs/screenshots). Link them here once you add them:

```markdown
![Home](docs/screenshots/home.png)
```

## Install

Build it from source (below) or download the APK from the GitHub Actions runs of this repository.

## Build

```
./gradlew :app_mojo:assembleDebug
```

On Windows use `.\gradlew.bat` and make sure `mojoexec`, `sdl` and `glfw` are symlinked into `app_pojavlauncher/src/main/jni/`.

### CurseForge key

Searching CurseForge needs a free API key from <https://console.curseforge.com>.

- **GitHub Actions:** add a repository secret named `CURSEFORGE_API_KEY`. The workflow already passes it to the build.
- **Local build:** put the key in `curseforge_key.txt` next to `gradlew`, or set the `CURSEFORGE_API_KEY` environment variable.
- **In the app:** Bedrock tab, Add-ons, "Key" button. It is also used for the Java mod search.

Never commit the key.

## Credits and license

DivineLauncher is a fork of [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), which is based on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). It is licensed under the [GNU LGPLv3](LICENSE). See [NOTICE-FORK.md](NOTICE-FORK.md).

## Contributing

Contributions are welcome, not only code. Submit changes as a pull request that explains what the change does and how to try it.

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
