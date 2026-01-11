#!/bin/bash
# Script para enviar todos os arquivos (inclusive ocultos) de /storage/emulated/0/filmes2/
# para o repositório GitHub, substituindo completamente o conteúdo remoto

# Configurações
FOLDER="/project/workspace/"
REPO="https://github.com/deivid22srk/Project-Orion.git"
BRANCH="main"

# Verifica se git está instalado
if ! command -v git &> /dev/null; then
    echo "Git não encontrado! Instale com: pkg install git"
    exit 1
fi

# Cria pasta temporária
TEMP_DIR=$(mktemp -d)
echo "📂 Clonando o repositório..."
git clone --branch "$BRANCH" "$REPO" "$TEMP_DIR" || exit 1

cd "$TEMP_DIR"

# Remove TODO o conteúdo existente
echo "🧹 Removendo arquivos antigos..."
git rm -rf . > /dev/null 2>&1

# Copia também os arquivos e pastas ocultos (que começam com ".")
echo "📁 Copiando arquivos novos (incluindo ocultos)..."
shopt -s dotglob
cp -r "$FOLDER"* "$TEMP_DIR/"
shopt -u dotglob

# Remove as pastas que não devem ser enviadas
echo "🗑️ Removendo pastas excluídas..."
rm -rf .codesandbox .devcontainer

# Configura identidade do Git
git config user.name "deivid22srk"
git config user.email "psvstore01@gmail.com"

# Adiciona e faz o commit
git add .
git commit -m "Fixed build errors and implemented PE icon extraction - $(date +"%Y-%m-%d %H:%M:%S")"

# Envia com força (substitui o que estiver no GitHub)
echo "🚀 Enviando para o GitHub (forçado)..."
git push --force "https://${GITHUB_TOKEN}@github.com/deivid22srk/Project-Orion.git" "$BRANCH"

echo "✅ Upload concluído com sucesso!"
