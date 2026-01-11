#!/bin/bash
set -e

REPO="https://${GITHUB_TOKEN}@github.com/deivid22srk/Project-Orion.git"
BRANCH="main"

git config --global user.name "deivid22srk"
git config --global user.email "psvstore01@gmail.com"

cd /project/workspace

echo "Initializing git repository..."
git init

echo "Adding remote..."
git remote add origin "$REPO" || git remote set-url origin "$REPO"

echo "Adding all files (excluding .codesandbox and .devcontainer)..."
git add .
git add -f java_reference

echo "Creating commit..."
git commit -m "Fixed build errors and implemented PE icon extraction - $(date +"%Y-%m-%d %H:%M:%S")"

echo "Pushing to GitHub (force push)..."
git push --force origin main:main

echo "Upload completed successfully!"
