# Correção: Plugin Kotlin Duplicado

## Erro
```
Error resolving plugin [id: 'org.jetbrains.kotlin.android', version: '1.9.25']
> The request for this plugin could not be satisfied because the plugin is already 
  on the classpath with an unknown version, so compatibility cannot be checked.
```

## Causa
O plugin Kotlin estava sendo declarado **duas vezes**:

### 1. No build.gradle (raiz)
```gradle
buildscript {
    dependencies {
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25'
    }
}
```

### 2. No app/build.gradle
```gradle
plugins {
    id 'org.jetbrains.kotlin.android' version '1.9.25'  ❌ DUPLICADO
}
```

Quando usamos `buildscript` para adicionar o plugin ao classpath, **não devemos** 
especificar a versão novamente no `plugins` block.

## Solução

**app/build.gradle - ANTES:**
```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android' version '1.9.25'  ❌
    id 'org.jetbrains.kotlin.plugin.parcelize' version '1.9.25'  ❌
}
```

**app/build.gradle - DEPOIS:**
```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'  ✅
    id 'org.jetbrains.kotlin.plugin.parcelize'  ✅
}
```

A versão 1.9.25 já está definida no buildscript, então apenas aplicamos o plugin 
sem especificar versão novamente.

## Estrutura Correta Final

```
build.gradle (raiz)
├── buildscript
│   └── dependencies
│       └── kotlin-gradle-plugin:1.9.25  ← Define a versão aqui

app/build.gradle
└── plugins
    ├── com.android.application
    ├── org.jetbrains.kotlin.android  ← Usa sem versão
    └── org.jetbrains.kotlin.plugin.parcelize  ← Usa sem versão
```

## Status
✅ Corrigido - O build deve compilar com sucesso agora!

## Resumo de Todas as Correções

| # | Erro | Solução |
|---|------|---------|
| 1 | Plugin `kotlin-android` não encontrado | IDs corretos e buildscript configurado |
| 2 | Repositórios duplicados | Removido `allprojects` |
| 3 | Plugin já no classpath | Removida versão dos plugins |
