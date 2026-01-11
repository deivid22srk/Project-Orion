# Erro Corrigido: Repositórios Duplicados

## Erro
```
Build was configured to prefer settings repositories over project repositories 
but repository 'Google' was added by build file 'build.gradle'
```

## Causa
O `settings.gradle` já define os repositórios com:
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

E eu havia adicionado `allprojects { repositories }` no `build.gradle` raiz, causando conflito.

## Solução
**Removido do build.gradle:**
```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

**Mantido apenas:**
```gradle
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.8.0'
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25'
    }
}
```

O `settings.gradle` já cuida dos repositórios para dependências.

## Status
✅ Corrigido - O build deve compilar agora!
