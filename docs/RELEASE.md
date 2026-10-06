# 배포 방법

앱은 시작할 때 GitHub API(`repos/choimy0204/AICam/releases/latest`)를 조회합니다. 릴리스 태그(`v1.1`)가 설치된 `versionName`보다 높으면 업데이트 대화상자를 띄우고, 릴리스에 첨부된 `.apk`를 내려받아 시스템 설치 화면을 엽니다.

## 새 버전 올리기

1. `app/build.gradle.kts`에서 `versionCode`를 1 올리고 `versionName`도 올립니다 (예: `1.1`).
2. 서명된 APK를 빌드합니다.
   ```
   $env:JAVA_HOME='D:\AndroidStudio\jbr'; .\gradlew.bat assembleRelease
   ```
3. 커밋하고 푸시한 뒤 릴리스를 만듭니다. 태그는 `v` + versionName으로 하고, 파일 이름은 반드시 `AICam.apk`로 합니다(README 다운로드 링크가 이 이름을 씁니다).
   ```
   Copy-Item app\build\outputs\apk\release\app-release.apk AICam.apk
   gh release create v1.1 AICam.apk --title "v1.1" --notes "바뀐 점"
   ```
   릴리스 노트(`--notes`)는 앱의 업데이트 대화상자에 그대로 보입니다.

## 서명 키 (중요)

- `keystore/aicam-release.jks`와 `keystore.properties`는 저장소에 올리지 않습니다(.gitignore).
- Android는 같은 키로 서명된 APK끼리만 업데이트 설치를 허용합니다. 키를 잃어버리면 기존 사용자는 앱을 지우고 다시 설치해야 하므로, 두 파일을 안전한 곳에 따로 백업해 두세요.
- `keystore.properties`가 없으면 release 빌드는 서명되지 않은 APK가 되어 설치할 수 없습니다.
